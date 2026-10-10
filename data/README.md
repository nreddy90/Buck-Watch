# Buck Watch reference datasets

The first dataset contains HUD FY2027 one-bedroom housing benchmarks for
Atlanta, New York City, San Francisco, Athens, Lancaster, and Gainesville.
Snapshot downloaded October 10, 2026. All housing values are monthly USD.

## Files for the team

| File | Contents |
| --- | --- |
| `processed/city_housing.csv` | Six destination cities with gross housing benchmarks and provenance. Use this for housing scenarios. |
| `processed/housing_county_matches.csv` | Eleven matching HUD county records, with city/application identifiers added. Use to audit geographic mapping. |
| `raw/hud/FY27_FMRs.csv` | All 4,764 HUD records exported without filtering. Identifier columns must be read as strings. |
| `raw/hud/FY27_FMRs.xlsx` | Unmodified official source workbook, including its field definitions. |
| `raw/hud/manifest.json` | Source URL, retrieval date, SHA-256 hash, field descriptions, and record counts. |
| `sources.md` | Definitions, geography mapping, and source links. |

## Meaning and integration

`housing_gross_monthly` is a 40th-percentile gross rental benchmark including
covered utilities. It is not city average asking rent or rent excluding utilities.
Telephone, cable, and broadband are outside this housing measure.
The `geography_code` and `geography_name` describe the HUD area, which can be
larger than the named city. `location_id` is a Buck Watch slug, not a Census ID.
`matched_county_fips` lists five-digit county identifiers separated by `|`.

The one-adult household describes our scenario. HUD prices are per dwelling by
bedroom count, not an independently measured one-adult household expense total.
Do not add overlapping utility expenses to this housing value.

For a move scenario, a possible reference adjustment is destination housing
benchmark minus origin housing benchmark. Keep the user's actual rent/bills and
utility treatment explicit before applying that adjustment.

Groceries, transportation, separate utilities, healthcare, and regional price
indices are still missing. There is no complete `city_costs.csv` yet. These
housing files cannot supply a full cost-of-living total or historical ML training.

## Read with Pandas

From the repository root:

```python
import pandas as pd

housing = pd.read_csv(
    "data/processed/city_housing.csv",
    dtype={"location_id": "string", "geography_code": "string",
           "matched_county_fips": "string"},
)
atlanta = housing.loc[housing["location_id"] == "atlanta-ga"].iloc[0]
monthly_housing = atlanta["housing_gross_monthly"]
```

Reading the original county export requires `dtype={"state": "string",
"fips": "string"}` to retain leading zeroes. Avoid treating identifiers as numbers.

## Regenerate the CSVs

Use a separate virtual environment for data work:

```bash
python3 -m venv .venv-data
.venv-data/bin/python -m pip install -r scripts/requirements-data.txt
.venv-data/bin/python scripts/prepare_housing.py
```

The script reads the preserved Excel snapshot, checks county matches and area
agreement, and recreates all three CSVs and the manifest. It does not download
new data. To update the snapshot, download the official source and review its
schema, geography, period, and retrieval date first.

These files must be committed and pushed to the shared GitHub repository before
teammates can access them through Git. For a published data branch, teammates can
fetch it and inspect `data/processed/city_housing.csv` there before merging.
