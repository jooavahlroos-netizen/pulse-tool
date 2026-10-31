package com.pulsetools.addon;

import com.pulsetools.addon.modules.ChunkFinder;
import com.pulsetools.addon.modules.SpawnerFinder;
import com.pulsetools.addon.modules.SwingSpeed;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class PulseTools extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Pulse Tools");

    @Override
    public void onInitialize() {
        LOG.info("[Pulse Tools] Initializing");

        register("swing-speed", SwingSpeed::new);
        register("chunk-finder", ChunkFinder::new);
        register("spawner-finder", SpawnerFinder::new);

        LOG.info("[Pulse Tools] Done");
    }

    private static void register(String name, Supplier<Module> factory) {
        try {
            Modules.get().add(factory.get());
            LOG.info("[Pulse Tools] Loaded module {}", name);
        } catch (Throwable t) {
            LOG.error("[Pulse Tools] Failed to load module " + name, t);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.pulsetools.addon";
    }
}
