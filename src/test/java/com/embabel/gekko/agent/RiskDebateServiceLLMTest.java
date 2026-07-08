package com.embabel.gekko.agent;

import com.embabel.gekko.agent.risk.AggressiveDebator;
import com.embabel.gekko.agent.risk.ConservativeDebator;
import com.embabel.gekko.agent.risk.NeutralDebator;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RiskDebateServiceLLMTest {

    private RiskDebateAgent createAgent() {
        var aggressive = new AggressiveDebator();
        var conservative = new ConservativeDebator();
        var neutral = new NeutralDebator();

        var aggressiveProvider = Mockito.mock(ObjectProvider.class);
        var conservativeProvider = Mockito.mock(ObjectProvider.class);
        var neutralProvider = Mockito.mock(ObjectProvider.class);
        when(aggressiveProvider.getObject()).thenReturn(aggressive);
        when(conservativeProvider.getObject()).thenReturn(conservative);
        when(neutralProvider.getObject()).thenReturn(neutral);

        return new RiskDebateAgent(aggressiveProvider, conservativeProvider, neutralProvider);
    }

    @Test
    void assessRisk_parsesRiskyAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("buy because of high risk appetite");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "buy because of high risk appetite"));
        var ticker = new ResearchTypes.Ticker("TSLA", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, "Invest", context);
        assertEquals(RiskLevel.RISKY, result.level());
        assertEquals(10, fake.getDelegate().getPromptRunner().getLlmInvocations().size());
    }

    @Test
    void assessRisk_parsesConservativeAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("sell to avoid risk");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.CONSERVATIVE, "sell to avoid risk"));
        var ticker = new ResearchTypes.Ticker("KO", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, "Invest", context);
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
        assertEquals(10, fake.getDelegate().getPromptRunner().getLlmInvocations().size());
    }

    @Test
    void assessRisk_parsesNeutralAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("hold position with moderate outlook");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "hold position with moderate outlook"));
        var ticker = new ResearchTypes.Ticker("JNJ", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, "Invest", context);
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertEquals(10, fake.getDelegate().getPromptRunner().getLlmInvocations().size());
    }

    @Test
    void assessRisk_handlesEmptyDebateHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("NEUTRAL");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "no debate history available"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                "", 0, briefs
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, "Invest", context);
        assertNotNull(result);
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertEquals(10, fake.getDelegate().getPromptRunner().getLlmInvocations().size());
    }

    @Test
    void assessRisk_handlesEmptyBriefs() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("NEUTRAL");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "NEUTRAL"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("", "", "", "");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, "Invest", context);
        assertNotNull(result);
        assertEquals(10, fake.getDelegate().getPromptRunner().getLlmInvocations().size());
    }
}
