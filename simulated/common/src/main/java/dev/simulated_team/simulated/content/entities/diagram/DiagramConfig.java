package dev.simulated_team.simulated.content.entities.diagram;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.simulated_team.simulated.util.SimCodecUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.List;

public class DiagramConfig {

    public static final Codec<DiagramConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(ResourceLocation.CODEC).fieldOf("enabled_force_groups").forGetter(DiagramConfig::enabledForceGroups),
            Codec.BOOL.fieldOf("display_center_of_mass").forGetter(DiagramConfig::displayCenterOfMass),
            Codec.BOOL.fieldOf("merge_forces").forGetter(DiagramConfig::mergeForces),
            Codec.DOUBLE.fieldOf("yaw").forGetter(DiagramConfig::yaw),
            Codec.DOUBLE.fieldOf("pitch").forGetter(DiagramConfig::pitch),
            NoteConfigs.NOTE_CONFIG_CODEC.fieldOf("note").forGetter(DiagramConfig::getNoteConfigs),
            Codec.BOOL.optionalFieldOf("display_center_of_lift", false).forGetter(DiagramConfig::displayCenterOfLift)
    ).apply(instance, DiagramConfig::new));

    public static final StreamCodec<ByteBuf, DiagramConfig> STREAM_CODEC = NeoForgeStreamCodecs.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), DiagramConfig::enabledForceGroups,
            ByteBufCodecs.BOOL, DiagramConfig::displayCenterOfMass,
            ByteBufCodecs.BOOL, DiagramConfig::mergeForces,
            ByteBufCodecs.DOUBLE, DiagramConfig::yaw,
            ByteBufCodecs.DOUBLE, DiagramConfig::pitch,
            NoteConfigs.NOTE_CONFIG_STREAM_CODEC, DiagramConfig::getNoteConfigs,
            ByteBufCodecs.BOOL, DiagramConfig::displayCenterOfLift,
            DiagramConfig::new);

    private final List<ResourceLocation> enabledForceGroups;
    private boolean displayCenterOfMass;
    private boolean mergeForces;
    private double yaw;
    private double pitch;
    private boolean displayCenterOfLift;

    private final NoteConfigs noteConfig;

    public static DiagramConfig makeDefault(final DiagramEntity entity) {
        final ObjectList<ResourceLocation> enabledForceGroups = new ObjectArrayList<>();

        for (final ResourceLocation groupId : ForceGroups.REGISTRY.keySet()) {
            if (ForceGroups.REGISTRY.get(groupId).defaultDisplayed())
                enabledForceGroups.add(groupId);
        }

        final NoteConfigs noteConfig = new NoteConfigs(new BoundingBox3d(), -entity.getYRot(), entity.getXRot(), false);
        return new DiagramConfig(enabledForceGroups, false, false, -entity.getYRot(), entity.getXRot(), noteConfig, false);
    }

    public DiagramConfig(final List<ResourceLocation> enabledForceGroups,
                         final boolean displayCenterOfMass,
                         final boolean mergeForces,
                         final double yaw,
                         final double pitch,
                         final NoteConfigs noteConfig,
                         final boolean displayCenterOfLift) {
        this.enabledForceGroups = enabledForceGroups;
        this.displayCenterOfMass = displayCenterOfMass;
        this.mergeForces = mergeForces;
        this.yaw = yaw;
        this.pitch = pitch;
        this.displayCenterOfLift = displayCenterOfLift;

        this.noteConfig = noteConfig;
    }

    public List<ResourceLocation> enabledForceGroups() {
        return this.enabledForceGroups;
    }

    public boolean displayCenterOfMass() {
        return this.displayCenterOfMass;
    }

    public boolean mergeForces() {
        return this.mergeForces;
    }

    public double yaw() {
        return this.yaw;
    }

    public double pitch() {
        return this.pitch;
    }

    public boolean displayCenterOfLift() {
        return this.displayCenterOfLift;
    }

    public void setDisplayCenterOfMass(final boolean displayCenterOfMass) {
        this.displayCenterOfMass = displayCenterOfMass;
    }

    public void setDisplayCenterOfLift(final boolean displayCenterOfLift) {
        this.displayCenterOfLift = displayCenterOfLift;
    }

    public void setMergeForces(final boolean mergeForces) {
        this.mergeForces = mergeForces;
    }

    public void setYaw(final double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(final double pitch) {
        this.pitch = pitch;
    }

    public NoteConfigs getNoteConfigs() {
        return this.noteConfig;
    }

    public static final class NoteConfigs {
        public static final Codec<NoteConfigs> NOTE_CONFIG_CODEC = RecordCodecBuilder.create(i ->
                i.group(BoundingBox3d.CODEC.fieldOf("note_scope").forGetter(NoteConfigs::getNoteScope),
                        Codec.DOUBLE.fieldOf("note_yaw").forGetter(NoteConfigs::getNoteYaw),
                        Codec.DOUBLE.fieldOf("note_pitch").forGetter(NoteConfigs::getNotePitch),
                        Codec.BOOL.fieldOf("note_active").forGetter(NoteConfigs::isActive)
                ).apply(i, NoteConfigs::new));

        public static final StreamCodec<ByteBuf, NoteConfigs> NOTE_CONFIG_STREAM_CODEC = StreamCodec.composite(
                SimCodecUtil.BOUNDING_BOX_3D_STREAM_CODEC, NoteConfigs::getNoteScope,
                ByteBufCodecs.DOUBLE, NoteConfigs::getNoteYaw,
                ByteBufCodecs.DOUBLE, NoteConfigs::getNotePitch,
                ByteBufCodecs.BOOL, NoteConfigs::isActive,
                NoteConfigs::new
        );

        private final BoundingBox3d noteScope;
        private double noteYaw;
        private double notePitch;
        private boolean active;

        public NoteConfigs(final BoundingBox3d noteScope, final double noteYaw, final double notePitch, final boolean active) {
            this.noteScope = noteScope;
            this.noteYaw = noteYaw;
            this.notePitch = notePitch;
            this.active = active;
        }

        public BoundingBox3d getNoteScope() {
            return this.noteScope;
        }

        public double getNotePitch() {
            return this.notePitch;
        }

        public double getNoteYaw() {
            return this.noteYaw;
        }

        public boolean isActive() {
            return this.active;
        }

        public void setActive(final boolean active) {
            this.active = active;
        }

        public void setNotePitch(final double notePitch) {
            this.notePitch = notePitch;
        }

        public void setNoteYaw(final double noteYaw) {
            this.noteYaw = noteYaw;
        }
    }
}
