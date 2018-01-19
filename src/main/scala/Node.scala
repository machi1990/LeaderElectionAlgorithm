package main.scala

import akka.actor.{Props,Actor,ActorRef}
import akka.actor.Address
import java.util.concurrent.TimeUnit
import akka.actor.AddressFromURIString
import akka.actor.ActorIdentity
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.actor.Identify
import akka.actor.Identify

case class BeatValue(id:Int)
case class NewNode(id:Int,node:ActorRef)
case class Start(nodes: List[ActorRef])
case class Actors(nodes: List[NodeValue])
/**
 * 
 * TODO
 */

class Register(id:Int,target:ActorRef) extends Actor {
  var actors: List[ActorRef] = Nil
  val identifyId = id + ""
  var counter:Int = 0
  
  for (i <- (0 to Configuration.addresses.length - 1)) {
        if (i != (id - 1)) {
          context.actorSelection(Configuration.addresses(i) + "/user/Node") ! Identify(identifyId)
        }
      }  
  
  def receive() = {
    case ActorIdentity(`identifyId`, Some(ref)) => {
      register(ref);
    }
    
    case ActorIdentity(`identifyId`, None) => {
      register();
    }
  }
  
  def register(ref:ActorRef): Unit = {
    actors = actors :+ ref
    register()
  }
  
  def register(): Unit = {
    counter +=1;
   
    println(counter)
    if (counter == Configuration.addresses.length - 1) {
      target ! Start(actors)
    }
  }
}

class Node(id:Int) extends Actor { 
  var status:Status = Status.PASSIVE
  var nodesList: List[NodeValue] = Nil
  var checker: ActorRef = null
  var cand_prev:Int = -1;
  var cand_succ:Int = -1;
  
  val election:ActorRef = context.actorOf(Props(new Election(this)),"Election")
  val display:ActorRef = context.actorOf(Props[Display],"Display")
  val beat:ActorRef = context.actorOf(Props(new Beat(self)),"Beat")
  
  def receive()  = { 
    case Start(list) => {   
        if (list.isEmpty) {
          
          nodesList = List(
            NodeValue(id,System.currentTimeMillis(),true,self)
          )
          
          //beat ! Tick // Activate beat here
          checker = context.actorOf(Props( new Checker(this,display,election,nodesList)), "Checker")
          checker ! InitiateElection(id)
        } else { 
          notifyNew(list)
        }
    }
    case Tick => {
      this.nodesList.foreach { (nodeValue:NodeValue) => nodeValue.node ! BeatValue(id) }
    }
    
    case BeatValue(i) => {
      checker ! BeatValue(i)
    }
    
    case NewLeader(id) => {
      checker ! NewLeader(id)
    }
    
    case NewNode(id,node) => {
      checker ! NewNode(id,node)
    }
    
    case Actors(nodes) => {
      if (nodesList.length != nodes.length) {
      setNodes(nodes)
      checker = context.actorOf(Props( new Checker(this,display,election,nodesList)), "Checker")
      checker ! InitiateElection(id)
      }
    }
    
    case AvsRsp(i) => {
      election ! AvsRsp(i)
    }
    
    case Algo(i) => {
      election ! Algo(i)
    }
    
    case Avs(i) => {
      election ! Avs(i)
    }
  }
  
  def sendNewLeaderNotification(id:Int): Unit = {
      this.nodesList.foreach { (nodeValue:NodeValue) => nodeValue.node ! NewLeader(id) }
  }
  
  def setStatus(status:Status): Unit = {
    this.status = status
  }
  
  def isCandidate():Boolean = {
    this.status == Status.CANDIDATE  
  }
  
  def isPassive():Boolean = {
    this.status == Status.PASSIVE
  }
  
  def isWaiting():Boolean = {
    this.status == Status.WAITING
  }
  
  def getStatus():Status = {
    this.status
  }
  
  def hasCandSucc():Boolean = {
    this.cand_succ != -1  
  }
  
  def setCandPred(cand_pred:Int):Unit = {
    this.cand_prev = cand_pred;
  }
  
  def hasCandPred():Boolean = {
    this.cand_prev != -1  
  }
  def getCandPred():Int = {
    cand_prev
  }
  
  def setCandSucc(cand_succ:Int):Unit = {
    this.cand_succ = cand_succ
  }
  
  def setNodes(nodes:List[NodeValue]): Unit = {
    this.nodesList = nodes;
  }
  
  def getCandSucc():Int = {
    cand_succ
  }
  
  def notifyNew(actors:List[ActorRef]):Unit = {
    actors.foreach { actor => actor ! NewNode(id,self) }
  }
}