package kr.newgodwar.game;

import java.util.Locale;

public enum KilltimeMode {
    PLAYER_COMBAT("player-combat", "유저 간 공격 차단"),
    CORE_ONLY("core-only", "코어 파괴 차단");

    private final String configValue;
    private final String displayName;

    KilltimeMode(String configValue, String displayName) {
        this.configValue = configValue;
        this.displayName = displayName;
    }

    public String configValue() {
        return configValue;
    }

    public String displayName() {
        return displayName;
    }

    public static KilltimeMode parse(String value) {
        if (value == null) {
            return PLAYER_COMBAT;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if ("core".equals(normalized) || "core-only".equals(normalized)) {
            return CORE_ONLY;
        }
        return PLAYER_COMBAT;
    }
}
