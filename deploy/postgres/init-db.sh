#!/bin/bash
set -euo pipefail

create_role() {
  local role=$1
  local password=$2
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    DO
    $$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${role}') THEN
        CREATE ROLE ${role} WITH LOGIN PASSWORD '${password}';
      END IF;
    END
    $$;
EOSQL
}

create_database() {
  local db=$1
  local owner=$2
  if ! psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --tuples-only --no-align -c "SELECT 1 FROM pg_database WHERE datname = '${db}'" | grep -q 1; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -c "CREATE DATABASE ${db} OWNER ${owner};"
  fi
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -c "GRANT ALL PRIVILEGES ON DATABASE ${db} TO ${owner};"
}

create_role "${ACCOUNTS_DB_USER}" "${ACCOUNTS_DB_PASSWORD}"
create_database "${ACCOUNTS_DB_NAME}" "${ACCOUNTS_DB_USER}"

create_role "${SUBSCRIPTIONS_DB_USER}" "${SUBSCRIPTIONS_DB_PASSWORD}"
create_database "${SUBSCRIPTIONS_DB_NAME}" "${SUBSCRIPTIONS_DB_USER}"

create_role "${PAYMENTS_DB_USER}" "${PAYMENTS_DB_PASSWORD}"
create_database "${PAYMENTS_DB_NAME}" "${PAYMENTS_DB_USER}"

create_role "${REPORTER_DB_USER}" "${REPORTER_DB_PASSWORD}"
create_database "${REPORTER_DB_NAME}" "${REPORTER_DB_USER}"

create_role "${NOTIFICATIONS_DB_USER}" "${NOTIFICATIONS_DB_PASSWORD}"
create_database "${NOTIFICATIONS_DB_NAME}" "${NOTIFICATIONS_DB_USER}"
