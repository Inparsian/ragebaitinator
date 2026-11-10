package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.*
import com.github.retrooper.packetevents.protocol.ConnectionState
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*
import java.util.concurrent.*

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
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.REPEATER,
    description = "Makes a player \"lag\".",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Lag : BaseToggleTroll() {
    private val rtt = param<Long>("RTT", "The round-trip time (RTT) in milliseconds (min 2, max 10000)", default = 300L, range = 2L..10000L)
    private val jitter = param<Long>("Jitter", "The amount of randomness to add to the RTT (min 0, max 100)", default = 5L, range = 0L..100L)

    override val syntax = withTarget(rtt, jitter)

    // Shared scheduler for each duplex
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    // After packets have gone past the main listener (w/ HIGH priority), this one decides the
    // final state (i.e., whether it must be delayed)
    private class LagPacketListener(val parent: Lag) : PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
        override fun onPacketSend(event: PacketSendEvent?) = parent.interceptEvent(event)
        override fun onPacketReceive(event: PacketReceiveEvent?) = parent.interceptEvent(event)
    }

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

    init {
        PacketEvents.getAPI().eventManager.registerListener(LagPacketListener(this))
    }

    override fun onDisable() {
        fieldValues<Session>("session").forEach { session -> session.inbound.close(); session.outbound.close() }
        scheduler.shutdownNow()
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val rtt = args[rtt]
        val jitter = args[jitter]
        val [inbound, outbound] = Pair(OrderedPacketQueue(scheduler), OrderedPacketQueue(scheduler))
        setField(target, "session", Session(rtt, jitter, inbound, outbound))
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now on Wi-Fi", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer lagging", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        getField<Session>(player, "session")?.let { session ->
            session.inbound.close()
            session.outbound.close()
        }
    }

    // Whatever a listener changes through a wrapper (Velocity, for one) isn't written back into the event's buffer until
    // every listener has run, so at this point the buffer still holds the packet as it was before any of them touched
    // it. When that has happened, the wrapper is written out the same way it will be once we're done
    private fun snapshot(event: ProtocolPacketEvent): ByteArray {
        val wrapper = event.lastUsedWrapper
        return if (wrapper != null && event.needsReEncode())
            serialize(event, wrapper)
        else
            snapshotBuffer(event)
    }

    private fun serialize(event: ProtocolPacketEvent, wrapper: PacketWrapper<*>): ByteArray {
        val original = wrapper.getBuffer()
        val buffer = Unpooled.buffer()
        return try {
            wrapper.setBuffer(buffer)
            wrapper.writeVarInt(event.packetId)
            wrapper.write()
            ByteBufUtil.getBytes(buffer)
        } finally {
            // if this packet isn't delayed after all, it still has to be written where it was going to be
            wrapper.setBuffer(original)
            buffer.release()
        }
    }

    private fun snapshotBuffer(event: ProtocolPacketEvent): ByteArray {
        val buffer = event.fullBufferClone as ByteBuf
        return try {
            ByteBufUtil.getBytes(buffer)
        } finally {
            buffer.release()
        }
    }

    // Add jitter for realism
    fun jitteryOneTripDelay(uuid: UUID): Long {
        val session = getField<Session>(uuid, "session") ?: return 0
        // Divide by 2 to represent one trip delay
        return (session.rtt + MathUtil.random(-session.jitter, session.jitter)) / 2
    }

    fun interceptEvent(event: ProtocolPacketEvent?) {
        // We are only really interested in delaying the PLAY state
        if (event == null || event.isCancelled || event.connectionState != ConnectionState.PLAY)
            return
        val uuid = event.user.uuid ?: return
        val session = getField<Session>(uuid, "session") ?: return
        val packet = snapshot(event)
        if (session.inbound.enqueue(jitteryOneTripDelay(uuid)) {
            when (event) {
                is PacketReceiveEvent -> event.user.receivePacketSilently(Unpooled.wrappedBuffer(packet))
                is PacketSendEvent -> event.user.sendPacketSilently(Unpooled.wrappedBuffer(packet))
            }
        }) {
            event.isCancelled = true
        }
    }
}