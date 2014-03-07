package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

/**
  * Generates task infos, given resource offers.
  * Keeps track of tasks, determines when the framework is done generating tasks.
  */
abstract class TaskGenerator(requestedTasks: Int) {

  /**
   * Callback from the scheduler when it starts.
   */
  def start() {
  }

  /**
    * Whether any more tasks will be created.
    * This is supposed to be queried by the scheduler to determine when to decline all offers.
    */
  def doneCreatingTasks(): Boolean

  private var _terminatedTasks = 0;

  /**
    * Whether all existing tasks are done executing or to be finished immediately
    * and no more tasks are to be scheduled.
    * This is supposed to be queried by the scheduler to determine
    * when to stop the scheduler and thus terminate the whole framework.
    */
  def done(): Boolean = {
    return _terminatedTasks >= requestedTasks && doneCreatingTasks()
  }

  /**
    */
  def generateTaskInfos(offer: Offer): java.util.Collection[TaskInfo]

  def observeTaskStatusUpdate(taskStatus: TaskStatus) = {
    taskStatus.getState() match {
      case TaskState.TASK_FINISHED |
        TaskState.TASK_FAILED |
        TaskState.TASK_KILLED |
        TaskState.TASK_LOST => {
        _terminatedTasks += 1;
      }
      case TaskState.TASK_STAGING |
        TaskState.TASK_STARTING |
        TaskState.TASK_RUNNING =>
    }
  }

  private val _executorUri = new java.io.File("./mesosaurus-executor").getCanonicalPath()

  // In this framework, all tasks can use the same kind of executor, no matter what.
  // So, we only need to define the following in one place (e.g. right here),
  // where it is accessible to any task info generator in use.
  protected[this] val _executorInfo = ExecutorInfo.newBuilder()
    .setExecutorId(ExecutorID.newBuilder().setValue("default"))
    .setCommand(CommandInfo.newBuilder().setValue(_executorUri))
    .setName("Mesosaurus Executor (Scala)")
    .setSource("mesosaurus_scala")
    .build();
}
