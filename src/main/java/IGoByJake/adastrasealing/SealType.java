package IGoByJake.adastrasealing;

/** What a sealed armor piece protects against. The order matters: a later type includes the earlier ones. */
public enum SealType {
    /** Not sealed. */
    NONE(""),
    /** Like Ad Astra's space suit: oxygen and cold. */
    SPACE("space"),
    /** Like Ad Astra's netherite space suit: oxygen, cold and heat. */
    INSULATED("insulated");

    private final String id;

    SealType(String id) {
        this.id = id;
    }

    /** The name stored in item NBT and used in recipe files. */
    public String id() {
        return id;
    }

    public boolean isSealed() {
        return this != NONE;
    }

    public boolean protectsFromHeat() {
        return this == INSULATED;
    }

    public static SealType byId(String id) {
        for (SealType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return NONE;
    }
}
