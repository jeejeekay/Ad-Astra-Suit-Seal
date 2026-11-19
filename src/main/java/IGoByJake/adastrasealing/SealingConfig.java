package IGoByJake.adastrasealing;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/adastra_sealing-common.toml */
public final class SealingConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue BASE_CAPACITY;
    private static final ForgeConfigSpec.IntValue MAX_CAPACITY;
    private static final ForgeConfigSpec.BooleanValue INSULATED_FIRE_IMMUNITY;

    private static final int DEFAULT_BASE = 1000;
    private static final int DEFAULT_MAX = 10000;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("oxygen");
        BASE_CAPACITY = builder
                .comment("Oxygen a sealed chestplate holds before any Capacity Upgrade, in millibuckets.",
                        "Ad Astra's space suit holds 1000.")
                .defineInRange("baseCapacity", DEFAULT_BASE, 1, 1_000_000);
        MAX_CAPACITY = builder
                .comment("The most oxygen any chestplate can hold after Capacity Upgrades, in millibuckets.",
                        "Applies to sealed chestplates and to upgraded Ad Astra suits.")
                .defineInRange("maxCapacity", DEFAULT_MAX, 1, 1_000_000);
        builder.pop();
        builder.push("protection");
        INSULATED_FIRE_IMMUNITY = builder
                .comment("A full set of Insulated-sealed pieces (netherite space suit pieces count too) takes no fire",
                        "or lava damage, the same as a full netherite space suit in Ad Astra.")
                .define("insulatedFireImmunity", true);
        builder.pop();
        SPEC = builder.build();
    }

    private SealingConfig() {
    }

    public static int baseCapacity() {
        try {
            return BASE_CAPACITY.get();
        } catch (RuntimeException notLoadedYet) {
            return DEFAULT_BASE;
        }
    }

    public static int maxCapacity() {
        try {
            return MAX_CAPACITY.get();
        } catch (RuntimeException notLoadedYet) {
            return DEFAULT_MAX;
        }
    }

    public static boolean insulatedFireImmunity() {
        try {
            return INSULATED_FIRE_IMMUNITY.get();
        } catch (RuntimeException notLoadedYet) {
            return true;
        }
    }
}
