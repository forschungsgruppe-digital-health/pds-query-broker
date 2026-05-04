#!/bin/sh
# Lädt alle FHIR-Ressourcen (OperationDefinition, MessageDefinition,
# GraphDefinition) aus dem catalog/-Verzeichnis in den Katalog-Server.

set -e

CATALOG_URL="${CATALOG_URL:-http://catalog-server:8080/fhir}"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "Warte auf Katalog-Server: ${CATALOG_URL}/metadata"
for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "${CATALOG_URL}/metadata" > /dev/null 2>&1; then
        echo "Katalog-Server erreichbar."
        break
    fi
    if [ "$i" -eq "$MAX_RETRIES" ]; then
        echo "Katalog-Server nicht erreichbar nach ${MAX_RETRIES} Versuchen."
        exit 1
    fi
    echo "  Versuch $i/$MAX_RETRIES ..."
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
echo "Seed abgeschlossen: ${LOADED} geladen, ${FAILED} fehlgeschlagen."
[ "$FAILED" -eq 0 ] || exit 1
