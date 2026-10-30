package com.lukehackett.gradle.mdbook

import com.lukehackett.gradle.mdbook.extension.MdbookExtension
import com.lukehackett.gradle.mdbook.tasks.AbstractMdbookTask
import com.lukehackett.gradle.mdbook.tasks.MdbookBuildTask
import com.lukehackett.gradle.mdbook.tasks.MdbookCleanTask
import com.lukehackett.gradle.mdbook.tasks.MdbookInitTask
import com.lukehackett.gradle.mdbook.tasks.MdbookServeTask
import com.lukehackett.gradle.mdbook.tasks.MdbookTestTask
import com.lukehackett.gradle.mdbook.tasks.MdbookWatchTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * A Gradle plugin that wraps the [mdBook](https://rust-lang.github.io/mdBook/) command line tool.
 *
 * Applying the plugin registers the `mdbook {}` extension (see [MdbookExtension]) and the `mdbook*` tasks.
 *
 * Usage in build.gradle.kts:
 * ```
 * plugins {
 *     id("com.lukehackett.gradle.mdbook")
 * }
 * ```
 */
class GradleMdbookPlugin : Plugin<Project> {
  /**
   * Applies the plugin to the specified [Project].
   *
   * The extension holds only Gradle lazy properties, and no [Project] reference is retained, which keeps the plugin
   * compatible with Gradle's configuration cache.
   *
   * @param project The project to which the plugin is being applied.
   */
  override fun apply(project: Project) {
    val extension = project.extensions.create(MdbookExtension.NAME, MdbookExtension::class.java)

    extension.bookDir.convention(project.layout.projectDirectory.dir(MdbookExtension.DEFAULT_BOOK_DIR))
    extension.outputDir.convention(project.layout.buildDirectory.dir(MdbookExtension.DEFAULT_OUTPUT_DIR))
    extension.executable.convention(MdbookExtension.DEFAULT_EXECUTABLE)

    val init =
        project.registerMdbookTask<MdbookInitTask>(
            "mdbookInit",
            "Creates a new book with minimal boilerplate.",
            extension,
        )

    // Registered before the build task, which is ordered after it
    val clean =
        project.registerMdbookTask<MdbookCleanTask>("mdbookClean", "Deletes the rendered book.", extension) {
          outputDir.convention(extension.outputDir)
        }
    project.registerMdbookTask<MdbookBuildTask>("mdbookBuild", "Renders the book.", extension) {
      outputDir.convention(extension.outputDir)

      // Like `gradle clean build`: when both are requested, the book is deleted before it is rendered, whichever order
      // they are requested in. Otherwise the rendered book could be deleted straight after it has been rendered, and
      // Gradle would not notice, so it would treat the next `mdbookBuild` as up to date.
      mustRunAfter(clean)
    }
    project.registerMdbookTask<MdbookWatchTask>(
        "mdbookWatch",
        "Renders the book again every time a source file changes.",
        extension,
    ) {
      outputDir.convention(extension.outputDir)
    }
    project.registerMdbookTask<MdbookServeTask>(
        "mdbookServe",
        "Serves the book over HTTP, rendering it again every time a source file changes.",
        extension,
    ) {
      outputDir.convention(extension.outputDir)
    }
    project.registerMdbookTask<MdbookTestTask>("mdbookTest", "Tests the Rust code samples in the book.", extension)

    // Gradle does not run unrelated tasks in the order they are requested, so say that a book has to be created before
    // anything else uses it. This only orders the tasks, it does not make them depend on `mdbookInit`.
    project.tasks.withType(AbstractMdbookTask::class.java).configureEach {
      if (this !is MdbookInitTask) mustRunAfter(init)
    }
  }

  /**
   * Registers a task, wired to the settings of the [extension].
   *
   * The settings are conventions (defaults) rather than values, so a build script can still configure an individual
   * task differently from the `mdbook {}` block.
   */
  private inline fun <reified T : AbstractMdbookTask> Project.registerMdbookTask(
      name: String,
      description: String,
      extension: MdbookExtension,
      crossinline configure: T.() -> Unit = {},
  ): TaskProvider<T> =
      tasks.register(name, T::class.java) {
        this.group = TASK_GROUP
        this.description = description
        this.bookDir.convention(extension.bookDir)
        this.executable.convention(extension.executable)
        configure()
      }

  private companion object {
    const val TASK_GROUP = "mdbook"
  }
}
