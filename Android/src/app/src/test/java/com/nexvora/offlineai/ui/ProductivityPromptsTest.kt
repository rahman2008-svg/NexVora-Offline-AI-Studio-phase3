/*
 * Copyright 2026 NexVora Lab's Ofc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexvora.offlineai.ui

import com.nexvora.offlineai.ui.nexvora.tools.NotesMode
import com.nexvora.offlineai.ui.nexvora.tools.ProductivityPrompts
import com.nexvora.offlineai.ui.nexvora.tools.ProductivityTool
import com.nexvora.offlineai.ui.nexvora.tools.RewriteTone
import com.nexvora.offlineai.ui.nexvora.tools.StudyMode
import com.nexvora.offlineai.ui.nexvora.tools.ToolOptions
import com.nexvora.offlineai.ui.nexvora.tools.WritingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductivityPromptsTest {
  @Test fun blankInput_buildsNothing_forEveryTool() {
    ProductivityTool.entries.forEach { assertNull(ProductivityPrompts.build(it, "   \n ")) }
  }

  @Test fun everyTool_includesTheUserText() {
    ProductivityTool.entries.forEach {
      val prompt = ProductivityPrompts.build(it, "unique-input-123")
      assertNotNull(prompt)
      assertTrue("$it should contain input", prompt!!.contains("unique-input-123"))
    }
  }

  @Test fun translate_namesTargetLanguage_andAsksForTranslationOnly() {
    val prompt = ProductivityPrompts.build(ProductivityTool.TRANSLATE, "Good morning", ToolOptions(targetLanguage = "Bangla"))!!
    assertTrue(prompt.contains("into Bangla"))
    assertTrue(prompt.contains("only the translation"))
  }

  @Test fun rewrite_usesSelectedTone() {
    val prompt = ProductivityPrompts.build(ProductivityTool.REWRITE, "hey", ToolOptions(tone = RewriteTone.FORMAL))!!
    assertTrue(prompt.contains("formal tone"))
  }

  @Test fun writing_kindChangesInstruction() {
    val email = ProductivityPrompts.build(ProductivityTool.WRITING, "ask for leave", ToolOptions(writingKind = WritingKind.EMAIL))!!
    val blog = ProductivityPrompts.build(ProductivityTool.WRITING, "ask for leave", ToolOptions(writingKind = WritingKind.BLOG))!!
    assertTrue(email.contains("email"))
    assertTrue(blog.contains("blog post"))
  }

  @Test fun study_modesDiffer() {
    val quiz = ProductivityPrompts.build(ProductivityTool.STUDY, "photosynthesis", ToolOptions(studyMode = StudyMode.QUIZ))!!
    val cards = ProductivityPrompts.build(ProductivityTool.STUDY, "photosynthesis", ToolOptions(studyMode = StudyMode.FLASHCARDS))!!
    assertTrue(quiz.contains("quiz"))
    assertTrue(cards.contains("flashcards"))
  }

  @Test fun notes_modesDiffer() {
    val actions = ProductivityPrompts.build(ProductivityTool.NOTES, "call Sam", ToolOptions(notesMode = NotesMode.ACTION_ITEMS))!!
    assertTrue(actions.contains("action items"))
  }

  @Test fun nonTranslateTools_askToMatchBanglaInput() {
    val prompt = ProductivityPrompts.build(ProductivityTool.SUMMARIZE, "text")!!
    assertTrue(prompt.contains("Bangla"))
  }

  @Test fun longInput_isCut() {
    val prompt = ProductivityPrompts.build(ProductivityTool.SUMMARIZE, "a".repeat(ProductivityPrompts.MAX_INPUT_CHARS + 500))!!
    assertEquals(ProductivityPrompts.MAX_INPUT_CHARS, prompt.count { it == 'a' } - prompt.take(prompt.indexOf("Text:")).count { it == 'a' })
  }

  @Test fun fromId_roundTrips() {
    ProductivityTool.entries.forEach { assertEquals(it, ProductivityTool.fromId(it.id)) }
    assertNull(ProductivityTool.fromId("unknown"))
    assertNull(ProductivityTool.fromId(null))
  }
}
