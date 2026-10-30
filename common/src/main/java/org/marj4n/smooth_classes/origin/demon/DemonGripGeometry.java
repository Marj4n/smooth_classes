package org.marj4n.smooth_classes.origin.demon;
/** Vanilla HeldItemFeatureRenderer's fixed local grip, expressed in model units.
 * Translate before its rotations/translation so any authored arm length still
 * ends at its actual wrist. No sword/shield-specific offsets are required. */
public final class DemonGripGeometry {
 public record Offset(float x,float y,float z){}
 private DemonGripGeometry(){}
 public static Offset offset(float[] wrist,float[] pivot,boolean left){
  return new Offset(wrist[0]-pivot[0]-(left?1:-1),pivot[1]-wrist[1]-10,wrist[2]-pivot[2]+2);
 }
}
