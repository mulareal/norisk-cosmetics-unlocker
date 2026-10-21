package gg.norisk.cosmetics.unlocker.api;

import gg.norisk.animations.model.GeoModel;
import gg.norisk.animations.resource.GeoResourceLocation;

/**
 * A held shield drawn by NoRisk's shield renderer with another model. The transforms are item display JSON
 * ({@code {"display": {"firstperson_righthand": ...}}}) for the normal and the blocking pose.
 * {@link #HIDDEN} draws nothing, e.g. because an add-on shows the shield on the arm instead.
 */
public record HeldShield(GeoModel model, GeoResourceLocation texture, String transform, String blockingTransform) {

    public static final HeldShield HIDDEN = new HeldShield(null, null, null, null);

    public boolean hidden() {
        return model == null;
    }
}
