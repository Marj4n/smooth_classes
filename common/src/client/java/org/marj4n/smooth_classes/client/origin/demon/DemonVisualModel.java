package org.marj4n.smooth_classes.client.origin.demon;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Arm;
import net.minecraft.client.render.entity.PlayerModelPart;
import org.marj4n.smooth_classes.entity.DemonSeatEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.marj4n.smooth_classes.origin.demon.DemonVisualStage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Renas rest-pose mesh buffers preserve exact face UVs and cube rotations. */
public final class DemonVisualModel {
    private static final Map<String, Definition> CACHE = new HashMap<>();
    private static final float DEG = (float)Math.PI / 180f;
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private record Part(String name,String bone,List<Vertex> vertices,float depthBias) {}
    private record Grip(String bone,float[] point) {}
    private record Bone(String name,String parent,float[] origin) {}
    private record Key(float time,float[] value) {}
    private record Channel(String bone,String channel,List<Key> keys) {}
    private record Clip(String name,float length,boolean loop,List<Channel> channels) {}
    private record Definition(Map<String,Bone> bones,List<Part> parts,List<Part> skinParts,List<Part> slimSkinParts,Map<String,List<Part>> armorParts,Map<String,Clip> clips,Identifier texture,Identifier emissive,Identifier horns,Identifier crown,Identifier crownEmissive,Map<String,Grip> grips,Map<String,Grip> slimGrips) {}
    private record Pose(Map<String,float[]> rotations,Map<String,float[]> positions) {}
    private static final Map<AbstractClientPlayerEntity,Pose> FRAME_POSES=new WeakHashMap<>();
    private static final Map<AbstractClientPlayerEntity,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion { boolean initialized,onGround; double dy; int landedAt=-1000,jumpedAt,transformedAt=-1000; boolean skull; }

    private DemonVisualModel() {}
    public static boolean applies(AbstractClientPlayerEntity player) {
        return player != null && OriginRuntime.state(player).origin() == OriginType.DEMON;
    }
    private static JsonObject resource(String name) {
        Identifier id = Identifier.of("smooth_classes", "demon_visual/"+name);
        try (var input=MinecraftClient.getInstance().getResourceManager().getResource(id).orElseThrow().getInputStream();
             var reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch(IOException e) { throw new IllegalStateException("Cannot load Demon visual "+id,e); }
    }
    private static float[] vector(JsonArray a) { return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()}; }
    private static Definition load(String name) {
        JsonObject data=resource(name+".mesh.json");Map<String,Bone> bones=new LinkedHashMap<>();
        for(JsonElement e:data.getAsJsonArray("bones")){var b=e.getAsJsonObject();String parent=b.get("parent").isJsonNull()?null:b.get("parent").getAsString();bones.put(b.get("name").getAsString(),new Bone(b.get("name").getAsString(),parent,vector(b.getAsJsonArray("origin"))));}
        List<Part> parts=new ArrayList<>();
        for(JsonElement e:data.getAsJsonArray("parts")){var p=e.getAsJsonObject();List<Vertex> vertices=new ArrayList<>();for(JsonElement v:p.getAsJsonArray("vertices")){var a=v.getAsJsonArray();vertices.add(new Vertex(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat(),a.get(7).getAsFloat()));}if(vertices.size()%4!=0)throw new IllegalArgumentException("Non-quad Demon mesh "+p.get("name"));parts.add(new Part(p.get("name").getAsString(),p.get("bone").getAsString(),List.copyOf(vertices),p.has("depth_bias")?p.get("depth_bias").getAsFloat():0));}
        Map<String,Clip> clips=new HashMap<>();for(JsonElement e:resource((name.equals("netherforged")?"goatborn":name.equals("demon_lord_skull")?"demon_lord":name)+".clips.json").getAsJsonArray("clips")){var p=e.getAsJsonObject();List<Channel> channels=new ArrayList<>();for(JsonElement el:p.getAsJsonArray("bones")){var b=el.getAsJsonObject();List<Key> keys=new ArrayList<>();for(JsonElement k:b.getAsJsonArray("keyframes")){var v=k.getAsJsonObject();keys.add(new Key(v.get("time").getAsFloat(),vector(v.getAsJsonArray("values"))));}channels.add(new Channel(b.get("bone").getAsString(),b.get("channel").getAsString(),List.copyOf(keys)));}clips.put(p.get("name").getAsString(),new Clip(p.get("name").getAsString(),p.get("length").getAsFloat(),p.get("loop").getAsString().equals("loop"),List.copyOf(channels)));}
        List<Part> skinParts=new ArrayList<>();
        if(data.has("skin_parts"))for(JsonElement e:data.getAsJsonArray("skin_parts")){var p=e.getAsJsonObject();List<Vertex> vertices=new ArrayList<>();for(JsonElement v:p.getAsJsonArray("vertices")){var a=v.getAsJsonArray();vertices.add(new Vertex(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat(),a.get(7).getAsFloat()));}skinParts.add(new Part(p.get("name").getAsString(),p.get("bone").getAsString(),List.copyOf(vertices),p.has("depth_bias")?p.get("depth_bias").getAsFloat():0));}
        List<Part> slimSkinParts=new ArrayList<>();
        if(data.has("skin_parts_slim"))for(JsonElement e:data.getAsJsonArray("skin_parts_slim")){var p=e.getAsJsonObject();List<Vertex> vertices=new ArrayList<>();for(JsonElement v:p.getAsJsonArray("vertices")){var a=v.getAsJsonArray();vertices.add(new Vertex(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat(),a.get(7).getAsFloat()));}slimSkinParts.add(new Part(p.get("name").getAsString(),p.get("bone").getAsString(),List.copyOf(vertices),0));}
        Map<String,List<Part>> armorParts=new HashMap<>();
        if(data.has("armor_parts"))for(JsonElement e:data.getAsJsonArray("armor_parts")){var p=e.getAsJsonObject();List<Vertex> vertices=new ArrayList<>();for(JsonElement v:p.getAsJsonArray("vertices")){var a=v.getAsJsonArray();vertices.add(new Vertex(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat(),a.get(7).getAsFloat()));}armorParts.computeIfAbsent(p.get("slot").getAsString(),k->new ArrayList<>()).add(new Part(p.get("name").getAsString(),p.get("bone").getAsString(),List.copyOf(vertices),p.has("depth_bias")?p.get("depth_bias").getAsFloat():0));}
        if(Boolean.getBoolean("smooth_classes.demonTest"))org.marj4n.smooth_classes.SmoothClasses.LOGGER.info("DEMON_VISUAL_LOAD {} parts={} skin={} armor={} code={}",name,parts.size(),skinParts.size(),armorParts.size(),DemonVisualModel.class.getProtectionDomain().getCodeSource().getLocation());
        return new Definition(Map.copyOf(bones),List.copyOf(parts),List.copyOf(skinParts),List.copyOf(slimSkinParts),Map.copyOf(armorParts),Map.copyOf(clips),Identifier.of("smooth_classes","textures/entity/origin/demon_visual/"+name+".png"),data.has("emissive")&&data.get("emissive").getAsBoolean()?Identifier.of("smooth_classes","textures/entity/origin/demon_visual/"+name+"_e.png"):null,data.has("horn_texture")?Identifier.of("smooth_classes","textures/entity/origin/demon_visual/"+name+"_horns.png"):null,data.has("crown_texture")?Identifier.of("smooth_classes","textures/entity/origin/demon_visual/"+name+"_crown.png"):null,data.has("crown_texture")?Identifier.of("smooth_classes","textures/entity/origin/demon_visual/"+name+"_crown_e.png"):null,grips(bones,parts,skinParts),grips(bones,parts,slimSkinParts.isEmpty()?skinParts:slimSkinParts));
    }
    private static Map<String,Grip> grips(Map<String,Bone> bones,List<Part> demon,List<Part> human){
        Map<String,Grip> result=new HashMap<>();
        for(String arm:List.of("left_arm","right_arm")){float lowest=Float.POSITIVE_INFINITY;Grip selected=null;
            for(List<Part> material:List.of(demon,human))for(Part part:material){
                if(!(part.name.contains("arm_block")||part.name.contains("upper_arm")||part.name.contains("forearm")||part.name.contains("arm_extension")||part.name.contains("arm_midpanel")))continue;
                boolean belongs=false;for(Bone b=bones.get(part.bone);b!=null;b=b.parent==null?null:bones.get(b.parent))if(b.name.equals(arm)){belongs=true;break;}if(!belongs)continue;
                float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,minY=Float.POSITIVE_INFINITY,minZ=Float.POSITIVE_INFINITY;
                for(Vertex v:part.vertices){minX=Math.min(minX,v.x);maxX=Math.max(maxX,v.x);minY=Math.min(minY,v.y);minZ=Math.min(minZ,v.z);}
                if(minY<lowest){lowest=minY;selected=new Grip(part.bone,new float[]{(minX+maxX)/2,minY,minZ});}
            }if(selected!=null)result.put(arm,selected);
        }return Map.copyOf(result);
    }
    private static Definition definition(AbstractClientPlayerEntity p){String name=DemonVisualStage.of(OriginRuntime.state(p)).asset();if(name.equals("demon_lord")&&DemonStreakClient.stacks(p.getUuid())>0)name="demon_lord_skull";return CACHE.computeIfAbsent(name,DemonVisualModel::load);}
    public static void invalidate(){CACHE.clear();FRAME_POSES.clear();MOTION.clear();}
    public static void tick(AbstractClientPlayerEntity p){
        if(!applies(p))return;Motion m=MOTION.computeIfAbsent(p,k->new Motion());
        if(m.initialized&&!m.onGround&&p.isOnGround()&&m.dy<-.01&&!p.isTouchingWater())m.landedAt=p.age;
        if(m.initialized&&m.onGround&&!p.isOnGround())m.jumpedAt=p.age;
        m.initialized=true;m.onGround=p.isOnGround();m.dy=p.getVelocity().y;
    }
    private static float[] sample(Channel c,float time){List<Key> keys=c.keys;if(time<=keys.get(0).time)return keys.get(0).value;if(time>=keys.get(keys.size()-1).time)return keys.get(keys.size()-1).value;for(int i=1;i<keys.size();i++){Key a=keys.get(i-1),b=keys.get(i);if(time<=b.time){float f=(time-a.time)/(b.time-a.time);return new float[]{a.value[0]+f*(b.value[0]-a.value[0]),a.value[1]+f*(b.value[1]-a.value[1]),a.value[2]+f*(b.value[2]-a.value[2])};}}return new float[3];}
    private static void apply(Definition d,Map<String,float[]> rotation,Map<String,float[]> position,String name,float time){Clip clip=d.clips.get(name);if(clip==null)return;float t=clip.loop?time%clip.length:Math.min(time,clip.length);for(Channel c:clip.channels){float[] v=sample(c,t);Map<String,float[]> out=c.channel.equals("position")?position:rotation;float[] target=out.computeIfAbsent(c.bone,k->new float[3]);for(int i=0;i<3;i++)target[i]+=v[i];}}
    private static void transforms(MatrixStack matrices,Definition d,Bone b,Map<String,float[]> rotations,Map<String,float[]> positions){if(b.parent!=null)transforms(matrices,d,d.bones.get(b.parent),rotations,positions);float[] parent=b.parent==null?new float[3]:d.bones.get(b.parent).origin;float[] pos=positions.getOrDefault(b.name,new float[3]);matrices.translate((b.origin[0]-parent[0]+pos[0])/16f,(b.origin[1]-parent[1]+pos[1])/16f,(b.origin[2]-parent[2]+pos[2])/16f);float[] r=rotations.getOrDefault(b.name,new float[3]);matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(r[2]));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r[1]));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(r[0]));}
    public static void render(AbstractClientPlayerEntity p,PlayerEntityModel<?> parent,MatrixStack matrices,VertexConsumerProvider consumers,int light,float limbAngle,float limbDistance,float age,float yaw,float pitch){render(p,parent,matrices,consumers,light,limbAngle,limbDistance,age,yaw,pitch,null);}
    public static void renderArm(AbstractClientPlayerEntity p,PlayerEntityModel<?> parent,MatrixStack matrices,VertexConsumerProvider consumers,int light,boolean left){render(p,parent,matrices,consumers,light,0,0,p.age,0,0,left?"left_arm":"right_arm");}
    public static void prepare(AbstractClientPlayerEntity p,PlayerEntityModel<?> parent,float limbAngle,float limbDistance,float age,float yaw,float pitch){
        if(!applies(p))return;Definition d=definition(p);Map<String,float[]> r=new HashMap<>(),pos=new HashMap<>();
        Motion motion=MOTION.computeIfAbsent(p,k->new Motion());
        boolean seated=p.getVehicle() instanceof DemonSeatEntity&&DemonVisualStage.of(OriginRuntime.state(p))==DemonVisualStage.DEMON_LORD;
        String clip=p.deathTime>0?"death":p.hurtTime>0?"hurt":seated?"throne_sit":p.hasVehicle()?"ride":p.getPose()==EntityPose.SWIMMING?"swim":!p.isOnGround()?"jump":p.isSneaking()?"crouch":age-motion.landedAt<5?"landing":p.isSprinting()&&limbDistance>.05f?"run":limbDistance>.03f?"walk":"idle";
        boolean poseProbe=Boolean.getBoolean("smooth_classes.demonTest")&&OriginRuntime.state(p).hasFlag("demon.debug.walk");
        if(poseProbe)clip="walk";
        float time=clip.equals("walk")?limbAngle*.16f:clip.equals("run")?limbAngle*.11f:clip.equals("hurt")?(10-p.hurtTime)/20f:clip.equals("death")?p.deathTime/20f:clip.equals("landing")?(age-motion.landedAt)/20f:clip.equals("jump")?(age-motion.jumpedAt)/20f:age/20f;apply(d,r,pos,clip,time);apply(d,r,pos,"tail",age/20f);apply(d,r,pos,"crown",age/20f);
        if(poseProbe){r.clear();pos.clear();apply(d,r,pos,"walk",age/20f);apply(d,r,pos,"tail",age/20f);apply(d,r,pos,"crown",age/20f);}
        apply(d,r,pos,"ears",age/20f);
        boolean skull=DemonVisualStage.of(OriginRuntime.state(p))==DemonVisualStage.DEMON_LORD&&DemonStreakClient.stacks(p.getUuid())>0;
        if(motion.skull!=skull){motion.skull=skull;motion.transformedAt=p.age;}
        if(age-motion.transformedAt<7)apply(d,r,pos,skull?"transform_in":"transform_out",(age-motion.transformedAt)/20f);
        // Preserve current vanilla/Player Animator weapon poses without replacing item rendering.
        if(p.handSwingProgress>0||p.isUsingItem()||!p.getMainHandStack().isEmpty()||!p.getOffHandStack().isEmpty()){
            r.put("left_arm",new float[]{-parent.leftArm.pitch/DEG,parent.leftArm.yaw/DEG,-parent.leftArm.roll/DEG});r.put("right_arm",new float[]{-parent.rightArm.pitch/DEG,parent.rightArm.yaw/DEG,-parent.rightArm.roll/DEG});
            pos.put("left_arm",new float[]{parent.leftArm.pivotX-5,2-parent.leftArm.pivotY,parent.leftArm.pivotZ});pos.put("right_arm",new float[]{parent.rightArm.pivotX+5,2-parent.rightArm.pivotY,parent.rightArm.pivotZ});
        }
        float[] head=r.computeIfAbsent("head",k->new float[3]);head[0]-=pitch;head[1]+=yaw;
        head[0]+=ForbiddenBirthClient.headBow(p.getUuid(),age-p.age);
        FRAME_POSES.put(p,new Pose(r,pos));
    }
    private static void render(AbstractClientPlayerEntity p,PlayerEntityModel<?> parent,MatrixStack matrices,VertexConsumerProvider consumers,int light,float limbAngle,float limbDistance,float age,float yaw,float pitch,String arm){
        if(!applies(p)||p.isInvisible()||p.isSpectator())return;
        if(Boolean.getBoolean("smooth_classes.demonTest")&&OriginRuntime.state(p).hasFlag("demon.debug.no_mesh"))return;
        prepare(p,parent,limbAngle,limbDistance,age,yaw,pitch);Definition d=definition(p);Pose pose=FRAME_POSES.get(p);var r=pose.rotations;var pos=pose.positions;
        matrices.push();matrices.translate(0,1.5,0);matrices.scale(1,-1,1);VertexConsumer buffer=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(d.texture));
        for(Part part:d.parts){if(d.horns!=null&&horn(part)||d.crown!=null&&crown(part))continue;if(arm!=null&&!descends(d,part.bone,arm))continue;
            drawPart(part,d,matrices,buffer,light,r,pos);
        }
        if(arm==null&&d.horns!=null){VertexConsumer hornBuffer=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(d.horns));for(Part part:d.parts)if(horn(part))drawPart(part,d,matrices,hornBuffer,light,r,pos);}
        if(arm==null&&d.crown!=null){VertexConsumer crownBuffer=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(d.crown));for(Part part:d.parts)if(crown(part))drawPart(part,d,matrices,crownBuffer,light,r,pos);}
        if(!d.skinParts.isEmpty()&&!(Boolean.getBoolean("smooth_classes.demonTest")&&OriginRuntime.state(p).hasFlag("demon.debug.no_skin"))){
            VertexConsumer skin=consumers.getBuffer(RenderLayer.getEntityTranslucent(p.getSkinTexture()));
            boolean one=Boolean.getBoolean("smooth_classes.demonTest")&&OriginRuntime.state(p).hasFlag("demon.debug.skin_one");
            List<Part> playerParts=p.getModel().equals("slim")&&!d.slimSkinParts.isEmpty()?d.slimSkinParts:d.skinParts;
            for(Part part:one?playerParts.subList(0,1):playerParts){if(arm!=null&&!descends(d,part.bone,arm))continue;if(part.name.equals("player_hat")&&!p.isPartVisible(PlayerModelPart.HAT)||part.name.equals("player_jacket")&&!p.isPartVisible(PlayerModelPart.JACKET)||part.name.equals("left_sleeve")&&!p.isPartVisible(PlayerModelPart.LEFT_SLEEVE)||part.name.equals("right_sleeve")&&!p.isPartVisible(PlayerModelPart.RIGHT_SLEEVE))continue;drawPart(part,d,matrices,skin,light,r,pos);}
        }
        if(d.emissive!=null){VertexConsumer glow=consumers.getBuffer(RenderLayer.getEyes(d.emissive));float intensity=(OriginRuntime.state(p).progress("demon.domain_active")>0||DemonStreakClient.stacks(p.getUuid())>0)?1f:.72f+.12f*(float)Math.sin(age*.08f);for(Part part:d.parts){if(arm!=null&&!descends(d,part.bone,arm))continue;drawPart(part,d,matrices,glow,0xF000F0,OverlayTexture.DEFAULT_UV,intensity,intensity,intensity,1,r,pos);}}
        if(arm==null&&d.crownEmissive!=null){VertexConsumer crownGlow=consumers.getBuffer(RenderLayer.getEyes(d.crownEmissive));float intensity=DemonStreakClient.crownIntensity(p.getUuid(),age-p.age);for(Part part:d.parts)if(crown(part))drawPart(part,d,matrices,crownGlow,0xF000F0,OverlayTexture.DEFAULT_UV,intensity,intensity,intensity,1,r,pos);}
        matrices.pop();
    }
    private static boolean horn(Part p){int i=p.name.lastIndexOf('_')+1;return (p.name.startsWith("left_horn_")||p.name.startsWith("right_horn_"))&&i<p.name.length()&&Character.isDigit(p.name.charAt(i));}
    private static boolean crown(Part p){return p.name.startsWith("crown_");}
    /** Use the exact same current-frame shoulder hierarchy as skin and armor. */
    public static boolean applyHeldArm(AbstractClientPlayerEntity p,Arm arm,MatrixStack matrices){
        Pose pose=FRAME_POSES.get(p);if(!applies(p)||pose==null)return false;Definition d=definition(p);Map<String,Grip> grips=p.getModel().equals("slim")?d.slimGrips:d.grips;Grip grip=grips.get(arm==Arm.LEFT?"left_arm":"right_arm");if(grip==null)return false;Bone b=d.bones.get(grip.bone);if(b==null)return false;
        matrices.translate(0,1.5,0);matrices.scale(1,-1,1);transforms(matrices,d,b,pose.rotations,pose.positions);matrices.scale(1,-1,1);
        var offset=org.marj4n.smooth_classes.origin.demon.DemonGripGeometry.offset(grip.point,b.origin,arm==Arm.LEFT);
        matrices.translate(offset.x()/16,offset.y()/16,offset.z()/16);
        return true;
    }
    /** Receives the same vanilla armor consumer for base, dye, trim and glint passes. */
    public static boolean renderArmor(AbstractClientPlayerEntity p,EquipmentSlot slot,MatrixStack matrices,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha){
        if(!applies(p))return false;Definition d=definition(p);List<Part> parts=d.armorParts.get(slot.getName());Pose pose=FRAME_POSES.get(p);if(parts==null||pose==null)return false;
        matrices.push();matrices.translate(0,1.5,0);matrices.scale(1,-1,1);
        for(Part part:parts)drawPart(part,d,matrices,buffer,light,overlay,red,green,blue,alpha,pose.rotations,pose.positions);
        matrices.pop();return true;
    }
    private static boolean descends(Definition d,String name,String parent){for(Bone b=d.bones.get(name);b!=null;b=b.parent==null?null:d.bones.get(b.parent))if(b.name.equals(parent))return true;return false;}
    private static void drawPart(Part part,Definition d,MatrixStack matrices,VertexConsumer buffer,int light,Map<String,float[]> r,Map<String,float[]> pos){drawPart(part,d,matrices,buffer,light,OverlayTexture.DEFAULT_UV,1,1,1,1,r,pos);}
    private static void drawPart(Part part,Definition d,MatrixStack matrices,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha,Map<String,float[]> r,Map<String,float[]> pos){Bone b=d.bones.get(part.bone);matrices.push();transforms(matrices,d,b,r,pos);var entry=matrices.peek();for(Vertex v:part.vertices)buffer.vertex(entry.getPositionMatrix(),(v.x+v.nx*part.depthBias-b.origin[0])/16f,(v.y+v.ny*part.depthBias-b.origin[1])/16f,(v.z+v.nz*part.depthBias-b.origin[2])/16f).color(red,green,blue,alpha).texture(v.u,v.v).overlay(overlay).light(light).normal(entry.getNormalMatrix(),v.nx,v.ny,v.nz).next();matrices.pop();}
}
