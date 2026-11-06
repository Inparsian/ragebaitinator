package sn.inpr.ragebaitinator.command.def

import org.bukkit.Material

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandInfo(
    val name: String,
    val description: String = "No description",
    val category: Category = Category.MISC,
    val icon: Material = Material.BOOK,
    val syntax: Array<CommandSyntax> = [],
    val aliases: Array<String> = [],
    val attributes: Array<Attribute> = [],
    val incompatible: Array<String> = [] // trolls that are incompatible with this troll. they will be automatically removed when this troll is added
) {
    enum class Category(val displayName: String, val color: String, val icon: Material, val blurb: String) {
        EXPLOSIVES("Explosives", "§c", Material.TNT, "Things that go boom."),
        HAZARDS("Mobs & Hazards", "§2", Material.ZOMBIE_HEAD, "Mobs, projectiles and other things out to get a player."),
        WORLD("Blocks & World", "§a", Material.GRASS_BLOCK, "Changes to the blocks and the world around a player."),
        MOVEMENT("Movement", "§b", Material.FEATHER, "Changes to how a player moves, falls and flies."),
        INVENTORY("Inventory & Items", "§6", Material.CHEST, "What a player carries, picks up and uses."),
        COMBAT("Combat", "§4", Material.IRON_SWORD, "Changes to how a player fights and takes hits."),
        SURVIVAL("Health & Hunger", "§d", Material.GOLDEN_APPLE, "Health, hunger, sleep and respawning."),
        CHAT("Chat & Commands", "§e", Material.OAK_SIGN, "Changes what a player says, and what happens when they say it."),
        ILLUSIONS("Illusions & Scares", "§5", Material.ENDER_EYE, "Things only the player sees, hears or feels."),
        MISC("Miscellaneous", "§7", Material.BARREL, "Everything else.");

        val key: String get() = name.lowercase()
    }

    enum class Attribute {
        LOGOUT,
        DEATH;
    }
}