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

class MdbookBuildTaskTest {
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

  private fun createTask(exec: RecordingExecOperations): MdbookBuildTask =
      project.tasks.register("mdbookBuild", MdbookBuildTask::class.java, exec.operations).get().apply {
        bookDir.set(this@MdbookBuildTaskTest.bookDir)
        outputDir.set(this@MdbookBuildTaskTest.outputDir)
        executable.set("mdbook")
      }

  private fun givenBook() {
    File(bookDir, "src").mkdirs()
    File(bookDir, "book.toml").writeText("[book]\ntitle = \"Test\"\n")
    File(bookDir, "src/SUMMARY.md").writeText("# Summary\n")
  }

  @Test
  fun `renders the book into the output directory`() {
    // Given
    givenBook()
    val exec = RecordingExecOperations()

    // When
    createTask(exec).buildBook()

    // Then
    assertEquals(listOf("mdbook"), exec.executables)
    assertEquals(
        listOf(listOf("build", bookDir.absolutePath, "--dest-dir", outputDir.absolutePath)),
        exec.arguments,
    )
  }

  @Test
  fun `fails without running mdbook when there is no book toml`() {
    // Given
    val exec = RecordingExecOperations()

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).buildBook() }

    // Then
    assertContains(failure.message.orEmpty(), "No book.toml found in '$bookDir'")
    assertTrue(exec.arguments.isEmpty(), "mdbook must not be run without a book.toml")
  }

  @Test
  fun `treats every file in the book directory as an input`() {
    // Given
    givenBook()

    // When
    val inputs = createTask(RecordingExecOperations()).bookFiles.files.map { it.name }

    // Then
    assertEquals(listOf("SUMMARY.md", "book.toml"), inputs.sorted())
  }

  @Test
  fun `has no inputs, rather than failing, when the book directory does not exist`() {
    // When
    val inputs = createTask(RecordingExecOperations()).bookFiles.files

    // Then
    assertTrue(inputs.isEmpty())
  }
}
