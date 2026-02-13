#!/usr/bin/env bash
#
# Create database from .env settings if it does not exist.
#
# Reads (in priority):
#   DB_URL (jdbc:postgresql://host:port/database)
#   or DATABASE_HOST / DATABASE_PORT / DATABASE_NAME
#
# Auth:
#   DB_USERNAME / DB_PASSWORD
#
# Usage:
#   ./scripts/db/create-db.sh
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

DEFAULT_DB_HOST="localhost"
DEFAULT_DB_PORT="5432"
DEFAULT_DB_NAME="currency_bot"
DEFAULT_DB_USERNAME="postgres"
DEFAULT_DB_PASSWORD="postgres"

log_info() {
  printf '[INFO] %s\n' "$1"
}

log_success() {
  printf '[OK] %s\n' "$1"
}

log_error() {
  printf '[ERROR] %s\n' "$1" >&2
}

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    log_error "File not found: $file"
    exit 1
  fi
}

read_env_value() {
  local key="$1"
  awk -v key="$key" '
    {
      line = $0
      sub(/\r$/, "", line)
      if (line ~ /^[[:space:]]*#/ || line ~ /^[[:space:]]*$/) next
      sub(/^[[:space:]]*export[[:space:]]+/, "", line)
      if (line !~ /=/) next

      raw_key = line
      sub(/=.*/, "", raw_key)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", raw_key)
      if (raw_key != key) next

      value = line
      sub(/^[^=]*=/, "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)

      quoted = 0
      if (value ~ /^".*"$/ || value ~ /^'\''.*'\''$/) {
        value = substr(value, 2, length(value) - 2)
        quoted = 1
      }
      if (!quoted) {
        sub(/[[:space:]]+#.*/, "", value)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      }

      print value
      exit
    }
  ' "$ENV_FILE"
}

parse_jdbc_url() {
  local jdbc_url="$1"
  python3 - "$jdbc_url" <<'PY'
import re
import sys

url = sys.argv[1].strip()
pattern = r"^jdbc:postgresql://(?P<host>\[[^\]]+\]|[^:/?#]+)(?::(?P<port>\d+))?/(?P<db>[^?;]+)"
match = re.match(pattern, url)
if not match:
    sys.exit(1)

host = match.group("host")
if host.startswith("[") and host.endswith("]"):
    host = host[1:-1]

port = match.group("port") or "5432"
db = match.group("db")
print(host)
print(port)
print(db)
PY
}

escape_sql_literal() {
  local value="$1"
  printf '%s' "$value" | sed "s/'/''/g"
}

escape_sql_identifier() {
  local value="$1"
  printf '%s' "$value" | sed 's/"/""/g'
}

run_psql_local() {
  local sql="$1"
  PGPASSWORD="$DB_PASSWORD" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USERNAME" \
    -d postgres \
    -v ON_ERROR_STOP=1 \
    -w \
    -qAt \
    -c "$sql"
}

run_psql_compose() {
  local sql="$1"
  docker compose exec -T \
    -e PGPASSWORD="$DB_PASSWORD" \
    postgres \
    psql \
    -h localhost \
    -p 5432 \
    -U "$DB_USERNAME" \
    -d postgres \
    -v ON_ERROR_STOP=1 \
    -w \
    -qAt \
    -c "$sql"
}

can_use_compose_postgres() {
  command -v docker >/dev/null 2>&1 \
    && docker compose ps --services --status running 2>/dev/null | grep -qx 'postgres'
}

run_sql() {
  local runner="$1"
  local sql="$2"

  if [[ "$runner" == "compose" ]]; then
    run_psql_compose "$sql"
  else
    run_psql_local "$sql"
  fi
}

main() {
  require_file "$ENV_FILE"

  local db_url
  db_url="$(read_env_value "DB_URL" || true)"

  DB_HOST="$(read_env_value "DATABASE_HOST" || true)"
  DB_PORT="$(read_env_value "DATABASE_PORT" || true)"
  DB_NAME="$(read_env_value "DATABASE_NAME" || true)"
  DB_USERNAME="$(read_env_value "DB_USERNAME" || true)"
  DB_PASSWORD="$(read_env_value "DB_PASSWORD" || true)"

  DB_HOST="${DB_HOST:-$DEFAULT_DB_HOST}"
  DB_PORT="${DB_PORT:-$DEFAULT_DB_PORT}"
  DB_NAME="${DB_NAME:-$DEFAULT_DB_NAME}"
  DB_USERNAME="${DB_USERNAME:-$DEFAULT_DB_USERNAME}"
  DB_PASSWORD="${DB_PASSWORD:-$DEFAULT_DB_PASSWORD}"

  if [[ -n "$db_url" ]]; then
    local parsed

    if ! parsed="$(parse_jdbc_url "$db_url" 2>/dev/null)"; then
      log_error "Invalid DB_URL format: $db_url"
      exit 1
    fi

    DB_HOST="$(printf '%s\n' "$parsed" | sed -n '1p')"
    DB_PORT="$(printf '%s\n' "$parsed" | sed -n '2p')"
    DB_NAME="$(printf '%s\n' "$parsed" | sed -n '3p')"
  fi

  if [[ -z "$DB_NAME" ]]; then
    log_error "DATABASE_NAME (or DB_URL database part) cannot be empty."
    exit 1
  fi

  log_info "Target database: $DB_NAME"
  log_info "Host: $DB_HOST:$DB_PORT | User: $DB_USERNAME"

  local psql_runner="local"
  if [[ "$DB_HOST" == "postgres" ]] && can_use_compose_postgres; then
    psql_runner="compose"
    log_info "Detected Docker Compose postgres service, using docker compose exec mode."
  fi

  if [[ "$psql_runner" == "local" ]] && ! command -v psql >/dev/null 2>&1; then
    log_error "psql not found. Install PostgreSQL client or run with Docker Compose postgres service."
    exit 1
  fi

  local db_name_literal
  local db_name_identifier
  db_name_literal="$(escape_sql_literal "$DB_NAME")"
  db_name_identifier="$(escape_sql_identifier "$DB_NAME")"

  local exists_sql
  exists_sql="SELECT 1 FROM pg_database WHERE datname = '$db_name_literal';"

  local exists_result
  exists_result="$(run_sql "$psql_runner" "$exists_sql" || true)"

  if [[ "$exists_result" == "1" ]]; then
    log_success "Database '$DB_NAME' already exists. Nothing to do."
    exit 0
  fi

  log_info "Database '$DB_NAME' does not exist. Creating..."
  local create_sql
  create_sql="CREATE DATABASE \"$db_name_identifier\";"

  run_sql "$psql_runner" "$create_sql"

  log_success "Database '$DB_NAME' created successfully."
}

main "$@"
