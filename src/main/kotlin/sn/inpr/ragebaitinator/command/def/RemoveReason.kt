package sn.inpr.ragebaitinator.command.def

/** Why a player stopped being affected by a toggle troll. This is what [BaseToggleTroll.onRemovePlayer] is told. */
enum class RemoveReason {
    /** Whoever ran the command toggled it off */
    TOGGLED,
    /** The player left the server */
    LOGOUT,
    /** The player died */
    DEATH,
    /** The troll was cleared from everyone, as when the plugin disables */
    CLEARED,
    /** The troll was finished with them. This is also what removing a player without saying why means */
    DONE,
    /** Pardoned, which turns off every troll for them */
    PARDONED,
    /** The player is immune to the troll */
    IMMUNE,
    /** A troll that can't run along with this one was toggled on */
    INCOMPATIBLE,
    /** The troll couldn't carry on, because something it needed didn't work */
    FAILED,
}