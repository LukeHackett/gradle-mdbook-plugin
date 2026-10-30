import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.shadow.jar)
  alias(libs.plugins.publish)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kover)
  alias(libs.plugins.ktfmt)
  `java-gradle-plugin`
  `kotlin-dsl`
  `maven-publish`
}

// Java version in use across the project (must be a major version like "25", not "25.0.1")
val javaVersion = file(".java-version").readText().trim()
val projectName = project.name
val projectDescription = project.description.orEmpty()
val repositoryUrl = providers.gradleProperty("repositoryUrl").get()

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt())) }
  withSourcesJar()
  withJavadocJar()
}

repositories { mavenCentral() }

dependencies {
  // Test Dependencies
  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
  testImplementation(libs.mockk)
  testImplementation(libs.archunit.junit6)
  testImplementation(libs.bundles.junit)
  testRuntimeOnly(libs.junit.platform.launcher)
}

detekt { config.setFrom(file("config/detekt/detekt.yml")) }

ktfmt { maxWidth.set(120) }

kover {
  // Coverage comes from the unit tests alone, so that verifying it does not need the mdbook binary
  currentProject { instrumentation { disabledForTestTasks.addAll("integrationTest", "architectureTest") } }
  reports {
    total {
      log { onCheck = true }
      html { onCheck = true }
      xml { onCheck = true }
    }
    verify { rule { bound { minValue = 85 } } }
  }
}

gradlePlugin {
  plugins {
    create("mdbook") {
      id = "$group"
      implementationClass = "$group.GradleMdbookPlugin"
      displayName = projectName
      description = projectDescription
      tags.set(listOf("mdbook", "documentation", "docs", "markdown", "book"))
    }
  }
  website.set(repositoryUrl)
  vcsUrl.set("$repositoryUrl.git")
}

publishing {
  publications {
    withType<MavenPublication> {
      pom {
        name.set(projectName)
        description.set(projectDescription)
        url.set(repositoryUrl)
        licenses {
          license {
            name.set("MIT License")
            url.set("${pom.url}/blob/main/LICENSE")
          }
        }
        developers {
          developer {
            id.set("LukeHackett")
            name.set("Luke Hackett")
          }
        }
      }
    }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(javaVersion))
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("")
  mergeServiceFiles()
}

tasks.named<Jar>("jar") { enabled = false }

// The test source set is split across three tasks. ArchUnit runs on its own JUnit engine, which does not honour @Tag,
// so the architecture rules are selected by engine and the integration tests by tag.
val archUnitEngine = "archunit"
val integrationTag = "integration"

tasks.withType<Test>().configureEach {
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging {
    events("passed", "skipped", "failed")
    showStandardStreams = true
    showExceptions = true
    showStackTraces = true
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

// Unit tests only: they need neither the mdbook binary, nor the architecture rules
tasks.named<Test>("test") {
  useJUnitPlatform {
    excludeEngines(archUnitEngine)
    excludeTags(integrationTag)
  }
}

// Architecture rules (ArchUnit) only inspect the compiled classes and need nothing else, so they are part of `check`
tasks.register<Test>("architectureTest") {
  description = "Runs the ArchUnit architecture rules."
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().runtimeClasspath
  useJUnitPlatform { includeEngines(archUnitEngine) }
  shouldRunAfter(tasks.named("test"))
}

// Integration tests run the real mdbook binary, so they are NOT part of `check`: mdbook must be installed to run them
tasks.register<Test>("integrationTest") {
  description = "Runs the integration tests against a real mdbook binary (which must be on the PATH)."
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().runtimeClasspath
  useJUnitPlatform {
    excludeEngines(archUnitEngine)
    includeTags(integrationTag)
  }
  dependsOn(tasks.named("mdbookDownload"))
  shouldRunAfter(tasks.named("test"), tasks.named("architectureTest"))
}

tasks.named("check") { dependsOn(tasks.named("architectureTest")) }

tasks.register<MdBookDownloadTask>("mdbookDownload") {
  group = "build setup"
  description = "Downloads the mdBook binary for the current platform"

  version.set("v0.5.4")
  binDirectory.set(layout.buildDirectory.dir("bin"))
}
