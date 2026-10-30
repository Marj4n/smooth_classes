package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.*;

public final class DemonTrials {
    private DemonTrials(){}
    public static String structure(ServerWorld w,BlockPos pos){
        if(w.getRegistryKey()!=World.NETHER||!w.isChunkLoaded(pos))return "";
        for(String id:List.of("fortress","bastion_remnant")){
            var key=RegistryKey.of(RegistryKeys.STRUCTURE,new Identifier("minecraft",id));
            var start=w.getStructureAccessor().getStructureContaining(pos,key);
            if(start!=null&&start.hasChildren())return id.equals("fortress")?"fortress":"bastion";
        }
        // YUNG's generated replacement is accepted only when its registered structure
        // exists, contains the chest, and the chest has a genuine fortress loot table.
        var registry=w.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for(var entry:registry.getEntrySet())if(entry.getKey().getValue().getNamespace().equals("betterfortresses")){
            var start=w.getStructureAccessor().getStructureContaining(pos,entry.getValue());if(start!=null&&start.hasChildren())return "fortress";
        }
        return "";
    }
    public static void provenance(LootableContainerBlockEntity chest,Identifier loot){
        if(!(chest.getWorld() instanceof ServerWorld w)||loot==null)return;
        String kind=structure(w,chest.getPos());if(kind.isEmpty())return;
        String id=loot.toString();boolean valid=kind.equals("fortress")?(id.equals("minecraft:chests/nether_bridge")||id.startsWith("betterfortresses:chests/")):id.startsWith("minecraft:chests/bastion_");
        if(valid){var ledger=DemonWorldState.get(w.getServer());ledger.naturalChests.put(chest.getPos().asLong(),kind);ledger.markDirty();}
    }
    public record Receipt(Inventory inventory,BlockPos pos,String kind,int count){}
    public static List<Receipt> before(ScreenHandler handler,PlayerEntity player){
        if(!(player instanceof ServerPlayerEntity p)||!DemonProgression.demon(p)||!DemonProgression.has(p,"world_ender")||p.getWorld().getRegistryKey()!=World.NETHER)return List.of();
        var ledger=DemonWorldState.get(p.getServer());List<Receipt> out=new ArrayList<>();Set<Inventory> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var entry:ledger.naturalChests.entrySet()){
            BlockPos pos=BlockPos.fromLong(entry.getKey());if(pos.getSquaredDistance(p.getBlockPos())>64||!p.getServerWorld().isChunkLoaded(pos))continue;
            if(!(p.getWorld().getBlockEntity(pos) instanceof Inventory container))continue;
            if(!entry.getValue().equals(structure(p.getServerWorld(),pos))||!entry.getValue().equals(structure(p.getServerWorld(),p.getBlockPos())))continue;
            for(var slot:handler.slots)if(slot.inventory==container||(slot.inventory instanceof DoubleInventory d&&d.isPart(container))){
                if(seen.add(slot.inventory))out.add(new Receipt(slot.inventory,pos,entry.getValue(),count(slot.inventory)));break;
            }
        }
        return out;
    }
    public static int count(Inventory inv){int total=0;for(int i=0;i<inv.size();i++)total+=inv.getStack(i).getCount();return total;}
    public static void after(List<Receipt> receipts,ScreenHandler handler,PlayerEntity player,int playerItems){
        if(!(player instanceof ServerPlayerEntity p)||count(p.getInventory())+handler.getCursorStack().getCount()<=playerItems)return;
        for(var receipt:receipts)if(count(receipt.inventory())<receipt.count()){
            var s=OriginRuntime.state(p);s.flag("demon.trial."+receipt.kind());
            s.flag("demon.visited."+receipt.kind()+"."+receipt.pos().asLong());
            DemonProgression.sync(p,s);org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);
        }
    }
    public static void blockChanged(ServerWorld w,BlockPos pos,net.minecraft.block.BlockState next){
        if(w.getRegistryKey()!=World.NETHER)return;
        var old=w.getBlockState(pos);
        if(old.getBlock()==next.getBlock()||!(w.getBlockEntity(pos) instanceof LootableContainerBlockEntity))return;
        var ledger=DemonWorldState.get(w.getServer());if(ledger.naturalChests.remove(pos.asLong())!=null)ledger.markDirty();
    }
}
