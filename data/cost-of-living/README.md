# Buck Watch cost-of-living research

Status: Housing collected, exported, and validated. Other categories pending research.
Cities: Atlanta, New York City,
San Francisco, Athens, Lancaster, and Gainesville. Prototype priorities: Atlanta,
New York City, and Athens; fourth city pending selection. Household: one adult with no children
renting a one-bedroom home. See `data/README.md` for the CSVs and usage instructions.
Source review date: 2026-10-10.

## Where this fits

The active app is `frontend/` (React), `backend/` (Spring Boot), and
`ml-service/` (FastAPI). The root HTML/JS demo uses illustrative expenses.
The existing database migration stores finance events, not city benchmarks.
Keep reference budgets here until their definitions are settled; later expose
them through the backend. A city benchmark is not a transaction history or
training data for a personal spending forecast.

## Research sequence

1. Select six cities and one household profile, including working adults,
   children, housing size, and transportation assumptions.
2. Map each city to the actual source geography. County, metro, HUD rent area,
   and city boundaries are different; record each explicitly.
3. Agree on expense definitions before collecting values.
4. Record original values, units, source URLs, source periods, retrieval dates,
   assumptions, and whether each value is observed or modeled.
5. Convert annual amounts to monthly amounts by dividing by 12. Keep the
   original amount; round only for presentation. Use decimal arithmetic.
6. Review six comparable budgets for missing values and overlapping categories.
   Missing means unknown, never zero. Then build `data/processed/city_costs.csv`
   and a companion measurement-level provenance file for the forecasting engine.

## Source shortlist

| Source | Use | Interpretation |
| --- | --- | --- |
| [Living Wage Calculator methodology](https://livingwage.mit.edu/pages/methodology) | Consistent household budget definitions and potential benchmark | Modeled basic-needs budgets by county or metro; not average spending or current listing prices. The 2026 model adjusts inputs to December 2025 dollars. |
| [HUD Fair Market Rents](https://www.huduser.gov/portal/datasets/fmr.html) | Downloadable housing benchmarks by area and bedroom count | Gross rent benchmark, not median asking rent. FY2027 became effective October 1, 2026; record the actual release used. |
| [USDA monthly food plans](https://www.fns.usda.gov/research/cnpp/usda-food-plans/cost-food-monthly-reports) | Food-at-home baseline | National baseline; do not present the same national amount as six independently measured city grocery prices. Specify plan and household adjustments. |
| Official local transit agencies | Transit scenario after cities are selected | Record fare/pass conditions. A transit pass is not a full car-ownership budget. |

Review the Living Wage Calculator FAQ and licensing/access requirements before
using its values in a redistributable dataset. Its website asks users not to
scrape data; no scraping pipeline is included here.

## Proposed categories

Housing including covered utilities; food at home; transportation; healthcare;
internet and mobile; other necessities; civic engagement; childcare when relevant.
Keep income/payroll taxes separate from expenses when comparing against take-home
income. Savings, debt repayment, dining out, and moving costs are user additions,
not implied parts of a basic-needs benchmark.

Do not add a separate utility bill to gross housing costs that already include
those utilities. Do not call a modeled healthcare budget an insurance quote.
Do not mix transit-only and car-inclusive transport budgets without an explicit
scenario label.

## Proposed observation contract

One row per city, household, category, source period, and scenario:

- `city_id`, `city_name`, `state`: application identifiers and labels.
- `geography_type`, `geography_name`, `geography_code`: actual source coverage.
- `household_id`, `scenario_id`: documented household and lifestyle assumptions.
- `category`, `currency`: stable category key and USD.
- `source_amount`, `source_unit`: original decimal value and monthly/annual unit.
- `monthly_amount`: derived decimal value, or null if unavailable.
- `source_name`, `source_url`, `source_period`, `retrieved_on`: provenance.
- `price_basis`: reference dollar period where known.
- `estimate_type`: observed, modeled, or derived.
- `includes`, `excludes`, `notes`: category boundaries and limitations.

Do not calculate a complete budget total if required categories are missing.
If source-reported totals differ from component sums because of rounding, retain
both and document the difference. Six cross-sectional budgets can support city
comparison and scenario defaults; they cannot establish spending trends.
