# Real-Time Collaborative Code Editor Backend

A production-ready Spring Boot 3.x backend for real-time collaborative code editing featuring Operational Transformation (OT) conflict resolution, WebSocket (STOMP), Redis Pub/Sub, and MySQL.

## Architecture Diagram

```text
+-------------------+      HTTP (JWT)      +---------------------------------+
|   Client 1        | -------------------> |         REST Controllers        |
| (React/Vue/etc)   |                      | (Auth, Documents, Revisions)    |
+-------------------+                      +---------------------------------+
        ^ |                                                 |
        | | WS (STOMP)                                      | 
        | v                                                 v
+-------------------+      WS + JWT        +---------------------------------+
| WebSocket Broker  | <------------------> |    EditorWebSocketHandler       |
|   (Spring Web)    |                      | (Routes Ops, Cursors, Presence) |
+-------------------+                      +---------------------------------+
                                                            |
                                                            v
+----------------------------------------------------------------------------+
|                            OT Engine (Transform & Apply)                   |
+----------------------------------------------------------------------------+
                                                            |
                                                            v
                                            +-------------------------------+
+-------------------+      Pub/Sub         |      RedisMessagePublisher      |
|    Redis Node     | <------------------- +-------------------------------+
| (Pub/Sub + Data)  | -------------------> |      RedisMessageSubscriber     |
+-------------------+                      +-------------------------------+
                                                            |
                                                            v
+-------------------+                      +-------------------------------+
|  MySQL Database   | <------------------> | Spring Data JPA Repositories  |
| (Users, Docs,     |                      |  (User, Document, Revision)   |
|  Revisions)       |                      +-------------------------------+
+-------------------+
```

## Features

- **JWT Authentication**: Secure REST and WebSocket connections.
- **OT Engine**: Custom Operational Transformation engine handling 6 core conflict scenarios (re-bases divergent user operations).
- **Multi-Server Ready**: Utilizes Redis Pub/Sub so users can collaborate even when connected to different backend instances.
- **WebSocket STOMP API**: Topic-based messaging for real-time cursor updates, join/leave presence, and text operations.
- **Revision History**: Every operation is stored. Documents can be restored to past states.
- **Session Tracking**: Active users per document are stored in Redis Hash/Sets with deterministic hex color assignment.

## Setup Requirements

1. **Java 17+**
2. **Maven 3.8+**
3. **MySQL 8.0+**
4. **Redis 6+**

### Local Setup Instructions

1. Start your local MySQL and Redis instances.
2. Ensure you have a database named `code_editor` created in MySQL, or let Spring Boot create it if your permissions allow (by default, `createDatabaseIfNotExist=true` is set).
3. The schema will be automatically initialized via `schema.sql` on first startup.

```bash
# Build the project
mvn clean install

# Run the app
mvn spring-boot:run
```

By default it will be available on `http://localhost:8080`.

## Testing the application

- The backend includes a full Postman Collection: `postman_collection.json`
- For WebSocket testing via command line, use `websocket_test.sh` (requires `wscat`).
- For automated testing, run `mvn test` to exercise the complete OT Engine test suite.
