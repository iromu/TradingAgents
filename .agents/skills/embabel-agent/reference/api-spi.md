# API vs SPI

Embabel makes a clean distinction between its **API** and **SPI** (Service Provider Interface).

- **API** (`com.embabel.agent.api.*`) — The public interface that users interact with. Stable and supported.
- **SPI** (`com.embabel.agent.spi.*`) — Intended for developers who want to extend or customize the behavior of Embabel, or platform providers.

> **IMPORTANT:** Application code should only depend on the API (`com.embabel.agent.api.*`), not the SPI. The SPI is subject to change and should not be used in production code.

---

*Source: Embabel Agent v1.0.0 documentation — `reference/api-spi`*
