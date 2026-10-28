# Content Moderation Dashboard

Spring Boot + JWT + MySQL + React (Vite, Recharts).

Report-to-resolution workflow: users flag posts; moderators approve, remove or warn.
Roles: USER, MODERATOR, ADMIN. Audit log of every action, keyword-flagged reports are
prioritised in the review queue, admin analytics page with filtering and pagination.

## Run (GitHub Codespaces or local)

```bash
# 1. database
docker compose up -d          # wait ~20 seconds

# 2. backend (terminal 1)
cd backend && mvn spring-boot:run

# 3. frontend (terminal 2)
cd frontend && npm install && npm run dev
```

Open port 5173. Demo logins: admin/admin123, mod/mod123, alice/alice123, bob/bob123.

## API summary
- POST /api/auth/register, /api/auth/login
- GET/POST /api/posts, POST /api/posts/{id}/report
- GET /api/moderation/reports?status=&category=&page=&size=
- POST /api/moderation/reports/{id}/resolve  body {"action":"APPROVE|REMOVE|WARN"}
- GET /api/moderation/audit
- GET /api/admin/analytics (ADMIN only)
