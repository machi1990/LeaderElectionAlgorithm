package main.scala

import akka.actor.Actor


case class DisplayValue(list:List[NodeValue])

class Display extends Actor{
  def receive() = {
    case DisplayValue(list) => {
      val size = list.length;
      
      print("Current Nodes : [")
      var leaderId : Int = -1
      
      for (i <- (0 to size - 2)) {
        print(list(i).id+" ,")
        
        if (list(i).isLeader) {
          leaderId = list(i).id;
        }
      }
      
      if (!list.isEmpty) {
         print(list(size-1).id+" ")
         if (leaderId == -1) {
         leaderId = list(size-1).id   
        }
      }
      
      println("]    Leader id: "+ leaderId)
    }
  }
}