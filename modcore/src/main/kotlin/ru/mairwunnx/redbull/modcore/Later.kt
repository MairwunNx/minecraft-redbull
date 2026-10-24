package ru.mairwunnx.redbull.modcore

/**
 * Runs tasks a number of server ticks later, at the end of a server tick, on the server thread; 0 and 1 are the end of
 * this one. A stopped server forgets them, as Paper's scheduler does.
 */
object Later {
  private val tasks = mutableListOf<Pair<Long, () -> Unit>>()
  private var ticks = 0L

  fun run(delay: Long, task: () -> Unit) {
    tasks += ticks + delay to task
  }

  /** Every server tick: runs what came due. */
  fun tick() {
    ticks++
    if (tasks.isEmpty()) return
    val due = tasks.filter { it.first <= ticks }
    tasks.removeAll(due)
    due.forEach { it.second() }
  }

  fun clear() = tasks.clear()
}
