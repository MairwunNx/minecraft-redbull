package ru.mairwunnx.redbull.neoforge

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.PriorityQueue

/** Runs tasks a number of server ticks later, on the server thread; a restart forgets them, as Paper's scheduler does. */
object Later {
  private class Task(val at: Long, val run: () -> Unit)

  private val tasks = PriorityQueue<Task>(compareBy { it.at })
  private var tick = 0L

  fun run(ticks: Int, task: () -> Unit) {
    tasks += Task(tick + ticks.coerceAtLeast(1), task)
  }

  fun listen(bus: IEventBus) {
    bus.on<ServerTickEvent.Post> {
      tick++
      while (tasks.peek()?.let { it.at <= tick } == true) tasks.poll().run()
    }
    bus.on<ServerStoppedEvent> { tasks.clear() }
  }
}
