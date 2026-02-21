# Documentation Index

## Purpose
This index defines the default reading order for humans and agents.
When documents conflict, this file decides precedence.
Default behavior: start with `README.md` and this file before any other Markdown docs.

## Canonical Documents (Read First)
1. `README.md` - project overview and local run commands.
2. `PRD_V2B.md` - current product requirements baseline.
3. `TECH_DESIGN_V2B.md` - current technical design baseline.
4. `API_CONTRACT.md` - API contract and payload expectations.
5. `DEPLOYMENT_GUIDE.md` - deployment and runtime operations.

## Supporting Documents (Read When Needed)
- `TEST_SUMMARY.md`
- `TEST_REPORT_V2B.md`
- `REGRESSION_TEST.md`
- `TROUBLESHOOTING.md`
- `PRODUCTION_CONFIG_REFERENCE.md`

## Versioning Rules
- Files with suffixes like `_V2A` are historical unless explicitly marked as current here.
- If both base and versioned files exist, follow the version explicitly listed in this index.
- If a document is not listed in this file, treat it as non-canonical reference material.
- Do not read `docs/archive/` unless you are debugging history, regression context, or requested explicitly.

## Archived Documents
Moved to `docs/archive/`:
- `BACKEND_IMPLEMENTATION_V2A.md`
- `DELIVERY_REPORT_V2A.md`
- `PRD.md`
- `PRD_V2A.md`
- `REGRESSION_TEST_V2A.md`
- `RELEASE_NOTES_V1.md`
- `TECH_DESIGN.md`
- `TECH_DESIGN_V2A.md`
