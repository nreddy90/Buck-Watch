# Buck Watch

## Cost-of-living reference data

The first researched dataset contains HUD FY2027 one-bedroom gross housing
benchmarks for Atlanta, New York City, San Francisco, Athens, Lancaster, and
Gainesville. See [data/README.md](data/README.md) for CSV locations, source
definitions, validation, and reproduction instructions. Other expense
categories are still pending research.

Hackathon personal-finance website prototype. Plain HTML, CSS, and JavaScript; no dependencies or build step.

## Run locally

Open `index.html` directly in your browser, or serve this folder:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080. If your repo already has an app, place this folder in a separate directory such as `buck-watch-demo/`; it is not a React component.

## Files

- `index.html`: dashboard, transactions page, and build roadmap.
- `styles.css`: responsive styling; optional Google Fonts with local fallbacks.
- `app.js`: scenario calculations, SVG chart, navigation, presets, and demo assistant.

## Current functionality

Starting balance $8,500; baseline income $5,200/month, rent $1,600/month, other expenses $1,800/month. Baseline ending balance is $30,100 after 12 months.

Scenario ending balance = 8500 + 12 * (income - rent - otherExpenses) - oneTimeExpense.

The one-time purchase occurs in month 1. Sliders update the chart and summary cards. Presets reset assumptions before applying a change. The demo assistant recognizes a rent increase or one-time purchase with a dollar amount; other queries get a generic summary. It is not an LLM.

Sample transaction rows are illustrative and are not a complete expense ledger. No bank connections, authentication, persistence, real ML, or OpenAI calls are implemented. Values reset on reload. All amounts are USD.

## Connect the real backend

1. Build a Spring Boot API for transaction import, summaries, forecasts, scenarios, and chat.
2. Replace local calculations with API calls. Keep accurate money calculations on the server using BigDecimal.
3. Have Spring Boot call a Python forecasting service for variable spending predictions.
4. Integrate the OpenAI Java SDK in Spring Boot for structured scenario interpretation and explanations of backend-computed results.
5. Add loading/error states and validate all model outputs before applying them.
6. Store API keys in server-side environment variables, never browser code.

Suggested routes:

- POST /api/transactions/import
- GET /api/transactions
- GET /api/summary
- POST /api/forecast
- POST /api/scenarios
- POST /api/chat

## Verification

JavaScript syntax and arithmetic checks for baseline, increased rent, and a one-time purchase passed. Browser visual testing was unavailable; verify desktop and mobile layouts in your browser.

## Buck Watch full-stack setup

The repository keeps the original prototype as a design reference, but the active development setup is structured as follows:

- Frontend: React + TypeScript + Vite
- Backend: Java 21 + Spring Boot + Maven
- ML service: Python + FastAPI
- Database: PostgreSQL in TigerData (no Docker required)

### Required tool versions

- Java 21 LTS
- Node.js 20 LTS or 22 LTS recommended
- npm bundled with Node.js
- Python 3.11+ recommended
- TigerData PostgreSQL instance
- Maven wrapper included in backend; no global Maven install required

### Environment setup

Copy the example file and then edit the values with your TigerData connection details:

```bash
cp .env.example .env
```

Spring Boot does not automatically load a root `.env` file in local development, so export the values before running the backend:

```bash
set -a
source .env
set +a
```

### TigerData configuration

The backend reads the database connection from `DB_URL` first. If `DB_URL` is not set, it falls back to the local `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` values.

Important: TigerData often provides a `postgres://...` URL, but Spring Boot expects a JDBC URL with the `jdbc:postgresql://` prefix and credentials supplied as query parameters, not embedded in the hostname. The correct pattern is:

For this setup-only milestone, Flyway is intentionally disabled in the backend config because TigerData is reporting PostgreSQL 18.6, while the current bundled Flyway version does not yet support that server version. This keeps the app bootable while the project remains scaffolded and ready for later DB migrations.

```bash
DB_URL='jdbc:postgresql://<your-tigerdata-host>:5432/buckwatch?user=<db_user>&password=<db_password>&sslmode=require'
DB_USER=buckwatch
DB_PASSWORD=<password>
ML_SERVICE_URL=http://localhost:8000
OPENAI_API_KEY=
```

### Backend startup

```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

Backend local URLs:

- http://localhost:8080
- http://localhost:8080/api/health

Expected health payload:

```json
{"status":"ok","service":"buckwatch-backend"}
```

### ML service startup

```bash
cd ml-service
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

ML service local URLs:

- http://localhost:8000
- http://localhost:8000/health

Expected health payload:

```json
{"status":"ok","service":"buckwatch-ml"}
```

### Frontend startup

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

Frontend local URL:

- http://localhost:5173

The frontend uses a Vite dev proxy for `/api` routes to the Spring Boot backend on port 8080. No authentication, charts, or financial features are implemented in this setup.

### Environment variable checklist

- DB_URL
- DB_HOST
- DB_PORT
- DB_NAME
- DB_USER
- DB_PASSWORD
- ML_SERVICE_URL
- OPENAI_API_KEY

`OPENAI_API_KEY` is reserved for future server-side integration. It is optional during setup and should never be placed in frontend code.

### Architecture

```text
Frontend (React + Vite on port 5173)
    -> Spring Boot API (Java 21 on port 8080)
    -> TigerData PostgreSQL
    -> later calls FastAPI ML service (Python on port 8000)
    -> later calls OpenAI (server-side only, optional)
```

### Troubleshooting

Port already in use:

```bash
lsof -i :5173
lsof -i :8080
lsof -i :8000
```

Then stop the conflicting process or change the port.

Database connection issues:

```bash
# confirm the values are loaded
set -a
source .env
set +a

echo "$DB_URL"
```

Check that the `.env` file exists and that `DB_URL` or the `DB_HOST`/`DB_PORT`/`DB_NAME` values match your TigerData database settings.

### Verification status

This environment supports the following toolchain checks:

- Java 21
- Node.js 25.4.0
- npm 11.8.0
- Python 3.14.2
- TigerData PostgreSQL connection is configured via environment variables, no local Docker required

The project setup has been created and the stack is ready to start with TigerData as the database backend.

### Commands to run locally

```bash
cp .env.example .env

# Update .env with your TigerData values before starting the backend
set -a
source .env
set +a

cd backend
./mvnw clean compile
./mvnw spring-boot:run

cd ../ml-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

cd ../frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

This setup intentionally keeps the prototype HTML/CSS/JS files in place as design references and begins the Buck Watch stack without implementing financial features yet.
