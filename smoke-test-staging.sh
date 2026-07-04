#!/usr/bin/env bash
# ==============================================================================
# STAGING DEPLOYMENT SMOKE-TEST SCRIPT (smoke-test-staging.sh)
# ==============================================================================
#
# Validates that the isolated staging environment has started correctly, Nginx
# reverse-proxy routing is functional, Actuator endpoints are restricted,
# Correlation-ID propagation is working, and the backend is not exposed.
#
# Usage:
#   ./smoke-test-staging.sh [STAGING_BASE_URL]
# Default base URL is http://localhost.
# ==============================================================================

set -eo pipefail

BASE_URL="${1:-http://localhost}"
BACKEND_INTERNAL_PORT=8080
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[0;33m'
COLOR_RESET='\033[0m'

echo "=============================================================================="
echo " Starting Staging Harness Smoke Tests against ${BASE_URL}"
echo "=============================================================================="

# 1. Wait for backend readiness via Nginx proxy
echo -n "Checking backend readiness via reverse proxy..."
READY=false
for i in {1..30}; do
    STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health/readiness" || true)
    if [ "$STATUS_CODE" -eq 200 ]; then
        HEALTH_BODY=$(curl -s "${BASE_URL}/actuator/health/readiness")
        if echo "$HEALTH_BODY" | grep -q '"status":"UP"'; then
            echo -e " [${COLOR_GREEN}OK${COLOR_RESET}]"
            READY=true
            break
        fi
    fi
    echo -n "."
    sleep 2
done

if [ "$READY" = "false" ]; then
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}]"
    echo "Staging backend did not report UP within 60 seconds."
    exit 1
fi

# 2. Verify health and info are publicly accessible
echo -n "Verifying /actuator/health is accessible..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health")
if [ "$STATUS" -eq 200 ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Status: $STATUS)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Status: $STATUS)"
    exit 1
fi

echo -n "Verifying /actuator/info is accessible..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/info")
if [ "$STATUS" -eq 200 ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Status: $STATUS)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Status: $STATUS)"
    exit 1
fi

# 3. Verify /actuator/prometheus is blocked publicly (Nginx should return 403)
echo -n "Verifying /actuator/prometheus is blocked publicly..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/prometheus")
if [ "$STATUS" -eq 403 ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Status: $STATUS - Blocked as intended)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Status: $STATUS - Access should be denied!)"
    exit 1
fi

# 4. Verify other Actuator paths are blocked publicly (Nginx should return 403)
echo -n "Verifying /actuator/env is blocked publicly..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/env")
if [ "$STATUS" -eq 403 ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Status: $STATUS - Blocked as intended)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Status: $STATUS - Access should be denied!)"
    exit 1
fi

# 5. Verify Correlation ID Response Header Behavior
echo -n "Verifying Correlation-ID generation..."
UUID_HEADER=$(curl -sI "${BASE_URL}/actuator/health" | grep -i "X-Correlation-ID" | tr -d '\r\n')
if [[ -n "$UUID_HEADER" ]]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] ($UUID_HEADER)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Header not found)"
    exit 1
fi

echo -n "Verifying Correlation-ID propagation of user-supplied trace ID..."
CUSTOM_TRACE_ID="smoke-test-trace-id-12345"
PROPAGATED_VAL=$(curl -sI -H "X-Correlation-ID: ${CUSTOM_TRACE_ID}" "${BASE_URL}/actuator/health" | grep -i "X-Correlation-ID" | awk '{print $2}' | tr -d '\r\n')
if [ "$PROPAGATED_VAL" = "$CUSTOM_TRACE_ID" ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Propagated trace: $PROPAGATED_VAL)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Expected $CUSTOM_TRACE_ID, got $PROPAGATED_VAL)"
    exit 1
fi

# 6. Verify /api/ routing works
echo -n "Verifying public catalog API routing (/api/products)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/products")
if [ "$STATUS" -eq 200 ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Status: $STATUS)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Status: $STATUS)"
    exit 1
fi

# 7. Verify direct backend exposure is blocked (port 8080 should not be reachable on host)
echo -n "Verifying direct backend port 8080 is blocked on host..."
# Extract hostname/IP from BASE_URL (defaults to localhost)
HOST_NAME=$(echo "$BASE_URL" | sed -e 's/[^/]*\/\/\([^@]*@\)\?\([^:/]*\).*/\2/')
STATUS_PORT=$(curl --max-time 3 -s -o /dev/null -w "%{http_code}" "http://${HOST_NAME}:${BACKEND_INTERNAL_PORT}/actuator/health" || echo "BLOCKED")
if [ "$STATUS_PORT" = "BLOCKED" ] || [ "$STATUS_PORT" -eq 000 ]; then
    echo -e " [${COLOR_GREEN}OK${COLOR_RESET}] (Port 8080 is unreachable from host as intended)"
else
    echo -e " [${COLOR_RED}FAILED${COLOR_RESET}] (Direct access to port 8080 returned HTTP status: $STATUS_PORT. Port exposure should be disabled!)"
    exit 1
fi

echo "=============================================================================="
echo -e "${COLOR_GREEN}All Staging Smoke Tests Passed Successfully!${COLOR_RESET}"
echo "=============================================================================="
exit 0
