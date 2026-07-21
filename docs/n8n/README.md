# Smart Hiring: free local AI and n8n

> For the AWS Hong Kong deployment with DeepSeek and credential-protected webhooks, follow [the Lightsail deployment guide](../deployment/AWS_LIGHTSAIL_HONG_KONG.md). The original workflows below remain the local Ollama variants.

This setup uses three local services:

1. React calls the Spring Boot API.
2. Spring calls n8n webhooks.
3. n8n calls local Ollama for matching or Gmail for important notifications.

The application still works when n8n, Ollama, or Gmail is unavailable. Matching falls back to the built-in score, notifications stay in the database, and failed emails are retried up to three times.

## 1. Install Ollama on Windows

Download and install Ollama from <https://ollama.com/download/windows>. It runs in the background and exposes its API on `http://localhost:11434`.

Open PowerShell and download the model:

```powershell
ollama pull llama3.2:3b
ollama run llama3.2:3b
```

Enter a short test prompt, then use `/bye`. If the 3B model is too slow, use `llama3.2:1b` and set `OLLAMA_MODEL=llama3.2:1b`.

API check:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:11434/api/generate `
  -ContentType "application/json" `
  -Body '{"model":"llama3.2:3b","prompt":"Return {\"ok\":true} as JSON","stream":false,"format":"json"}'
```

## 2. Start free n8n Community Edition

Use Node.js 22.22 or newer (while remaining within n8n's supported Node range). Save a strong webhook secret in the project `.env` file first. Then load that exact value into n8n directly from the file, avoiding a copy/paste mismatch. n8n 2.x blocks Code nodes from reading environment variables by default, so this local workflow also requires environment access to be enabled:

```powershell
$projectEnv='C:\Users\Raza Ahmed\Desktop\Smart_Hiring\.env'
$secretLine=Get-Content $projectEnv | Where-Object { $_ -match '^N8N_WEBHOOK_SECRET=' } | Select-Object -Last 1
$env:N8N_WEBHOOK_SECRET=($secretLine -split '=',2)[1].Trim()
$env:N8N_BLOCK_ENV_ACCESS_IN_NODE='false'
$env:N8N_WEBHOOK_SECRET.Length
npx.cmd n8n@2.26.8
```

The length check must print `64` before n8n starts. Open <http://localhost:5678> and create the local owner account. n8n stores its local data under your Windows user profile, so workflows survive restarts.

The exact same `N8N_WEBHOOK_SECRET` must be loaded by Spring. Never commit it. Allowing environment access applies to every Code node in this local n8n instance, so import only workflows you trust.

## 3. Import and activate workflows

In n8n, use **Workflows → Import from File** and import:

- `docs/n8n/workflows/worker-shift-match.json`
- `docs/n8n/workflows/manager-applicant-match.json`
- `docs/n8n/workflows/notification-email.json`

Open each workflow and verify the Webhook path. Run a manual test before selecting **Publish** or **Active**. Spring uses the production `/webhook/...` URLs, not n8n's temporary `/webhook-test/...` URLs.

The matching workflows call `http://localhost:11434/api/generate`. This is correct when n8n runs through `npx` on Windows. If n8n later runs in Docker, change the URL to `http://host.docker.internal:11434/api/generate`.

## 4. Connect Gmail securely

The exported workflow contains no Gmail credentials.

For self-hosted n8n:

1. Create a Google Cloud project.
2. Enable the Gmail API.
3. Configure the OAuth consent screen.
4. Add the sending Google account as a test user while the app is in testing mode.
5. Create an OAuth client of type **Web application**.
6. Copy the OAuth Redirect URL shown by the n8n Gmail credential into Google's authorized redirect URIs. For local n8n it normally resembles `http://localhost:5678/rest/oauth2-credential/callback`.
7. Enter the Google Client ID and Client Secret in n8n and choose **Sign in with Google**.
8. Open the **Send Gmail** node in the notification workflow and select that credential.

Do not put the Google client secret, refresh token, or Gmail credential in `.env` or a workflow export.

## 5. Configure Smart Hiring

Add these values to the project-root `.env`:

```properties
MATCHING_PROVIDER=n8n
N8N_MATCHING_ENABLED=true
N8N_WORKER_MATCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/worker-shift-match
N8N_MANAGER_MATCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/manager-applicant-match
N8N_NOTIFICATION_ENABLED=true
N8N_NOTIFICATION_WEBHOOK_URL=http://localhost:5678/webhook/smart-hiring/notification-email
N8N_WEBHOOK_SECRET=replace-with-the-same-long-random-secret
APP_PUBLIC_URL=http://localhost:3000
OLLAMA_MODEL=llama3.2:3b
```

Then start Spring:

```powershell
.\mvnw.cmd spring-boot:run
```

Start React in another PowerShell window:

```powershell
Set-Location staffmatch-frontend
npm.cmd start
```

Flyway automatically creates the `notifications` table on backend startup.

## 6. Verify the complete flow

1. Log in as a worker and open **AI Job Match**. A working n8n/Ollama result shows `AI via n8n + Ollama`.
2. Post a shift as a manager. The API returns immediately while active workers are scored in the background.
3. A worker with a genuine AI score of 80 or higher receives one in-app and Gmail job alert.
4. Apply as a worker. The manager receives an in-app notification and email.
5. Accept or reject the application. Affected workers receive notifications.
6. Start, complete, and pay the shift. The assigned worker receives lifecycle and payment notifications.
7. Chat messages and ratings appear in-app only.
8. Use the header bell for recent items or open `/notifications` for history.

## Notification email policy

Email is enabled for AI job alerts, applications, shift lifecycle changes, payments, issue status changes, and account moderation. Chat and rating notifications are intentionally in-app only.

## Troubleshooting

- `source: FALLBACK`: confirm n8n is active, Ollama is running, the model is downloaded, and both webhook secrets match.
- n8n returns 404: publish/activate the workflow and use `/webhook/`, not `/webhook-test/`.
- Gmail node is disconnected: repeat Google OAuth and select the credential in **Send Gmail**.
- Gmail OAuth stops after seven days: Google OAuth apps in external testing mode can require reconnection; publish the consent app or reconnect for demonstrations.
- Notifications appear but emails do not: confirm `N8N_NOTIFICATION_ENABLED=true`, inspect the n8n execution, and check the notification's email status in the database.
- Hosted n8n cannot call your PC's Ollama: deploy both where they can reach each other, or keep the entire setup local. Do not expose Ollama directly to the public internet.

## Hosted DeepSeek workflow variants

The three files beginning with `hosted-` are production variants. They deliberately contain no credential references or secrets, so imported credential-dependent nodes remain incomplete until credentials are selected.

- Use `Smart Hiring Webhook Secret` Header Auth on every hosted Webhook node.
- Use `DeepSeek Authorization` Header Auth on each hosted **Ask DeepSeek** node.
- Use the hosted Gmail OAuth credential on **Send Gmail**.
- Keep `N8N_BLOCK_ENV_ACCESS_IN_NODE=true` on AWS.
- Publish only the hosted workflows in production. Hosted and local matching workflows use the same paths and must not be active in the same n8n instance.
