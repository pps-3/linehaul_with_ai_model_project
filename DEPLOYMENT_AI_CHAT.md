# AI Chat (Groq) — What changed & how to deploy

## What changed, file by file

**Removed (the old pattern-matching engine):**
- `backend/src/main/java/com/example/linehaul/util/QuestionParser.java`
- `backend/src/main/java/com/example/linehaul/util/ParsedQuestion.java`
- `backend/src/test/java/com/example/linehaul/util/QuestionParserTest.java`

These parsed the question with regex/keywords into a `Focus`/`Action`/`id`
enum combo, then `ChatService` branched on that combo to hand-build a
string. That's the "entity focus" pattern-matching you wanted gone.

**Added:**
- `backend/src/main/java/com/example/linehaul/service/LinehaulContextBuilder.java`
  — queries Mongo (via the existing `RouteService`/`OrderService`/
  `DriverService`/`VehicleService`) and flattens the current data into a
  plain-text table. This is the *retrieval* step: no branching on what the
  user asked, it just always snapshots everything.
- `backend/src/main/java/com/example/linehaul/service/GroqClient.java`
  — a small wrapper around Groq's OpenAI-compatible
  `/chat/completions` endpoint using Spring's built-in `RestClient` (no new
  Maven dependency needed).

**Rewritten:**
- `backend/src/main/java/com/example/linehaul/service/ChatService.java`
  — now just: build the data snapshot → wrap it in a system prompt that
  says "answer only from this data" → send it + the user's question to
  Groq → return whatever it says. No `switch`, no ID/keyword detection.

**Untouched:**
- `ChatController` (still `POST /api/chat` → `{answer}`), and the entire
  React frontend (`ChatAssistant.jsx`, `chatService.js`, etc.) — the HTTP
  contract didn't change, so nothing there needed editing.

## Why this satisfies "not entity-focused"

The old code first tried to figure out *what kind of question* this was
(an ID lookup? a count? a list?) before deciding how to answer. The new
code never classifies the question at all — it just gives the model the
same table a dispatcher would look at and lets the model read it and
answer in natural language. Add a new field to `Order`/`Route`/etc. and
the assistant can answer questions about it immediately, without touching
`ChatService`.

## Getting a Groq API key

1. Go to <https://console.groq.com>, sign up (free), and open **API Keys**.
2. Create a key (starts with `gsk_...`). Groq's free tier is generous
   enough for a project like this.
3. Set it as an environment variable — **never commit it to
   `application.properties`**:
   ```bash
   export GROQ_API_KEY=gsk_your_key_here
   ```

`application.properties` already reads it via
`groq.api.key=${GROQ_API_KEY:}`.

## Model choice

Default is `openai/gpt-oss-20b` (fast, cheap, good enough for this kind of
Q&A). For noticeably better reasoning on complex questions, switch to
`openai/gpt-oss-120b`:
```bash
export GROQ_MODEL=openai/gpt-oss-120b
```
Groq's model lineup changes over time — check
<https://console.groq.com/docs/models> for the current list and retire
dates before you lock in a model for production.

## Running locally

```bash
export GROQ_API_KEY=gsk_your_key_here
cd backend
./mvnw spring-boot:run
```
```bash
cd frontend
npm install
npm run dev
```

## Deploying

The app is two independent pieces: a Spring Boot JAR (backend) and a
static Vite build (frontend). MongoDB is already hosted on Atlas per the
existing `spring.mongodb.uri`.

### Backend (Render / Railway / Fly.io / any container host)
1. Build: `./mvnw clean package` → `backend/target/linehaul-1.0.0.jar`.
2. Deploy that jar (or build a container — a one-line Dockerfile works
   since it's a single fat jar: `FROM eclipse-temurin:21-jre` +
   `COPY target/*.jar app.jar` + `ENTRYPOINT ["java","-jar","/app.jar"]`).
3. Set environment variables on the host: `GROQ_API_KEY` (required),
   optionally `GROQ_MODEL`, and `spring.mongodb.uri` if you don't want it
   hardcoded in `application.properties`.
4. Expose port 8080 (or set `server.port` via env var `PORT` if your host
   requires it).

### Frontend (Netlify / Vercel / any static host)
1. `npm run build` → static files in `frontend/dist`.
2. Deploy `frontend/dist` as a static site.
3. Point it at the backend: either deploy both under the same domain with
   a reverse-proxy rule for `/api/*`, or set the frontend's fetch base URL
   to your backend's public URL (currently `chatService.js` calls the
   relative path `/api/chat`, so the simplest setup is serving frontend
   and backend from the same origin/reverse proxy).

That's it — no local model to install or run, which is the whole point of
using Groq instead of Ollama.
