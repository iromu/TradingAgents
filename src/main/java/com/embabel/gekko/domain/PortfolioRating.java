package com.embabel.gekko.domain;

/**
 * 5-tier portfolio rating used by the Research Manager and Portfolio Manager.
 */
public enum PortfolioRating {
    BUY("Strong conviction — recommend taking or growing the position"),
    OVERWEIGHT("Constructive view — recommend gradually increasing exposure"),
    HOLD("Balanced view — recommend maintaining the current position"),
    UNDERWEIGHT("Cautious view — recommend trimming exposure"),
    SELL("Strong conviction in the bear thesis — recommend exiting or avoiding the position");

    private final String description;

    PortfolioRating(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
