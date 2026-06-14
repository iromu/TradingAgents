package com.embabel.gekko.domain;

import com.embabel.gekko.domain.PortfolioRating;

/**
 * Structured output produced by the Portfolio Manager.
 * Mirrors Python's PortfolioDecision Pydantic model.
 */
public record PortfolioDecisionOutput(
        PortfolioRating rating,
        String executiveSummary,
        String investmentThesis,
        Double priceTarget,
        String timeHorizon
) {
    public PortfolioDecisionOutput {
        if (rating == null) {
            throw new IllegalArgumentException("rating must not be null");
        }
        if (executiveSummary == null || executiveSummary.isBlank()) {
            throw new IllegalArgumentException("executiveSummary must not be blank");
        }
        if (investmentThesis == null || investmentThesis.isBlank()) {
            throw new IllegalArgumentException("investmentThesis must not be blank");
        }
    }

    /**
     * Render to markdown for memory log, CLI display, and saved reports.
     */
    public String render() {
        var sb = new StringBuilder();
        sb.append("**Rating**: ").append(rating).append("\n\n");
        sb.append("**Executive Summary**: ").append(executiveSummary).append("\n\n");
        sb.append("**Investment Thesis**: ").append(investmentThesis);

        if (priceTarget != null) {
            sb.append("\n\n**Price Target**: ").append(priceTarget);
        }
        if (timeHorizon != null && !timeHorizon.isBlank()) {
            sb.append("\n\n**Time Horizon**: ").append(timeHorizon);
        }
        return sb.toString();
    }
}
