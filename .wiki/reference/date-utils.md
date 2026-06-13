---
title: "Date Utilities"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/DateUtils.java"
  - "src/test/java/com/embabel/gekko/util/DateUtilsTest.java"
updated_at: "2026-06-13"
---

# Date Utilities

`DateUtils` is a static utility class providing date parsing and formatting for the yyyy-MM-dd format used throughout the application.

## Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `parseDate(String)` | `LocalDate` | Parses a date string in yyyy-MM-dd format |
| `formatDate(LocalDate)` | `String` | Formats a LocalDate to yyyy-MM-dd |
| `toCalendar(LocalDate)` | `Calendar` | Converts LocalDate to java.util.Calendar |

## Validation

- `parseDate()` throws `IllegalArgumentException` if the input is null or not in yyyy-MM-dd format.
- The date format is enforced via `DateTimeFormatter.ofPattern("yyyy-MM-dd")`.

## Usage

Used by `MarketDataTools`, `YFinService`, and other components that need to convert between string dates from LLM tool calls and Java date objects.
