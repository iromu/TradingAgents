---
name: embabel-write-and-review-agent
description: WriteAndReviewAgent from java-agent-template
source: https://github.com/embabel/java-agent-template/blob/main/src/main/java/com/embabel/template/agent/WriteAndReviewAgent.java
---

# WriteAndReviewAgent

This is the complete source code from the [java-agent-template](https://github.com/embabel/java-agent-template) repository.

[source,java]
----
/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.template.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.domain.library.HasContent;
import com.embabel.agent.prompt.persona.Persona;
import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.core.types.Timestamped;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

abstract class Personas {
    static final RoleGoalBackstory WRITER = new RoleGoalBackstory(
            "Creative Storyteller",
            "Write engaging and imaginative stories",
            "Has a PhD in French literature; used to work in a circus");

    static final Persona REVIEWER = new Persona(
            "Media Book Review",
            "New York Times Book Reviewer",
            "Professional and insightful",
            "Help guide readers toward good stories"
    );
}


@Agent(description = "Generate a story based on user input and review it")
public class WriteAndReviewAgent {

    public record Story(String text) {
    }

    public record ReviewedStory(
            Story story,
            String review,
            Persona reviewer
    ) implements HasContent, Timestamped {

        @Override
        @NonNull
        public Instant getTimestamp() {
            return Instant.now();
        }

        @Override
        @NonNull
        public String getContent() {
            return String.format("""
                            # Story
                            %s
                            
                            # Review
                            %s
                            
                            # Reviewer
                            %s, %s
                            """,
                    story.text(),
                    review,
                    reviewer.getName(),
                    getTimestamp().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
            ).trim();
        }
    }

    private final int storyWordCount;
    private final int reviewWordCount;

    WriteAndReviewAgent(
            @Value("${storyWordCount:100}") int storyWordCount,
            @Value("${reviewWordCount:100}") int reviewWordCount
    ) {
        this.storyWordCount = storyWordCount;
        this.reviewWordCount = reviewWordCount;
    }

    @AchievesGoal(
            description = "The story has been crafted and reviewed by a book reviewer",
            export = @Export(remote = true, name = "writeAndReviewStory"))
    @Action
    ReviewedStory reviewStory(UserInput userInput, Story story, Ai ai) {
        var review = ai
                .withAutoLlm()
                .withPromptContributor(Personas.REVIEWER)
                .generateText(String.format("""
                                You will be given a short story to review.
                                Review it in %d words or less.
                                Consider whether or not the story is engaging, imaginative, and well-written.
                                Also consider whether the story is appropriate given the original user input.
                                
                                # Story
                                %s
                                
                                # User input that inspired the story
                                %s
                                """,
                        reviewWordCount,
                        story.text(),
                        userInput.getContent()
                ).trim());

        return new ReviewedStory(
                story,
                review,
                Personas.REVIEWER
        );
    }

    @Action
    Story craftStory(UserInput userInput, Ai ai) {
        return ai
                // Higher temperature for more creative output
                .withLlm(LlmOptions
                        .withAutoLlm() // You can also choose a specific model or role here
                        .withTemperature(.7)
                )
                .withPromptContributor(Personas.WRITER)
                .creating(Story.class)
                .fromPrompt(String.format("""
                                Craft a short story in %d words or less.
                                The story should be engaging and imaginative.
                                Use the user's input as inspiration if possible.
                                If the user has provided a name, include it in the story.
                                
                                # User input
                                %s
                                """,
                        storyWordCount,
                        userInput.getContent()
                ).trim());
    }
}
----

## Test Files

### Unit Test

[source,java]
----
package com.embabel.template.agent;

import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteAndReviewAgentTest {

    @Test
    void testWriteAndReviewAgent() {
        var context = FakeOperationContext.create();
        var promptRunner = (FakePromptRunner) context.promptRunner();
        context.expectResponse(new WriteAndReviewAgent.Story("One upon a time Sir Galahad . . "));

        var agent = new WriteAndReviewAgent(200, 400);
        var story = agent.craftStory(new UserInput("Tell me a story about a brave knight", Instant.now()), context.ai());

        var prompt = promptRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(prompt.contains("knight"), "Expected prompt to contain 'knight'");

    }

    @Test
    void testReview() {
        var agent = new WriteAndReviewAgent(200, 400);
        var userInput = new UserInput("Tell me a story about a brave knight", Instant.now());
        var story = new WriteAndReviewAgent.Story("Once upon a time, Sir Galahad...");
        var context = FakeOperationContext.create();
        context.expectResponse("A thrilling tale of bravery and adventure!");
        var review = agent.reviewStory(userInput, story, context.ai());
        var llmInvocation = context.getLlmInvocations().getFirst();
        var prompt = llmInvocation.getMessages().getFirst().getContent();
        assertTrue(prompt.contains("knight"), "Expected prompt to contain 'knight'");
        assertTrue(prompt.contains("review"), "Expected prompt to contain 'review'");
    }

}
----

### Integration Test

[source,java]
----
package com.embabel.template.agent;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Use framework superclass to test the complete workflow of writing and reviewing a story.
 * This will run under Spring Boot against an AgentPlatform instance
 * that has loaded all our agents.
 */
class WriteAndReviewAgentIntegrationTest extends EmbabelMockitoIntegrationTest {

    @BeforeAll
    static void setUp() {
        // Set shell configuration to non-interactive mode
        System.setProperty("embabel.agent.shell.interactive.enabled", "false");
    }

    @Test
    void shouldExecuteCompleteWorkflow() {
        var input = new UserInput("Write about artificial intelligence");

        var story = new WriteAndReviewAgent.Story("AI will transform our world...");
        var reviewedStory = new WriteAndReviewAgent.ReviewedStory(story, "Excellent exploration of AI themes.", Personas.REVIEWER);

        whenCreateObject(prompt -> prompt.contains("Craft a short story"), WriteAndReviewAgent.Story.class)
                .thenReturn(story);

        // The second call uses generateText
        whenGenerateText(prompt -> prompt.contains("You will be given a short story to review"))
                .thenReturn(reviewedStory.review());

        var invocation = AgentInvocation.create(agentPlatform, WriteAndReviewAgent.ReviewedStory.class);
        var reviewedStoryResult = invocation.invoke(input);

        assertNotNull(reviewedStoryResult);
        assertTrue(reviewedStoryResult.getContent().contains(story.text()),
                "Expected story content to be present: " + reviewedStoryResult.getContent());
        assertEquals(reviewedStory, reviewedStoryResult,
                "Expected review to match: " + reviewedStoryResult);

        verifyCreateObjectMatching(prompt -> prompt.contains("Craft a short story"), WriteAndReviewAgent.Story.class,
                llm -> llm.getLlm().getTemperature() == 0.7 && llm.getToolGroups().isEmpty());
        verifyGenerateTextMatching(prompt -> prompt.contains("You will be given a short story to review"));
        verifyNoMoreInteractions();
    }
}
----
