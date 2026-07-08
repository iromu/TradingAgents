package com.embabel.gekko.agent;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContextAi;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.api.common.PlatformServices;
import com.embabel.agent.core.ProcessContext;
import com.embabel.agent.core.internal.LlmOperations;
import com.embabel.agent.test.unit.FakeOperationContext;
import org.mockito.Mockito;

/**
 * Wraps a FakeOperationContext to provide an ActionContext for testing.
 * Uses Mockito to delegate ai(), processContext, and promptRunner() to the underlying FakeOperationContext.
 * Also replaces the ProcessContext's platformServices.llmOperations with a Mockito stub so that
 * asSubProcess can use Mockito-stubbed LLM responses.
 */
public class FakeActionContext {

    private final FakeOperationContext delegate;
    private final ActionContext actionContext;
    private final LlmOperations llmOperationsMock;

    private FakeActionContext(FakeOperationContext delegate) {
        this.delegate = delegate;
        this.actionContext = Mockito.mock(ActionContext.class);
        this.llmOperationsMock = Mockito.mock(LlmOperations.class);

        var fakeRunner = delegate.getPromptRunner();
        ProcessContext processContext = delegate.getProcessContext();

        // Replace the ProcessContext's platformServices.llmOperations with our mock.
        // This is critical: asSubProcess uses the ProcessContext's platformServices.llmOperations.
        // We mock PlatformServices and wire it back into the ProcessContext via copy.
        var platformServicesMock = Mockito.mock(PlatformServices.class);
        Mockito.when(platformServicesMock.getLlmOperations()).thenReturn(llmOperationsMock);
        Mockito.when(platformServicesMock.getAgentPlatform()).thenReturn(delegate.agentPlatform());
        Mockito.when(platformServicesMock.getObjectMapper()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper());
        Mockito.when(platformServicesMock.getTemplateRenderer()).thenReturn(new com.embabel.common.textio.template.JinjavaTemplateRenderer());
        processContext = processContext.copy(
                processContext.getProcessOptions(),
                platformServicesMock,
                processContext.getOutputChannel(),
                processContext.getAgentProcess()
        );

        // Stub processContext — required by asSubProcess (TypedAgentScopeBuilder.asSubProcess calls this.processContext)
        Mockito.when(actionContext.getProcessContext()).thenReturn(processContext);

        // Stub Blackboard methods — required by asSubProcess to read output from the blackboard
        Mockito.when(actionContext.getObjects()).thenAnswer(inv -> delegate.getObjects());
        Mockito.when(actionContext.last(Mockito.any(Class.class))).thenAnswer(inv -> {
            Class<?> clazz = inv.getArgument(0, Class.class);
            return delegate.last(clazz);
        });

        // Stub ai() to return an OperationContextAi that delegates withLlmByRole() to the FakePromptRunner
        var aiProxy = Mockito.mock(OperationContextAi.class);
        Mockito.when(aiProxy.withLlmByRole(Mockito.anyString())).thenAnswer(inv -> {
            var role = inv.getArgument(0, String.class);
            return fakeRunner.withLlm(new com.embabel.common.ai.model.LlmOptions(null, null, role, null, null, null, null, null, null, null, null));
        });
        Mockito.when(aiProxy.withLlm((com.embabel.common.ai.model.LlmOptions) Mockito.any())).thenAnswer(inv -> {
            var llm = inv.getArgument(0, com.embabel.common.ai.model.LlmOptions.class);
            return fakeRunner.withLlm(llm);
        });

        Mockito.when(actionContext.ai()).thenReturn(aiProxy);

        // Wire up promptRunner() directly on ActionContext
        Mockito.when(actionContext.promptRunner(
                Mockito.any(),
                Mockito.anySet(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyBoolean()
        )).thenAnswer(inv -> delegate.promptRunner(
                inv.getArgument(0),
                inv.getArgument(1),
                inv.getArgument(2),
                inv.getArgument(3),
                inv.getArgument(4),
                inv.getArgument(5)
        ));
    }

    public static FakeActionContext create() {
        return new FakeActionContext(FakeOperationContext.create());
    }

    public FakeOperationContext getDelegate() {
        return delegate;
    }

    public ActionContext getActionContext() {
        return actionContext;
    }

    public LlmOperations getLlmOperationsMock() {
        return llmOperationsMock;
    }
}
