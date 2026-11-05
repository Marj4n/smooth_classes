package org.marj4n.smooth_classes.client.renderer.model;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.entity.GreaterDreadglareEntity;
/** Original Vigil Husk geometry. Generated from genuine native Renas source.
 * Static visual adapter only; renderer scale, gameplay and entity behavior unchanged.
 * Bind only the new deterministically UV-adapted original atlas from this handoff.
 */
public class GreaterDreadglareModel extends EntityModel<GreaterDreadglareEntity> {
    private final ModelPart bb_main;
    public GreaterDreadglareModel(ModelPart root) { this.bb_main = root.getChild("bb_main"); }
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData bb = data.getRoot().addChild("bb_main", ModelPartBuilder.create(), ModelTransform.pivot(0F,29.4666667F,0F));
        bb.addChild("husk_central_core", ModelPartBuilder.create().uv(2, 2).cuboid(-6.0F, -16.0F, -2.0F, 12.0F, 12.0F, 7.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("husk_upper_slope", ModelPartBuilder.create().uv(42, 2).cuboid(-4.0F, -18.0F, -1.0F, 8.0F, 2.0F, 5.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("husk_lower_slope", ModelPartBuilder.create().uv(70, 2).cuboid(-4.0F, -4.0F, -1.0F, 8.0F, 2.0F, 5.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_husk_cheek", ModelPartBuilder.create().uv(98, 2).cuboid(-7.0F, -14.0F, -1.0F, 1.0F, 8.0F, 5.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("left_husk_cheek", ModelPartBuilder.create().uv(112, 2).cuboid(6.0F, -14.0F, -1.0F, 1.0F, 8.0F, 5.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("lens_equator", ModelPartBuilder.create().uv(2, 23).cuboid(-5.0F, -13.0F, -5.0F, 10.0F, 6.0F, 3.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("lens_upper", ModelPartBuilder.create().uv(30, 23).cuboid(-3.0F, -15.0F, -4.0F, 6.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("lens_lower", ModelPartBuilder.create().uv(48, 23).cuboid(-3.0F, -7.0F, -4.0F, 6.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_lens_corner", ModelPartBuilder.create().uv(66, 23).cuboid(-6.0F, -12.0F, -4.0F, 1.0F, 4.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("left_lens_corner", ModelPartBuilder.create().uv(74, 23).cuboid(5.0F, -12.0F, -4.0F, 1.0F, 4.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("iris_equator", ModelPartBuilder.create().uv(82, 23).cuboid(-3.0F, -12.0F, -6.0F, 6.0F, 4.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("iris_upper", ModelPartBuilder.create().uv(98, 23).cuboid(-2.0F, -14.0F, -6.0F, 4.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("iris_lower", ModelPartBuilder.create().uv(110, 23).cuboid(-2.0F, -8.0F, -6.0F, 4.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("pupil_slit", ModelPartBuilder.create().uv(2, 34).cuboid(-1.0F, -13.0F, -7.0F, 2.0F, 6.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("upper_lid_bridge", ModelPartBuilder.create().uv(10, 34).cuboid(-3.0F, -16.0F, -5.0F, 6.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("lower_lid_bridge", ModelPartBuilder.create().uv(28, 34).cuboid(-3.0F, -6.0F, -5.0F, 6.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_upper_lid_arc", ModelPartBuilder.create().uv(46, 34).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(-4.0F, -14.5F, -4.0F, 0.0F, 0.0F, -0.663225116F));
        bb.addChild("left_upper_lid_arc", ModelPartBuilder.create().uv(56, 34).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(4.0F, -14.5F, -4.0F, 0.0F, 0.0F, 0.663225116F));
        bb.addChild("right_lower_lid_arc", ModelPartBuilder.create().uv(66, 34).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(-4.0F, -5.5F, -4.0F, 0.0F, 0.0F, 0.663225116F));
        bb.addChild("left_lower_lid_arc", ModelPartBuilder.create().uv(76, 34).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(4.0F, -5.5F, -4.0F, 0.0F, 0.0F, -0.663225116F));
        bb.addChild("right_eye_canthus", ModelPartBuilder.create().uv(86, 34).cuboid(-7.0F, -11.0F, -5.0F, 2.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("left_eye_canthus", ModelPartBuilder.create().uv(96, 34).cuboid(5.0F, -11.0F, -5.0F, 2.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_flank_plate_0", ModelPartBuilder.create().uv(106, 34).cuboid(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, Dilation.NONE), ModelTransform.of(-8.0F, -8.0F, 1.5F, 0.0F, 0.34906585F, -0.34906585F));
        bb.addChild("left_flank_plate_0", ModelPartBuilder.create().uv(2, 43).cuboid(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, Dilation.NONE), ModelTransform.of(8.0F, -8.0F, 1.5F, 0.0F, -0.34906585F, 0.34906585F));
        bb.addChild("right_flank_ivory_edge_0", ModelPartBuilder.create().uv(18, 43).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(-10.0F, -7.5F, 1.0F, 0.0F, 0.34906585F, -0.34906585F));
        bb.addChild("left_flank_ivory_edge_0", ModelPartBuilder.create().uv(28, 43).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(10.0F, -7.5F, 1.0F, 0.0F, -0.34906585F, 0.34906585F));
        bb.addChild("right_flank_plate_1", ModelPartBuilder.create().uv(38, 43).cuboid(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, Dilation.NONE), ModelTransform.of(-8.0F, -11.0F, 1.5F, 0.0F, 0.34906585F, 0.0F));
        bb.addChild("left_flank_plate_1", ModelPartBuilder.create().uv(54, 43).cuboid(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, Dilation.NONE), ModelTransform.of(8.0F, -11.0F, 1.5F, 0.0F, -0.34906585F, 0.0F));
        bb.addChild("right_flank_ivory_edge_1", ModelPartBuilder.create().uv(70, 43).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(-10.0F, -10.5F, 1.0F, 0.0F, 0.34906585F, 0.0F));
        bb.addChild("left_flank_ivory_edge_1", ModelPartBuilder.create().uv(80, 43).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(10.0F, -10.5F, 1.0F, 0.0F, -0.34906585F, 0.0F));
        bb.addChild("right_flank_plate_2", ModelPartBuilder.create().uv(90, 43).cuboid(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, Dilation.NONE), ModelTransform.of(-8.0F, -14.0F, 1.5F, 0.0F, 0.34906585F, 0.34906585F));
        bb.addChild("left_flank_plate_2", ModelPartBuilder.create().uv(106, 43).cuboid(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, Dilation.NONE), ModelTransform.of(8.0F, -14.0F, 1.5F, 0.0F, -0.34906585F, -0.34906585F));
        bb.addChild("right_flank_ivory_edge_2", ModelPartBuilder.create().uv(2, 50).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(-10.0F, -13.5F, 1.0F, 0.0F, 0.34906585F, 0.34906585F));
        bb.addChild("left_flank_ivory_edge_2", ModelPartBuilder.create().uv(12, 50).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(10.0F, -13.5F, 1.0F, 0.0F, -0.34906585F, -0.34906585F));
        bb.addChild("right_brow_talon_root", ModelPartBuilder.create().uv(22, 50).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, Dilation.NONE), ModelTransform.of(-4.0F, -18.0F, 1.0F, 0.0F, 0.0F, -0.34906585F));
        bb.addChild("left_brow_talon_root", ModelPartBuilder.create().uv(32, 50).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, Dilation.NONE), ModelTransform.of(4.0F, -18.0F, 1.0F, 0.0F, 0.0F, 0.34906585F));
        bb.addChild("right_brow_talon_tip", ModelPartBuilder.create().uv(42, 50).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, Dilation.NONE), ModelTransform.of(-5.5F, -20.5F, 0.5F, 0.0F, 0.0F, -0.610865238F));
        bb.addChild("left_brow_talon_tip", ModelPartBuilder.create().uv(48, 50).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, Dilation.NONE), ModelTransform.of(5.5F, -20.5F, 0.5F, 0.0F, 0.0F, 0.610865238F));
        bb.addChild("right_keel_talon_root", ModelPartBuilder.create().uv(54, 50).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, Dilation.NONE), ModelTransform.of(-3.0F, -3.0F, 1.0F, 0.0F, 0.0F, 0.34906585F));
        bb.addChild("left_keel_talon_root", ModelPartBuilder.create().uv(64, 50).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, Dilation.NONE), ModelTransform.of(3.0F, -3.0F, 1.0F, 0.0F, 0.0F, -0.34906585F));
        bb.addChild("right_keel_talon_tip", ModelPartBuilder.create().uv(74, 50).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, Dilation.NONE), ModelTransform.of(-3.5F, -0.5F, -0.5F, 0.0F, 0.0F, 0.488692191F));
        bb.addChild("left_keel_talon_tip", ModelPartBuilder.create().uv(80, 50).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, Dilation.NONE), ModelTransform.of(3.5F, -0.5F, -0.5F, 0.0F, 0.0F, -0.488692191F));
        bb.addChild("keel_living_cable", ModelPartBuilder.create().uv(86, 50).cuboid(-1.0F, -3.0F, 1.0F, 2.0F, 3.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("rear_vertebra_0", ModelPartBuilder.create().uv(96, 50).cuboid(-1.0F, -6.0F, 5.0F, 2.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("rear_vertebra_1", ModelPartBuilder.create().uv(104, 50).cuboid(-1.0F, -9.0F, 5.0F, 2.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("rear_vertebra_2", ModelPartBuilder.create().uv(112, 50).cuboid(-1.0F, -12.0F, 5.0F, 2.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("rear_vertebra_3", ModelPartBuilder.create().uv(120, 50).cuboid(-1.0F, -15.0F, 5.0F, 2.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_rear_shell_lobe", ModelPartBuilder.create().uv(2, 58).cuboid(-2.5F, -5.0F, -1.0F, 5.0F, 10.0F, 2.0F, Dilation.NONE), ModelTransform.of(-3.5F, -10.0F, 5.0F, 0.0F, -0.261799388F, 0.0F));
        bb.addChild("left_rear_shell_lobe", ModelPartBuilder.create().uv(18, 58).cuboid(-2.5F, -5.0F, -1.0F, 5.0F, 10.0F, 2.0F, Dilation.NONE), ModelTransform.of(3.5F, -10.0F, 5.0F, 0.0F, 0.261799388F, 0.0F));
        bb.addChild("right_rear_shell_inlay", ModelPartBuilder.create().uv(34, 58).cuboid(-5.0F, -14.0F, 5.0F, 1.0F, 8.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("left_rear_shell_inlay", ModelPartBuilder.create().uv(42, 58).cuboid(4.0F, -14.0F, 5.0F, 1.0F, 8.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_mantle_rear_plate_0", ModelPartBuilder.create().uv(50, 58).cuboid(-2.5F, -1.5F, -1.5F, 5.0F, 3.0F, 3.0F, Dilation.NONE), ModelTransform.of(-9.5F, -7.5F, 3.5F, 0.0F, 0.34906585F, -0.436332313F));
        bb.addChild("left_mantle_rear_plate_0", ModelPartBuilder.create().uv(68, 58).cuboid(-2.5F, -1.5F, -1.5F, 5.0F, 3.0F, 3.0F, Dilation.NONE), ModelTransform.of(9.5F, -7.5F, 3.5F, 0.0F, -0.34906585F, 0.436332313F));
        bb.addChild("right_mantle_ivory_spine_0", ModelPartBuilder.create().uv(86, 58).cuboid(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(-12.5F, -7.5F, 3.0F, 0.0F, 0.34906585F, -0.436332313F));
        bb.addChild("left_mantle_ivory_spine_0", ModelPartBuilder.create().uv(98, 58).cuboid(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(12.5F, -7.5F, 3.0F, 0.0F, -0.34906585F, 0.436332313F));
        bb.addChild("right_mantle_rear_plate_1", ModelPartBuilder.create().uv(110, 58).cuboid(-2.5F, -1.5F, -1.5F, 5.0F, 3.0F, 3.0F, Dilation.NONE), ModelTransform.of(-9.5F, -11.5F, 3.5F, 0.0F, 0.34906585F, 0.0F));
        bb.addChild("left_mantle_rear_plate_1", ModelPartBuilder.create().uv(2, 72).cuboid(-2.5F, -1.5F, -1.5F, 5.0F, 3.0F, 3.0F, Dilation.NONE), ModelTransform.of(9.5F, -11.5F, 3.5F, 0.0F, -0.34906585F, 0.0F));
        bb.addChild("right_mantle_ivory_spine_1", ModelPartBuilder.create().uv(20, 72).cuboid(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(-12.5F, -11.5F, 3.0F, 0.0F, 0.34906585F, 0.0F));
        bb.addChild("left_mantle_ivory_spine_1", ModelPartBuilder.create().uv(32, 72).cuboid(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(12.5F, -11.5F, 3.0F, 0.0F, -0.34906585F, 0.0F));
        bb.addChild("right_mantle_rear_plate_2", ModelPartBuilder.create().uv(44, 72).cuboid(-2.5F, -1.5F, -1.5F, 5.0F, 3.0F, 3.0F, Dilation.NONE), ModelTransform.of(-9.5F, -15.5F, 3.5F, 0.0F, 0.34906585F, 0.436332313F));
        bb.addChild("left_mantle_rear_plate_2", ModelPartBuilder.create().uv(62, 72).cuboid(-2.5F, -1.5F, -1.5F, 5.0F, 3.0F, 3.0F, Dilation.NONE), ModelTransform.of(9.5F, -15.5F, 3.5F, 0.0F, -0.34906585F, -0.436332313F));
        bb.addChild("right_mantle_ivory_spine_2", ModelPartBuilder.create().uv(80, 72).cuboid(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(-12.5F, -15.5F, 3.0F, 0.0F, 0.34906585F, 0.436332313F));
        bb.addChild("left_mantle_ivory_spine_2", ModelPartBuilder.create().uv(92, 72).cuboid(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, Dilation.NONE), ModelTransform.of(12.5F, -15.5F, 3.0F, 0.0F, -0.34906585F, -0.436332313F));
        bb.addChild("right_high_crown_root", ModelPartBuilder.create().uv(104, 72).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(-3.0F, -19.5F, 3.0F, 0.0F, 0.0F, -0.314159265F));
        bb.addChild("left_high_crown_root", ModelPartBuilder.create().uv(114, 72).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(3.0F, -19.5F, 3.0F, 0.0F, 0.0F, 0.314159265F));
        bb.addChild("right_high_crown_prong", ModelPartBuilder.create().uv(2, 81).cuboid(-0.5F, -2.0F, -0.5F, 1.0F, 4.0F, 1.0F, Dilation.NONE), ModelTransform.of(-4.5F, -23.0F, 2.5F, 0.0F, 0.0F, -0.41887902F));
        bb.addChild("left_high_crown_prong", ModelPartBuilder.create().uv(8, 81).cuboid(-0.5F, -2.0F, -0.5F, 1.0F, 4.0F, 1.0F, Dilation.NONE), ModelTransform.of(4.5F, -23.0F, 2.5F, 0.0F, 0.0F, 0.41887902F));
        bb.addChild("crown_median_root", ModelPartBuilder.create().uv(14, 81).cuboid(-1.0F, -23.0F, 2.0F, 2.0F, 6.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("crown_median_tip", ModelPartBuilder.create().uv(24, 81).cuboid(-1.0F, -27.0F, 2.0F, 2.0F, 4.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("crown_crimson_groove", ModelPartBuilder.create().uv(32, 81).cuboid(-0.5F, -24.0F, 1.0F, 1.0F, 5.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("right_orbital_clasp", ModelPartBuilder.create().uv(38, 81).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(-6.0F, -11.0F, -5.5F, 0.0F, 0.0F, -0.34906585F));
        bb.addChild("left_orbital_clasp", ModelPartBuilder.create().uv(46, 81).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, Dilation.NONE), ModelTransform.of(6.0F, -11.0F, -5.5F, 0.0F, 0.0F, 0.34906585F));
        bb.addChild("right_jaw_guard", ModelPartBuilder.create().uv(54, 81).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(-4.0F, -2.5F, -2.0F, 0.0F, 0.0F, 0.436332313F));
        bb.addChild("left_jaw_guard", ModelPartBuilder.create().uv(64, 81).cuboid(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.of(4.0F, -2.5F, -2.0F, 0.0F, 0.0F, -0.436332313F));
        bb.addChild("right_jaw_needle", ModelPartBuilder.create().uv(74, 81).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, Dilation.NONE), ModelTransform.of(-4.5F, 0.5F, -2.5F, 0.0F, 0.0F, 0.436332313F));
        bb.addChild("left_jaw_needle", ModelPartBuilder.create().uv(80, 81).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, Dilation.NONE), ModelTransform.of(4.5F, 0.5F, -2.5F, 0.0F, 0.0F, -0.436332313F));
        bb.addChild("rear_median_crest", ModelPartBuilder.create().uv(86, 81).cuboid(-1.0F, -16.0F, 6.0F, 2.0F, 11.0F, 2.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        bb.addChild("rear_crest_inlay", ModelPartBuilder.create().uv(96, 81).cuboid(-0.5F, -15.0F, 8.0F, 1.0F, 9.0F, 1.0F, Dilation.NONE), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return TexturedModelData.of(data,128,128);
    }
    @Override public void render(MatrixStack matrices,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha) { bb_main.render(matrices,vertices,light,overlay,red,green,blue,alpha); }
    @Override public void setAngles(GreaterDreadglareEntity entity,float limbAngle,float limbDistance,float animationProgress,float headYaw,float headPitch) {}
}
