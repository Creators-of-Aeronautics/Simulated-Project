package dev.eriksonn.aeronautics.content.blocks.hot_air;


import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.SavedBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * Physics-related interface for {@link net.minecraft.world.level.block.entity.BlockEntity BlockEntities} that can produce hot air for hot air balloons.
 */
public interface BlockEntityLiftingGasProvider {

    static double getPredictedVolume(final ClientBalloonInfo info, final int ticksSinceSync) {
        double volumeInterp = info.clientBalloonFilled + info.clientBalloonChange * ticksSinceSync;

        // avoid imprecision or div by 0, the outcome would be nearly 0 anyways
        if (Math.abs(info.clientBalloonFilled - info.clientBalloonTarget - info.clientBalloonChange) > 0.01) {
            // recreating the discrete exponential decay performed serverside
            // f[n+1] = r * (f[n] - Target) + Target
            // given f[0] = Filled, f[0] = f[-1] + Change
            // r = (Filled - Target) / (Filled - Target - Change)
            // with hot air burners, r = 0.995, but the system has potential for gases with different filling/emptying times
            final double r = (info.clientBalloonFilled - info.clientBalloonTarget) / (info.clientBalloonFilled - info.clientBalloonTarget - info.clientBalloonChange);
            volumeInterp = Math.pow(r, ticksSinceSync) * (info.clientBalloonFilled - info.clientBalloonTarget) + info.clientBalloonTarget;
        }
        return volumeInterp;
    }

    static MutableComponent barComponent(final int amount, final int target, final int total) {
        final int lower = Math.min(amount, target - 1);
        final int upper = Math.max(amount - target, 0);
        return Component.empty()
                .append(bars(Math.max(0, lower), ChatFormatting.DARK_AQUA))
                .append(bars(Math.max(0, target - lower - 1), ChatFormatting.DARK_GRAY))
                .append(bars(target == 0 ? 0 : 1, ChatFormatting.GOLD))
                .append(bars(upper, ChatFormatting.DARK_AQUA))
                .append(bars(Math.max(0, total - target - upper), ChatFormatting.DARK_GRAY));

    }

    private static MutableComponent bars(final int count, final ChatFormatting format) {
        return Component.literal(Strings.repeat('|', count))
                .withStyle(format);
    }

    default BlockPos getRaycastedPosition(final Level level, final Vec3 rayStart, final Vec3 rayEnd) {
        final BlockHitResult clip = level.clip(new ClipContext(rayStart, rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));

        final BlockPos hitBlockPos = clip.getBlockPos();
        if (clip.getType() == HitResult.Type.MISS || !level.getBlockState(hitBlockPos).is(AeroTags.BlockTags.AIRTIGHT))
            return null;

        return hitBlockPos.relative(clip.getDirection());
    }

    Balloon getBalloon();

    void setBalloon(Balloon balloon);

    /**
     * Attempts to "join" a balloon, if one already exists at the position we would floodfill to create one
     */
    default void tryJoinBalloon() {
        if (this.getBalloon() != null)
            return;

        final BlockPos castPos = this.getCastPosition();

        if (castPos != null) {
            final Level level = this.getLevel();
            if (level != null) {
                final BalloonMap balloonMap = BalloonMap.MAP.get(level);
                if (balloonMap != null) {
                    final Balloon existingBalloon = balloonMap.getBalloon(castPos);

                    if (existingBalloon != null) {
                        // Yip yip!
                        existingBalloon.addHeater(this);
                        this.setBalloon(existingBalloon);
                    }
                }
            }
        }
    }

    /**
     * Attempts to create a balloon
     */
    default void tryCreateBalloon() {
        if (this.getBalloon() != null)
            return;

        final Level level = this.getLevel();
        final BlockPos castPos = this.getCastPosition();

        final BalloonMap balloonMap = BalloonMap.MAP.get(level);

        if (castPos == null) {
            return;
        }

        final Balloon newBalloon = BalloonBuilder.attemptBuildBalloon(this, castPos);

        if (newBalloon == null) {
            return;
        }

        // handle the case that we're creating a balloon exactly where one was unloaded
        if (newBalloon instanceof final ServerBalloon serverBalloon) {
            final Iterable<SavedBalloon> unloadedBalloons = balloonMap.getUnloadedBalloons();
            final Iterator<SavedBalloon> iter = unloadedBalloons.iterator();

            // TODO: some sort of spatial index or chunk-storage of these would be nice
            //  so that we aren't just scanning all of them for a potential match
            while (iter.hasNext()) {
                final SavedBalloon unloaded = iter.next();
                final BalloonLayerGraph graph = newBalloon.getGraph();

                if (graph.hasBlockAt(unloaded.controllerPos())) {
                    serverBalloon.loadFrom(unloaded);
                    balloonMap.markDirty();
                    iter.remove();
                    break;
                }
            }
        }

        this.setBalloon(newBalloon);
        balloonMap.addBalloon(newBalloon);
    }

    default void removeFromBalloon() {
        // remove us from the balloon
        final Balloon balloon = this.getBalloon();

        if (balloon instanceof final ServerBalloon serverBalloon) {
            balloon.removeHeater(this);

            // we're the last heater un-loading, take the balloon with us
            // it's just like the scorpion and the frog

            if (this.isChunkUnloaded() && balloon.getHeaters().isEmpty()) {
                final Level level = this.getLevel();
                assert level != null;

                final BalloonMap balloonMap = BalloonMap.MAP.get(level);
                if (balloonMap != null) {
                    balloonMap.unloadBalloon(serverBalloon);
                }
            }

            this.setBalloon(null);
        }
    }

    default void addBalloonGoggleInformation(final List<Component> tooltip, final ClientBalloonInfo info, final int ticksSinceSync, final double airPressure) {
        if (info != null) {
            final int totalVolume = info.clientBalloonVolume;
            if (totalVolume == 0) {
                AeroLang.translate("lifting_gas.no_suitable_balloon").style(ChatFormatting.RED)
                        .forGoggles(tooltip, 2);
                return;
            }

            final MutableComponent gasOutputComponent = AeroLang.translate("unit.meter_cubed", String.format("%.2f", this.getGasOutput())).style(ChatFormatting.AQUA).component();
            AeroLang.translate("lifting_gas.gas_output", this.getLiftingGasType().getName(), gasOutputComponent).style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);

            AeroLang.emptyLine(tooltip);
            AeroLang.translate("lifting_gas.balloon").forGoggles(tooltip, 1);

            final int totalBar = 30;
            final int targetBar = (int) Math.ceil(totalBar * info.clientBalloonTarget / totalVolume);
            final double volumeInterp = getPredictedVolume(info, ticksSinceSync);
            final int volumeBar = Mth.clamp((int) Math.ceil(totalBar * volumeInterp / totalVolume), 0, totalBar);

            final MutableComponent base = barComponent(volumeBar, targetBar, totalBar);
            AeroLang.translate("lifting_gas.fill", base).style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);

            double lift = info.clientBalloonLift * airPressure;
            if (info.clientBalloonFilled > 0.01) {
                // lift is linear with volume, so we can use the fractional change in volume to also dynamically update lift
                lift *= volumeInterp / info.clientBalloonFilled;
            }

            final MutableComponent liftComponent = AeroLang.kilopixelGram(lift)
                    .style(ChatFormatting.AQUA).component();

            AeroLang.translate("lifting_gas.total_lift", liftComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);

            final MutableComponent balloonVolumeComponent = AeroLang.translate("unit.meter_cubed", totalVolume)
                    .style(ChatFormatting.AQUA).component();
            AeroLang.translate("lifting_gas.balloon_volume", balloonVolumeComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);
        }
    }

    default double getAirPressure(final ClientBalloonInfo balloonInfo, final Level level) {
        final Vector3dc globalPosition = Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(balloonInfo.gasCenter()));
        return DimensionPhysicsData.getAirPressure(level, globalPosition);
    }

    @Nullable
    BlockPos getCastPosition();

    @Nullable
    void doRaycast();

    /**
     * Gets the current hot air output of the object.
     */
    double getGasOutput();

    LiftingGasType getLiftingGasType();

    boolean canOutputGas();

    double getClientPredictedVolume();

    BlockPos getBlockPos();

    Level getLevel();

    boolean isChunkUnloaded();

    default void tickBalloonLogic() {
        this.doRaycast();

        if (this.getBalloon() == null)
            this.tryJoinBalloon();

        if (this.getBalloon() == null)
            this.tryCreateBalloon();

        if (this.getBalloon() instanceof final ServerBalloon balloon && balloon.getTotalFilledVolume() > 1) {
            AeroAdvancements.HEAD_IN_THE_CLOUDS.awardToNearby(this.getBlockPos(), this.getLevel());
        }
    }

    record ClientBalloonInfo(int clientBalloonVolume, double clientBalloonFilled,
                             double clientBalloonTarget, double clientBalloonLift,
                             double clientBalloonChange, Vec3 gasCenter) {
        public static void writeToNBT(final CompoundTag tag, final ServerBalloon balloon) {
            if (balloon != null && balloon.getCenter() != null) {
                tag.putInt("Volume", balloon.getCapacity());
                tag.putDouble("Filled", balloon.getTotalFilledVolume());
                tag.putDouble("Target", balloon.getTotalTargetVolume());
                tag.putDouble("Delta", balloon.getTotalVolumeChange());
                tag.putDouble("Lift", balloon.getTotalLift());
                tag.putDouble("CenterX", balloon.getCenter().x);
                tag.putDouble("CenterY", balloon.getCenter().y);
                tag.putDouble("CenterZ", balloon.getCenter().z);
            }
        }

        public static ClientBalloonInfo readFromNBT(final CompoundTag tag) {
            return new ClientBalloonInfo(
                    tag.getInt("Volume"),
                    tag.getDouble("Filled"),
                    tag.getDouble("Target"),
                    tag.getDouble("Lift"),
                    tag.getDouble("Delta"),
                    new Vec3(
                            tag.getDouble("CenterX"),
                            tag.getDouble("CenterY"),
                            tag.getDouble("CenterZ")
                    ));
        }
    }
}