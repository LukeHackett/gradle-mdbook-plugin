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

/** Runs `mdbookInit` against the real mdbook binary, which must be on the PATH. */
@Tag("integration")
class MdbookInitIntegrationTest {
  @TempDir lateinit var tempDir: File

  @Test
  fun `creates a new book in the docs directory`() {
    // Given
    val project = TestProject.fromFixture("empty", tempDir)

    // When
    val result = project.run("mdbookInit")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookInit")?.outcome)
    assertTrue(project.file("docs/book.toml").isFile, "docs/book.toml should exist")
    assertTrue(project.file("docs/src/SUMMARY.md").isFile, "docs/src/SUMMARY.md should exist")
  }

  @Test
  fun `creates the book in a configured book directory`() {
    // Given
    val project = TestProject.fromFixture("empty", tempDir)
    project.write(
        "build.gradle.kts",
        """
        plugins { id("com.lukehackett.gradle.mdbook") }

        mdbook { bookDir.set(layout.projectDirectory.dir("guide")) }
        """
            .trimIndent(),
    )

    // When
    project.run("mdbookInit")

    // Then
    assertTrue(project.file("guide/book.toml").isFile, "guide/book.toml should exist")
    assertTrue(!project.file("docs").exists(), "the default docs directory should not be created")
  }

  @Test
  fun `refuses to overwrite an existing book`() {
    // Given
    val project = TestProject.fromFixture("empty", tempDir)
    project.write("docs/book.toml", "[book]\ntitle = \"Mine\"\n")

    // When
    val result = project.runAndFail("mdbookInit")

    // Then
    assertContains(result.output, "A book.toml already exists")
    assertEquals("[book]\ntitle = \"Mine\"\n", project.file("docs/book.toml").readText())
    assertTrue(!project.file("docs/src").exists(), "nothing else should be created")
  }

  @Test
  fun `explains how to configure the executable when mdbook cannot be found`() {
    // Given
    val project = TestProject.fromFixture("empty", tempDir)
    project.write(
        "build.gradle.kts",
        """
        plugins { id("com.lukehackett.gradle.mdbook") }

        mdbook { executable.set("a-binary-that-does-not-exist") }
        """
            .trimIndent(),
    )

    // When
    val result = project.runAndFail("mdbookInit")

    // Then
    assertContains(result.output, "Unable to run 'a-binary-that-does-not-exist'")
    assertContains(result.output, "mdbook { executable }")
  }
}
