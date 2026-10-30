package org.marj4n.smooth_classes.client.origin.demon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.demon.DemonVisualStage;
import org.marj4n.smooth_classes.origin.demon.DemonBodyGeometry;
/** Tick-based cosmetic sparks, bounded globally per client; never a persistent entity. */
public final class DemonVisualEffects {
    private static ClientWorld previousWorld;
    private DemonVisualEffects(){}
    public static void register(){net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(DemonVisualEffects::tick);}
    private static void tick(MinecraftClient client){
        if(client.world!=previousWorld){previousWorld=client.world;DemonVisualModel.invalidate();}
        if(client.world==null)return;int emitted=0;
        for(AbstractClientPlayerEntity p:client.world.getPlayers()){
            DemonVisualModel.tick(p);DemonBodyGeometry.refresh(p);
            if(!DemonVisualModel.applies(p)||p.isInvisible()||p.isSpectator()||client.player==null||p.squaredDistanceTo(client.player)>4096||emitted>=8)continue;
            var stage=DemonVisualStage.of(OriginRuntime.state(p));if(stage!=DemonVisualStage.DEMON_LORD)continue;
            String detail=client.options.getParticles().getValue().name();if(detail.equals("MINIMAL"))continue;
            boolean active=OriginRuntime.state(p).progress("demon.domain_active")>0||DemonStreakClient.stacks(p.getUuid())>0;int interval=(active?5:20)*(detail.equals("DECREASED")?2:1);
            if(p.age%interval!=0)continue;int count=active?2:1;
            for(int i=0;i<count&&emitted<8;i++,emitted++){
                double x=p.getX()+(p.getRandom().nextDouble()-.5)*.55,z=p.getZ()+(p.getRandom().nextDouble()-.5)*.55;
                double y=stage==DemonVisualStage.DEMON_LORD?p.getY()+p.getHeight()+.45:p.getBodyY(.6);
                client.world.addParticle(ParticleTypes.SMALL_FLAME,x,y,z,0,.006,0);
            }
        }
    }
}
