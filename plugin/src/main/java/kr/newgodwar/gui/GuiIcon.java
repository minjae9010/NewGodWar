package kr.newgodwar.gui;

import java.util.Locale;

/** Stable, namespaced menu assets in the optional NewGodWar art pack. */
enum GuiIcon {
    SETTINGS, GAME, TEAM, WORLD, CORE, DISPLAY, COMBAT, GAMBLING,
    HOME, HELP, ABILITY, ITEMS, MAP, REWARDS, BACK, NEXT, PREVIOUS, CLOSE,
    TOGGLE_ON, TOGGLE_OFF, PLUS, MINUS, CONFIRM, CANCEL, REFRESH, PLAY, STOP,
    COIN, INFO, FRAME, ACCENT;

    String key() { return "gui/" + name().toLowerCase(Locale.ROOT); }
}
