package com.lukehackett.gradle.mdbook

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Follows a book through its whole life, the way a user would: create it, write it, render it, test it, and delete it.
 *
 * Needs the real mdbook binary on the PATH, along with a Rust toolchain.
 */
@Tag("integration")
class MdbookWorkflowIntegrationTest {
  @TempDir lateinit var tempDir: File

  @Test
  fun `a book can be created, rendered, tested and deleted`() {
    val project = TestProject.fromFixture("empty", tempDir)

    // A new book is created
    assertEquals(TaskOutcome.SUCCESS, project.run("mdbookInit").task(":mdbookInit")?.outcome)
    assertTrue(project.file("docs/book.toml").isFile)

    // It is written
    project.write(
        "docs/src/chapter_1.md",
        """
        # Chapter 1

        Written by the workflow test.

        ```rust
        fn main() {
            assert_eq!(2 * 2, 4);
        }
        ```
        """
            .trimIndent(),
    )

    // It is rendered
    assertEquals(TaskOutcome.SUCCESS, project.run("mdbookBuild").task(":mdbookBuild")?.outcome)
    assertContains(project.file("build/mdbook/chapter_1.html").readText(), "Written by the workflow test.")

    // Its code samples are tested
    assertEquals(TaskOutcome.SUCCESS, project.run("mdbookTest").task(":mdbookTest")?.outcome)

    // Rendering it again changes nothing
    assertEquals(TaskOutcome.UP_TO_DATE, project.run("mdbookBuild").task(":mdbookBuild")?.outcome)

    // It is deleted, but only the rendered book
    assertEquals(TaskOutcome.SUCCESS, project.run("mdbookClean").task(":mdbookClean")?.outcome)
    assertTrue(!project.file("build/mdbook").exists(), "the rendered book should be deleted")
    assertTrue(project.file("docs/src/chapter_1.md").isFile, "the sources should be kept")
  }

  @Test
  fun `every task can be run together in one build`() {
    // Given
    val project = TestProject.fromFixture("empty", tempDir)

    // When
    val result = project.run("mdbookInit", "mdbookBuild", "mdbookTest", "mdbookClean")

    // Then the book is created first, and deleted before it is rendered
    val tasks = result.tasks.map { it.path }
    assertEquals(4, tasks.size)
    assertEquals(":mdbookInit", tasks.first())
    assertTrue(tasks.indexOf(":mdbookClean") < tasks.indexOf(":mdbookBuild"), "clean should run before build: $tasks")
    assertTrue(result.tasks.all { it.outcome == TaskOutcome.SUCCESS })
  }
}
