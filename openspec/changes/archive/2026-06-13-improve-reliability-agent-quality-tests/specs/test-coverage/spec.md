## ADDED Requirements

### Requirement: Core agent actions have unit tests
The system SHALL have unit tests covering the core `TraderAgent` actions to verify correct behavior of the investment research pipeline.

#### Scenario: tickerFromForm validates input
- **WHEN** `tickerFromForm()` is called with valid ticker input
- **THEN** it returns a `Ticker` with uppercase content
- **AND** invalid formats throw `IllegalArgumentException`

#### Scenario: generateFundamentalsReport uses cache
- **WHEN** `generateFundamentalsReport()` is called twice with the same ticker
- **THEN** the cache is hit on the second call
- **AND** the result is a valid `FundamentalsReport`

#### Scenario: generateMarketReport uses cache
- **WHEN** `generateMarketReport()` is called twice with the same ticker
- **THEN** the cache is hit on the second call
- **AND** the result is a valid `MarketReport`

#### Scenario: generateNewsReport uses cache
- **WHEN** `generateNewsReport()` is called twice with the same ticker
- **THEN** the cache is hit on the second call
- **AND** the result is a valid `NewsReport`

#### Scenario: generateSocialMediaReport uses cache
- **WHEN** `generateSocialMediaReport()` is called twice with the same ticker
- **THEN** the cache is hit on the second call
- **AND** the result is a valid `SocialMediaReport`

#### Scenario: prepareDebateBriefs validates inputs
- **WHEN** `prepareDebateBriefs()` is called with null or blank reports
- **THEN** it throws `IllegalArgumentException`
- **AND** all four report types are validated

### Requirement: Researcher agents have unit tests
The system SHALL have unit tests covering `BullResearcher` and `BearResearcher` to verify correct argument generation.

#### Scenario: BullResearcher.argue produces response
- **WHEN** `argue()` is called with valid briefs and history
- **THEN** it returns a non-empty string response
- **AND** the response references the debate briefs

#### Scenario: BearResearcher.argue produces response
- **WHEN** `argue()` is called with valid briefs and history
- **THEN** it returns a non-empty string response
- **AND** the response references the debate briefs

### Requirement: Data flow services have unit tests
The system SHALL have unit tests covering `YFinService` and `VendorRouter` to verify correct data retrieval behavior.

#### Scenario: YFinService loads bar series
- **WHEN** `YFinService` is asked for a bar series for a valid ticker
- **THEN** it returns a non-null TA4j BarSeries
- **AND** the series contains price data

#### Scenario: VendorRouter routes to correct service
- **WHEN** `VendorRouter` is called with a known method name
- **THEN** it routes to the correct underlying service (Alpha Vantage or Yahoo Finance)
- **AND** unknown method names throw an appropriate exception

### Requirement: Integration tests cover full pipeline
The system SHALL have integration tests that exercise the full research pipeline from ticker input to investment plan generation.

#### Scenario: Full pipeline executes end-to-end
- **WHEN** a ticker is submitted through the pipeline
- **THEN** all four analyst reports are generated
- **AND** debate briefs are produced
- **AND** the investment debate completes
- **AND** the research manager produces a plan

#### Scenario: Pipeline handles API failures gracefully
- **WHEN** an external API (Alpha Vantage or Yahoo Finance) returns an error
- **THEN** the pipeline throws a descriptive exception
- **AND** the exception does not leak internal implementation details

### Requirement: Prompt file extension tests
The system SHALL verify that all prompt files load correctly with the unified `.jinja` extension.

#### Scenario: All analyst prompts load as .jinja
- **WHEN** the system starts
- **THEN** all four analyst prompts load without errors
- **AND** the prompt content is non-empty

#### Scenario: No .txt prompt files remain on classpath
- **WHEN** the prompts directory is scanned at runtime
- **THEN** no `.txt` files are found
- **AND** all prompts use `.jinja` extension
