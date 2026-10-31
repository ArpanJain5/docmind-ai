# Learning Log

## Day 0
- Installed Git, Node, Java 21, MySQL
- Created the GitHub repo and folder structure
- Learned: add, commit, push
- Fixed PowerShell script error with Set-ExecutionPolicy
- Moved project out of OneDrive to C:\Projects
- Verified MySQL Workbench and VS Code extensions

## Day 1
- Created Spring Boot project (Java 21, Maven) with Spring Web, Lombok, DevTools
- Built my first REST endpoint: GET /api/hello
- Learned: a Controller is where requests come in
- Learned to read a stack trace: the last "Caused by:" line shows the real error
- Fixed 3 problems: nested folder, groupId typo, package names not matching folders

## Day 2
- Created the React frontend with Vite in the frontend folder
- Ran two servers at once: Spring Boot on 8080 and React on 5173
- Used fetch + useState + useEffect to call GET /api/hello
- Learned: CORS is enforced by the browser. The request reached Spring Boot, but the browser blocked React from reading the response until the server allowed the origin
- Fixed it with a WebConfig class in Spring Boot

## Day 3
- Built POST /api/chat that accepts and returns JSON
- Learned DTOs (Java records), the Controller → Service split, and dependency injection
- Built a chat screen in React: message list, input box, loading state, error bubble, auto-scroll
- Moved fetch code into src/api/chatApi.js
- Learned: test the backend alone first (Invoke-RestMethod), then connect the frontend
- Learned: a POST with JSON triggers a CORS preflight (OPTIONS) request

## Day 4
- Created the docmind database and a dedicated MySQL user (least privilege, utf8mb4)
- Added Spring Data JPA + MySQL driver
- Learned: entities (Java class = table), repositories (Spring writes the SQL), Conversation 1-to-many Message
- Kept the DB password out of Git using a git-ignored application-local.properties profile
- Saved every chat message to MySQL and loaded history after a page refresh
- Learned: do not keep a DB transaction open while waiting for a slow call

## Day 5
- Added Spring Security and Bean Validation
- Created the User entity and POST /api/auth/register
- Learned: passwords are hashed with BCrypt (one-way, salted), never stored or logged
- Verified in MySQL: same password for two users gives two different 60-character hashes
- Learned: Spring Security locks every endpoint by default; I opened only what is needed
- Learned: Spring Security must be told to use the CORS rules, or React is blocked again
- Handled duplicate emails two ways: a pre-check plus the database unique constraint (409 Conflict)
- Debugged: all error responses were 403 because Spring Security blocked the internal /error path. Reading the symptom (every error is 403, even validation failures) pointed to a security rule, not my controller code

## Day 6
- Added POST /api/auth/login that checks the BCrypt hash and returns a signed JWT
- Added GET /api/auth/me, which only works with a valid Bearer token
- Learned: a JWT is signed, not encrypted; I decoded my own token in PowerShell
- Learned: tampered or missing tokens give 401, and I opened only register and login to strangers
- Learned: the same error message for wrong email and wrong password avoids leaking which emails exist
- Kept the JWT secret out of Git using application-local.properties

## Day 7
- Built login/register screens in React and a Log out button
- Added a central request() helper that attaches the Bearer token and logs the user out on 401
- Learned: React Context (AuthProvider) shares "who is logged in" across the app
- Learned: a token sent to an open endpoint is still validated, so an expired token causes 401 even there
- Tested: token expiry (1 minute), tampered token, wrong password, refresh keeps me logged in
- Learned: the login gate is only in the UI today; the backend still allows chat requests without a token

## Day 8
- Added a user_id owner to conversations (many conversations per user)
- Learned IDOR / Broken Access Control: guessing ids must never expose another user's data
- Every conversation lookup now includes the owner: findByIdAndUserId
- The user id comes from the verified JWT subject, never from the request body
- Non-owners get 404, not 403, so ids of other users' chats are not revealed
- Closed the temporary open endpoints: chat and conversations now require a token
- Tested with two users in PowerShell (401, 404, 404, own chat works)
- Built a "My chats" sidebar in React

## Day 9
- Put the AI behind an AiService interface with two implementations: a fake one and a real one, chosen by app.ai.provider
- Called a real AI API with Spring's RestClient: API key header, version header, system prompt, max_tokens, and timeouts
- Learned: the AI API is stateless, so the app resends the last 20 messages each time
- Learned: keep the API key in a git-ignored file, and verify with git grep --cached before committing
- Handled failures: provider errors and timeouts become a 502 plus a friendly message, with details only in the logs
- Tested: memory within a chat, no memory in a new chat, wrong key, timeout, and a misspelled provider name
- Chose Groq's free tier because I have no budget for a paid API, and wrote the OpenAI-compatible implementation myself

## Day 10
- Added PATCH /api/conversations/{id} (rename) and DELETE /api/conversations/{id}, both with the ownership check (404 for other users' chats)
- Learned: delete in one transaction, messages first then the conversation; derived deletes need @Transactional
- Learned: PATCH must be allowed in CORS, and a 204 response has no body to parse
- A failed AI call now undoes the saved user message (and a new empty conversation), and React puts the text back in the box
- Built inline rename and delete buttons in the sidebar (window.prompt is not supported in VS Code's browser)
- Learned to rotate leaked secrets and to share only the .example config file

## Day 11
- Rendered AI replies as Markdown with react-markdown and remark-gfm (lists, tables, code blocks)
- Learned: AI output is untrusted input. Never inject it as raw HTML, because that allows XSS and token theft from localStorage
- Tested the safety: HTML and javascript: links in a reply did nothing
- Added a Copy button that copies the original Markdown text, with a fallback for browsers that restrict the Clipboard API
- Auto-scroll now only follows new messages when the reader is already at the bottom
- Extracted a MessageBubble component to keep ChatPage readable

## Day 12
- Streamed AI replies end to end: Groq stream (data: lines until [DONE]) to Spring SseEmitter to a React fetch stream reader
- Learned: SSE is one long HTTP response of named events, simpler than WebSockets for one-way data
- Learned: EventSource cannot send an Authorization header, so React reads the stream with fetch
- Learned: an HTTP status cannot change after streaming starts, so the ownership check runs before the stream and later failures become an error event
- Added a Stop button: the server notices the disconnect and undoes the exchange
- Tested the backend alone with curl.exe -N before building the UI
- Refactored ChatService so chat() and chatStreaming() share one method with one undo rule

## Day 13
- Wrote 9 unit tests for ChatService with Mockito: ownership, the 20-message history cap, the undo rule on AI failure, streaming, and a browser that leaves mid-stream
- Wrote 10 integration tests that start the whole app on a random port with H2 and the fake AI and call it over real HTTP
- Learned: MockMvc skips the error dispatch to /error, so it would not have caught my Day 5 "everything is 403" bug; real HTTP does
- Wrote 19 Vitest tests for the request() helper and the SSE stream reader
- Broke the app on purpose (removed the ownership check, locked /error, broke the 401 rule) and watched the right tests fail
- Learned: flipping the last character of a base64 JWT signature may not change its bytes, so tamper with the first character
- Kept real secrets out of the test profile and used the fake AI so tests are free and offline