# ⚡ Real-Time Collaborative Code Editor

A full-stack **real-time collaborative code editor** built with **Spring Boot 3** (backend) and **React + Vite** (frontend). Multiple people edit the same document at once — like Google Docs for code — with live cursors, presence, and conflict-free merging powered by **CRDTs (Yjs)**.

> **v2 — CRDT rewrite.** The original hand-rolled Operational Transformation engine has been replaced with **Yjs**, an industry-proven CRDT. Concurrent edits now converge deterministically, remote cursors and selections render natively, and the whole system tolerates reconnects and offline edits. The Spring Boot backend acts as a **persistent relay** — it stores and forwards opaque CRDT updates without ever having to reason about merge conflicts.

---

## 🏗️ Architecture

```text
┌─────────────┐   HTTP / JWT    ┌────────────────────────────────────────┐
│  React App  │ ───────────────►│  REST Controllers                      │
│  (Vite +    │                 │  Auth · Documents · Snapshots (history)│
│   Monaco +  │                 └────────────────────────────────────────┘
│   Yjs)      │                                   │
│             │  Binary WS       ┌────────────────────────────────────────┐
│             │◄───────────────►│  YjsWebSocketHandler (native, binary)   │
│  y-monaco   │  (Yjs frames)    │  handshake auth · SYNC/AWARENESS relay  │
└─────────────┘                 └────────────────────────────────────────┘
                                                  │
                        ┌─────────────────────────┼──────────────────────────┐
                        ▼                                                     ▼
             ┌────────────────────┐                         ┌──────────────────────────┐
             │  Redis Pub/Sub     │                         │  MySQL                    │
             │  (cross-node relay)│                         │  users · documents ·      │
             └────────────────────┘                         │  document_users ·         │
                                                            │  document_updates (CRDT log)│
                                                            │  document_snapshots (history)│
                                                            └──────────────────────────┘
```

### How real-time sync works

1. Each client holds a **Yjs document**. Every keystroke produces a compact binary **update**.
2. Updates travel over a **native binary WebSocket** (`/ws/yjs/{docId}`) as `[type byte][payload]` frames.
3. The server treats updates as **opaque bytes**: it appends them to a per-document log and fans them out to everyone else (locally, and across nodes via **Redis**).
4. A joining client sends `SYNC_REQUEST`; the server **replays the log**. Because Yjs updates are commutative and idempotent, replay in any order reconstructs the exact same document for everyone — no merge logic on the server.
5. **Cursors & presence** ride Yjs *awareness* (relayed, never stored). **`y-monaco`** renders each remote user's caret and selection automatically.
6. The log is periodically **compacted** into one merged update (`SNAPSHOT`), and a plain-text mirror is saved for previews/search.

---

## ✨ Features

### Backend (Java 17 / Spring Boot 3)
| Feature | Description |
|---|---|
| **CRDT relay** | Stores & forwards opaque Yjs updates — convergence guaranteed by the CRDT, not the server |
| **Native binary WebSocket** | Compact `[type][payload]` frames; auth + document access enforced at the handshake |
| **Redis Pub/Sub** | Relays frames between instances for horizontal scaling |
| **Append-only update log + compaction** | Durable history, bounded by client-driven snapshots |
| **Version history** | Named snapshots (merged CRDT state + text) with one-click restore for all clients |
| **Role-based access** | OWNER / EDITOR / VIEWER enforced on REST **and** the WebSocket |
| **JWT auth** | Stateless tokens; claims carry the user id to avoid per-request lookups |
| **OpenAPI / Swagger UI** | Interactive API docs with a JWT "Authorize" button |
| **Consistent error envelope + actuator health** | Clean 4xx/5xx JSON and a `/actuator/health` probe |

### Frontend (React 19 / Vite / Tailwind CSS)
| Feature | Description |
|---|---|
| **Monaco Editor** | VS Code's editor engine, bundled locally (no CDN) |
| **Yjs + y-monaco** | Conflict-free editing with native remote cursors & selections |
| **Custom WebSocket provider** | ~200 lines, speaks the server's binary protocol, auto-reconnects & flushes offline edits |
| **Presence** | Deterministic per-user colors, live collaborator avatars |
| **Version history UI** | Save & restore named versions from the editor |
| **Sharing** | Invite collaborators as editor or viewer |
| **Glassmorphism UI** | Dark theme with gradients and micro-animations |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend language | Java 17 (builds on JDK 17–24) |
| Backend framework | Spring Boot 3.5, Spring Security, Spring WebSocket |
| Database | MySQL 8 (JPA / Hibernate) |
| Cross-node relay | Redis 6+ |
| Auth | JWT (jjwt 0.13.0) |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Frontend framework | React 19 (Vite) |
| Realtime engine | **Yjs**, **y-monaco**, **y-protocols** (awareness) |
| Code editor | Monaco Editor (`@monaco-editor/react`, bundled) |
| State management | Zustand |
| Styling | Tailwind CSS 4 |
| Icons | Lucide React |

---

## 📋 Prerequisites

- **JDK 17+** (tested on JDK 24) and **Maven 3.8+**
- **Node.js 18+** and **npm 9+**
- **MySQL 8.0+** (on `localhost:3306`)
- **Redis 6+** (on `localhost:6379`)

---

## 🚀 Getting Started

### 1. Clone

```bash
git clone https://github.com/Gopi-yenduru/Real-Time-Collaborative-Code-Editor.git
cd Real-Time-Collaborative-Code-Editor
```

### 2. Configure environment

Defaults suit local dev (see `.env.example`). Override before starting:

```bash
# Linux / macOS
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=your-long-random-secret-key
```

```powershell
# Windows (PowerShell)
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="your-long-random-secret-key"
```

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | MySQL host/port |
| `DB_NAME` | `code_editor` | Database (auto-created) |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / *(empty)* | MySQL credentials |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis host/port |
| `JWT_SECRET` | `changeme...` | Signing key (≥32 chars, or Base64 256-bit) |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in ms (24h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed web origins |

### 3. Run the backend

```bash
mvn spring-boot:run
```

- API: **http://localhost:8080**
- Swagger UI: **http://localhost:8080/swagger-ui.html**
- Health: **http://localhost:8080/actuator/health**

### 4. Run the frontend

```bash
cd realtime-editor-frontend
npm install --legacy-peer-deps
npm run dev
```

Frontend: **http://localhost:5173**

> `--legacy-peer-deps` sidesteps a peer-range mismatch between Vite and the Tailwind Vite plugin.

---

## 📡 API & WebSocket

### REST
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Log in → `{ token, expiresInMs, user }` |
| `GET` | `/api/auth/me` | Current authenticated user |
| `GET` | `/api/documents` | List documents you can access (with your role) |
| `POST` | `/api/documents` | Create a document |
| `GET` | `/api/documents/{id}` | Get a document (access-checked, includes content) |
| `PUT` | `/api/documents/{id}/content` | Update the plain-text mirror (editors) |
| `POST` | `/api/documents/{id}/share` | Share as EDITOR/VIEWER (owner) |
| `DELETE` | `/api/documents/{id}` | Delete a document (owner) |
| `GET` | `/api/documents/{id}/snapshots` | List saved versions |
| `POST` | `/api/documents/{id}/snapshots` | Save the current version |
| `POST` | `/api/documents/{id}/snapshots/{sid}/restore` | Restore a version (resets all clients) |

### WebSocket (native binary)

Connect to `ws://localhost:8080/ws/yjs/{documentId}?token={JWT}`. Auth and access are verified during the handshake. Each frame is `[1 type byte][payload]`:

| Type | Name | Direction | Payload |
|---|---|---|---|
| `0` | `SYNC` | both | Yjs document update |
| `1` | `AWARENESS` | both | Cursor / presence update |
| `2` | `SYNC_REQUEST` | client → server | *(empty)* — request history replay |
| `3` | `SYNCED` | server → client | *(empty)* — history sent |
| `4` | `SNAPSHOT` | client → server | Merged full state (compacts the log) |
| `5` | `RESET` | server → client | *(empty)* — discard & re-sync (after restore) |

---

## 🧪 Testing

```bash
mvn test                      # backend unit tests (frame codec + relay orchestration)
```

- **API:** import `postman_collection.json`.
- **WebSocket:** `websocket_test.sh` (proves the handshake authorizes).

### Try real-time collaboration
1. Open **http://localhost:5173** in two browser windows.
2. Register two accounts; create a document with the first.
3. Share it with the second user's email (as EDITOR).
4. Open the same document in both windows and type — edits, cursors, and selections sync live. Kill the network briefly and watch it reconnect and converge.

---

## 📂 Project Structure

```
Real-Time-Collaborative-Code-Editor/
├── pom.xml
├── .env.example
├── src/main/java/com/codeeditor/
│   ├── config/            # OpenApiConfig
│   ├── controller/        # Auth · Document · Snapshot · GlobalExceptionHandler
│   ├── dto/               # request/response records
│   ├── exception/         # NotFound / Forbidden / Conflict
│   ├── model/             # User · Document · DocumentUser · DocumentUpdate · DocumentSnapshot
│   ├── redis/             # RedisConfig · CollaborationRelay (cross-node)
│   ├── repository/        # Spring Data JPA
│   ├── security/          # JWT + Spring Security
│   ├── service/           # Auth · Document · Collaboration · Snapshot
│   └── websocket/         # YjsWebSocketConfig · handler · handshake · registry · protocol
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql         # reference only (Hibernate manages the schema)
├── src/test/              # FramesTest · CollaborationServiceTest
├── realtime-editor-frontend/
│   ├── .env.example
│   └── src/
│       ├── components/CollaborativeEditor.jsx
│       ├── hooks/useCollaboration.js
│       ├── lib/           # collab.js (provider) · monacoSetup.js · colors.js · bytes.js · api.js
│       ├── pages/         # Login · Register · Dashboard · EditorPage
│       └── store/         # authStore · editorStore
├── postman_collection.json
├── websocket_test.sh
└── README.md
```

---

## 📄 License

MIT License.

## 🙋 Author

**Gopi Yenduru** — [GitHub](https://github.com/Gopi-yenduru)
