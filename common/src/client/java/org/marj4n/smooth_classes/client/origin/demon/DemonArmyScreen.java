package org.marj4n.smooth_classes.client.origin.demon;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.marj4n.smooth_classes.client.SmoothClassesClient;
import org.marj4n.smooth_classes.origin.demon.*;

public final class DemonArmyScreen extends Screen {
    private int hovered=-1;private boolean held,finished;
    public DemonArmyScreen(){super(Text.literal("All Hell Shall Kneel"));}
    @Override protected void init(){held=SmoothClassesClient.classSpecialKey().isPressed();}
    @Override public void tick(){if(held&&!SmoothClassesClient.classSpecialKey().isPressed())finish();}
    @Override public boolean shouldPause(){return false;}
    private void finish(){if(finished)return;finished=true;if(hovered>=0){var packet=PacketByteBufs.create();packet.writeVarInt(hovered);ClientPlayNetworking.send(DemonNetworking.ARMY,packet);}close();}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0){finish();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean keyReleased(int key,int scan,int modifiers){if(SmoothClassesClient.classSpecialKey().matchesKey(key,scan)){finish();return true;}return super.keyReleased(key,scan,modifiers);}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderBackground(c);int cx=width/2,cy=height/2;double dx=mx-cx,dy=my-cy;double distance=Math.hypot(dx,dy);
        hovered=distance>32&&distance<150?Math.floorMod((int)Math.floor((Math.atan2(dy,dx)+Math.PI/2+Math.PI/6)/(Math.PI/3)),6):-1;
        for(int i=0;i<180;i++){
            double a=i*Math.PI/90-Math.PI/2;int sector=Math.floorMod((int)Math.floor((a+Math.PI/2+Math.PI/6)/(Math.PI/3)),6);
            int color=sector==hovered?0xFFAA332A:0xFF493335;
            for(int r=53;r<100;r+=3){int x=cx+(int)(Math.cos(a)*r),y=cy+(int)(Math.sin(a)*r);c.fill(x-2,y-2,x+2,y+2,color);}
        }
        for(int i=0;i<6;i++){double a=i*Math.PI/3-Math.PI/2;int x=cx+(int)(Math.cos(a)*119),y=cy+(int)(Math.sin(a)*119);c.drawCenteredTextWithShadow(textRenderer,Text.literal(DemonRules.COMMANDS[i]),x,y-4,i==hovered?0xFFF0C2:0xE0D0CD);}
        c.drawCenteredTextWithShadow(textRenderer,Text.literal(hovered>=0?DemonRules.COMMANDS[hovered]:"COMMAND"),cx,cy-4,0xFFF0C2);
        c.drawCenteredTextWithShadow(textRenderer,title,cx,cy-160,0xE6A596);
        c.drawCenteredTextWithShadow(textRenderer,Text.literal("Release the ability key or click to command. ESC cancels."),cx,cy+157,0xC5B7B4);
    }
}
