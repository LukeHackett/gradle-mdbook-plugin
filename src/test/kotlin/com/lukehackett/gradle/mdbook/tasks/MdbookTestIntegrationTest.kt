package com.lukehackett.gradle.mdbook.tasks

import com.lukehackett.gradle.mdbook.TestProject
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Runs `mdbookTest` against the real mdbook binary, which must be on the PATH, along with a Rust toolchain. */
@Tag("integration")
class MdbookTestIntegrationTest {
  @TempDir lateinit var tempDir: File

  private val project by lazy { TestProject.fromFixture("book", tempDir) }

  @Test
  fun `passes when the Rust code samples pass`() {
    // When
    val result = project.run("mdbookTest")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookTest")?.outcome)
    assertContains(result.output, "Testing chapter 'Rust Samples'")
  }

  @Test
  fun `fails when a Rust code sample fails`() {
    // Given
    project.write(
        "docs/src/rust-samples.md",
        """
        # Rust Samples

        ```rust
        fn main() {
            assert_eq!(1 + 1, 3, "this sample is broken");
        }
        ```
        """
            .trimIndent(),
    )

    // When
    val result = project.runAndFail("mdbookTest")

    // Then
    assertContains(result.output, "'mdbook test' failed with exit code")
  }

  @Test
  fun `runs every time`() {
    // Given
    project.run("mdbookTest")

    // When
    val result = project.run("mdbookTest")

    // Then
    assertEquals(TaskOutcome.SUCCESS, result.task(":mdbookTest")?.outcome)
  }

  @Test
  fun `fails with a clear message when there is no book toml`() {
    // Given
    val emptyProject = TestProject.fromFixture("empty", File(tempDir, "empty").apply { mkdirs() })

    // When
    val result = emptyProject.runAndFail("mdbookTest")

    // Then
    assertContains(result.output, "No book.toml found")
  }
}
