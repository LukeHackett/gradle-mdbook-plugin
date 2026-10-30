package com.lukehackett.gradle.mdbook.tasks

import com.lukehackett.gradle.mdbook.TestProject
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Runs `mdbookClean` against the real mdbook binary, which must be on the PATH. */
@Tag("integration")
class MdbookCleanIntegrationTest {
  @TempDir lateinit var tempDir: File

  private val project by lazy { TestProject.fromFixture("book", tempDir) }

  @Test
  fun `deletes the rendered book`() {
    // Given
    project.run("mdbookBuild")
    assertTrue(project.file("build/mdbook/index.html").isFile, "the book should be rendered first")

    // When
    val result = project.run("mdbookClean")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookClean")?.outcome)
    assertTrue(!project.file("build/mdbook").exists(), "the output directory should be deleted")
  }

  @Test
  fun `keeps the book sources`() {
    // Given
    project.run("mdbookBuild")

    // When
    project.run("mdbookClean")

    // Then
    assertTrue(project.file("docs/book.toml").isFile)
    assertTrue(project.file("docs/src/introduction.md").isFile)
  }

  @Test
  fun `succeeds when there is nothing to delete`() {
    // When
    val result = project.run("mdbookClean")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookClean")?.outcome)
  }

  @Test
  fun `the book is rendered again after being cleaned`() {
    // Given
    project.run("mdbookBuild")
    project.run("mdbookClean")

    // When
    val result = project.run("mdbookBuild")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookBuild")?.outcome)
    assertTrue(project.file("build/mdbook/index.html").isFile)
  }

  @Test
  fun `deletes the book before rendering it when both are requested, whichever order they are requested in`() {
    // When
    val result = project.run("mdbookBuild", "mdbookClean")

    // Then
    assertEquals(listOf(":mdbookClean", ":mdbookBuild"), result.tasks.map { it.path })
    assertTrue(project.file("build/mdbook/index.html").isFile, "the book should be rendered after it was cleaned")
  }

  @Test
  fun `always runs, even when there is nothing to delete`() {
    // Given
    project.run("mdbookClean")

    // When
    val result = project.run("mdbookClean")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookClean")?.outcome)
  }

  @Test
  fun `deletes the configured output directory`() {
    // Given
    project.write(
        "build.gradle.kts",
        """
        plugins { id("com.lukehackett.gradle.mdbook") }

        mdbook { outputDir.set(layout.buildDirectory.dir("site")) }
        """
            .trimIndent(),
    )
    project.run("mdbookBuild")

    // When
    project.run("mdbookClean")

    // Then
    assertTrue(!project.file("build/site").exists(), "the configured output directory should be deleted")
  }

  @Test
  fun `fails with a clear message when there is no book toml`() {
    // Given
    val emptyProject = TestProject.fromFixture("empty", File(tempDir, "empty").apply { mkdirs() })

    // When
    val result = emptyProject.runAndFail("mdbookClean")

    // Then
    assertContains(result.output, "No book.toml found")
  }
}
