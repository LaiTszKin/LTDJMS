#!/usr/bin/env bash

# 環境變數同步腳本
# 功能：將 .env 依照 .env.example 同步
# - 備份現有 .env 到 .env.bak
# - 移除過時的環境變數（.env 有、.env.example 無）
# - 添加缺失的新配置（.env.example 有、.env 無）
# - 既有鍵保留 .env 目前值

set -euo pipefail

ENV_FILE=".env"
EXAMPLE_FILE=".env.example"
BACKUP_FILE=".env.bak"

# 顏色定義
COLOR_RESET='\033[0m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[0;33m'
COLOR_RED='\033[0;31m'
COLOR_BLUE='\033[0;34m'
COLOR_CYAN='\033[0;36m'

# 日誌函數
log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[✓]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[!]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[✗]${COLOR_RESET} $1"
}

contains_key() {
    local keys="$1"
    local key="$2"
    printf '%s\n' "$keys" | grep -Fqx "$key"
}

parse_env_keys() {
    local file="$1"
    awk '
        {
            line = $0
            sub(/\r$/, "", line)
            sub(/^[[:space:]]*export[[:space:]]+/, "", line)
            if (line ~ /^[A-Za-z_][A-Za-z0-9_]*=/) {
                split(line, parts, "=")
                print parts[1]
            }
        }
    ' "$file" | sort -u
}

get_env_value() {
    local file="$1"
    local key="$2"
    awk -v key="$key" '
        {
            line = $0
            sub(/\r$/, "", line)
            sub(/^[[:space:]]*export[[:space:]]+/, "", line)
            if (line ~ ("^" key "=")) {
                sub(/^[^=]*=/, "", line)
                print line
                exit
            }
        }
    ' "$file"
}

replace_key_value_in_file() {
    local file="$1"
    local key="$2"
    local value="$3"
    local next_file="${file}.next"

    awk -v key="$key" -v value="$value" '
        {
            line = $0
            raw = line
            sub(/^[[:space:]]*export[[:space:]]+/, "", raw)

            if (raw ~ ("^" key "=")) {
                # 若 .env.example 使用 export 前綴，保留前綴
                prefix = ""
                if (match(line, /^[[:space:]]*export[[:space:]]+/)) {
                    prefix = substr(line, RSTART, RLENGTH)
                }
                print prefix key "=" value
            } else {
                print line
            }
        }
    ' "$file" > "$next_file"

    mv "$next_file" "$file"
}

check_files() {
    if [ ! -f "$EXAMPLE_FILE" ]; then
        log_error "$EXAMPLE_FILE 檔案不存在"
        exit 1
    fi

    if [ ! -f "$ENV_FILE" ]; then
        log_warning "$ENV_FILE 檔案不存在，將以 $EXAMPLE_FILE 建立"
        cp "$EXAMPLE_FILE" "$ENV_FILE"
        log_success "已建立 ${ENV_FILE}（此次無舊檔可備份）"
        exit 0
    fi
}

backup_env() {
    log_info "備份 $ENV_FILE 到 $BACKUP_FILE"
    cp "$ENV_FILE" "$BACKUP_FILE"
    log_success "備份完成"
}

sync_env() {
    local example_keys="$1"
    local env_keys="$2"
    local temp_file

    temp_file=$(mktemp "${ENV_FILE}.tmp.XXXXXX")
    trap 'rm -f "$temp_file" "${temp_file}.next"' EXIT

    cp "$EXAMPLE_FILE" "$temp_file"

    while IFS= read -r key; do
        [ -z "$key" ] && continue
        if contains_key "$example_keys" "$key"; then
            local value
            value="$(get_env_value "$ENV_FILE" "$key")"
            replace_key_value_in_file "$temp_file" "$key" "$value"
        fi
    done <<< "$env_keys"

    mv "$temp_file" "$ENV_FILE"
    trap - EXIT
}

main() {
    echo -e "${COLOR_CYAN}========================================${COLOR_RESET}"
    echo -e "${COLOR_CYAN}  環境變數同步腳本${COLOR_RESET}"
    echo -e "${COLOR_CYAN}========================================${COLOR_RESET}"
    echo ""

    check_files
    backup_env

    local example_keys
    local env_keys
    example_keys="$(parse_env_keys "$EXAMPLE_FILE")"
    env_keys="$(parse_env_keys "$ENV_FILE")"

    local to_delete=()
    while IFS= read -r key; do
        [ -z "$key" ] && continue
        if ! contains_key "$example_keys" "$key"; then
            to_delete+=("$key")
        fi
    done <<< "$env_keys"

    local to_add=()
    while IFS= read -r key; do
        [ -z "$key" ] && continue
        if ! contains_key "$env_keys" "$key"; then
            to_add+=("$key")
        fi
    done <<< "$example_keys"

    if [ ${#to_delete[@]} -eq 0 ] && [ ${#to_add[@]} -eq 0 ]; then
        log_success "$ENV_FILE 已與 $EXAMPLE_FILE 同步（無新增/刪除鍵）"
        log_info "舊版 $ENV_FILE 已備份至 $BACKUP_FILE"
        exit 0
    fi

    echo ""
    log_info "執行同步（無互動提示）"

    if [ ${#to_delete[@]} -gt 0 ]; then
        echo -e "  ${COLOR_RED}將刪除 ${#to_delete[@]} 個過時變數：${COLOR_RESET}"
        for key in "${to_delete[@]}"; do
            echo -e "    ${COLOR_RED}-${COLOR_RESET} $key"
        done
    fi

    if [ ${#to_add[@]} -gt 0 ]; then
        echo -e "  ${COLOR_GREEN}將添加 ${#to_add[@]} 個新變數：${COLOR_RESET}"
        for key in "${to_add[@]}"; do
            echo -e "    ${COLOR_GREEN}+${COLOR_RESET} $key"
        done
    fi

    sync_env "$example_keys" "$env_keys"

    echo ""
    echo -e "${COLOR_CYAN}========================================${COLOR_RESET}"
    echo -e "${COLOR_CYAN}  同步完成${COLOR_RESET}"
    echo -e "${COLOR_CYAN}========================================${COLOR_RESET}"
    echo ""

    if [ ${#to_delete[@]} -gt 0 ]; then
        echo -e "${COLOR_RED}已刪除的變數：${COLOR_RESET}"
        for key in "${to_delete[@]}"; do
            echo -e "  ${COLOR_RED}-${COLOR_RESET} $key"
        done
        echo ""
    fi

    if [ ${#to_add[@]} -gt 0 ]; then
        echo -e "${COLOR_GREEN}已添加的變數：${COLOR_RESET}"
        for key in "${to_add[@]}"; do
            local value
            value="$(get_env_value "$ENV_FILE" "$key")"
            echo -e "  ${COLOR_GREEN}+${COLOR_RESET} $key=$value"
        done
        echo ""
    fi

    log_success "環境變數同步完成！"
    log_info "舊版 $ENV_FILE 已備份至 $BACKUP_FILE"
}

main "$@"
