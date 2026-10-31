package com.pulsetools.addon.util;

import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ScanUtils {
    private ScanUtils() {}

    /**
     * Returns loaded chunks around the player that should be (re)scanned now.
     * Never-scanned chunks come first, then chunks whose last scan is older than intervalMs.
     */
    public static List<WorldChunk> dueChunks(Map<ChunkPos, Long> lastScan, long intervalMs, int max) {
        List<WorldChunk> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;

        long now = System.currentTimeMillis();

        // Forget chunks that are no longer loaded
        lastScan.keySet().removeIf(p -> mc.world.getChunkManager().getWorldChunk(p.x, p.z) == null);

        int radius = mc.options.getViewDistance().getValue();
        ChunkPos center = mc.player.getChunkPos();
        List<WorldChunk> rescans = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                ChunkPos pos = new ChunkPos(cx, cz);

                Long last = lastScan.get(pos);
                if (last != null && now - last < intervalMs) continue;

                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                if (last == null) {
                    out.add(chunk);
                    lastScan.put(pos, now);
                    if (out.size() >= max) return out;
                } else {
                    rescans.add(chunk);
                }
            }
        }

        for (WorldChunk chunk : rescans) {
            if (out.size() >= max) break;
            out.add(chunk);
            lastScan.put(chunk.getPos(), now);
        }

        return out;
    }

    /** Calls consumer for every block in the chunk (between minY and maxY inclusive) that matches filter. */
    public static void forEachBlock(WorldChunk chunk, Predicate<BlockState> filter, int minY, int maxY, Consumer<BlockPos> consumer) {
        ChunkSection[] sections = chunk.getSectionArray();
        int bottomSection = chunk.getBottomSectionCoord();
        ChunkPos cp = chunk.getPos();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) continue;

            int baseY = (bottomSection + i) << 4;
            if (baseY + 15 < minY || baseY > maxY) continue;
            if (!section.hasAny(filter)) continue;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    int wy = baseY + y;
                    if (wy < minY || wy > maxY) continue;

                    for (int z = 0; z < 16; z++) {
                        if (filter.test(section.getBlockState(x, y, z))) {
                            consumer.accept(new BlockPos(cp.getStartX() + x, wy, cp.getStartZ() + z));
                        }
                    }
                }
            }
        }
    }

    /** Small notification ping. */
    public static void ping(float volume) {
        if (mc.player == null) return;
        mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, volume, 1.4f);
    }
}
