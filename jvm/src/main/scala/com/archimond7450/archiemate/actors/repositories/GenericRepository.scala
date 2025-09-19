package com.archimond7450.archiemate.actors.repositories

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import org.apache.pekko.persistence.typed.state.RecoveryCompleted

abstract class GenericRepository[Command, Event, State](using ctx: ActorContext[Command]) {
  protected val actorName: String
  protected val emptyState: State
  protected val commandHandler: (State, Command) => Effect[Event, State]
  protected val eventHandler: (State, Event) => State
  protected val onRecoveryCompleted: State => Unit = state => {}

  val eventSourcedBehavior: () => EventSourcedBehavior[Command, Event, State] = () => EventSourcedBehavior[Command, Event, State](
    persistenceId = PersistenceId.ofUniqueId(actorName),
    emptyState = emptyState,
    commandHandler = commandHandler,
    eventHandler = eventHandler
  ).receiveSignal {
    case (state, RecoveryCompleted) =>
      ctx.log.debug("Recovery completed for {}: {}", actorName, state)
      onRecoveryCompleted(state)
  }
}
