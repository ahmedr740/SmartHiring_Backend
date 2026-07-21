# AWS Lightsail Hong Kong deployment

This guide deploys Smart Hiring to one 4 GB Ubuntu Lightsail instance in Asia Pacific (Hong Kong), `ap-east-1`. React, Spring Boot, PostgreSQL, n8n, backups, and Caddy run as Docker containers on the instance. DeepSeek is the only metered external application dependency.

## Cost guardrails

- Select the **4 GB Linux instance with public IPv4**. Do not create a Lightsail database, container service, load balancer, CDN distribution, block disk, or snapshot unless the additional charge is intentional.
- Create an AWS monthly cost budget for **USD 60** with email alerts at USD 30, USD 45, and USD 60.
- An attached Lightsail static IP is included; release it when the instance is deleted.
- Route 53 domain registration is annual and non-refundable. Turn off domain auto-renewal if the domain is not needed after the first year.
- Top up only a small DeepSeek balance, initially USD 5. Matching falls back safely when that balance is exhausted.

## 1. Test before AWS

Run the backend and frontend checks from the repository root:

```powershell
.\mvnw.cmd test
Set-Location .\staffmatch-frontend
npm test -- --watchAll=false
npm run build
```

Confirm that Git does not contain `.env`, `.env.production`, API keys, database passwords, JWT secrets, or n8n credentials.

The frontend is a Git submodule. Commit and push frontend changes in `staffmatch-frontend` first, then commit the updated submodule pointer and deployment files in the backend repository. Both repositories must be accessible to the Lightsail deploy key.

## 2. Enable Hong Kong and create the server

1. Open the Lightsail console and go to **Account > Profile**.
2. Under opt-in Regions, enable **Asia Pacific (Hong Kong)**.
3. Create an instance in `ap-east-1` using **Linux/Unix > OS Only > Ubuntu 24.04 LTS**.
4. Select the **4 GB, 2 vCPU, public IPv4** plan.
5. Name it `smart-hiring-demo`.
6. Create a static IP in Hong Kong and attach it immediately.
7. Download the Hong Kong SSH key or upload an existing public key.
8. In the instance firewall, allow TCP 80 and 443 from anywhere. Restrict TCP 22 to the administrator's current public IP. Remove any public rules for 5432, 5678, or 8080.

Create the USD 60 AWS Budget before starting the instance. Budget alerts are notifications, not an instant billing cutoff, so the Billing dashboard must still be reviewed.

## 3. Register and point the domain

Register the chosen domain through Route 53. In its hosted zone, create `A` records pointing to the attached Lightsail static IP for:

- The root domain, such as `example.com`.
- `www.example.com`.
- `api.example.com`.
- `automation.example.com`.

Wait until all four names resolve to the static IP. Caddy cannot obtain HTTPS certificates until DNS resolves and ports 80/443 reach the instance.

## 4. Prepare Ubuntu

Connect through SSH and clone the repository with its frontend submodule:

```bash
git clone --recurse-submodules https://github.com/ahmedr740/SmartHiring_Backend.git Smart_Hiring
```

If the repository was already cloned, run `git submodule update --init --recursive`. Then prepare the server:

```bash
cd Smart_Hiring
sudo sh deploy/scripts/setup-lightsail.sh
```

Sign out and reconnect so Docker group membership applies. Verify with:

```bash
docker version
docker compose version
swapon --show
```

The setup script installs Docker and creates a 2 GB swap file. It does not create AWS resources or change Lightsail firewall rules.

## 5. Create production secrets

Copy the tracked template without committing the result:

```bash
cp .env.production.example .env.production
chmod 600 .env.production
```

Generate a different value for every application secret:

```bash
openssl rand -hex 32
```

Generate the Caddy password hash:

```bash
docker run --rm caddy:2.10-alpine caddy hash-password --plaintext 'CHOOSE_A_STRONG_PASSWORD'
```

Set `DOMAIN` to the registered root domain and `TLS_EMAIL` to the certificate contact email. Put the bcrypt hash in `N8N_PROXY_PASSWORD_HASH` inside single quotes so its dollar signs remain literal.

For a new database, set `ADMIN_SEED_ENABLED=true` and provide a unique administrator email and strong temporary password before the first start. After the first successful administrator login, change `ADMIN_SEED_ENABLED=false` and recreate the backend container with `docker compose --env-file .env.production -f docker-compose.prod.yml up -d backend`.

Never put the DeepSeek API key in `.env.production`. It is added later as an encrypted n8n credential.

## 6. Start the stack

Validate the expanded configuration and build the two application images sequentially to reduce peak memory:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml build backend
docker compose --env-file .env.production -f docker-compose.prod.yml build frontend
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

Follow startup logs without enabling paid CloudWatch logging:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 backend n8n caddy
```

Check:

- `https://example.com`
- `https://api.example.com/api/health`
- `https://automation.example.com`

The first PostgreSQL start creates `staffmatch_prod` and `n8n_prod` with separate application roles. Flyway creates the Smart Hiring schema when the backend starts. Database and n8n ports stay on the private Docker network.

The production frontend is built with demo quick-access credentials hidden and source maps disabled, so the local fixed demo credentials are not published. If demonstration accounts are needed, create them with strong temporary passwords.

## 7. Configure hosted n8n

1. Open `https://automation.example.com` and pass the Caddy login.
2. Create the n8n owner account and enable two-factor authentication.
3. Create a **Header Auth** credential named `Smart Hiring Webhook Secret`:
   - Header name: `X-StaffMatch-Webhook-Secret`
   - Header value: the exact `N8N_WEBHOOK_SECRET` value from `.env.production`
4. Create another **Header Auth** credential named `DeepSeek Authorization`:
   - Header name: `Authorization`
   - Header value: `Bearer YOUR_DEEPSEEK_API_KEY`
5. Import `hosted-worker-shift-match-deepseek.json` and `hosted-manager-applicant-match-deepseek.json` from `docs/n8n/workflows`.
6. On each Webhook node, select `Smart Hiring Webhook Secret`.
7. On each **Ask DeepSeek** node, select `DeepSeek Authorization`.
8. Save and publish both workflows.

The exports contain no API key, webhook secret, credential ID, execution data, or user information. Code nodes cannot read container environment variables. DeepSeek receives only the sanitized system and user prompts—not the response `targetId`.

For Gmail, import `hosted-notification-email.json`, select the webhook credential, create the Gmail OAuth credential with the hosted n8n callback URL, select it on **Send Gmail**, then publish the workflow. Change `N8N_NOTIFICATION_ENABLED=true` only after a manual email test succeeds and restart the backend:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml up -d backend
```

## 8. Acceptance test

1. Register and log in as worker and manager accounts.
2. Confirm the manager approval flow with the administrator.
3. Post a shift and open worker matches.
4. Confirm genuine hosted matches return `source: N8N_DEEPSEEK` and display **DeepSeek AI**.
5. Apply, accept/reject, start, complete, mock-pay, rate, chat, report an issue, and review notifications.
6. Stop n8n and verify matching returns `FALLBACK`, then restart n8n.
7. Restart the instance and confirm application data, n8n workflows, credentials, and HTTPS certificates persist.
8. Test with up to 10 concurrent browser sessions and check `docker stats` for memory pressure.

AI results are advisory. Do not treat an AI score as an automated employment decision.

## Backups and updates

The `backup` container creates daily custom-format dumps in `./backups` and removes dumps older than seven days. Download backups to the local computer before important demonstrations.

Restore into an empty database only after stopping the backend and n8n. Example:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml stop backend n8n backup
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres dropdb -U postgres --if-exists staffmatch_prod
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres createdb -U postgres -O staffmatch_app staffmatch_prod
cat backups/STAFFMATCH_BACKUP.dump | docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres pg_restore -U postgres -d staffmatch_prod --no-owner
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
```

Before an application update, download a backup, pull the reviewed commit, rebuild sequentially, and run `up -d` again. Never use floating n8n versions during the demonstration period.

## August 31 teardown

1. Download the latest `staffmatch_prod` and `n8n_prod` dumps.
2. Export all n8n workflows from the editor.
3. Record required screenshots and test evidence.
4. Revoke the DeepSeek API key.
5. Delete the Lightsail instance—not merely stop it.
6. Delete the static IP and any manually created snapshots or disks.
7. Delete the Route 53 hosted zone after the domain no longer needs DNS.
8. Disable domain auto-renewal if it should expire after one year.
9. Check AWS Billing and Lightsail in every enabled region until no chargeable resources remain.

Stopping a Lightsail instance does not end all charges. Deleting every listed resource is the teardown acceptance condition.
