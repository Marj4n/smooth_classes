package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

/** Actual converted Psemata with-face schematic. No procedural replacement tree. */
public final class ForbiddenTreeAsset {
    public static final String HASH="0ffe42aed930e87687e22fb605cefa65325c0f96a7f87126d7cb790a5a62ec1a";
    public static final int WIDTH=64,HEIGHT=85,LENGTH=37;
    public record Cell(BlockPos offset,BlockState state){}
    public static final Vec3d FRUIT=new Vec3d(33.5,30.4,28.5),LANDING=new Vec3d(33.5,1,28.5),FACE=new Vec3d(28.5,11.5,23.8),CENTER=new Vec3d(32,42,18.5);
    private static List<Cell> cells;private static int paletteCount;
    private ForbiddenTreeAsset(){}
    public static List<Cell> cells(){if(cells!=null)return cells;try(var in=ForbiddenTreeAsset.class.getResourceAsStream("/data/smooth_classes/structures/forbidden_apple_tree.nbt")){
        if(in==null)throw new IllegalStateException("Missing original converted Forbidden Tree resource");var n=NbtIo.readCompressed(in);var palette=new ArrayList<BlockState>();
        for(var e:n.getList("palette",10)){var p=(NbtCompound)e;Identifier id=Identifier.tryParse(p.getString("Name"));if(id==null||!org.marj4n.smooth_classes.platform.PlatformRegistries.BLOCK.containsId(id))throw new IllegalStateException("Unknown1.20.1 block "+id);var block=org.marj4n.smooth_classes.platform.PlatformRegistries.BLOCK.get(id);BlockState state=block.getDefaultState();var props=p.getCompound("Properties");for(String key:props.getKeys()){Property<?> property=block.getStateManager().getProperty(key);if(property==null)throw new IllegalStateException("Unknown block property "+id+"/"+key);state=property(state,property,props.getString(key));}palette.add(state);}
        paletteCount=palette.size();var list=new ArrayList<Cell>();for(var e:n.getList("blocks",10)){var b=(NbtCompound)e;var pos=b.getList("pos",3);int i=b.getInt("state");if(i<0||i>=palette.size())throw new IllegalStateException("Palette index");BlockPos offset=new BlockPos(pos.getInt(0),pos.getInt(1),pos.getInt(2));if(offset.getX()<0||offset.getX()>=WIDTH||offset.getY()<0||offset.getY()>=HEIGHT||offset.getZ()<0||offset.getZ()>=LENGTH)throw new IllegalStateException("Tree coordinate outside actual source bounds");list.add(new Cell(offset,palette.get(i)));}
        if(list.size()!=8271||paletteCount!=53||!n.getList("entities",10).isEmpty())throw new IllegalStateException("Wrong tree variant or incomplete conversion");cells=List.copyOf(list);return cells;
    }catch(java.io.IOException ex){throw new IllegalStateException("Cannot read Forbidden Tree",ex);}}
    private static <T extends Comparable<T>> BlockState property(BlockState s,Property<T> p,String value){return s.with(p,p.parse(value).orElseThrow(()->new IllegalStateException("Unknown1.20.1 state "+p.getName()+"="+value)));}
    public static int paletteCount(){cells();return paletteCount;}
    public static BlockPos anchor(ForbiddenTreeRecord r){return new BlockPos(r.x,r.y,r.z);}
    public static Box bounds(BlockPos a){return new Box(a.getX(),a.getY(),a.getZ(),a.getX()+WIDTH,a.getY()+HEIGHT,a.getZ()+LENGTH);}
}
