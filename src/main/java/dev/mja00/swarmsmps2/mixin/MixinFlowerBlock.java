package dev.mja00.swarmsmps2.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Random;

@Mixin(FlowerBlock.class)
public abstract class MixinFlowerBlock extends BushBlock implements BonemealableBlock {

    public MixinFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
        return pState.is(BlockTags.SMALL_FLOWERS);
    }

    @Override
    public boolean isBonemealSuccess(Level pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
        swarmsmp_s2$bedrockEditionBehavior(pLevel, pRandom, pPos);
    }

    @Unique
    private void swarmsmp_s2$bedrockEditionBehavior(ServerLevel level, Random random, BlockPos pos) {
        final int maxSuccesses = swarmsmp_s2$getRandomIntInclusive(1, 7, random);
        int successCounter = 0;
        for (int i = 0; i < 64 && successCounter < maxSuccesses; i++) {
            BlockPos newPos = pos;
            for (int j = 0; j < i / 22 + 1; j++) {
                newPos = newPos.offset(
                        swarmsmp_s2$getRandomIntInclusive(-1, 1, random),
                        0,
                        swarmsmp_s2$getRandomIntInclusive(-1, 1, random)
                );
            }
            newPos = newPos.offset(0, swarmsmp_s2$getRandomIntInclusive(-1, 1, random), 0);
            final BlockPos below = newPos.below();
            if (this.mayPlaceOn(level.getBlockState(below), level, below)) {
                if (level.getBlockState(newPos).isAir()) {
                    level.setBlock(newPos, this.defaultBlockState(), 1 | 2);
                    swarmsmp_s2$addGrowthParticles(level, newPos, random.nextInt(14));
                    successCounter++;
                }
            }
        }
    }

    // Copied from BoneMealItem and modified to work from the server side.
    @Unique
    private static void swarmsmp_s2$addGrowthParticles(ServerLevel pLevel, BlockPos pPos, int pData) {
        if (pData == 0) {
            pData = 15;
        }

        BlockState blockstate = pLevel.getBlockState(pPos);
        double d0 = 0.5D;
        double d1;
        if (blockstate.is(Blocks.WATER)) {
            pData *= 3;
            d1 = 1.0D;
            d0 = 3.0D;
        } else if (blockstate.isSolidRender(pLevel, pPos)) {
            pPos = pPos.above();
            pData *= 3;
            d0 = 3.0D;
            d1 = 1.0D;
        } else {
            d1 = blockstate.getShape(pLevel, pPos).max(Direction.Axis.Y);
        }

        pLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, (double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        Random randomsource = pLevel.getRandom();

        for(int i = 0; i < pData; ++i) {
            double d2 = randomsource.nextGaussian() * 0.02D;
            double d3 = randomsource.nextGaussian() * 0.02D;
            double d4 = randomsource.nextGaussian() * 0.02D;
            double d9 = randomsource.nextGaussian() * 0.02D;
            double d5 = 0.5D - d0;
            double d6 = (double)pPos.getX() + d5 + randomsource.nextDouble() * d0 * 2.0D;
            double d7 = (double)pPos.getY() + randomsource.nextDouble() * d1;
            double d8 = (double)pPos.getZ() + d5 + randomsource.nextDouble() * d0 * 2.0D;
            // If the block below d6, d7, d8 is not air, send particles
            if (!pLevel.getBlockState(new BlockPos(d6, d7, d8).below()).isAir()) {
                pLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, d6, d7, d8, 1, d2, d3, d4, d9);
            }
        }
    }

    @Unique
    private int swarmsmp_s2$getRandomIntInclusive(int lower, int upper, Random random) {
        return random.nextInt(upper - lower + 1) + lower;
    }
}
