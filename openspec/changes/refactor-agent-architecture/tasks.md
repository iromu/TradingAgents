# Tasks: Refactor Agent Architecture

## 1. Create OrchestratorAgent

- [ ] 1.1 Create `OrchestratorAgent.java` with @Agent annotation and TICKER_MODEL/BEST_ROLE constants
- [ ] 1.2 Implement `tickerFromForm(String formContent, OperationContext context)` → Ticker using creating(Ticker.class).fromPrompt()
- [ ] 1.3 Implement `generateResearchPlan(Ticker ticker, OperationContext context)` → ResearchPlan using synchronous createObject() with ResearchManager template (human_approved=false)
- [ ] 1.4 Implement `waitForPlanApproval(ResearchPlan plan, Ticker ticker)` → PlanApproval using WaitFor.formSubmission()
- [ ] 1.5 Implement `executeDebate(Ticker ticker, PlanApproval approval, ActionContext context)` → InvestmentPlan using context.asSubProcess(InvestmentPlan.class, debateAgent)
- [ ] 1.6 Add @Autowired fields for debateAgent and cache

## 2. Create DebateAgent

- [ ] 2.1 Create `DebateAgent.java` with @Agent annotation, @Agentic, and all role constants
- [ ] 2.2 Implement `generateFundamentalsReport(Ticker ticker, OperationContext context)` → FundamentalsReport using synchronous createObject() with FundamentalsReport template
- [ ] 2.3 Implement `generateMarketReport(Ticker ticker, OperationContext context)` → MarketReport using synchronous createObject() with MarketReport template
- [ ] 2.4 Implement `generateNewsReport(Ticker ticker, OperationContext context)` → NewsReport using synchronous createObject() with NewsReport template
- [ ] 2.5 Implement `generateSocialMediaReport(Ticker ticker, OperationContext context)` → SocialMediaReport using synchronous createObject() with SocialMediaReport template
- [ ] 2.6 Implement `prepareDebateBriefs(Ticker ticker, FundamentalsReport, MarketReport, NewsReport, SocialMediaReport, ActionContext)` → DebateBriefs using synchronous createObject() with DebateBrief template
- [ ] 2.7 Implement `runDebate(Ticker ticker, DebateBriefs briefs, ActionContext)` → InvestmentDebateState using context.asSubProcess(InvestmentDebateState.class, debateLoopAgent)
- [ ] 2.8 Implement `runRiskDebate(Ticker ticker, DebateBriefs briefs, InvestmentDebateState state, ActionContext)` → RiskAssessment using context.asSubProcess(RiskAssessment.class, riskDebateAgent)
- [ ] 2.9 Implement `waitForReview(InvestmentDebateState state, Ticker ticker)` → InvestmentReviewFeedback using WaitFor.formSubmission()
- [ ] 2.10 Implement `researchManager(Ticker ticker, InvestmentDebateState state, InvestmentReviewFeedback feedback, OperationContext)` → InvestmentPlan using synchronous createObject() with ResearchManager template (human_approved=true)
- [ ] 2.11 Add @AchievesGoal on researchManager
- [ ] 2.12 Add @Autowired fields for debateLoopAgent, riskDebateAgent, cache, templateRenderer

## 3. Create DebateLoopAgent

- [ ] 3.1 Create `DebateLoopAgent.java` with @Agent, @Agentic, and CHEAPEST_ROLE constant
- [ ] 3.2 Implement `debate(Ticker ticker, DebateBriefs briefs, ActionContext)` → InvestmentDebateState with @AchievesGoal
- [ ] 3.3 Implement the bull/bear iterative debate loop using RepeatUntilBuilder with similarity convergence check
- [ ] 3.4 Use synchronous createObject() for each bull/bear LLM call (not manual Flux)
- [ ] 3.5 Pass briefs to the loop via RepeatUntilBuilder.withInitialAction() or blackboard

## 4. Create RiskDebateAgent

- [ ] 4.1 Create `RiskDebateAgent.java` with @Agent, @Agentic, and CHEAPEST_ROLE constant
- [ ] 4.2 Implement `assessRisk(Ticker ticker, DebateBriefs briefs, InvestmentDebateState debateState, ActionContext)` → RiskAssessment with @AchievesGoal
- [ ] 4.3 Implement the 3-round structured debate (bull → bear → judge) using synchronous createObject()
- [ ] 4.4 Use parseRiskAssessment() to convert LLM output to RiskAssessment record
- [ ] 4.5 Pass briefs to the debate via blackboard or RepeatUntilBuilder

## 5. Wire agents in TraderAgentConfig

- [ ] 5.1 Create bean definitions for DebateAgent, DebateLoopAgent, RiskDebateAgent
- [ ] 5.2 Create bean definition for OrchestratorAgent (depends on DebateAgent)
- [ ] 5.3 Remove TraderAgent bean definition
- [ ] 5.4 Remove RiskDebateService bean definition
- [ ] 5.5 Keep existing cache, templateRenderer, and LlmOptions beans

## 6. Simplify controllers

- [ ] 6.1 Remove "travel" naming artifacts from TradingHtmxController (replace with "research", "plan")
- [ ] 6.2 Simplify ProcessStatusController WAITING state handling — remove manual FormBindingRequest/FormSubmission boilerplate
- [ ] 6.3 Simplify HitlService — remove manual form submission logic, keep only waitFor() and submit()
- [ ] 6.4 Keep all existing HTTP endpoints (/plan, /status/{processId}) for compatibility with existing Thymeleaf templates

## 7. Clean up

- [ ] 7.1 Delete TraderAgent.java
- [ ] 7.2 Delete RiskDebateService.java
- [ ] 7.3 Remove unused imports from modified files
- [ ] 7.4 Verify GekkoApplication.java doesn't reference deleted classes

## 8. Verify build

- [ ] 8.1 Run `./mvnw compile` to verify no compilation errors
- [ ] 8.2 Run `./mvnw verify` to confirm build passes
