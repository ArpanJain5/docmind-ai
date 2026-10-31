# DocMind AI 🧠

An AI-powered workspace for students, developers, and office teams. Sign up, chat with an AI assistant, and keep every conversation saved in your own private history. PDF Q&A, summaries, email drafting, resume checks, and spam detection are on the roadmap.

> Built step by step as a learning project. Every technology choice, its alternatives, and every problem solved is documented in [docs/tech-decisions.md](docs/tech-decisions.md) and [docs/learning-log.md](docs/learning-log.md).

## Features

### Working now
- [x] AI chat assistant (real LLM replies through a swappable `AiService` interface)
- [x] User accounts: register and log in (BCrypt-hashed passwords, JWT)
- [x] Saved chat history, private to each user
- [x] "My chats" sidebar: switch, rename, and delete chats
- [x] Failed AI calls are undone, so no unanswered messages are left behind
- [x] AI replies rendered as Markdown (lists, tables, code) with a Copy button
- [x] Streaming AI replies with a Stop button

### Planned
- [ ] Chat with PDF documents
- [ ] Document summarizer
- [ ] AI email generator
- [ ] Resume checker
- [ ] Spam detector

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React (Vite), JavaScript |
| Backend | Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA |
| Database | MySQL 8 |
| AI | OpenAI-compatible LLM API (Groq free tier), behind an `AiService` interface |

## Architecture

```
[ React (Vite) ]  --HTTP/JSON-->  [ Spring Boot REST API ]  --JDBC-->  [ MySQL ]
   Login, chat UI                   Controllers -> Services
   My chats sidebar                 JWT security, ownership checks
                                            |
                                            +--HTTPS-->  [ LLM API ]
```

## API

| Method and path | Auth | Purpose |
|---|---|---|
| `POST /api/auth/register` | open | Create an account |
| `POST /api/auth/login` | open | Log in, returns a JWT |
| `GET /api/auth/me` | token | Current user |
| `POST /api/chat` | token | Send a message, get the AI reply |
| `GET /api/conversations` | token | List my chats |
| `GET /api/conversations/{id}/messages` | token | Messages of one of my chats |
| `PATCH /api/conversations/{id}` | token | Rename my chat |
| `DELETE /api/conversations/{id}` | token | Delete my chat |
| `POST /api/chat/stream` | token | Send a message, get the reply as a stream of events |

## Security highlights
- Passwords hashed with BCrypt; never stored or logged
- Stateless JWT authentication (signed, short-lived)
- Every chat lookup includes the owner from the verified token, so other users' chats return 404
- Secrets live in a git-ignored `application-local.properties`; only a `.example` file is committed
- AI calls have timeouts, a message length limit, and a capped history; failures return a friendly 502 and details stay in the logs

## Run it locally

**Prerequisites:** JDK 21, Node.js 20 or newer, MySQL 8.

**1. Create the database** (in MySQL Workbench or the mysql shell):

```sql
CREATE DATABASE docmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'docmind_user'@'localhost' IDENTIFIED BY 'CHOOSE_A_PASSWORD';
GRANT ALL PRIVILEGES ON docmind.* TO 'docmind_user'@'localhost';
FLUSH PRIVILEGES;
```

**2. Configure the backend.** In `backend/src/main/resources/`, copy `application-local.properties.example` to `application-local.properties` and fill in the values:

- the database password from step 1
- `app.jwt.secret`: a random value of at least 32 characters. In PowerShell:

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

- the AI: keep `app.ai.provider=fake` to run with no key, or set `groq` with your own API key and model

**3. Start the backend:**

```powershell
cd backend
.\mvnw spring-boot:run
```

**4. Start the frontend** (in a second terminal):

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, create an account, and start chatting.

## Tests

```powershell
cd backend
.\mvnw test        # 9 unit tests, 10 real-HTTP integration tests (in-memory database, fake AI)

cd ..\frontend
npm test           # 19 Vitest tests (request helper and stream reader)
```

## Project structure

```
backend/   Spring Boot API (ai, config, controller, dto, model, repository, service)
frontend/  React app (api helpers, auth context, pages)
docs/      learning-log.md and tech-decisions.md
```

## Progress

| Day | What was built | Status |
|---|---|---|
| 0 | Tools, GitHub repo, project structure | ✅ |
| 1 | Spring Boot backend with Maven | ✅ |
| 2 | React frontend connected to the backend (CORS) | ✅ |
| 3 | Chat screen with a fake reply over JSON | ✅ |
| 4 | Chat history saved in MySQL | ✅ |
| 5 | User registration with hashed passwords | ✅ |
| 6 | Login with JWT and a protected endpoint | ✅ |
| 7 | Login and register screens | ✅ |
| 8 | Private chats per user and a "My chats" sidebar | ✅ |
| 9 | Real AI replies behind an `AiService` interface | ✅ |
| 10 | Rename and delete chats, safe handling of failed AI calls | ✅ |
| 11 | Safe Markdown rendering, Copy button, smarter scrolling | ✅ |
| 12 | Streaming replies (Server-Sent Events) with a Stop button | ✅ |
| 13 | Automated tests (unit, real-HTTP integration, frontend) | ✅ |
| 14 | UI polish and routing | 🔜 Next |

## Roadmap
Chat polish and tests, then PDF upload and chat with PDF, summarizer and email generator, resume checker and spam detector, and finally rate limiting, refresh tokens, migrations, Docker, CI, and deployment.