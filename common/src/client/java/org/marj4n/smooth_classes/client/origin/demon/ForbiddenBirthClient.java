package org.marj4n.smooth_classes.client.origin.demon;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.demon.*;

/** Presentation only. No world/block/item entity or player UUID is created. */
public final class ForbiddenBirthClient {
    public record Scene(UUID player,UUID token,long start,Vec3d anchor,Vec3d fruit,Vec3d landing,int received){}
    public record CameraPose(Vec3d position,Vec3d target,double fov){}
    public static final class Settings {public boolean reducedCameraMotion=false;}
    private static final Map<UUID,Scene> SCENES=new HashMap<>();
    private static final Set<UUID> READY_SENT=new HashSet<>();
    private static int ticks,returnFade;private static double previousOwnTime=-1;
    private static Settings settings=new Settings();private static final ItemStack FRUIT=new ItemStack(ForbiddenBirth.FRUIT);
    private ForbiddenBirthClient(){}
    private static Vec3d vector(PacketByteBuf b){return new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());}
    private static Path config(){return org.marj4n.smooth_classes.platform.PlatformLoader.getInstance().getConfigDir().resolve("smooth_classes-forbidden-birth-client.json");}
    public static void register(){
        try{if(Files.exists(config())){var read=new Gson().fromJson(Files.readString(config()),Settings.class);if(read!=null)settings=read;}}catch(Exception ex){org.marj4n.smooth_classes.SmoothClasses.LOGGER.warn("Using safe birth camera defaults after client config error",ex);}
        ClientPlayNetworking.registerGlobalReceiver(ForbiddenBirth.START,(client,handler,b,sender)->{UUID player=b.readUuid(),token=b.readUuid();long start=b.readLong();Vec3d anchor=vector(b),fruit=vector(b),landing=vector(b);client.execute(()->{SCENES.put(player,new Scene(player,token,start,anchor,fruit,landing,ticks));if(client.player!=null&&client.player.getUuid().equals(player)){previousOwnTime=-1;if(!(client.currentScreen instanceof BirthScreen))client.setScreen(new BirthScreen());}});});
        ClientPlayNetworking.registerGlobalReceiver(ForbiddenBirth.END,(client,handler,b,sender)->{UUID player=b.readUuid(),token=b.readUuid();client.execute(()->{var old=SCENES.get(player);if(old!=null&&old.token().equals(token))remove(player);});});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(ForbiddenBirthClient::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(ForbiddenBirthClient::renderFruit);
        HudRenderCallback.EVENT.register(ForbiddenBirthClient::hud);
        ForbiddenBirthDiagnosticsClient.register();
    }
    public static Scene own(){var p=MinecraftClient.getInstance().player;return p==null?null:SCENES.get(p.getUuid());}
    public static boolean active(){return own()!=null;}
    public static boolean hidden(UUID id){var s=SCENES.get(id);return s!=null&&inNether()&&(s.start()<0||ForbiddenBirthTimeline.hidden(time(s,0)));}
    public static float scale(UUID id,float partial){var s=SCENES.get(id);return s==null||!inNether()?1:(float)ForbiddenBirthTimeline.emergence(time(s,partial));}
    public static float headBow(UUID id,float partial){var s=SCENES.get(id);if(s==null||!inNether())return 0;double t=time(s,partial);return t<ForbiddenBirthTimeline.REVEAL?0:(float)(22*(1-ForbiddenBirthTimeline.smooth((t-ForbiddenBirthTimeline.REVEAL)/70)));}
    private static boolean inNether(){var w=MinecraftClient.getInstance().world;return w!=null&&w.getRegistryKey()==World.NETHER;}
    private static double time(Scene s,float partial){var w=MinecraftClient.getInstance().world;return s.start()<0?0:Math.max(0,w==null?0:w.getTime()+partial-s.start());}
    public static void reducedMotion(){settings.reducedCameraMotion=true;try{Files.writeString(config(),new Gson().toJson(settings));}catch(Exception ex){throw new IllegalStateException(ex);}}
    public static void skip(){var s=own();if(s!=null&&ClientPlayNetworking.canSend(ForbiddenBirth.SKIP)){var b=PacketByteBufs.create();b.writeUuid(s.token());ClientPlayNetworking.send(ForbiddenBirth.SKIP,b);}if(s!=null)remove(s.player());}
    private static void remove(UUID id){var old=SCENES.remove(id);if(old==null)return;READY_SENT.remove(old.token());var c=MinecraftClient.getInstance();if(c.player!=null&&c.player.getUuid().equals(id)){returnFade=15;previousOwnTime=-1;KeyBinding.unpressAll();if(c.currentScreen instanceof BirthScreen)c.setScreen(null);}}
    private static void reset(){SCENES.clear();READY_SENT.clear();returnFade=0;previousOwnTime=-1;var c=MinecraftClient.getInstance();KeyBinding.unpressAll();if(c.currentScreen instanceof BirthScreen)c.setScreen(null);}
    private static void tick(MinecraftClient c){
        ticks++;if(returnFade>0)returnFade--;
        if(c.world==null||c.player==null){if(!SCENES.isEmpty())reset();return;}
        for(var s:new ArrayList<>(SCENES.values()))if(!inNether()||c.player.isDead()&&s.player().equals(c.player.getUuid())||time(s,0)>ForbiddenBirthTimeline.TIMEOUT||s.start()<0&&ticks-s.received()>240){remove(s.player());}
        var own=own();if(own!=null){
            c.options.attackKey.setPressed(false);c.options.useKey.setPressed(false);c.options.forwardKey.setPressed(false);c.options.backKey.setPressed(false);c.options.leftKey.setPressed(false);c.options.rightKey.setPressed(false);c.options.jumpKey.setPressed(false);c.options.sneakKey.setPressed(false);c.options.sprintKey.setPressed(false);
            if(!(c.currentScreen instanceof BirthScreen)&&!c.player.isDead())c.setScreen(new BirthScreen());
            if(own.start()<0&&!READY_SENT.contains(own.token())&&ticks-own.received()>=50&&loaded(c,own)){
                var b=PacketByteBufs.create();b.writeUuid(own.token());ClientPlayNetworking.send(ForbiddenBirth.READY,b);READY_SENT.add(own.token());
            }
            double t=time(own,0);if(own.start()>=0){
                if(previousOwnTime<1&&t>=1)c.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,.65f,.3f));
                if(previousOwnTime<ForbiddenBirthTimeline.IMPACT&&t>=ForbiddenBirthTimeline.IMPACT)c.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),.75f,.35f));
                if(previousOwnTime<ForbiddenBirthTimeline.FIRST_BREATH&&t>=ForbiddenBirthTimeline.FIRST_BREATH)c.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,.6f,.3f));
                previousOwnTime=t;
            }
        }
        int budget=12;for(var s:SCENES.values()){
            if(s.start()<0||budget<=0||c.player.squaredDistanceTo(s.landing())>9216)continue;double t=time(s,0);Vec3d p=fruit(s,t);
            if(t>=ForbiddenBirthTimeline.FALL_START&&t<ForbiddenBirthTimeline.IMPACT&&ticks%3==0){c.world.addParticle(ParticleTypes.ASH,p.x,p.y,p.z,0,.005,0);budget--;}
            if(t>=ForbiddenBirthTimeline.IMPACT&&t<ForbiddenBirthTimeline.FIRST_BREATH&&ticks%2==0)for(int i=0;i<4&&budget-->0;i++){double a=(ticks+i*5)*.7,r=.2+(t-ForbiddenBirthTimeline.IMPACT)*.012;c.world.addParticle(i%2==0?ParticleTypes.SOUL:ParticleTypes.SMALL_FLAME,s.landing().x+Math.cos(a)*r,s.landing().y+.3+(i%3)*.25,s.landing().z+Math.sin(a)*r,0,.02,0);}
        }
    }
    private static boolean loaded(MinecraftClient c,Scene s){int x=(int)s.anchor().x>>4,z=(int)s.anchor().z>>4;for(int i=0;i<12;i++)if(!c.world.getChunkManager().isChunkLoaded(x+i%4,z+i/4))return false;return true;}
    private static Vec3d fruit(Scene s,double t){double f=ForbiddenBirthTimeline.fall(t);return s.fruit().lerp(s.landing().add(0,.6,0),f);}
    public static CameraPose camera(float partial){
        var s=own();if(s==null||!inNether())return null;double t=time(s,partial);Vec3d f=fruit(s,t),pos,target;double fov;
        if(settings.reducedCameraMotion){pos=s.landing().add(6,3,8);target=s.landing().add(0,1.1,0);fov=65;}
        else if(t<ForbiddenBirthTimeline.FALL_START){
            Vec3d center=s.anchor().add(ForbiddenTreeAsset.CENTER),wide=center.add(36,3,48),close=s.fruit().add(5,-1,0);double mix=ForbiddenBirthTimeline.smooth((t-35)/30);
            pos=wide.lerp(close,mix);target=center.lerp(s.fruit(),mix);fov=100-45*mix;
        }else if(t<ForbiddenBirthTimeline.REVEAL){double mix=ForbiddenBirthTimeline.smooth((t-ForbiddenBirthTimeline.FALL_START)/90);pos=f.add(5,-1+4*mix,7*mix);target=f;fov=50+5*mix;}
        else{double mix=ForbiddenBirthTimeline.smooth((t-ForbiddenBirthTimeline.REVEAL)/100);pos=s.landing().add(5-2*mix,2.8-mix,7-3*mix);target=s.landing().add(0,.9+mix*.35,0);fov=55;}
        // Adapt the camera to actual cave occlusion; never modify blocks for framing.
        var c=MinecraftClient.getInstance();var hit=c.world.raycast(new net.minecraft.world.RaycastContext(target,pos,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,c.player));
        if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.MISS&&hit.getPos().squaredDistanceTo(target)>1)pos=hit.getPos().add(target.subtract(pos).normalize().multiply(.25));
        return new CameraPose(pos,target,fov);
    }
    private static void renderFruit(WorldRenderContext ctx){
        var c=MinecraftClient.getInstance();if(c.world==null||!inNether()||ctx.matrixStack()==null||ctx.consumers()==null)return;var matrices=ctx.matrixStack();
        for(var s:SCENES.values()){
            if(s.start()<0||c.player==null||c.player.squaredDistanceTo(s.landing())>16384)continue;double t=time(s,ctx.tickDelta()),growth=ForbiddenBirthTimeline.emergence(t);if(growth>=1)continue;
            Vec3d at=fruit(s,t).subtract(ctx.camera().getPos());matrices.push();matrices.translate(at.x,at.y,at.z);float scale=(float)(1.35*(1-growth)*(1+.025*Math.sin(t*.13)));matrices.scale(scale,scale,scale);matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float)(t<95?Math.sin(t*.07)*5:(t-95)*.7)));
            c.getItemRenderer().renderItem(FRUIT,ModelTransformationMode.GROUND,LightmapTextureManager.MAX_LIGHT_COORDINATE,OverlayTexture.DEFAULT_UV,matrices,ctx.consumers(),c.world,0);matrices.pop();
        }
    }
    private static void hud(DrawContext ctx,float partial){if(active())drawOverlay(ctx);else if(returnFade>0){int alpha=Math.min(255,returnFade*255/15);ctx.fill(0,0,ctx.getScaledWindowWidth(),ctx.getScaledWindowHeight(),alpha<<24);}}
    private static void drawOverlay(DrawContext ctx){var c=MinecraftClient.getInstance();var s=own();if(s==null)return;double t=time(s,0);int w=ctx.getScaledWindowWidth(),h=ctx.getScaledWindowHeight();ctx.fill(0,0,w,22,0xFF080409);ctx.fill(0,h-28,w,h,0xFF080409);String label=s.start()<0?"Awakening the Forbidden Tree...":t<55?"The Forbidden Tree":t<155?"The Forbidden Fruit":t<215?"Born from the Forbidden Tree.":"APPLEBORN";ctx.drawCenteredTextWithShadow(c.textRenderer,Text.literal(label),w/2,h-20,0xFFE4C2);int fade=s.start()<0?255:t<12?(int)((1-t/12)*255):t>270?(int)(Math.min(1,(t-270)/15)*255):0;if(fade>0)ctx.fill(0,0,w,h,fade<<24);}
    public static final class BirthScreen extends Screen {
        public BirthScreen(){super(Text.literal("Appleborn Awakening"));}
        @Override protected void init(){addDrawableChild(ButtonWidget.builder(Text.literal("Skip cinematic"),b->skip()).dimensions(width-145,height-52,135,20).build());addDrawableChild(ButtonWidget.builder(Text.literal("Camera motion: "+(settings.reducedCameraMotion?"Reduced":"Cinematic")),b->{settings.reducedCameraMotion=!settings.reducedCameraMotion;try{Files.writeString(config(),new GsonBuilder().setPrettyPrinting().create().toJson(settings));}catch(Exception ex){org.marj4n.smooth_classes.SmoothClasses.LOGGER.warn("Cannot save camera accessibility preference",ex);}clearAndInit();}).dimensions(10,height-52,195,20).build());}
        @Override public boolean shouldPause(){return false;}
        @Override public void close(){skip();}
        @Override public void render(DrawContext c,int x,int y,float dt){drawOverlay(c);super.render(c,x,y,dt);}
    }
}
