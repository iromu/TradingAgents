package com.embabel.gekko.domain;

import com.embabel.gekko.domain.PortfolioRating;

/**
 * Structured investment plan produced by the Research Manager.
 * Mirrors Python's ResearchPlan Pydantic model.
 */
public record ResearchPlanOutput(
        PortfolioRating recommendation,
        String rationale,
        String strategicActions
) {
    public ResearchPlanOutput {
        if (recommendation == null) {
            throw new IllegalArgumentException("recommendation must not be null");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        if (strategicActions == null || strategicActions.isBlank()) {
            throw new IllegalArgumentException("strategicActions must not be blank");
        }
    }

    /**
     * Render to markdown for storage and the trader's prompt context.
     */
    public String render() {
        return """
                **Recommendation**: %s

                **Rationale**: %s

                **Strategic Actions**: %s
                """.formatted(recommendation, rationale, strategicActions);
    }
}
