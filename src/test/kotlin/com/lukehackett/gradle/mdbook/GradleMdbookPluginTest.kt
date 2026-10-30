package com.lukehackett.gradle.mdbook

import com.lukehackett.gradle.mdbook.extension.MdbookExtension
import com.lukehackett.gradle.mdbook.tasks.MdbookBuildTask
import com.lukehackett.gradle.mdbook.tasks.MdbookCleanTask
import com.lukehackett.gradle.mdbook.tasks.MdbookInitTask
import com.lukehackett.gradle.mdbook.tasks.MdbookServeTask
import com.lukehackett.gradle.mdbook.tasks.MdbookTestTask
import com.lukehackett.gradle.mdbook.tasks.MdbookWatchTask
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GradleMdbookPluginTest {
  @TempDir lateinit var tempDir: File

  private lateinit var projectDir: File
  private lateinit var project: Project

  @BeforeEach
  fun setUp() {
    // Resolve symlinks (e.g. /var -> /private/var on macOS) so paths compare equal to Gradle's
    projectDir = tempDir.canonicalFile
    project = ProjectBuilder.builder().withProjectDir(projectDir).build()
  }

  @Test
  fun `plugin registers mdbook extension when applied`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val extension = project.extensions.findByName("mdbook")
    assertNotNull(extension, "The 'mdbook' extension should be registered")
    assertIs<MdbookExtension>(extension)
  }

  @Test
  fun `plugin can be applied using its ID`() {
    // When
    project.plugins.apply("com.lukehackett.gradle.mdbook")

    // Then
    assertNotNull(project.extensions.findByName("mdbook"))
  }

  @Test
  fun `extension defaults bookDir to the docs directory`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // When
    val extension = project.extensions.getByType(MdbookExtension::class.java)

    // Then
    assertEquals(File(projectDir, "docs"), extension.bookDir.get().asFile)
  }

  @Test
  fun `extension defaults outputDir to build mdbook`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // When
    val extension = project.extensions.getByType(MdbookExtension::class.java)

    // Then
    assertEquals(File(projectDir, "build/mdbook"), extension.outputDir.get().asFile)
  }

  @Test
  fun `extension defaults executable to mdbook on the PATH`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // When
    val extension = project.extensions.getByType(MdbookExtension::class.java)

    // Then
    assertEquals("mdbook", extension.executable.get())
  }

  @Test
  fun `plugin registers the mdbookInit task in the mdbook group`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val task = project.tasks.findByName("mdbookInit")
    assertIs<MdbookInitTask>(task)
    assertEquals("mdbook", task.group)
    assertEquals("Creates a new book with minimal boilerplate.", task.description)
  }

  @Test
  fun `plugin registers the mdbookBuild task in the mdbook group`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val task = project.tasks.findByName("mdbookBuild")
    assertIs<MdbookBuildTask>(task)
    assertEquals("mdbook", task.group)
    assertEquals("Renders the book.", task.description)
  }

  @Test
  fun `plugin registers the mdbookWatch task in the mdbook group`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val task = project.tasks.findByName("mdbookWatch")
    assertIs<MdbookWatchTask>(task)
    assertEquals("mdbook", task.group)
    assertEquals("Renders the book again every time a source file changes.", task.description)
  }

  @Test
  fun `plugin registers the mdbookServe task in the mdbook group`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val task = project.tasks.findByName("mdbookServe")
    assertIs<MdbookServeTask>(task)
    assertEquals("mdbook", task.group)
    assertEquals("Serves the book over HTTP, rendering it again every time a source file changes.", task.description)
  }

  @Test
  fun `the watch and serve tasks take their output directory from the extension`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val extension = project.extensions.getByType(MdbookExtension::class.java)
    val watch = project.tasks.getByName("mdbookWatch") as MdbookWatchTask
    val serve = project.tasks.getByName("mdbookServe") as MdbookServeTask

    // When
    extension.outputDir.set(project.layout.buildDirectory.dir("site"))

    // Then
    assertEquals(File(projectDir, "build/site"), watch.outputDir.get().asFile)
    assertEquals(File(projectDir, "build/site"), serve.outputDir.get().asFile)
  }

  @Test
  fun `plugin registers the mdbookTest task in the mdbook group`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val task = project.tasks.findByName("mdbookTest")
    assertIs<MdbookTestTask>(task)
    assertEquals("mdbook", task.group)
    assertEquals("Tests the Rust code samples in the book.", task.description)
  }

  @Test
  fun `plugin registers the mdbookClean task in the mdbook group`() {
    // When
    project.plugins.apply(GradleMdbookPlugin::class.java)

    // Then
    val task = project.tasks.findByName("mdbookClean")
    assertIs<MdbookCleanTask>(task)
    assertEquals("mdbook", task.group)
    assertEquals("Deletes the rendered book.", task.description)
  }

  @Test
  fun `the clean task takes its output directory from the extension`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val extension = project.extensions.getByType(MdbookExtension::class.java)
    val task = project.tasks.getByName("mdbookClean") as MdbookCleanTask

    // When
    extension.outputDir.set(project.layout.buildDirectory.dir("site"))

    // Then
    assertEquals(File(projectDir, "build/site"), task.outputDir.get().asFile)
  }

  @Test
  fun `every task is ordered after the init task, without depending on it`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val init = project.tasks.getByName("mdbookInit")

    // Then
    listOf("mdbookBuild", "mdbookClean", "mdbookTest", "mdbookWatch", "mdbookServe").forEach { name ->
      val task = project.tasks.getByName(name)
      assertTrue(init in task.mustRunAfter.getDependencies(task), "$name should run after mdbookInit")
      assertTrue(init !in task.taskDependencies.getDependencies(task), "$name should not depend on mdbookInit")
    }
    assertTrue(init.mustRunAfter.getDependencies(init).isEmpty(), "mdbookInit should not wait for any task")
  }

  @Test
  fun `the build task is ordered after the clean task, without depending on it`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val build = project.tasks.getByName("mdbookBuild")
    val clean = project.tasks.getByName("mdbookClean")

    // Then
    assertTrue(clean in build.mustRunAfter.getDependencies(build))
    assertTrue(clean !in build.taskDependencies.getDependencies(build))
  }

  @Test
  fun `the build task takes its output directory from the extension`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val extension = project.extensions.getByType(MdbookExtension::class.java)
    val task = project.tasks.getByName("mdbookBuild") as MdbookBuildTask

    // When
    extension.outputDir.set(project.layout.buildDirectory.dir("site"))

    // Then
    assertEquals(File(projectDir, "build/site"), task.outputDir.get().asFile)
  }

  @Test
  fun `tasks take their settings from the extension`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val extension = project.extensions.getByType(MdbookExtension::class.java)
    val task = project.tasks.getByName("mdbookInit") as MdbookInitTask

    // When
    extension.bookDir.set(project.layout.projectDirectory.dir("guide"))
    extension.executable.set("/opt/mdbook/bin/mdbook")

    // Then
    assertEquals(File(projectDir, "guide"), task.bookDir.get().asFile)
    assertEquals("/opt/mdbook/bin/mdbook", task.executable.get())
  }

  @Test
  fun `a task can be configured differently from the extension`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val task = project.tasks.getByName("mdbookInit") as MdbookInitTask

    // When
    task.bookDir.set(project.layout.projectDirectory.dir("other"))

    // Then
    assertEquals(File(projectDir, "other"), task.bookDir.get().asFile)
  }

  @Test
  fun `extension values can be overridden`() {
    // Given
    project.plugins.apply(GradleMdbookPlugin::class.java)
    val extension = project.extensions.getByType(MdbookExtension::class.java)

    // When
    extension.bookDir.set(project.layout.projectDirectory.dir("guide"))
    extension.outputDir.set(project.layout.buildDirectory.dir("site"))
    extension.executable.set("/opt/mdbook/bin/mdbook")

    // Then
    assertEquals(File(projectDir, "guide"), extension.bookDir.get().asFile)
    assertEquals(File(projectDir, "build/site"), extension.outputDir.get().asFile)
    assertEquals("/opt/mdbook/bin/mdbook", extension.executable.get())
  }
}
