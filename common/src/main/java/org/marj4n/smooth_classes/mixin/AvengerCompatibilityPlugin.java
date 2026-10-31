package org.marj4n.smooth_classes.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.*;
import java.util.*;

/** Narrow, loader-neutral call-site adapters; never cancel an external mixin globally. */
public final class AvengerCompatibilityPlugin implements IMixinConfigPlugin {
    private static final String RUNTIME="org/marj4n/smooth_classes/content/avenger/runtime/AvengerReworkRuntime";
    public void onLoad(String mixinPackage){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){return true;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){
        if(mixin.endsWith("AvengerServerWorldCompatMixin")){
            for(MethodNode method:node.methods){
                if(!method.name.contains("eldritch_end$fixSpawn"))continue;
                int changed=0;
                for(var instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode call
                        &&Set.of("isOf","method_27852","m_60713_").contains(call.name)
                        &&Type.getArgumentTypes(call.desc).length==1&&Type.getReturnType(call.desc).getSort()==Type.BOOLEAN){
                    String descriptor="(L"+call.owner+";"+Type.getArgumentTypes(call.desc)[0].getDescriptor()+"Ljava/lang/Object;)Z";
                    method.instructions.insertBefore(call,new VarInsnNode(Opcodes.ALOAD,1));
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,RUNTIME,"rejectNaturalCaveSpawn",descriptor,false));
                    method.maxStack++;changed++;
                }
                if(changed!=1)throw new IllegalStateException("Unexpected Eldritch End spawn-guard ABI: "+changed);
            }
        }
        if(mixin.endsWith("AvengerWhirlwindHabitatMixin")){
            int kills=0,ticks=0;
            for(MethodNode method:node.methods){
                if(Set.of("tick","method_5773","m_8119_").contains(method.name)&&method.desc.equals("()V")){
                    // Reset only the temporary environmental lifetime, never health or combat damage.
                    InsnList head=new InsnList();
                    head.add(new VarInsnNode(Opcodes.ALOAD,0));head.add(new VarInsnNode(Opcodes.ALOAD,0));
                    head.add(new VarInsnNode(Opcodes.ALOAD,0));head.add(new FieldInsnNode(Opcodes.GETFIELD,node.name,"lifeLeft","I"));
                    head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,RUNTIME,"boundHabitatLife","(Ljava/lang/Object;I)I",false));
                    head.add(new FieldInsnNode(Opcodes.PUTFIELD,node.name,"lifeLeft","I"));
                    method.instructions.insert(head);method.maxStack=Math.max(method.maxStack,3);ticks++;
                }
                if(!Set.of("tick","method_5773","m_8119_","tickMovement","method_6007","m_8107_").contains(method.name))continue;
                for(var instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode call
                        &&Set.of("kill","method_31472","m_6074_").contains(call.name)&&call.desc.equals("()V")){
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,RUNTIME,"killHabitatMobUnlessBound","(Ljava/lang/Object;)V",false));kills++;
                }
            }
            if(kills!=2||ticks!=1)throw new IllegalStateException("Unexpected Whirlwind habitat ABI: "+kills+"/"+ticks);
        }
        if(mixin.endsWith("AvengerAechorHabitatMixin")){
            int changed=0;
            for(MethodNode method:node.methods)if(Set.of("tick","method_5773","m_8119_").contains(method.name)&&method.desc.equals("()V")){
                for(var instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode call
                        &&Set.of("discard","method_5768","m_146870_").contains(call.name)&&call.desc.equals("()V")){
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,RUNTIME,"discardHabitatMobUnlessBound","(Ljava/lang/Object;)V",false));changed++;
                }
            }
            if(changed!=1)throw new IllegalStateException("Unexpected Aechor Plant habitat ABI: "+changed);
        }
    }
}
