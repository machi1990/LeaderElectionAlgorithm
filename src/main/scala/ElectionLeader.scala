package main.scala

import akka.actor.{Props, ActorSystem}
import akka.actor.actorRef2Scala
import akka.actor.AddressFromURIString
import akka.actor.Deploy
import akka.remote.RemoteScope
import akka.actor.Address
import akka.actor.ActorRef
import akka.actor.ActorSelection
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import akka.pattern.AskableActorSelection
import akka.actor.ActorIdentity
import scala.concurrent.impl.Future
import akka.actor.Identify
import akka.actor.Identify
import scala.concurrent.impl.Future

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import java.io.File

import com.typesafe.config.ConfigFactory

object Configuration {
   val addresses: List[Address] = List (
         Address("akka.tcp","LeaderElection","127.0.0.1",9999),
         Address("akka.tcp","LeaderElection","127.0.0.1",10000),
         Address("akka.tcp","LeaderElection","127.0.0.1",10001),
         Address("akka.tcp","LeaderElection","127.0.0.1",10002)
      )    
}

object ElectionLeader extends App {
  if (args.isEmpty || args(0).toInt < 1 || args(0).toInt > 5) {
    println("You must supply node\'s id between 1 and 5")
    System.exit(0)
  }
  
  val id:Int  = args(0).toInt
  
  val address:Address = Configuration.addresses(id-1);
  
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=\""+address.host.get+"\",akka.remote.netty.tcp.port=\""+address.port.get+"\"")
    .withFallback(ConfigFactory.load());
  
  val system = ActorSystem("LeaderElection",config)
 
  implicit val timeout = Timeout(2 seconds)
  
  var list: List[ActorRef] =Nil
  
    for (i <- (0 to Configuration.addresses.length - 1)) {
        if (i != (id - 1)) {
          try {
            val future = system.actorSelection(Configuration.addresses(i) + "/user/Node").resolveOne()(timeout) // enabled by the “ask” import
            val result = Await.result(future, timeout.duration).asInstanceOf[ActorRef]
            list = list :+ result
          } catch {
            case t: Throwable => {
              // catch and continue
            }
          } 
        }
      } 
  
  val node = system.actorOf(Props( new Node(id)),"Node")
  node ! Start(list)
}