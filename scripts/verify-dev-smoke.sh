#!/usr/bin/env bash
# Local dev smoke test for viken-chaos-monkey (dev profile, file persistence, security off).
# Prerequisites: mvn spring-boot:run -Dspring-boot.run.profiles=dev
#
# Usage: ./scripts/verify-dev-smoke.sh [base-url]
# Default base-url: http://localhost:20000
set -euo pipefail

BASE_URL="${1:-http://localhost:20000}"
BASE_URL="${BASE_URL%/}"

pass=0
fail=0

check() {
  local name="$1"
  local method="$2"
  local path="$3"
  local expected_code="${4:-200}"
  local url="${BASE_URL}${path}"
  local code

  code=$(curl -s -o /tmp/chaos-smoke-body.json -w "%{http_code}" -X "${method}" "${url}" \
    -H "Content-Type: application/json" \
    ${5:+-d "$5"})

  if [[ "${code}" == "${expected_code}" ]]; then
    echo "PASS  ${name} (${code})"
    pass=$((pass + 1))
  else
    echo "FAIL  ${name} (expected ${expected_code}, got ${code})"
    head -c 400 /tmp/chaos-smoke-body.json 2>/dev/null || true
    echo ""
    fail=$((fail + 1))
  fi
}

echo "Chaos Monkey dev smoke — ${BASE_URL}"
echo ""

check "actuator health" GET "/actuator/health"
check "chaos status" GET "/api/chaos/status"
check "list experiments" GET "/api/chaos/experiments"
check "kill switch disable" POST "/api/chaos/emergency/disable"
check "kill switch enable" POST "/api/chaos/emergency/enable"

echo ""
echo "Results: ${pass} passed, ${fail} failed"

if [[ "${fail}" -gt 0 ]]; then
  exit 1
fi
