package com.embabel.gekko.web;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that every Thymeleaf template under src/main/resources/templates/
 * parses and renders without errors when provided with a minimal model.
 *
 * Uses a standalone Thymeleaf {@link TemplateEngine} configured with the
 * Spring Standard Dialect so that @{...} URIs and SpringEL expressions
 * resolve correctly — no Spring context needed.
 */
class TemplateParsingTest {

    private static final Path TEMPLATES_DIR = Path.of("src/main/resources/templates");

    @Test
    void allTemplates_parseWithoutErrors() {
        var templateEngine = createTemplateEngine();
        var collected = collectTemplateFiles();
        assertTrue(collected.size() > 0, "Expected at least one template file");

        var failures = new LinkedHashMap<Path, String>();
        for (Path template : collected) {
            try {
                processTemplate(templateEngine, template, failures);
            } catch (Exception e) {
                failures.put(template, e.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            var sb = new StringBuilder();
            sb.append(failures.size()).append(" template(s) failed to render:\n");
            failures.forEach((path, msg) -> {
                sb.append("  ").append(TEMPLATES_DIR.relativize(path)).append(": ").append(msg).append("\n");
            });
            fail(sb.toString());
        }
    }

    /**
     * Build a standalone Thymeleaf TemplateEngine configured with Spring dialects
     * so that @{...} and SpringEL expressions work without a running Spring context.
     */
    private static TemplateEngine createTemplateEngine() {
        var templateEngine = new TemplateEngine();

        // FileTemplateResolver reads directly from src/main/resources/templates
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateResolver.setOrder(0);
        templateEngine.setTemplateResolver(templateResolver);

        // Spring Standard Dialect — handles @{...}, th:action, th:field, etc.
        templateEngine.setDialect(new SpringStandardDialect());

        return templateEngine;
    }

    /**
     * Process a template with a WebContext so that @{...} and SpringEL expressions work.
     */
    private void processTemplate(
            TemplateEngine templateEngine,
            Path template,
            Map<Path, String> failures
    ) {
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        request.setServletPath("");
        request.setRequestURI("/");
        var response = new MockHttpServletResponse();

        var app = JakartaServletWebApplication.buildApplication(servletContext);
        var exchange = app.buildExchange(request, response);
        var ctx = new WebContext(exchange, Locale.US);

        // Provide comprehensive model attributes that templates reference.
        var model = new LinkedHashMap<String, Object>();
        model.put("ticker", new TickerForm(""));
        model.put("travelPlan", createMockTravelPlan());
        model.put("pageTitle", "Test Page");
        model.put("error", "");
        model.put("success", "");
        model.put("planContent", "");
        model.put("processId", "test-process-1");
        model.put("_htmx", false);
        model.put("_sse", false);
        model.put("extraHead", null);
        model.put("extraHeadContent", null);
        model.put("title", "Test Title");
        model.put("content", null);
        model.put("researchResult", "");
        model.put("detail", "Processing...");
        model.put("resultModelKey", "");
        model.put("agentProcess", createMockAgentProcess());
        model.put("hitlSession", createMockHitlSession());
        model.put("debateHistory", List.of(
                Map.of("bull", true, "text", "Bull argument"),
                Map.of("bull", false, "text", "Bear argument")
        ));
        model.put("now", new Date());
        model.put("content", "");
        model.put("name", "Test User");
        model.put("email", "test@example.com");
        model.put("picture", "");
        model.put("successView", "plan");

        ctx.setVariables(model);

        String templateName = TEMPLATES_DIR.relativize(template).toString();
        templateEngine.process(templateName, ctx);
    }

    /**
     * Create a minimal mock travel plan with nested objects.
     * Templates access: travelPlan.proposal.title, travelPlan.brief, travelPlan.stays,
     * travelPlan.journeyMapUrl, travelPlan.travelers.travelers
     */
    @SuppressWarnings("unchecked")
    private static TravelPlan createMockTravelPlan() {
        var brief = new TravelBrief("New York", "Paris", new Date(), new Date(), "A test trip");
        var traveler = new Traveler("Test Traveler", "A test traveler");
        var stay = new Stay("https://example.com/stay", List.of("2025-01-01"), "Cozy Apartment");
        var proposal = new Proposal("Test Trip", "Go to Paris", null, null);
        var travelers = new Travelers(List.of(traveler));
        return new TravelPlan(proposal, brief, List.of(stay), "https://example.com/map", travelers);
    }

    /**
     * Create a minimal mock agent process with proper getters.
     * Templates call: agentProcess.goal?.name, agentProcess.history,
     * agentProcess.cost(), agentProcess.modelsUsed(), agentProcess.usage.promptTokens
     */
    private static AgentProcess createMockAgentProcess() {
        var goal = new Goal("Test Goal");
        var usage = new TokenUsage(1500, 500);
        var history = List.of(
                new Action("Analyze market"),
                new Action("Generate plan")
        );
        var modelsUsed = List.of(new LlmModel("gpt-4"), new LlmModel("claude-3"));
        return new AgentProcess(goal, history, usage, modelsUsed, 2.50);
    }

    /**
     * Create a minimal mock HITL session object.
     */
    private static HitlSession createMockHitlSession() {
        return new HitlSession("test-process-1", "InvestmentAgent", "Test error message",
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
    }

    private Set<Path> collectTemplateFiles() {
        var result = new HashSet<Path>();
        try (var walk = Files.walk(TEMPLATES_DIR)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".html"))
                    // form.html uses th:field which requires Spring MVC RequestContext/BindStatus
                    // not available in standalone Thymeleaf — tested via integration tests instead
                    .filter(p -> !p.toString().endsWith("/form.html"))
                    // layout.html is a fragment template, not a standalone page
                    .filter(p -> !p.toString().endsWith("/common/layout.html"))
                    .forEach(result::add);
        } catch (Exception e) {
            fail("Failed to walk templates directory: " + e.getMessage(), e);
        }
        return result;
    }

    // ─── Minimal value classes for template model attributes ──────────────────

    record TickerForm(String content) {}

    record Goal(String name) {}

    record Action(String actionName) {}

    record TokenUsage(int promptTokens, int completionTokens) {}

    record LlmModel(String name) {}

    record AgentProcess(Goal goal, List<Action> history, TokenUsage usage,
                        List<LlmModel> modelsUsed, Double cost) {
        // cost() and modelsUsed() method calls work via record accessor methods
    }

    record TravelBrief(String from, String to, Date departureDate, Date returnDate, String brief) {}

    record Traveler(String name, String about) {}

    record Stay(String airbnbUrl, List<String> days, String stayingAt) {
        public String stayingAt() { return stayingAt; }
    }

    record PageLink(String url, String summary) {}

    record Travelers(List<Traveler> travelers) {}

    record Proposal(String title, String plan, List<PageLink> pageLinks, List<PageLink> videoLinks) {}

    record TravelPlan(Proposal proposal, TravelBrief brief, List<Stay> stays,
                      String journeyMapUrl, Travelers travelers) {}

    record HitlSession(String processId, String agentName, String errorMessage, Instant occurredAt) {}
}