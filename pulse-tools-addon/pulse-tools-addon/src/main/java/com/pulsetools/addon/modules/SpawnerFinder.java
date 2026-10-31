package com.pulsetools.addon.modules;

import com.pulsetools.addon.PulseTools;
import com.pulsetools.addon.util.ScanUtils;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SpawnerFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAlerts = settings.createGroup("Alerts");

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the line going from the spawner up to the build limit.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Setting<Boolean> chat = sgAlerts.add(new BoolSetting.Builder()
        .name("chat-coordinates")
        .description("Posts the spawner coordinates in chat when one is found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sound = sgAlerts.add(new BoolSetting.Builder()
        .name("sound")
        .description("Plays a small sound when a new spawner is found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> volume = sgAlerts.add(new DoubleSetting.Builder()
        .name("volume")
        .description("Volume of the alert sound.")
        .defaultValue(0.4)
        .range(0, 1)
        .sliderRange(0, 1)
        .visible(() -> sound.get())
        .build()
    );

    private final Map<ChunkPos, Long> lastScan = new HashMap<>();
    private final Set<BlockPos> spawners = new HashSet<>();
    private final Set<BlockPos> announced = new HashSet<>();
    private int ticks;

    public SpawnerFinder() {
        super(PulseTools.CATEGORY, "spawner-finder", "Draws a line from every spawner up to the build limit and posts its coordinates in chat.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        lastScan.clear();
        spawners.clear();
        announced.clear();
        ticks = 0;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        ticks++;
        if (ticks % 10 != 0) return;

        // Drop spawners that were broken or are no longer loaded
        Iterator<BlockPos> it = spawners.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!mc.world.getBlockState(pos).isOf(Blocks.SPAWNER)) it.remove();
        }

        int minY = mc.world.getBottomY();
        int maxY = minY + mc.world.getHeight() - 1;
        boolean foundNew = false;

        for (WorldChunk chunk : ScanUtils.dueChunks(lastScan, 10_000L, 64)) {
            Set<BlockPos> found = new HashSet<>();
            ScanUtils.forEachBlock(chunk, state -> state.isOf(Blocks.SPAWNER), minY, maxY, found::add);

            for (BlockPos pos : found) {
                spawners.add(pos);

                if (announced.add(pos)) {
                    foundNew = true;
                    if (chat.get()) info("Spawner found at X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }

        if (foundNew && sound.get()) ScanUtils.ping(volume.get().floatValue());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || spawners.isEmpty()) return;

        double top = mc.world.getBottomY() + mc.world.getHeight();

        for (BlockPos pos : spawners) {
            double x = pos.getX() + 0.5;
            double z = pos.getZ() + 0.5;
            event.renderer.line(x, pos.getY() + 0.5, z, x, top, z, lineColor.get());
        }
    }
}
