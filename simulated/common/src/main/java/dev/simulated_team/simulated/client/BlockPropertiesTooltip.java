package dev.simulated_team.simulated.client;

import com.simibubi.create.AllKeys;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimRegistries;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.mixin.accessor.BlockBehaviourAccessor;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.util.SimColors;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class BlockPropertiesTooltip {
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();

    private static final Component NONE = Component.translatable("simulated.tooltip.mass.none").withStyle(ChatFormatting.GRAY);
    private static final Component SUPER_LIGHT = Component.translatable("simulated.tooltip.mass.super_light").withStyle(ChatFormatting.AQUA);
    private static final Component LIGHT = Component.translatable("simulated.tooltip.mass.light").withStyle(ChatFormatting.GREEN);
    private static final Component HEAVY = Component.translatable("simulated.tooltip.mass.heavy").withStyle(ChatFormatting.YELLOW);
    private static final Component SUPER_HEAVY = Component.translatable("simulated.tooltip.mass.super_heavy").withColor(SimColors.NUH_UH_RED);
    private static final Component ABSURDLY_HEAVY = Component.translatable("simulated.tooltip.mass.absurdly_heavy").withColor(SimColors.NUH_UH_RED);
    private static final Component BOUNCY = Component.translatable("simulated.tooltip.bouncy").withStyle(ChatFormatting.GREEN);
    private static final Component SLIPPERY = Component.translatable("simulated.tooltip.friction.slippery").withStyle(ChatFormatting.AQUA);
    private static final Component STICKY = Component.translatable("simulated.tooltip.friction.sticky").withStyle(ChatFormatting.DARK_GREEN);
    private static final Component FRAGILE = Component.translatable("simulated.tooltip.fragile").withColor(SimColors.NUH_UH_RED);
    private static final Component AIRTIGHT = Component.translatable("simulated.tooltip.airtight").withStyle(ChatFormatting.WHITE);
    private static final Component FLOATING = Component.translatable("simulated.tooltip.floating").withStyle(ChatFormatting.DARK_GREEN);

    static {
        DECIMAL_FORMAT.setDecimalSeparatorAlwaysShown(false);
        DECIMAL_FORMAT.setMaximumFractionDigits(2);
        DECIMAL_FORMAT.setMinimumIntegerDigits(1);
    }

    public static boolean shouldShowTooltip(final Condition condition, final TooltipFlag iTooltipFlag, final @Nullable Player player) {
        if (Minecraft.getInstance().screen instanceof PonderUI) {
            return true;
        }

        if (player == null) {
            return condition.allows();
        }

        return condition.test(AllKeys.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT), GogglesItem.isWearingGoggles(player));
    }

    public static void register(final SimulatedRegistrate registrate, final String name, final TooltipFunction tooltipFunction, final float priority) {
        registrate.propertyTooltip(name, () -> new Entry(tooltipFunction, priority));
    }

    public static void init() {
        final SimulatedRegistrate registrate = Simulated.getRegistrate();
        int priority = 0;

        register(registrate, "mass", BlockPropertiesTooltip::getMassComponent, priority++);
        register(registrate, "friction", BlockPropertiesTooltip::getFrictionComponent, priority++);
        register(registrate, "restitution", BlockPropertiesTooltip::getRestitutionComponent, priority++);
        register(registrate, "fragile", BlockPropertiesTooltip::getFragileComponent, priority++);
        register(registrate, "airtight", BlockPropertiesTooltip::getAirtightComponent, priority++);
        register(registrate, "floating", BlockPropertiesTooltip::getFloatingComponent, priority++);
    }

    public static void appendTooltip(final ItemStack stack, final TooltipFlag iTooltipFlag, final Player player, final List<Component> itemTooltip) {
        if (stack.getItem() instanceof final BlockItem blockItem) {
            final boolean showNumbers = true;
            final BlockStateExtension properties = ((BlockStateExtension) blockItem.getBlock().defaultBlockState());
            final List<Component> toAdd = new ObjectArrayList<>();

            SimRegistries.PROPERTY_TOOLTIP.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(f -> {
                final Component component = f.getValue().tooltipFunction.apply(properties, blockItem, showNumbers);
                if (component != null) {
                    toAdd.add(component);
                }
            });

            if (!toAdd.isEmpty()) {
                for (final Component property : toAdd) {
                    itemTooltip.add(Component.literal(" ").append(property));
                }
            }
        }
    }

    public static Component getMassComponent(final BlockStateExtension properties, final BlockItem item, final boolean showNumbers) {
        final double mass = ((BlockBehaviourAccessor) item.getBlock()).getHasCollision() ?
                properties.sable$getProperty(PhysicsBlockPropertyTypes.MASS.get()) :
                0;

        Component comp;
        if (mass == 1) {
            return null;
        } else if (mass <= 0) {
            comp = NONE;
        } else if (mass <= 0.25) {
            comp = SUPER_LIGHT;
        } else if (mass <= 0.5) {
            comp = LIGHT;
        } else if (mass < 4) {
            comp = HEAVY;
        } else if (mass < 50) {
            comp = SUPER_HEAVY;
        } else {
            comp = ABSURDLY_HEAVY;
        }

        if (showNumbers) {
            return Component.empty().append(comp).append(formatValue("simulated.unit.mass", mass).withStyle(ChatFormatting.DARK_GRAY));
        }
        return comp;
    }

    public static @Nullable Component getRestitutionComponent(final BlockStateExtension properties, final BlockItem item, final boolean showNumbers) {
        final double restitution = properties.sable$getProperty(PhysicsBlockPropertyTypes.RESTITUTION.get());

        if (restitution == 0) {
            return null;
        }

        if (showNumbers) {
            return Component.empty().append(BOUNCY).append(formatValue("simulated.unit.restitution", restitution * 100.0).withStyle(ChatFormatting.DARK_GRAY));
        }
        return BOUNCY;
    }

    public static @Nullable Component getFrictionComponent(final BlockStateExtension properties, final BlockItem item, final boolean showNumbers) {
        final double friction = properties.sable$getProperty(PhysicsBlockPropertyTypes.FRICTION.get());

        if (friction == 1) {
            return null;
        }

        Component comp;
        if (friction < 1) {
            comp = SLIPPERY;
        } else {
            comp = STICKY;
        }

        if (showNumbers) {
            return Component.empty().append(comp).append(formatValue("simulated.unit.friction", friction).withStyle(ChatFormatting.DARK_GRAY));
        }
        return comp;
    }

    public static @Nullable Component getFragileComponent(final BlockStateExtension properties, final BlockItem item, final boolean showNumbers) {
        final boolean fragile = properties.sable$getProperty(PhysicsBlockPropertyTypes.FRAGILE.get());
        if (fragile) {
            return FRAGILE;
        }
        return null;
    }

    public static @Nullable Component getAirtightComponent(final BlockStateExtension properties, final BlockItem item, final boolean showNumbers) {
        if (item.getBlock().defaultBlockState().is(SimTags.Blocks.AIRTIGHT)) {
            return AIRTIGHT;
        }
        return null;
    }

    public static @Nullable Component getFloatingComponent(final BlockStateExtension properties, final BlockItem item, final boolean showNumbers) {
        final ResourceLocation materialID = properties.sable$getProperty(PhysicsBlockPropertyTypes.FLOATING_MATERIAL.get());
        if (materialID == null) {
            return null;
        }

        final FloatingBlockMaterial material = FloatingBlockMaterialDataHandler.allMaterials.get(materialID);
        if (material == null) {
            return null;
        }

        final double materialScale = properties.sable$getProperty(PhysicsBlockPropertyTypes.FLOATING_SCALE.get());
        final double liftStrength = material.liftStrength() * materialScale;
        if (liftStrength <= 0) {
            return null;
        }

        if (showNumbers) {
            return Component.empty().append(FLOATING).append(formatValue("simulated.unit.floating", liftStrength).withStyle(ChatFormatting.DARK_GRAY));
        }
        return FLOATING;
    }

    private static MutableComponent formatValue(final String key, final double value) {
        final String valueString = DECIMAL_FORMAT.format(value);
        return Component.literal(" (").append(Component.translatable(key, valueString)).append(")");
    }

    public enum Condition {
        ALWAYS(true, false, false),
        SHIFT(true, true, false),
        GOGGLES(true, false, true),
        SHIFT_GOGGLES(true, true, true),
        NEVER(false, false, false);

        private final boolean allow;
        private final boolean requireShift;
        private final boolean requireGoggles;

        Condition(final boolean allow, final boolean requireShift, final boolean requireGoggles) {
            this.allow = allow;
            this.requireShift = requireShift;
            this.requireGoggles = requireGoggles;
        }

        public boolean test(final boolean shift, final boolean goggles) {
            return this.allow && (!this.requireShift || shift) && (!this.requireGoggles || goggles);
        }

        public boolean allows() {
            return this.allow;
        }
    }

    @FunctionalInterface
    public interface TooltipFunction {
        @Nullable
        Component apply(BlockStateExtension properties, BlockItem item, boolean showNumbers);
    }

    /**
     * lower priority = higher up in the list
     */
    public record Entry(TooltipFunction tooltipFunction, float priority) implements Comparable<Entry> {
        @Override
        public int compareTo(@NotNull final BlockPropertiesTooltip.Entry o) {
            return Float.compare(this.priority, o.priority);
        }
    }
}
