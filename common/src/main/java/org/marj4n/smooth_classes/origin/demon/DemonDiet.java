package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import org.marj4n.smooth_classes.origin.*;

public final class DemonDiet {
    private DemonDiet(){}
    public static boolean applies(PlayerEntity p){return DemonProgression.demon(p)&&OriginRuntime.state(p).hasFlag("demon.world_ender");}
    public static int plant(Item item){
        if(item==Items.CRIMSON_FUNGUS||item==Items.WARPED_FUNGUS)return 2;
        if(item==Items.CRIMSON_ROOTS||item==Items.WARPED_ROOTS||item==Items.NETHER_SPROUTS||item==Items.NETHER_WART)return 1;
        return 0;
    }
    public static boolean allowed(Item item){return item==Items.PORKCHOP||item==Items.COOKED_PORKCHOP||plant(item)>0;}
    public static boolean reject(PlayerEntity p,ItemStack stack){
        if(!applies(p)||!stack.isFood()||allowed(stack.getItem()))return false;
        if(p instanceof net.minecraft.server.network.ServerPlayerEntity server){
            var state=OriginRuntime.state(p);long now=server.getServer().getOverworld().getTime();
            if(now>=state.longProgress("demon.food_notice_after")){p.sendMessage(Text.literal("Your infernal body rejects this food."),true);state.longProgress("demon.food_notice_after",now+20);}
        }
        return true;
    }
    public static ItemStack consume(World world,PlayerEntity p,ItemStack stack){
        int food=plant(stack.getItem());
        if(!world.isClient&&applies(p)&&food>0){
            // HungerManager.add takes saturation MODIFIER, not raw saturation points.
            float points=(float)(food==2?DemonConfig.INSTANCE.fungusSaturation:DemonConfig.INSTANCE.plantSaturation);
            p.getHungerManager().add(food,points/(food*2F));
            world.playSound(null,p.getBlockPos(),net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EAT,net.minecraft.sound.SoundCategory.PLAYERS,1F,1F);
            Item foodItem=stack.getItem();if(!p.isCreative())stack.decrement(1);
            p.incrementStat(net.minecraft.stat.Stats.USED.getOrCreateStat(foodItem));
        }
        return stack;
    }
}
