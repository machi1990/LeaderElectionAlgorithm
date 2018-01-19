package main.scala

import akka.actor.{Actor, ActorRef}

abstract class AlgoValue
case class InitiateElection(value:Int) extends AlgoValue
case class Algo(i:Int) extends AlgoValue
case class Avs(i:Int) extends AlgoValue
case class AvsRsp(i:Int) extends AlgoValue
case class NewLeader(id:Int) extends AlgoValue

class Election(parent:Node) extends Actor {
  
  def receive() = {
    case InitiateElection(i) => {
      initiate(i)
    }
    
    case Algo(j) => {
      algo(j)
    }
    
    case Avs(j) => {
      avs(j)
    }
    
    case AvsRsp(j) => {
      avsresp(j) // TODO
    }
  }
  
   def initiate(i:Int):Unit = {
      parent.setStatus(Status.CANDIDATE)
      parent.setCandSucc(-1)
      parent.setCandPred(-1)
      parent.nodesList((i+1) % parent.nodesList.length).node ! Algo(i)
    }
   
   def algo(j:Int):Unit = {
       //println("Got algo from j= "+ j);
       
      if (parent.isPassive()) {
        //println("I am DUMMY")
              
        parent.setStatus(Status.DUMMY)
        
        // println("Send algo to j+2 = " (j+2))
        parent.nodesList((j+2) % parent.nodesList.length).node ! Algo(j)
      } else if (parent.isCandidate()) {
        parent.setCandPred(j);
        val i:Int  = parent.nodesList.indexWhere { nodeValue => nodeValue.node.equals(parent.self) }
        
        if ( i < j) {
          if (!parent.hasCandSucc()) {
            //println("I am WAITING")
            //println("AVS to j "+ j + " of i " + i)
              
             parent.setStatus(Status.WAITING)
             parent.nodesList(j).node ! Avs(i)
          } else {
            //println("AVSRSP to j  "+ j + " of cand pred " + parent.getCandPred())
            //println("I am DUMMY")
            parent.nodesList(parent.getCandSucc()).node ! AvsRsp(parent.getCandPred()) 
            parent.setStatus(Status.DUMMY)
          }
        } else if (i == j) {
          //println("New leader's id i= "+ j)
          
          parent.sendNewLeaderNotification(i);
          parent.setStatus(Status.LEADER)
        } 
      }
   }
   
   def avs(j:Int):Unit = {
      //println("Got avs from j= "+ j);
      
      if (parent.isCandidate()) {
        if (!parent.hasCandPred()) {
          //println("has no cand pred Set cand succ of j "+ j)
          parent.setCandSucc(j);
        } else {
          
          //println("AVSRSP to j  "+ j + " of cand pred " + parent.getCandPred())
          //println("I am DUMMY")
            
          parent.nodesList(j).node ! AvsRsp(parent.getCandPred())
          parent.setStatus(Status.DUMMY)
        }
      } else if (parent.isWaiting()) {
        // println("is waiting Set cand succ of j "+ j)
        parent.setCandSucc(j)
      }
   }
   
   def avsresp(k:Int):Unit = {
     // println("Got avsrsp from k= "+ k);
      
     if (parent.isWaiting()) {
        val i:Int  = parent.nodesList.indexWhere { nodeValue => nodeValue.node.equals(parent.self) }
        
        if (i == k) {
          //println("New leader's id k= "+ i)
          parent.sendNewLeaderNotification(i)
          parent.setStatus(Status.LEADER)
        } else {
          parent.setCandPred(k)
          
          if (!parent.hasCandSucc()) {
            if (k < i) {
             // println("AVS to k "+ k + " of i " + i)
             // println("I am WAITING")
            
              parent.setStatus(Status.WAITING)
              parent.nodesList(k).node ! avs(i)
            }
          } else {
              parent.setStatus(Status.DUMMY)
             // println("I am DUMMY")
            
              //println("AVSRSP to cand success "+ parent.getCandSucc() + " of k " + k)
              parent.nodesList(parent.getCandSucc()).node ! AvsRsp(k)
            }
        }
      }
   }
}