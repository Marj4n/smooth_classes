package org.marj4n.smooth_classes.client.avenger.dance;
import java.util.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.*;
import org.marj4n.smooth_classes.mixin.client.*;

/** Read-only mapping of original vanilla ModelPart objects, never a replacement mesh. */
public final class DanceRig {
    public final Map<String,ModelPart> parts=new LinkedHashMap<>();
    public final Map<String,ModelPart> roots=new LinkedHashMap<>();
    public DanceRig(EntityModel<?> m){
        if(m instanceof SinglePartEntityModel<?> s){roots.put("root",s.getPart());walk("",s.getPart());}
        else if(m instanceof ShulkerEntityModel<?> sh){int i=0;for(var part:sh.getParts())root("shell_"+i++,part);root("lid",sh.getLid());root("head",sh.getHead());}
        else if(m instanceof BipedEntityModel<?> b){root("head",b.head);root("hat",b.hat);root("body",b.body);root("right_arm",b.rightArm);root("left_arm",b.leftArm);root("right_leg",b.rightLeg);root("left_leg",b.leftLeg);}
        else if(m instanceof QuadrupedEntityModel<?>){var q=(AvengerDanceQuadrupedAccessor)m;root("head",q.smoothClasses$danceHead());root("body",q.smoothClasses$danceBody());root("right_hind_leg",q.smoothClasses$danceRH());root("left_hind_leg",q.smoothClasses$danceLH());root("right_front_leg",q.smoothClasses$danceRF());root("left_front_leg",q.smoothClasses$danceLF());}
        else if(m instanceof AnimalModel<?>){var a=(AvengerDanceAnimalAccessor)m;int i=0;for(var part:a.smoothClasses$danceHeads())root("head_"+i++,part);i=0;for(var part:a.smoothClasses$danceBodies())root("body_"+i++,part);}
        if(m instanceof PlayerEntityModel<?> p){root("right_sleeve",p.rightSleeve);root("left_sleeve",p.leftSleeve);root("right_pants",p.rightPants);root("left_pants",p.leftPants);root("jacket",p.jacket);}
    }
    private void root(String name,ModelPart p){roots.put(name,p);walk(name,p);}
    private void walk(String name,ModelPart p){parts.put(name,p);for(var e:((AvengerDancePartAccessor)(Object)p).smoothClasses$danceChildren().entrySet())walk(name.isEmpty()?e.getKey():name+"/"+e.getKey(),e.getValue());}
    public ModelPart part(String name){return parts.get(name);}
}
