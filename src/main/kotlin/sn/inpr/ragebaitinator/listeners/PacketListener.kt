package sn.inpr.ragebaitinator.listeners

import com.github.retrooper.packetevents.event.PacketListener as PacketEventsListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import sn.inpr.ragebaitinator.command.CommandManager

interface PacketEventSendSubscriber {
    fun onPacketSend(event: PacketSendEvent)
}

interface PacketEventReceiveSubscriber {
    fun onPacketReceive(event: PacketReceiveEvent)
}

class PacketListener : PacketEventsListener {
    override fun onPacketSend(event: PacketSendEvent?) {
        if (event == null || event.isCancelled)
            return

        for (command in CommandManager.commands) {
            if (command is PacketEventSendSubscriber) {
                command.onPacketSend(event)
            }
        }
    }

    override fun onPacketReceive(event: PacketReceiveEvent?) {
        if (event == null || event.isCancelled)
            return

        for (command in CommandManager.commands) {
            if (command is PacketEventReceiveSubscriber) {
                command.onPacketReceive(event)
            }
        }
    }
}