package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies feedback length validation in TradingApiController.approvePlan().
 * Feedback exceeding 10,000 characters must be rejected with a 400 Bad Request.
 */
class FeedbackValidationTest {

    private static final String PROCESS_ID = "test_process_1";

    private AgentPlatform agentPlatform;
    private ResearchPlanService researchPlanService;
    private TradingApiController controller;

    @BeforeEach
    void setUp() {
        agentPlatform = mock(AgentPlatform.class);
        researchPlanService = mock(ResearchPlanService.class);
        controller = new TradingApiController(agentPlatform, researchPlanService);
    }

    @Test
    void feedbackWithinLimit_processedNormally() {
        var process = mock(AgentProcess.class);
        when(process.getStatus()).thenReturn(AgentProcessStatusCode.WAITING);
        when(agentPlatform.getAgentProcess(PROCESS_ID)).thenReturn(process);
        when(researchPlanService.submitWaitForForm(eq(process), anyMap(), anyString()))
                .thenReturn(process);

        String feedback = "a".repeat(10_000);
        ResponseEntity<Map<String, Object>> response =
                controller.approvePlan(PROCESS_ID, new TradingApiController.ApprovalRequest(true, feedback));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("RESUMED", response.getBody().get("status"));
        verify(researchPlanService).submitWaitForForm(eq(process), anyMap(), anyString());
    }

    @Test
    void feedbackExceedsLimit_rejectedWithBadRequest() {
        String feedback = "a".repeat(10_001);
        ResponseEntity<Map<String, Object>> response =
                controller.approvePlan(PROCESS_ID, new TradingApiController.ApprovalRequest(true, feedback));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().get("error").toString().contains("must not exceed 10,000 characters"),
                "Error should mention the 10,000 character limit but was: " + response.getBody().get("error"));
        verify(agentPlatform, never()).getAgentProcess(anyString());
        verify(researchPlanService, never()).submitWaitForForm(any(), anyMap(), anyString());
    }

    @Test
    void nullFeedback_processedNormally() {
        var process = mock(AgentProcess.class);
        when(process.getStatus()).thenReturn(AgentProcessStatusCode.WAITING);
        when(agentPlatform.getAgentProcess(PROCESS_ID)).thenReturn(process);
        when(researchPlanService.submitWaitForForm(eq(process), anyMap(), anyString()))
                .thenReturn(process);

        ResponseEntity<Map<String, Object>> response =
                controller.approvePlan(PROCESS_ID, new TradingApiController.ApprovalRequest(true, null));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("RESUMED", response.getBody().get("status"));
    }
}
