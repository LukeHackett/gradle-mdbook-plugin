package com.lukehackett.gradle.mdbook.tasks

import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Renders the book into the [outputDir], by running `mdbook build`.
 *
 * The files in the [bookDir] are the inputs and the [outputDir] is the output, so Gradle skips the task when nothing
 * has changed since the last time the book was rendered.
 *
 * The [outputDir] is always passed to mdbook as `--dest-dir`, so it wins over any `build-dir` set in `book.toml`.
 */
@DisableCachingByDefault(
    because =
        "Rendering depends on the installed mdbook version, so the output is only reused locally (up-to-date checks)"
)
abstract class MdbookBuildTask @Inject constructor(execOperations: ExecOperations) :
    AbstractMdbookTask(execOperations) {

  /** The directory the rendered book is written to. */
  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  /**
   * Everything in the [bookDir], so that a change to `book.toml`, a chapter, or the theme renders the book again.
   *
   * This is a file tree rather than a directory input, so a missing [bookDir] reaches [requireBookToml] and its clear
   * error message, instead of Gradle's validation failure.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  val bookFiles: FileTree
    get() = bookDir.asFileTree

  /** Renders the book. */
  @TaskAction
  fun buildBook() {
    requireBookToml()
    runMdbook("build", bookDir.get().asFile.absolutePath, "--dest-dir", outputDir.get().asFile.absolutePath)
  }
}
