---
name: official-income-tax-brackets
description: Research official individual income-tax brackets for supported countries and create independently verified Flyway migrations to backfill or correct database schedules.
---

# Official income-tax brackets

Use this skill when asked to collect or update **individual marginal income-tax brackets** for Simple Accounting from government sources. This is a research and extraction workflow, not an API integration or an automated scraper. The initial supported set is `AU`, `US`, `CA`, `GB`, `NZ`, `IE`, and `ZA` only. Do not silently add countries; extend this guide with official starting points when coverage is requested.

## Define the request

1. Establish the calendar years to backfill (by default the current calendar year and the previous one). Collect **every authority tax period intersecting either calendar year**, including fiscal years beginning before the earliest calendar year or ending after the latest. Ask if the intended tax type is unclear: these tables are for individual income tax on taxable income, not VAT/GST, corporate tax, payroll withholding, or an effective tax rate on gross receipts.
2. Obtain the country codes (or, if requested, derive distinct ISO 3166-1 alpha-2 `Workspace.residency` values). Report unsupported codes rather than substituting third-party data.
3. Agree on variants if relevant. Unless specified otherwise, collect the standard resident individual schedule, US **single filer**, IE **single without qualifying children**, CA **federal only**, and GB **England/Wales/Northern Ireland**. Mark these assumptions explicitly; a country code alone does not select a US state, Canadian province, or Scottish schedule. Do not imply that these are complete tax-liability calculations.
4. Inspect existing `income_tax_schedule`, `income_tax_bracket`, and `income_tax_schedule_source` data migrations before drafting another migration. Identify missing periods and outdated rows without deleting unrelated or test-created data.

## Official starting points

Navigate from these authority pages to the tables applicable to the requested calendar years. Recheck the published page and any linked amendments on every run; these links are entry points, not frozen tax facts.

| Code | Authority and starting point | Watch for |
| --- | --- | --- |
| `AU` | [Australian Taxation Office: Australian resident rates](https://www.ato.gov.au/tax-rates-and-codes/tax-rates-australian-residents) | Income year July–June; resident schedule; Medicare levy is separate. |
| `US` | [IRS: federal rates and brackets](https://www.irs.gov/filing/federal-income-tax-rates-and-brackets) and [inflation-adjusted items by tax year](https://www.irs.gov/newsroom/inflation-adjusted-tax-items-by-tax-year); for 2026, [Revenue Procedure 2025-32](https://www.irs.gov/pub/irs-drop/rp-25-32.pdf) | Use the **tax-year** schedule for the chosen filing status, not the filing year's prior-year return or withholding table; state tax and standard deduction are separate. |
| `CA` | [Canada Revenue Agency: all years' brackets](https://www.canada.ca/en/revenue-agency/services/tax/individuals/tax-rates-brackets/all-years.html) | Federal and provincial/territorial schedules are separate; Quebec may need its own authority; credits are not brackets. In 2025, the annual federal lowest rate is 14.5% despite a July 1 statutory change. |
| `GB` | [HMRC/GOV.UK: income-tax rates](https://www.gov.uk/income-tax-rates); [rates without Personal Allowance](https://www.gov.uk/government/publications/rates-and-allowances-income-tax/income-tax-rates-and-allowances-current-and-past) | Tax year April 6–April 5; Scotland differs. Prefer rates on **taxable income** and represent the Personal Allowance separately; it tapers with income. |
| `NZ` | [Inland Revenue: individual rates](https://www.ird.govt.nz/income-tax/income-tax-for-individuals/tax-codes-and-tax-rates-for-individuals/tax-rates-for-individuals) | Tax year April–March; read the effective-from date; do not confuse secondary/withholding rates with annual tax brackets. |
| `IE` | [Revenue: rates, bands and reliefs](https://www.revenue.ie/en/personal-tax-credits-reliefs-and-exemptions/tax-relief-charts/index.aspx) | Calendar-year bands vary by marital/family status; credits and Universal Social Charge are separate. |
| `ZA` | [SARS: rates of tax for individuals](https://www.sars.gov.za/tax-rates/income-tax/rates-of-tax-for-individuals/) | SARS tax-year number is the **ending** year (March–February); rebates and age thresholds are separate. |

## Extract and normalize

- Use only the tax authority or primary legislation to establish figures. Search results, calculators, aggregators, previous skill outputs, and model memory may help find a page but are **not** evidence for the values. Prefer the actual table over examples or summaries; cross-check an amendment if it changes the table during the period.
- Confirm each schedule's tax year and start/end dates overlap the requested calendar years. Include every overlapping authority period exactly once, even when adjacent periods have identical brackets. If rates change mid-period, follow the authority's **annual tax-year table** (for example, Canada's blended 2025 federal rate), not a guessed calendar-date split; represent genuinely separate authority schedules with non-overlapping ranges. Never relabel a prior tax-year table simply because it is still displayed on a page.
- Record the currency, taxpayer variant, jurisdiction level, and basis of the thresholds. Represent marginal **taxable-income** schedules as SQL numeric `threshold` / `rate` pairs in ascending order: the rate applies from that threshold to the next threshold; the last tier has no upper limit. Thresholds are decimal currency amounts (no symbols or thousands separators) and rates are decimal fractions (`0.20` for 20%). First threshold must be `0`. Translate authority tables of inclusive whole-dollar bands into the underlying transition thresholds (e.g., `0–18,200` then `18,201–45,000` becomes thresholds `0`, `18200`, `45000`); check authority wording before normalizing.
- If the published tax formula is not a constant marginal rate inside a band, do not flatten it into a made-up constant-rate bracket. Mark that schedule unresolved and explain the limitation. Keep allowances, deductions, credits, surcharges, social contributions, Medicare levy, and regional taxes **out of the bracket list**; mention material exclusions in `limitations` and include independently verified extra fields only if the user asks for them.
- For each source, retain the official URL, title, and access date. Check the authority's reuse terms before claiming the seeded data is licensed for redistribution; being publicly readable is not itself a licence.

## Flyway migration contract

Create a new versioned migration under `app/src/main/resources/db/migration/`, after the highest existing version. Do **not** edit an already-applied migration. The tables were introduced by `V0004__Income_tax_schedules.sql`; `V0005__Backfill_income_tax_schedules.sql` seeds the initial 2025–2026 coverage. Never introduce a parallel JSON snapshot as the runtime source of truth.

- `income_tax_schedule` stores the country code, jurisdiction, taxpayer variant, authority period label, `period_start` and exclusive `period_end_exclusive`, currency, `annual_taxable_income` basis, limitations, and the standard entity ID/version/creation timestamp. Its natural key is `(country_code, jurisdiction, taxpayer, period_start)`. It is country-level reference data, **not** owned by a workspace.
- `income_tax_bracket` stores `(schedule_id, threshold, rate)`; `income_tax_schedule_source` stores `(schedule_id, title, url, accessed_on)`. Both cascade-delete when a schedule is deleted, so tests can delete and reinsert schedules with their child sets via `IncomeTaxSchedulesRepository`.
- For missing schedules, insert only the missing authority periods. Use unique stable IDs of at most 10 characters for SQL-seeded schedules; do not overwrite other taxpayer/jurisdiction variants. For corrections, upsert by the schedule's **natural key**, preserving its existing ID and creation time; replace only that schedule's bracket/source rows in the same migration. For new SQL rows provide `version` and `created_at`; for modified rows increment `version`. Do not truncate tables or delete unrelated schedules. Once a Flyway migration has run it must not be changed; a subsequent correction needs a newer migration.
- Ensure period ranges are non-overlapping for the same country, jurisdiction, and taxpayer. Preserve each distinct authority tax year even where adjacent periods have identical values. Document missing/unresolved periods in the response rather than inserting guessed brackets.

Run the relevant Gradle validation (including Flyway startup) after writing the migration. Verify numeric thresholds are unique and sorted, rates lie between 0 and 1, source URLs are official, and the seeded periods cover both full calendar years for every supported country and selected variant.

## Independent verification (mandatory)

After drafting the migration, **spawn a verification sub-agent** to independently read the same official authorities for every proposed schedule. Give it the calendar years, country codes, jurisdiction and taxpayer variants, the official starting links above, and the schema definitions; **do not give it the drafted figures initially**. Ask it to return its own thresholds and rates, exact tax-period dates, official URLs, any ambiguity or mid-year changes, and check for missing intersecting tax years. Then provide the migration and ask it to compare every threshold, rate, variant, date, and citation, reporting discrepancies explicitly. The primary agent must resolve discrepancies against the official source; neither agent's unsupported assertion is sufficient. If a sub-agent cannot be run, say verification is incomplete and do not represent the migration as independently verified.

Before delivery, compare migration records to the verification report. Summarize unresolved countries, assumptions, exclusions, and any authority reuse restrictions. Do not modify production database rows directly; Flyway applies the reviewed migration on application startup.
