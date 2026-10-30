package com.lukehackett.gradle.mdbook.tasks

import javax.inject.Inject
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Tests the Rust code samples in the book, by running `mdbook test`.
 *
 * mdbook uses `rustdoc` to compile and run the samples, so a Rust toolchain must be installed. The task has no outputs,
 * so it always runs.
 */
@DisableCachingByDefault(because = "Runs the code samples, which must run every time")
abstract class MdbookTestTask @Inject constructor(execOperations: ExecOperations) : AbstractMdbookTask(execOperations) {

  /** Tests the Rust code samples in the book. */
  @TaskAction
  fun testBook() {
    requireBookToml()
    runMdbook("test", bookDir.get().asFile.absolutePath)
  }
}
