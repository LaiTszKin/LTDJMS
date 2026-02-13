#!/usr/bin/env bash

set -euo pipefail

TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$TEST_ROOT/../.." && pwd)"
SCRIPT_NAME="create-db.sh"

PASS_COUNT=0
FAIL_COUNT=0

assert_contains() {
  local file="$1"
  local needle="$2"
  if ! grep -Fq -- "$needle" "$file"; then
    echo "[FAIL] Expected to find '$needle' in $file"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    return 1
  fi
}

assert_not_contains() {
  local file="$1"
  local needle="$2"
  if grep -Fq -- "$needle" "$file"; then
    echo "[FAIL] Expected NOT to find '$needle' in $file"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    return 1
  fi
}

setup_workspace() {
  local workspace
  workspace="$(mktemp -d)"
  mkdir -p "$workspace/scripts/db" "$workspace/mock-bin"
  cp "$PROJECT_ROOT/scripts/db/$SCRIPT_NAME" "$workspace/scripts/db/$SCRIPT_NAME"
  chmod +x "$workspace/scripts/db/$SCRIPT_NAME"

  cat > "$workspace/mock-bin/psql" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
log_file="${MOCK_LOG_FILE:?}"
printf 'psql %s\n' "$*" >> "$log_file"
sql=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -c)
      sql="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done
printf 'sql=%s\n' "$sql" >> "$log_file"

if [[ "$sql" == *"SELECT 1 FROM pg_database"* ]]; then
  if [[ "${MOCK_DB_EXISTS:-0}" == "1" ]]; then
    printf '1\n'
  fi
  exit 0
fi

if [[ "$sql" == *"CREATE DATABASE"* ]]; then
  printf 'CREATE_CALLED\n' >> "$log_file"
  exit 0
fi
EOF

  cat > "$workspace/mock-bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
log_file="${MOCK_LOG_FILE:?}"
printf 'docker %s\n' "$*" >> "$log_file"

if [[ "$1" == "compose" && "$2" == "ps" ]]; then
  if [[ "${MOCK_COMPOSE_RUNNING:-0}" == "1" ]]; then
    printf 'postgres\n'
  fi
  exit 0
fi

if [[ "$1" == "compose" && "$2" == "exec" ]]; then
  shift 2
  while [[ $# -gt 0 && "$1" != "psql" ]]; do
    shift
  done
  if [[ $# -eq 0 ]]; then
    exit 1
  fi
  "${MOCK_PSQL_BIN:?}" "$@"
  exit 0
fi

exit 1
EOF

  chmod +x "$workspace/mock-bin/psql" "$workspace/mock-bin/docker"
  printf '%s\n' "$workspace"
}

run_script() {
  local workspace="$1"
  local stdout_file="$2"
  local stderr_file="$3"
  local db_exists="$4"
  local compose_running="$5"
  local original_path="$PATH"

  PATH="$workspace/mock-bin:$original_path" \
    MOCK_LOG_FILE="$workspace/mock.log" \
    MOCK_DB_EXISTS="$db_exists" \
    MOCK_COMPOSE_RUNNING="$compose_running" \
    MOCK_PSQL_BIN="$workspace/mock-bin/psql" \
    bash "$workspace/scripts/db/$SCRIPT_NAME" \
    >"$stdout_file" 2>"$stderr_file"
}

test_missing_env_file() {
  local workspace
  workspace="$(setup_workspace)"
  local stdout_file="$workspace/stdout.log"
  local stderr_file="$workspace/stderr.log"

  set +e
  run_script "$workspace" "$stdout_file" "$stderr_file" 1 0
  local rc=$?
  set -e

  if [[ $rc -eq 0 ]]; then
    echo "[FAIL] test_missing_env_file expected non-zero exit code"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    rm -rf "$workspace"
    return 1
  fi
  assert_contains "$stderr_file" "File not found"
  PASS_COUNT=$((PASS_COUNT + 1))
  rm -rf "$workspace"
}

test_existing_db_no_create() {
  local workspace
  workspace="$(setup_workspace)"
  cat > "$workspace/.env" <<'EOF'
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=my_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
EOF

  local stdout_file="$workspace/stdout.log"
  local stderr_file="$workspace/stderr.log"
  run_script "$workspace" "$stdout_file" "$stderr_file" 1 0

  assert_contains "$stdout_file" "already exists"
  assert_contains "$workspace/mock.log" "SELECT 1 FROM pg_database"
  assert_not_contains "$workspace/mock.log" "CREATE DATABASE"
  PASS_COUNT=$((PASS_COUNT + 1))
  rm -rf "$workspace"
}

test_missing_db_create_with_inline_comment() {
  local workspace
  workspace="$(setup_workspace)"
  cat > "$workspace/.env" <<'EOF'
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=my_db # inline comment
DB_USERNAME=postgres
DB_PASSWORD=postgres
EOF

  local stdout_file="$workspace/stdout.log"
  local stderr_file="$workspace/stderr.log"
  run_script "$workspace" "$stdout_file" "$stderr_file" 0 0

  assert_contains "$workspace/mock.log" "CREATE DATABASE \"my_db\";"
  PASS_COUNT=$((PASS_COUNT + 1))
  rm -rf "$workspace"
}

test_db_url_overrides_database_fields() {
  local workspace
  workspace="$(setup_workspace)"
  cat > "$workspace/.env" <<'EOF'
DB_URL=jdbc:postgresql://dbhost:5544/url_db?sslmode=disable
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=ignored
DB_USERNAME=postgres
DB_PASSWORD=postgres
EOF

  local stdout_file="$workspace/stdout.log"
  local stderr_file="$workspace/stderr.log"
  run_script "$workspace" "$stdout_file" "$stderr_file" 1 0

  assert_contains "$workspace/mock.log" "-h dbhost"
  assert_contains "$workspace/mock.log" "-p 5544"
  assert_contains "$workspace/mock.log" "datname = 'url_db'"
  PASS_COUNT=$((PASS_COUNT + 1))
  rm -rf "$workspace"
}

test_postgres_host_uses_compose_exec() {
  local workspace
  workspace="$(setup_workspace)"
  cat > "$workspace/.env" <<'EOF'
DATABASE_HOST=postgres
DATABASE_PORT=5432
DATABASE_NAME=compose_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
EOF

  local stdout_file="$workspace/stdout.log"
  local stderr_file="$workspace/stderr.log"
  run_script "$workspace" "$stdout_file" "$stderr_file" 1 1

  assert_contains "$workspace/mock.log" "docker compose exec -T -e PGPASSWORD=postgres postgres psql -h localhost -p 5432"
  PASS_COUNT=$((PASS_COUNT + 1))
  rm -rf "$workspace"
}

main() {
  test_missing_env_file
  test_existing_db_no_create
  test_missing_db_create_with_inline_comment
  test_db_url_overrides_database_fields
  test_postgres_host_uses_compose_exec

  if [[ $FAIL_COUNT -gt 0 ]]; then
    echo "[RESULT] PASS=$PASS_COUNT FAIL=$FAIL_COUNT"
    exit 1
  fi

  echo "[RESULT] PASS=$PASS_COUNT FAIL=0"
}

main "$@"
