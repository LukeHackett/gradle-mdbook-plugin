package com.lukehackett.gradle.mdbook.tasks

import javax.inject.Inject
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Creates a new book with minimal boilerplate, by running `mdbook init`.
 *
 * Fails, rather than overwrite anything, when the book directory already contains a `book.toml`.
 *
 * mdbook asks for a title and whether to create a `.gitignore` unless told not to, which would hang a build that has no
 * terminal, so the prompts are always skipped with `--force`.
 */
@DisableCachingByDefault(because = "Creates the book on disk, and there is nothing to reuse from a cache")
abstract class MdbookInitTask @Inject constructor(execOperations: ExecOperations) : AbstractMdbookTask(execOperations) {

  /** Creates the book in the [bookDir]. */
  @TaskAction
  fun initBook() {
    requireNoBookToml()
    runMdbook("init", bookDir.get().asFile.absolutePath, "--force")
  }
}
