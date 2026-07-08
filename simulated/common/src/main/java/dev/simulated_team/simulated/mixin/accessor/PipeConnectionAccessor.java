package dev.simulated_team.simulated.mixin.accessor;

import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.PipeConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(PipeConnection.class)
public interface PipeConnectionAccessor {

    @Accessor("source")
    void simulated$setSource(Optional<FlowSource> source);

    @Accessor("flow")
    void simulated$setFlow(Optional<PipeConnection.Flow> flow);
}
