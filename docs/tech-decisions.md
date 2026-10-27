# DocMind AI: Technology Decisions

> A living document. Every time we pick a tool, we record **what** it is, **why** we chose it, **what the alternatives were**, and **why we did not choose them**.
> Status tags: ✅ In use | 🔜 Planned | ❓ Undecided

---

## 1. Project Description (Interview Pitch)

### 30-second version
> "DocMind AI is a full-stack AI workspace I built with React, Spring Boot, and MySQL. Users can sign up, chat with an AI assistant, upload PDFs and ask questions about them, summarize documents, generate professional emails, check resumes, and detect spam. All conversations are saved per user. I built it to learn how to integrate LLM APIs into a secure, production-style Java backend."

### 2-minute version
- **Problem:** Students, developers, and office workers waste time reading long documents and writing repetitive text. Existing AI tools are generic and don't keep organized history or work with their own files.
- **Solution:** One app with several AI tools behind a single login, where every chat and document is saved.
- **Users:** Students (summarize notes, ask questions), developers (understand technical PDFs and API docs), office employees (emails, reports), and teams (a base for an internal company assistant).
- **Architecture:** React frontend → REST API → Spring Boot backend → MySQL database, with the backend calling an external LLM API.
- **Key engineering challenges:** JWT authentication, secure file upload, extracting and chunking PDF text, prompt design, handling slow AI responses, keeping the API key secret.

### Architecture at a glance

```
[ React (Vite) ]  --HTTP/JSON-->  [ Spring Boot REST API ]  --JDBC-->  [ MySQL ]
   Chat UI, login,                  Controllers, Services,
   PDF upload                       Security (JWT), PDF processing
                                              |
                                              +--HTTPS-->  [ LLM API ]
```

**Restaurant analogy:** React = dining room, REST API = waiter, Spring Boot = kitchen, MySQL = pantry, LLM API = a famous guest chef.

---

## 2. Decision Summary Table

| # | Area | Choice | Status |
|---|---|---|---|
| 1 | Version control | Git + GitHub | ✅ |
| 2 | Editor | VS Code | ✅ |
| 3 | Backend language | Java 21 (LTS) | ✅ |
| 4 | Backend framework | Spring Boot | ✅ |
| 5 | Build tool | Maven | ✅ |
| 6 | Boilerplate reduction | Lombok | ✅ |
| 7 | Dev productivity | Spring Boot DevTools | ✅ |
| 8 | Database | MySQL | 🔜 (Week 2) |
| 9 | Data access | Spring Data JPA (Hibernate) | 🔜 |
| 10 | Authentication | Spring Security + JWT | 🔜 |
| 11 | Frontend library | React | 🔜 (Day 2) |
| 12 | Frontend tooling | Vite | 🔜 (Day 2) |
| 13 | AI provider | LLM API (Claude / OpenAI) | 🔜 |
| 14 | PDF text extraction | Apache PDFBox | 🔜 |
| 15 | Chat-with-PDF approach | Chunking + retrieval (RAG) | 🔜 |
| 16 | Secrets handling | Environment variables | 🔜 |
| 17 | Frontend styling | Tailwind CSS or similar | ❓ |
| 18 | Deployment | Docker + a cloud host | ❓ |
| 19 | Cross-origin access | CORS config in Spring Boot | ✅ |
| 20 | HTTP calls from React | Built-in `fetch` | ✅ |
| 21 | Frontend language | JavaScript (JSX) | ✅ |
| 22 | Linting | ESLint | ✅ |
| 23 | Data format between React and Spring | JSON | ✅ |
| 24 | Request/response objects | Java records as DTOs | ✅ |
| 25 | Backend structure | Controller → Service layers | ✅ |
| 26 | Frontend state | React `useState` (no state library yet) | ✅ |
| 27 | Local DB credentials | Git-ignored `application-local.properties` (Spring profile) | ✅ |
| 28 | Schema management | Hibernate `ddl-auto=update` (development only) | ✅ |
| 29 | Chat data model | `Conversation` 1 → many `Message` | ✅ |
| 30 | Password hashing | BCrypt (`BCryptPasswordEncoder`) | ✅ |
| 31 | Input validation | Jakarta Bean Validation (`@Valid`) | ✅ |
| 32 | Email handling | Lowercased, unique constraint, 409 on duplicate | ✅ |
| 33 | JWT creation and checking | Spring Security OAuth2 resource server (Nimbus), HS256 | ✅ |
| 34 | Token lifetime | 60 minutes, configurable | ✅ |
| 35 | Login error handling | One generic 401 message | ✅ |
| 36 | Where the token is stored | `localStorage` (for now) | ✅ |
| 37 | Auth state in React | React Context (`AuthProvider` + `useAuth`) | ✅ |
| 38 | API calls from React | One `request()` helper around `fetch` | ✅ |
| 39 | Screen switching | Conditional rendering (no router yet) | ✅ |
| 40 | Access control for chats | Ownership check in the service layer (`findByIdAndUserId`) | ✅ |
| 41 | Response for someone else's chat | 404 Not Found (not 403) | ✅ |
| 42 | Chat ownership model | `Conversation` many-to-one `User` | ✅ |
| 43 | Dev data migration | Delete old test data before adding the required `user_id` | ✅ |
| 44 | AI abstraction | `AiService` interface with fake and real implementations | ✅ |
| 45 | HTTP client for the AI call | Spring `RestClient` | ✅ |
| 46 | Chat memory strategy | Resend the last 20 messages each call | ✅ |
| 47 | AI failure handling | `AiException`, then 502 to the client, details only in logs | ✅ |
| 48 | AI secrets | API key in git-ignored `application-local.properties` | ✅ |

---

## 3. Detailed Decisions

### 3.1 Git + GitHub ✅
- **What:** Git tracks every version of the code. GitHub stores it online.
- **Why we chose it:** Industry standard. A public repo with daily commits works as a portfolio and proves consistency to recruiters. It's also our backup.
- **Alternatives:** GitLab, Bitbucket, Azure DevOps.
- **Why not those:** They are all good and work the same way with Git. GitHub has the largest developer community, and recruiters look at GitHub profiles first.
- **Interview line:** "I committed daily with conventional commit messages like `feat:` and `fix:` so the history is readable."

### 3.2 Java 21 (LTS) ✅
- **What:** The programming language of the backend.
- **Why we chose it:** Java is widely used for enterprise backends. It is strongly typed, so many mistakes are caught before running. Version 21 is a Long-Term Support release, so it gets updates for years and supports modern features (records, pattern matching, virtual threads).
- **Alternatives:** Python (Django/FastAPI), Node.js (Express/NestJS), C# (.NET), Go.
- **Why not those:**
  - *Python:* Great for AI and quick prototyping, but I wanted to build strong Java and Spring skills, which are in high demand for backend roles.
  - *Node.js:* Would let us use JavaScript everywhere, but weaker typing makes large codebases harder to maintain, and it wouldn't teach enterprise Java.
  - *C# / Go:* Excellent, but outside the stack I'm targeting.
- **Why Java 21 and not 17 or 25:** 21 is an LTS release with wide framework support. Non-LTS versions are supported for a short time only.
- **Interview line:** "The AI part is just an HTTP call, so the language doesn't limit us. I chose Java to practice production-grade backend design."

### 3.3 Spring Boot ✅
- **What:** A Java framework that gives a ready-made web server and structure for building REST APIs.
- **Why we chose it:** Auto-configuration means little setup. It has a mature ecosystem: security, database access, validation, testing. It is the most common Java backend framework in job listings.
- **Alternatives:** Quarkus, Micronaut, Jakarta EE, plain Java with a library like Javalin.
- **Why not those:**
  - *Quarkus / Micronaut:* Faster startup and lower memory, but smaller communities and fewer tutorials. Their advantages matter more for serverless and large-scale deployments.
  - *Jakarta EE:* More configuration, and heavier to learn.
  - *Plain Java:* We would have to build security, JSON handling, and dependency injection ourselves.
- **Interview line:** "Spring Boot handles the plumbing, so I could focus on business logic and security."

### 3.4 Maven ✅
- **What:** Downloads libraries and builds the project. Dependencies are listed in `pom.xml`.
- **Why we chose it:** It's the default in Spring Initializr and in most tutorials, so help is easy to find. The XML structure is predictable. The `mvnw` wrapper means nobody needs Maven installed.
- **Alternatives:** Gradle.
- **Why not Gradle:** Gradle is faster on big projects and more flexible, but its scripts (Groovy/Kotlin) have a steeper learning curve. Maven's rigid structure is better for learning, and build speed doesn't matter for a project this size.

### 3.5 Lombok ✅
- **What:** Generates repetitive Java code (getters, setters, constructors) from annotations like `@Getter` and `@Data`.
- **Why we chose it:** Keeps entity and DTO classes short and readable.
- **Alternatives:** Java `record` types, IDE-generated code, writing it by hand.
- **Why not those:** Records are great for immutable DTOs, and we may use them there. But JPA entities need a no-argument constructor and mutable fields, which records don't support. So Lombok is the practical choice for entities.
- **Trade-off to mention:** Lombok hides code, which can confuse beginners and some tools. We accept that for readability.

### 3.6 Spring Boot DevTools ✅
- **What:** Automatically restarts the app when code changes.
- **Why we chose it:** Saves time during development.
- **Alternatives:** Manual restart, JRebel (paid).
- **Why not those:** Manual restarts waste time, and JRebel costs money. DevTools is free and built in. It is disabled in production builds automatically.

### 3.7 Spring Web (REST API) ✅
- **What:** The Spring module for building HTTP endpoints (`@RestController`).
- **Why we chose it:** Standard way to expose JSON endpoints that React can call.
- **Alternatives:** Spring WebFlux (reactive), GraphQL, gRPC.
- **Why not those:**
  - *WebFlux:* Helps with very high concurrency but makes code harder to write and debug. Spring MVC with Java 21 virtual threads handles our load fine.
  - *GraphQL:* Useful when clients need flexible queries. Our API has simple, fixed operations, so REST is simpler.
  - *gRPC:* Meant for service-to-service calls, and browsers don't support it directly.

### 3.8 Why we did NOT add Security and JPA on Day 1 ✅
- **Decision:** Start with Spring Web, Lombok, and DevTools only.
- **Reason:** Spring Data JPA crashes on startup if no database is configured, and Spring Security locks every endpoint behind a login by default. Adding tools one at a time lets us understand each one and debug problems in isolation.
- **Interview line:** "I added dependencies incrementally so every failure had a single, obvious cause."

---

### 3.9 MySQL ✅
- **What:** Relational database storing users, chats, messages, and document metadata.
- **Why we chose it:** Our data is structured and related (a user has many chats, a chat has many messages), which fits tables and foreign keys. MySQL is free, widely used, and has good Spring support. Workbench makes inspection easy.
- **Alternatives:** PostgreSQL, MongoDB, H2 (in-memory), SQLite.
- **Why not those:**
  - *PostgreSQL:* Equally valid, and has better built-in vector search (pgvector). It's a strong candidate if we add embeddings later. MySQL was already installed and is very common in job listings.
  - *MongoDB:* Flexible documents suit chat logs, but our data has clear relationships, and SQL joins are a skill worth practicing.
  - *H2 / SQLite:* Good for tests, but not representative of a real deployment.
- **What we store vs. not:** Uploaded PDF files go on disk (or cloud storage). The database stores only metadata and extracted text.

### 3.10 Spring Data JPA + Hibernate ✅
- **What:** Maps Java classes to database tables, so we write less SQL.
- **Why we chose it:** Standard in Spring projects. Repository interfaces give us CRUD operations with almost no code.
- **Alternatives:** Plain JDBC / JdbcTemplate, MyBatis, jOOQ.
- **Why not those:** JDBC means a lot of repetitive code. MyBatis and jOOQ give finer SQL control, but JPA is more common and quicker to start with.
- **Related:** We'll use Flyway or Liquibase later if we want versioned schema changes. Otherwise `ddl-auto=update` is acceptable for development only.

### 3.11 Spring Security + JWT 🔜
- **What:** Login and protection of endpoints. After login, the server gives a signed token (JWT). The browser sends it with each request.
- **Why we chose it:** JWT is stateless, so the server doesn't need to store sessions. It works well with a separate React frontend. Passwords will be hashed with BCrypt, never stored in plain text.
- **Alternatives:** Server sessions with cookies, OAuth2 / "Login with Google", Auth0 / Firebase Auth, Keycloak.
- **Why not those:**
  - *Sessions:* Simple and secure, but need server-side state, which complicates scaling.
  - *OAuth2 / third-party auth:* Good for production, but hides the learning. We may add Google login later as an extra.
  - *Keycloak/Auth0:* Overkill for a learning project, but worth knowing for enterprise.
- **Known trade-off:** JWTs can't be revoked easily before they expire. We keep expiry short and can add refresh tokens.

### 3.12 React 🔜
- **What:** JavaScript library for building the user interface from components.
- **Why we chose it:** Largest ecosystem and job market. Component structure suits a chat UI, upload forms, and history lists.
- **Alternatives:** Angular, Vue, Svelte, server-rendered pages (Thymeleaf).
- **Why not those:**
  - *Angular:* Full framework with more built-in structure, but heavier to learn.
  - *Vue / Svelte:* Excellent and simpler, but smaller job market.
  - *Thymeleaf:* Keeps everything in Java, but less interactive, and a separate frontend teaches how real apps are structured.

### 3.13 Vite 🔜
- **What:** Tool that creates and runs the React project.
- **Why we chose it:** Very fast startup and instant updates. It is the current recommended way to start React projects.
- **Alternatives:** Create React App (CRA), Next.js, Parcel.
- **Why not those:**
  - *CRA:* No longer maintained and slow, so React's own docs no longer recommend it.
  - *Next.js:* Powerful (server rendering, routing) but adds concepts we don't need, since our backend is Spring Boot.

### 3.14 LLM API (AI provider) ✅
- **What:** An external AI model we call over HTTPS to chat, summarize, write emails, check resumes, and detect spam.
- **Why we chose it:** Hosted models give high quality without GPUs or training. Most of our features are the same call with different **prompts**.
- **Alternatives:** OpenAI, Google Gemini, local models (Ollama, Llama), training our own model, Hugging Face.
- **Why not those:**
  - *Local models:* Free and private, but need a strong computer, and quality is lower on typical laptops.
  - *Training our own:* Needs large data and money, and isn't needed for these tasks.
  - *Which hosted provider:* To be finalized. We'll hide the provider behind an `AiService` interface so we can swap providers without changing controllers.
- **Spam detector note:** A traditional ML classifier (Naive Bayes, etc.) is cheaper at scale, but an LLM needs no training data, so it's the faster option for this project. We note the trade-off in interviews.
- **Security rule:** The API key lives in an environment variable and is never committed. The browser never sees it, because only the backend calls the AI.
- **Decision (Day 9):** We started with the Grok API. It has a single HTTPS endpoint, simple authentication (an API key header), clear documentation, and models with large context windows, which should help with PDF chat later. I have not benchmarked providers against each other yet. Because the provider sits behind `AiService`, comparing OpenAI, Gemini, or a local model means writing one new class, and the result of that comparison will be recorded here.
- **Dev tip:** Use the small, cheap model tier for development, and the fake provider for work that doesn't need real AI.

### 3.15 Apache PDFBox 🔜
- **What:** Java library to read text out of PDF files.
- **Why we chose it:** Free (Apache 2.0 license), pure Java, easy to add as a dependency.
- **Alternatives:** Apache Tika, iText, sending the PDF directly to an AI model that accepts files, OCR (Tesseract).
- **Why not those:**
  - *iText:* Powerful, but its open-source license (AGPL) has restrictions that matter for commercial use.
  - *Tika:* Handles many file types, which is nice later (DOCX), but is heavier than we need for PDFs.
  - *Direct PDF to AI:* Possible with some providers, but extracting text ourselves lets us store it, chunk it, and control cost.
  - *OCR:* Needed only for scanned PDFs. We'll mention it as a future improvement.

### 3.16 Chat with PDF: chunking + retrieval (RAG) 🔜
- **What:** Split the PDF text into chunks, find the chunks relevant to the question, and send only those to the AI with the question.
- **Why:** Sending an entire long PDF with every message is slow and expensive, and may exceed the model's context limit.
- **Plan (start simple, improve later):**
  1. *Version 1:* Small PDFs go to the AI whole.
  2. *Version 2:* Chunk the text and rank chunks by keyword match.
  3. *Version 3 (stretch):* Embeddings plus a vector store.
- **Alternatives for the vector part:** pgvector, Pinecone, Chroma, Elasticsearch.
- **Why not now:** A vector database adds infrastructure and learning load. We prove the product works with simple retrieval first.
- **Interview line:** "I started with the simplest thing that works and upgraded only when I could explain why."

### 3.17 Secrets: environment variables 🔜
- **What:** API keys and passwords live outside the code.
- **Why:** Committed secrets get stolen by bots within minutes. `.env` and `application-local.properties` are in `.gitignore`.
- **Alternatives:** Hard-coding (never), Vault, cloud secret managers.
- **Why not those:** Vault and cloud managers are the right answer in production, but env variables are enough here.

### 3.18 Styling ❓
- Options: Tailwind CSS, Material UI, plain CSS, Bootstrap.
- To be decided on Day 2 or 3. We'll update this file.

### 3.19 Deployment ❓
- Options: Docker + Render / Railway / AWS, or a VPS.
- To be decided in Week 8.

### 3.20 CORS configuration ✅
- **What:** CORS (Cross-Origin Resource Sharing) is a browser rule. A page from one address (localhost:5173) may read responses from another (localhost:8080) only if that server allows it.
- **Why we chose it:** A global `WebMvcConfigurer` allows only our React origin on `/api/**`. The rule lives in one place instead of on every controller.
- **Alternatives:** `@CrossOrigin` on each controller, a Vite dev proxy, allowing all origins (`*`).
- **Why not those:**
  - *`@CrossOrigin` per controller:* Easy to forget on new controllers.
  - *Vite proxy:* Good dev-only trick, but it hides the problem and does not exist in production.
  - *Allow `*`:* Works, but unsafe once we use login tokens.
- **Trade-off / note:** When we add Spring Security, CORS must also be enabled there, or login requests will be blocked.
- **Interview line:** "CORS is enforced by the browser, not the server. My request reached Spring Boot, but the browser blocked the response until the server sent the allowed-origin header."

### 3.21 fetch (built-in) vs Axios ✅
- **What:** `fetch` is the browser's built-in way to call an API.
- **Why we chose it:** No extra library, and it teaches how HTTP calls really work.
- **Alternatives:** Axios, TanStack Query (React Query), SWR.
- **Why not those:**
- *Axios:* Nicer syntax and automatic JSON handling, but an extra dependency. We may adopt it when we add login tokens.
- *TanStack Query:* Great for caching and loading states, but adds concepts. A possible upgrade later.
- **Update (Day 7):** We stayed with `fetch` and wrapped it in a `request()` helper (see 3.42) instead of adopting Axios.

### 3.22 JavaScript vs TypeScript ✅
- **What:** We use plain JavaScript (JSX) for the React code.
- **Why we chose it:** One new thing at a time, since we're already learning React and Spring Boot together.
- **Alternatives:** TypeScript.
- **Why not TypeScript now:** It catches many bugs and is common in jobs, but adds setup and syntax to learn. A good stretch goal to migrate to later.

### 3.23 ESLint (linter) ✅
- **What:** A tool that checks JavaScript/React code for likely mistakes and style problems as I write it.
- **Why we chose it:** The long-standing standard in React projects, so the skill carries over to most teams and job listings.
- **Alternatives:** Oxlint, Biome, no linter.
- **Why not those:**
  - *Oxlint / Biome:* Much faster and promising, but newer and less common in teams today. Speed does not matter at our project size.
  - *No linter:* Bugs like unused variables would only show up at runtime.
- **Interview line:** "I used ESLint so common mistakes were caught as I typed, not in the browser."

### 3.24 JSON + DTOs as Java records ✅
- **What:** React and Spring Boot exchange JSON. DTOs (`ChatRequest`, `ChatResponse`) describe the exact shape of each message. We write them as Java `record`s.
- **Why we chose it:** A DTO makes the API contract explicit. Records are short and immutable, which fits one-way data carriers.
- **Alternatives:** `Map<String, Object>`, database entities used directly as API objects, Lombok `@Data` classes.
- **Why not those:**
  - *Maps:* No type safety, so typos only fail at runtime.
  - *Entities as API objects:* Leaks database structure (like password hashes) and couples the API to the database.
  - *Lombok classes:* Work fine, but records need no library for simple carriers.
- **Interview line:** "I use DTOs so the API contract is separate from the database model, and records keep them short and immutable."

### 3.25 Controller → Service layering ✅
- **What:** The controller handles HTTP (receive request, return status). The service holds the business logic.
- **Why we chose it:** Each class has one job. The service can later call the AI API or the database without changing HTTP code, and it can be tested without a web server.
- **Alternatives:** Everything in the controller, or heavier architectures (hexagonal / clean architecture) from day one.
- **Why not those:**
  - *All in controller:* Becomes unreadable once AI and database code arrive.
  - *Full clean architecture:* Too heavy for the current size. We add structure when there is a reason.
- **Related:** Constructor injection (Spring passes `ChatService` into the controller) instead of `@Autowired` on fields, because dependencies are explicit and easy to test.
- **Interview line:** "The controller only deals with HTTP. The service is where the AI call will live, so swapping providers won't touch the controller."

### 3.26 React state with useState ✅
- **What:** The chat screen keeps messages, input text, and a loading flag in `useState`.
- **Why we chose it:** The state belongs to one screen, so built-in hooks are enough.
- **Alternatives:** Redux Toolkit, Zustand, React Context, TanStack Query.
- **Why not those:**
  - *Redux / Zustand:* Useful when many screens share state. We have one screen so far.
  - *Context:* We'll likely use it for the logged-in user later.
  - *TanStack Query:* Helpful for caching server data, a possible upgrade when we load chat history.
- **Interview line:** "I add a state library only when state is shared across screens. Until then, useState keeps things simple."

### 3.27 Chat data model: Conversation + Message ✅
- **What:** Two tables. `conversations` (id, title, created_at) and `messages` (id, conversation_id, role, content, created_at).
- **Why we chose it:** One chat has many messages, which is a classic one-to-many relationship. Separate tables let us list chats cheaply and load one chat's messages when it is opened. It also gives us a place to attach the user later.
- **Alternatives:** A single messages table with a loose chat-id string, one JSON column holding the whole chat, a document database (MongoDB).
- **Why not those:**
  - *Single table:* No clean place for chat-level data (title, owner).
  - *JSON column:* Rewriting the whole chat on every message, and hard to query.
  - *MongoDB:* Reasonable for chat logs, but our data is relational and SQL joins are a skill worth practicing.
- **Details worth mentioning:** `role` is stored as a string enum (`EnumType.STRING`), so reordering the enum cannot silently corrupt old rows. `content` is `TEXT`, since the default 255-character column would fail on long messages. Timestamps are `Instant` (UTC). The relation is `LAZY` to avoid loading data we don't need.
- **Interview line:** "I stored enums as strings so a reordered enum could never change the meaning of existing data."
- **Update (Day 8):** `conversations` now has a required `user_id` (see 3.46).

### 3.28 Dedicated DB user + git-ignored credentials ✅
- **What:** The app connects as `docmind_user`, which has rights only on the `docmind` database. Its password is in `application-local.properties`, ignored by Git. A `.example` copy shows the format.
- **Why we chose it:** Least privilege limits the damage if the app is compromised, and a committed password is leaked permanently, because Git history keeps it.
- **Alternatives:** Connecting as root, hard-coding the password, environment variables, a secrets manager (Vault, cloud).
- **Why not those:**
  - *root:* Full control of the whole server.
  - *Hard-coded:* Ends up on GitHub.
  - *Env vars / secrets manager:* The right choice for deployment, and we'll switch when we deploy. A local profile file is the simplest safe option for development.
- **Interview line:** "I verified with `git check-ignore` that the credentials file was ignored before my first commit."

### 3.29 Schema management: ddl-auto=update ✅
- **What:** Hibernate creates and updates tables from the entity classes at startup.
- **Why we chose it:** Fast iteration while the model is changing every day.
- **Alternatives:** Flyway or Liquibase (versioned SQL migrations), hand-written SQL scripts.
- **Why not those (yet):** Migrations are the correct approach for production, because every schema change is reviewed, versioned, and repeatable. `ddl-auto=update` can't safely rename or drop columns and gives no history. Planned: switch to Flyway before deployment.
- **Interview line:** "ddl-auto=update is for development. For production I'd use Flyway migrations so schema changes are versioned and reviewable."

### 3.30 No transaction around slow calls ✅
- **What:** `ChatService.chat()` saves the user message, then calls the (slow) reply generator, then saves the reply. Each save is its own short transaction.
- **Why we chose it:** A transaction holds a database connection. Wrapping a 1 to 5 second AI call in one would tie up connections, and under load the connection pool would run out.
- **Alternatives:** `@Transactional` on the whole method.
- **Trade-off:** If the AI call fails, the user's message is already saved (acceptable, and even useful for retrying), but the chat has no reply.
- **Interview line:** "I keep transactions short and never hold one open across an external API call."

### 3.31 Known limitations (temporary) ✅
- **Active chat is remembered in the browser's localStorage.** This is a stopgap until login exists, when the server will list each user's own chats.
- **Conversation ids are sequential and unprotected.** Anyone who guesses `/api/conversations/2/messages` can read it. This is fixed in Week 2 by tying conversations to users and checking ownership on every request.
- **Interview line:** "I know the endpoint has no authorization yet. It's the first thing the login work fixes."

### 3.32 Password hashing with BCrypt ✅
- **What:** Passwords are run through BCrypt before saving. Only the 60-character hash is stored.
- **Why we chose it:** BCrypt is deliberately slow (so brute-forcing is expensive) and adds a random salt (so identical passwords produce different hashes). It's the default in Spring Security and widely trusted.
- **Alternatives:** Argon2, scrypt, PBKDF2, SHA-256 or MD5 (plain hashes), storing the password as is.
- **Why not those:**
  - *Plain MD5/SHA-256:* Built to be fast, so attackers can test billions of guesses per second. Not suitable for passwords.
  - *Storing as-is:* One database leak exposes every account (and people reuse passwords elsewhere).
  - *Argon2 / scrypt:* Stronger against specialized hardware and a fine upgrade. Spring supports them, and BCrypt is the safe, well-understood default.
- **Known limit:** BCrypt only uses the first 72 bytes, so we cap passwords at 72 characters.
- **Interview line:** "I never store or log passwords. BCrypt is slow on purpose and salts each hash, so even two users with the same password have different hashes."

### 3.33 Input validation with Bean Validation ✅
- **What:** Rules like `@Email`, `@NotBlank`, and `@Size` are written on the request DTO, and `@Valid` in the controller enforces them. A broken rule gives 400 Bad Request.
- **Why we chose it:** The rules live beside the data they describe, and the controller stays short.
- **Alternatives:** Manual `if` checks in the controller or service, validating only in React.
- **Why not those:**
  - *Manual checks:* Repetitive and easy to forget.
  - *Only in React:* Users (or attackers) can call the API directly, so the server must always validate.
- **Interview line:** "Frontend validation is for user experience. Backend validation is for security."

### 3.34 Email handling and duplicate protection ✅
- **What:** Emails are trimmed and lowercased before saving. A pre-check returns 409 Conflict, and a database `unique` constraint is the safety net for simultaneous requests.
- **Why we chose it:** Without normalizing, `A@x.com` and `a@x.com` could become two accounts. The pre-check alone has a race condition, and the constraint closes it.
- **Alternatives:** Pre-check only, constraint only.
- **Why not those:** Pre-check only can still produce duplicates when two requests arrive at the same moment. Constraint only gives an ugly 500 error unless we catch it.
- **Trade-off:** Returning "email already registered" tells an attacker which emails have accounts (user enumeration). Many apps accept this for better user experience. Rate limiting on this endpoint is a future improvement.

### 3.35 Security configuration decisions ✅
- **What:** A `SecurityFilterChain` that turns on CORS, disables CSRF, uses stateless sessions, opens `/api/auth/**` and `/api/hello`, and requires authentication for everything else.
- **Why we chose it:** The API will use tokens in headers, not cookies. CSRF attacks rely on the browser automatically sending cookies, so with a header token they don't apply. Stateless means no server-side session to manage.
- **Alternatives:** Cookie sessions with CSRF protection, Spring's default form login, HTTP Basic.
- **Why not those:**
  - *Cookie sessions + CSRF:* A valid approach (and often simpler for same-site apps), but our React app is a separate origin and we want a mobile-friendly API.
  - *Form login / Basic:* Meant for server-rendered pages, not a JSON API with a separate frontend.
- **Reminder:** This is the "CORS must also be enabled in Spring Security" note from 3.20, now in practice. Without `.cors(...)`, the browser's preflight request is blocked.
- **Gotcha:** `/error` must be permitted. Spring Boot forwards every error response (400, 409, 404) to `/error`. If Security locks it, all errors are replaced by a generic 403 and the real cause is hidden.

### 3.36 Known limitations (updated) ✅
- **Chat endpoints are still open** on the backend. The login gate exists only in the React UI, so anyone can call `/api/chat` and `/api/conversations/**` directly. Fix: tie conversations to users and require a token (Day 8).
- **Token in localStorage** is readable by injected scripts (see 3.40).
- **No refresh tokens:** users must log in again after 60 minutes.
- **If the backend is down on page load,** the user sees the login screen even with a saved token.
- **No email verification and no rate limiting** on register or login.

### 3.37 JWT with Spring's built-in resource server ✅
- **What:** After login, the server returns a signed token. Spring Security's `oauth2ResourceServer().jwt()` reads `Authorization: Bearer <token>` on each request and checks the signature and expiry. We create tokens with `NimbusJwtEncoder`.
- **Why we chose it:** It's maintained by the Spring team, needs no hand-written filter (less security code to get wrong), and the token checking is the same code real production apps use.
- **Alternatives:** The `jjwt` library with a custom filter, Auth0 `java-jwt`, server-side sessions, an external identity provider (Keycloak, Auth0, Firebase).
- **Why not those:**
  - *jjwt / java-jwt with a custom filter:* Works and is popular in tutorials, but we would own the filter code and its edge cases.
  - *Sessions:* Simple, but they need server-side state, and our React app and API are on different origins.
  - *External provider:* The right choice for many production systems, but it hides the mechanics we are here to learn. A possible later upgrade.
- **Algorithm:** HS256 (one shared secret signs and verifies). That's fine for a single backend. With several services, we'd move to RS256 or ES256 (a private key signs, a public key verifies).
- **Interview line:** "A JWT is signed, not encrypted. I only put the user id and timestamps in it, and I verified the signature and expiry on every request."

### 3.38 Token contents and lifetime ✅
- **What:** The token holds the issuer, issue time, expiry, and the user id as `sub`. It lasts 60 minutes.
- **Why we chose it:** The payload is readable by anyone, so it carries the minimum. A short lifetime limits the damage if a token leaks, because we cannot easily revoke a JWT before it expires.
- **Alternatives:** Long-lived tokens (days or weeks), refresh tokens, a server-side token blacklist.
- **Why not those (yet):** Long-lived tokens are risky. Refresh tokens add flow complexity and are a planned improvement. A blacklist brings back server-side state, which is what JWTs avoid.
- **Known limit:** Users get logged out after an hour until refresh tokens exist.
- **Open question:** Where should React store the token? `localStorage` is simple, but any injected script can read it. An httpOnly cookie is safer against that, but needs CSRF thought. Decision on Day 7.

### 3.39 Login error handling ✅
- **What:** Wrong password and unknown email both return the same 401 "Invalid email or password".
- **Why we chose it:** Different messages would let an attacker test which emails have accounts (user enumeration).
- **Trade-offs and limits:** Registration still reveals duplicates (409), and an unknown-email login is slightly faster than a wrong-password login, because BCrypt is only run when the user exists. Rate limiting and constant-time handling are future improvements.
- **Interview line:** "I return identical errors for both failures so the login form doesn't confirm which emails exist."

### 3.40 Where to store the JWT: localStorage (for now) ✅
- **What:** After login, the token is saved in the browser's `localStorage` and attached to each request as `Authorization: Bearer <token>`.
- **Why we chose it:** It survives refreshes and works with our stateless, header-based backend with no extra server changes.
- **Alternatives:** An httpOnly cookie, keeping the token only in JavaScript memory (plus a refresh cookie), `sessionStorage`.
- **Why not those (yet):**
  - *httpOnly cookie:* JavaScript cannot read it, so injected scripts can't steal it. But it needs backend changes (cookie handling, SameSite, CSRF protection back on).
  - *Memory only:* Safest against theft, but the user is logged out on every refresh unless refresh tokens exist.
  - *sessionStorage:* Same theft risk as localStorage, and the login is lost when the tab closes.
- **Trade-off:** Any script injected into our page (XSS) could read a localStorage token. Mitigations: React escapes output by default, we never use `dangerouslySetInnerHTML`, and tokens expire after 60 minutes.
- **Planned upgrade:** Refresh tokens in an httpOnly cookie plus a short-lived access token in memory.
- **Interview line:** "localStorage is simple but readable by injected scripts, so I limit the damage with short expiry and plan to move to an httpOnly refresh cookie."

### 3.41 Auth state with React Context ✅
- **What:** `AuthProvider` holds the current user and the `login`, `register`, and `logout` functions. Any component reads them with `useAuth()`.
- **Why we chose it:** Many parts of the app need to know "who is logged in", and Context avoids passing props through every level (prop drilling).
- **Alternatives:** Redux Toolkit, Zustand, passing props, TanStack Query for the user.
- **Why not those:** Redux or Zustand are heavier than a single, rarely changing value needs. Props would spread auth data through every component.
- **Startup behavior:** If a token is saved, the app calls `GET /auth/me` and shows "Checking your session..." until the answer arrives, so users never see a flash of the wrong screen.

### 3.42 One request() helper (no Axios) ✅
- **What:** Every API call goes through `request()`, which adds the Bearer token, throws a typed `ApiError`, maps status codes to friendly messages, and logs the user out on a 401 when a token was sent.
- **Why we chose it:** Cross-cutting rules (auth header, expiry handling, error shape) live in one file instead of every screen. This also answers the Day 2 question: we stayed with `fetch` instead of adding Axios.
- **Alternatives:** Axios with interceptors, TanStack Query, calling `fetch` everywhere.
- **Why not those:** Axios interceptors would do the same job, with an extra dependency. TanStack Query is useful later for caching chat history.
- **Details:** Login and register use `skipAuth`, so a stale token can't break them. Messages are written client-side because Spring Boot hides error text by default.

### 3.43 No React Router yet ✅
- **What:** `App` chooses between `AuthPage` and `ChatPage` using the logged-in state.
- **Why we chose it:** Two screens don't need URL routing.
- **Alternatives:** React Router, TanStack Router.
- **Why not yet:** We'll add React Router when we have more pages (chat list, PDF upload, settings) or need bookmarkable URLs.

### 3.44 Ownership checks in the service layer (IDOR prevention) ✅
- **What:** Every lookup of a conversation includes the logged-in user's id (`findByIdAndUserId`). The user id comes from the verified JWT, not from anything the client sends.
- **Why we chose it:** Conversation ids are sequential and easy to guess. Without this, any logged-in user could read or write another user's chats (Insecure Direct Object Reference, part of OWASP's #1 risk, Broken Access Control). Putting the rule in the query means a forgotten check can't leak data.
- **Alternatives:** Method security (`@PreAuthorize`) with an ownership expression, a check only in the controller, unguessable ids (UUIDs) as the only protection, database row-level security.
- **Why not those:**
  - *`@PreAuthorize`:* A fine approach and worth learning, but it still needs the owner lookup, and the query-level check is simpler to read.
  - *Controller-only check:* Every new controller must remember it.
  - *UUIDs alone:* Make guessing hard, but they are not authorization. Ids leak through logs, links, and screenshots.
  - *Row-level security:* Strong, but MySQL doesn't support it natively.
- **Interview line:** "I never trust an id from the client. Every query includes the owner from the verified token, so guessing another user's id returns nothing."

### 3.45 404 instead of 403 for other people's chats ✅
- **What:** If a chat exists but belongs to someone else, the API answers exactly as if it didn't exist (404).
- **Why we chose it:** A 403 would confirm that the id exists, which helps an attacker map out other users' data.
- **Alternatives:** 403 Forbidden.
- **Trade-off:** 403 is more precise for debugging and is common for resources that are not secret. For private user data, 404 is the safer default.

### 3.46 Conversation belongs to a user (data model update) ✅
- **What:** `conversations.user_id` is a required foreign key to `users`. The model is now User → many Conversations → many Messages.
- **Migration choice:** Old test conversations had no owner, so we deleted them in development before adding the required column. Adding a required column to a table with rows fails or forces fake owner ids.
- **Alternatives:** Add the column as nullable and backfill, assign old chats to one user, versioned migrations (Flyway).
- **Why not those:** The data was throwaway test data. In production we'd use a Flyway migration that adds the column, backfills it, and then makes it required.
- **Interview line:** "I deleted dev data instead of inventing owners. In production I'd write a migration that backfills first."

### 3.47 AiService interface with a fake and a real implementation ✅
- **What:** `ChatService` talks to an `AiService` interface. `FakeAiService` and `GrokAiService` implement it, and the property `app.ai.provider` chooses one at startup.
- **Why we chose it:** The rest of the app doesn't know which provider is used, so swapping or adding providers touches one class. The fake provider lets us develop, demo, and test with no key, no cost, and no internet.
- **Alternatives:** Calling the provider directly from `ChatService`, using the provider's official SDK, using Spring AI.
- **Why not those:**
  - *Direct call:* Locks the whole app to one provider and makes testing cost money.
  - *Provider SDK:* Convenient, but it ties our code to one vendor and hides the HTTP details we are here to learn.
  - *Spring AI:* A real option that already offers a provider-neutral API. I want to understand the raw HTTP first, then evaluate Spring AI as an upgrade.
- **Interview line:** "I hid the AI behind an interface, with a fake implementation for tests and a real one for production, selected by configuration."

### 3.48 RestClient for calling the AI API ✅
- **What:** Spring's `RestClient` sends the HTTPS request, with explicit connect and read timeouts.
- **Why we chose it:** It's the current synchronous HTTP client in Spring, simple to read, and it needs no extra dependency.
- **Alternatives:** `WebClient` (reactive), `RestTemplate` (older), the JDK `HttpClient` directly, a provider SDK.
- **Why not those:** `WebClient` brings in reactive programming we don't use. `RestTemplate` is in maintenance mode. A raw `HttpClient` needs manual JSON handling.
- **Note:** The JSON response is read into small records with `@JsonIgnoreProperties(ignoreUnknown = true)`, so new fields from the provider don't break us.

### 3.49 Conversation memory: resend the last 20 messages ✅
- **What:** The AI API is stateless. On each call we send the system prompt plus up to the last 20 messages of that conversation.
- **Why we chose it:** It's the simplest approach that works, and a cap keeps cost and speed predictable.
- **Alternatives:** Send the whole history, summarize older messages, store embeddings and retrieve the relevant ones.
- **Why not those:** The whole history grows cost and latency without bound and can exceed the model's limit. Summaries and retrieval are good upgrades, but need more machinery.
- **Trade-off:** In a long chat the AI forgets anything older than 20 messages.
- **Tidying:** The adapter drops a leading assistant message and merges consecutive messages from the same role, since an earlier failed call can leave two user messages in a row.

### 3.50 Failure handling, timeouts, and cost controls ✅
- **What:** Provider errors, network failures, and timeouts become an `AiException`. `ChatService` turns that into `502 Bad Gateway`, and React shows a friendly message. Details go to the logs, without secrets. Calls have a 10-second connect timeout and a 60-second read timeout.
- **Why we chose it:** AI calls are slow, and they fail in ways our own database never does. Users should not see raw provider errors, and a hung request should not hang our server.
- **Cost controls:** `max_tokens`, a 4000-character message limit, a 20-message history cap, login required, and a spending limit set at the provider.
- **Not done yet:** rate limiting per user, retries with backoff, and streaming replies.
- **Known gap:** If the AI fails, the user's message is already saved without a reply. A retry would add a second copy of it.
- **Interview line:** "I treat the AI call as an unreliable dependency: timeouts, an error mapped to 502, no secrets in logs, and a fake implementation so everything else still works if the provider is down."

### 3.36 Known limitations: add these bullets ✅
- **Replies show raw text.** Markdown from the AI (bold, lists, code) isn't rendered yet.
- **No streaming.** The user waits for the full reply before seeing anything.
- **No per-user rate limiting or retry button.**

---

## 4. Common Interview Questions (and how to answer)

**Q: Why Java for an AI project?**
A: The AI model is reached through an API, so the backend language doesn't affect AI quality. I picked Java/Spring Boot to build secure, structured, production-style backend skills.

**Q: How do you keep the API key safe?**
A: It's stored in an environment variable, ignored by Git, and only the backend uses it. The React app never sees it.

**Q: How does login work?**
A: Passwords are hashed with BCrypt. After login, the server returns a signed JWT. React sends it in the `Authorization` header, and a Spring Security filter validates it on each request.

**Q: How does Chat with PDF work?**
A: The backend extracts text with PDFBox, splits it into chunks, selects the relevant chunks for the question, and sends those plus the question to the LLM.

**Q: What would you improve with more time?**
A: Streaming responses, embeddings for better retrieval, refresh tokens, rate limiting, caching, tests, and Docker deployment.

**Q: What was the hardest bug?**
A: My Spring Boot app started, then crashed with "declared package does not match expected package." I read the stack trace to the last `Caused by:` line and saw I had typed the Group Id as `com.docmin` instead of `com.docmind`. Renaming the folders did not update the `package` line inside each file, so I used a search command to find the leftovers, fixed them, and ran `mvnw clean` to remove old compiled files.

**Q: How do you store chat history?**
A: Two tables: conversations and messages, linked one-to-many. The service saves the user message, calls the AI without holding a transaction open, then saves the reply. React loads history by conversation id.

**Q: How do you store passwords?**
A: Hashed with BCrypt, which is one-way, slow on purpose, and salted. I verified two users with the same password get different hashes. Passwords are never logged or returned by the API, because I return a DTO that has no password field.

**Q: Why disable CSRF?**
A: CSRF abuses cookies that browsers send automatically. My API will authenticate with a token that JavaScript adds to a header, so a malicious site can't trigger authenticated requests. If I used cookie sessions, I would keep CSRF protection on.

**Q: How does JWT authentication work in your app?**
A: Login checks the password against the BCrypt hash and returns a signed token holding the user id and expiry. React sends it in the Authorization header, and Spring Security verifies the signature and expiry on every request. The token is signed, not encrypted, so it carries nothing private.

**Q: What is the downside of JWTs?**
A: They cannot be revoked easily before they expire, so I keep the lifetime short and plan refresh tokens.

**Q: Where do you store the JWT in the browser, and what are the risks?**
A: In localStorage for now. The risk is that an injected script (XSS) could read it, so I rely on React's output escaping, avoid dangerouslySetInnerHTML, and keep tokens short-lived. The planned upgrade is a refresh token in an httpOnly cookie.

**Q: How does your frontend handle an expired token?**
A: All API calls go through one request() helper. If the server returns 401 for a request that carried a token, it clears the token and logs the user out, so the app returns to the login screen.

**Q: How do you stop one user from reading another user's chats?**
A: Every conversation query includes the owner's id taken from the verified JWT, using findByIdAndUserId. If the chat isn't theirs, the query finds nothing and the API returns 404. I tested it with two accounts: one user got 404 trying to read or write into the other's chat.

**Q: What is IDOR?**
A: Insecure Direct Object Reference: the app uses an id from the request to fetch data without checking that the user is allowed to see it. Guessing sequential ids then exposes other people's data. The fix is to authorize every lookup, not to hide the ids.

**Q: How does the AI remember the conversation?**
A: It doesn't. The AI API is stateless, so on every request I send the system prompt plus the last 20 messages from my database. A new conversation starts with no history, which I tested.

**Q: How would you switch to a different AI provider?**
A: The chat code depends on an AiService interface. I'd write one new implementation class and change one setting, so no controller or screen changes.

**Q: What happens if the AI provider is down or slow?**
A: Timeouts stop the call from hanging, the failure becomes a 502 with a friendly message, and the details are logged without secrets. The rest of the app, including history and login, keeps working.

---

## 5. Challenges I Faced and Fixed (add as you go)

| Day | Problem | Cause | Fix |
|---|---|---|---|
| 0 | `npm` blocked in PowerShell | Windows script execution policy | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| 0 | Project inside OneDrive | OneDrive syncs thousands of `node_modules` files | Moved project to `C:\Projects` |
| 0 | `init.defaultBranch` showed `master` and `main` | Two config levels; last one wins | Left as is, since `main` overrides |
| 1 | Spring Initializr created `backend/backend/pom.xml` | Initializr makes its own subfolder named after the Artifact Id | Moved all files up one level with `Move-Item` and deleted the empty folder |
| 1 | `Unresolved compilation problem: declared package does not match expected package` | Group Id typed as `com.docmin` (missing "d"), so folders and package lines did not match | Fixed groupId in pom.xml, renamed folders, updated the `package` line in each file, ran `mvnw clean` |
| 2 | Browser showed "Failed to fetch" and a CORS error in the Console | Frontend (5173) and backend (8080) are different origins. The request reached Spring Boot, but the browser blocked the response because the backend did not allow 5173 | Added `WebConfig` with `addCorsMappings` allowing `http://localhost:5173` |
| 4 | MySQL Workbench query would not run (Ctrl+Shift+Enter did nothing, toolbar icons greyed out) | The query tab was opened without a connection to the server | Opened the tab from inside the "Local instance MySQL80" connection (Ctrl+T) and ran it with the toolbar lightning bolt |
| 5 | Duplicate-email and validation errors returned 403 instead of 409 / 400 | Spring Boot forwards errors to `/error`, and Security's `anyRequest().authenticated()` rule blocked that path, hiding the real status code | Added `.requestMatchers("/error").permitAll()` to the security config |
| 6 | App failed to start: `Could not resolve placeholder 'app.jwt.secret'` | The secret was missing from `application-local.properties` (or misspelled) | Added `app.jwt.secret` with a generated random value |
| 6 | App refused to start: secret too short | HS256 needs at least 32 bytes, and my secret was shorter | Generated a new 48-byte random secret |
| 6 | `/api/auth/me` returned 200 without a token | The wildcard `/api/auth/**` was still permitted from Day 5 | Replaced it with specific matchers for POST `/register` and `/login` only |
| 6 | Compile errors on JWT classes | The dependency had not downloaded yet | Synchronized the Maven project and ran `.\mvnw clean` |
| 7 | White page: `Failed to resolve import "./auth/AuthContext"` | Wrong folder or file name (case matters) | Fixed the folder and file names to match the import paths |
| 7 | Browser blocked requests that carry the `Authorization` header (CORS preflight error) | The backend CORS config did not explicitly allow the header | Added `.allowedHeaders("*")` in `WebConfig` and restarted the backend |
| 7 | Expired token caused 401 even on open endpoints like `/api/chat` | The bearer-token filter validates any token that is sent, even to `permitAll` endpoints | Skipped the token for login/register and made `request()` clear the token and log out on a 401 |
| 8 | Backend would not start after adding `user_id` (null or foreign key error) | The `conversations` table still had old rows with no owner, and the new column is required | Deleted old rows from `messages` and `conversations`, then restarted |
| 8 | Everything returned 401 in PowerShell tests | The saved token was older than 60 minutes or missing | Logged in again to get a fresh token |
| 9 | Backend failed to start: no bean of type `AiService` | `app.ai.provider` was missing or misspelled, so neither implementation was created | Set it to `fake` or `grok` exactly |
| 9 | AI call failed with HTTP 401 | The API key was wrong, or was pasted with quotes or spaces | Pasted the key again with no quotes or spaces and restarted |
| 9 | AI call failed with HTTP 404 about the model | The model ID in `app.ai.model` was wrong or retired | Copied a current model ID from the provider's models page |
| 9 | AI call failed with a billing or credit error | The provider account had no credit | Added credit and set a spending limit in the console |

---

## 6. Template for New Decisions

Copy this each time we add a tool:

```markdown
### X.X Tool name (status)
- **What:**
- **Why we chose it:**
- **Alternatives:**
- **Why not those:**
- **Trade-offs / Interview line:**
```

---

## 7. Daily Update Log

| Day | What changed in this file |
|---|---|
| 0 | Created the document and recorded the Git/GitHub setup decisions |
| 1 | Marked Spring Boot, Maven, Lombok, DevTools, Spring Web as in use; added 2 challenges |
| 2 | Marked React and Vite as in use; added CORS, fetch, JavaScript, and ESLint decisions; added the CORS challenge |
| 3 | Added JSON/DTO, controller-service layering, and React state decisions |
| 4 | Marked MySQL and JPA as in use; added data model, credentials, schema management, transactions, and known-limitations decisions |
| 5 | Added BCrypt, Bean Validation, email handling, security configuration decisions, and updated the known limitations |
| 5 | Duplicate email and validation errors all returned 403 instead of 409 / 400 | Spring forwards errors internally to `/error`, and Spring Security's `anyRequest().authenticated()` rule blocked that path, hiding the real status code | Added `.requestMatchers("/error").permitAll()` to the security config |
| 6 | Marked Spring Security + JWT as in use; added JWT library choice, token lifetime, login error handling; updated known limitations |
| 7 | Added token storage, auth Context, request helper, and no-router decisions; updated fetch-vs-Axios note and known limitations |
| 8 | Added ownership model, IDOR prevention, 404-vs-403 and data migration decisions; closed the temporary open endpoints |
| 9 | Chose the AI provider; added AiService interface, RestClient, memory strategy, and failure/cost-control decisions; updated known limitations |