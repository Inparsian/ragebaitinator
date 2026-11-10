package sn.inpr.ragebaitinator.command.impl.troll.toggle

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.*
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.AsyncPlayerChatSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil

// Another one of my favorites. Cause of Kotlin I can do way more than the old iteration
// without it looking like spaghetti :)
@CommandInfo(
    name = "genie",
    category = CommandInfo.Category.CHAT,
    icon = Material.LANTERN,
    description = "Be careful what you wish for :)",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Genie : BaseToggleTroll(), AsyncPlayerChatSubscriber {
    data class GenieMobEntry(val singular: String, val plural: String, val type: EntityType)
    data class GeniePotionEntry(val singular: String, val effect: PotionEffect)

    companion object {
        val genieMobs: Set<GenieMobEntry> field = mutableSetOf(
            // custom mob singular/plurals
            GenieMobEntry("skel(e|ling)ton", "skel(e|ling)tons", EntityType.SKELETON),
            GenieMobEntry("creep(er|ah)", "creep(er|ah)e?s", EntityType.CREEPER),
            GenieMobEntry("slime|booger", "(slime|booger)s", EntityType.SLIME),
            GenieMobEntry("wolf|dog(gy)?", "(wol(f|ve)s)|(dog(gie|gy)?s)", EntityType.WOLF),
            GenieMobEntry("bunny|(r|w)abbit", "(bunn(y|ies))|((r|w)abbits?)", EntityType.RABBIT),
            GenieMobEntry("bat", "bats", EntityType.BAT),
            GenieMobEntry("magma ?cube", "magma ?cubes", EntityType.MAGMA_CUBE),
            GenieMobEntry("silverfish(ie)?", "silverfish(ie)?s", EntityType.SILVERFISH),
        )

        val geniePotions: Set<GeniePotionEntry> field = mutableSetOf(
            GeniePotionEntry("levitat(e|ion)", PotionEffect(PotionEffectType.LEVITATION, 600, 2)),
            GeniePotionEntry("confus(e|ion)|nause(ous|a(t(ed|ing))?)|d(runk|izz(y|iness))", PotionEffect(PotionEffectType.NAUSEA, 600, 2)),
            GeniePotionEntry("hung(er|ry)", PotionEffect(PotionEffectType.HUNGER, 600, 2)),
            GeniePotionEntry("dig|min(e|ing)", PotionEffect(PotionEffectType.MINING_FATIGUE, 600, 2)),
            GeniePotionEntry("slow", PotionEffect(PotionEffectType.SLOWNESS, 600, 2)),
            GeniePotionEntry("sloth", PotionEffect(PotionEffectType.SLOWNESS, 600, 100)),
            GeniePotionEntry("blind|unsee", PotionEffect(PotionEffectType.BLINDNESS, 600, 2)),
            GeniePotionEntry("poison", PotionEffect(PotionEffectType.POISON, 600, 2)),
            GeniePotionEntry("weak", PotionEffect(PotionEffectType.WEAKNESS, 1200, 2)),
            GeniePotionEntry("wither", PotionEffect(PotionEffectType.WITHER, 600, 2)),
            GeniePotionEntry("glow|bright|radian(t|ce)|gleam", PotionEffect(PotionEffectType.GLOWING, 600, 2)),
            GeniePotionEntry("luck", PotionEffect(PotionEffectType.UNLUCK, 2400, 2))
        )

        init {
            fun pluralize(name: String): String = when {
                name.endsWith("man") -> name.dropLast(3) + "men"
                name.endsWith("y") && name.length > 1 && name[name.lastIndex - 1] !in "aeiou" -> name.dropLast(1) + "ies"
                name.endsWith("s") || name.endsWith("x") || name.endsWith("z") || name.endsWith("ch") || name.endsWith("sh") -> name + "es"
                else -> name + "s"
            }

            for (type in EntityType.entries) {
                if (type.entityClass?.let { Monster::class.java.isAssignableFrom(it) } == true && genieMobs.none { it.type == type }) {
                    val singular = type.name.lowercase().replace("_", " ?")
                    val plural = pluralize(singular)
                    genieMobs.add(GenieMobEntry(singular, plural, type))
                }
            }
        }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String =
        placeholdersOr("init", "%prefix% %c2%Anything %c1%%player% %c2%says shall now come to fruition", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can no longer manifest words into reality", target)
            .toString()

    override fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (!event.isAsynchronous || !hasPlayer(event.player))
            return
        val message = ChatColor.stripColor(event.message).lowercase()
        runTask({
            fun m(match: String) = Regex("\\b$match\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)

            // mob pass
            val mobs: MutableList<LivingEntity> = mutableListOf()
            for ([_, plural, type] in genieMobs.filter { m(it.plural) || m(it.singular) }) {
                val amount = if (m(plural)) 5 else 1
                repeat(amount) {
                    val entity = event.player.world.spawnEntity(LocationUtil.locationRelativeYaw(event.player, 180, 6.0), type)
                    if (entity is Rabbit)
                        entity.rabbitType = Rabbit.Type.THE_KILLER_BUNNY
                    if (entity is Creature)
                        entity.target = event.player
                    mobs.add(entity as? LivingEntity ?: return@repeat)
                }
            }

            // effects pass
            for ([_, effect] in geniePotions.filter { m(it.singular) }) {
                if (mobs.isNotEmpty())
                    mobs.forEach { it.addPotionEffect(effect) }
                event.player.addPotionEffect(effect)
            }

            when {
                m("death|die|kill|dying|decease") -> event.player.health = 0.0
                m("hal(f|ve)") -> if (event.player.health > 0.0) event.player.health /= 2
                m("hurt|pain|owie|heal") -> event.player.damage(4.0)
                m("explo(de|sion)|boom") -> event.player.world.createExplosion(event.player.location, 4f)
                m("fire|burn|flame") -> event.player.fireTicks = 100
                m("lightning|thunder") -> event.player.world.strikeLightning(event.player.location)
                m("starv(e|ing)") -> event.player.foodLevel = 0
                m("launch|fly|yeet|space") -> event.player.velocity = event.player.velocity.add(Vector(0.0, 10.0, 0.0))
                m("kick|log ?out|quit|le(ave|ft)|disconnect|bya|cya|gtg") -> event.player.kickPlayer(listOf("Cya!", "Goodbye!", "Bye!", "See ya!", "Later!").random())
                m("f[ae]ll|skydiv(e|ing)") -> event.player.teleport(event.player.location.add(0.0, 400.0, 0.0))
                m("void|abyss") -> {
                    val newLocation = event.player.location.clone()
                    newLocation.y = LocationUtil.downHeightLimit - 4.0
                    event.player.teleport(newLocation)
                }
            }
        })
    }
}