package com.archimond7450.archiemate.extensions

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object BehaviorsExtensions {
  extension(behaviors: Behaviors.type) {
    def receiveAndLogMessage[Command](receiveMessageCallback: PartialFunction[Command, Behavior[Command]])(using ctx: ActorContext[Command]): Behavior[Command] = Behaviors.receiveMessage { cmd =>
      ctx.log.debug("Received command {}", cmd)
      receiveMessageCallback(cmd)
    }
  }
}
