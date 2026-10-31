package com.pulsetools.addon.modules;

import com.pulsetools.addon.PulseTools;
import com.pulsetools.addon.util.ScanUtils;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChunkFinder extends Module {
    public enum LineColor {
        Green,
        Blue,
        Custom
    }

    private static class Flags {
        boolean spawner;
        boolean amethyst;
        long playerUntil;
        boolean alerted;
    }

    private static final Color GREEN = new Color(0, 255, 0, 255);
    private static final Color BLUE = new Color(0, 120, 255, 255);

    private final SettingGroup sgRender = settings.getDefaultGroup();
    private final SettingGroup sgDetect = settings.createGroup("Detection");
    private final SettingGroup sgAlerts = settings.createGroup("Alerts");

    // Render
    private final Setting<LineColor> lineColor = sgRender.add(new EnumSetting.Builder<LineColor>()
        .name("line-color")
        .description("Color of the chunk line.")
        .defaultValue(LineColor.Green)
        .build()
    );

    private final Setting<SettingColor> customColor = sgRender.add(new ColorSetting.Builder()
        .name("custom-color")
        .description("Color used when line-color is set to Custom.")
        .defaultValue(new SettingColor(0, 255, 0, 255))
        .visible(() -> lineColor.get() == LineColor.Custom)
        .build()
    );

    // Detection
    private final Setting<Boolean> detectSpawners = sgDetect.add(new BoolSetting.Builder()
        .name("spawners")
        .description("Flags chunks that contain spawners underground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> spawnerMaxY = sgDetect.add(new IntSetting.Builder()
        .name("spawner-max-y")
        .description("Only spawners at or below this Y level count (0 = deepslate level).")
        .defaultValue(0)
        .range(-64, 320)
        .sliderRange(-64, 64)
        .visible(() -> detectSpawners.get())
        .build()
    );

    private final Setting<Boolean> detectAmethyst = sgDetect.add(new BoolSetting.Builder()
        .name("amethyst-clusters")
        .description("Flags chunks that contain fully grown amethyst clusters.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> amethystMinCount = sgDetect.add(new IntSetting.Builder()
        .name("min-amethyst-clusters")
        .description("How many fully grown clusters a chunk needs before it is flagged.")
        .defaultValue(1)
        .range(1, 64)
        .sliderRange(1, 16)
        .visible(() -> detectAmethyst.get())
        .build()
    );

    private final Setting<Boolean> detectPlayers = sgDetect.add(new BoolSetting.Builder()
        .name("player-activity")
        .description("Flags the chunk of every other player you can see.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> playerSeconds = sgDetect.add(new IntSetting.Builder()
        .name("player-memory-seconds")
        .description("How long a chunk stays flagged after a player was last seen in it.")
        .defaultValue(30)
        .range(1, 600)
        .sliderRange(5, 120)
        .visible(() -> detectPlayers.get())
        .build()
    );

    private final Setting<Integer> rescanSeconds = sgDetect.add(new IntSetting.Builder()
        .name("rescan-seconds")
        .description("How often loaded chunks are checked again (catches amethyst growing later).")
        .defaultValue(10)
        .range(1, 120)
        .sliderRange(1, 60)
        .build()
    );

    // Alerts
    private final Setting<Boolean> sound = sgAlerts.add(new BoolSetting.Builder()
        .name("sound")
        .description("Plays a small sound when a chunk gets flagged.")
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

    private final Map<ChunkPos, Flags> flagged = new HashMap<>();
    private final Map<ChunkPos, Long> lastScan = new HashMap<>();
    private int ticks;

    public ChunkFinder() {
        super(PulseTools.CATEGORY, "chunk-finder", "Marks chunks with underground spawners, fully grown amethyst clusters or player activity with a line from bedrock to the build limit.");
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
        flagged.clear();
        lastScan.clear();
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
        long now = System.currentTimeMillis();

        // Player activity
        if (detectPlayers.get()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                Flags flags = flagged.computeIfAbsent(player.getChunkPos(), k -> new Flags());
                flags.playerUntil = now + playerSeconds.get() * 1000L;
            }
        }

        // Block scanning (spawners + amethyst)
        if (ticks % 10 == 0 && (detectSpawners.get() || detectAmethyst.get())) {
            for (WorldChunk chunk : ScanUtils.dueChunks(lastScan, rescanSeconds.get() * 1000L, 64)) {
                scanChunk(chunk);
            }
        }

        // Update alerts / clean up
        boolean newFlag = false;
        Iterator<Map.Entry<ChunkPos, Flags>> it = flagged.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, Flags> entry = it.next();
            ChunkPos pos = entry.getKey();
            Flags flags = entry.getValue();

            boolean active = (detectSpawners.get() && flags.spawner)
                || (detectAmethyst.get() && flags.amethyst)
                || (detectPlayers.get() && now < flags.playerUntil);

            if (!active || mc.world.getChunkManager().getWorldChunk(pos.x, pos.z) == null) {
                it.remove();
                continue;
            }

            if (!flags.alerted) {
                flags.alerted = true;
                newFlag = true;
            }
        }

        if (newFlag && sound.get()) ScanUtils.ping(volume.get().floatValue());
    }

    private void scanChunk(WorldChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = mc.world.getBottomY();
        int maxY = minY + mc.world.getHeight() - 1;

        boolean hasSpawner = false;
        boolean hasAmethyst = false;

        if (detectSpawners.get()) {
            int[] count = {0};
            ScanUtils.forEachBlock(chunk, state -> state.isOf(Blocks.SPAWNER), minY, Math.min(maxY, spawnerMaxY.get()), p -> count[0]++);
            hasSpawner = count[0] > 0;
        }

        if (detectAmethyst.get()) {
            int[] count = {0};
            ScanUtils.forEachBlock(chunk, state -> state.isOf(Blocks.AMETHYST_CLUSTER), minY, maxY, p -> count[0]++);
            hasAmethyst = count[0] >= amethystMinCount.get();
        }

        Flags flags = flagged.get(pos);
        if (flags == null) {
            if (!hasSpawner && !hasAmethyst) return;
            flags = new Flags();
            flagged.put(pos, flags);
        }

        flags.spawner = hasSpawner;
        flags.amethyst = hasAmethyst;
    }

    private Color getColor() {
        return switch (lineColor.get()) {
            case Green -> GREEN;
            case Blue -> BLUE;
            case Custom -> customColor.get();
        };
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || flagged.isEmpty()) return;

        double bottom = mc.world.getBottomY();
        double top = bottom + mc.world.getHeight();
        Color color = getColor();

        for (ChunkPos pos : flagged.keySet()) {
            double x = pos.getStartX() + 8;
            double z = pos.getStartZ() + 8;
            event.renderer.line(x, bottom, z, x, top, z, color);
        }
    }
}
