# Raft
Fault Tolerance System

TO RUN:
“sbt run” at root folder

No further input is necessary. Everything is generated at random so there will be no similar outputs. All phases start at phase0 and increase on the first timeout. The Raft algorithm was implemented using akka actors. I wanted it to be a true distributed system without having a maserActor(server) above the actorServers. So all “actorServers are at /user/_. The servers have direct contact with one another until told not to by the raftTest.

CLASSES:
The raftTest randomly chooses to make a actorServer crash or return to service. It may be the leader or a follower that crashes and the raft implementation handles the cases. In some cases, we may not have enough servers to have a majority agreement on the leader, so the running servers will keep trying until the needed majority is back up. Also, the test(s) may start in phase0 with a server down, before it even runs the raft implementation. 

The raftTimer is an actor for each actorServer that tells the actorServer when it times out. The timeout is use for both as a follower and as a leaderElect. A leaderElect is when an actorServer increases the phase but needs a majority to become the leader, before it times out. 

The hearbeatTimer is used to give the leading actorServer heartbeat interval releases to the other actorServers who are following the leader. If you are not the leader or came from being a leader, these will not appear to the actorServer and/or will be CANCELED. 

The raft.scala file is the heart of the algorithm and coordinates as an actorServer. 

The applicationconfig file contains the hard interval numbers. 

If you wish to add/subtract additional servers, you will have to change 3 places:
-applicationconfig file
-in “object raft” > add/subtract additional variable declaration(s) of actor
-under sendAll() you need to add/subtract the path of the new actor

There are also case classes used to help declare and transmit messages.

NOTES:
If you wish to see the returning HEARTBEATREPLYs then uncomment the marked section. This helps aid to see which servers are replying, so you know which servers are down – if they have been down for a while. 

