# Cost-of-living dataset workflow

The active reference is the complete user-supplied Kaggle city-index dataset:
536 cities worldwide. See [../README.md](../README.md) for file paths, field
definitions, source notes, and a runnable spending-adjustment example.

## What we are doing

1. Preserve the downloaded CSV unchanged so every prepared value can be traced
   to the original. Record the Kaggle URL and a file hash.
2. Inspect the actual row structure: each row is a city and each metric is an
   index relative to New York = 100. The filename/URL does not make the rows
   country averages or household budgets.
3. Normalize column names and location labels for code, keeping all rows and
   all six numeric indexes. Keep the original label and rank for auditing.
4. Validate missing cells, numeric values, duplicates, and the reference-city
   baseline before generating `data/processed/city_costs.csv`.
5. Use destination/origin index ratios to estimate changes in the user's own
   category spending. Preserve actual spending and label results as estimates.

## How this fits Buck Watch

The active app is `frontend/` (React), `backend/` (Spring Boot), and
`ml-service/` (FastAPI). The database currently stores finance events; it does
not yet import these city indexes. This dataset preparation does not modify
app behavior or add API routes.

The frontend can eventually offer all available locations using the source
labels. The backend can load the CSV by `location_id` and calculate category
adjustments using BigDecimal. The ML service can use the ratios as scenario
inputs alongside a user's actual spending history.

Keep rents separate from non-rent costs when the user has those categories.
Groceries and restaurants are already part of the broad excluding-rent basket,
so select either the broad adjustment or category adjustments for those amounts.
Local purchasing power belongs in explanatory comparisons, not in expense
multipliers or salary estimates.

This CSV has no household breakdown or separate medical/childcare/tax data.
Users with different situations supply their own baseline bills and spending;
the index ratio estimates relative location effects. More detailed household
estimates require additional data. Future source refreshes need a known date
and comparable methodology before claiming change over time.
