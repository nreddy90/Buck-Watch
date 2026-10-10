# Buck Watch cost-of-living indexes

The active dataset uses **all 536 cities worldwide** in the CSV supplied by
the user on October 10, 2026. There is no five-city selection or household
filter. Each row describes one city, not one household. The data contains
six relative price/purchasing-power indexes, with **New York City = 100**.
These values are not dollar amounts or monthly budgets.

## Files for the team

| File | Purpose |
| --- | --- |
| `processed/city_costs.csv` | All 536 cities, normalized field names, location identifiers, unchanged index values, and source metadata. Main application reference. |
| `raw/cost_of_living_index/Cost_of_living_index.csv` | Unmodified user-supplied file, including original column names, ordering, and numeric text. |
| `raw/cost_of_living_index/manifest.json` | Kaggle source URL, source/output hashes, record counts, interpretation, and metadata gaps. |
| `../scripts/prepare_cost_of_living.py` | Reproducible validation and normalization using Python's standard library. No downloads. |
| `cost-of-living/README.md` | Learning sequence and application integration notes. |

Source: [Cost of Living Index by Cities, debdutta on Kaggle](https://www.kaggle.com/datasets/debdutta/cost-of-living-index-by-country/data).
The supplied description attributes the indexes to Numbeo. Despite the URL's
wording, the file contains city records, not country averages. Index definitions
were checked against [Numbeo's explanation](https://www.numbeo.com/cost-of-living/cpi_explained.jsp).

The file does not identify its observation date. `source_period` is blank and
`source_period_status` is `unknown`; October 10, 2026 is the date provided, not
the price observation date. The research tool could not verify a license or
dataset version on Kaggle, so those metadata fields remain unknown. Do not
describe the snapshot as current 2026 prices.

## Columns and definitions

| Field | Meaning |
| --- | --- |
| `location_id` | Stable lookup slug derived from the source location label. US IDs use city/state; other IDs use city/country. Not an official geographic code. |
| `city_name`, `state_code`, `country` | Location labels parsed from the source. State code is populated for US rows; blank elsewhere because comparable region codes are not supplied. |
| `source_city_label`, `source_rank` | Original city label and worldwide ranking. Rank is not a price multiplier. |
| `cost_of_living_excluding_rent_index` | Relative prices for the source's non-rent basket, including groceries, restaurants, transportation, and utilities. |
| `rent_index` | Relative apartment rental prices. |
| `cost_of_living_plus_rent_index` | Source's combined non-rent and rent index. Preserve it as published; do not add the other two indexes to recreate it. |
| `groceries_index` | Relative grocery basket prices. |
| `restaurant_price_index` | Relative restaurant meal/drink prices. |
| `local_purchasing_power_index` | Relative purchasing power based on local average net salary and costs. This is not an expense or salary amount. |
| `index_base_city`, `index_base_value` | New York City and 100 for all rows. Values are dimensionless. |
| `source_period`, `source_period_status` | Unknown observation period, represented by an empty value and explicit status. |
| `source_file`, `source_url` | Preserved raw file path and Kaggle source link. |

All six index values are preserved without rounding or rescaling. A cost index
of 80 means the represented basket is estimated at 80% of New York's price,
or 20% lower. A purchasing-power index above 100 has a different interpretation:
greater purchasing power relative to New York, not higher costs.

City names can contain commas. Use a CSV parser, not string splitting on commas.
Keep `location_id` and the full source label when presenting ambiguous city names.
Country labels remain exactly as provided; they are not ISO country codes.

## Using indexes with a person's spending

For the same spending category and currency:

```text
estimated destination spending = current spending × destination index / origin index
```

For example, Atlanta's non-rent index is 72.05 and New York's is 100.
Someone spending $1,000/month on the represented non-rent basket in New York
would get an estimated Atlanta amount of $720.50/month. This is a scenario
estimate anchored to that person's spending, not a published dollar budget.

Use the rent index for rent, the groceries index for groceries, and the
restaurant index for dining out. Alternatively, use the broader excluding-rent
index once for a combined non-rent basket. Do not apply overlapping indexes to
the same amount or sum index values. The combined plus-rent index reflects the
source's basket weights, not necessarily the user's rent/spending proportions.

The CSV does not provide separate healthcare, childcare, transport, or utility
indexes, household counts, tax rates, or salaries. There is no basis here for
inventing those values or inferring an absolute monthly cost for a household.
Cross-border estimates retain the currency of the user's baseline; currency
conversion, income, and taxes require separate inputs.

## Read and estimate with Python

From the repository root, using the ML service environment with Pandas:

```python
from decimal import Decimal, ROUND_HALF_UP
import pandas as pd

costs = pd.read_csv("data/processed/city_costs.csv", dtype=str, keep_default_na=False)
costs = costs.set_index("location_id", verify_integrity=True)

origin = costs.loc["new-york-city-ny"]
destination = costs.loc["atlanta-ga"]
metric = "cost_of_living_excluding_rent_index"
current_monthly_spending = Decimal("1000.00")
estimate = (
    current_monthly_spending * Decimal(destination[metric]) / Decimal(origin[metric])
).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
print(estimate)  # 720.50
```

Both rows must come from the same snapshot and use the same index column.
The backend should label the result as estimated, retain the user's actual
spending separately, and reject unavailable city/category lookups.

## Regenerate and validate

```bash
python3 scripts/prepare_cost_of_living.py
```

The script uses only the standard library. It checks headers, required cells,
unique labels/IDs/ranks, positive finite numeric indexes, and New York's six
baseline values. It preserves every source row and index value, normalizes
headers, parses labels, and records hashes in the manifest.

The source remains byte-for-byte unchanged. For a new release, preserve its
original file and record its observation period/version when available before
regeneration. A single cross-sectional snapshot supports city comparisons;
it is not a historical series or personal forecasting training data.

## Earlier research

The tracked `raw/hud/`, `processed/city_housing.csv`, and
`processed/housing_county_matches.csv` remain earlier housing-only research.
Their source definitions are in `sources.md`. The abandoned MIT household
snapshot has been removed from this local change. Use the new index dataset
as the active reference; do not mix money amounts and index values.

Updates are made on `main`. Files must be committed and pushed before teammates
can retrieve them through Git. The CSV preparation does not yet load these
records into the backend/database or change the frontend.
