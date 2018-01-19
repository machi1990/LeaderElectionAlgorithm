package main.scala

import akka.actor.{Actor,ActorRef}
import akka.util.Timeout
import scala.concurrent.duration.`package`.DurationLong

object TickValue {
  val value:Long = 10
}

case object Tick;
  
class Beat(parent:ActorRef) extends Actor {
  implicit val timeout = Timeout(DurationLong(TickValue.value).millisecond)
  val scheduler = context.system.scheduler
  val leaderId:Int = -1
  
  self ! Tick; // Activate beat
  
  def receive = {
    case Tick => {
      scheduler.scheduleOnce(timeout.duration, self, Tick)(context.dispatcher,self)
      parent ! Tick
    }
  }
}