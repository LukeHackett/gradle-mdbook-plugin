package com.lukehackett.gradle.mdbook.tasks

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MdbookInitTaskTest {
  @TempDir lateinit var tempDir: File

  private lateinit var project: Project
  private lateinit var bookDir: File

  @BeforeEach
  fun setUp() {
    bookDir = File(tempDir.canonicalFile, "docs")
    project = ProjectBuilder.builder().withProjectDir(tempDir.canonicalFile).build()
  }

  private fun createTask(exec: RecordingExecOperations): MdbookInitTask =
      project.tasks.register("mdbookInit", MdbookInitTask::class.java, exec.operations).get().apply {
        bookDir.set(this@MdbookInitTaskTest.bookDir)
        executable.set("mdbook")
      }

  @Test
  fun `initialises a book in the book directory without prompting`() {
    // Given
    val exec = RecordingExecOperations()

    // When
    createTask(exec).initBook()

    // Then
    assertEquals(listOf("mdbook"), exec.executables)
    assertEquals(listOf(listOf("init", bookDir.absolutePath, "--force")), exec.arguments)
  }

  @Test
  fun `initialises a book when the book directory already exists but is empty`() {
    // Given
    bookDir.mkdirs()
    val exec = RecordingExecOperations()

    // When
    createTask(exec).initBook()

    // Then
    assertEquals(1, exec.arguments.size)
  }

  @Test
  fun `refuses to overwrite an existing book`() {
    // Given
    bookDir.mkdirs()
    File(bookDir, "book.toml").writeText("[book]\ntitle = \"Mine\"\n")
    val exec = RecordingExecOperations()

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).initBook() }

    // Then
    assertContains(failure.message.orEmpty(), "already exists")
    assertTrue(exec.arguments.isEmpty(), "mdbook must not be run when a book already exists")
    assertEquals("[book]\ntitle = \"Mine\"\n", File(bookDir, "book.toml").readText())
  }
}
