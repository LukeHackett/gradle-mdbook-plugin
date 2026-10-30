package com.lukehackett.gradle.mdbook.tasks

import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Renders the book, and renders it again every time a source file changes, by running `mdbook watch`.
 *
 * The task runs until the build is stopped (e.g. with Ctrl-C), so it never finishes by itself. It has no declared
 * outputs, so it always runs.
 *
 * The [outputDir] is always passed to mdbook as `--dest-dir`, so it wins over any `build-dir` set in `book.toml`.
 */
@DisableCachingByDefault(because = "Runs until the build is stopped, and produces no output to cache")
abstract class MdbookWatchTask @Inject constructor(execOperations: ExecOperations) :
    AbstractMdbookTask(execOperations) {

  /** The directory the rendered book is written to. */
  @get:Internal abstract val outputDir: DirectoryProperty

  /** Renders the book, then watches for changes until the build is stopped. */
  @TaskAction
  fun watchBook() {
    requireBookToml()
    runMdbook("watch", bookDir.get().asFile.absolutePath, "--dest-dir", outputDir.get().asFile.absolutePath)
  }
}
