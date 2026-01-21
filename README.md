# SprintDesk

![SprintDesk](frontend/assets/sprintdesk-logo.png)

SprintDesk is a multi-tenant issue tracker + knowledge base focused on crisp, editor-grade UI and team workflows. The repo is built in phased commits, keeping each stage runnable.

## What it does

- Workspaces with role-based access (owner/admin/member/viewer)
- Issues with comments, assignees, priorities, and realtime updates
- Knowledge base articles with markdown preview and linked issues
- Notifications for assignments and mentions
- Auth with JWT access + refresh rotation, Google sign-in, email OTP, and demo mode

## Tech stack

- Frontend: React + TypeScript (Vite), Tailwind CSS, Radix UI, Lucide icons
- Backend: Node.js + Express, MongoDB + Mongoose, Socket.io
- Auth: JWT + refresh cookies, Google ID token verification, SMTP OTP

## Quick Start (Docker)

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:4000/api/health

## Local Dev (No Docker)

```bash
cd backend
npm install
npm run dev
```

```bash
cd frontend
npm install
npm run dev
```

## Environment

- `backend/.env.example`
- `frontend/.env.example`

Google login requires:
- Backend: `GOOGLE_CLIENT_ID`
- Frontend: `VITE_GOOGLE_CLIENT_ID`

Email OTP login requires SMTP settings in `backend/.env`:
`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`.

Demo mode:
- `DEMO_MODE=true` enables `/api/auth/demo` for demo logins.
- `DEMO_SEED_ON_START=true` seeds demo users/workspace on startup.

## shadcn/ui Setup

The frontend is pre-configured for shadcn/ui. To add components, run:

```bash
cd frontend
npx shadcn@latest add button card dialog sheet table tabs badge dropdown-menu input textarea separator skeleton
```

This uses the `components.json` already included in the repo.
