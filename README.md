# DocMind AI 🧠

An AI-powered workspace for students, developers, and office teams. Sign up, chat with an AI assistant, upload your PDFs and ask questions about them, and keep every conversation saved in your own private history. Document summaries, email drafting, resume checks, and spam detection are on the roadmap.

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
- [x] Real page addresses (`/login`, `/register`, `/chat`), guarded routes, and a phone-friendly layout
- [x] PDF upload, private to each user, with the text read on the server: page count, preview, and a status (text ready, no text found)
- [x] Chat about a PDF: pick one of your documents when you start a chat, and the AI answers from its text (the first 12,000 characters, with a clear warning when it is cut)

### Planned
- [ ] Smarter chat with long PDFs (chunks and retrieval)
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
| Documents | Apache PDFBox (text extraction), files on local disk, text in MySQL |

## Architecture

```
[ React (Vite) ]  --HTTP/JSON-->  [ Spring Boot REST API ]  --JDBC-->  [ MySQL ]
   Login, chat UI                   Controllers -> Services              users, chats,
   My chats sidebar                 JWT security, ownership checks       documents, texts
   Documents page                           |
                                            +--files-->  [ local disk (PDFs) ]
                                            |
                                            +--HTTPS-->  [ LLM API ]
```

## API

| Method and path | Auth | Purpose |
|---|---|---|
| `POST /api/auth/register` | open | Create an account |
| `POST /api/auth/login` | open | Log in, returns a JWT |
| `GET /api/auth/me` | token | Current user |
| `POST /api/chat` | token | Send a message, get the AI reply. For a new chat the body may contain `documentId` to chat about one of your documents |
| `POST /api/chat/stream` | token | The same, but the reply arrives as a stream of events |
| `GET /api/conversations` | token | My chats (with the document name and whether its text was cut, if the chat is about a document) |
| `GET /api/conversations/{id}/messages` | token | Messages of one of my chats |
| `PATCH /api/conversations/{id}` | token | Rename my chat |
| `DELETE /api/conversations/{id}` | token | Delete my chat |
| `POST /api/documents` | token | Upload a PDF (max 5 MB). Returns status, page count and preview |
| `GET /api/documents` | token | List my documents |
| `DELETE /api/documents/{id}` | token | Delete my document (chats about it become ordinary chats) |

## Security highlights
- Passwords hashed with BCrypt; never stored or logged
- Stateless JWT authentication (signed, short-lived)
- Every chat and document lookup includes the owner from the verified token, so other users' data returns 404
- Secrets live in a git-ignored `application-local.properties`; only a `.example` file is committed
- AI calls have timeouts, a message length limit, and a capped history; failures return a friendly 502 and details stay in the logs
- Uploads: size limit, file type checked by content, random file names, per-user ownership
- Uploads are checked by their first bytes, then parsed with a page cap. Unreadable files get a clean 422, and error replies never show stack traces, SQL or exception names
- Chat documents: only your own documents can be attached (404 otherwise), and document text is treated as untrusted data (prompt injection is reduced, with a test that the text cannot break out of its block)

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
- optional: `app.ai.max-document-chars` (default 12000) sets how much of a document the AI reads with each question

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

Open http://localhost:5173, create an account, and start chatting. Upload a PDF on the Documents page to chat about it.

## Tests

```powershell
cd backend
.\mvnw test        # 50 tests: unit tests, PDF extractor and prompt tests, real-HTTP integration tests (in-memory database, fake AI)

cd ..\frontend
npm test           # 62 Vitest tests (request helper, stream reader, upload, documents page, chat page, routes)
npm run e2e        # 8 Playwright browser tests (needs the backend and the frontend running)
```

## Project structure

```
backend/   Spring Boot API (ai, config, controller, dto, model, repository, service)
frontend/  React app (api helpers, auth context, components, pages) and e2e/ (Playwright tests)
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
| 14 | React Router, route guards, phone drawer, loading/error/empty states | ✅ |
| 15 | PDF upload with checks, private storage and a documents page | ✅ |
| 16 | Read the text of PDFs (PDFBox), status, page count, preview, browser tests | ✅ |
| 17 | Chat with a PDF (version 1: the first part of the text, with a warning) | ✅ |
| 18 | Smarter chat with long PDFs (chunks and retrieval) | 🔜 Next |

## Roadmap
Smarter chat with long PDFs (chunks and retrieval), then summarizer and email generator, resume checker and spam detector, and finally rate limiting, refresh tokens, migrations, Docker, CI, and deployment.