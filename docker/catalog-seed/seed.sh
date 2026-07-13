#!/bin/sh
# Loads all FHIR resources (OperationDefinition, MessageDefinition,
# GraphDefinition) from the catalog/ directory into the catalog server.

set -e

CATALOG_URL="${CATALOG_URL:-http://catalog-server:8080/fhir}"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "Waiting for catalog server: ${CATALOG_URL}/metadata"
for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "${CATALOG_URL}/metadata" > /dev/null 2>&1; then
        echo "Catalog server reachable."
        break
    fi
    if [ "$i" -eq "$MAX_RETRIES" ]; then
        echo "Catalog server not reachable after ${MAX_RETRIES} attempts."
        exit 1
    fi
    echo "  Attempt $i/$MAX_RETRIES ..."
    sleep $RETRY_INTERVAL
done

LOADED=0
FAILED=0

for dir in OperationDefinition MessageDefinition GraphDefinition; do
    if [ -d "/catalog/${dir}" ]; then
        echo ""
        echo "=== ${dir} ==="
        for f in /catalog/${dir}/*.json; do
            [ -f "$f" ] || continue
            NAME=$(basename "$f")
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                -X POST "${CATALOG_URL}/${dir}" \
                -H "Content-Type: application/fhir+json" \
                -d @"$f")

            if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
                echo "  ✓ ${NAME} (HTTP ${HTTP_CODE})"
                LOADED=$((LOADED + 1))
            else
                echo "  ✗ ${NAME} (HTTP ${HTTP_CODE})"
                FAILED=$((FAILED + 1))
            fi
        done
    fi
done

echo ""
echo "Seed complete: ${LOADED} loaded, ${FAILED} failed."
[ "$FAILED" -eq 0 ] || exit 1
