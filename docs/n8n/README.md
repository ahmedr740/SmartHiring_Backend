# n8n AI Matching (optional)

Use this path only if you set `MATCHING_PROVIDER=n8n`. The default setup calls Ollama directly from Spring — no n8n required.

## Prerequisites

- Ollama running locally with a model pulled (for example `llama3.2:3b`)
- n8n running on `http://localhost:5678`

## Environment variables

```bash
export MATCHING_PROVIDER=n8n
export N8N_MATCHING_ENABLED=true
export N8N_WORKER_MATCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/worker-shift-match
export N8N_MANAGER_MATCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/manager-applicant-match
export N8N_WEBHOOK_SECRET=local-dev-secret
```

## Worker shift match workflow

Create a workflow with:

1. **Webhook** — POST path `staffmatch/worker-shift-match`, authentication optional
2. **IF** — check header `X-StaffMatch-Webhook-Secret` equals your secret (skip if empty locally)
3. **HTTP Request** — POST to `http://host.docker.internal:11434/api/generate` (or `http://localhost:11434/api/generate` if n8n is not in Docker)

   Body (JSON):

   ```json
   {
     "model": "llama3.2:3b",
     "prompt": "{{ $json.body.systemPrompt }}\n\n{{ $json.body.userPrompt }}\n\nReturn JSON only with targetId, aiScore, label, explanation, strengths, risks, recommendedAction.",
     "stream": false,
     "format": "json"
   }
   ```

4. **Code** — parse Ollama `response` text as JSON, set `source: "N8N_OLLAMA"`, copy `targetId` and `fallbackScore` from the webhook body if missing
5. **Respond to Webhook** — return the normalized JSON object

## Manager applicant match workflow

Same as above, but webhook path `staffmatch/manager-applicant-match`.

Spring sends `matchType` in the webhook body (`WORKER_SHIFT` or `MANAGER_APPLICANT`) so you can branch in n8n if needed.

## Expected response shape

```json
{
  "targetId": 10,
  "aiScore": 85,
  "fallbackScore": 72,
  "label": "Good match",
  "explanation": "Skills and location align with the shift.",
  "strengths": ["Waiter experience", "Nearby location"],
  "risks": ["Confirm availability"],
  "recommendedAction": "Invite to interview.",
  "source": "N8N_OLLAMA"
}
```

If the webhook fails or times out, Spring uses built-in fallback scoring automatically.
