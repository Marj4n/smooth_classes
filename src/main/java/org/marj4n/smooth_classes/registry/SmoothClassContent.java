package org.marj4n.smooth_classes.registry;

import org.marj4n.smooth_classes.content.archer.ArcherClass;
import org.marj4n.smooth_classes.content.archer.ArcherContent;
import org.marj4n.smooth_classes.content.assassin.AssassinClass;
import org.marj4n.smooth_classes.content.assassin.AssassinContent;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.content.avenger.AvengerContent;
import org.marj4n.smooth_classes.content.berserker.BerserkerClass;
import org.marj4n.smooth_classes.content.berserker.BerserkerContent;
import org.marj4n.smooth_classes.content.caster.CasterClass;
import org.marj4n.smooth_classes.content.caster.CasterContent;
import org.marj4n.smooth_classes.content.foreigner.ForeignerClass;
import org.marj4n.smooth_classes.content.foreigner.ForeignerContent;
import org.marj4n.smooth_classes.content.rider.RiderClass;
import org.marj4n.smooth_classes.content.rider.RiderContent;
import org.marj4n.smooth_classes.content.ruler.RulerClass;
import org.marj4n.smooth_classes.content.ruler.RulerContent;
import org.marj4n.smooth_classes.content.saber.SaberClass;
import org.marj4n.smooth_classes.content.saber.SaberContent;

/** Registers Smooth Classes Java-side content.
 * Puffish Skills owns the skill-tree/progression metadata and GUI.
 */
public final class SmoothClassContent {
    private SmoothClassContent() {}

    public static void register() {
        ClassRegistry.register(new AvengerClass());
        ClassRegistry.register(new ForeignerClass());
        ClassRegistry.register(new CasterClass());
        ClassRegistry.register(new BerserkerClass());
        ClassRegistry.register(new ArcherClass());
        ClassRegistry.register(new AssassinClass());
        ClassRegistry.register(new SaberClass());
        ClassRegistry.register(new RulerClass());
        ClassRegistry.register(new RiderClass());

        AvengerContent.register();
        ForeignerContent.register();
        CasterContent.register();
        BerserkerContent.register();
        ArcherContent.register();
        AssassinContent.register();
        SaberContent.register();
        RulerContent.register();
        RiderContent.register();
    }
}
