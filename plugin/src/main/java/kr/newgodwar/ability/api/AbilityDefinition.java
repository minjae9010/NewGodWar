package kr.newgodwar.ability.api;

public final class AbilityDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final String normalSkill;
    private final int normalStoneCost;
    private final String advancedSkill;
    private final int advancedStoneCost;
    private final String passiveSkill;
    private final String author;
    private final boolean enabledByDefault;
    private final AbilityFactory factory;

    public AbilityDefinition(String id, String name, String description, String author, boolean enabledByDefault, AbilityFactory factory) {
        this(id, name, description, "", 0, "", 0, "", author, enabledByDefault, factory);
    }

    public AbilityDefinition(String id, String name, String description, String normalSkill, int normalStoneCost, String advancedSkill, int advancedStoneCost, String passiveSkill, String author, boolean enabledByDefault, AbilityFactory factory) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.normalSkill = normalSkill;
        this.normalStoneCost = normalStoneCost;
        this.advancedSkill = advancedSkill;
        this.advancedStoneCost = advancedStoneCost;
        this.passiveSkill = passiveSkill;
        this.author = author;
        this.enabledByDefault = enabledByDefault;
        this.factory = factory;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String normalSkill() {
        return normalSkill;
    }

    public int normalStoneCost() {
        return normalStoneCost;
    }

    public String advancedSkill() {
        return advancedSkill;
    }

    public int advancedStoneCost() {
        return advancedStoneCost;
    }

    public String passiveSkill() {
        return passiveSkill;
    }

    public String author() {
        return author;
    }

    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    public GodAbility create() {
        return factory.create();
    }
}
