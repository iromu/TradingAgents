package com.embabel.gekko.aot.hint;

import com.embabel.gekko.domain.ResearchTypes;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers reflection hints for all agent classes and their shared record types.
 */
public class TraderAgentRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        var reflection = hints.reflection();

        // Shared record types used across all agents
        reflection.registerType(ResearchTypes.DebateBriefs.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(ResearchTypes.InvestmentDebateState.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(ResearchTypes.InvestmentPlan.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(ResearchTypes.InvestmentReviewFeedback.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(ResearchTypes.Ticker.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(ResearchTypes.PlanApproval.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(ResearchTypes.ResearchPlan.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.agent.RiskAssessment.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.agent.RiskLevel.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.domain.Analysts.FundamentalsReport.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.domain.Analysts.MarketReport.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.domain.Analysts.NewsReport.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.domain.Analysts.SocialMediaReport.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));

        // All four agent classes
        reflection.registerType(com.embabel.gekko.agent.OrchestratorAgent.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.agent.DebateAgent.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.agent.DebateLoopAgent.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        reflection.registerType(com.embabel.gekko.agent.RiskDebateAgent.class, b -> b
                .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
    }
}
