package kr.newgodwar.ability.feedback;

/** Geometry describes the action, not an arbitrary emblem attached to an ability's name. */
public enum EffectCue {
    NONE(0, "추가 입자 없음"), HIT(20, "타격 파편"), SLASH(35, "단일 검격"),
    HEAL(80, "상승하는 회복 하트"), CLEANSE(85, "정화 빛"), POISON(65, "독 기운"),
    ROOT(60, "발목을 감는 뿌리"), SLOW(40, "발목 속박"), GUARD(55, "전방 방패"),
    FIRE(50, "화염"), STEALTH(70, "사라지는 연기"), BLIND(65, "눈앞의 어둠"), WIND(35, "발뒤 바람"),
    ITEM(30, "손의 생성 반짝임"), FORGE(40, "손의 단조 불꽃"), PORTAL(70, "이동 관문"),
    MUSIC(65, "음표"), CHARGE(40, "주먹의 전하"), WINGS(40, "등의 깃털 날개"),
    SUN(40, "머리 위 태양"), MOON(40, "머리 위 초승달"), FROST(50, "눈 결정"),
    SLEEP(70, "수면 표시"), SEAL(75, "봉인 자물쇠"), WATER(45, "낮은 물보라"),
    ARCANE(30, "손끝 주문"), BLOOM(45, "새싹"), HUNGER(25, "허기 연기");

    public enum Ink { CRIT, SWEEP, HEART, LIGHT, WITCH, SMOKE, FLAME, CLOUD, SPARK, PORTAL, NOTE, SNOW, WATER, ENCHANT, LEAF, COLOR, ELEMENT }
    public interface PointSink { void point(Ink ink, double x, double y, double z, int rgb); }
    private static final double[] SHIELD_X = {-0.5D, 0.5D, 0.45D, 0, -0.45D};
    private static final double[] SHIELD_Y = {1.65D, 1.65D, 0.8D, 0.35D, 0.8D};
    private final int priority;
    private final String description;
    EffectCue(int priority, String description) { this.priority = priority; this.description = description; }
    public int priority() { return priority; }
    public String description() { return description; }

    /** One complete, small silhouette. It is never redrawn at three overlapping sizes. */
    public void draw(PointSink sink) {
        if (this == NONE) return;
        int count = this == SLASH ? 1 : this == HEAL || this == MUSIC ? 3 : this == WIND ? 4
            : this == BLIND ? 6 : this == WINGS ? 30 : this == SUN ? 28
            : this == ROOT || this == SEAL || this == SLEEP ? 24
            : this == GUARD || this == PORTAL || this == MOON ? 20 : 10;
        for (int i = 0; i < count; i++) {
            double t = i / (double) Math.max(1, count - 1), a = t * Math.PI * 2;
            double x = 0, y = 1, z = 0; int rgb = 0; Ink ink;
            switch (this) {
                case SLASH: ink = Ink.SWEEP; z = 0.2D; break;
                case HIT: ink = Ink.CRIT; x = Math.cos(a) * 0.25D; y += Math.sin(a) * 0.25D; z = 0.4D; break;
                case HEAL: ink = Ink.HEART; x = (i - 1) * 0.35D; y = 1.6D + i * 0.18D; z = 0.4D; break;
                case CLEANSE: ink = Ink.LIGHT; x = Math.cos(a) * 0.4D; z = Math.sin(a) * 0.4D; y = 0.3D + t * 1.5D; break;
                case POISON: ink = Ink.WITCH; x = Math.cos(a) * 0.3D; z = Math.sin(a) * 0.3D; y = 0.5D + t; break;
                case ROOT:
                    ink = Ink.COLOR; rgb = i % 6 == 5 ? 0x78A34C : 0x705335;
                    double branch = (i / 6) * Math.PI / 2, climb = (i % 6) / 5D;
                    x = Math.cos(branch + climb * 1.5D) * (0.5D - climb * 0.18D);
                    z = Math.sin(branch + climb * 1.5D) * (0.5D - climb * 0.18D); y = 0.05D + climb * 0.65D; break;
                case SLOW: ink = Ink.LIGHT; x = Math.cos(a) * 0.42D; z = Math.sin(a) * 0.42D; y = 0.2D; break;
                case GUARD:
                    ink = Ink.LIGHT;
                    // A pointed shield outline in front of the chest, not a full-body sphere.
                    int shieldEdge = i / 4, next = (shieldEdge + 1) % 5; double progress = (i % 4) / 4D;
                    x = SHIELD_X[shieldEdge] + (SHIELD_X[next] - SHIELD_X[shieldEdge]) * progress;
                    y = SHIELD_Y[shieldEdge] + (SHIELD_Y[next] - SHIELD_Y[shieldEdge]) * progress; z = 0.65D; break;
                case FIRE: ink = Ink.FLAME; x = Math.cos(a) * 0.3D; z = Math.sin(a) * 0.3D; y = 0.2D + t * 1.3D; break;
                case STEALTH: ink = Ink.SMOKE; x = Math.cos(a) * 0.3D; z = Math.sin(a) * 0.3D; y = 0.4D + t; break;
                case BLIND: ink = Ink.SMOKE; x = (t - 0.5D) * 0.6D; y = 1.65D; z = 0.4D; break;
                case WIND: ink = Ink.CLOUD; x = (i % 2 == 0 ? -1 : 1) * 0.25D; y = 0.12D; z = -0.3D - t * 0.7D; break;
                case ITEM:
                    ink = Ink.SPARK; x = 0.38D + Math.cos(a) * 0.12D; y = 1.0D + Math.sin(a) * 0.12D; z = 0.4D; break;
                case FORGE:
                    ink = i < 3 ? Ink.FLAME : Ink.SPARK;
                    x = 0.38D + Math.cos(i * 2.4D) * t * 0.3D;
                    y = 0.95D + Math.sin(i * 2.4D) * t * 0.25D; z = 0.4D + t * 0.1D; break;
                case PORTAL: ink = Ink.PORTAL; x = Math.cos(a) * 0.5D; y = 1 + Math.sin(a) * 0.95D; break;
                case MUSIC: ink = Ink.NOTE; x = (t - 0.5D) * 0.8D; y = 1.7D + t * 0.4D; z = 0.35D; break;
                case CHARGE: ink = Ink.CRIT; x = (i % 2 == 0 ? -0.4D : 0.4D) + Math.cos(a) * 0.08D; y = 0.95D + Math.sin(a) * 0.12D; z = 0.3D; break;
                case WINGS:
                    ink = Ink.ELEMENT;
                    int side = i < 15 ? -1 : 1, feather = (i % 15) / 3;
                    double along = (i % 3) / 2D;
                    x = side * (0.28D + feather * 0.2D + along * 0.2D);
                    y = 1.45D + feather * 0.11D - along * (0.35D + feather * 0.07D); z = -0.3D - along * 0.2D; break;
                case SUN:
                    ink = Ink.FLAME;
                    double sunAngle = i < 12 ? i * Math.PI / 6 : ((i - 12) / 2) * Math.PI / 4;
                    double sunRadius = i < 12 ? 0.28D : i % 2 == 0 ? 0.42D : 0.57D;
                    x = Math.cos(sunAngle) * sunRadius; y = 2.75D + Math.sin(sunAngle) * sunRadius; break;
                case MOON:
                    ink = Ink.LIGHT;
                    double moonAngle = i < 12 ? Math.PI / 3 + i / 11D * Math.PI * 4 / 3 : 4.474D - (i - 12) / 7D * 2.665D;
                    x = (i < 12 ? 0 : 0.32D) + Math.cos(moonAngle) * (i < 12 ? 0.45D : 0.402D);
                    y = 2.65D + Math.sin(moonAngle) * (i < 12 ? 0.45D : 0.402D); break;
                case FROST: ink = Ink.SNOW; x = Math.cos(a) * 0.4D; z = Math.sin(a) * 0.4D; y = 0.3D + t * 0.7D; break;
                case SLEEP:
                    ink = Ink.COLOR; rgb = 0xD9E3F7;
                    double part = (i % 8) / 7D;
                    x = i < 8 ? -0.2D + part * 0.4D : i < 16 ? 0.2D - part * 0.4D : -0.2D + part * 0.4D;
                    y = i < 8 ? 2.65D : i < 16 ? 2.65D - part * 0.4D : 2.25D; break;
                case SEAL:
                    ink = Ink.COLOR; rgb = 0xD2B175;
                    if (i < 16) { double edge = (i % 4) / 3D; int wall = i / 4;
                        x = wall == 0 ? -0.25D + edge * 0.5D : wall == 1 ? 0.25D : wall == 2 ? 0.25D - edge * 0.5D : -0.25D;
                        y = wall == 0 ? 1.3D : wall == 1 ? 1.3D - edge * 0.4D : wall == 2 ? 0.9D : 0.9D + edge * 0.4D;
                    } else { double arc = (i - 16) / 7D * Math.PI; x = Math.cos(arc) * 0.16D; y = 1.3D + Math.sin(arc) * 0.2D; }
                    z = 0.45D; break;
                case WATER: ink = Ink.WATER; x = Math.cos(a) * 0.65D; z = Math.sin(a) * 0.65D; y = 0.2D; break;
                case ARCANE: ink = Ink.ENCHANT; x = 0.35D; z = 0.45D + t * 0.25D; y = 1 + t * 0.2D; break;
                case BLOOM: ink = Ink.LEAF; x = Math.cos(a) * 0.4D; z = Math.sin(a) * 0.4D; y = 0.15D + t * 0.4D; break;
                case HUNGER: ink = Ink.SMOKE; x = Math.cos(a) * 0.2D; y = 0.7D + t * 0.3D; break;
                default: throw new AssertionError(this);
            }
            sink.point(ink, x, y, z, rgb);
        }
    }
}
