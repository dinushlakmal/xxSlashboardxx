package org.slashboard.ime.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.slashboard.ime.data.LocalLearningStore

@RunWith(RobolectricTestRunner::class)
class EnglishPredictionEngineTest {
    private lateinit var context: Context
    private lateinit var store: LocalLearningStore
    private lateinit var engine: EnglishPredictionEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        store = LocalLearningStore(context)
        engine = EnglishPredictionEngine(context, store)
    }

    @Test
    fun testPrefixPrediction() {
        val candidates = engine.candidates("hel", emptyList(), 3)
        assertTrue(candidates.isNotEmpty())
        val words = candidates.map { it.text.lowercase() }
        assertTrue(words.contains("hello") || words.contains("help"))
    }

    @Test
    fun testTypoCorrection() {
        val candidates = engine.candidates("teh", emptyList(), 3)
        assertTrue(candidates.isNotEmpty())
        val top = candidates.first()
        assertEquals("the", top.text.lowercase())
        assertTrue(top.isCorrection)
    }

    @Test
    fun testContractionCorrection() {
        val candidates = engine.candidates("dont", emptyList(), 3)
        assertTrue(candidates.isNotEmpty())
        val words = candidates.map { it.text }
        assertTrue(words.contains("don't"))
    }

    @Test
    fun testPhraseBigramPrediction() {
        val candidates = engine.candidates("", listOf("how"), 3)
        assertTrue(candidates.isNotEmpty())
        val words = candidates.map { it.text.lowercase() }
        assertTrue(words.contains("are") || words.contains("is") || words.contains("do"))
    }

    @Test
    fun testPhraseTrigramPrediction() {
        val candidates = engine.candidates("", listOf("how", "are"), 3)
        assertTrue(candidates.isNotEmpty())
        val words = candidates.map { it.text.lowercase() }
        assertTrue(words.contains("you"))
    }

    @Test
    fun testPhraseLearning() {
        // Learn a custom unique phrase: "slashboard awesome keyboard"
        store.record("slashboard", null, null)
        store.record("awesome", "slashboard", null)
        store.record("keyboard", "awesome", "slashboard")
        engine.learn("awesome", "slashboard", null)
        engine.learn("keyboard", "awesome", "slashboard")

        val nextAfterSlashboard = engine.candidates("", listOf("slashboard"), 3)
        assertTrue(nextAfterSlashboard.any { it.text.equals("awesome", ignoreCase = true) })

        val nextAfterTwoWords = engine.candidates("", listOf("slashboard", "awesome"), 3)
        assertTrue(nextAfterTwoWords.any { it.text.equals("keyboard", ignoreCase = true) })
    }
}
