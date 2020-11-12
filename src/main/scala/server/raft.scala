package server

import akka.actor.{ActorSystem, Props, ActorRef, Actor, ActorPath}
import com.typesafe.config.ConfigFactory
import akka.routing.{Broadcast, BroadcastPool}

case class ServerPhase(phaseNum : Int, leader: ActorRef)
case class Heartbeat(serverPhase: ServerPhase)
case class HeartbeatReply(reply: Boolean) //yes or no that you are leader

class RaftServer extends Actor {

	var numberOfServers : Int = ConfigFactory.load.getInt("number-servers")

	//Phase Stats
	var currLPhase : Int = 0
	var currLRef : ActorRef = self

	//Leader Stats
	var isLeader : Boolean = false

	//Election Stats
	var votes : Int = 0
	var isLeaderElect : Boolean = false

	//Other
	val generator = new scala.util.Random
  var serverBlocked = false

  //Additional Actors
  val raftTimer = context.actorOf(Props[RaftTimer], "RaftTimer")
  val raftTester= context.actorOf(Props[RaftTest], "RaftTest")
  val raftHeartbeatTimer= context.actorOf(Props[HeartbeatTimer], "HeartbeatTimer")

  /*
  Handles all incoming messages 
  */
  def receive() = {
    case TIMEOUT =>
      if (!serverBlocked) {
        if (isLeaderElect && !isLeader) {
          //leaderElect TIMEOUT, no server was agreed upon
          println(self.path.name + ": had a leaderElect timeout. Was not made a leader.")
          isLeaderElect = false
          raftTimer ! START
        } else if (!isLeader) {
          //follower TIMEOUT, Leader must have failed so hold new election
          println(self.path.name + ": had a timeout. Holding election. Sending heartbeat for election.")
          isLeaderElect = true
          currLPhase = currLPhase + 1
          currLRef = self
          votes = 1
          sendAll(Heartbeat(ServerPhase(currLPhase, currLRef)))
          raftTimer ! START
        }
      }
    case HeartbeatReply(reply) =>
      if (!serverBlocked) {
        if (reply && isLeaderElect && !isLeader) {
         //I am a leaderElect and got an upvote
         votes = votes + 1
         println(self.path.name + ": got voted for. Total votes are now " + votes + ".")

         //Check if won
         if (votes >= math.ceil(numberOfServers.toDouble / 2).toInt) {
          //won
          println(self.path.name + ": (Leader) I am the leader for phase" + currLPhase + "!")
            isLeader = true
          isLeaderElect = false
          raftHeartbeatTimer ! START
        }
      } else if (reply && isLeader) {
        //UNCOMMENT IF WOULD LIKE TO SEE WHICH SERVERS GIVE LEADER A HEARBEATREPLY
        //println(self.path.name + ": (Leader) got postive heartbeat reply from " + sender.path.name)
      }
    }
  case Heartbeat(ServerPhase(hPhaseNum, hleader)) =>
    if (!serverBlocked) {
      if (hPhaseNum == currLPhase && hleader == currLRef && isLeaderElect == false && isLeader == false) { 
       //I am on the same phase, got a hearbeat from the same leader, am not in an election or the leader
       raftTimer ! CANCEL
       sender ! HeartbeatReply(true)
       raftTimer ! START
     } else if (hPhaseNum == currLPhase && hleader != currLRef) { 
       //I am on the same phase and got a heartbeat from a different leader then the one I know
       sender ! HeartbeatReply(false)
       raftTimer ! START
     } else if (hPhaseNum > currLPhase) {
       //I am a follower, leaderElect, or leader getting a Heartbeat on a higher phase
       raftTimer ! CANCEL
       currLPhase = hPhaseNum
       currLRef = hleader
       isLeader = false
       isLeaderElect = false
       println(self.path.name + ": adjusted to phase" + currLPhase + ".")
       sender ! HeartbeatReply(true)
       raftTimer ! START
     }
   }
 case SERVERBLOCK =>
  if (serverBlocked) {
    println(self.path.name + ": Starting back up. Last saw phase" + currLPhase + ".")
    serverBlocked = false
    raftTimer ! START
  } else {
    println(self.path.name + ": Server down. Stopped on phase" + currLPhase + ".")
    serverBlocked = true
    raftTimer ! CANCEL
  }
case LEADERBEAT =>
  if (!serverBlocked && isLeader) {
    println(self.path.name + ": (Leader) is sending heartbeats for phase" + currLPhase + ".")
    sendAll(Heartbeat(ServerPhase(currLPhase, currLRef)))
  } else {
    isLeader = false
    raftHeartbeatTimer ! CANCEL
  }
}

  /*
  Sends the message to all actors in defined paths
  */
  def sendAll(something : Any) = {
  	val listOfActors = List(context.actorSelection("/user/ActorServer1"), context.actorSelection("/user/ActorServer2"), context.actorSelection("/user/ActorServer3"), context.actorSelection("/user/ActorServer4"), context.actorSelection("/user/ActorServer5"))
  	for (i <- listOfActors) {
  		if (i != context.actorSelection(self.path)) {
  			i ! something
  		}
  	}
  }

  /*
  Starts the election process
  */
  raftTimer ! START
  raftTester ! START //to randomly make servers fail

}

object Raft extends App {

	val system = ActorSystem("RaftSystem")

	val raftServer1 = system.actorOf(Props[RaftServer], "ActorServer1")
	val raftServer2 = system.actorOf(Props[RaftServer], "ActorServer2")
	val raftServer3 = system.actorOf(Props[RaftServer], "ActorServer3")
  val raftServer4 = system.actorOf(Props[RaftServer], "ActorServer4")
  val raftServer5 = system.actorOf(Props[RaftServer], "ActorServer5")

}

