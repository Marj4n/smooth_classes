package org.marj4n.smooth_classes.origin.demon;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.marj4n.smooth_classes.entity.DemonSeatEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;

/** Shared authoritative Lord body; ornaments do not enlarge collision. */
public final class DemonBodyGeometry {
    public static final float WIDTH=.8f,HEIGHT=3.5f,CROUCH_HEIGHT=2.75f,PRONE_HEIGHT=.9f,THRONE_HEIGHT=2.2f;
    public static final float RENDER_MULTIPLIER=16f/15f; // Cancel vanilla0.9375; actual body56/16 =3.5 blocks.
    private DemonBodyGeometry(){}
    public static boolean applies(PlayerEntity p){if(p==null)return false;var s=OriginRuntime.state(p);return s!=null&&s.origin()==OriginType.DEMON&&s.hasFlag("demon.lord");}
    public static EntityDimensions dimensions(PlayerEntity p,EntityPose pose){return EntityDimensions.changing(WIDTH,height(pose,p.getVehicle() instanceof DemonSeatEntity));}
    public static float height(EntityPose pose,boolean throne){
        if(throne)return THRONE_HEIGHT;
        return switch(pose){case CROUCHING->CROUCH_HEIGHT;case SWIMMING,FALL_FLYING,SPIN_ATTACK->PRONE_HEIGHT;case SLEEPING,DYING->.3f;default->HEIGHT;};
    }
    public static float eyeHeight(EntityPose pose,boolean throne){if(throne)return 1.9f;return switch(pose){case CROUCHING->2.48f;case SWIMMING,FALL_FLYING,SPIN_ATTACK->.65f;case SLEEPING,DYING->.15f;default->3.22f;};}
    private static boolean fits(PlayerEntity p,EntityPose pose){float h=height(pose,false);double r=WIDTH/2;return p.getWorld().isSpaceEmpty(p,new Box(p.getX()-r,p.getY(),p.getZ()-r,p.getX()+r,p.getY()+h,p.getZ()+r));}
    public static void refresh(PlayerEntity p){
        if(!applies(p))return;var expected=dimensions(p,p.getPose());var box=p.getBoundingBox();
        if(Math.abs(box.maxY-box.minY-expected.height)<.0005&&Math.abs(box.maxX-box.minX-expected.width)<.0005)return;
        // Use Minecraft's normal crouch/crawl poses when growth meets a ceiling.
        // Clients follow server-tracked poses; they do not choose remote-player poses.
        if(!p.getWorld().isClient&&!p.hasVehicle()&&!fits(p,p.getPose())){
            if(fits(p,EntityPose.CROUCHING))p.setPose(EntityPose.CROUCHING);
            else if(fits(p,EntityPose.SWIMMING))p.setPose(EntityPose.SWIMMING);
        }
        p.calculateDimensions();
    }
}
