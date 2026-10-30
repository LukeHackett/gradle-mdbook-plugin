package com.lukehackett.gradle.mdbook.tasks

import com.lukehackett.gradle.mdbook.TestProject
import java.io.File
import kotlin.test.assertContains
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Runs `mdbookWatch` against the real mdbook binary, which must be on the PATH.
 *
 * The task never finishes by itself, so each test stops mdbook once it has seen what it needs to, which fails the
 * build.
 */
@Tag("integration")
class MdbookWatchIntegrationTest {
  @TempDir lateinit var tempDir: File

  private val project by lazy { TestProject.fromFixture("book", tempDir) }

  @Test
  fun `renders the book again when a source file changes`() {
    // Given
    val build = project.runInBackground("mdbookWatch")

    try {
      // When a chapter is changed. mdbook only renders the book after the first change, and cannot see changes made
      // before it has started watching, so the chapter is written again until the book has been rendered.
      build.await("the changed chapter to be rendered", intervalMillis = WRITE_INTERVAL_MILLIS) {
        project.write("docs/src/introduction.md", "# Introduction\n\nThis text was changed while watching.\n")
        project.file("build/mdbook/introduction.html").takeIf { it.isFile }?.readText()?.contains(CHANGED_TEXT) == true
      }
    } finally {
      // Then
      assertContains(build.stop().output, "'mdbook watch' failed")
    }
  }

  private companion object {
    const val CHANGED_TEXT = "This text was changed while watching."
    const val WRITE_INTERVAL_MILLIS = 2_000L
  }
}
