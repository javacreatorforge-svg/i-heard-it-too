package com.redstonedev.iheardittoo.event;

import com.redstonedev.iheardittoo.IHeardItToo;
import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import com.redstonedev.iheardittoo.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class ForgeEvents {

    private static final Random RNG = new Random();
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % 100 != 0) return; // ~5s cadence
        if (event.getServer() == null) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            trySpawn(level);
        }
    }

    private boolean hasWraith(ServerLevel level) {
        return !level.getEntities(ModEntities.MATERNAL_WRAITH.get(), w -> !w.isRemoved()).isEmpty();
    }

    private void trySpawn(ServerLevel level) {
        List<? extends ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        if (hasWraith(level)) return; // one Sarah per dimension - she's the only one

        for (ServerPlayer player : players) {
            // Time + weather gating:
            //   night surface or any cave -> normal rate
            //   thunderstorm              -> bumped rate (even more)
            //   day surface               -> rare
            boolean underground = isPlayerUnderground(level, player);
            boolean night = !level.isDay() || underground;
            boolean thunder = level.isThundering();

            int chance;
            if (thunder)         chance = 150;  // 1-in-150 / 5s = avg ~12 min
            else if (night)      chance = 300;  // 1-in-300 / 5s = avg ~25 min
            else                 chance = 1200; // 1-in-1200 / 5s = avg ~100 min (day surface, rare)

            if (RNG.nextInt(chance) != 0) continue;

            BlockPos spawnPos;
            if (underground) {
                spawnPos = pickCaveSpawnPos(level, player);
            } else {
                spawnPos = pickSurfaceSpawnPos(level, player);
            }
            if (spawnPos == null) continue;

            MaternalWraithEntity wraith = ModEntities.MATERNAL_WRAITH.get().create(level);
            if (wraith == null) return;
            wraith.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360F, 0);
            wraith.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.EVENT, null, null);
            level.addFreshEntity(wraith);

            IHeardItToo.LOGGER.debug("Spawned Sarah near {} (thunder={}, night={}, underground={})",
                    player.getName().getString(), thunder, night, underground);
            return;
        }
    }

    private boolean isPlayerUnderground(Level level, ServerPlayer p) {
        BlockPos pos = p.blockPosition();
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        return p.getY() < surface - 8;
    }

    private BlockPos pickSurfaceSpawnPos(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0;
            double dist  = 16 + RNG.nextInt(16); // 16-32 blocks out
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (isValidSpawn(level, candidate)) return candidate;
        }
        return null;
    }

    private BlockPos pickCaveSpawnPos(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 30; attempt++) {
            int dx = RNG.nextInt(33) - 16;
            int dz = RNG.nextInt(33) - 16;
            int dy = RNG.nextInt(11) - 5;
            BlockPos candidate = origin.offset(dx, dy, dz);
            if (Math.abs(dx) < 6 && Math.abs(dz) < 6) continue; // too close
            if (isValidSpawn(level, candidate)) return candidate;
        }
        return null;
    }

    private boolean isValidSpawn(ServerLevel level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        return here.isAir() && above.isAir() && !below.isAir();
    }
}
