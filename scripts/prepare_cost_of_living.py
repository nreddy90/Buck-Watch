"""Prepare Buck Watch city price indexes from the user's preserved CSV.

Run: python3 scripts/prepare_cost_of_living.py
Standard library only. No network requests. Indexes are not dollar amounts.
"""

import csv
import hashlib
import json
import re
from decimal import Decimal
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "data/raw/cost_of_living_index"
SOURCE = RAW / "Cost_of_living_index.csv"
PROCESSED = ROOT / "data/processed"
SOURCE_URL = "https://www.kaggle.com/datasets/debdutta/cost-of-living-index-by-country/data"
METRICS = {
    "Cost of Living Index": "cost_of_living_excluding_rent_index",
    "Rent Index": "rent_index",
    "Cost of Living Plus Rent Index": "cost_of_living_plus_rent_index",
    "Groceries Index": "groceries_index",
    "Restaurant Price Index": "restaurant_price_index",
    "Local Purchasing Power Index": "local_purchasing_power_index",
}
EXPECTED_HEADERS = ["Rank", "City", *METRICS]
FIELDS = [
    "location_id", "city_name", "state_code", "country", "source_city_label",
    "source_rank", *METRICS.values(), "index_base_city", "index_base_value",
    "source_period", "source_period_status", "source_file", "source_url",
]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def slug(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def read_and_normalize():
    rows, labels, ids, ranks = [], set(), set(), set()
    with SOURCE.open(newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        require(reader.fieldnames == EXPECTED_HEADERS, "Source CSV headers changed")
        for source in reader:
            require(None not in source and all(v is not None and v.strip() for v in source.values()),
                    "Source row has missing values or unexpected cells")
            label = source["City"].strip()
            require(label not in labels, f"Duplicate city label: {label}")
            labels.add(label)
            rank = int(source["Rank"])
            require(rank > 0 and rank not in ranks, f"Invalid/duplicate rank: {rank}")
            ranks.add(rank)
            city, country = label.rsplit(", ", 1)
            state = ""
            if country == "United States":
                city, state = city.rsplit(", ", 1)
                require(re.fullmatch(r"[A-Z]{2}", state), f"Unexpected US state code: {label}")
            location_id = slug(f"{city}-{state}" if state else label)
            if label == "New York, NY, United States":
                location_id = "new-york-city-ny"
            require(location_id and location_id not in ids, f"Location ID collision: {location_id}")
            ids.add(location_id)
            row = {
                "location_id": location_id, "city_name": city, "state_code": state,
                "country": country, "source_city_label": label, "source_rank": rank,
                "index_base_city": "New York City", "index_base_value": "100",
                "source_period": "", "source_period_status": "unknown",
                "source_file": str(SOURCE.relative_to(ROOT)),
                "source_url": SOURCE_URL,
            }
            for original, normalized in METRICS.items():
                numeric = Decimal(source[original])
                require(numeric.is_finite() and numeric > 0, f"Invalid index: {label} / {original}")
                row[normalized] = format(numeric, "f")
            rows.append(row)
    require(rows and ranks == set(range(1, len(rows) + 1)), "Source ranks are not consecutive")
    nyc = [r for r in rows if r["location_id"] == "new-york-city-ny"]
    require(len(nyc) == 1 and all(Decimal(nyc[0][m]) == 100 for m in METRICS.values()),
            "New York City must have index 100 in all six metrics")
    return rows


def write_csv(path, rows):
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def main():
    rows = read_and_normalize()
    PROCESSED.mkdir(parents=True, exist_ok=True)
    app_file = PROCESSED / "city_costs.csv"
    write_csv(app_file, rows)
    manifest = {
        "source_file": str(SOURCE.relative_to(ROOT)),
        "source_sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "provided_on": "2026-10-10",
        "source_description": "User-provided CSV; attached dataset description attributes the indexes to Numbeo",
        "download_page_url": SOURCE_URL, "source_period": None, "license": None,
        "metadata_notes": "The supplied file has no observation date or license field; Kaggle's page did not expose these to the research tool. Provided date is not the source observation date.",
        "definitions_url": "https://www.numbeo.com/cost-of-living/cpi_explained.jsp",
        "units": "Dimensionless index; New York City = 100; not USD",
        "source_row_count": len(rows), "processed_row_count": len(rows),
        "country_label_count": len({r["country"] for r in rows}),
        "us_row_count": sum(r["country"] == "United States" for r in rows),
        "scope": "Every city in the supplied CSV; no location or household filtering",
        "normalization": "Preserve every numeric index; normalize headers; split location labels; retain source rank and label",
        "outputs": {str(app_file.relative_to(ROOT)): hashlib.sha256(app_file.read_bytes()).hexdigest()},
    }
    (RAW / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Validated and preserved all {len(rows)} worldwide cities; NYC base = 100.")
    print(f"App indexes: {app_file.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
