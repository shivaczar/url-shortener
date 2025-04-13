# 🔐 URL Shortener v2.0 Migration Guide

## ⚠️ Major Breaking Change: Authentication Required

Starting from **v2.0.0**, all API endpoints now require a valid `X-API-KEY` header for access. This is part of our new **API Key–based Authentication** system to secure and monitor API usage.

---

## 🗽 What's Changing?

### 🔴 Before (No Auth Required)

You could call the shorten endpoint like this:

```bash
curl -X POST http://localhost:8080/api/urls/shorten \
     -H "Content-Type: application/json" \
     -d '{"url": "https://example.com"}'
```

---

### 🟢 After (Auth Required via API Key)

Now, all secured endpoints require an `X-API-KEY` header:

```bash
curl -X POST http://localhost:8080/api/urls/shorten \
     -H "Content-Type: application/json" \
     -H "X-API-KEY: YOUR_API_KEY" \
     -d '{"url": "https://example.com"}'
```

---

## ✅ How to Migrate

### 1. **Get Your API Key**

- Register or log in to your account.
- Your unique API key will be generated and available in your dashboard or via email.

> Example API Key: `API_KEY_12345`

---

### 2. **Update All API Calls**

Ensure every request includes the `X-API-KEY` header. This includes:

- Shorten URL (`/api/urls/shorten`)
- Retrieve All URLs (`/api/urls/all`)
- Bulk Shorten (`/api/urls/shorten/batch`)
- Top 10 Clicked / Shortened URLs
- Delete, Recent, and Custom URLs

---

## 📌 Notes

- Unauthorized requests will now return:
  ```json
  {
    "error": "Invalid API Key"
  }
  ```
  with status `400` or `401`.

- Health check endpoint (`/health`) **does not** require authentication.

---

## 🛠️ Future Plans

We're working on adding:

- API key rotation
- Usage quotas
- Rate limiting per API key

