package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.gekko.htmx.HitlService;
import com.embabel.gekko.htmx.ProcessStatusController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies process ownership verification in the HTMX controllers.
 * A request for a processId not bound to the current HTTP session must be rejected.
 */
class ProcessOwnershipTest {

    private static final String SESSION_PROCESS_ID = "hitl_processId";
    private static final String OWNED_PROCESS = "owned_process_1";
    private static final String FOREIGN_PROCESS = "foreign_process_2";

    // ─── TradingHtmxController.submitPlanApproval ─────────────────────────────

    @Nested
    class SubmitPlanApproval {

        private AgentPlatform agentPlatform;
        private ResearchPlanService researchPlanService;
        private TradingHtmxController controller;
        private MockHttpSession session;
        private RedirectAttributesModelMap redirectAttrs;

        @BeforeEach
        void setUp() {
            agentPlatform = mock(AgentPlatform.class);
            researchPlanService = mock(ResearchPlanService.class);
            controller = new TradingHtmxController(agentPlatform, researchPlanService);
            session = new MockHttpSession();
            redirectAttrs = new RedirectAttributesModelMap();
        }

        @Test
        void authorizedAccess_matchingSessionProceedsNormally() {
            session.setAttribute(SESSION_PROCESS_ID, OWNED_PROCESS);
            var process = mock(com.embabel.agent.core.AgentProcess.class);
            when(process.getStatus()).thenReturn(AgentProcessStatusCode.WAITING);
            when(agentPlatform.getAgentProcess(OWNED_PROCESS)).thenReturn(process);
            when(researchPlanService.submitWaitForForm(eq(process), anyMap(), anyString()))
                    .thenReturn(process);

            String view = controller.submitPlanApproval(OWNED_PROCESS, "true", null, session, redirectAttrs);

            assertEquals("redirect:/plan/status/" + OWNED_PROCESS, view);
            assertNull(session.getAttribute("error"), "No error expected for authorized access");
        }

        @Test
        void unauthorizedAccess_differentSessionRejected() {
            session.setAttribute(SESSION_PROCESS_ID, FOREIGN_PROCESS);

            String view = controller.submitPlanApproval(OWNED_PROCESS, "true", null, session, redirectAttrs);

            assertEquals("redirect:/", view);
            assertFlashErrorContains("Access denied");
            verify(agentPlatform, never()).getAgentProcess(anyString());
        }

        @Test
        void noSessionBinding_rejected() {
            String view = controller.submitPlanApproval(OWNED_PROCESS, "true", null, session, redirectAttrs);

            assertEquals("redirect:/", view);
            assertFlashErrorContains("Access denied");
            verify(agentPlatform, never()).getAgentProcess(anyString());
        }

        private void assertFlashErrorContains(String fragment) {
            Object error = redirectAttrs.getFlashAttributes().get("error");
            assertNotNull(error, "Expected flash attribute 'error'");
            assertTrue(error.toString().contains(fragment),
                    "Flash error should contain '" + fragment + "' but was: " + error);
        }
    }

    // ─── ProcessStatusController.resubmit ─────────────────────────────────────

    @Nested
    class Resubmit {

        private AgentPlatform agentPlatform;
        private HitlService hitlService;
        private ResearchPlanService researchPlanService;
        private ProcessStatusController controller;
        private MockHttpSession session;
        private Model model;

        @BeforeEach
        void setUp() {
            agentPlatform = mock(AgentPlatform.class);
            hitlService = mock(HitlService.class);
            researchPlanService = mock(ResearchPlanService.class);
            controller = new ProcessStatusController(agentPlatform, hitlService, researchPlanService);
            session = new MockHttpSession();
            model = new ExtendedModelMap();
        }

        @Test
        void authorizedAccess_matchingSessionProceedsNormally() {
            session.setAttribute(SESSION_PROCESS_ID, OWNED_PROCESS);
            var existingSession = new HitlService.HitlSession(
                    OWNED_PROCESS, "OrchestratorAgent", "failure", java.time.LocalDateTime.now(), "", "", false);
            when(hitlService.getSession(OWNED_PROCESS)).thenReturn(java.util.Optional.of(existingSession));
            var agent = mock(com.embabel.agent.core.Agent.class);
            when(agent.getName()).thenReturn("OrchestratorAgent");
            when(agentPlatform.agents()).thenReturn(java.util.List.of(agent));
            var newProcess = mock(com.embabel.agent.core.AgentProcess.class);
            when(newProcess.getId()).thenReturn("retry_process_3");
            when(researchPlanService.createAndStart(eq(agent), any())).thenReturn(newProcess);

            String view = controller.resubmit(OWNED_PROCESS, "NVDA", "", session, model);

            assertEquals("common/processing", view);
            assertNull(((ExtendedModelMap) model).getAttribute("error"), "No error expected for authorized access");
        }

        @Test
        void unauthorizedAccess_differentSessionRejected() {
            session.setAttribute(SESSION_PROCESS_ID, FOREIGN_PROCESS);

            String view = controller.resubmit(OWNED_PROCESS, "NVDA", "", session, model);

            assertEquals("common/processing-error", view);
            assertModelErrorContains("Access denied");
            verify(hitlService, never()).getSession(anyString());
        }

        @Test
        void noSessionBinding_rejected() {
            String view = controller.resubmit(OWNED_PROCESS, "NVDA", "", session, model);

            assertEquals("common/processing-error", view);
            assertModelErrorContains("Access denied");
            verify(hitlService, never()).getSession(anyString());
        }

        private void assertModelErrorContains(String fragment) {
            Object error = ((ExtendedModelMap) model).getAttribute("error");
            assertNotNull(error, "Expected model attribute 'error'");
            assertTrue(error.toString().contains(fragment),
                    "Model error should contain '" + fragment + "' but was: " + error);
        }
    }
}
