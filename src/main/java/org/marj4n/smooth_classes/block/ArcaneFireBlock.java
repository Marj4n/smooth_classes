package org.marj4n.smooth_classes.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.marj4n.smooth_classes.registry.SmoothParticles;

/** Temporary damaging fire; scheduled block ticks survive chunk saves/reloads. */
public final class ArcaneFireBlock extends Block {
    public static final int LIFETIME_TICKS = 80;
    public static final float CONTACT_DAMAGE = 2F;

    public ArcaneFireBlock(Settings settings) { super(settings); }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos,
                             BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this))
            world.scheduleBlockTick(pos, this, LIFETIME_TICKS);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBlockState(pos).isOf(this))
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos below = pos.down();
        return !world.getBlockState(below).getCollisionShape(world, below).isEmpty()
                && world.getFluidState(pos).isEmpty();
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
            BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return canPlaceAt(state, world, pos) ? state : Blocks.AIR.getDefaultState();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity && entity.isAlive()) {
            // Deliberately no owner/team exclusion or fire-immunity check.
            // Normal hurt cooldown prevents multiple tiles multiplying damage.
            entity.damage(world.getDamageSources().magic(), CONTACT_DAMAGE);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(12) == 0)
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS,
                    0.5F, 0.8F + random.nextFloat() * 0.3F, false);
        world.addParticle(SmoothParticles.ARCANE_FLAME,
                pos.getX() + random.nextDouble(), pos.getY() + 0.4,
                pos.getZ() + random.nextDouble(), 0, 0.025, 0);
    }
}
