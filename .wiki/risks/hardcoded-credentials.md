---
title: "Hardcoded Credentials"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "src/main/resources/application-local.yaml"
updated_at: "2026-06-11"
---

# Hardcoded Credentials

## The Problem

Sensitive credentials are stored in `application-local.yaml`, which is gitignored but may be accidentally committed.

## What's Stored

| Credential | Location | Risk Level |
|-------------|----------|------------|
| Alpha Vantage API key | `app.alphavantage.api-key` | Medium — public API key |
| Langfuse credentials (base64-encoded) | `application-local.yaml` | High — observability credentials |

## Why It's a Risk

1. **Accidental commit** — if `application-local.yaml` is added to git, credentials are exposed
2. **Base64 is not encryption** — Langfuse credentials stored as base64 can be decoded by anyone
3. **Shared machines** — if the project directory is shared, other users can read the file

## Mitigations

- `application-local.yaml` is in `.gitignore`
- API keys should be provided via environment variables instead

## Recommended Fix

1. Move all credentials to environment variables
2. Use a `.env` file (gitignored) for local development
3. Remove base64-encoded credentials — use proper secret management
4. Consider using Spring Boot's encrypted properties (Jasypt) for shared environments
