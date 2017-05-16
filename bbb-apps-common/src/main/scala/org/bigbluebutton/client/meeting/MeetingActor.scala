package org.bigbluebutton.client.meeting

import akka.actor.{Actor, ActorLogging, Props}
import org.bigbluebutton.client.bus.{ConnectMsg, DisconnectMsg, MsgFromClientMsg}
import org.bigbluebutton.common2.messages.BbbServerMsg

object MeetingActor {
  def props(meetingId: String): Props =
  Props(classOf[MeetingActor], meetingId)
}

class MeetingActor(val meetingId: String) extends Actor with ActorLogging {

  private val userMgr = new UsersManager

  def receive = {
    case msg: ConnectMsg => handleConnectMsg(msg)
    case msg: DisconnectMsg => handleDisconnectMsg(msg)
    case msg: MsgFromClientMsg => handleMsgFromClientMsg(msg)
    case msg: BbbServerMsg => handleBbbServerMsg(msg)
      // TODO: Should keep track of user lifecycle so we can remove when user leaves the meeting.
  }

  private def createUser(id: String): User = {
    User(id)
  }

  def handleConnectMsg(msg: ConnectMsg): Unit = {
    UsersManager.findWithId(userMgr, msg.connInfo.userId) match {
      case Some(m) => m.actorRef forward(msg)
      case None =>
        val m = createUser(msg.connInfo.userId)
        UsersManager.add(userMgr, m)
        m.actorRef forward(msg)
    }
  }

  def handleDisconnectMsg(msg: DisconnectMsg): Unit = {
    for {
      m <- UsersManager.findWithId(userMgr, msg.connInfo.meetingId)
    } yield {
      m.actorRef forward(msg)
    }
  }

  def handleMsgFromClientMsg(msg: MsgFromClientMsg):Unit = {
    for {
      m <- UsersManager.findWithId(userMgr, msg.connInfo.meetingId)
    } yield {
      m.actorRef forward(msg)
    }
  }

  def handleBbbServerMsg(msg: BbbServerMsg): Unit = {
    for {
      msgType <- msg.envelope.routing.get("msgType")
    } yield {
      handleServerMsg(msgType, msg)
    }
  }

  def handleServerMsg(msgType: String, msg: BbbServerMsg): Unit = {
    msgType match {
      case "direct" => handleDirectMessage(msg)
      case "broadcast" => handleBroadcastMessage(msg)
      case "system" => handleSystemMessage(msg)
    }
  }

  private def forwardToUser(msg: BbbServerMsg): Unit = {
    for {
      userId <- msg.envelope.routing.get("userId")
      m <- UsersManager.findWithId(userMgr, userId)
    } yield {
      m.actorRef forward(msg)
    }
  }

  def handleDirectMessage(msg: BbbServerMsg): Unit = {
    // In case we want to handle specific messages. We can do it here.
    forwardToUser(msg)
  }

  def handleBroadcastMessage(msg: BbbServerMsg): Unit = {
    // In case we want to handle specific messages. We can do it here.
    forwardToUser(msg)
  }

  def handleSystemMessage(msg: BbbServerMsg): Unit = {
    // In case we want to handle specific messages. We can do it here.
    forwardToUser(msg)
  }
}
