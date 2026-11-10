package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.asRb

@CommandInfo(
    name = "bleed",
    category = CommandInfo.Category.COMBAT,
    icon = Material.REDSTONE,
    description = "Every wound is a bad one.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Bleed : BaseToggleTroll(), EntityDamageSubscriber {
    private val duration = param<Int>("Duration", "How many seconds they bleed for after each hit (min 1, max 300).", default = 10, range = 1..300)
    private val damage = param<Double>("Damage", "How much health every second of bleeding costs (min 0.5, max 10).", default = 1.0, range = 0.5..10.0)

    override val syntax = withTarget(duration, damage)

    private companion object {
        // damage that already is damage over time doesn't open a new wound
        val IGNORED = setOf(
            EntityDamageEvent.DamageCause.POISON, EntityDamageEvent.DamageCause.WITHER,
            EntityDamageEvent.DamageCause.STARVATION, EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.FREEZE
        )
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "duration", args[duration])
        setField(target, "damage", args[damage])
        setField(target, "remaining", 0)
        setTaskTimer(target, { tick(target) }, 20, 20)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now bleed after getting hurt", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has stopped bleeding", target)
            .toString()

    private fun tick(player: RbPlayer) {
        val remaining = getField<Int>(player, "remaining") ?: 0
        val body = player.bukkit ?: return
        if (remaining <= 0 || player.isDead)
            return

        setField(player, "remaining", remaining - 1)
        body.world.spawnParticle(
            Particle.BLOCK, body.location.add(0.0, 1.0, 0.0), 15, 0.25, 0.4, 0.25,
            Material.REDSTONE_BLOCK.createBlockData()
        )

        addFlag(player, "bleeding")
        try {
            player.damage(getField<Double>(player, "damage") ?: 1.0)
        } finally {
            removeFlag(player, "bleeding")
        }
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity.asRb ?: return
        if (event.isCancelled || !hasPlayer(player) || hasFlag(player, "bleeding"))
            return
        if (event.cause in IGNORED || event.finalDamage <= 0.0)
            return

        if ((getField<Int>(player, "remaining") ?: 0) <= 0)
            player.send("&4You are bleeding...")
        setField(player, "remaining", getField<Int>(player, "duration") ?: 10)
    }
}
