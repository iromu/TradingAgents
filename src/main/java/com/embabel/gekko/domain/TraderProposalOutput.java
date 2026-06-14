package com.embabel.gekko.domain;

import com.embabel.gekko.domain.TraderAction;

/**
 * Structured transaction proposal produced by the Trader.
 * Mirrors Python's TraderProposal Pydantic model.
 */
public record TraderProposalOutput(
        TraderAction action,
        String reasoning,
        Double entryPrice,
        Double stopLoss,
        String positionSizing
) {
    public TraderProposalOutput {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("reasoning must not be blank");
        }
    }

    /**
     * Render to markdown for storage and downstream consumption.
     */
    public String render() {
        var sb = new StringBuilder();
        sb.append("**Action**: ").append(action).append("\n\n");
        sb.append("**Reasoning**: ").append(reasoning);

        if (entryPrice != null) {
            sb.append("\n\n**Entry Price**: ").append(entryPrice);
        }
        if (stopLoss != null) {
            sb.append("\n\n**Stop Loss**: ").append(stopLoss);
        }
        if (positionSizing != null && !positionSizing.isBlank()) {
            sb.append("\n\n**Position Sizing**: ").append(positionSizing);
        }
        sb.append("\n\nFINAL TRANSACTION PROPOSAL: **").append(action).append("**");
        return sb.toString();
    }
}
