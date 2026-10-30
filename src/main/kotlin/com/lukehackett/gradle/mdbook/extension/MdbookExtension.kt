package com.lukehackett.gradle.mdbook.extension

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Configuration for the mdBook plugin, exposed in build scripts as the `mdbook {}` block.
 *
 * Conventions (defaults) are applied by [com.lukehackett.gradle.mdbook.GradleMdbookPlugin].
 *
 * ### Example Usage in build.gradle.kts:
 * ```kotlin
 * mdbook {
 *   // Directory containing book.toml (defaults to <project>/docs)
 *   bookDir.set(layout.projectDirectory.dir("docs"))
 *
 *   // Where the rendered book is written (defaults to <project>/build/mdbook)
 *   outputDir.set(layout.buildDirectory.dir("mdbook"))
 *
 *   // Optional: path to (or name of) the mdbook binary (defaults to "mdbook" on the PATH)
 *   executable.set("/opt/mdbook/bin/mdbook")
 * }
 * ```
 */
abstract class MdbookExtension {
  /** The directory containing the book's `book.toml`. */
  abstract val bookDir: DirectoryProperty

  /** The directory the rendered book is written to. Always passed to mdbook as `--dest-dir`. */
  abstract val outputDir: DirectoryProperty

  /** The mdbook binary to run. Either a bare command resolved from the `PATH`, or a path to the binary. */
  abstract val executable: Property<String>

  internal companion object {
    const val NAME = "mdbook"
    const val DEFAULT_BOOK_DIR = "docs"
    const val DEFAULT_OUTPUT_DIR = "mdbook"
    const val DEFAULT_EXECUTABLE = "mdbook"
  }
}
