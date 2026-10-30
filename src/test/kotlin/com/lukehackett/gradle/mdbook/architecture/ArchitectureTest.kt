package com.lukehackett.gradle.mdbook.architecture

import com.lukehackett.gradle.mdbook.tasks.AbstractMdbookTask
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaCall
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.properties.HasName
import com.tngtech.archunit.core.domain.properties.HasOwner
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.process.ExecOperations

/**
 * The architecture rules of the plugin, checked against the compiled production classes.
 *
 * These only inspect bytecode, so they need nothing installed. They run on the ArchUnit JUnit engine, which the build
 * routes to the `architectureTest` task (part of `check`) and keeps out of the unit `test` task.
 */
@AnalyzeClasses(packages = [ROOT], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

  // region Layering

  /**
   * Nothing in the plugin pulls in a third party library: the extension and the tasks may only use the plugin itself,
   * the JDK, Kotlin, and the Gradle API.
   */
  @ArchTest
  @JvmField
  val extensionAndTasksOnlyDependOnThePlatform: ArchRule =
      classes()
          .that()
          .resideInAnyPackage(EXTENSION, TASKS)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "$ROOT..",
              "java..",
              "javax.inject..",
              "kotlin..",
              "org.gradle..",
              "org.jetbrains.annotations..",
          )

  /**
   * The plugin is the only class that knows about both the extension and the tasks and wires them together. The
   * extension and the tasks never depend on each other, so tasks can be configured without the extension.
   */
  @ArchTest
  @JvmField
  val onlyThePluginMayUseTheExtensionAndTheTasks: ArchRule =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Plugin")
          .definedBy(ROOT)
          .layer("Extension")
          .definedBy(EXTENSION)
          .layer("Tasks")
          .definedBy(TASKS)
          .whereLayer("Plugin")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Extension")
          .mayOnlyBeAccessedByLayers("Plugin")
          .whereLayer("Tasks")
          .mayOnlyBeAccessedByLayers("Plugin")

  // endregion

  // region Configuration cache safety

  /** A Project cannot be serialized into the configuration cache, so nothing may keep a reference to one. */
  @ArchTest @JvmField val noClassHoldsAProjectField: ArchRule = noFields().should().haveRawType(Project::class.java)

  /** Tasks and the extension are configured through lazy properties, never by reaching back to the Project. */
  @ArchTest
  @JvmField
  val extensionAndTasksNeverAccessTheProject: ArchRule =
      noClasses()
          .that()
          .resideInAnyPackage(EXTENSION, TASKS)
          .should()
          .callMethodWhere(callTo(Task::class.java, "getProject"))

  // endregion

  // region Process execution

  /** Processes are only started by the tasks, through the injected [ExecOperations]. */
  @ArchTest
  @JvmField
  val onlyMdbookTasksUseExecOperations: ArchRule =
      noClasses()
          .that()
          .areNotAssignableTo(AbstractMdbookTask::class.java)
          .should()
          .dependOnClassesThat()
          .areAssignableTo(ExecOperations::class.java)

  /** Starting a process by any other route bypasses the failure handling of [AbstractMdbookTask]. */
  @ArchTest
  @JvmField
  val noClassStartsProcessesDirectly: ArchRule =
      noClasses()
          .should()
          .dependOnClassesThat()
          .areAssignableTo(ProcessBuilder::class.java)
          .orShould()
          .callMethodWhere(callTo(Runtime::class.java, "exec"))
          .orShould()
          .callMethodWhere(callTo(Project::class.java, "exec"))
          .orShould()
          .callMethodWhere(callTo(Project::class.java, "javaexec"))

  // endregion

  // region Naming and placement

  /** Concrete tasks are named after the mdbook command they run, e.g. MdbookBuildTask. */
  @ArchTest
  @JvmField
  val tasksAreNamedMdbookSomethingTask: ArchRule =
      classes()
          .that()
          .areAssignableTo(AbstractMdbookTask::class.java)
          .and()
          .doNotHaveFullyQualifiedName(AbstractMdbookTask::class.java.name)
          .should()
          .haveSimpleNameStartingWith("Mdbook")
          .andShould()
          .haveSimpleNameEndingWith("Task")

  /** Every task lives in the tasks package. */
  @ArchTest
  @JvmField
  val tasksResideInTheTasksPackage: ArchRule =
      classes().that().areAssignableTo(Task::class.java).should().resideInAPackage(TASKS)

  /** Every task shares the behaviour of [AbstractMdbookTask]. */
  @ArchTest
  @JvmField
  val tasksExtendAbstractMdbookTask: ArchRule =
      classes().that().areAssignableTo(Task::class.java).should().beAssignableTo(AbstractMdbookTask::class.java)

  // endregion

  private companion object {
    /** Matches calls to [methodName] on [owner], or any subclass of [owner]. */
    fun callTo(owner: Class<*>, methodName: String): DescribedPredicate<JavaCall<*>> =
        JavaCall.Predicates.target(HasName.Predicates.name(methodName))
            .and(JavaCall.Predicates.target(HasOwner.Predicates.With.owner(JavaClass.Predicates.assignableTo(owner))))
  }
}

private const val ROOT = "com.lukehackett.gradle.mdbook"
private const val EXTENSION = "$ROOT.extension.."
private const val TASKS = "$ROOT.tasks.."
