package sn.inpr.ragebaitinator.command.def

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandInfo(
    val name: String,
    val description: String = "No description",
    val syntax: Array<CommandSyntax> = [],
    val aliases: Array<String> = [],
    val attributes: Array<Attribute> = [],
    val incompatible: Array<String> = [] // trolls that are incompatible with this troll. they will be automatically removed when this troll is added
) {
    enum class Attribute(val humanReadable: String) {
        LOGOUT("Removed on logout"),
        DEATH("Removed on death");
    }
}