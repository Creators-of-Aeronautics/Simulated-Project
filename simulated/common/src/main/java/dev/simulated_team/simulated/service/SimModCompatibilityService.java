package dev.simulated_team.simulated.service;

import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * The constructor of this class must be empty and NOT reference the mod compatibility. All render logic should be handled in {@link #init()}.
 *
 * @author KyanBirb
 */
public interface SimModCompatibilityService {

    /**
     * This will only be called if the mod is loaded.
     */
    void init();

    /**
     */
    String getModId();

    @ApiStatus.Internal
    static void initLoaded() {
        final ServiceLoader<SimModCompatibilityService> loader = ServiceLoader.load(SimModCompatibilityService.class, SimModCompatibilityService.class.getClassLoader());
        final Iterator<SimModCompatibilityService> iterator = loader.iterator();

        while (iterator.hasNext()) {
            try {
                final SimModCompatibilityService service = iterator.next();
                if(SimPlatformService.INSTANCE.isLoaded(service.getModId())) {
                    service.init();
                }
            } catch (final ServiceConfigurationError | NoClassDefFoundError ignored) {

            }
        }
    }
}
