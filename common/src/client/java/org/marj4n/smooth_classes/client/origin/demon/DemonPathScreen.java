package org.marj4n.smooth_classes.client.origin.demon;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.marj4n.smooth_classes.origin.demon.DemonNetworking;
import org.marj4n.smooth_classes.client.origin.OriginClientState;

public final class DemonPathScreen extends Screen {
    public DemonPathScreen(){super(Text.literal("Choose Your Path"));}
    @Override protected void init(){
        String[] names={"World-Ender","Temptation / Succubus — Coming Soon","Hellkeeper — Coming Soon","Divine Demon — Coming Soon"};
        for(int i=0;i<4;i++){
            final int index=i;
            var button=ButtonWidget.builder(Text.literal(names[i]),b->{
                if(index!=0)return;var packet=PacketByteBufs.create();packet.writeString("world_ender");ClientPlayNetworking.send(DemonNetworking.CHOOSE_PATH,packet);close();
            }).dimensions(width/2-150,height/2-46+i*28,300,22).build();
            button.active=i==0&&OriginClientState.hasFlag("demon.cinder_unlocked")&&!OriginClientState.hasFlag("demon.world_ender");addDrawableChild(button);
        }
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){renderBackground(c);c.drawCenteredTextWithShadow(textRenderer,title,width/2,height/2-95,0xFFE3AA);c.drawCenteredTextWithShadow(textRenderer,Text.literal("One permanent evolutionary path. No Origin XP."),width/2,height/2-75,0xCCC1B9);super.render(c,mx,my,delta);}
    @Override public boolean shouldPause(){return false;}
}
