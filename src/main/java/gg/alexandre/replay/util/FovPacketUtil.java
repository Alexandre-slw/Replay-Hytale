package gg.alexandre.replay.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.assets.UpdateFluidFX;
import com.hypixel.hytale.protocol.packets.assets.UpdateFluids;
import com.hypixel.hytale.protocol.packets.world.ServerSetFluids;
import com.hypixel.hytale.protocol.packets.world.SetFluidCmd;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.*;

public final class FovPacketUtil {

    private static final double MIN_FOV = 0.10;
    private static final double MAX_FOV = 2.00;
    private static final double STEP = 0.01;
    private static final int COUNT = (int) Math.round((MAX_FOV - MIN_FOV) / STEP) + 1;

    private static final double NEUTRAL_FOV = 1.0;

    private static final byte FULL_FLUID_LEVEL = 15;

    private final int fluidFxBaseId =
            com.hypixel.hytale.server.core.asset.type.fluidfx.config.FluidFX.getAssetMap().getNextIndex();

    private final int fluidBaseId =
            com.hypixel.hytale.server.core.asset.type.fluid.Fluid.getAssetMap().getNextIndex();

    private final PlayerRef playerRef;
    private final World world;

    private boolean setup;

    private int currentFluidId = -1;
    private final Map<BlockCell, FluidState> cells = new LinkedHashMap<>();

    public FovPacketUtil(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        this.playerRef = playerRef;
        this.world = world;
    }

    public void apply(double requestedFovModifier, @Nonnull Vector3d playerPosition) {
        if (requestedFovModifier == NEUTRAL_FOV) {
            clear();
            return;
        }

        int fluidId = fluidIdFor(requestedFovModifier);

        Set<BlockCell> targetCells = cellsFor(playerPosition);

        if (fluidId == currentFluidId && cells.keySet().equals(targetCells)) {
            return;
        }

        Map<BlockCell, FluidState> actualStates = new LinkedHashMap<>();
        for (BlockCell cell : targetCells) {
            actualStates.put(cell, getActualFluidState(cell));
        }

        Map<BlockCell, FluidState> restorePackets = new LinkedHashMap<>();
        for (Map.Entry<BlockCell, FluidState> entry : cells.entrySet()) {
            if (!targetCells.contains(entry.getKey())) {
                restorePackets.put(entry.getKey(), entry.getValue());
            }
        }

        if (!restorePackets.isEmpty()) {
            sendFluidStatePackets(restorePackets);
        }

        Map<BlockCell, FluidState> packets = new LinkedHashMap<>();
        for (BlockCell cell : targetCells) {
            packets.put(cell, new FluidState(fluidId, FULL_FLUID_LEVEL));
        }
        sendFluidStatePackets(packets);

        cells.clear();
        cells.putAll(actualStates);
        currentFluidId = fluidId;
    }

    public void clear() {
        if (cells.isEmpty()) {
            currentFluidId = -1;
            return;
        }

        sendFluidStatePackets(cells);
        cells.clear();
        currentFluidId = -1;
    }

    public void setup() {
        if (setup) {
            return;
        }

        Map<Integer, FluidFX> fxMap = new HashMap<>();
        Map<Integer, Fluid> fluidMap = new HashMap<>();

        for (int i = 0; i < COUNT; i++) {
            float modifier = (float) (MIN_FOV + i * STEP);
            int fxId = fluidFxBaseId + i;
            int fluidId = fluidBaseId + i;

            fxMap.put(fxId, buildFluidFx("fov_fx_" + i, modifier));
            fluidMap.put(fluidId, buildInvisibleFluid("fov_fluid_" + i, fxId));
        }

        playerRef.getPacketHandler().writeNoCache(
                new UpdateFluidFX(UpdateType.AddOrUpdate, fluidFxBaseId + COUNT, fxMap)
        );
        playerRef.getPacketHandler().writeNoCache(
                new UpdateFluids(UpdateType.AddOrUpdate, fluidBaseId + COUNT, fluidMap)
        );

        setup = true;
    }

    @Nonnull
    private FluidFX buildFluidFx(@Nonnull String id, float fovModifier) {
        FluidFX fx = new FluidFX();
        fx.id = id;
        fx.shader = ShaderType.None;

        fx.fogMode = FluidFog.EnvironmentTint;
        fx.fogDistance = new NearFar(10000.0f, 10001.0f);
        fx.fogDepthStart = 10000.0f;
        fx.fogDepthFalloff = 1.0f;

        fx.colorFilter = new Color((byte) 200, (byte) 210, (byte) 230);
        fx.colorSaturation = 1.0f;

        fx.distortionAmplitude = 0;
        fx.distortionFrequency = 0;

        fx.particle = null;
        fx.movementSettings = new FluidFXMovementSettings(
                1.0f,
                1.0f,
                0,
                1.0f,
                fovModifier,
                1.0f
        );

        return fx;
    }

    @Nonnull
    private Fluid buildInvisibleFluid(@Nonnull String id, int fluidFxIndex) {
        Fluid fluid = new Fluid();
        fluid.id = id;
        fluid.maxFluidLevel = 15;
        fluid.requiresAlphaBlending = false;
        fluid.opacity = Opacity.Solid;
        fluid.shaderEffect = new ShaderType[]{ShaderType.None};
        fluid.light = new ColorLight((byte) 0, (byte) 0, (byte) 0, (byte) 0);
        fluid.particles = new ModelParticle[0];
        fluid.drawType = FluidDrawType.None;
        fluid.fluidFXIndex = fluidFxIndex;
        fluid.blockSoundSetIndex = 0;
        fluid.blockParticleSetId = null;
        fluid.particleColor = null;
        fluid.tagIndexes = new int[0];
        return fluid;
    }

    @Nonnull
    private Set<BlockCell> cellsFor(Vector3d playerPosition) {
        int x = (int) Math.floor(playerPosition.x);
        int y = (int) Math.floor(playerPosition.y);
        int z = (int) Math.floor(playerPosition.z);

        LinkedHashSet<BlockCell> cells = new LinkedHashSet<>();

        for (int offsetY = -1; offsetY < 4; offsetY++) {
            for (int offsetX = -1; offsetX < 3; offsetX++) {
                for (int offsetZ = -1; offsetZ < 3; offsetZ++) {
                    int cellY = y + offsetY;
                    if (cellY >= 0 && cellY < 320) {
                        cells.add(new BlockCell(x + offsetX, cellY, z + offsetZ));
                    }
                }
            }
        }

        return cells;
    }

    @Nonnull
    private FluidState getActualFluidState(@Nonnull BlockCell cell) {
        if (cell.y < 0 || cell.y >= 320) {
            return FluidState.EMPTY;
        }

        Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(cell.x, cell.y, cell.z);
        FluidSection fluidSection = sectionRef == null ? null :
                world.getChunkStore().getStore().getComponent(sectionRef, FluidSection.getComponentType());

        int fluidId = fluidSection == null ? 0 : fluidSection.getFluidId(cell.x, cell.y, cell.z);
        byte level = fluidSection == null ? 0 : fluidSection.getFluidLevel(cell.x, cell.y, cell.z);

        if (fluidId == Integer.MIN_VALUE || level == 0 || fluidId == 0) {
            return FluidState.EMPTY;
        }

        return new FluidState(fluidId, level);
    }

    private void sendFluidStatePackets(@Nonnull Map<BlockCell, FluidState> cells) {
        Map<SectionKey, List<SetFluidCmd>> grouped = new HashMap<>();

        for (Map.Entry<BlockCell, FluidState> entry : cells.entrySet()) {
            BlockCell cell = entry.getKey();
            FluidState state = entry.getValue();

            SectionKey key = new SectionKey(
                    ChunkUtil.chunkCoordinate(cell.x),
                    ChunkUtil.indexSection(cell.y),
                    ChunkUtil.chunkCoordinate(cell.z)
            );

            grouped.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new SetFluidCmd((short) ChunkUtil.indexBlock(cell.x, cell.y, cell.z), state.fluidId, state.level));
        }

        for (Map.Entry<SectionKey, List<SetFluidCmd>> entry : grouped.entrySet()) {
            SectionKey key = entry.getKey();

            long chunkIndex = ChunkUtil.indexChunk(key.chunkX, key.chunkZ);
            if (!playerRef.getChunkTracker().isLoaded(chunkIndex)) {
                continue;
            }

            ServerSetFluids packet = new ServerSetFluids(
                    key.chunkX,
                    key.sectionY,
                    key.chunkZ,
                    entry.getValue().toArray(SetFluidCmd[]::new)
            );

            playerRef.getPacketHandler().writeNoCache(packet);
        }
    }

    private int fluidIdFor(double requestedFovModifier) {
        double clamped = Math.max(MIN_FOV, Math.min(MAX_FOV, requestedFovModifier));
        int slot = (int) Math.round((clamped - MIN_FOV) / STEP);
        return fluidBaseId + slot;
    }

    private record BlockCell(int x, int y, int z) {
    }

    private record SectionKey(int chunkX, int sectionY, int chunkZ) {
    }

    private record FluidState(int fluidId, byte level) {
        static final FluidState EMPTY = new FluidState(0, (byte) 0);
    }
}
