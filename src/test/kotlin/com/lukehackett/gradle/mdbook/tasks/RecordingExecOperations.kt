package com.lukehackett.gradle.mdbook.tasks

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

/**
 * A mocked [ExecOperations] that records the process each task asks to start, without starting anything.
 *
 * @param exitValue The exit value reported for every process.
 * @param startFailure When set, starting a process fails with this exception (e.g. the binary does not exist).
 */
class RecordingExecOperations(
    exitValue: Int = 0,
    startFailure: GradleException? = null,
) {
  /** The mock to hand to the task under test. */
  val operations: ExecOperations = mockk()

  /** The executable of each process started, in order. */
  val executables = mutableListOf<String>()

  /** The arguments of each process started, in order. */
  val arguments = mutableListOf<List<String>>()

  /** Whether the exit value was configured to be ignored for each process started, in order. */
  val ignoredExitValues = mutableListOf<Boolean>()

  init {
    every { operations.exec(any<Action<in ExecSpec>>()) } answers
        {
          if (startFailure != null) throw startFailure

          val executable = slot<String>()
          val args = slot<Iterable<*>>()
          val ignoreExitValue = slot<Boolean>()
          val spec = mockk<ExecSpec>(relaxed = true)
          every { spec.setExecutable(capture(executable)) } just Runs
          every { spec.args(capture(args)) } returns spec
          every { spec.setIgnoreExitValue(capture(ignoreExitValue)) } returns spec

          firstArg<Action<ExecSpec>>().execute(spec)

          executables.add(executable.captured)
          arguments.add(args.captured.map { it.toString() })
          ignoredExitValues.add(ignoreExitValue.captured)

          mockk<ExecResult> { every { this@mockk.exitValue } returns exitValue }
        }
  }
}
