# Buck Watch

Buck Watch includes a React/Vite dashboard and Spring Boot moving-city scenario API. The original static prototype and Python ML service remain in the repository; this feature uses deterministic Java calculations and does not train or call an ML model.

## Dataset
The current dataset preserves all 536 worldwide cities in the supplied Kaggle
cost-of-living CSV. Use `data/processed/city_costs.csv` for price indexes relative
to New York City = 100. These support city comparisons and estimates based on a
user's existing spending; the values are indexes, not monthly dollar budgets.
See [data/README.md](data/README.md) for definitions, provenance, validation,
and usage examples. The original uploaded CSV is preserved under `data/raw/`.

The city index CSV is packaged at `backend/src/main/resources/data/Cost_of_living_index.csv`. Its repository copy is `data/Cost_of_living_index.csv`. The loader uses Apache Commons CSV to handle quoted names containing commas, loads once at startup, and rejects blank, duplicate, malformed, or non-positive numeric index records. If the dataset is missing, place the supplied file at `backend/src/main/resources/data/Cost_of_living_index.csv`.

The CSV has no observation-date column. Buck Watch does not infer one. Optional provenance can be supplied separately with `COST_INDEX_SOURCE` and `COST_INDEX_OBSERVATION_DATE`; these values describe metadata only and do not change estimates. Only full city identifiers from the file are canonical. Search matches case-insensitively; ambiguous city text returns choices. Scenario moves are limited to records identified as United States cities and USD budgets.

Rent, groceries, restaurant, and other location-sensitive expenses scale by their respective index ratios. Other location-sensitive expenses use Cost of Living Index as a rough fallback. Fixed expenses are unchanged. Cost of Living Plus Rent and Local Purchasing Power are not used. User destination rent overrides the estimate. Monthly buckets apply a move at the start of its selected month with no proration. The horizon is 1–24 months. City adjustments are estimates, assume unchanged household and spending behavior, and do not include inflation or investment growth.

## Profile and authentication

There is no account persistence or authentication in this repository. The API therefore uses a clearly labeled local demo profile, enabled by `BUCKWATCH_DEMO_ENABLED=true`, with sample values and no arbitrary user ID parameter. Set the flag to `false` to disable demo profile access. Replace `BudgetService` with an authenticated account provider when account storage is implemented. The demo values are not user data.

## Configure and run

Requirements: Java 21, Maven (or the included wrapper), Node.js and npm.

From PowerShell at the repository root, configure environment values as needed, then start the backend:

```powershell
$env:BUCKWATCH_DEMO_ENABLED = 'true'
$env:DEMO_CURRENT_CITY = 'Atlanta, GA, United States'
$env:OPENAI_API_KEY = '' # optional; manual scenario works without it
$env:OPENAI_MODEL = 'gpt-5.2'
cd backend
mvn spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend is at http://localhost:5173 and proxies `/api` to http://localhost:8080. Maven is also available as `./mvnw spring-boot:run` on Unix or `./mvnw.cmd spring-boot:run` on Windows. A PostgreSQL server is not required by these endpoints; demo profile calls are in-memory.

OpenAI interpretation and explanation use the official OpenAI Java SDK Responses API. `OPENAI_API_KEY` and `OPENAI_MODEL` are read only by the server and must never be set in frontend variables. If the API key is missing, the question route responds with AI unavailable; manual calculation remains available. OpenAI-produced interpretation must pass Java validation before calculation. When explanation generation fails after calculation, deterministic numerical results are still returned with a template explanation.

## API

- `GET /api/cities?query=Chicago` — case-insensitive full-name matches.
- `GET /api/profile` — explicitly marked local demo profile.
- `POST /api/scenarios/move` — deterministic scenario; does not require OpenAI.
- `POST /api/chat/scenario` — interprets a natural-language scenario using Structured Outputs when configured.
- `GET /api/health` — service health.

Manual request:

```json
{
  "destinationCity": "Chicago, IL, United States",
  "moveMonth": "2026-11",
  "horizonMonths": 12,
  "currency": "USD",
  "movingCosts": 2500,
  "newMonthlyIncome": null,
  "destinationRent": null
}
```

Send `movingCosts: 0` to confirm no moving costs. A null or missing value excludes them and is labeled in warnings; no cost is invented. Other amount overrides accept zero. Example natural-language request:

```json
{"question":"What if I move from Atlanta to Chicago next month?"}
```

The chat endpoint asks for missing month, costs, or other required information before running a scenario. No shared global pending-chat state is used. Clients can pass the clarification answer in a follow-up question; state is not persisted across requests in this MVP.

Successful scenario responses contain `results` with category costs, surplus, monthly balances, final and lowest balances, estimate methods, assumptions, and warnings. Error responses have `{ "error": { "code", "message", "choices" } }`.

## Checks

From the repository root, run:

```powershell
cd backend
mvn test
cd ../frontend
npm run build
```
