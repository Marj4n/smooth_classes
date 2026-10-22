package org.marj4n.smooth_classes.origin;

/** Pure rules for Vampire sunlight exposure, shared by all Vampire forms.
 *  Keep tests independent of Minecraft runtime classes.
 */
public final class VampireSunlightRules {
    private VampireSunlightRules() {}

    /** A normal vanilla rainstorm shields only where precipitation reaches the
     *  ground; severe thunderclouds and local Blood Rain also shield the sky.
     */
    public static boolean weatherSheltered(boolean rainingHere, boolean thunderstorm,
                                           boolean insideBloodRain) {
        return rainingHere || thunderstorm || insideBloodRain;
    }

    public static boolean exposedToSunlight(boolean batForm, boolean isDay,
                                             boolean skyVisible, boolean weatherShelter) {
        return !batForm && isDay && skyVisible && !weatherShelter;
    }

    /** Match the exact 30-tick temporary effects Smooth Classes uses for sunlight.
     *  The calling code must ALSO require its sunlight-origin state marker.
     */
    public static boolean isShortSunlightEffect(int amplifier, int duration,
                                                boolean ambient, boolean particles, boolean icon) {
        return amplifier == 0 && duration > 0 && duration <= 30
                && !ambient && !particles && icon;
    }
}
