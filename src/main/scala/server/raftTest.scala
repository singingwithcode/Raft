package server

import akka.actor.{ActorSystem, Props, ActorRef, Actor, ActorPath}
import com.typesafe.config.ConfigFactory
import akka.routing.{Broadcast, BroadcastPool}
import java.util.{Timer, TimerTask}

case object SERVERBLOCK

class RaftTest extends Actor {

  var testInterval : Int = ConfigFactory.load.getInt("test-Interval")
  var testFrequency : Int = ConfigFactory.load.getInt("test-Frequency")

  //Globals
	var timer = new java.util.Timer()
	var followerTimeout = new java.util.TimerTask {
		def run() = {
			println("this should not be printed")
		}
	}
  var generator = new scala.util.Random

  /*
  Handles all incoming messages 
  */
  def receive() = {
  	case CANCEL =>
  		followerTimeout.cancel()
  	case START =>
  		timer = new java.util.Timer()
  		followerTimeout = new java.util.TimerTask {
  			var tempSender = sender
  			def run() = {
          var num = generator.nextInt(testFrequency)
          if (num == 5) {
  				  tempSender ! SERVERBLOCK
          }
  			}
  		}
  		timer.schedule(followerTimeout, 0, testInterval)
  	}
  }

