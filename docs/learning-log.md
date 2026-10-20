# Learning Log

## Day 0
- Installed Git, Node, Java 21, MySQL
- Created the GitHub repo and folder structure
- add → commit → push

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