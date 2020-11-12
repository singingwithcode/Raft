package server

import akka.actor.{ActorSystem, Props, ActorRef, Actor, ActorPath}
import com.typesafe.config.ConfigFactory
import akka.routing.{Broadcast, BroadcastPool}
import java.util.{Timer, TimerTask}

case object CANCEL
case object START
case object TIMEOUT

class RaftTimer extends Actor {

	var minTimeout = ConfigFactory.load.getInt("minTimeout")
	var maxTimeout = ConfigFactory.load.getInt("maxTimeout")

	//Other
	var generator = new scala.util.Random

	var timer = new java.util.Timer()
	var followerTimeout = new java.util.TimerTask {
		def run() = {
			println("this should not be printed")
		}
	}

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
  				tempSender ! TIMEOUT
  			}
  		}
  		timer.schedule(followerTimeout, minTimeout + generator.nextInt( (maxTimeout - minTimeout) + 1))
  	}
  }

