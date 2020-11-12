package server

import akka.actor.{ActorSystem, Props, ActorRef, Actor, ActorPath}
import com.typesafe.config.ConfigFactory
import akka.routing.{Broadcast, BroadcastPool}
import java.util.{Timer, TimerTask}

case object LEADERBEAT

class HeartbeatTimer extends Actor {

  var heartbeatInterval = ConfigFactory.load.getInt("heartbeat-Interval")

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
  				tempSender ! LEADERBEAT
  			}
  		}
  		timer.schedule(followerTimeout, 0, heartbeatInterval)
  	}
  }

