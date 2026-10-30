package org.marj4n.smooth_classes.origin.demon;

/** Transient session state. Time is authoritative simulation ticks, never client time. */
public final class DemonStreakCounter {
    public static final int TIMEOUT_TICKS=66*20;
    private long stacks;
    private long expiresAt;
    public long stacks(){return stacks;}
    public int remaining(long now){return stacks==0?0:(int)Math.max(0,Math.min(TIMEOUT_TICKS,expiresAt-now));}
    public void kill(long now){
        if(remaining(now)==0)clear();
        // Numeric representation guard only; no reachable gameplay stack cap.
        if(stacks<Long.MAX_VALUE)stacks++;
        expiresAt=now>Long.MAX_VALUE-TIMEOUT_TICKS?Long.MAX_VALUE:now+TIMEOUT_TICKS;
    }
    public boolean expire(long now){if(stacks>0&&remaining(now)==0){clear();return true;}return false;}
    public double healthBonus(){return (double)stacks;}
    public double armorBonus(){return stacks*.5d;}
    public void clear(){stacks=0;expiresAt=0;}
}
