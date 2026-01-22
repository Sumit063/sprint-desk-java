# SprintDesk UI API Contract (from frontend usage)

This document captures the API endpoints, request payloads, and response shapes used by the existing React UI in `frontend/`.

Base URL
- UI uses `VITE_API_URL` (fallback `http://localhost:4000`) from `frontend/src/lib/api.ts`.
- Axios uses `withCredentials: true` and attaches `Authorization: Bearer <accessToken>` when available.

Auth tokens
- Access token is stored in memory (Zustand store) via `setAccessToken`.
- Refresh token is expected to be stored in an HTTP-only cookie; UI calls `POST /api/auth/refresh` on 401.

WebSocket (current UI)
- Client: socket.io in `frontend/src/hooks/useWorkspaceSocket.ts`.
- Connects to `VITE_API_URL` and sends `{ auth: { token } }`.
- Emits: `join_workspace` with `workspaceId`, expects ACK `{ ok?: boolean, message?: string }`.
- Listens:
  - `issue_created`: `{ issueId?, title?, actorId? }`
  - `issue_updated`: `{ issueId?, actorId?, fields?: string[] }`
  - `comment_added`: `{ issueId?, actorId? }`
  - `notification_created`: `{ message?, notificationId? }`

---

## Auth

POST `/api/auth/register`
- Request: `{ name: string, email: string, password: string }`
- Response: `{ accessToken: string, user: { id, email, name, avatarUrl?, contact? } }`

POST `/api/auth/login`
- Request: `{ email: string, password: string }`
- Response: `{ accessToken: string, user: { id, email, name, avatarUrl?, contact? } }`

POST `/api/auth/refresh`
- Request: (no body, cookie-based refresh)
- Response: `{ accessToken: string, user: { id, email, name, avatarUrl?, contact? } }`

POST `/api/auth/logout`
- Request: (no body)
- Response: `{ ok: true }`

POST `/api/auth/google`
- Request: `{ credential: string }`
- Response: `{ accessToken: string, user: { id, email, name, avatarUrl?, contact? } }`

POST `/api/auth/otp/request`
- Request: `{ email: string }`
- Response: `{ message: string }`

POST `/api/auth/otp/verify`
- Request: `{ email: string, code: string }`
- Response: `{ accessToken: string, user: { id, email, name, avatarUrl?, contact? } }`

POST `/api/auth/demo`
- Request: `{ type: "owner" | "member" }`
- Response: `{ accessToken: string, user: { id, email, name, avatarUrl?, contact? } }`

---

## Users

GET `/api/users/me`
- Response: `{ user: { id, email, name, avatarUrl?, contact? } }`

PATCH `/api/users/me`
- Request: `{ name?: string, avatarUrl?: string | null, contact?: string | null }`
- Response: `{ user: { id, email, name, avatarUrl?, contact? } }`

---

## Workspaces

GET `/api/workspaces`
- Response: `{ workspaces: Array<{ id, name, key?, ownerId?, role: "OWNER" | "ADMIN" | "MEMBER" | "VIEWER" }> }`

POST `/api/workspaces`
- Request: `{ name: string, key: string }`
- Response: `{ workspace: { id, name, key }, role: "OWNER" | "ADMIN" | "MEMBER" | "VIEWER" }`

POST `/api/workspaces/join`
- Request: `{ code: string }`
- Response: `{ workspace: { id, name, key }, role: "OWNER" | "ADMIN" | "MEMBER" | "VIEWER" }`

GET `/api/workspaces/{wid}/members`
- Response: `{ members: Array<{ id, role, user: { id, name, email, avatarUrl?, contact? } }> }`

PATCH `/api/workspaces/{wid}/members/{memberId}`
- Request: `{ role: "OWNER" | "ADMIN" | "MEMBER" | "VIEWER" }`
- Response: `{ member: { id, role } }`

GET `/api/workspaces/{wid}/members/{memberId}/overview`
- Response:
  ```json
  {
    "user": { "id": "...", "name": "...", "email": "...", "avatarUrl": null, "contact": null },
    "stats": { "issuesCreated": 0, "issuesAssigned": 0, "kbWorkedOn": 0 },
    "recent": {
      "issuesCreated": [{ "_id": "...", "ticketId": "ACME-1", "title": "...", "status": "OPEN", "priority": "MEDIUM" }],
      "issuesAssigned": [{ "_id": "...", "ticketId": "ACME-2", "title": "...", "status": "OPEN", "priority": "MEDIUM" }],
      "kbWorkedOn": [{ "_id": "...", "kbId": "ACME-KB-1", "title": "...", "updatedAt": "..." }]
    }
  }
  ```

POST `/api/workspaces/{wid}/invite`
- Request: (no body)
- Response: `{ inviteCode: string, inviteLink?: string, expiresAt?: string }`

GET `/api/workspaces/{wid}/activities`
- Query: `limit`
- Response: `{ activities: Array<{ _id, action, createdAt, actorId?, issueId?, meta? }> }`

---

## Issues

GET `/api/workspaces/{wid}/issues`
- Query: `status`, `priority`, `assigneeId`, `ticketId`, `page`, `limit`
- Response: `{ issues: Issue[], pagination: { page: number, limit: number, total: number } }`

POST `/api/workspaces/{wid}/issues`
- Request: `{ title, description?, status?, priority?, labels?, assigneeId?, dueDate? }`
- Response: `{ issue: Issue }`

GET `/api/workspaces/{wid}/issues/{issueId}`
- Response: `{ issue: Issue }`

PATCH `/api/workspaces/{wid}/issues/{issueId}`
- Request: `{ title?, description?, status?, priority?, labels?, assigneeId?, dueDate? }`
- Response: `{ issue: Issue }`

DELETE `/api/workspaces/{wid}/issues/{issueId}`
- Response: `{ ok: true }`

Issue shape (UI usage)
```json
{
  "_id": "...",
  "ticketId": "ACME-1",
  "title": "...",
  "description": "...",
  "status": "OPEN" | "IN_PROGRESS" | "DONE",
  "priority": "LOW" | "MEDIUM" | "HIGH",
  "labels": ["api", "ui"],
  "assigneeId": { "_id": "...", "name": "...", "email": "...", "avatarUrl": null } | null,
  "createdBy": { "_id": "...", "name": "...", "email": "...", "avatarUrl": null } | null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

## Comments

GET `/api/issues/{issueId}/comments`
- Response: `{ comments: Array<{ _id, body, userId: { name, email, avatarUrl? }, createdAt }> }`

POST `/api/issues/{issueId}/comments`
- Request: `{ body: string }`
- Response: `{ comment: Comment }`

---

## Knowledge Base (Articles)

GET `/api/workspaces/{wid}/articles`
- Query: `issueId` (optional)
- Response: `{ articles: Article[] }`

POST `/api/workspaces/{wid}/articles`
- Request: `{ title, body?, linkedIssueIds? }`
- Response: `{ article: Article }`

GET `/api/workspaces/{wid}/articles/{articleId}`
- Response: `{ article: Article }`

PATCH `/api/workspaces/{wid}/articles/{articleId}`
- Request: `{ title?, body?, linkedIssueIds? }`
- Response: `{ article: Article }`

DELETE `/api/workspaces/{wid}/articles/{articleId}`
- Response: `{ ok: true }`

Article shape (UI usage)
```json
{
  "_id": "...",
  "kbId": "ACME-KB-1",
  "title": "...",
  "body": "...",
  "linkedIssueIds": ["..."],
  "createdAt": "...",
  "updatedAt": "...",
  "createdBy": { "name": "...", "email": "...", "avatarUrl": null } | null,
  "updatedBy": { "name": "...", "email": "...", "avatarUrl": null } | null
}
```

---

## Notifications

GET `/api/notifications`
- Query: `unread=true` (optional)
- Response: `{ notifications: Array<{ _id, message, type, readAt?, createdAt, issueId? }> }`

PATCH `/api/notifications/read-all`
- Response: `{ updated: number }`

PATCH `/api/notifications/{id}/read`
- Response: `{ notification: { _id, message, type, readAt?, createdAt, issueId? } }`