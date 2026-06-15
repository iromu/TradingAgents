---
title: "Data Vendor Layer"
type: "feature"
status: "active"
language: "default"
source_paths: [tradingagents/dataflows/]
updated_at: "2026-06-14"
---

# Data Vendor Layer

The data layer provides a vendor-routing abstraction so the same agent tool can fetch data from multiple providers.

## Vendor routing

The `route_to_vendor()` function in `dataflows/interface.py` is the central dispatch. It:

1. Looks up the configured vendor(s) for the tool's category
2. Tries each vendor in order
3. Silently skips rate-limited or misconfigured vendors
4. Returns a `NO_DATA_AVAILABLE` sentinel if all vendors report no data (with a specific reason)
5. Raises the first real error if no vendor could serve the call

> **Important**: The configured vendor chain IS the chain — the system does not silently fall back to vendors the user didn't choose. For multi-vendor fallback, list them in order, e.g. `"yfinance,alpha_vantage"`.

## Data categories

| Category | Tools | Available Vendors |
|---|---|---|
| **core_stock_apis** | `get_stock_data` (OHLCV prices) | yfinance, Alpha Vantage |
| **technical_indicators** | `get_indicators` (RSI, MACD, etc.) | yfinance, Alpha Vantage |
| **fundamental_data** | `get_fundamentals`, `get_balance_sheet`, `get_cashflow`, `get_income_statement` | yfinance, Alpha Vantage |
| **news_data** | `get_news`, `get_global_news`, `get_insider_transactions` | yfinance, Alpha Vantage |
| **macro_data** | `get_macro_indicators` | FRED only |
| **prediction_markets** | `get_prediction_markets` | Polymarket only (keyless) |

## Configuration

Data vendors are configured in `DEFAULT_CONFIG["data_vendors"]` (category-level) and `DEFAULT_CONFIG["tool_vendors"]` (tool-level, takes precedence):

```python
config["data_vendors"] = {
    "core_stock_apis": "yfinance",
    "technical_indicators": "yfinance",
    "fundamental_data": "yfinance",
    "news_data": "yfinance",
    "macro_data": "fred",
    "prediction_markets": "polymarket",
}
```

Override via environment: `TRADINGAGENTS_*` env vars do not directly override vendor config — edit the config dict or use `set_config()` before creating the graph.

## Error handling

Three error types are defined in `dataflows/errors.py`:

| Error | Meaning |
|---|---|
| `NoMarketDataError` | Symbol is invalid, delisted, not covered, or data is stale |
| `VendorNotConfiguredError` | Vendor requires API key that isn't set |
| `VendorRateLimitError` | Vendor returned a rate-limit response |

## Yahoo Finance (default vendor)

`y_finance.py` and `yfinance_news.py` wrap the `yfinance` library for stock data, fundamentals, and news. This is the default for most categories.

## Alpha Vantage

Separate modules handle each Alpha Vantage endpoint: `alpha_vantage_stock.py`, `alpha_vantage_indicator.py`, `alpha_vantage_fundamentals.py`, `alpha_vantage_news.py`, and `alpha_vantage_common.py`.

## FRED

`fred.py` fetches macroeconomic indicators (rates, inflation, labor, growth) from the Federal Reserve Economic Data API. Requires `FRED_API_KEY`.

## Polymarket

`polymarket.py` fetches market-implied probabilities for forward-looking events. No API key required.

## Symbol normalization

`dataflows/symbol_utils.py` (`normalize_symbol()`) handles ticker normalization (e.g., `XAUUSD` → `GC=F`) so realized-return lookups hit the correct instrument.

## Market data validation

`market_data_validator.py` provides `get_verified_market_snapshot()` — a deterministic OHLCV snapshot builder that grounds the market analyst's price claims in verified data, preventing hallucination.
