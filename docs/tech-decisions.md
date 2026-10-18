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

### 3.9 MySQL 🔜
- **What:** Relational database storing users, chats, messages, and document metadata.
- **Why we chose it:** Our data is structured and related (a user has many chats, a chat has many messages), which fits tables and foreign keys. MySQL is free, widely used, and has good Spring support. Workbench makes inspection easy.
- **Alternatives:** PostgreSQL, MongoDB, H2 (in-memory), SQLite.
- **Why not those:**
  - *PostgreSQL:* Equally valid, and has better built-in vector search (pgvector). It's a strong candidate if we add embeddings later. MySQL was already installed and is very common in job listings.
  - *MongoDB:* Flexible documents suit chat logs, but our data has clear relationships, and SQL joins are a skill worth practicing.
  - *H2 / SQLite:* Good for tests, but not representative of a real deployment.
- **What we store vs. not:** Uploaded PDF files go on disk (or cloud storage). The database stores only metadata and extracted text.

### 3.10 Spring Data JPA + Hibernate 🔜
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

### 3.14 LLM API (AI provider) 🔜
- **What:** An external AI model we call over HTTPS to chat, summarize, write emails, check resumes, and detect spam.
- **Why we chose it:** Hosted models give high quality without GPUs or training. Most of our features are the same call with different **prompts**.
- **Alternatives:** OpenAI, Google Gemini, local models (Ollama, Llama), training our own model, Hugging Face.
- **Why not those:**
  - *Local models:* Free and private, but need a strong computer, and quality is lower on typical laptops.
  - *Training our own:* Needs large data and money, and isn't needed for these tasks.
  - *Which hosted provider:* To be finalized. We'll hide the provider behind an `AiService` interface so we can swap providers without changing controllers.
- **Spam detector note:** A traditional ML classifier (Naive Bayes, etc.) is cheaper at scale, but an LLM needs no training data, so it's the faster option for this project. We note the trade-off in interviews.
- **Security rule:** The API key lives in an environment variable and is never committed. The browser never sees it, because only the backend calls the AI.

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
A: *(Fill this in as you go. Real stories are your best interview material.)*

---

## 5. Challenges I Faced and Fixed (add as you go)

| Day | Problem | Cause | Fix |
|---|---|---|---|
| 0 | `npm` blocked in PowerShell | Windows script execution policy | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| 0 | Project inside OneDrive | OneDrive syncs thousands of `node_modules` files | Moved project to `C:\Projects` |
| 0 | `init.defaultBranch` showed `master` and `main` | Two config levels; last one wins | Left as is, since `main` overrides |

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
