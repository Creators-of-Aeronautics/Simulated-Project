package dev.simulated_team.simulated.mixin.hold_interaction;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyMapping.class)
public interface KeyMappingInvoker {
    @Invoker
    void invokeRelease();
}
