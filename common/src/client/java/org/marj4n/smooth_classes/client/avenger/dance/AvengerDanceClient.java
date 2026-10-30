package org.marj4n.smooth_classes.client.avenger.dance;

import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.*;
import org.marj4n.smooth_classes.content.avenger.dance.AvengerDanceRuntime;

public final class AvengerDanceClient {
    public record Playback(int id,UUID uuid,String dimension,String profile,BlockPos box,long start,long end,int disc){}
    private static final Map<Integer,Playback> ACTIVE=new HashMap<>();
    private static final Map<Integer,Long> RECEIVED=new HashMap<>();
    private static final Map<Integer,Long> STARTED=new HashMap<>(),STOPPING=new HashMap<>();
    private static final class WaterBlend { float previous,current; }
    private static final Map<Integer,WaterBlend> WATER=new HashMap<>();
    private static final Map<String,DanceClip> CLIPS=new HashMap<>();
    private static final Map<EntityModel<?>,Pose> POSES=new WeakHashMap<>();
    private static net.minecraft.client.world.ClientWorld world;
    public static long renderNanos,renderCalls;public static int restoreErrors,restoreChecks;
    public static final Map<UUID,Map<String,Object>> probes=new LinkedHashMap<>();
    public static final Map<UUID,Map<String,Object>> poseDetails=new LinkedHashMap<>();
    private static final class Pose {
        final DanceRig rig;final List<ModelPart> touched=new ArrayList<>();float[] saved;boolean[] visible;boolean applied,layer;
        final org.joml.Quaternionf rotation=new org.joml.Quaternionf();
        final Map<DanceClip,ModelPart[]> bindings=new IdentityHashMap<>();final Set<DanceClip> unsupported=Collections.newSetFromMap(new IdentityHashMap<>());
        Pose(EntityModel<?> model){rig=new DanceRig(model);touched.addAll(new LinkedHashSet<>(rig.parts.values()));saved=new float[touched.size()*9];visible=new boolean[touched.size()];}
        ModelPart[] bind(DanceClip clip){if(unsupported.contains(clip))return null;var old=bindings.get(clip);if(old!=null)return old;var mapped=new ModelPart[clip.tracks.size()];for(int i=0;i<mapped.length;i++){var t=clip.tracks.get(i);if(t.bone().equals("__root")||t.bone().equals("__cat_right_elbow")&&rig.part("body_3")!=null&&rig.part("body_6")!=null)continue;mapped[i]=rig.part(t.bone());if(mapped[i]==null&&!clip.generic){unsupported.add(clip);return null;}}bindings.put(clip,mapped);return mapped;}
        void snapshot(){for(int i=0;i<touched.size();i++){var p=touched.get(i);int n=i*9;saved[n]=p.pitch;saved[n+1]=p.yaw;saved[n+2]=p.roll;saved[n+3]=p.pivotX;saved[n+4]=p.pivotY;saved[n+5]=p.pivotZ;saved[n+6]=p.xScale;saved[n+7]=p.yScale;saved[n+8]=p.zScale;visible[i]=p.visible;}applied=true;}
        void restore(){if(!applied)return;CatForepawJoint.clear(rig.part("body_3"));for(int i=0;i<touched.size();i++){var p=touched.get(i);int n=i*9;p.pitch=saved[n];p.yaw=saved[n+1];p.roll=saved[n+2];p.pivotX=saved[n+3];p.pivotY=saved[n+4];p.pivotZ=saved[n+5];p.xScale=saved[n+6];p.yScale=saved[n+7];p.zScale=saved[n+8];p.visible=visible[i];}if(org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ENABLED&&DanceDiagnostics.benchmark==null){restoreChecks++;for(int i=0;i<touched.size();i++){var p=touched.get(i);int n=i*9;if(p.pitch!=saved[n]||p.yaw!=saved[n+1]||p.roll!=saved[n+2]||p.pivotX!=saved[n+3]||p.pivotY!=saved[n+4]||p.pivotZ!=saved[n+5]||p.xScale!=saved[n+6]||p.yScale!=saved[n+7]||p.zScale!=saved[n+8]||p.visible!=visible[i])restoreErrors++;}}applied=false;layer=false;}
    }
    public static void register(){
        DanceDiagnostics.register();
        ClientPlayNetworking.registerGlobalReceiver(AvengerDanceRuntime.SYNC,(c,h,b,response)->{
            String dim=b.readString(128);int id=b.readVarInt();UUID uuid=b.readUuid();boolean on=b.readBoolean();Playback p=on?new Playback(id,uuid,dim,b.readString(64),b.readBlockPos(),b.readLong(),b.readLong(),b.readVarInt()):null;boolean hard=!on&&b.readBoolean();
            c.execute(()->{if(c.world==null||!c.world.getRegistryKey().getValue().toString().equals(dim))return;if(world!=c.world)clear(c.world);if(on){boolean fresh=!ACTIVE.containsKey(id);ACTIVE.put(id,p);RECEIVED.put(id,c.world.getTime());if(fresh)STARTED.put(id,c.world.getTime());STOPPING.remove(id);}else{var old=ACTIVE.get(id);if(old!=null&&old.uuid().equals(uuid)){if(hard)ACTIVE.remove(id);else STOPPING.put(id,c.world.getTime());}}});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->c.execute(()->clear(null)));
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(world!=c.world)clear(c.world);if(c.world!=null)ACTIVE.values().removeIf(p->{var e=c.world.getEntityById(p.id());return c.world.getTime()>=p.end()+6||STOPPING.containsKey(p.id())&&c.world.getTime()-STOPPING.get(p.id())>=6||e!=null&&!e.getUuid().equals(p.uuid())||e==null&&c.world.getTime()-RECEIVED.getOrDefault(p.id(),0L)>40;});RECEIVED.keySet().retainAll(ACTIVE.keySet());STARTED.keySet().retainAll(ACTIVE.keySet());STOPPING.keySet().retainAll(ACTIVE.keySet());WATER.keySet().retainAll(ACTIVE.keySet());if(c.world!=null)for(var p:ACTIVE.values())if(p.profile().equals("drowned")){var entity=c.world.getEntityById(p.id());var b=WATER.computeIfAbsent(p.id(),id->new WaterBlend());b.previous=b.current;float target=entity!=null&&entity.isSubmergedInWater()?1:0;b.current+=MathHelper.clamp(target-b.current,-.125f,.125f);}DanceDiagnostics.tick(c);
            if(c.world!=null&&c.player!=null&&c.world.getTime()%20==0){int particles=0;for(var p:ACTIVE.values()){if(particles>=8)break;var e=c.world.getEntityById(p.id());if(!(e instanceof LivingEntity living)||playback(living)==null||e.squaredDistanceTo(c.player)>24*24)continue;
                if(p.profile().equals("witch")){c.world.addParticle(net.minecraft.particle.ParticleTypes.ENTITY_EFFECT,e.getX(),e.getBodyY(.7),e.getZ(),.3,.1,.6);particles++;}
                else if(p.profile().equals("enderman")&&c.world.getTime()%40==0){c.world.addParticle(net.minecraft.particle.ParticleTypes.PORTAL,e.getX(),e.getBodyY(.4),e.getZ(),.01,0,.01);particles++;}
            }}});
    }
    private static void clear(net.minecraft.client.world.ClientWorld w){for(var p:POSES.values())p.restore();POSES.clear();ACTIVE.clear();RECEIVED.clear();STARTED.clear();STOPPING.clear();WATER.clear();probes.clear();poseDetails.clear();world=w;}
    public static Playback playback(LivingEntity e){var p=ACTIVE.get(e.getId());return e.getWorld()==world&&p!=null&&p.uuid().equals(e.getUuid())&&e.isAlive()&&e.hurtTime==0&&!org.marj4n.smooth_classes.content.avenger.dance.DancePriority.special(e)&&e.getWorld().getTime()<p.end()+6&&e.getPos().squaredDistanceTo(Vec3d.ofCenter(p.box()))<=256?p:null;}
    public static Collection<Playback> playbacks(){return List.copyOf(ACTIVE.values());}
    public static float phase(long now,float delta,long start){return (float)Math.floorMod(now-start,40)/40F+delta/40F;}
    public static float phase(Playback p,long now,float delta){return CLIPS.computeIfAbsent(p.profile(),DanceClip::load).phase(now,delta,p.start());}
    public static boolean apply(EntityModel<?> model,LivingEntity e,float delta,MatrixStack matrices){
        return applyInternal(model,e,delta,matrices,true);
    }
    public static boolean applyLayer(EntityModel<?> model,LivingEntity e,float delta,MatrixStack matrices){return applyInternal(model,e,delta,matrices,false);}
    public static void finishLayer(EntityModel<?> model,MatrixStack matrices){var pose=POSES.get(model);if(pose!=null&&pose.applied&&pose.layer){pose.restore();matrices.pop();}}
    private static boolean applyInternal(EntityModel<?> model,LivingEntity e,float delta,MatrixStack matrices,boolean includeRoot){
        var p=playback(e);
        if(includeRoot&&org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ENABLED&&DanceDiagnostics.benchmark==null&&probes.size()<1000)probes.put(e.getUuid(),Map.of("model_instance",System.identityHashCode(model),"overlay",p!=null,"applied",false,"type",net.minecraft.registry.Registries.ENTITY_TYPE.getId(e.getType()).toString(),"tick",e.getWorld().getTime()));
        if(p==null)return false;long begin=System.nanoTime();
        var pose=POSES.computeIfAbsent(model,Pose::new);if(!includeRoot&&pose.applied)return false;pose.restore();if(pose.rig.parts.isEmpty())return false;
        var clip=CLIPS.computeIfAbsent(p.profile(),DanceClip::load);if(clip.tracks.isEmpty())return false;
        var mapped=pose.bind(clip);if(mapped==null)return false;
        float phase=clip.phase(e.getWorld().getTime(),delta,p.start());
        float elapsed=e.getWorld().getTime()+delta-STARTED.getOrDefault(e.getId(),e.getWorld().getTime());
        float weight=clip.transitionTicks<=0?1:MathHelper.clamp(elapsed/clip.transitionTicks,0,1);
        Long stopping=STOPPING.get(e.getId());if(stopping!=null)weight*=MathHelper.clamp(1-(e.getWorld().getTime()+delta-stopping)/6f,0,1);
        weight*=MathHelper.clamp((p.end()+6-e.getWorld().getTime()-delta)/6f,0,1);
        float amplitude=clip.amplitude*weight;
        DanceClip variant=p.profile().equals("drowned")?CLIPS.computeIfAbsent("drowned_water",DanceClip::load):null;
        var wet=WATER.get(e.getId());float wetWeight=wet==null?0:MathHelper.lerp(delta,wet.previous,wet.current);
        pose.snapshot();pose.layer=!includeRoot;matrices.push();
        // Same T * R * S order as the authored root; translation must not rotate with the body.
        if(includeRoot){
            for(int i=0;i<clip.tracks.size();i++){var track=clip.tracks.get(i);if(track.bone().equals("__root")&&track.channel().equals("position"))matrices.translate(sample(clip,variant,i,phase,0,wetWeight)*amplitude/16,sample(clip,variant,i,phase,1,wetWeight)*amplitude/16,sample(clip,variant,i,phase,2,wetWeight)*amplitude/16);}
            for(int i=0;i<clip.tracks.size();i++){var track=clip.tracks.get(i);if(track.bone().equals("__root")&&track.channel().equals("rotation"))matrices.multiply(pose.rotation.rotationZYX(sample(clip,variant,i,phase,2,wetWeight)*amplitude*MathHelper.RADIANS_PER_DEGREE,sample(clip,variant,i,phase,1,wetWeight)*amplitude*MathHelper.RADIANS_PER_DEGREE,sample(clip,variant,i,phase,0,wetWeight)*amplitude*MathHelper.RADIANS_PER_DEGREE));}
            for(int i=0;i<clip.tracks.size();i++){var track=clip.tracks.get(i);if(track.bone().equals("__root")&&track.channel().equals("scale"))matrices.scale(1+(sample(clip,variant,i,phase,0,wetWeight)-1)*amplitude,1+(sample(clip,variant,i,phase,1,wetWeight)-1)*amplitude,1+(sample(clip,variant,i,phase,2,wetWeight)-1)*amplitude);}
        }
        // Vanilla illager folded-arms and free-arms are alternative existing meshes, never new geometry.
        if(e instanceof net.minecraft.entity.mob.IllagerEntity&&(p.profile().equals("pillager")||p.profile().equals("vindicator")||p.profile().equals("evoker"))){var folded=pose.rig.part("arms");var right=pose.rig.part("right_arm");var left=pose.rig.part("left_arm");if(folded!=null&&right!=null&&left!=null){folded.visible=false;right.visible=true;left.visible=true;}}
        for(int i=0;i<clip.tracks.size();i++){var track=clip.tracks.get(i);if(track.bone().equals("__root"))continue;float x=sample(clip,variant,i,phase,0,wetWeight)*amplitude,y=sample(clip,variant,i,phase,1,wetWeight)*amplitude,z=sample(clip,variant,i,phase,2,wetWeight)*amplitude;
            var part=mapped[i];if(part==null)continue; // audited generic AnimalModel fallback: whole-rig motion only
            if(track.channel().equals("position")){part.pivotX+=x;part.pivotY+=y;part.pivotZ+=z;}else{part.pitch+=x*MathHelper.RADIANS_PER_DEGREE;part.yaw+=y*MathHelper.RADIANS_PER_DEGREE;part.roll+=z*MathHelper.RADIANS_PER_DEGREE;}
        }
        if(p.profile().equals("cat"))for(var track:clip.tracks)if(track.bone().equals("__cat_right_elbow")){CatForepawJoint.pose(pose.rig,amplitude);break;}
        if(model instanceof net.minecraft.client.render.entity.model.PlayerEntityModel<?> player){player.rightSleeve.copyTransform(player.rightArm);player.leftSleeve.copyTransform(player.leftArm);player.rightPants.copyTransform(player.rightLeg);player.leftPants.copyTransform(player.leftLeg);player.jacket.copyTransform(player.body);}
        if(model instanceof net.minecraft.client.render.entity.model.ShulkerEntityModel<?> shulker)shulker.getLid().pivotY=Math.max(8,shulker.getLid().pivotY);
        if(p.profile().equals("creeper")&&phase<.035f)for(var part:pose.touched){part.pitch+=.012f;} // cosmetic tremble; never touches fuse.
        if(includeRoot&&org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ENABLED&&DanceDiagnostics.benchmark==null){
            var details=new LinkedHashMap<String,Object>();details.put("profile",p.profile());if(p.profile().equals("cat"))details.put("right_paw_mouth_error_model_units",CatForepawJoint.contactError);details.put("phase",phase);details.put("pose",e.getPose().toString());details.put("mapped_tracks",mapped.length);details.put("water_blend",wetWeight);details.put("generic_fallback",clip.generic);
            if(e instanceof net.minecraft.entity.boss.WitherEntity){var heads=new LinkedHashMap<String,Object>();for(String name:List.of("center_head","right_head","left_head")){var part=pose.rig.part(name);if(part!=null)heads.put(name,List.of(part.pitch,part.yaw,part.roll));}details.put("three_heads",heads);}
            poseDetails.put(e.getUuid(),details);
        }
        renderCalls++;renderNanos+=System.nanoTime()-begin;
        if(includeRoot&&org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ENABLED&&DanceDiagnostics.benchmark==null&&probes.size()<1000)probes.put(e.getUuid(),Map.of("model_instance",System.identityHashCode(model),"overlay",true,"applied",true,"submerged",e.isSubmergedInWater(),"type",net.minecraft.registry.Registries.ENTITY_TYPE.getId(e.getType()).toString(),"tick",e.getWorld().getTime()));
        return true;
    }
    private static float sample(DanceClip base,DanceClip variant,int i,float phase,int axis,float mix){
        var a=base.tracks.get(i);float value=a.sample(phase,axis);if(variant==null||mix<=0||i>=variant.tracks.size())return value;var b=variant.tracks.get(i);if(!a.bone().equals(b.bone())||!a.channel().equals(b.channel()))return value;return MathHelper.lerp(mix,value,b.sample(phase,axis));
    }
    public static void restore(EntityModel<?> m){var p=POSES.get(m);if(p!=null)p.restore();}
    private AvengerDanceClient(){}
}
