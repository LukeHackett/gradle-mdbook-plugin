package com.lukehackett.gradle.mdbook.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Base class for all tasks that run the mdbook command line tool.
 *
 * It owns the two things every task needs: the [bookDir] (the directory containing `book.toml`) and the [executable].
 * It is also the only place that starts a process, via the injected [ExecOperations], so that failures to start or run
 * mdbook are reported consistently.
 *
 * Subclasses are declared `abstract`, so that Gradle generates the property implementations, and take [ExecOperations]
 * as an injected constructor parameter.
 */
@DisableCachingByDefault(because = "Runs an external tool, whose output depends on the installed mdbook version")
abstract class AbstractMdbookTask(private val execOperations: ExecOperations) : DefaultTask() {
  /**
   * The directory containing the book's `book.toml`.
   *
   * Not annotated as an input here, as `mdbookInit` creates the book and so the directory may not exist yet.
   */
  @get:Internal abstract val bookDir: DirectoryProperty

  /** The mdbook binary to run, either a bare command resolved from the `PATH`, or a path to the binary. */
  @get:Input abstract val executable: Property<String>

  /** Fails the task when the [bookDir] does not contain a `book.toml`. */
  protected fun requireBookToml() {
    if (!bookTomlExists()) {
      throw GradleException(
          "No $BOOK_TOML found in '${bookDir.get().asFile}'. " +
              "Run 'mdbookInit' to create a new book, or point mdbook { bookDir } at the directory containing $BOOK_TOML."
      )
    }
  }

  /**
   * Fails the task when the [bookDir] already contains a `book.toml`, so that an existing book is never overwritten.
   */
  protected fun requireNoBookToml() {
    if (bookTomlExists()) {
      throw GradleException(
          "A $BOOK_TOML already exists in '${bookDir.get().asFile}', refusing to overwrite the existing book."
      )
    }
  }

  /**
   * Runs mdbook with the given [subCommand] (e.g. `build`) and any further [arguments].
   *
   * @throws GradleException if mdbook cannot be started, or exits with a non-zero exit code.
   */
  protected fun runMdbook(subCommand: String, vararg arguments: String) {
    val command = executable.get()
    val argumentList = listOf(subCommand) + arguments.toList()
    logger.info("Running: {} {}", command, argumentList.joinToString(" "))

    val result =
        try {
          // The kotlin-dsl plugin makes Gradle's Action lambdas receive the ExecSpec as `this`
          execOperations.exec {
            this.executable = command
            this.args(argumentList)
            // Handled below, so that the failure message names the mdbook sub command
            this.isIgnoreExitValue = true
          }
        } catch (e: GradleException) {
          throw GradleException(
              "Unable to run '$command'. Make sure mdbook is installed and on the PATH, " +
                  "or set mdbook { executable } to the location of the mdbook binary.",
              e,
          )
        }

    if (result.exitValue != 0) {
      throw GradleException("'$command $subCommand' failed with exit code ${result.exitValue}.")
    }
  }

  private fun bookTomlExists() = bookDir.get().file(BOOK_TOML).asFile.isFile

  private companion object {
    const val BOOK_TOML = "book.toml"
  }
}
