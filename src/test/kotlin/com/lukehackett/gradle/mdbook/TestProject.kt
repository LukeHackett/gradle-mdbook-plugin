package com.lukehackett.gradle.mdbook

import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.fail
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * A throwaway Gradle build that applies the plugin under test, for integration tests that run the real mdbook binary.
 *
 * Every build runs with the configuration cache enabled, so a task that is not compatible with it fails the test.
 */
class TestProject private constructor(val dir: File) {
  /** Resolves a path relative to the project directory. */
  fun file(path: String) = File(dir, path)

  /** Writes [text] to the file at [path], relative to the project directory, replacing any existing content. */
  fun write(path: String, text: String) {
    file(path).apply { parentFile.mkdirs() }.writeText(text)
  }

  /** Runs Gradle with the given [arguments], expecting the build to succeed. */
  fun run(vararg arguments: String): BuildResult = runner(arguments).build()

  /** Runs Gradle with the given [arguments], expecting the build to fail. */
  fun runAndFail(vararg arguments: String): BuildResult = runner(arguments).buildAndFail()

  /**
   * Starts Gradle with the given [arguments] on another thread, for tasks that never finish by themselves.
   *
   * The only way such a build ends is [BackgroundBuild.stop], which stops mdbook and so makes the build fail.
   */
  fun runInBackground(vararg arguments: String): BackgroundBuild {
    val runner = runner(arguments)
    return BackgroundBuild(dir, CompletableFuture.supplyAsync { runner.buildAndFail() })
  }

  private fun runner(arguments: Array<out String>) =
      GradleRunner.create()
          .withProjectDir(dir)
          .withPluginClasspath()
          .withArguments(listOf(*arguments, "--configuration-cache", "--stacktrace"))

  /** A Gradle build running on another thread, which is waiting on a long running mdbook process. */
  class BackgroundBuild(private val projectDir: File, private val result: CompletableFuture<BuildResult>) {
    /**
     * Waits until [condition] is true, polling it.
     *
     * [condition] is checked every [intervalMillis]. Fails the test straight away, with the output of the build, if the
     * build finishes first (e.g. because mdbook could not be started), and also if [description] has not happened after
     * [timeoutSeconds].
     */
    fun await(
        description: String,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        intervalMillis: Long = POLL_MILLIS,
        condition: () -> Boolean,
    ) {
      val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
      while (!condition()) {
        if (result.isDone) fail("The build finished before $description:\n${result.get().output}")
        if (System.nanoTime() > deadline) fail("Timed out after ${timeoutSeconds}s waiting for $description")
        Thread.sleep(intervalMillis)
      }
    }

    /** Stops the mdbook process started by the build, and returns the result of the build once it has failed. */
    fun stop(): BuildResult {
      mdbookProcesses().forEach { it.destroy() }
      return try {
        result.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      } finally {
        mdbookProcesses().forEach { it.destroyForcibly() }
      }
    }

    /** The mdbook processes working on this project. The (unique) name of its temp directory is in their arguments. */
    private fun mdbookProcesses() =
        ProcessHandle.allProcesses().filter { process ->
          val command = process.info().commandLine().orElse("")
          command.contains("mdbook") && command.contains(projectDir.name)
        }
  }

  companion object {
    private const val DEFAULT_TIMEOUT_SECONDS = 60L
    private const val POLL_MILLIS = 100L

    /** Copies the fixture build in `src/test/resources/functionalTest/[fixture]` into [dir]. */
    fun fromFixture(fixture: String, dir: File): TestProject {
      val resource = "functionalTest/$fixture"
      val url =
          requireNotNull(TestProject::class.java.classLoader.getResource(resource)) {
            "Could not find the test fixture: $resource"
          }
      File(url.toURI()).copyRecursively(dir, overwrite = true)
      return TestProject(dir)
    }
  }
}
