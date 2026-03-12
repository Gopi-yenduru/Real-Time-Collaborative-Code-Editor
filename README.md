# ⚡ Real-Time Collaborative Code Editor

A full-stack, production-quality **real-time collaborative code editor** built with **Spring Boot 3.x** (Backend) and **React + Vite** (Frontend). Multiple users can edit the same document simultaneously with live cursor tracking, conflict resolution via Operational Transformation, and instant syncing over WebSockets.

---

## 🏗️ Architecture

```text
┌─────────────┐     HTTP/JWT     ┌───────────────────────────────────┐
│  React App  │ ───────────────► │       REST Controllers            │
│  (Vite +    │                  │  (Auth, Documents, Revisions)     │
│   Monaco)   │                  └───────────────────────────────────┘
│             │                                    │
│             │     WS/STOMP                       ▼
│             │ ◄──────────────► ┌───────────────────────────────────┐
└─────────────┘                  │  EditorWebSocketHandler           │
                                 │  (Ops, Cursors, Presence)         │
                                 └───────────────────────────────────┘
                                                   │
                                                   ▼
                                 ┌───────────────────────────────────┐
                                 │     OT Engine (Transform & Apply) │
                                 └───────────────────────────────────┘
                                                   │
                          ┌────────────────────────┼────────────────────────┐
                          ▼                                                 ▼
               ┌──────────────────┐                          ┌──────────────────────┐
               │   Redis (Pub/Sub │                          │  MySQL (Users, Docs, │
               │   + Sessions)    │                          │  Revisions)          │
               └──────────────────┘                          └──────────────────────┘
```

---

## ✨ Features

### Backend (Java 17 / Spring Boot 3.x)
| Feature | Description |
|---|---|
| **JWT Authentication** | Stateless token-based auth for REST & WebSocket endpoints |
| **OT Engine** | Custom Operational Transformation handling 6 core conflict scenarios |
| **STOMP WebSockets** | Topic-based real-time messaging for ops, cursors & presence |
| **Redis Pub/Sub** | Multi-instance broadcasting for horizontal scaling |
| **Revision History** | Every operation stored with author + timestamp; point-in-time restore |
| **Session Manager** | Per-document user tracking in Redis with color assignment |
| **Role-Based Access** | OWNER / EDITOR / VIEWER roles per document |

### Frontend (React / Vite / Tailwind CSS)
| Feature | Description |
|---|---|
| **Monaco Editor** | VS Code's editor engine for a native coding experience |
| **Live Collaboration** | Real-time text sync via STOMP WebSocket |
| **Remote Cursors** | Color-coded cursor decorations for each connected user |
| **Presence Bar** | Shows who is online in each document session |
| **Glassmorphism UI** | Premium dark theme with gradients and micro-animations |
| **Dashboard** | Document grid view, creation modal, and search |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 3.x, Spring Security, Spring WebSocket |
| Database | MySQL 8 (JPA/Hibernate) |
| Cache / Pub-Sub | Redis 6+ |
| Auth | JWT (jjwt 0.12.5) |
| Build | Maven |
| Frontend Framework | React 18 (Vite 8) |
| Code Editor | Monaco Editor (`@monaco-editor/react`) |
| State Management | Zustand |
| WebSocket Client | `@stomp/stompjs` + `sockjs-client` |
| Styling | Tailwind CSS 4 |
| Icons | Lucide React |

---

## 📋 Prerequisites

- **Java 17+** and **Maven 3.8+**
- **Node.js 18+** and **npm 9+**
- **MySQL 8.0+** (running on `localhost:3306`)
- **Redis 6+** (running on `localhost:6379`)

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Gopi-yenduru/Real-Time-Collaborative-Code-Editor.git
cd Real-Time-Collaborative-Code-Editor
```

### 2. Configure Environment Variables

The app reads credentials from environment variables with safe defaults. Set them before starting:

```bash
# Linux / macOS
export DB_PASSWORD=your_mysql_password
export DB_USERNAME=root
export JWT_SECRET=your-long-random-secret-key

# Windows (PowerShell)
$env:DB_PASSWORD="your_mysql_password"
$env:DB_USERNAME="root"
$env:JWT_SECRET="your-long-random-secret-key"
```

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `code_editor` | Database name (auto-created) |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | *(empty)* | MySQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | `changeme...` | JWT signing secret |
| `JWT_EXPIRATION` | `86400000` | Token lifetime (ms) — default 24h |

### 3. Start the Backend

```bash
mvn clean install
mvn spring-boot:run
```

Backend will be available at **http://localhost:8080**

### 4. Start the Frontend

```bash
cd realtime-editor-frontend
npm install
npm run dev
```

Frontend will be available at **http://localhost:5173**

---

## 📡 API Endpoints

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT token |

### Documents
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/documents` | List all documents for logged-in user |
| `POST` | `/api/documents` | Create a new document |
| `GET` | `/api/documents/{id}` | Get document by ID |
| `POST` | `/api/documents/{id}/share` | Share document with another user |

### Revisions
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/revisions/{docId}` | Get revision history for a document |
| `POST` | `/api/revisions/{docId}/restore/{revisionNumber}` | Restore document to a specific revision |

### WebSocket (STOMP)
| Endpoint | Type | Description |
|---|---|---|
| `/ws/editor` | Connect | SockJS WebSocket handshake URL |
| `/app/editor/join` | Send | Join a document editing session |
| `/app/editor/operation` | Send | Send an edit operation (INSERT/DELETE) |
| `/app/editor/cursor` | Send | Broadcast cursor position |
| `/topic/document/{docId}` | Subscribe | Receive real-time operations |
| `/topic/presence/{docId}` | Subscribe | User join/leave notifications |
| `/topic/cursors/{docId}` | Subscribe | Remote cursor position updates |

---

## 🧪 Testing

```bash
# Run OT Engine unit tests
mvn test

# Use the included Postman collection for API testing
# File: postman_collection.json

# WebSocket testing via wscat
# File: websocket_test.sh
```

### Testing Real-Time Collaboration
1. Open **http://localhost:5173** in two browser windows
2. Register two different accounts
3. Create a document with the first user
4. Share it with the second user's email
5. Open the same document in both windows
6. Start typing — edits appear instantly in both windows!

---

## 📂 Project Structure

```
Real-Time-Collaborative-Code-Editor/
├── pom.xml                          # Maven config
├── src/main/java/com/codeeditor/
│   ├── RealtimeCodeEditorApplication.java
│   ├── config/
│   │   └── RedisConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── DocumentController.java
│   │   ├── RevisionController.java
│   │   └── GlobalExceptionHandler.java
│   ├── engine/
│   │   ├── OTEngine.java           # Operational Transformation
│   │   └── Operation.java
│   ├── model/
│   │   ├── Document.java
│   │   ├── User.java
│   │   ├── Revision.java
│   │   ├── DocumentUser.java
│   │   └── Role.java / OpType.java
│   ├── repository/                  # Spring Data JPA
│   ├── security/                    # JWT + Spring Security
│   ├── service/                     # Business logic
│   ├── redis/                       # Pub/Sub publisher & subscriber
│   └── websocket/                   # STOMP handlers & DTOs
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql
├── src/test/                        # Unit tests
├── realtime-editor-frontend/        # React frontend
│   ├── src/
│   │   ├── components/
│   │   │   └── CollaborativeEditor.jsx
│   │   ├── hooks/
│   │   │   └── useWebSocket.js
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   └── EditorPage.jsx
│   │   ├── store/
│   │   │   ├── authStore.js
│   │   │   └── editorStore.js
│   │   └── lib/
│   │       └── api.js
│   ├── package.json
│   └── vite.config.js
├── postman_collection.json
├── websocket_test.sh
└── README.md
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

---

## 🙋 Author

**Gopi Yenduru** — [GitHub](https://github.com/Gopi-yenduru)
