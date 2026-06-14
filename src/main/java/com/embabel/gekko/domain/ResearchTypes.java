package com.embabel.gekko.domain;

import com.embabel.gekko.agent.RiskAssessment;

import java.util.List;

/**
 * Shared record types used across all agent classes.
 * Extracted from the original TraderAgent monolith.
 */
public final class ResearchTypes {

    private ResearchTypes() {}

    public interface Report {
        String content();
    }

    public record DebateBriefs(
            String fundamentalsBrief,
            String marketBrief,
            String newsBrief,
            String socialBrief
    ) implements Report {
        @Override
        public String content() {
            return "### FUNDAMENTALS BRIEF\n\n" + fundamentalsBrief
                    + "\n\n### MARKET BRIEF\n\n" + marketBrief
                    + "\n\n### NEWS BRIEF\n\n" + newsBrief
                    + "\n\n### SOCIAL BRIEF\n\n" + socialBrief;
        }
    }

    public record InvestmentDebateState(
            List<String> history,
            List<String> bullHistory,
            List<String> bearHistory,
            String currentResponse,
            int count,
            DebateBriefs briefs,
            RiskAssessment riskAssessment,
            // Risk debate state fields (matching Python RiskDebateState)
            String latestSpeaker,
            String currentAggressiveResponse,
            String currentConservativeResponse,
            String currentNeutralResponse,
            String traderProposal
    ) implements Report {
        public InvestmentDebateState {
            if (latestSpeaker == null) {
                latestSpeaker = "";
            }
            if (currentAggressiveResponse == null) {
                currentAggressiveResponse = "";
            }
            if (currentConservativeResponse == null) {
                currentConservativeResponse = "";
            }
            if (currentNeutralResponse == null) {
                currentNeutralResponse = "";
            }
            if (traderProposal == null) {
                traderProposal = "";
            }
        }

        @Override
        public String content() {
            return currentResponse;
        }
    }

    public record InvestmentPlan(String judgeDecision, InvestmentDebateState investmentDebateState) implements Report {
        @Override
        public String content() {
            return judgeDecision;
        }
    }

    public record Ticker(String content, String feedback) {
    }

    public record InvestmentReviewFeedback(
            String feedback,
            boolean approved
    ) {
    }

    public record ResearchPlan(String content) implements Report {
        @Override
        public String content() {
            return content;
        }
    }

    public record PlanApproval(
            String feedback,
            boolean approved
    ) {
    }
}
