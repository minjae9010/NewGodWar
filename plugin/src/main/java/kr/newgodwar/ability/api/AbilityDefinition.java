package kr.newgodwar.ability.api;

public final class AbilityDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final String author;
    private final boolean enabledByDefault;
    private final AbilityFactory factory;

    public AbilityDefinition(String id, String name, String description, String author, boolean enabledByDefault, AbilityFactory factory) {
        this.id = id;
        this.name = name;
        this.description = description;
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
