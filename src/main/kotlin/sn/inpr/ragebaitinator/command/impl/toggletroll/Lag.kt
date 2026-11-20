package sn.inpr.ragebaitinator.command.impl.toggletroll

import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.ScheduledFuture
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil
import java.nio.channels.ClosedChannelException
import java.util.*
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

    private companion object {
        const val HANDLER = "ragebaitinator_lag"
    }

    private class Lane {
        private class Held(val deadline: Long, val pass: () -> Unit, val discard: () -> Unit)

        private val held = ArrayDeque<Held>()
        private var timer: ScheduledFuture<*>? = null
        private var last = 0L

        fun hold(ctx: ChannelHandlerContext, delayNanos: Long, pass: () -> Unit, discard: () -> Unit) {
            val now = System.nanoTime()
            val deadline = maxOf(last, now + delayNanos)
            last = deadline
            if (held.isEmpty() && deadline <= now) {
                pass()
            } else {
                held.addLast(Held(deadline, pass, discard))
                if (timer == null)
                    arm(ctx)
            }
        }

        private fun arm(ctx: ChannelHandlerContext) {
            val delay = (held.first.deadline - System.nanoTime()).coerceAtLeast(0)
            timer = ctx.executor().schedule({ release(ctx) }, delay, TimeUnit.NANOSECONDS)
        }

        private fun release(ctx: ChannelHandlerContext) {
            timer = null
            val now = System.nanoTime()
            while (held.isNotEmpty() && held.first.deadline <= now)
                held.removeFirst().pass()
            if (held.isNotEmpty())
                arm(ctx)
        }

        fun flushAll() {
            timer?.cancel(false)
            timer = null
            while (held.isNotEmpty()) held.removeFirst().pass()
            last = 0L
        }

        fun discardAll() {
            timer?.cancel(false)
            timer = null
            while (held.isNotEmpty()) held.removeFirst().discard()
        }
    }

    // two lanes makes a road kekw
    private class Road(val inbound: Lane, val outbound: Lane) {
        fun flushAll() { outbound.flushAll(); inbound.flushAll() }
        fun discardAll() { inbound.discardAll(); outbound.discardAll() }
    }

    private class LagHandler(@Volatile var rtt: Long, @Volatile var jitter: Long) : ChannelDuplexHandler() {
        @Volatile var enabled = true
        private val road = Road(Lane(), Lane())

        // Add jitter for realism. Divide by 2 to represent one trip delay
        private fun oneTripNanos(): Long = if (!enabled) 0 else {
            val millis = (rtt + MathUtil.random(-jitter, jitter)) / 2
            return TimeUnit.MILLISECONDS.toNanos(millis.coerceAtLeast(0))
        }

        fun disable(channel: Channel) {
            enabled = false
            channel.eventLoop().execute {
                if (channel.pipeline().context(this) == null)
                    return@execute
                road.flushAll()
                channel.pipeline().remove(this)
            }
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) = road.inbound.hold(
            ctx, oneTripNanos(),
            pass = { ctx.fireChannelRead(msg) },
            discard = { ReferenceCountUtil.release(msg) }
        )

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) = road.outbound.hold(
            ctx, oneTripNanos(),
            pass = { ctx.writeAndFlush(msg, promise) },
            discard = {
                ReferenceCountUtil.release(msg)
                promise.tryFailure(ClosedChannelException())
            }
        )

        override fun channelInactive(ctx: ChannelHandlerContext) {
            road.discardAll()
            ctx.fireChannelInactive()
        }

        override fun handlerRemoved(ctx: ChannelHandlerContext) {
            if (ctx.channel().isActive) {
                road.flushAll()
            } else {
                road.discardAll()
            }
        }
    }

    private class Session(val channel: Channel, val handler: LagHandler)

    override fun onDisable() {
        fieldValues<Session>("session").toList().forEach { it.handler.disable(it.channel) }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val rtt = args[rtt]
        val jitter = args[jitter]
        val channel = target.packetEventsUser?.channel as? Channel ?: return errPlaceholdersOr(
            "noConnection",
            "%prefix% &cCould not find %player%'s connection",
            target
        ).toString()

        val handler = LagHandler(rtt, jitter)
        channel.eventLoop().execute {
            val pipeline = channel.pipeline()
            if (pipeline.get(HANDLER) != null)
                pipeline.remove(HANDLER)
            pipeline.addFirst(HANDLER, handler)
        }
        setField(target, "session", Session(channel, handler))
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now on Wi-Fi", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer lagging", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        getField<Session>(player, "session")?.let { it.handler.disable(it.channel) }
    }
}
