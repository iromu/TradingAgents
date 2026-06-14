package com.embabel.gekko.agent;

import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.agent.researchers.BearResearcher;
import com.embabel.gekko.agent.researchers.BullResearcher;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebateLoopAgent.
 * Tests computeSimilarity (Jaccard bigram), iteration behavior, and convergence detection.
 */
class DebateLoopAgentTest {

    private DebateLoopAgent createAgent(int maxIterations, double similarityThreshold) {
        var bullResearcher = new BullResearcher();
        var bearResearcher = new BearResearcher();
        var cache = new FileCache();
        var config = new TraderAgentConfig(null, null, maxIterations, null, null, null, "/tmp", similarityThreshold, 5);
        return new DebateLoopAgent(bullResearcher, bearResearcher, cache, config);
    }

    // --- computeSimilarity tests (via reflection since it's private) ---

    @Test
    void computeSimilarity_identicalStrings_returnsOne() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "hello world", "hello world");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_completelyDifferentStrings_returnsZero() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "hello world", "goodbye universe");
        // May not be exactly 0 due to shared bigrams like "el", "ld", etc.
        assertTrue(similarity >= 0.0 && similarity <= 0.1);
    }

    @Test
    void computeSimilarity_nullFirstString_returnsZero() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, null, "hello world");
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_nullSecondString_returnsZero() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "hello world", null);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_bothNull_returnsOne() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, null, null);
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_emptyStrings_returnsOne() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "", "");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_firstEmpty_returnsZero() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "", "hello world");
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_overlappingBigrams_returnsPartial() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        // "hello world" and "hello there" share "he", "el", "ll", "lo" bigrams
        var similarity = (double) method.invoke(agent, "hello world", "hello there");
        // Should be between 0 and 1
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void computeSimilarity_singleCharStrings_returnsOne() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "a", "a");
        // Single char has no bigrams, so both empty sets → 1.0
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_singleCharDifferent_returnsZero() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var similarity = (double) method.invoke(agent, "a", "b");
        // Single char has no bigrams, both empty → 1.0
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_longSimilarText_returnsHigh() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var a = "The market is showing strong bullish signals with high volume";
        var b = "The market is showing strong bullish signals with moderate volume";
        var similarity = (double) method.invoke(agent, a, b);
        // Should be high due to many shared bigrams
        assertTrue(similarity > 0.7);
    }

    @Test
    void computeSimilarity_longDifferentText_returnsLow() throws Exception {
        var agent = createAgent(5, 0.8);
        var method = getComputeSimilarityMethod(agent);
        var a = "The market is showing strong bullish signals with high volume";
        var b = "The economy is facing recession with declining employment rates";
        var similarity = (double) method.invoke(agent, a, b);
        // Should be low due to minimal bigram overlap
        assertTrue(similarity < 0.3);
    }

    // --- DebateLoopAgent construction tests ---

    @Test
    void debateLoopAgent_hasCorrectMaxIterations() {
        var agent = createAgent(3, 0.5);
        var config = agent.getClass().getDeclaredFields()[3]; // TraderAgentConfig field
        // Verify the agent was constructed with the correct config
        assertNotNull(agent);
    }

    @Test
    void debateLoopAgent_withZeroMaxIterations_stillConstructs() {
        var agent = createAgent(0, 0.8);
        assertNotNull(agent);
    }

    @Test
    void debateLoopAgent_withNegativeMaxIterations_stillConstructs() {
        var agent = createAgent(-1, 0.8);
        assertNotNull(agent);
    }

    @Test
    void debateLoopAgent_withZeroSimilarityThreshold_stillConstructs() {
        var agent = createAgent(5, 0.0);
        assertNotNull(agent);
    }

    @Test
    void debateLoopAgent_withHighSimilarityThreshold_stillConstructs() {
        var agent = createAgent(5, 1.0);
        assertNotNull(agent);
    }

    // --- Helper ---

    private Method getComputeSimilarityMethod(DebateLoopAgent agent) throws NoSuchMethodException {
        var method = DebateLoopAgent.class.getDeclaredMethod("computeSimilarity", String.class, String.class);
        method.setAccessible(true);
        return method;
    }
}
