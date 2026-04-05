/*
 * Copyright 2026 Google LLC
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
package com.hartagis.edgear.customtasks.hermes

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private const val TAG = "HermesTools"

/**
 * ToolSet for Hermes personal assistant — todo, notes, memory, reminders.
 *
 * TODO: Inject Room DAOs (TodoDao, NoteDao, MemoryDao, ReminderDao) once the database layer is wired up.
 * For now, tools return placeholder responses. Replace the TODO blocks with actual DAO calls.
 */
class HermesTools() : ToolSet {
lateinit var context: Context

// Result placeholders for image/webview from tool execution.
var resultImageToShow: com.hartagis.edgear.common.CallJsSkillResultImage? = null
var resultWebviewToShow: com.hartagis.edgear.common.CallJsSkillResultWebview? = null

// TODO: Replace with actual DAO injection once Room database is defined.

  // TODO(wire-dao): Lazy singleton DB — uncomment when database exists:
  // private val db by lazy { HermesDatabase.getInstance(appContext) }
  // private val todoDao by lazy { db.todoDao() }
  // private val noteDao by lazy { db.noteDao() }
  // private val memoryDao by lazy { db.memoryDao() }
  // private val reminderDao by lazy { db.reminderDao() }

  // In-memory placeholders until DAOs are wired.
  private val _todos = mutableListOf<Map<String, Any>>()
  private val _notes = mutableListOf<Map<String, Any>>()
  private val _memories = mutableMapOf<String, String>()
  private val _reminders = mutableListOf<Map<String, Any>>()
  private var _nextTodoId = 1
  private var _nextNoteId = 1

  // ───────────────────── TODO ─────────────────────

  @Tool(description = "Create a new todo item.")
  fun createTodo(
    @ToolParam(description = "Title of the todo item.") title: String,
    @ToolParam(description = "Description of the todo item.") description: String,
    @ToolParam(description = "Optional due date in ISO format (e.g. 2026-04-15T10:00:00). Leave empty if none.") dueDate: String = "",
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      Log.d(TAG, "createTodo: title=$title, dueDate=$dueDate")
      val todoId = _nextTodoId++
      val todo = mapOf(
        "id" to todoId,
        "title" to title,
        "description" to description,
        "dueDate" to dueDate,
        "done" to false,
      )
      _todos.add(todo)

      // TODO(wire-dao): Replace in-memory store with DAO call:
      // val newTodo = TodoEntity(title = title, description = description, dueDate = dueDate)
      // val insertedId = todoDao.insert(newTodo)

      mapOf(
        "status" to "success",
        "todoId" to todoId.toString(),
        "title" to title,
      )
    }
  }

  @Tool(description = "List todo items. Set showDone=true to include completed items.")
  fun listTodos(
    @ToolParam(description = "Whether to include completed todos. Default: false.") showDone: String = "false",
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val includeDone = showDone.toBoolean()
      val filtered = if (includeDone) _todos else _todos.filter { it["done"] == false }

      // TODO(wire-dao): Replace with DAO call:
      // val filtered = if (includeDone) todoDao.getAll() else todoDao.getPending()

      val items = filtered.joinToString("\n") { todo ->
        val title = todo["title"] ?: "—"
        val desc = todo["description"] ?: ""
        val done = if (todo["done"] == true) "✅" else "⬜"
        val id = todo["id"]
        "$done [ID: $id] $title — $desc"
      }

      mapOf(
        "status" to if (filtered.isEmpty()) "empty" else "success",
        "count" to filtered.size.toString(),
        "todos" to items.ifEmpty { "Niciun todo găsit." },
      )
    }
  }

  @Tool(description = "Mark a todo item as completed.")
  fun completeTodo(
    @ToolParam(description = "The ID of the todo to complete.") todoId: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // todoDao.markDone(id = todoId.toInt())

      val idx = _todos.indexOfFirst { it["id"].toString() == todoId }
      if (idx >= 0) {
        val updated = _todos[idx].toMutableMap()
        updated["done"] = true
        _todos[idx] = updated
        mapOf(
          "status" to "success",
          "todoId" to todoId,
          "message" to "Todo marcat ca finalizat.",
        )
      } else {
        mapOf(
          "status" to "error",
          "message" to "Todo cu ID-ul $todoId nu a fost găsit.",
        )
      }
    }
  }

  @Tool(description = "Delete a todo item.")
  fun deleteTodo(
    @ToolParam(description = "The ID of the todo to delete.") todoId: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // todoDao.delete(id = todoId.toInt())

      val removed = _todos.removeIf { it["id"].toString() == todoId }
      if (removed) {
        mapOf(
          "status" to "success",
          "todoId" to todoId,
          "message" to "Todo șters cu succes.",
        )
      } else {
        mapOf(
          "status" to "error",
          "message" to "Todo cu ID-ul $todoId nu a fost găsit.",
        )
      }
    }
  }

  // ───────────────────── NOTES ─────────────────────

  @Tool(description = "Save a note with title, content, and optional tags.")
  fun saveNote(
    @ToolParam(description = "Title of the note.") title: String,
    @ToolParam(description = "Content of the note.") content: String,
    @ToolParam(description = "Tags as a JSON array string, e.g. [\"work\",\"ideas\"]. Leave empty for no tags.") tags: String = "[]",
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // val note = NoteEntity(title = title, content = content, tags = tags)
      // val insertedId = noteDao.insert(note)

      val noteId = _nextNoteId++
      val note = mapOf(
        "id" to noteId,
        "title" to title,
        "content" to content,
        "tags" to tags,
        "timestamp" to System.currentTimeMillis().toString(),
      )
      _notes.add(note)

      mapOf(
        "status" to "success",
        "noteId" to noteId.toString(),
        "title" to title,
      )
    }
  }

  @Tool(description = "Search notes by title or content. Leave empty to list all.")
  fun getNotes(
    @ToolParam(description = "Search query to filter notes by title or content.") query: String = "",
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // val results = if (query.isNotEmpty()) noteDao.search(query) else noteDao.getAll()

      val results = if (query.isEmpty()) {
        _notes
      } else {
        val q = query.lowercase()
        _notes.filter { note ->
          (note["title"] as? String).orEmpty().lowercase().contains(q) ||
            (note["content"] as? String).orEmpty().lowercase().contains(q)
        }
      }

      val items = results.joinToString("\n") { note ->
        val title = note["title"] ?: "—"
        val content = (note["content"] as? String).orEmpty().take(80)
        val tags = note["tags"] ?: ""
        "[ID: ${note["id"]}] $title\n  👉 $content\n  🏷 $tags"
      }

      mapOf(
        "status" to if (results.isEmpty()) "empty" else "success",
        "count" to results.size.toString(),
        "notes" to items.ifEmpty { "Nicio notă găsită." },
      )
    }
  }

  // ───────────────────── MEMORY ─────────────────────

  @Tool(description = "Save a memory entry with a key-value pair.")
  fun saveMemory(
    @ToolParam(description = "Key for the memory entry.") key: String,
    @ToolParam(description = "Content to store.") content: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // memoryDao.upsert(MemoryEntity(key = key, content = content, updatedAt = ...))

      _memories[key] = content
      mapOf(
        "status" to "success",
        "key" to key,
        "message" to "Memoria a fost salvată.",
      )
    }
  }

  @Tool(description = "Retrieve a memory entry by its key.")
  fun getMemory(
    @ToolParam(description = "Key of the memory to retrieve.") key: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // val entry = memoryDao.get(key)

      val value = _memories[key]
      if (value != null) {
        mapOf(
          "status" to "success",
          "key" to key,
          "content" to value,
        )
      } else {
        mapOf(
          "status" to "not_found",
          "key" to key,
          "message" to "Nicio memorie găsită pentru cheia '$key'.",
        )
      }
    }
  }

  @Tool(description = "Search memory entries by matching keys or content.")
  fun searchMemory(
    @ToolParam(description = "Search query to match against memory keys and values.") query: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call:
      // val results = memoryDao.search(query)

      val q = query.lowercase()
      val results = _memories.filter { (k, v) ->
        k.lowercase().contains(q) || v.lowercase().contains(q)
      }

      val items = results.entries.joinToString("\n") { (k, v) ->
        "🔑 $k → $v"
      }

      mapOf(
        "status" to if (results.isEmpty()) "empty" else "success",
        "count" to results.size.toString(),
        "memories" to items.ifEmpty { "Nicio memorie găsită pentru '$query'." },
      )
    }
  }

  // ───────────────────── REMINDERS ─────────────────────

  @Tool(description = "Set a reminder with title, datetime, and optional repeat pattern.")
  fun setReminder(
    @ToolParam(description = "Title of the reminder.") title: String,
    @ToolParam(description = "Date/time in ISO format (e.g. 2026-04-15T09:00:00).") datetime: String,
    @ToolParam(description = "Repeat pattern: 'none', 'daily', 'weekly', 'monthly'. Default: none.") repeat: String = "none",
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      // TODO(wire-dao): Replace with DAO call + AlarmManager scheduling:
      // val reminder = ReminderEntity(title = title, datetime = datetime, repeat = repeat, active = true)
      // reminderDao.insert(reminder)
      // scheduleAlarm(reminder)

      val reminder = mapOf(
        "title" to title,
        "datetime" to datetime,
        "repeat" to repeat,
        "active" to true,
        "createdAt" to System.currentTimeMillis().toString(),
      )
      _reminders.add(reminder)

      mapOf(
        "status" to "success",
        "title" to title,
        "datetime" to datetime,
        "repeat" to repeat,
        "message" to "Memento-ul a fost programat pentru $datetime.",
      )
    }
  }
}
