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

package com.nexvora.offlineai.ui.nexvora.tools

/** The AI productivity tools. Every tool runs on the user's local LLM; nothing is faked. */
enum class ProductivityTool(val id: String) {
  WRITING("writing"),
  SUMMARIZE("summarize"),
  TRANSLATE("translate"),
  STUDY("study"),
  REWRITE("rewrite"),
  NOTES("notes");

  companion object {
    fun fromId(id: String?): ProductivityTool? = entries.firstOrNull { it.id == id }
  }
}

enum class WritingKind { FREE, EMAIL, BLOG, SOCIAL }

enum class RewriteTone(val adjective: String) {
  PROFESSIONAL("professional"),
  FRIENDLY("friendly"),
  CASUAL("casual"),
  FORMAL("formal"),
  CONCISE("short and concise"),
}

enum class StudyMode { EXPLAIN, QUIZ, FLASHCARDS }

enum class NotesMode { CLEAN_UP, ACTION_ITEMS, OUTLINE }

data class ToolOptions(
  val writingKind: WritingKind = WritingKind.FREE,
  val targetLanguage: String = "English",
  val tone: RewriteTone = RewriteTone.PROFESSIONAL,
  val studyMode: StudyMode = StudyMode.EXPLAIN,
  val notesMode: NotesMode = NotesMode.CLEAN_UP,
)

/** Builds the instruction text that is sent to the on-device model. Pure and unit-tested. */
object ProductivityPrompts {
  /** Input longer than this is cut, to protect small context windows on 6 GB devices. */
  const val MAX_INPUT_CHARS = 6000

  val TARGET_LANGUAGES =
    listOf("English", "Bangla", "Hindi", "Arabic", "Spanish", "French", "German", "Chinese", "Japanese")

  private const val MATCH_LANGUAGE =
    "Answer in the same language as the text. If the text is in Bangla, answer in Bangla."

  fun trimInput(input: String): String = input.trim().take(MAX_INPUT_CHARS)

  /** Returns null when there is nothing to send (blank input). */
  fun build(tool: ProductivityTool, rawInput: String, options: ToolOptions = ToolOptions()): String? {
    val input = trimInput(rawInput)
    if (input.isEmpty()) return null
    return when (tool) {
      ProductivityTool.WRITING -> {
        val task =
          when (options.writingKind) {
            WritingKind.FREE -> "Write a clear, well-structured text based on the request below."
            WritingKind.EMAIL ->
              "Write a polite, clear email (with a subject line) based on the request below."
            WritingKind.BLOG ->
              "Write a short blog post (title, introduction, a few sections, conclusion) based on the request below."
            WritingKind.SOCIAL -> "Write a short, engaging social media post based on the request below."
          }
        "$task $MATCH_LANGUAGE\n\nRequest:\n$input"
      }
      ProductivityTool.SUMMARIZE ->
        "Summarize the following text in a few clear bullet points. Keep the key facts and do not add " +
          "information that is not in the text. $MATCH_LANGUAGE\n\nText:\n$input"
      ProductivityTool.TRANSLATE ->
        "Translate the following text into ${options.targetLanguage}. Output only the translation, " +
          "without explanations.\n\nText:\n$input"
      ProductivityTool.REWRITE ->
        "Rewrite the following text in a ${options.tone.adjective} tone. Keep the original meaning. " +
          "Output only the rewritten text. $MATCH_LANGUAGE\n\nText:\n$input"
      ProductivityTool.STUDY -> {
        val task =
          when (options.studyMode) {
            StudyMode.EXPLAIN ->
              "Explain the following topic or text in simple words for a student, with one short example."
            StudyMode.QUIZ ->
              "Create 5 quiz questions with short answers based on the following material."
            StudyMode.FLASHCARDS ->
              "Create 8 flashcards from the following material. Format each as 'Q: ...' then 'A: ...'."
          }
        "$task $MATCH_LANGUAGE\n\nMaterial:\n$input"
      }
      ProductivityTool.NOTES -> {
        val task =
          when (options.notesMode) {
            NotesMode.CLEAN_UP ->
              "Clean up the following rough notes: fix spelling and grammar and organize them clearly."
            NotesMode.ACTION_ITEMS ->
              "Extract the action items from the following notes as a checklist. Do not invent tasks."
            NotesMode.OUTLINE -> "Turn the following notes into a structured outline with headings."
          }
        "$task $MATCH_LANGUAGE\n\nNotes:\n$input"
      }
    }
  }
}
