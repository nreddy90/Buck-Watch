"""Convert HUD's preserved workbook into auditable housing CSVs.

Run from any directory: python scripts/prepare_housing.py
Requires pandas and openpyxl (see scripts/requirements-data.txt).
"""

import hashlib
import json
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "data/raw/hud"
PROCESSED = ROOT / "data/processed"
SOURCE = RAW / "FY27_FMRs.xlsx"
URL = "https://www.huduser.gov/portal/datasets/fmr/fmr2027/FY27_FMRs.xlsx"
# This snapshot was downloaded on this date; reruns do not change its provenance.
RETRIEVED_ON = "2026-10-10"

# HUD fips joins state + county + county-subdivision identifiers.
# These counties have countywide rows ending in 99999 in the downloaded file.
CITIES = [
    ("Atlanta", "GA", "atlanta-ga", ["1312199999", "1308999999"]),
    ("New York City", "NY", "new-york-city-ny", [
        "3600599999", "3604799999", "3606199999", "3608199999", "3608599999"
    ]),
    ("San Francisco", "CA", "san-francisco-ca", ["0607599999"]),
    ("Athens", "GA", "athens-ga", ["1305999999"]),
    ("Lancaster", "PA", "lancaster-pa", ["4207199999"]),
    ("Gainesville", "FL", "gainesville-fl", ["1200199999"]),
]


def main():
    if not SOURCE.is_file() or SOURCE.stat().st_size == 0:
        raise ValueError(f"Missing or empty HUD workbook: {SOURCE}")

    # Text loading preserves leading zeroes in identifiers; convert rent separately.
    raw = pd.read_excel(SOURCE, sheet_name="FY27_FMRs", dtype=str,
                        keep_default_na=False)
    required = {"stusps", "fips", "hud_area_code", "hud_area_name", "fmr_1"}
    if not required.issubset(raw.columns):
        raise ValueError(f"HUD schema changed: missing {required - set(raw.columns)}")

    evidence, cities = [], []
    for city, state, location_id, fips in CITIES:
        matched = raw.loc[raw["fips"].isin(fips)].copy()
        if len(matched) != len(fips) or set(matched["fips"]) != set(fips):
            raise ValueError(f"Missing or duplicate county rows for {city}")
        if set(matched["stusps"]) != {state}:
            raise ValueError(f"Unexpected state for {city}")
        amounts = pd.to_numeric(matched["fmr_1"], errors="raise")
        if amounts.isna().any() or (amounts <= 0).any() or (amounts % 1 != 0).any():
            raise ValueError(f"Invalid one-bedroom rent for {city}")
        # Deduplicate a shared area benchmark, never average differing areas.
        if (matched["hud_area_code"].nunique() != 1 or
                matched["hud_area_name"].nunique() != 1 or amounts.nunique() != 1):
            raise ValueError(f"{city} spans different benchmarks; decide a mapping rule")
        matched.insert(0, "location_id", location_id)
        matched.insert(0, "city", city)
        evidence.append(matched)
        cities.append({
            "city": city, "state": state, "location_id": location_id,
            "housing_gross_monthly": int(amounts.iloc[0]),
            "currency": "USD", "bedrooms": 1,
            "household": "one_adult_no_children_renter",
            "geography_type": "HUD_FMR_area",
            "geography_code": matched["hud_area_code"].iloc[0],
            "geography_name": matched["hud_area_name"].iloc[0],
            "matched_county_fips": "|".join(sorted(code[:5] for code in fips)),
            "source_name": "HUD Fair Market Rents", "source_year": "FY2027",
            "source_url": URL, "retrieved_on": RETRIEVED_ON,
            "utilities_included": "true",
            "benchmark_type": "40th_percentile_gross_rent",
        })

    PROCESSED.mkdir(parents=True, exist_ok=True)
    # These CSVs are exports; the original Excel file remains untouched.
    raw.to_csv(RAW / "FY27_FMRs.csv", index=False, lineterminator="\n")
    pd.concat(evidence, ignore_index=True).to_csv(
        PROCESSED / "housing_county_matches.csv", index=False, lineterminator="\n")
    pd.DataFrame(cities).to_csv(
        PROCESSED / "city_housing.csv", index=False, lineterminator="\n")

    manifest = {
        "source_url": URL, "retrieved_on": RETRIEVED_ON,
        "source_file": "FY27_FMRs.xlsx",
        "sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "source_sheet": "FY27_FMRs", "source_rows": len(raw),
        "source_fields": raw.columns.tolist(),
        "field_descriptions": pd.read_excel(
            SOURCE, sheet_name="Field_Descriptions", keep_default_na=False
        ).to_dict(orient="records"),
        "matched_rows": sum(len(frame) for frame in evidence),
        "city_rows": len(cities),
    }
    (RAW / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"Exported {len(raw)} HUD records, {manifest['matched_rows']} matches, "
          f"and {len(cities)} city benchmarks.")
    print(pd.DataFrame(cities)[["city", "housing_gross_monthly"]].to_string(index=False))


if __name__ == "__main__":
    main()
