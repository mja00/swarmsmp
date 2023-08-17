package dev.mja00.swarmsmps2.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel extends Level implements WorldGenLevel {

    @Final
    @Shadow
    private ServerLevelData serverLevelData;

    @Final
    @Shadow
    private MinecraftServer server;

    protected MixinServerLevel(WritableLevelData pLevelData, ResourceKey<Level> pDimension, Holder<DimensionType> pDimensionTypeRegistration, Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed) {
        super(pLevelData, pDimension, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug, pBiomeZoomSeed);
    }

    @Inject(method = "advanceWeatherCycle()V", at = @At("HEAD"), cancellable = true)
    @Unique
    public void swarmsmps2$customWeatherCycle(CallbackInfo ci) {
        if (this.dimensionType().hasSkyLight()) {
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                int clearWeatherTime = this.serverLevelData.getClearWeatherTime();
                int thunderTime = this.serverLevelData.getThunderTime();
                int rainTime = this.serverLevelData.getRainTime();
                boolean levelDataThundering = this.levelData.isThundering();
                boolean levelDataRaining = this.levelData.isRaining();
                // If there is clear weather time left, decrement it and don't let other weather happen
                if (clearWeatherTime > 0) {
                    // We decrement it, and set the thunderTime to 0 if it's thundering, or 1 if it's not
                    --clearWeatherTime;
                    thunderTime = levelDataThundering ? 0 : 1;
                    rainTime = levelDataRaining ? 0 : 1;
                    levelDataThundering = false;
                    levelDataRaining = false;
                } else {
                    // Otherwise we start decrementing the thunderTime and rainTime till one of those is 0
                    if (thunderTime > 0) {
                        // We decrement it, and invert the thundering state if it's 0
                        --thunderTime;
                        if (thunderTime == 0) {
                            levelDataThundering = !levelDataThundering;
                        }
                    } else if (levelDataThundering) {
                        // If it is thundering we set it to a random value between 3 minutes and 13 minutes (3600 and 15600 ticks)
                        thunderTime = Mth.randomBetweenInclusive(this.random, 180 * 20, 780 * 20);
                    } else {
                        // Otherwise we set it to a random value between 10 minutes and 2 and a half hours (12000 and 180000 ticks)
                        thunderTime = Mth.randomBetweenInclusive(this.random, 600 * 20, 9000 * 20);
                    }

                    // If rainTime is greater than 0
                    if (rainTime > 0) {
                        // We decrement it, and invert the raining state if it's 0
                        --rainTime;
                        if (rainTime == 0) {
                            levelDataRaining = !levelDataRaining;
                        }
                    } else if (levelDataRaining) {
                        // If it is raining we set it to a random value between 10 minutes and 20 minutes (12000 and 24000 ticks)
                        rainTime = Mth.randomBetweenInclusive(this.random, 600 * 20, 1200 * 20);
                    } else {
                        // Otherwise we set it to a random value between 10 minutes and 2 and a half hours (12000 and 180000 ticks)
                        rainTime = Mth.randomBetweenInclusive(this.random, 600 * 20, 9000 * 20);
                    }
                }

                // Then we set all the needed data for the next tick
                this.serverLevelData.setThunderTime(thunderTime);
                this.serverLevelData.setRainTime(rainTime);
                this.serverLevelData.setClearWeatherTime(clearWeatherTime);
                this.serverLevelData.setThundering(levelDataThundering);
                this.serverLevelData.setRaining(levelDataRaining);
            }

            // Every time we advance the weather cycle we either increment or decrement the thunderLevel
            this.oThunderLevel = this.thunderLevel;
            if (this.levelData.isThundering()) {
                this.thunderLevel += 0.01F;
            } else {
                this.thunderLevel -= 0.01F;
            }
            // Clamp thunder between 0 and 1
            this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);

            // Same with rain
            this.oRainLevel = this.rainLevel;
            if (this.levelData.isRaining()) {
                this.rainLevel += 0.01F;
            } else {
                this.rainLevel -= 0.01F;
            }
            this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
        }

        // If either level changed, we send the packet to the client, this'll make their client keep sync
        if (this.oRainLevel != this.rainLevel) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }

        /* The function in use here has been replaced in order to only send the weather info to players in the correct dimension,
         * rather than to all players on the server. This is what causes the client-side rain, as the
         * client believes that it has started raining locally, rather than in another dimension.
         */
        boolean raining = this.isRaining();
        if (raining != this.isRaining()) {
            if (raining) {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F), this.dimension());
            } else {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F), this.dimension());
            }

            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }
    // Cancel the original method
    ci.cancel();
    }
}
