package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.ProtocolPacketEvent
import com.github.retrooper.packetevents.protocol.ConnectionState
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PacketEventReceiveSubscriber
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/*
    This simulates actual network lag by delaying packets in transit instead of using
    Bukkit event listeners to delay chat, "lag back" (unrealistic cause TCP), etc.

    Client network lag is bidirectional; it takes time for packets to travel from the
    client to the server and vice versa

    No packet types (keep alive, ping/pong, etc.) need to be blacklisted as we use a FIFO
    queue to abide by TCP ordering, which is future-proof against protocol changes

    I am quite proud of how this has turned out, this is probably one of my favorite
    commands in this plugin
*/
@CommandInfo(
    name = "lag",
    description = "Makes a player \"lag\".",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.LONG,
            purpose = "RTT",
            description = "The round-trip time (RTT) in milliseconds (min 2, max 10000)",
            default = "300"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.LONG,
            purpose = "Jitter",
            description = "The amount of randomness to add to the RTT (min 0, max 100)",
            default = "5"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Lag : BaseToggleTroll(), PacketEventReceiveSubscriber, PacketEventSendSubscriber {
    // Shared scheduler for each duplex
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private class OrderedPacketQueue(val scheduler: ScheduledExecutorService) {
        private class Entry(val deadline: Long, val send: () -> Unit)

        private val pending = ArrayDeque<Entry>()
        private var scheduled: ScheduledFuture<*>? = null
        private var closed = false

        @Synchronized
        fun enqueue(delayMs: Long, send: () -> Unit): Boolean {
            if (closed)
                return false
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMs.coerceAtLeast(0))
            pending.addLast(Entry(deadline, send))
            if (scheduled == null) {
                try {
                    scheduleHead()
                } catch (_: RejectedExecutionException) {
                    pending.removeLast()
                    return false
                }
            }
            return true
        }

        private fun scheduleHead() {
            val delay = (pending.first.deadline - System.nanoTime()).coerceAtLeast(0)
            scheduled = scheduler.schedule({ deliverHead() }, delay, TimeUnit.NANOSECONDS)
        }

        @Synchronized
        private fun deliverHead() {
            scheduled = null
            if (closed || pending.isEmpty())
                return
            try {
                pending.removeFirst().send()
            } finally {
                if (pending.isNotEmpty()) {
                    try {
                        scheduleHead()
                    } catch (_: RejectedExecutionException) {
                        drain()
                    }
                }
            }
        }

        @Synchronized
        fun close() {
            closed = true
            scheduled?.cancel(false)
            scheduled = null
            drain()
        }

        private fun drain() {
            while (pending.isNotEmpty()) pending.removeFirst().send()
        }
    }

    private class Session(
        val rtt: Long,
        val jitter: Long,
        val inbound: OrderedPacketQueue,
        val outbound: OrderedPacketQueue
    )

    private val sessions = ConcurrentHashMap<UUID, Session>()

    override fun onDisable() {
        sessions.forEach { [_, session] -> session.inbound.close(); session.outbound.close() }
        scheduler.shutdownNow()
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val rtt = args.getOrNull(2)?.toLongOrNull()?.coerceIn(2L, 10000L) ?: 300L
        val jitter = args.getOrNull(3)?.toLongOrNull()?.coerceIn(0L, 100L) ?: 5L
        val [inbound, outbound] = Pair(OrderedPacketQueue(scheduler), OrderedPacketQueue(scheduler))
        sessions[target.uniqueId] = Session(rtt, jitter, inbound, outbound)
        return "&c${target.name} &fis now on Wi-Fi"
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fis no longer lagging"

    override fun onRemovePlayer(player: Player, reason: String) {
        sessions[player.uniqueId]?.let { session ->
            session.inbound.close()
            session.outbound.close()
            sessions.remove(player.uniqueId, session)
        }
    }

    // We are only really interested in delaying the PLAY state
    private fun isUnsafe(event: ProtocolPacketEvent): Boolean
        = event.connectionState != ConnectionState.PLAY

    private fun snapshot(event: ProtocolPacketEvent): ByteArray {
        val buffer = event.fullBufferClone as ByteBuf
        return try {
            ByteBufUtil.getBytes(buffer)
        } finally {
            buffer.release()
        }
    }

    // Add jitter for realism
    fun jitteryOneTripDelay(uuid: UUID): Long {
        val session = sessions[uuid] ?: return 0
        // Divide by 2 to represent one trip delay
        return (session.rtt + MathUtil.random(-session.jitter, session.jitter)) / 2
    }

    fun interceptEvent(event: ProtocolPacketEvent) {
        if (isUnsafe(event)) return
        val session = sessions[event.user.uuid] ?: return
        val packet = snapshot(event)
        if (session.inbound.enqueue(jitteryOneTripDelay(event.user.uuid)) {
            when (event) {
                is PacketReceiveEvent -> event.user.receivePacketSilently(Unpooled.wrappedBuffer(packet))
                is PacketSendEvent -> event.user.sendPacketSilently(Unpooled.wrappedBuffer(packet))
            }
        }) {
            event.isCancelled = true
        }
    }

    override fun onPacketReceive(event: PacketReceiveEvent) = interceptEvent(event)
    override fun onPacketSend(event: PacketSendEvent) = interceptEvent(event)
}