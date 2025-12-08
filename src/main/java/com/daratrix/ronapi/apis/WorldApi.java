package com.daratrix.ronapi.apis;

import com.daratrix.ronapi.models.ApiWorld;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IPlayerWidget;
import com.daratrix.ronapi.models.interfaces.IResource;
import com.daratrix.ronapi.models.interfaces.IStructure;
import com.daratrix.ronapi.models.savedata.WorldApiResourceSaveData;
import com.daratrix.ronapi.timer.Timer;
import com.daratrix.ronapi.timer.TimerServerEvents;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldApi {

    private static ApiWorld singleton = null;

    public static ApiWorld getSingleton() {
        if (singleton == null) {
            System.out.println("Created ApiWorld singleton");
            singleton = new ApiWorld();
        }
        return singleton;
    }

    private static final Timer gridScanTimer = TimerServerEvents.queueTimerLooping(0, WorldApi::doGridScan).pause();
    private static boolean gridScanRunning = false;
    private static int gridScanSize;
    private static int gridScanProgress;
    private static int gridScanTarget;

    public static void startGridScan(int size) {
        synchronized (gridScanTimer) {
            if (gridScanRunning) {
                System.err.println("Grid scan is already running!");
                return;
            }

            getSingleton().resources.clear();
            gridScanRunning = true;
            gridScanSize = size;
            gridScanTarget = (gridScanSize * 2) * (gridScanSize * 2);
            gridScanProgress = 0;
            System.out.println("Starting grid scan with size: " + gridScanSize + "(" + gridScanTarget + " chunks to scan)");
            terrainHeightCache.clear();
            gridScanTimer.unpause();
        }
    }

    public static void doGridScan(MinecraftServer server) {
        if(server.getPlayerCount() == 0) {
            return; // can't scan yet
        }

        int steps = 5;
        while (steps > 0 && !gridScanStep(server)) {
            --steps;
        }
    }

    public static boolean gridScanStep(MinecraftServer server) {
        if (gridScanProgress == gridScanTarget) {
            System.out.println("Grid scan completed! (size: " + gridScanSize + ")");

            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(Component.literal("Grid scan completed! (size: " + gridScanSize + ")"), false);
            }

            gridScanRunning = false;
            gridScanTimer.pause();

            WorldApiResourceSaveData.getInstance(server.overworld()).save();

            return true; // stop running
        }

        var x = gridScanProgress % (gridScanSize * 2);
        var z = gridScanProgress / (gridScanSize * 2);
        x -= gridScanSize;
        z -= gridScanSize;

        if(!getSingleton().scanChunk(server, x, z)) {
            return true; // stop running for now
        }

        // display progress
        if (gridScanProgress % 50 == 0) {
            System.out.printf("%d %% scanned...\r\n", (100 * gridScanProgress / gridScanTarget));
        }

        ++gridScanProgress;
        return false; // keep going!
    }

    private static Timer queuedWorldUpdate = null;

    public static void queueWorldUpdate() {
        if (queuedWorldUpdate == null) {
            queuedWorldUpdate = TimerServerEvents.queueTimer(0, WorldApi::updateWorld); // queue a world update ASAP
        }
    }

    public static void updateWorld(MinecraftServer server) {
        getSingleton().updateTracking(server);
        TimerServerEvents.destroyTimer(queuedWorldUpdate);
        queuedWorldUpdate = null;
    }

    public static boolean hasResourcesWithinDistance(IStructure w, float distance) {
        return getSingleton().resources.values().stream().anyMatch(r -> GeometryUtils.isWithinDistance(r, w, distance));
    }

    public static boolean hasResourcesWithinDistanceOfType(IStructure w, float distance, int type) {
        return getSingleton().resources.values().stream().filter(r -> type == r.getTypeId()).anyMatch(r -> GeometryUtils.isWithinDistance(r, w, distance));
    }

    public static boolean hasResourcesWithinDistanceOfType(IStructure w, float distance, int... types) {
        return getSingleton().resources.values().stream().filter(r -> Arrays.stream(types).anyMatch(t -> t == r.getTypeId())).anyMatch(r -> GeometryUtils.isWithinDistance(r, w, distance));
    }

    public static IResource getResourceAt(BlockPos pos) {
        return getSingleton().resources.getOrDefault(pos, null);
    }

    public static int getTerrainHeight(Level level, BlockPos.MutableBlockPos pos) {
        if (level == null) {
            return 0;
        }

        var chunk = level.getChunkAt(pos);
        return getTerrainHeight(level, chunk, pos);
    }

    private static HashMap<Vec3i, Integer> terrainHeightCache = new HashMap<Vec3i, Integer>();

    public static int getTerrainHeight(Level level, ChunkAccess chunk, BlockPos.MutableBlockPos pos) {
        // from cache, dig down in case blocks are destroyed
        if (terrainHeightCache.containsKey(pos)) {
            var maxY = terrainHeightCache.get(pos);
            var y = getTerrainHeight(chunk, pos, maxY, maxY - 20);
            if (y != maxY) {
                terrainHeightCache.put(pos, y);
            }

            return y;
        }

        // from yLevel - yLevel is expected to be the lowest level units can reach
        var yLevelRule = level.getGameRules().getRule(GameRuleRegistrar.GROUND_Y_LEVEL);
        if (yLevelRule != null) {
            int maxY = yLevelRule.get() + 30;
            int minY = yLevelRule.get() - 2;
            var y = getTerrainHeight(chunk, pos, maxY, minY);
            terrainHeightCache.put(pos, y);

            return y;
        }

        // default large scan
        var y = getTerrainHeight(chunk, pos, 96, 64);
        terrainHeightCache.put(pos, y);

        return y;
    }


    public static int getTerrainHeight(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int maxY, int minY) {
        int y = maxY;

        while (y >= minY) {
            pos.setY(y);
            var block = chunk.getBlockState(pos);
            if (!block.isAir() && block.isSolid()) {
                break;
            }
            --y;
        }

        return y;
    }

    public static boolean playerCanAfford(String ownerName, ResourceCost itemCost) {
        IPlayer player = getSingleton().players.getOrDefault(ownerName, null);
        if (player == null) {
            return false;
        }

        return player.canAfford(itemCost);
    }

    public static Stream<IPlayer> getPlayerEnemies(IPlayer p) {
        return singleton.players.values().stream()
                .filter(x -> x != p && !x.isDefeated() && !AlliancesServerEvents.isAllied(x.getName(), p.getName()))
                .map(x -> (IPlayer) x);
    }

    public static Stream<IPlayer> getPlayerEnemies(String playerName) {
        return singleton.players.values().stream()
                .filter(x -> !Objects.equals(x.getName(), playerName) && !x.isDefeated() && !AlliancesServerEvents.isAllied(x.getName(), playerName))
                .map(x -> x);
    }

    public static IPlayer getPlayer(String playerName) {
        return singleton.players.getOrDefault(playerName, null);
    }

    private static String getThreatsPlayerName = null;

    private static boolean getThreatsPredicate(LivingEntity e) {
        if (e instanceof Unit u) {
            var owner = u.getOwnerName();
            return !Objects.equals(owner, getThreatsPlayerName) && !AlliancesServerEvents.isAllied(getThreatsPlayerName, u.getOwnerName());
        }
        return false;
    }

    public static void findThreats(AABB boundingBox, String playerName, Collection<IPlayerWidget> outputThreats) {
        var enemies = getPlayerEnemies(playerName);
        enemies.forEach(p -> {
            p.getUnitsFiltered(u -> !u.isWorker()).filter(u -> GeometryUtils.contains(boundingBox, u.getPos())).collect(Collectors.toCollection(() -> outputThreats));
        });
    }

    public static boolean isNearPlayerBases(IPlayerWidget defenseTarget, IPlayer player, float distance) {
        return player.getUnitsFiltered(b -> GeometryUtils.isWithinDistance(b, defenseTarget, distance)).findAny().isPresent();
    }
}
