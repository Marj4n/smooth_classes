package org.marj4n.smooth_classes.client.origin.demon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.marj4n.smooth_classes.origin.demon.*;

/** Displays authoritative server counters directly; no independent wall-clock countdown. */
public final class DemonHud {
    private DemonHud(){}
    public static void render(DrawContext c,float tickDelta){
        var mc=MinecraftClient.getInstance();if(mc.player==null||mc.options.hudHidden||!"demon".equals(OriginClientState.originId))return;
        int y=8;
        if(OriginClientState.hasFlag("demon.netherforged"))y=line(c,mc,y,Items.BLAZE_POWDER,"Netherforged — Fire Resistance",0xFFE2AC);
        else if(OriginClientState.progress("demon.cinder_active")>0)y=line(c,mc,y,Items.BLAZE_POWDER,"Cinder Reflex — Fire Resistance "+time(OriginClientState.progress("demon.cinder_active")),0xFFE2AC);
        else if(OriginClientState.progress("demon.cinder_recovery")>0)y=line(c,mc,y,Items.GUNPOWDER,"Cinder Reflex — Recovering "+time(OriginClientState.progress("demon.cinder_recovery")),0xC7B4AA);
        if(!OriginClientState.hasFlag("demon.lord")){
            int ticks=OriginClientState.progress("demon.decay");String stage=new String[]{"Stable","Decay Warning","Soul Decay","Zombified Demon"}[DemonRules.decayStage(ticks)];
            y=line(c,mc,y,Items.ROTTEN_FLESH,"Netherbound — "+stage+" "+time(ticks),DemonRules.decayStage(ticks)>=2?0xDF7777:0xD6C4AF);
        }
        if(OriginClientState.progress("demon.domain_active")>0)y=line(c,mc,y,Items.FIRE_CHARGE,"Pandemonium — "+time(OriginClientState.progress("demon.domain_active")),0xFFAF7B);
        if(OriginClientState.hasFlag("demon.lord"))y=line(c,mc,y,Items.NETHER_STAR,"Thronebound — "+(OriginClientState.progress("demon.throne_cd")>0?time(OriginClientState.progress("demon.throne_cd")):"Ready"),0xDED0EF);
        if(OriginClientState.hasFlag("demon.lord")){long stacks=DemonStreakClient.stacks(mc.player.getUuid());if(stacks>0){y=line(c,mc,y,Items.WITHER_SKELETON_SKULL,"Infernal Streak: "+stacks+" | +"+stacks+" HP | +"+String.format(java.util.Locale.ROOT,"%.1f",stacks*.5)+" Armor | "+time(DemonStreakClient.remaining(mc.player.getUuid())),0xFFB974);}}
    }
    private static int line(DrawContext c,MinecraftClient mc,int y,Item icon,String label,int color){c.fill(5,y-1,mc.textRenderer.getWidth(label)+30,y+18,0x900E0C12);c.drawItem(new ItemStack(icon),8,y);c.drawTextWithShadow(mc.textRenderer,Text.literal(label),29,y+4,color);return y+21;}
    private static String time(int ticks){int sec=(ticks+19)/20;return sec/60+":"+String.format(java.util.Locale.ROOT,"%02d",sec%60);}
    public static void register(){
        ForbiddenBirthClient.register();
        DemonStreakClient.register();
        DemonVisualTestClient.register();
        DemonClientMetrics.register();
        DemonVisualEffects.register();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(DemonNetworking.OPEN_PATH,(client,handler,buf,sender)->client.execute(()->client.setScreen(new DemonPathScreen())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(DemonNetworking.OPEN_ARMY,(client,handler,buf,sender)->client.execute(()->client.setScreen(new DemonArmyScreen())));
    }
}
