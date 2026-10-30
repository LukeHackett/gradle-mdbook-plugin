package com.lukehackett.gradle.mdbook.tasks

import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Deletes the rendered book, by running `mdbook clean`.
 *
 * The [outputDir] is always passed to mdbook as `--dest-dir`, so it is the directory that is deleted, whatever
 * `build-dir` is set to in `book.toml`.
 *
 * The task has no declared outputs, so it always runs.
 */
@DisableCachingByDefault(because = "Deletes files, and there is no output to reuse from a cache")
abstract class MdbookCleanTask @Inject constructor(execOperations: ExecOperations) :
    AbstractMdbookTask(execOperations) {

  /** The directory the rendered book was written to, which is deleted. */
  @get:Internal abstract val outputDir: DirectoryProperty

  /** Deletes the rendered book. */
  @TaskAction
  fun cleanBook() {
    requireBookToml()
    runMdbook("clean", bookDir.get().asFile.absolutePath, "--dest-dir", outputDir.get().asFile.absolutePath)
  }
}
