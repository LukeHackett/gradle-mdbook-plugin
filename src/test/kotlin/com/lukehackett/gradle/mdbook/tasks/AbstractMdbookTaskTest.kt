package com.lukehackett.gradle.mdbook.tasks

import java.io.File
import javax.inject.Inject
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AbstractMdbookTaskTest {
  /** Exposes the protected helpers of [AbstractMdbookTask] so they can be tested directly. */
  abstract class ExposedTask @Inject constructor(execOperations: ExecOperations) : AbstractMdbookTask(execOperations) {
    fun checkBookTomlExists() = requireBookToml()

    fun checkBookTomlMissing() = requireNoBookToml()

    fun run(subCommand: String, vararg arguments: String) = runMdbook(subCommand, *arguments)
  }

  @TempDir lateinit var tempDir: File

  private lateinit var project: Project
  private lateinit var bookDir: File

  @BeforeEach
  fun setUp() {
    bookDir = File(tempDir.canonicalFile, "docs")
    project = ProjectBuilder.builder().withProjectDir(tempDir.canonicalFile).build()
  }

  private fun createTask(exec: RecordingExecOperations = RecordingExecOperations()): ExposedTask =
      project.tasks.register("exposed", ExposedTask::class.java, exec.operations).get().apply {
        bookDir.set(this@AbstractMdbookTaskTest.bookDir)
        executable.set("mdbook")
      }

  private fun givenBookToml() {
    bookDir.mkdirs()
    File(bookDir, "book.toml").writeText("[book]\ntitle = \"Test\"\n")
  }

  @Test
  fun `requireBookToml passes when book toml exists`() {
    // Given
    givenBookToml()

    // When / Then
    createTask().checkBookTomlExists()
  }

  @Test
  fun `requireBookToml fails when the book directory does not exist`() {
    // When
    val failure = assertFailsWith<GradleException> { createTask().checkBookTomlExists() }

    // Then
    assertContains(failure.message.orEmpty(), "No book.toml found in '$bookDir'")
    assertContains(failure.message.orEmpty(), "mdbookInit")
  }

  @Test
  fun `requireBookToml fails when the book directory has no book toml`() {
    // Given
    bookDir.mkdirs()

    // When
    val failure = assertFailsWith<GradleException> { createTask().checkBookTomlExists() }

    // Then
    assertContains(failure.message.orEmpty(), "No book.toml found")
  }

  @Test
  fun `requireNoBookToml passes when book toml is absent`() {
    // When / Then
    createTask().checkBookTomlMissing()
  }

  @Test
  fun `requireNoBookToml fails when book toml already exists`() {
    // Given
    givenBookToml()

    // When
    val failure = assertFailsWith<GradleException> { createTask().checkBookTomlMissing() }

    // Then
    assertContains(failure.message.orEmpty(), "A book.toml already exists in '$bookDir'")
    assertContains(failure.message.orEmpty(), "refusing to overwrite")
  }

  @Test
  fun `runMdbook starts the configured executable with the sub command and arguments`() {
    // Given
    val exec = RecordingExecOperations()

    // When
    createTask(exec).run("build", "docs", "--dest-dir", "out")

    // Then
    assertEquals(listOf("mdbook"), exec.executables)
    assertEquals(listOf(listOf("build", "docs", "--dest-dir", "out")), exec.arguments)
  }

  @Test
  fun `runMdbook uses a custom executable`() {
    // Given
    val exec = RecordingExecOperations()
    val task = createTask(exec).apply { executable.set("/opt/mdbook/bin/mdbook") }

    // When
    task.run("clean")

    // Then
    assertEquals(listOf("/opt/mdbook/bin/mdbook"), exec.executables)
  }

  @Test
  fun `runMdbook handles the exit value itself`() {
    // Given
    val exec = RecordingExecOperations()

    // When
    createTask(exec).run("build")

    // Then
    assertEquals(listOf(true), exec.ignoredExitValues)
  }

  @Test
  fun `runMdbook fails with the sub command and exit code when mdbook exits with a non-zero exit code`() {
    // Given
    val exec = RecordingExecOperations(exitValue = 101)

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).run("test") }

    // Then
    assertEquals("'mdbook test' failed with exit code 101.", failure.message)
  }

  @Test
  fun `runMdbook explains how to configure the executable when mdbook cannot be started`() {
    // Given
    val cause = GradleException("A problem occurred starting process 'command 'mdbook''")
    val exec = RecordingExecOperations(startFailure = cause)

    // When
    val failure = assertFailsWith<GradleException> { createTask(exec).run("build") }

    // Then
    assertContains(failure.message.orEmpty(), "Unable to run 'mdbook'")
    assertContains(failure.message.orEmpty(), "mdbook { executable }")
    assertTrue(failure.cause === cause, "The original failure should be kept as the cause")
    assertIs<GradleException>(failure.cause)
  }
}
