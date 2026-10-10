# Buck Watch

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
