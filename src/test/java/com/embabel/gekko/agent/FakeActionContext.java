package com.embabel.gekko.agent;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContextAi;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.test.unit.FakeOperationContext;
import org.mockito.Mockito;

/**
 * Wraps a FakeOperationContext to provide an ActionContext for testing.
 * Uses Mockito to delegate ai() and promptRunner() to the underlying FakeOperationContext
 * so that LLM invocations are recorded on the FakePromptRunner.
 */
public class FakeActionContext {

    private final FakeOperationContext delegate;
    private final ActionContext actionContext;

    private FakeActionContext(FakeOperationContext delegate) {
        this.delegate = delegate;
        this.actionContext = Mockito.mock(ActionContext.class);

        var fakeRunner = delegate.getPromptRunner();

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
}
