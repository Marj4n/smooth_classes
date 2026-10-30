package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.ai.goal.*;
import java.util.EnumSet;

/** Scheduler drives travel. Vanilla ranged/melee attack goals remain active during combat. */
public final class DemonCommandGoal extends Goal {
    private final MobEntity mob;
    public DemonCommandGoal(MobEntity mob){this.mob=mob;setControls(EnumSet.of(Control.MOVE));}
    @Override public boolean canStart(){
        var d=DemonEntityData.of(mob);if(d.commander==null||mob.getWorld().isClient||mob.getServer()==null)return false;
        var p=mob.getServer().getPlayerManager().getPlayer(d.commander);
        if(p==null||!p.isAlive()||!DemonProgression.demon(p)||!DemonProgression.has(p,"army")||p.getWorld()!=mob.getWorld()||!DemonMobIdentity.eligible(mob))return false;
        return d.command!=2&&(d.command!=3||mob.getTarget()==null);
    }
    @Override public boolean shouldContinue(){return canStart();}
    public interface Access { GoalSelector smooth_classes$demonGoals(); }
}
