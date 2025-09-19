package com.archimond7450.archiemate

import com.archimond7450.archiemate.actors.UserGuardian
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

private object ArchieMateBackend {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(UserGuardian(), UserGuardian.actorName)
  }
}
