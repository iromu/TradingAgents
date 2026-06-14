package com.embabel.gekko.domain;

/**
 * 3-tier transaction direction used by the Trader.
 */
public enum TraderAction {
    BUY("Enter or add to position"),
    HOLD("Maintain current position, no action needed"),
    SELL("Exit position or avoid entry");

    private final String description;

    TraderAction(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
