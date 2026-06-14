package com.embabel.gekko.domain;

/**
 * Structured sentiment report produced by the Sentiment Analyst.
 * Mirrors Python's SentimentReport Pydantic model.
 */
public record SentimentReportOutput(
        SentimentBand overallBand,
        Double overallScore,
        String confidence,
        String narrative
) {
    public SentimentReportOutput {
        if (overallBand == null) {
            throw new IllegalArgumentException("overallBand must not be null");
        }
        if (overallScore == null) {
            throw new IllegalArgumentException("overallScore must not be null");
        }
        if (confidence == null || confidence.isBlank()) {
            throw new IllegalArgumentException("confidence must not be blank");
        }
        if (narrative == null || narrative.isBlank()) {
            throw new IllegalArgumentException("narrative must not be blank");
        }
    }
}
