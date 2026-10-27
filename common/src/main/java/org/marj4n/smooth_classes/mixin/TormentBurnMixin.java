package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import org.marj4n.smooth_classes.runtime.TormentBurnAccess;
import org.marj4n.smooth_classes.runtime.TormentRuntime;
import org.marj4n.smooth_classes.registry.SmoothParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(LivingEntity.class)
public abstract class TormentBurnMixin implements TormentBurnAccess {
    @Unique private final Map<UUID,NbtCompound> smooth$burns=new HashMap<>();
    @Override public void smooth$ignite(UUID cast,UUID owner,long expires,float damage,float coefficient){
        // Re-entering the same cast never extends its absolute expiry.
        if(smooth$burns.containsKey(cast))return;
        var n=new NbtCompound();n.putUuid("Cast",cast);if(owner!=null)n.putUuid("Owner",owner);n.putLong("Expires",expires);n.putFloat("Damage",damage);n.putFloat("Coefficient",coefficient);smooth$burns.put(cast,n);
    }
    @Inject(method="tick",at=@At("TAIL"))
    private void smooth$burnTick(CallbackInfo ci){
        var e=(LivingEntity)(Object)this;
        if(!(e.getWorld() instanceof ServerWorld world))return;
        long now=TormentRuntime.now(world);
        smooth$burns.values().removeIf(n->n.getLong("Expires")<=now);
        if(!e.isAlive()){smooth$burns.clear();return;}
        if(smooth$burns.isEmpty())return;
        if(e.age%4==0)world.spawnParticles(SmoothParticles.BLACK_FLAME,e.getX(),e.getY()+e.getHeight()*.45,e.getZ(),8,e.getWidth()*.5,e.getHeight()*.4,e.getWidth()*.5,.02);
        if(e.age%20==0){
            // Overlapping casts retain independent expiry; only the strongest live burn deals damage.
            NbtCompound strongest=null;
            float strongestDamage=Float.NEGATIVE_INFINITY;
            for(NbtCompound burn:smooth$burns.values()){
                float damage=burn.getFloat("Damage");
                if(damage>strongestDamage){strongestDamage=damage;strongest=burn;}
            }
            if(strongest==null)return;
            float coefficient=strongest.contains("Coefficient")?strongest.getFloat("Coefficient"):TormentRuntime.BURN_COEFFICIENT;
            TormentRuntime.hurt(e,strongest.containsUuid("Owner")?strongest.getUuid("Owner"):null,strongest.getFloat("Damage"),coefficient);
        }
    }
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void smooth$saveBurn(NbtCompound n,CallbackInfo ci){var list=new NbtList();smooth$burns.values().forEach(b->list.add(b.copy()));n.put("SmoothTormentBurns",list);}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void smooth$loadBurn(NbtCompound n,CallbackInfo ci){smooth$burns.clear();var list=n.getList("SmoothTormentBurns",10);for(int i=0;i<list.size();i++){var b=list.getCompound(i);if(b.containsUuid("Cast"))smooth$burns.put(b.getUuid("Cast"),b.copy());}}
}
