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

/** Runs `mdbookBuild` against the real mdbook binary, which must be on the PATH. */
@Tag("integration")
class MdbookBuildIntegrationTest {
  @TempDir lateinit var tempDir: File

  private val project by lazy { TestProject.fromFixture("book", tempDir) }

  @Test
  fun `renders the book into build mdbook`() {
    // When
    val result = project.run("mdbookBuild")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookBuild")?.outcome)
    assertContains(project.file("build/mdbook/index.html").readText(), "Fixture Book")
    assertContains(project.file("build/mdbook/introduction.html").readText(), "Hello from the fixture book.")
  }

  @Test
  fun `is up to date when nothing has changed`() {
    // Given
    project.run("mdbookBuild")

    // When
    val result = project.run("mdbookBuild")

    // Then
    assertEquals(TaskOutcome.UP_TO_DATE, result.task(":mdbookBuild")?.outcome)
  }

  @Test
  fun `renders the book again when a chapter changes`() {
    // Given
    project.run("mdbookBuild")
    project.write("docs/src/introduction.md", "# Introduction\n\nThis text has changed.\n")

    // When
    val result = project.run("mdbookBuild")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookBuild")?.outcome)
    assertContains(project.file("build/mdbook/introduction.html").readText(), "This text has changed.")
  }

  @Test
  fun `renders the book again when the output has been deleted`() {
    // Given
    project.run("mdbookBuild")
    project.file("build/mdbook").deleteRecursively()

    // When
    val result = project.run("mdbookBuild")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookBuild")?.outcome)
    assertTrue(project.file("build/mdbook/index.html").isFile)
  }

  @Test
  fun `renders into a configured output directory and book directory`() {
    // Given
    project.file("docs").renameTo(project.file("guide"))
    project.write(
        "build.gradle.kts",
        """
        plugins { id("com.lukehackett.gradle.mdbook") }

        mdbook {
          bookDir.set(layout.projectDirectory.dir("guide"))
          outputDir.set(layout.buildDirectory.dir("site"))
        }
        """
            .trimIndent(),
    )

    // When
    project.run("mdbookBuild")

    // Then
    assertTrue(project.file("build/site/index.html").isFile, "the book should be rendered into build/site")
    assertTrue(!project.file("build/mdbook").exists(), "the default output directory should not be used")
  }

  @Test
  fun `the configured output directory wins over the build-dir in book toml`() {
    // Given
    project.write(
        "docs/book.toml",
        """
        [book]
        title = "Fixture Book"

        [build]
        build-dir = "elsewhere"
        """
            .trimIndent(),
    )

    // When
    project.run("mdbookBuild")

    // Then
    assertTrue(project.file("build/mdbook/index.html").isFile, "the book should be rendered into build/mdbook")
    assertTrue(!project.file("docs/elsewhere").exists(), "the build-dir from book.toml should be ignored")
  }

  @Test
  fun `fails with a clear message when there is no book toml`() {
    // Given
    val emptyProject = TestProject.fromFixture("empty", File(tempDir, "empty").apply { mkdirs() })

    // When
    val result = emptyProject.runAndFail("mdbookBuild")

    // Then
    assertContains(result.output, "No book.toml found")
    assertContains(result.output, "mdbookInit")
  }
}
