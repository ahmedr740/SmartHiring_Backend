#!/bin/sh
set -eu

mkdir -p /backups

while true; do
    timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
    pg_dump --host=postgres --username="$POSTGRES_USER" --format=custom --file="/backups/staffmatch_prod-${timestamp}.dump" staffmatch_prod
    pg_dump --host=postgres --username="$POSTGRES_USER" --format=custom --file="/backups/n8n_prod-${timestamp}.dump" n8n_prod
    find /backups -type f -name '*.dump' -mtime +7 -delete
    sleep 86400
done
