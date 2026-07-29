#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.production}"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_DIR/docker-compose.prod.yml}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing production environment file: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing production compose file: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ "${1:-}" != "--apply" ]]; then
  echo "This script will add these display-only accounts to the production admin page:"
  echo ""
  echo "Pending managers: Grace Chan, Jason Wong, Michelle Lau, Kevin Ho, Samantha Cheung"
  echo "Active workers:   Adrian Lee, Chloe Lam, Ethan Ng, Fiona Yip, Ryan Cheng"
  echo ""
  echo "No changes were made. Run it with --apply to insert the accounts:"
  echo "  bash deploy/scripts/seed-admin-showcase-users.sh --apply"
  exit 0
fi

compose=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

if ! "${compose[@]}" ps --status running postgres | grep -q postgres; then
  echo "The production postgres container is not running." >&2
  echo "Start the production stack before running this script." >&2
  exit 1
fi

echo "Adding five pending managers and five active workers..."

"${compose[@]}" exec -T postgres sh -lc \
  'PGPASSWORD="$POSTGRES_PASSWORD" psql --set ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname staffmatch_prod' <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

WITH locked_password AS (
    SELECT crypt(encode(gen_random_bytes(48), 'hex'), gen_salt('bf', 12)) AS password_hash
), manager_rows(name, email, restaurant_name, phone, location, row_order) AS (
    VALUES
        ('Grace Chan', 'grace.chan@pearlharbour.example', 'Pearl Harbour Kitchen', '+852 5555 0101', 'Central', 1),
        ('Jason Wong', 'jason.wong@kowloongarden.example', 'Kowloon Garden Bistro', '+852 5555 0102', 'Tsim Sha Tsui', 2),
        ('Michelle Lau', 'michelle.lau@victoriatable.example', 'Victoria Table', '+852 5555 0103', 'Causeway Bay', 3),
        ('Kevin Ho', 'kevin.ho@harbourlane.example', 'Harbour Lane Restaurant', '+852 5555 0104', 'Wan Chai', 4),
        ('Samantha Cheung', 'samantha.cheung@jadepantry.example', 'Jade Pantry', '+852 5555 0105', 'Mong Kok', 5)
)
INSERT INTO users (
    name, email, password, role, status, skills, rating, rating_count,
    completed_shifts_count, restaurant_name, phone, location, availability,
    experience, cv_file_name, cv_text, cv_uploaded_at, created_at
)
SELECT
    manager_rows.name,
    manager_rows.email,
    locked_password.password_hash,
    'MANAGER',
    'PENDING',
    NULL,
    0,
    0,
    0,
    manager_rows.restaurant_name,
    manager_rows.phone,
    manager_rows.location,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP - (manager_rows.row_order * INTERVAL '1 minute')
FROM manager_rows
CROSS JOIN locked_password
ON CONFLICT (email) DO NOTHING;

WITH locked_password AS (
    SELECT crypt(encode(gen_random_bytes(48), 'hex'), gen_salt('bf', 12)) AS password_hash
), worker_rows(name, email, skills, location, availability, experience, rating, rating_count, completed_shifts_count, row_order) AS (
    VALUES
        ('Adrian Lee', 'adrian.lee@worker.example', 'waiter, floor service, banquet service', 'Central', 'Weekday evenings and weekends', 'Two years of restaurant floor service, including busy dinner and banquet shifts.', 4.8::double precision, 12, 18, 6),
        ('Chloe Lam', 'chloe.lam@worker.example', 'barista, cashier, customer service', 'Causeway Bay', 'Mornings and weekend afternoons', 'Experienced in espresso preparation, counter service, cash handling, and opening routines.', 4.7::double precision, 9, 14, 7),
        ('Ethan Ng', 'ethan.ng@worker.example', 'kitchen helper, food preparation, dishwasher', 'Tsim Sha Tsui', 'Evenings from 17:00 and all day Sunday', 'Kitchen support experience covering food preparation, cleaning, dishwashing, and closing duties.', 4.6::double precision, 8, 11, 8),
        ('Fiona Yip', 'fiona.yip@worker.example', 'waiter, hostess, private dining', 'Wan Chai', 'Friday evenings and weekends', 'Guest-facing experience in table service, private events, reservations, and dining-room setup.', 4.9::double precision, 15, 22, 9),
        ('Ryan Cheng', 'ryan.cheng@worker.example', 'cashier, server, event crew', 'Mong Kok', 'Flexible weekday schedule and weekends', 'Hospitality and event experience covering POS operation, guest assistance, service, and venue setup.', 4.5::double precision, 7, 10, 10)
)
INSERT INTO users (
    name, email, password, role, status, skills, rating, rating_count,
    completed_shifts_count, restaurant_name, phone, location, availability,
    experience, cv_file_name, cv_text, cv_uploaded_at, created_at
)
SELECT
    worker_rows.name,
    worker_rows.email,
    locked_password.password_hash,
    'WORKER',
    'ACTIVE',
    worker_rows.skills,
    worker_rows.rating,
    worker_rows.rating_count,
    worker_rows.completed_shifts_count,
    NULL,
    NULL,
    worker_rows.location,
    worker_rows.availability,
    worker_rows.experience,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP - (worker_rows.row_order * INTERVAL '1 minute')
FROM worker_rows
CROSS JOIN locked_password
ON CONFLICT (email) DO NOTHING;

COMMIT;

SELECT name, email, role, status
FROM users
WHERE email IN (
    'grace.chan@pearlharbour.example',
    'jason.wong@kowloongarden.example',
    'michelle.lau@victoriatable.example',
    'kevin.ho@harbourlane.example',
    'samantha.cheung@jadepantry.example',
    'adrian.lee@worker.example',
    'chloe.lam@worker.example',
    'ethan.ng@worker.example',
    'fiona.yip@worker.example',
    'ryan.cheng@worker.example'
)
ORDER BY role, created_at DESC;
SQL

echo ""
echo "Finished. Refresh the admin page:"
echo "  - Manager Requests shows five pending managers."
echo "  - Workers shows five active workers."
echo "The script is safe to rerun; existing email addresses are skipped."
