#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --set=staffmatch_password="$STAFFMATCH_DB_PASSWORD" \
    --set=n8n_password="$N8N_DB_PASSWORD" <<'EOSQL'
CREATE ROLE staffmatch_app LOGIN PASSWORD :'staffmatch_password';
CREATE DATABASE staffmatch_prod OWNER staffmatch_app;
REVOKE ALL ON DATABASE staffmatch_prod FROM PUBLIC;
GRANT CONNECT ON DATABASE staffmatch_prod TO staffmatch_app;

CREATE ROLE n8n_app LOGIN PASSWORD :'n8n_password';
CREATE DATABASE n8n_prod OWNER n8n_app;
REVOKE ALL ON DATABASE n8n_prod FROM PUBLIC;
GRANT CONNECT ON DATABASE n8n_prod TO n8n_app;
EOSQL
