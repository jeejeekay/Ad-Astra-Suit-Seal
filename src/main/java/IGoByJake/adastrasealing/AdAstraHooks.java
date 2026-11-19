package IGoByJake.adastrasealing;

import earth.terrarium.adastra.api.events.AdAstraEvents;

/**
 * Tells Ad Astra when a suit that includes sealed pieces is protecting its wearer.
 * <p>
 * Ad Astra checks suits by item tag, which is per item type, so it can't see a seal on one particular chestplate.
 * Its tick events let another mod cancel the damage instead: returning false from a listener skips that effect.
 * Sets made only of real Ad Astra pieces never reach these (Ad Astra handles them before firing the events, or
 * the listeners below say "carry on").
 */
public final class AdAstraHooks {
    private AdAstraHooks() {
    }

    public static void register() {
        // Fired once a second before Ad Astra's suffocation check. False = skip the check.
        AdAstraEvents.OxygenTickEvent.register((level, entity) -> !SuitLogic.suppliesOxygen(entity));
        // Fired when Ad Astra is about to freeze or burn an entity. False = don't.
        AdAstraEvents.ColdTemperatureTickEvent.register((level, entity) -> !SuitLogic.protectsFromCold(entity));
        AdAstraEvents.HotTemperatureTickEvent.register((level, entity) -> !SuitLogic.protectsFromHeat(entity));
    }
}
