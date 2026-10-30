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

class MdbookTestTaskTest {
  @TempDir lateinit var tempDir: File

  private lateinit var project: Project
  private lateinit var bookDir: File

  @BeforeEach
  fun setUp() {
    bookDir = File(tempDir.canonicalFile, "docs")
    project = ProjectBuilder.builder().withProjectDir(tempDir.canonicalFile).build()
  }

  private fun createTask(exec: RecordingExecOperations): MdbookTestTask =
      project.tasks.register("mdbookTest", MdbookTestTask::class.java, exec.operations).get().apply {
        bookDir.set(this@MdbookTestTaskTest.bookDir)
        executable.set("mdbook")
      }

  @Test
  fun `tests the book in the book directory`() {
    // Given
    bookDir.mkdirs()
    File(bookDir, "book.toml").writeText("[book]\ntitle = \"Test\"\n")
    val exec = RecordingExecOperations()

    // When
    createTask(exec).testBook()

    // Then
    assertEquals(listOf("mdbook"), exec.executables)
    assertEquals(listOf(listOf("test", bookDir.absolutePath)), exec.arguments)
  }

  @Test
  fun `fails when a code sample fails`() {
    // Given
    bookDir.mkdirs()
    File(bookDir, "book.toml").writeText("[book]\ntitle = \"Test\"\n")
    val exec = RecordingExecOperations(exitValue = 101)

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).testBook() }

    // Then
    assertEquals("'mdbook test' failed with exit code 101.", failure.message)
  }

  @Test
  fun `fails without running mdbook when there is no book toml`() {
    // Given
    val exec = RecordingExecOperations()

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).testBook() }

    // Then
    assertContains(failure.message.orEmpty(), "No book.toml found in '$bookDir'")
    assertTrue(exec.arguments.isEmpty(), "mdbook must not be run without a book.toml")
  }
}
