package org.marj4n.smooth_classes.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;

/**
 * Vampire daytime coffin. V1 keeps it intentionally deterministic: using it in
 * daylight starts a short blackout/rest phase and advances the world to night.
 */
public final class VampireCoffinBlock extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public VampireCoffinBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        if (OriginRuntime.state(serverPlayer).origin() != OriginType.VAMPIRE) {
            serverPlayer.sendMessage(Text.literal("Only a Vampire can rest in this coffin."), true);
            return ActionResult.CONSUME;
        }
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.CONSUME;
        if (serverWorld.isNight()) {
            serverPlayer.sendMessage(Text.literal("The night is already yours."), true);
            return ActionResult.CONSUME;
        }

        var origin = OriginRuntime.state(serverPlayer);
        long now = serverWorld.getTime();
        origin.longProgress("vampire.coffin_rest_until", now + 60L);
        origin.longProgress("vampire.coffin_x", pos.getX());
        origin.longProgress("vampire.coffin_y", pos.getY());
        origin.longProgress("vampire.coffin_z", pos.getZ());
        serverPlayer.sendMessage(Text.literal("You retreat into the coffin until nightfall..."), true);
        return ActionResult.CONSUME;
    }
}
