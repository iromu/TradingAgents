# Tasks: Refactor Agent Architecture

## 1. Create OrchestratorAgent

- [x] 1.1 Create `OrchestratorAgent.java` with @Agent annotation and TICKER_MODEL/BEST_ROLE constants
- [x] 1.2 Implement `tickerFromForm(String formContent, OperationContext context)` → Ticker using creating(Ticker.class).fromPrompt()
- [x] 1.3 Implement `generateResearchPlan(Ticker ticker, OperationContext context)` → ResearchPlan using synchronous createObject() with ResearchManager template (human_approved=false)
- [x] 1.4 Implement `waitForPlanApproval(ResearchPlan plan, Ticker ticker)` → PlanApproval using WaitFor.formSubmission()
- [x] 1.5 Implement `executeDebate(Ticker ticker, PlanApproval approval, ActionContext context)` → InvestmentPlan using context.asSubProcess(InvestmentPlan.class, debateAgent)
- [x] 1.6 Add @Autowired fields for debateAgent and cache

## 2. Create DebateAgent

- [x] 2.1 Create `DebateAgent.java` with @Agent annotation, @Agentic, and all role constants
- [x] 2.2 Implement `generateFundamentalsReport(Ticker ticker, OperationContext context)` → FundamentalsReport using synchronous createObject() with FundamentalsReport template
- [x] 2.3 Implement `generateMarketReport(Ticker ticker, OperationContext context)` → MarketReport using synchronous createObject() with MarketReport template
- [x] 2.4 Implement `generateNewsReport(Ticker ticker, OperationContext context)` → NewsReport using synchronous createObject() with NewsReport template
- [x] 2.5 Implement `generateSocialMediaReport(Ticker ticker, OperationContext context)` → SocialMediaReport using synchronous createObject() with SocialMediaReport template
- [x] 2.6 Implement `prepareDebateBriefs(Ticker ticker, FundamentalsReport, MarketReport, NewsReport, SocialMediaReport, ActionContext)` → DebateBriefs using synchronous createObject() with DebateBrief template
- [x] 2.7 Implement `runDebate(Ticker ticker, DebateBriefs briefs, ActionContext)` → InvestmentDebateState using context.asSubProcess(InvestmentDebateState.class, debateLoopAgent)
- [x] 2.8 Implement `runRiskDebate(Ticker ticker, DebateBriefs briefs, InvestmentDebateState state, ActionContext)` → RiskAssessment using context.asSubProcess(RiskAssessment.class, riskDebateAgent)
- [x] 2.9 Implement `waitForReview(InvestmentDebateState state, Ticker ticker)` → InvestmentReviewFeedback using WaitFor.formSubmission()
- [x] 2.10 Implement `researchManager(Ticker ticker, InvestmentDebateState state, InvestmentReviewFeedback feedback, OperationContext)` → InvestmentPlan using synchronous createObject() with ResearchManager template (human_approved=true)
- [x] 2.11 Add @AchievesGoal on researchManager
- [x] 2.12 Add @Autowired fields for debateLoopAgent, riskDebateAgent, cache, templateRenderer

## 3. Create DebateLoopAgent

- [x] 3.1 Create `DebateLoopAgent.java` with @Agent, @Agentic, and CHEAPEST_ROLE constant
- [x] 3.2 Implement `debate(Ticker ticker, DebateBriefs briefs, ActionContext)` → InvestmentDebateState with @AchievesGoal
- [x] 3.3 Implement the bull/bear iterative debate loop using RepeatUntilBuilder with similarity convergence check
- [x] 3.4 Use synchronous createObject() for each bull/bear LLM call (not manual Flux)
- [x] 3.5 Pass briefs to the loop via RepeatUntilBuilder.withInitialAction() or blackboard

## 4. Create RiskDebateAgent

- [x] 4.1 Create `RiskDebateAgent.java` with @Agent, @Agentic, and CHEAPEST_ROLE constant
- [x] 4.2 Implement `assessRisk(Ticker ticker, DebateBriefs briefs, InvestmentDebateState debateState, ActionContext)` → RiskAssessment with @AchievesGoal
- [x] 4.3 Implement the 3-round structured debate (bull → bear → judge) using synchronous createObject()
- [x] 4.4 Use parseRiskAssessment() to convert LLM output to RiskAssessment record
- [x] 4.5 Pass briefs to the debate via blackboard or RepeatUntilBuilder

## 5. Wire agents in TraderAgentConfig

- [x] 5.1 Create bean definitions for DebateAgent, DebateLoopAgent, RiskDebateAgent
- [x] 5.2 Create bean definition for OrchestratorAgent (depends on DebateAgent)
- [x] 5.3 Remove TraderAgent bean definition
- [x] 5.4 Remove RiskDebateService bean definition
- [x] 5.5 Keep existing cache, templateRenderer, and LlmOptions beans

## 6. Simplify controllers

- [x] 6.1 Remove "travel" naming artifacts from TradingHtmxController (replace with "research", "plan")
- [x] 6.2 Simplify ProcessStatusController WAITING state handling — remove manual FormBindingRequest/FormSubmission boilerplate
- [x] 6.3 Simplify HitlService — remove manual form submission logic, keep only waitFor() and submit()
- [x] 6.4 Keep all existing HTTP endpoints (/plan, /status/{processId}) for compatibility with existing Thymeleaf templates

## 7. Clean up

- [x] 7.1 Delete TraderAgent.java
- [x] 7.2 Delete RiskDebateService.java
- [x] 7.3 Remove unused imports from modified files
- [x] 7.4 Verify GekkoApplication.java doesn't reference deleted classes

## 8. Verify build

- [x] 8.1 Run `./mvnw compile` to verify no compilation errors
- [x] 8.2 Run `./mvnw verify` to confirm build passes
