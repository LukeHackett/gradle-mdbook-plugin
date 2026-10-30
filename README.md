<!-- Project Introduction -->
<div align="center">
  <a href="https://github.com/LukeHackett/gradle-mdbook-plugin">
    <img src="./.github/docs/logo.png" alt="Logo" width="400" height="125">
  </a>
  <h3 align="center">gradle-mdbook-plugin</h3>
  <p align="center">
    A <a href="https://gradle.org/">Gradle</a> plugin for creating, building, serving, and testing
    <a href="https://rust-lang.github.io/mdBook/">mdBook</a> sites
  </p>
  <p align="center">
    <img src="https://img.shields.io/github/stars/LukeHackett/gradle-mdbook-plugin?label=GitHub%20Stars&style=flat-square" alt="GitHub Repository Stars" />
    <a href="https://github.com/LukeHackett/gradle-mdbook-plugin/blob/main/LICENSE">
      <img src="https://img.shields.io/github/license/LukeHackett/gradle-mdbook-plugin?style=flat-square" alt="License" />
    </a>
    <img src="https://img.shields.io/github/last-commit/LukeHackett/gradle-mdbook-plugin?style=flat-square" alt="Last Commit" />
    <a href="https://github.com/LukeHackett/gradle-mdbook-plugin/releases">
      <img src="https://img.shields.io/github/v/release/LukeHackett/gradle-mdbook-plugin?style=flat-square" alt="Latest Release" />
    </a>
    <a href="https://github.com/LukeHackett/gradle-mdbook-plugin/issues">
      <img src="https://img.shields.io/github/issues/LukeHackett/gradle-mdbook-plugin?style=flat-square" alt="Issues" />
    </a>
  </p>
</div>

<!-- Core Features -->
## Features

- 📖 **Every mdBook command as a Gradle task** — `init`, `build`, `watch`, `serve`, `test`, and `clean`
- ♻️ **Incremental builds** — `mdbookBuild` is skipped by Gradle when nothing in the book has changed
- 📁 **Gradle-owned output** — the rendered book always goes to `build/mdbook` (or wherever you configure), whatever `book.toml` says
- ⚙️ **Small configuration** — the book directory, the output directory, and the `mdbook` binary, in an `mdbook {}` block
- 🛡️ **Safe by default** — `mdbookInit` never overwrites an existing book, and every other task explains what is missing
- 🔌 **Opt-in lifecycle wiring** — the tasks are standalone, so you decide whether `build`, `check`, or `clean` should run them
- 🧩 **Kotlin & Groovy Compatible** — Works in both `build.gradle.kts` and `build.gradle` scripts, and with the configuration cache

<!-- Getting Started -->
## Installation

### Prerequisites

The plugin runs the [`mdbook`](https://rust-lang.github.io/mdBook/guide/installation.html) command line tool, which
needs to be installed and on your `PATH` (or configured with [`executable`](#configuration)).

`mdbookTest` also needs a [Rust](https://www.rust-lang.org/tools/install) toolchain, as mdBook uses `rustdoc` to test
the code samples in your book.

### Applying the plugin

Apply the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.lukehackett.gradle.mdbook") version "1.0.0"
}
```

<details>
<summary>Using Groovy (build.gradle)</summary>

```groovy
plugins {
    id 'com.lukehackett.gradle.mdbook' version '1.0.0'
}
```

</details>

<details>
<summary>Using Legacy Plugin Application</summary>

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.lukehackett.gradle.mdbook:gradle-mdbook-plugin:1.0.0")
    }
}

apply(plugin = "com.lukehackett.gradle.mdbook")
```

</details>

<!-- Usage -->
## Usage

Once the plugin is applied, the `mdbook*` tasks and an `mdbook` extension are available in your build. By default the
book lives in the `docs` directory of your project (the directory containing `book.toml`), and is rendered into
`build/mdbook`.

```shell
# Create a new book in docs/
./gradlew mdbookInit

# Render it into build/mdbook
./gradlew mdbookBuild

# Preview it at http://localhost:3000, rendering it again whenever a source file changes
./gradlew mdbookServe
```

### Tasks

| Task           | mdBook command | Description                                                                          |
|----------------|----------------|--------------------------------------------------------------------------------------|
| `mdbookInit`   | `mdbook init`  | Creates a new book with minimal boilerplate.                                         |
| `mdbookBuild`  | `mdbook build` | Renders the book.                                                                    |
| `mdbookWatch`  | `mdbook watch` | Renders the book again every time a source file changes.                             |
| `mdbookServe`  | `mdbook serve` | Serves the book over HTTP, rendering it again every time a source file changes.      |
| `mdbookTest`   | `mdbook test`  | Tests the Rust code samples in the book.                                             |
| `mdbookClean`  | `mdbook clean` | Deletes the rendered book.                                                           |

All of the tasks are in the `mdbook` task group, so `./gradlew tasks --group mdbook` lists them.

#### `mdbookInit`

Creates `book.toml`, `src/SUMMARY.md` and a first chapter in the book directory.

It fails, rather than overwrite anything, when the book directory already contains a `book.toml`. It never asks for a
title or whether to create a `.gitignore` (it runs `mdbook init --force`), so it is safe to run without a terminal.
Edit the generated `book.toml` to set the title.

#### `mdbookBuild`

Renders the book into the output directory. The files in the book directory are the task's inputs and the output
directory is its output, so Gradle skips the task when nothing has changed since the book was last rendered.

The output directory is always passed to mdbook as `--dest-dir`, so a `build-dir` set in `book.toml` is ignored.

#### `mdbookWatch` and `mdbookServe`

Both run until you stop the build (Ctrl-C), so use them on their own rather than with other tasks.

- `mdbookWatch` renders the book again when a source file changes. mdBook only renders after the first change, so run
  `mdbookBuild` first if you need the book to exist straight away.
- `mdbookServe` renders the book, then serves it at `http://localhost:3000`. mdBook's host and port are not
  configurable through the plugin.

#### `mdbookTest`

Compiles and runs the Rust code samples in the book. Mark a sample you do not want tested with `rust,ignore`. The task
fails if any sample fails.

#### `mdbookClean`

Deletes the output directory, and nothing else. The sources of the book are untouched.

#### When there is no `book.toml`

Every task except `mdbookInit` fails with a message pointing at `mdbookInit`, and at the `bookDir` setting, when the
book directory does not contain a `book.toml`.

#### Order of the tasks

When several of the tasks are requested in one build, `mdbookInit` runs first, and `mdbookClean` runs before
`mdbookBuild`, as with `gradle clean build`. Nothing depends on anything else: requesting `mdbookBuild` does not run
`mdbookInit` or `mdbookClean`.

### Configuration

The `mdbook` extension supports the following configuration options:

| Option       | Description                                                                              | Default            |
|--------------|------------------------------------------------------------------------------------------|--------------------|
| `bookDir`    | The directory containing the book's `book.toml`.                                         | `docs`             |
| `outputDir`  | The directory the rendered book is written to.                                           | `build/mdbook`     |
| `executable` | The `mdbook` binary to run: a command on the `PATH`, or the path to the binary.         | `mdbook`           |

#### Full Configuration Example

```kotlin
plugins {
    id("com.lukehackett.gradle.mdbook") version "1.0.0"
}

mdbook {
    // The directory containing book.toml (relative paths are relative to the project)
    bookDir.set(layout.projectDirectory.dir("docs"))

    // Where the rendered book is written
    outputDir.set(layout.buildDirectory.dir("mdbook"))

    // Optional: use an mdbook that is not on the PATH
    executable.set("/opt/mdbook/bin/mdbook")
}
```

<details>
<summary>Using Groovy (build.gradle)</summary>

```groovy
mdbook {
    bookDir = layout.projectDirectory.dir('docs')
    outputDir = layout.buildDirectory.dir('mdbook')
    executable = '/opt/mdbook/bin/mdbook'
}
```

</details>

Each task takes its defaults from the extension, so a single task can still be configured differently:

```kotlin
import com.lukehackett.gradle.mdbook.tasks.MdbookBuildTask

tasks.named<MdbookBuildTask>("mdbookBuild") {
    outputDir.set(layout.buildDirectory.dir("site"))
}
```

### Hooking into the Gradle lifecycle

The tasks are not attached to `build`, `check`, or `clean`, so nothing runs mdbook unless you ask for it. Add the
dependencies you want:

```kotlin
// Render the book as part of `./gradlew build`
tasks.named("build") { dependsOn("mdbookBuild") }

// Test the code samples as part of `./gradlew check`
tasks.named("check") { dependsOn("mdbookTest") }

// Delete the rendered book as part of `./gradlew clean`
tasks.named("clean") { dependsOn("mdbookClean") }
```

<!-- Development -->
## Development

```shell
./gradlew test               # unit tests, with mdbook mocked
./gradlew architectureTest   # ArchUnit rules for the structure of the plugin
./gradlew check              # the above, plus formatting (ktfmt), static analysis (Detekt), and coverage (Kover)
./gradlew integrationTest    # runs the real mdbook
```

`test` and `check` need nothing but a JDK.

`integrationTest` runs the real `mdbook`, and is not part of `check`. It needs network access (to download `mdbook` the
first time) and a [Rust](https://www.rust-lang.org/tools/install) toolchain on the `PATH`, as `mdbookTest` uses
`rustdoc` to test the code samples. It is also slower than the other tests.

It does not use an `mdbook` you have installed. It depends on the `mdbookDownload` task, which saves a pinned version
of mdBook for your platform to `build/bin`, and puts that directory at the front of the `PATH` for the tests. The tests
therefore run against the same `mdbook` locally and in CI. You can run `./gradlew mdbookDownload` on its own to get the
binary.

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

Please review the [Contributing Guidelines](./CONTRIBUTING.md) if you would like to make a contribution to this project.

## Issues

If you encounter any other bugs or need some other features feel free to open an [issue](https://github.com/LukeHackett/gradle-mdbook-plugin/issues).

## License

Distributed under the MIT License. See [LICENSE.md](./LICENSE) for more information.
