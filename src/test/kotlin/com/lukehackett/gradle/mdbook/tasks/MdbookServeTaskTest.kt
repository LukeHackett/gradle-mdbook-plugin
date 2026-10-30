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

class MdbookServeTaskTest {
  @TempDir lateinit var tempDir: File

  private lateinit var project: Project
  private lateinit var bookDir: File
  private lateinit var outputDir: File

  @BeforeEach
  fun setUp() {
    bookDir = File(tempDir.canonicalFile, "docs")
    outputDir = File(tempDir.canonicalFile, "build/mdbook")
    project = ProjectBuilder.builder().withProjectDir(tempDir.canonicalFile).build()
  }

  private fun createTask(exec: RecordingExecOperations): MdbookServeTask =
      project.tasks.register("mdbookServe", MdbookServeTask::class.java, exec.operations).get().apply {
        bookDir.set(this@MdbookServeTaskTest.bookDir)
        outputDir.set(this@MdbookServeTaskTest.outputDir)
        executable.set("mdbook")
      }

  @Test
  fun `runs mdbook serve for the book into the output directory`() {
    // Given
    bookDir.mkdirs()
    File(bookDir, "book.toml").writeText("[book]\ntitle = \"Test\"\n")
    val exec = RecordingExecOperations()

    // When
    createTask(exec).serveBook()

    // Then
    assertEquals(listOf("mdbook"), exec.executables)
    assertEquals(
        listOf(listOf("serve", bookDir.absolutePath, "--dest-dir", outputDir.absolutePath)),
        exec.arguments,
    )
  }

  @Test
  fun `fails without running mdbook when there is no book toml`() {
    // Given
    val exec = RecordingExecOperations()

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).serveBook() }

    // Then
    assertContains(failure.message.orEmpty(), "No book.toml found in '$bookDir'")
    assertTrue(exec.arguments.isEmpty(), "mdbook must not be run without a book.toml")
  }
}
