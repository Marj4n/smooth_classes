package org.marj4n.smooth_classes.origin.demon;

/** Pure design rules shared by gameplay and deterministic tests. All durations use gameplay ticks. */
public final class DemonRules {
    public static final int CINDER = 666 * 20, BRAND = 30 * 20, DOMAIN = 45 * 20,
            DOMAIN_RECOVERY = 60 * 20, TRIBUTE = 180 * 20, THRONE = 900 * 20;
    public static final int DOMAIN_RADIUS = 64, COMMAND_RADIUS = 128, TRIBUTE_RADIUS = 24;
    public static final String[] COMMANDS = {"RALLY", "MARCH", "ATTACK", "GUARD", "WITHDRAW", "BACKUP"};
    public static final String[] BOSSES = {"nether_gauntlet", "decaying_king", "azazel", "ignis", "netherite_monstrosity"};
    private DemonRules() {}
    public record Cycle(int active, int cooldown) {}
    public static Cycle advance(int active, int cooldown) {
        if (active > 0) return active == 1 ? new Cycle(0, CINDER) : new Cycle(active - 1, 0);
        return new Cycle(0, Math.max(0, cooldown - 1));
    }
    public static int decayStage(int ticks) { return ticks >= 6000 ? 3 : ticks >= 4200 ? 2 : ticks >= 3000 ? 1 : 0; }
    public static int decay(int ticks, boolean nether, boolean lord) {
        int bounded=Math.max(0,Math.min(6000,ticks));
        return lord ? 0 : nether ? Math.max(0, bounded - 10) : Math.min(6000, bounded + 1);
    }
    public static boolean inSphere(double dx, double dy, double dz, double radius) { return dx*dx+dy*dy+dz*dz <= radius*radius; }
    public static int[] rotate(int x, int z, int rotation) {
        for (int i=0;i<Math.floorMod(rotation,4);i++) { int old=x; x=2-z; z=old; }
        return new int[]{x,z};
    }
    public static char throne(int x, int y, int z) {
        if (x<0||x>2||z<0||z>2||y<0||y>3) throw new IllegalArgumentException("Outside throne");
        if (y==0) return 'N';
        if (y==1) return z==2 || (z==1 && x!=1) ? 'N' : '.';
        if (y==2) return z==2 ? (x==1 ? 'B':'N') : '.';
        return z==2 && x!=1 ? 'N':'.';
    }
}
