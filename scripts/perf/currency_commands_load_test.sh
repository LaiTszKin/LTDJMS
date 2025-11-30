#!/bin/bash
#
# Currency Commands Load Test Script
#
# This script runs the performance tests for the currency slash commands.
# It verifies SC-001 requirements:
# - p95 latency <= 1 second
# - Error rate < 1%
# - 95% of commands complete within 3 seconds
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "======================================"
echo "Currency Commands Load Test"
echo "======================================"
echo ""
echo "Project root: $PROJECT_ROOT"
echo ""

# Check if PostgreSQL is running
if ! docker compose -f "$PROJECT_ROOT/docker-compose.yml" ps postgres 2>/dev/null | grep -q "running"; then
    echo "Starting PostgreSQL..."
    docker compose -f "$PROJECT_ROOT/docker-compose.yml" up -d postgres
    echo "Waiting for PostgreSQL to be ready..."
    sleep 5
fi

# Run the performance tests
echo ""
echo "Running performance tests..."
echo ""

cd "$PROJECT_ROOT"
mvn test -Dtest="SlashCommandPerformanceTest" -q 2>&1 | tee /tmp/perf_test_results.txt

# Check results
if grep -q "BUILD SUCCESS" /tmp/perf_test_results.txt; then
    echo ""
    echo "======================================"
    echo "✅ Performance tests PASSED"
    echo "======================================"
    echo ""
    echo "SC-001 Requirements Met:"
    echo "  - p95 latency <= 1000ms"
    echo "  - Error rate < 1%"
    echo "  - 95% of commands complete within 3 seconds"
else
    echo ""
    echo "======================================"
    echo "❌ Performance tests FAILED"
    echo "======================================"
    exit 1
fi
