package com.embabel.gekko.aot.hint;

import com.embabel.gekko.agent.TraderAgent;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers reflection hint for TraderAgent.extractTicker(UserInput, Ai)
 */
public class TraderAgentRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
//        try {
//            Class<?> agentClass = Class.forName("com.embabel.gekko.agent.TraderAgent", false, classLoader);
//            Class<?> userInputClass = Class.forName("com.embabel.agent.domain.io.UserInput", false, classLoader);
//            Class<?> aiClass = Class.forName("com.embabel.agent.api.common.Ai", false, classLoader);
//            Method m = agentClass.getDeclaredMethod("extractTicker", userInputClass, aiClass);
//            hints.reflection().registerMethod(m, ExecutableMode.INVOKE);
//        } catch (ClassNotFoundException | NoSuchMethodException ex) {
//            // Class or method not available on this module's classpath—skip registration safely
//        }
        hints.reflection().registerType(TraderAgent.class, builder -> builder
                .withMembers(
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.DECLARED_FIELDS,
                        MemberCategory.INVOKE_PUBLIC_METHODS
                ));
    }
}
