# Buck Watch source research

Research and retrieval date: 2026-10-10. Dataset 1: housing collected and exported.

## Scope

Six cities: Atlanta GA, New York City NY, San Francisco CA, Athens GA,
Lancaster PA, Gainesville FL. Retain Atlanta, New York City, and Athens
as prototype priorities; the fourth prototype city is not yet selected.
Budget profile: one adult, no children,
renting a one-bedroom home; monthly USD.

## Dataset 1: HUD Fair Market Rents

Collected HUD's FY2027 county-level FMR workbook through the official download.
Verified the `FY27_FMRs` and `Field_Descriptions` sheets: 4,764 records and
14 columns. The one-bedroom monthly benchmark field is `fmr_1`.
The preserved workbook, full CSV export, and source hash are in `data/raw/hud/`.

- Publisher: U.S. Department of Housing and Urban Development.
- [Dataset landing page](https://www.huduser.gov/portal/datasets/fmr.html).
- [FY2027 county-level Excel download](https://www.huduser.gov/portal/datasets/fmr/fmr2027/FY27_FMRs.xlsx).
- [Area documentation lookup](https://www.huduser.gov/datasets/fmr/fmr2027).
- Units: monthly USD; select the one-bedroom estimate after inspecting headers.
- Reference period: FY2027, effective October 1, 2026; record any area-specific
  exceptions or revisions found during collection.
- Measurement: estimated 40th-percentile gross rent for standard-quality units,
  not average rent or a current apartment listing quote.
- Geography: metropolitan FMR areas, HUD subdivisions, and nonmetropolitan
  counties. A county row may repeat the estimate for its broader FMR area.
- Access: public Excel download; the separate API requires registration/token.

HUD gross rent includes covered tenant-paid utilities; telephone, cable, and
broadband are excluded. See [24 CFR 888.113](https://www.ecfr.gov/current/title-24/subtitle-B/chapter-VIII/part-888/subpart-A/section-888.113).
Store this initially as `housing_gross_monthly`. Before populating the requested
`rent_monthly` and `utilities_monthly` fields, explicitly settle their definitions.
Do not split gross rent into invented rent and utility components or add the
same utilities twice.

## Geography checks for collection

Verified against the downloaded workbook. All matched counties for each city
share the same HUD area code, name, and one-bedroom value. City labels identify
the application's destinations; the source measurements cover HUD FMR areas.

| City | County lookup candidates | What to check |
| --- | --- | --- |
| Atlanta, GA | Fulton and DeKalb | Both share Atlanta-Sandy Springs-Roswell, GA HUD Metro FMR Area. |
| New York City, NY | Bronx, Kings, New York, Queens, Richmond | All five share New York, NY HUD Metro FMR Area. |
| San Francisco, CA | San Francisco | San Francisco, CA HUD Metro FMR Area. |
| Athens, GA | Clarke | Athens-Clarke County, GA MSA. |
| Lancaster, PA | Lancaster | Lancaster, PA MSA. |
| Gainesville, FL | Alachua | Gainesville, FL HUD Metro FMR Area. |

Retain each matching row and source geography before deriving one city benchmark.
If a city spans differing benchmarks, do not silently select one county or
average them without a documented weighting rule.

## Reproduce the collection and export

1. Open the landing page; locate **2027 Data → County Level Data**.
2. Save its workbook unchanged as `data/raw/hud/FY27_FMRs.xlsx`.
3. Record download date and source URL; preserve raw data as received.
4. Run `python scripts/prepare_housing.py` with the data dependencies installed.
5. The script preserves identifiers as text, matches 11 county rows, validates
   identical area benchmarks within each city, and exports six city rows.
6. A changed workbook needs renewed schema/geography review and an updated
   retrieval date in the script. The recorded date describes this snapshot.

This sequence lets us learn the source schema before writing transformation
code. A downloaded file is raw evidence; a cleaned table is our reproducible
interpretation of that evidence. Later, a move scenario can use the difference
between two compatible housing benchmarks, alongside the user's actual budget.

Other datasets remain queued for separate research steps: grocery levels,
transportation, utilities, healthcare, and BEA regional price parities. USDA
Food Price Outlook supplies inflation assumptions rather than local monthly
grocery budgets; a price index is not a monthly expense.
