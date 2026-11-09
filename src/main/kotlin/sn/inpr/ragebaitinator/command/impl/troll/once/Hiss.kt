package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.protocol.sound.SoundCategory
import com.github.retrooper.packetevents.protocol.sound.Sounds
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "hiss",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.CREEPER_SPAWN_EGG,
    description = "Plays a creeper hiss sound from directly behind the player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ]
)
class Hiss : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val behind = target.location.toVector()
            .add(LocationUtil.vectorRelativeYaw(target, 180, 2.0))
        val position = Vector3d(behind.x, behind.y, behind.z)
        target.sendPacket(WrapperPlayServerSoundEffect(
            Sounds.ENTITY_CREEPER_PRIMED,
            SoundCategory.HOSTILE,
            position,
            1.0f,
            0.5f
        ))
        return placeholdersOr("init", "%prefix% %c2%Gave %c1%%player% %c2%the scare of their life")
            .put("player", target.name)
            .toString()
    }
}