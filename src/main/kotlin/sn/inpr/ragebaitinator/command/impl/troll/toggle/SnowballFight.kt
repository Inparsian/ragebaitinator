package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.EntityShootBowEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityShootBowSubscriber

@CommandInfo(
    name = "snowballfight",
    description = "Replaces every arrow shot by the player with snowballs.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class SnowballFight : BaseToggleTroll(), EntityShootBowSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = "&c${target.name} &fis now reminiscing about their Christmas childhood"

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fis now able to shoot arrows again"

    override fun onEntityShootBow(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            val velocity = event.projectile.velocity
            event.projectile.remove()
            val snowball = player.launchProjectile(Snowball::class.java)
            snowball.velocity = velocity
        }
    }
}