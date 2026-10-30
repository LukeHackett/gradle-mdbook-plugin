package com.lukehackett.gradle.mdbook.tasks

import com.lukehackett.gradle.mdbook.TestProject
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertContains
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Runs `mdbookServe` against the real mdbook binary, which must be on the PATH.
 *
 * mdbook serves on port 3000 and the task cannot change that, so the test is skipped when something else is using it.
 * The task never finishes by itself, so the test stops mdbook once it has seen what it needs to, which fails the build.
 */
@Tag("integration")
class MdbookServeIntegrationTest {
  @TempDir lateinit var tempDir: File

  private val project by lazy { TestProject.fromFixture("book", tempDir) }

  @Test
  fun `serves the book over HTTP, and renders it again when a source file changes`() {
    // Given
    assumeTrue(isPortFree(MDBOOK_PORT), "Port $MDBOOK_PORT is in use, and mdbook serve always uses it")
    val build = project.runInBackground("mdbookServe")

    try {
      // When the book is being served
      build.await("the book to be served") { get("/")?.contains("Fixture Book") == true }

      // And a chapter is changed
      project.write("docs/src/introduction.md", "# Introduction\n\nThis text was changed while serving.\n")

      // Then the changed chapter is served
      build.await("the changed chapter to be served") {
        get("/introduction.html")?.contains("This text was changed while serving.") == true
      }
    } finally {
      // Then
      assertContains(build.stop().output, "'mdbook serve' failed")
    }
  }

  private fun isPortFree(port: Int) =
      try {
        ServerSocket(port).use { true }
      } catch (_: IOException) {
        false
      }

  /**
   * The body of the page at [path], or null when it cannot be fetched (yet).
   *
   * mdbook serves on `localhost`, which can be the IPv4 or the IPv6 loopback address, depending on the machine. Java
   * only tries the one it resolves `localhost` to, so both are tried.
   */
  private fun get(path: String): String? = LOOPBACK_HOSTS.firstNotNullOfOrNull { host ->
    try {
      val request = HttpRequest.newBuilder(URI("http://$host:$MDBOOK_PORT$path")).GET().build()
      val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
      response.body().takeIf { response.statusCode() == HTTP_OK }
    } catch (_: IOException) {
      null
    }
  }

  private companion object {
    val LOOPBACK_HOSTS = listOf("127.0.0.1", "[::1]")
    const val MDBOOK_PORT = 3000
    const val HTTP_OK = 200
  }
}
