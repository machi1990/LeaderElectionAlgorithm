package main.scala

import akka.actor.{ Actor, ActorRef }


case class NodeValue(id: Int, beatTime: Long, isLeader: Boolean, node:ActorRef)

class Checker(parent: Node,display: ActorRef, election: ActorRef, var nodeList: List[NodeValue]) extends Actor {
  def receive() = {
    case BeatValue(id) => { 
      val now: Long = System.currentTimeMillis();  
      
      val oldNodesLength:Int = nodeList.length 
      val newlist: List[NodeValue] = nodeList.map { (node: NodeValue) =>
        {
          if (node.id == id) {
            NodeValue(id, now, node.isLeader,node.node)
          } else {
            node
          }
        }
      }.filter {
        (node: NodeValue) =>
          {
            now - node.beatTime < 2*Math.pow(TickValue.value, 3) || node.node.equals(parent.self)
          }
      };

      nodeList = newlist
      parent.setNodes(nodeList)
      
      
      if (newlist.exists{ (node: NodeValue) => node.isLeader }) {
          if (oldNodesLength != nodeList.length) { // There has been changes to the list 
            display ! DisplayValue(newlist) 
          }
        } else {
          val index:Int  = newlist.indexWhere { nodeValue => nodeValue.node.equals(parent.self) }
          parent.setStatus(Status.PASSIVE)
          election ! InitiateElection(index)   
      }
    }

    case NewLeader(id) => {
      val now: Long = System.currentTimeMillis();
      var newList:List[NodeValue] = Nil
      
      for (index <- (0 to nodeList.length - 1)) {
        val value:NodeValue = nodeList(index)
        if (index == id) {
          newList = newList :+ NodeValue(value.id, now, true,value.node)
        } else if (parent.self.equals(value.node)) {
          newList = newList :+ NodeValue(value.id, now, false,value.node)
        }else {
          newList = newList :+ NodeValue(value.id, value.beatTime, false,value.node)
        }
      }
      
      nodeList = newList
      parent.setCandPred(-1)
      parent.setCandSucc(-1)
      parent.setNodes(nodeList)
      display ! DisplayValue(nodeList)
     }

    case InitiateElection(i) => {
      if (!nodeList.exists { value => value.isLeader }) {
        parent.setStatus(Status.PASSIVE)
        val index:Int  = nodeList.indexWhere { nodeValue => nodeValue.id == i }
        election ! InitiateElection(index)  
      } else {
         display ! DisplayValue(nodeList) 
      }
    }
    
    case NewNode(i,node) => { 
      if (!nodeList.exists { value => value.node.equals(node) }) {
        nodeList = nodeList :+ NodeValue(i,System.currentTimeMillis(),false,node);
        
        display ! DisplayValue(nodeList)
        parent.setNodes(nodeList)
        
        node ! Actors(nodeList)
      }
    }
  }

}  