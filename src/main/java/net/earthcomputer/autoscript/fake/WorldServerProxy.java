package net.earthcomputer.autoscript.fake;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSetMultimap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.capabilities.Capability;

public class WorldServerProxy extends WorldServer {

	private static final ThreadLocal<WorldServer> currentWorldForDimension = new ThreadLocal<WorldServer>();
	private static final ThreadLocal<WorldClient> constructingDelegate = new ThreadLocal<WorldClient>();
	private WorldClient delegate;

	public WorldServerProxy(WorldClient delegate) {
		super(getMinecraftServer(delegate), delegate.getSaveHandler(), delegate.getWorldInfo(),
				delegate.provider.getDimension(), delegate.theProfiler);
		if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
			// Set the world back here due to it being set in the
			// superconstructor
			DimensionManager.setWorld(delegate.provider.getDimension(), currentWorldForDimension.get(),
					Minecraft.getMinecraft().getIntegratedServer());
		}
		currentWorldForDimension.set(null);
		constructingDelegate.set(null);
		this.delegate = delegate;
		this.capturedBlockSnapshots = delegate.capturedBlockSnapshots;
		this.loadedEntityList = delegate.loadedEntityList;
		this.loadedTileEntityList = delegate.loadedTileEntityList;
		this.playerEntities = delegate.playerEntities;
		this.provider = delegate.provider;
		// Need to set the world back here due to it being set in the
		// superconstructor
		this.provider.setWorld(delegate);
		this.rand = delegate.rand;
		this.tickableTileEntities = delegate.tickableTileEntities;
		this.villageCollectionObj = delegate.villageCollectionObj;
		this.weatherEffects = delegate.weatherEffects;
	}

	/**
	 * Hack function to set the delegate before the superconstructor is called
	 */
	private static MinecraftServer getMinecraftServer(WorldClient delegate) {
		if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
			currentWorldForDimension.set(DimensionManager.getWorld(delegate.provider.getDimension()));
		}
		constructingDelegate.set(delegate);
		if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
			return Minecraft.getMinecraft().getIntegratedServer();
		} else {
			// We need a fake Minecraft server here because stuff calles methods
			// in the superconstructor and via getMinecraftServer()
			return new MinecraftServer(new File(Minecraft.getMinecraft().mcDataDir, "saves"), null, null, null, null,
					null, null) {
				@Override
				public PlayerList getPlayerList() {
					return new PlayerList(this) {
						@Override
						public int getEntityViewDistance() {
							return (Minecraft.getMinecraft().gameSettings.renderDistanceChunks - 1) * 16;
						}
					};
				}

				@Override
				public boolean init() throws IOException {
					return false;
				}

				@Override
				public boolean canStructuresSpawn() {
					return false;
				}

				@Override
				public GameType getGameType() {
					return GameType.SURVIVAL;
				}

				@Override
				public EnumDifficulty getDifficulty() {
					return EnumDifficulty.NORMAL;
				}

				@Override
				public boolean isHardcore() {
					return false;
				}

				@Override
				public int getOpPermissionLevel() {
					return 4;
				}

				@Override
				public boolean shouldBroadcastRconToOps() {
					return false;
				}

				@Override
				public boolean shouldBroadcastConsoleToOps() {
					return false;
				}

				@Override
				public boolean isDedicatedServer() {
					return false;
				}

				@Override
				public boolean shouldUseNativeTransport() {
					return false;
				}

				@Override
				public boolean isCommandBlockEnabled() {
					return false;
				}

				@Override
				public String shareToLAN(GameType type, boolean allowCheats) {
					return null;
				}
			};
		}
	}

	@Override
	public void tick() {
		if (delegate == null) {
			constructingDelegate.get().tick();
		} else {
			delegate.tick();
		}
	}

	public void doPreChunk(int chunkX, int chunkZ, boolean loadChunk) {
		if (delegate == null) {
			constructingDelegate.get().doPreChunk(chunkX, chunkZ, loadChunk);
		} else {
			delegate.doPreChunk(chunkX, chunkZ, loadChunk);
		}
	}

	@Override
	public boolean spawnEntity(Entity entityIn) {
		return delegate == null ? constructingDelegate.get().spawnEntity(entityIn) : delegate.spawnEntity(entityIn);
	}

	@Override
	public Biome getBiome(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getBiome(pos) : delegate.getBiome(pos);
	}

	@Override
	public void removeEntity(Entity entityIn) {
		if (delegate == null) {
			constructingDelegate.get().removeEntity(entityIn);
		} else {
			delegate.removeEntity(entityIn);
		}
	}

	@Override
	public Biome getBiomeForCoordsBody(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getBiomeForCoordsBody(pos)
				: delegate.getBiomeForCoordsBody(pos);
	}

	@Override
	public void onEntityAdded(Entity entityIn) {
		if (delegate == null) {
			constructingDelegate.get().onEntityAdded(entityIn);
		} else {
			delegate.onEntityAdded(entityIn);
		}
	}

	@Override
	public void onEntityRemoved(Entity entityIn) {
		if (delegate == null) {
			constructingDelegate.get().onEntityRemoved(entityIn);
		} else {
			delegate.onEntityRemoved(entityIn);
		}
	}

	public void addEntityToWorld(int entityID, Entity entityToSpawn) {
		if (delegate == null) {
			constructingDelegate.get().addEntityToWorld(entityID, entityToSpawn);
		} else {
			delegate.addEntityToWorld(entityID, entityToSpawn);
		}
	}

	@Override
	public BiomeProvider getBiomeProvider() {
		return delegate == null ? constructingDelegate.get().getBiomeProvider() : delegate.getBiomeProvider();
	}

	@Override
	public Entity getEntityByID(int id) {
		return delegate == null ? constructingDelegate.get().getEntityByID(id) : delegate.getEntityByID(id);
	}

	@Override
	public MinecraftServer getMinecraftServer() {
		return super.getMinecraftServer();
	}

	@Override
	public void setInitialSpawnLocation() {
		if (delegate == null) {
			constructingDelegate.get().setInitialSpawnLocation();
		} else {
			delegate.setInitialSpawnLocation();
		}
	}

	public Entity removeEntityFromWorld(int entityID) {
		return delegate == null ? constructingDelegate.get().removeEntityFromWorld(entityID)
				: delegate.removeEntityFromWorld(entityID);
	}

	@Override
	public IBlockState getGroundAboveSeaLevel(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getGroundAboveSeaLevel(pos)
				: delegate.getGroundAboveSeaLevel(pos);
	}

	@Deprecated
	public boolean invalidateRegionAndSetBlock(BlockPos pos, IBlockState state) {
		return delegate == null ? constructingDelegate.get().invalidateRegionAndSetBlock(pos, state)
				: delegate.invalidateRegionAndSetBlock(pos, state);
	}

	@Override
	public void sendQuittingDisconnectingPacket() {
		if (delegate == null) {
			constructingDelegate.get().sendQuittingDisconnectingPacket();
		} else {
			delegate.sendQuittingDisconnectingPacket();
		}
	}

	@Override
	public boolean isAirBlock(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isAirBlock(pos) : delegate.isAirBlock(pos);
	}

	@Override
	public boolean isBlockLoaded(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isBlockLoaded(pos) : delegate.isBlockLoaded(pos);
	}

	@Override
	public boolean isBlockLoaded(BlockPos pos, boolean allowEmpty) {
		return delegate == null ? constructingDelegate.get().isBlockLoaded(pos, allowEmpty)
				: delegate.isBlockLoaded(pos, allowEmpty);
	}

	@Override
	public boolean isAreaLoaded(BlockPos center, int radius) {
		return delegate == null ? constructingDelegate.get().isAreaLoaded(center, radius)
				: delegate.isAreaLoaded(center, radius);
	}

	@Override
	public boolean isAreaLoaded(BlockPos center, int radius, boolean allowEmpty) {
		return delegate == null ? constructingDelegate.get().isAreaLoaded(center, radius, allowEmpty)
				: delegate.isAreaLoaded(center, radius, allowEmpty);
	}

	@Override
	public boolean isAreaLoaded(BlockPos from, BlockPos to) {
		return delegate == null ? constructingDelegate.get().isAreaLoaded(from, to) : delegate.isAreaLoaded(from, to);
	}

	@Override
	public boolean isAreaLoaded(BlockPos from, BlockPos to, boolean allowEmpty) {
		return delegate == null ? constructingDelegate.get().isAreaLoaded(from, to, allowEmpty)
				: delegate.isAreaLoaded(from, to, allowEmpty);
	}

	public void doVoidFogParticles(int posX, int posY, int posZ) {
		if (delegate == null) {
			constructingDelegate.get().doVoidFogParticles(posX, posY, posZ);
		} else {
			delegate.doVoidFogParticles(posX, posY, posZ);
		}
	}

	@Override
	public boolean isAreaLoaded(StructureBoundingBox box) {
		return delegate == null ? constructingDelegate.get().isAreaLoaded(box) : delegate.isAreaLoaded(box);
	}

	@Override
	public boolean isAreaLoaded(StructureBoundingBox box, boolean allowEmpty) {
		return delegate == null ? constructingDelegate.get().isAreaLoaded(box, allowEmpty)
				: delegate.isAreaLoaded(box, allowEmpty);
	}

	public void showBarrierParticles(int p_184153_1_, int p_184153_2_, int p_184153_3_, int p_184153_4_, Random random,
			boolean p_184153_6_, MutableBlockPos pos) {
		if (delegate == null) {
			constructingDelegate.get().showBarrierParticles(p_184153_1_, p_184153_2_, p_184153_3_, p_184153_4_, random,
					p_184153_6_, pos);
		} else {
			delegate.showBarrierParticles(p_184153_1_, p_184153_2_, p_184153_3_, p_184153_4_, random, p_184153_6_, pos);
		}
	}

	@Override
	public Chunk getChunkFromBlockCoords(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getChunkFromBlockCoords(pos)
				: delegate.getChunkFromBlockCoords(pos);
	}

	@Override
	public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
		return delegate == null ? constructingDelegate.get().getChunkFromChunkCoords(chunkX, chunkZ)
				: delegate.getChunkFromChunkCoords(chunkX, chunkZ);
	}

	public void removeAllEntities() {
		if (delegate == null) {
			constructingDelegate.get().removeAllEntities();
		} else {
			delegate.removeAllEntities();
		}
	}

	@Override
	public boolean isChunkGeneratedAt(int x, int z) {
		return delegate == null ? constructingDelegate.get().isChunkGeneratedAt(x, z)
				: delegate.isChunkGeneratedAt(x, z);
	}

	@Override
	public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
		return delegate == null ? constructingDelegate.get().setBlockState(pos, newState, flags)
				: delegate.setBlockState(pos, newState, flags);
	}

	@Override
	public CrashReportCategory addWorldInfoToCrashReport(CrashReport report) {
		return delegate == null ? constructingDelegate.get().addWorldInfoToCrashReport(report)
				: delegate.addWorldInfoToCrashReport(report);
	}

	@Override
	public void markAndNotifyBlock(BlockPos pos, Chunk chunk, IBlockState iblockstate, IBlockState newState,
			int flags) {
		if (delegate == null) {
			constructingDelegate.get().markAndNotifyBlock(pos, chunk, iblockstate, newState, flags);
		} else {
			delegate.markAndNotifyBlock(pos, chunk, iblockstate, newState, flags);
		}
	}

	@Override
	public void playSound(EntityPlayer player, double x, double y, double z, SoundEvent soundIn, SoundCategory category,
			float volume, float pitch) {
		if (delegate == null) {
			constructingDelegate.get().playSound(player, x, y, z, soundIn, category, volume, pitch);
		} else {
			delegate.playSound(player, x, y, z, soundIn, category, volume, pitch);
		}
	}

	@Override
	public boolean setBlockToAir(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().setBlockToAir(pos) : delegate.setBlockToAir(pos);
	}

	public void playSound(BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch,
			boolean distanceDelay) {
		if (delegate == null) {
			constructingDelegate.get().playSound(pos, soundIn, category, volume, pitch, distanceDelay);
		} else {
			delegate.playSound(pos, soundIn, category, volume, pitch, distanceDelay);
		}
	}

	@Override
	public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
		return delegate == null ? constructingDelegate.get().destroyBlock(pos, dropBlock)
				: delegate.destroyBlock(pos, dropBlock);
	}

	@Override
	public void playSound(double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume,
			float pitch, boolean distanceDelay) {
		if (delegate == null) {
			constructingDelegate.get().playSound(x, y, z, soundIn, category, volume, pitch, distanceDelay);
		} else {
			delegate.playSound(x, y, z, soundIn, category, volume, pitch, distanceDelay);
		}
	}

	@Override
	public boolean setBlockState(BlockPos pos, IBlockState state) {
		return delegate == null ? constructingDelegate.get().setBlockState(pos, state)
				: delegate.setBlockState(pos, state);
	}

	@Override
	public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ,
			NBTTagCompound compund) {
		if (delegate == null) {
			constructingDelegate.get().makeFireworks(x, y, z, motionX, motionY, motionZ, compund);
		} else {
			delegate.makeFireworks(x, y, z, motionX, motionY, motionZ, compund);
		}
	}

	@Override
	public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
		if (delegate == null) {
			constructingDelegate.get().notifyBlockUpdate(pos, oldState, newState, flags);
		} else {
			delegate.notifyBlockUpdate(pos, oldState, newState, flags);
		}
	}

	@Override
	public void sendPacketToServer(Packet<?> packetIn) {
		if (delegate == null) {
			constructingDelegate.get().sendPacketToServer(packetIn);
		} else {
			if (delegate == null) {
				constructingDelegate.get().sendPacketToServer(packetIn);
			} else {
				delegate.sendPacketToServer(packetIn);
			}
		}
	}

	@Override
	public void notifyNeighborsRespectDebug(BlockPos pos, Block blockType, boolean p_175722_3_) {
		if (delegate == null) {
			constructingDelegate.get().notifyNeighborsRespectDebug(pos, blockType, p_175722_3_);
		} else {
			delegate.notifyNeighborsRespectDebug(pos, blockType, p_175722_3_);
		}
	}

	public void setWorldScoreboard(Scoreboard scoreboardIn) {
		if (delegate == null) {
			constructingDelegate.get().setWorldScoreboard(scoreboardIn);
		} else {
			delegate.setWorldScoreboard(scoreboardIn);
		}
	}

	@Override
	public void setWorldTime(long time) {
		if (delegate == null) {
			constructingDelegate.get().setWorldTime(time);
		} else {
			delegate.setWorldTime(time);
		}
	}

	@Override
	public void markBlocksDirtyVertical(int x1, int z1, int x2, int z2) {
		if (delegate == null) {
			constructingDelegate.get().markBlocksDirtyVertical(x1, z1, x2, z2);
		} else {
			delegate.markBlocksDirtyVertical(x1, z1, x2, z2);
		}
	}

	@Override
	public void markBlockRangeForRenderUpdate(BlockPos rangeMin, BlockPos rangeMax) {
		if (delegate == null) {
			constructingDelegate.get().markBlockRangeForRenderUpdate(rangeMin, rangeMax);
		} else {
			delegate.markBlockRangeForRenderUpdate(rangeMin, rangeMax);
		}
	}

	@Override
	public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
		if (delegate == null) {
			constructingDelegate.get().markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
		} else {
			delegate.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
		}
	}

	@Override
	public void updateObservingBlocksAt(BlockPos pos, Block blockType) {
		if (delegate == null) {
			constructingDelegate.get().updateObservingBlocksAt(pos, blockType);
		} else {
			delegate.updateObservingBlocksAt(pos, blockType);
		}
	}

	@Override
	public void notifyNeighborsOfStateChange(BlockPos pos, Block blockType, boolean updateObservers) {
		if (delegate == null) {
			constructingDelegate.get().notifyNeighborsOfStateChange(pos, blockType, updateObservers);
		} else {
			delegate.notifyNeighborsOfStateChange(pos, blockType, updateObservers);
		}
	}

	@Override
	public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, EnumFacing skipSide) {
		if (delegate == null) {
			constructingDelegate.get().notifyNeighborsOfStateExcept(pos, blockType, skipSide);
		} else {
			delegate.notifyNeighborsOfStateExcept(pos, blockType, skipSide);
		}
	}

	@Override
	public void neighborChanged(BlockPos pos, Block p_190524_2_, BlockPos p_190524_3_) {
		if (delegate == null) {
			constructingDelegate.get().neighborChanged(pos, p_190524_2_, p_190524_3_);
		} else {
			delegate.neighborChanged(pos, p_190524_2_, p_190524_3_);
		}
	}

	@Override
	public void observedNeighborChanged(BlockPos pos, Block p_190529_2_, BlockPos p_190529_3_) {
		if (delegate == null) {
			constructingDelegate.get().observedNeighborChanged(pos, p_190529_2_, p_190529_3_);
		} else {
			delegate.observedNeighborChanged(pos, p_190529_2_, p_190529_3_);
		}
	}

	@Override
	public boolean isBlockTickPending(BlockPos pos, Block blockType) {
		return delegate == null ? constructingDelegate.get().isBlockTickPending(pos, blockType)
				: delegate.isBlockTickPending(pos, blockType);
	}

	@Override
	public boolean canSeeSky(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().canSeeSky(pos) : delegate.canSeeSky(pos);
	}

	@Override
	public boolean canBlockSeeSky(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().canBlockSeeSky(pos) : delegate.canBlockSeeSky(pos);
	}

	@Override
	public int getLight(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getLight(pos) : delegate.getLight(pos);
	}

	@Override
	public int getLightFromNeighbors(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getLightFromNeighbors(pos)
				: delegate.getLightFromNeighbors(pos);
	}

	@Override
	public int getLight(BlockPos pos, boolean checkNeighbors) {
		return delegate == null ? constructingDelegate.get().getLight(pos, checkNeighbors)
				: delegate.getLight(pos, checkNeighbors);
	}

	@Override
	public BlockPos getHeight(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getHeight(pos) : delegate.getHeight(pos);
	}

	@Override
	public int getHeight(int x, int z) {
		return delegate == null ? constructingDelegate.get().getHeight(x, z) : delegate.getHeight(x, z);
	}

	@Override
	public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getLightFromNeighborsFor(type, pos)
				: delegate.getLightFromNeighborsFor(type, pos);
	}

	@Override
	public int getLightFor(EnumSkyBlock type, BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getLightFor(type, pos) : delegate.getLightFor(type, pos);
	}

	@Override
	public void setLightFor(EnumSkyBlock type, BlockPos pos, int lightValue) {
		if (delegate == null) {
			constructingDelegate.get().setLightFor(type, pos, lightValue);
		} else {
			delegate.setLightFor(type, pos, lightValue);
		}
	}

	@Override
	public void notifyLightSet(BlockPos pos) {
		if (delegate == null) {
			constructingDelegate.get().notifyLightSet(pos);
		} else {
			delegate.notifyLightSet(pos);
		}
	}

	@Override
	public float getLightBrightness(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getLightBrightness(pos) : delegate.getLightBrightness(pos);
	}

	@Override
	public boolean isDaytime() {
		return delegate == null ? constructingDelegate.get().isDaytime() : delegate.isDaytime();
	}

	@Override
	public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end) {
		return delegate == null ? constructingDelegate.get().rayTraceBlocks(start, end)
				: delegate.rayTraceBlocks(start, end);
	}

	@Override
	public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end, boolean stopOnLiquid) {
		return delegate == null ? constructingDelegate.get().rayTraceBlocks(start, end, stopOnLiquid)
				: delegate.rayTraceBlocks(start, end, stopOnLiquid);
	}

	@Override
	public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid,
			boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
		return delegate.rayTraceBlocks(vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox,
				returnLastUncollidableBlock);
	}

	@Override
	public void playSound(EntityPlayer player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume,
			float pitch) {
		if (delegate == null) {
			constructingDelegate.get().playSound(player, pos, soundIn, category, volume, pitch);
		} else {
			delegate.playSound(player, pos, soundIn, category, volume, pitch);
		}
	}

	@Override
	public void playRecord(BlockPos blockPositionIn, SoundEvent soundEventIn) {
		if (delegate == null) {
			constructingDelegate.get().playRecord(blockPositionIn, soundEventIn);
		} else {
			delegate.playRecord(blockPositionIn, soundEventIn);
		}
	}

	@Override
	public void spawnParticle(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord,
			double xSpeed, double ySpeed, double zSpeed, int... parameters) {
		if (delegate == null) {
			constructingDelegate.get().spawnParticle(particleType, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed,
					parameters);
		} else {
			delegate.spawnParticle(particleType, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
		}
	}

	@Override
	public void spawnAlwaysVisibleParticle(int p_190523_1_, double p_190523_2_, double p_190523_4_, double p_190523_6_,
			double p_190523_8_, double p_190523_10_, double p_190523_12_, int... p_190523_14_) {
		delegate.spawnAlwaysVisibleParticle(p_190523_1_, p_190523_2_, p_190523_4_, p_190523_6_, p_190523_8_,
				p_190523_10_, p_190523_12_, p_190523_14_);
	}

	@Override
	public void spawnParticle(EnumParticleTypes particleType, boolean ignoreRange, double xCoord, double yCoord,
			double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
		if (delegate == null) {
			constructingDelegate.get().spawnParticle(particleType, ignoreRange, xCoord, yCoord, zCoord, xSpeed, ySpeed,
					zSpeed, parameters);
		} else {
			delegate.spawnParticle(particleType, ignoreRange, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed,
					parameters);
		}
	}

	@Override
	public boolean addWeatherEffect(Entity entityIn) {
		return delegate == null ? constructingDelegate.get().addWeatherEffect(entityIn)
				: delegate.addWeatherEffect(entityIn);
	}

	@Override
	public void removeEntityDangerously(Entity entityIn) {
		if (delegate == null) {
			constructingDelegate.get().removeEntityDangerously(entityIn);
		} else {
			delegate.removeEntityDangerously(entityIn);
		}
	}

	@Override
	public void addEventListener(IWorldEventListener listener) {
		if (delegate == null) {
			constructingDelegate.get().addEventListener(listener);
		} else {
			delegate.addEventListener(listener);
		}
	}

	@Override
	public void removeEventListener(IWorldEventListener listener) {
		if (delegate == null) {
			constructingDelegate.get().removeEventListener(listener);
		} else {
			delegate.removeEventListener(listener);
		}
	}

	@Override
	public boolean collidesWithAnyBlock(AxisAlignedBB bbox) {
		return delegate == null ? constructingDelegate.get().collidesWithAnyBlock(bbox)
				: delegate.collidesWithAnyBlock(bbox);
	}

	@Override
	public int calculateSkylightSubtracted(float partialTicks) {
		return delegate == null ? constructingDelegate.get().calculateSkylightSubtracted(partialTicks)
				: delegate.calculateSkylightSubtracted(partialTicks);
	}

	@Override
	public float getSunBrightnessFactor(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getSunBrightnessFactor(partialTicks)
				: delegate.getSunBrightnessFactor(partialTicks);
	}

	@Override
	public float getSunBrightness(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getSunBrightness(partialTicks)
				: delegate.getSunBrightness(partialTicks);
	}

	@Override
	public float getSunBrightnessBody(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getSunBrightnessBody(partialTicks)
				: delegate.getSunBrightnessBody(partialTicks);
	}

	@Override
	public Vec3d getSkyColor(Entity entityIn, float partialTicks) {
		return delegate == null ? constructingDelegate.get().getSkyColor(entityIn, partialTicks)
				: delegate.getSkyColor(entityIn, partialTicks);
	}

	@Override
	public Vec3d getSkyColorBody(Entity entityIn, float partialTicks) {
		return delegate == null ? constructingDelegate.get().getSkyColorBody(entityIn, partialTicks)
				: delegate.getSkyColorBody(entityIn, partialTicks);
	}

	@Override
	public int getMoonPhase() {
		return delegate == null ? constructingDelegate.get().getMoonPhase() : delegate.getMoonPhase();
	}

	@Override
	public float getCurrentMoonPhaseFactorBody() {
		return delegate == null ? constructingDelegate.get().getCurrentMoonPhaseFactorBody()
				: delegate.getCurrentMoonPhaseFactorBody();
	}

	@Override
	public Vec3d getFogColor(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getFogColor(partialTicks)
				: delegate.getFogColor(partialTicks);
	}

	@Override
	public BlockPos getPrecipitationHeight(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getPrecipitationHeight(pos)
				: delegate.getPrecipitationHeight(pos);
	}

	@Override
	public BlockPos getTopSolidOrLiquidBlock(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getTopSolidOrLiquidBlock(pos)
				: delegate.getTopSolidOrLiquidBlock(pos);
	}

	@Override
	public float getStarBrightness(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getStarBrightness(partialTicks)
				: delegate.getStarBrightness(partialTicks);
	}

	@Override
	public float getStarBrightnessBody(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getStarBrightnessBody(partialTicks)
				: delegate.getStarBrightnessBody(partialTicks);
	}

	@Override
	public boolean isUpdateScheduled(BlockPos pos, Block blk) {
		return delegate == null ? constructingDelegate.get().isUpdateScheduled(pos, blk)
				: delegate.isUpdateScheduled(pos, blk);
	}

	@Override
	public void scheduleUpdate(BlockPos pos, Block blockIn, int delay) {
		if (delegate == null) {
			constructingDelegate.get().scheduleUpdate(pos, blockIn, delay);
		} else {
			delegate.scheduleUpdate(pos, blockIn, delay);
		}
	}

	@Override
	public void updateBlockTick(BlockPos pos, Block blockIn, int delay, int priority) {
		if (delegate == null) {
			constructingDelegate.get().updateBlockTick(pos, blockIn, delay, priority);
		} else {
			delegate.updateBlockTick(pos, blockIn, delay, priority);
		}
	}

	@Override
	public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority) {
		if (delegate == null) {
			constructingDelegate.get().scheduleBlockUpdate(pos, blockIn, delay, priority);
		} else {
			delegate.scheduleBlockUpdate(pos, blockIn, delay, priority);
		}
	}

	@Override
	public void updateEntities() {
		if (delegate == null) {
			constructingDelegate.get().updateEntities();
		} else {
			delegate.updateEntities();
		}
	}

	@Override
	public boolean addTileEntity(TileEntity tile) {
		return delegate == null ? constructingDelegate.get().addTileEntity(tile) : delegate.addTileEntity(tile);
	}

	@Override
	public void addTileEntities(Collection<TileEntity> tileEntityCollection) {
		if (delegate == null) {
			constructingDelegate.get().addTileEntities(tileEntityCollection);
		} else {
			delegate.addTileEntities(tileEntityCollection);
		}
	}

	@Override
	public void updateEntity(Entity ent) {
		if (delegate == null) {
			constructingDelegate.get().updateEntity(ent);
		} else {
			delegate.updateEntity(ent);
		}
	}

	@Override
	public void updateEntityWithOptionalForce(Entity entityIn, boolean forceUpdate) {
		if (delegate == null) {
			constructingDelegate.get().updateEntityWithOptionalForce(entityIn, forceUpdate);
		} else {
			delegate.updateEntityWithOptionalForce(entityIn, forceUpdate);
		}
	}

	@Override
	public boolean checkNoEntityCollision(AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().checkNoEntityCollision(bb)
				: delegate.checkNoEntityCollision(bb);
	}

	@Override
	public boolean checkNoEntityCollision(AxisAlignedBB bb, Entity entityIn) {
		return delegate == null ? constructingDelegate.get().checkNoEntityCollision(bb, entityIn)
				: delegate.checkNoEntityCollision(bb, entityIn);
	}

	@Override
	public boolean checkBlockCollision(AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().checkBlockCollision(bb) : delegate.checkBlockCollision(bb);
	}

	@Override
	public boolean containsAnyLiquid(AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().containsAnyLiquid(bb) : delegate.containsAnyLiquid(bb);
	}

	@Override
	public boolean isFlammableWithin(AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().isFlammableWithin(bb) : delegate.isFlammableWithin(bb);
	}

	@Override
	public boolean handleMaterialAcceleration(AxisAlignedBB bb, Material materialIn, Entity entityIn) {
		return delegate == null ? constructingDelegate.get().handleMaterialAcceleration(bb, materialIn, entityIn)
				: delegate.handleMaterialAcceleration(bb, materialIn, entityIn);
	}

	@Override
	public boolean isMaterialInBB(AxisAlignedBB bb, Material materialIn) {
		return delegate == null ? constructingDelegate.get().isMaterialInBB(bb, materialIn)
				: delegate.isMaterialInBB(bb, materialIn);
	}

	@Override
	public Explosion createExplosion(Entity entityIn, double x, double y, double z, float strength, boolean isSmoking) {
		return delegate == null ? constructingDelegate.get().createExplosion(entityIn, x, y, z, strength, isSmoking)
				: delegate.createExplosion(entityIn, x, y, z, strength, isSmoking);
	}

	@Override
	public Explosion newExplosion(Entity entityIn, double x, double y, double z, float strength, boolean isFlaming,
			boolean isSmoking) {
		return delegate == null
				? constructingDelegate.get().newExplosion(entityIn, x, y, z, strength, isFlaming, isSmoking)
				: delegate.newExplosion(entityIn, x, y, z, strength, isFlaming, isSmoking);
	}

	@Override
	public String getDebugLoadedEntities() {
		return delegate == null ? constructingDelegate.get().getDebugLoadedEntities()
				: delegate.getDebugLoadedEntities();
	}

	@Override
	public String getProviderName() {
		return delegate == null ? constructingDelegate.get().getProviderName() : delegate.getProviderName();
	}

	@Override
	public TileEntity getTileEntity(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getTileEntity(pos) : delegate.getTileEntity(pos);
	}

	@Override
	public void setTileEntity(BlockPos pos, TileEntity tileEntityIn) {
		if (delegate == null) {
			constructingDelegate.get().setTileEntity(pos, tileEntityIn);
		} else {
			delegate.setTileEntity(pos, tileEntityIn);
		}
	}

	@Override
	public void removeTileEntity(BlockPos pos) {
		if (delegate == null) {
			constructingDelegate.get().removeTileEntity(pos);
		} else {
			delegate.removeTileEntity(pos);
		}
	}

	@Override
	public void markTileEntityForRemoval(TileEntity tileEntityIn) {
		if (delegate == null) {
			constructingDelegate.get().markTileEntityForRemoval(tileEntityIn);
		} else {
			delegate.markTileEntityForRemoval(tileEntityIn);
		}
	}

	@Override
	public boolean isBlockFullCube(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isBlockFullCube(pos) : delegate.isBlockFullCube(pos);
	}

	@Override
	public boolean isBlockNormalCube(BlockPos pos, boolean _default) {
		return delegate == null ? constructingDelegate.get().isBlockNormalCube(pos, _default)
				: delegate.isBlockNormalCube(pos, _default);
	}

	@Override
	public void calculateInitialSkylight() {
		if (delegate == null) {
			constructingDelegate.get().calculateInitialSkylight();
		} else {
			delegate.calculateInitialSkylight();
		}
	}

	@Override
	public void setAllowedSpawnTypes(boolean hostile, boolean peaceful) {
		if (delegate == null) {
			constructingDelegate.get().setAllowedSpawnTypes(hostile, peaceful);
		} else {
			delegate.setAllowedSpawnTypes(hostile, peaceful);
		}
	}

	@Override
	public void calculateInitialWeatherBody() {
		if (delegate == null) {
			constructingDelegate.get().calculateInitialWeatherBody();
		} else {
			delegate.calculateInitialWeatherBody();
		}
	}

	@Override
	public void updateWeatherBody() {
		if (delegate == null) {
			constructingDelegate.get().updateWeatherBody();
		} else {
			delegate.updateWeatherBody();
		}
	}

	@Override
	public boolean canBlockFreezeWater(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().canBlockFreezeWater(pos)
				: delegate.canBlockFreezeWater(pos);
	}

	@Override
	public boolean canBlockFreezeNoWater(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().canBlockFreezeNoWater(pos)
				: delegate.canBlockFreezeNoWater(pos);
	}

	@Override
	public boolean canBlockFreeze(BlockPos pos, boolean noWaterAdj) {
		return delegate == null ? constructingDelegate.get().canBlockFreeze(pos, noWaterAdj)
				: delegate.canBlockFreeze(pos, noWaterAdj);
	}

	@Override
	public boolean canBlockFreezeBody(BlockPos pos, boolean noWaterAdj) {
		return delegate == null ? constructingDelegate.get().canBlockFreezeBody(pos, noWaterAdj)
				: delegate.canBlockFreezeBody(pos, noWaterAdj);
	}

	@Override
	public boolean canSnowAt(BlockPos pos, boolean checkLight) {
		return delegate == null ? constructingDelegate.get().canSnowAt(pos, checkLight)
				: delegate.canSnowAt(pos, checkLight);
	}

	@Override
	public boolean canSnowAtBody(BlockPos pos, boolean checkLight) {
		return delegate == null ? constructingDelegate.get().canSnowAtBody(pos, checkLight)
				: delegate.canSnowAtBody(pos, checkLight);
	}

	@Override
	public boolean checkLight(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().checkLight(pos) : delegate.checkLight(pos);
	}

	@Override
	public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos) {
		return delegate == null ? constructingDelegate.get().checkLightFor(lightType, pos)
				: delegate.checkLightFor(lightType, pos);
	}

	@Override
	public boolean tickUpdates(boolean p_72955_1_) {
		return delegate == null ? constructingDelegate.get().tickUpdates(p_72955_1_) : delegate.tickUpdates(p_72955_1_);
	}

	@Override
	public List<NextTickListEntry> getPendingBlockUpdates(Chunk chunkIn, boolean p_72920_2_) {
		return delegate == null ? constructingDelegate.get().getPendingBlockUpdates(chunkIn, p_72920_2_)
				: delegate.getPendingBlockUpdates(chunkIn, p_72920_2_);
	}

	@Override
	public List<NextTickListEntry> getPendingBlockUpdates(StructureBoundingBox structureBB, boolean p_175712_2_) {
		return delegate == null ? constructingDelegate.get().getPendingBlockUpdates(structureBB, p_175712_2_)
				: delegate.getPendingBlockUpdates(structureBB, p_175712_2_);
	}

	@Override
	public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity entityIn, AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().getEntitiesWithinAABBExcludingEntity(entityIn, bb)
				: delegate.getEntitiesWithinAABBExcludingEntity(entityIn, bb);
	}

	@Override
	public List<Entity> getEntitiesInAABBexcluding(Entity entityIn, AxisAlignedBB boundingBox,
			Predicate<? super Entity> predicate) {
		return delegate == null
				? constructingDelegate.get().getEntitiesInAABBexcluding(entityIn, boundingBox, predicate)
				: delegate.getEntitiesInAABBexcluding(entityIn, boundingBox, predicate);
	}

	@Override
	public <T extends Entity> List<T> getEntities(Class<? extends T> entityType, Predicate<? super T> filter) {
		return delegate == null ? constructingDelegate.get().getEntities(entityType, filter)
				: delegate.getEntities(entityType, filter);
	}

	@Override
	public <T extends Entity> List<T> getPlayers(Class<? extends T> playerType, Predicate<? super T> filter) {
		return delegate == null ? constructingDelegate.get().getPlayers(playerType, filter)
				: delegate.getPlayers(playerType, filter);
	}

	@Override
	public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> classEntity, AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().getEntitiesWithinAABB(classEntity, bb)
				: delegate.getEntitiesWithinAABB(classEntity, bb);
	}

	@Override
	public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> clazz, AxisAlignedBB aabb,
			Predicate<? super T> filter) {
		return delegate == null ? constructingDelegate.get().getEntitiesWithinAABB(clazz, aabb, filter)
				: delegate.getEntitiesWithinAABB(clazz, aabb, filter);
	}

	@Override
	public List<Entity> getLoadedEntityList() {
		return delegate == null ? constructingDelegate.get().getLoadedEntityList() : delegate.getLoadedEntityList();
	}

	@Override
	public void markChunkDirty(BlockPos pos, TileEntity unusedTileEntity) {
		if (delegate == null) {
			constructingDelegate.get().markChunkDirty(pos, unusedTileEntity);
		} else {
			delegate.markChunkDirty(pos, unusedTileEntity);
		}
	}

	@Override
	public int countEntities(Class<?> entityType) {
		return delegate == null ? constructingDelegate.get().countEntities(entityType)
				: delegate.countEntities(entityType);
	}

	@Override
	public void loadEntities(Collection<Entity> entityCollection) {
		if (delegate == null) {
			constructingDelegate.get().loadEntities(entityCollection);
		} else {
			delegate.loadEntities(entityCollection);
		}
	}

	@Override
	public boolean mayPlace(Block p_190527_1_, BlockPos p_190527_2_, boolean p_190527_3_, EnumFacing p_190527_4_,
			Entity p_190527_5_) {
		return delegate == null
				? constructingDelegate.get().mayPlace(p_190527_1_, p_190527_2_, p_190527_3_, p_190527_4_, p_190527_5_)
				: delegate.mayPlace(p_190527_1_, p_190527_2_, p_190527_3_, p_190527_4_, p_190527_5_);
	}

	@Override
	public int getSeaLevel() {
		return delegate == null ? constructingDelegate.get().getSeaLevel() : delegate.getSeaLevel();
	}

	@Override
	public void setSeaLevel(int seaLevelIn) {
		if (delegate == null) {
			constructingDelegate.get().setSeaLevel(seaLevelIn);
		} else {
			delegate.setSeaLevel(seaLevelIn);
		}
	}

	@Override
	public int getStrongPower(BlockPos pos, EnumFacing direction) {
		return delegate == null ? constructingDelegate.get().getStrongPower(pos, direction)
				: delegate.getStrongPower(pos, direction);
	}

	@Override
	public WorldType getWorldType() {
		return delegate == null ? constructingDelegate.get().getWorldType() : delegate.getWorldType();
	}

	@Override
	public int getStrongPower(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getStrongPower(pos) : delegate.getStrongPower(pos);
	}

	@Override
	public boolean isSidePowered(BlockPos pos, EnumFacing side) {
		return delegate == null ? constructingDelegate.get().isSidePowered(pos, side)
				: delegate.isSidePowered(pos, side);
	}

	@Override
	public int getRedstonePower(BlockPos pos, EnumFacing facing) {
		return delegate == null ? constructingDelegate.get().getRedstonePower(pos, facing)
				: delegate.getRedstonePower(pos, facing);
	}

	@Override
	public boolean isBlockPowered(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isBlockPowered(pos) : delegate.isBlockPowered(pos);
	}

	@Override
	public int isBlockIndirectlyGettingPowered(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isBlockIndirectlyGettingPowered(pos)
				: delegate.isBlockIndirectlyGettingPowered(pos);
	}

	@Override
	public EntityPlayer getNearestPlayerNotCreative(Entity entityIn, double distance) {
		return delegate == null ? constructingDelegate.get().getNearestPlayerNotCreative(entityIn, distance)
				: delegate.getNearestPlayerNotCreative(entityIn, distance);
	}

	@Override
	public boolean isAnyPlayerWithinRangeAt(double x, double y, double z, double range) {
		return delegate == null ? constructingDelegate.get().isAnyPlayerWithinRangeAt(x, y, z, range)
				: delegate.isAnyPlayerWithinRangeAt(x, y, z, range);
	}

	@Override
	public EntityPlayer getNearestAttackablePlayer(Entity entityIn, double maxXZDistance, double maxYDistance) {
		return delegate == null
				? constructingDelegate.get().getNearestAttackablePlayer(entityIn, maxXZDistance, maxYDistance)
				: delegate.getNearestAttackablePlayer(entityIn, maxXZDistance, maxYDistance);
	}

	@Override
	public EntityPlayer getNearestAttackablePlayer(BlockPos pos, double maxXZDistance, double maxYDistance) {
		return delegate == null
				? constructingDelegate.get().getNearestAttackablePlayer(pos, maxXZDistance, maxYDistance)
				: delegate.getNearestAttackablePlayer(pos, maxXZDistance, maxYDistance);
	}

	@Override
	public EntityPlayer getNearestAttackablePlayer(double posX, double posY, double posZ, double maxXZDistance,
			double maxYDistance, Function<EntityPlayer, Double> playerToDouble, Predicate<EntityPlayer> p_184150_12_) {
		return delegate.getNearestAttackablePlayer(posX, posY, posZ, maxXZDistance, maxYDistance, playerToDouble,
				p_184150_12_);
	}

	@Override
	public EntityPlayer getPlayerEntityByName(String name) {
		return delegate == null ? constructingDelegate.get().getPlayerEntityByName(name)
				: delegate.getPlayerEntityByName(name);
	}

	@Override
	public EntityPlayer getPlayerEntityByUUID(UUID uuid) {
		return delegate == null ? constructingDelegate.get().getPlayerEntityByUUID(uuid)
				: delegate.getPlayerEntityByUUID(uuid);
	}

	@Override
	public void checkSessionLock() throws MinecraftException {
		if (delegate == null) {
			constructingDelegate.get().checkSessionLock();
		} else {
			delegate.checkSessionLock();
		}
	}

	@Override
	public void setTotalWorldTime(long worldTime) {
		if (delegate == null) {
			constructingDelegate.get().setTotalWorldTime(worldTime);
		} else {
			delegate.setTotalWorldTime(worldTime);
		}
	}

	@Override
	public long getSeed() {
		return delegate == null ? constructingDelegate.get().getSeed() : delegate.getSeed();
	}

	@Override
	public long getTotalWorldTime() {
		return delegate == null ? constructingDelegate.get().getTotalWorldTime() : delegate.getTotalWorldTime();
	}

	@Override
	public long getWorldTime() {
		return delegate == null ? constructingDelegate.get().getWorldTime() : delegate.getWorldTime();
	}

	@Override
	public BlockPos getSpawnPoint() {
		return delegate == null ? constructingDelegate.get().getSpawnPoint() : delegate.getSpawnPoint();
	}

	@Override
	public void setSpawnPoint(BlockPos pos) {
		if (delegate == null) {
			constructingDelegate.get().setSpawnPoint(pos);
		} else {
			delegate.setSpawnPoint(pos);
		}
	}

	@Override
	public void joinEntityInSurroundings(Entity entityIn) {
		if (delegate == null) {
			constructingDelegate.get().joinEntityInSurroundings(entityIn);
		} else {
			delegate.joinEntityInSurroundings(entityIn);
		}
	}

	@Override
	public boolean isBlockModifiable(EntityPlayer player, BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isBlockModifiable(player, pos)
				: delegate.isBlockModifiable(player, pos);
	}

	@Override
	public boolean canMineBlockBody(EntityPlayer player, BlockPos pos) {
		return delegate == null ? constructingDelegate.get().canMineBlockBody(player, pos)
				: delegate.canMineBlockBody(player, pos);
	}

	@Override
	public void setEntityState(Entity entityIn, byte state) {
		if (delegate == null) {
			constructingDelegate.get().setEntityState(entityIn, state);
		} else {
			delegate.setEntityState(entityIn, state);
		}
	}

	@Override
	public void addBlockEvent(BlockPos pos, Block blockIn, int eventID, int eventParam) {
		if (delegate == null) {
			constructingDelegate.get().addBlockEvent(pos, blockIn, eventID, eventParam);
		} else {
			delegate.addBlockEvent(pos, blockIn, eventID, eventParam);
		}
	}

	@Override
	public ISaveHandler getSaveHandler() {
		return delegate == null ? constructingDelegate.get().getSaveHandler() : delegate.getSaveHandler();
	}

	@Override
	public WorldInfo getWorldInfo() {
		return delegate == null ? constructingDelegate.get().getWorldInfo() : delegate.getWorldInfo();
	}

	@Override
	public GameRules getGameRules() {
		return delegate == null ? constructingDelegate.get().getGameRules() : delegate.getGameRules();
	}

	@Override
	public float getThunderStrength(float delta) {
		return delegate == null ? constructingDelegate.get().getThunderStrength(delta)
				: delegate.getThunderStrength(delta);
	}

	@Override
	public void setThunderStrength(float strength) {
		if (delegate == null) {
			constructingDelegate.get().setThunderStrength(strength);
		} else {
			delegate.setThunderStrength(strength);
		}
	}

	@Override
	public float getRainStrength(float delta) {
		return delegate == null ? constructingDelegate.get().getRainStrength(delta) : delegate.getRainStrength(delta);
	}

	@Override
	public void setRainStrength(float strength) {
		if (delegate == null) {
			constructingDelegate.get().setRainStrength(strength);
		} else {
			delegate.setRainStrength(strength);
		}
	}

	@Override
	public boolean isThundering() {
		return delegate == null ? constructingDelegate.get().isThundering() : delegate.isThundering();
	}

	@Override
	public boolean isRaining() {
		return delegate == null ? constructingDelegate.get().isRaining() : delegate.isRaining();
	}

	@Override
	public boolean isRainingAt(BlockPos strikePosition) {
		return delegate == null ? constructingDelegate.get().isRainingAt(strikePosition)
				: delegate.isRainingAt(strikePosition);
	}

	@Override
	public boolean isBlockinHighHumidity(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().isBlockinHighHumidity(pos)
				: delegate.isBlockinHighHumidity(pos);
	}

	@Override
	public MapStorage getMapStorage() {
		return delegate == null ? constructingDelegate.get().getMapStorage() : delegate.getMapStorage();
	}

	@Override
	public void setData(String dataID, WorldSavedData worldSavedDataIn) {
		if (delegate == null) {
			constructingDelegate.get().setData(dataID, worldSavedDataIn);
		} else {
			delegate.setData(dataID, worldSavedDataIn);
		}
	}

	@Override
	public WorldSavedData loadData(Class<? extends WorldSavedData> clazz, String dataID) {
		return delegate == null ? constructingDelegate.get().loadData(clazz, dataID) : delegate.loadData(clazz, dataID);
	}

	@Override
	public int getUniqueDataId(String key) {
		return delegate == null ? constructingDelegate.get().getUniqueDataId(key) : delegate.getUniqueDataId(key);
	}

	@Override
	public void playBroadcastSound(int id, BlockPos pos, int data) {
		if (delegate == null) {
			constructingDelegate.get().playBroadcastSound(id, pos, data);
		} else {
			delegate.playBroadcastSound(id, pos, data);
		}
	}

	@Override
	public void playEvent(int type, BlockPos pos, int data) {
		if (delegate == null) {
			constructingDelegate.get().playEvent(type, pos, data);
		} else {
			delegate.playEvent(type, pos, data);
		}
	}

	@Override
	public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {
		if (delegate == null) {
			constructingDelegate.get().playEvent(player, type, pos, data);
		} else {
			delegate.playEvent(player, type, pos, data);
		}
	}

	@Override
	public int getHeight() {
		return delegate == null ? constructingDelegate.get().getHeight() : delegate.getHeight();
	}

	@Override
	public Random setRandomSeed(int p_72843_1_, int p_72843_2_, int p_72843_3_) {
		return delegate == null ? constructingDelegate.get().setRandomSeed(p_72843_1_, p_72843_2_, p_72843_3_)
				: delegate.setRandomSeed(p_72843_1_, p_72843_2_, p_72843_3_);
	}

	@Override
	public double getHorizon() {
		return delegate == null ? constructingDelegate.get().getHorizon() : delegate.getHorizon();
	}

	@Override
	public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
		if (delegate == null) {
			constructingDelegate.get().sendBlockBreakProgress(breakerId, pos, progress);
		} else {
			delegate.sendBlockBreakProgress(breakerId, pos, progress);
		}
	}

	@Override
	public Scoreboard getScoreboard() {
		return delegate == null ? constructingDelegate.get().getScoreboard() : delegate.getScoreboard();
	}

	@Override
	public DifficultyInstance getDifficultyForLocation(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getDifficultyForLocation(pos)
				: delegate.getDifficultyForLocation(pos);
	}

	@Override
	public EnumDifficulty getDifficulty() {
		return delegate == null ? constructingDelegate.get().getDifficulty() : delegate.getDifficulty();
	}

	@Override
	public int getSkylightSubtracted() {
		return delegate == null ? constructingDelegate.get().getSkylightSubtracted() : delegate.getSkylightSubtracted();
	}

	@Override
	public void setSkylightSubtracted(int newSkylightSubtracted) {
		if (delegate == null) {
			constructingDelegate.get().setSkylightSubtracted(newSkylightSubtracted);
		} else {
			delegate.setSkylightSubtracted(newSkylightSubtracted);
		}
	}

	@Override
	public int getLastLightningBolt() {
		return delegate == null ? constructingDelegate.get().getLastLightningBolt() : delegate.getLastLightningBolt();
	}

	@Override
	public void setLastLightningBolt(int lastLightningBoltIn) {
		if (delegate == null) {
			constructingDelegate.get().setLastLightningBolt(lastLightningBoltIn);
		} else {
			delegate.setLastLightningBolt(lastLightningBoltIn);
		}
	}

	@Override
	public VillageCollection getVillageCollection() {
		return delegate == null ? constructingDelegate.get().getVillageCollection() : delegate.getVillageCollection();
	}

	@Override
	public WorldBorder getWorldBorder() {
		return delegate == null ? constructingDelegate.get().getWorldBorder() : delegate.getWorldBorder();
	}

	@Override
	public boolean isSpawnChunk(int x, int z) {
		return delegate == null ? constructingDelegate.get().isSpawnChunk(x, z) : delegate.isSpawnChunk(x, z);
	}

	@Override
	public boolean isSideSolid(BlockPos pos, EnumFacing side) {
		return delegate == null ? constructingDelegate.get().isSideSolid(pos, side) : delegate.isSideSolid(pos, side);
	}

	@Override
	public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
		return delegate == null ? constructingDelegate.get().isSideSolid(pos, side, _default)
				: delegate.isSideSolid(pos, side, _default);
	}

	@Override
	public ImmutableSetMultimap<ChunkPos, Ticket> getPersistentChunks() {
		return delegate == null ? constructingDelegate.get().getPersistentChunks() : delegate.getPersistentChunks();
	}

	@Override
	public Iterator<Chunk> getPersistentChunkIterable(Iterator<Chunk> chunkIterator) {
		return delegate == null ? constructingDelegate.get().getPersistentChunkIterable(chunkIterator)
				: delegate.getPersistentChunkIterable(chunkIterator);
	}

	@Override
	public int countEntities(EnumCreatureType type, boolean forSpawnCount) {
		return delegate == null ? constructingDelegate.get().countEntities(type, forSpawnCount)
				: delegate.countEntities(type, forSpawnCount);
	}

	@Override
	public boolean equals(Object arg0) {
		return delegate == null ? constructingDelegate.get().equals(arg0) : delegate.equals(arg0);
	}

	@Override
	public ChunkProviderServer getChunkProvider() {
		throw new UnsupportedOperationException("Unsupported for WorldServerProxy");
	}

	@Override
	@Deprecated
	public int getChunksLowestHorizon(int x, int z) {
		return delegate == null ? constructingDelegate.get().getChunksLowestHorizon(x, z)
				: delegate.getChunksLowestHorizon(x, z);
	}

	@Override
	public int getCombinedLight(BlockPos pos, int lightValue) {
		return delegate == null ? constructingDelegate.get().getCombinedLight(pos, lightValue)
				: delegate.getCombinedLight(pos, lightValue);
	}

	@Override
	public IBlockState getBlockState(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getBlockState(pos) : delegate.getBlockState(pos);
	}

	@Override
	public List<AxisAlignedBB> getCollisionBoxes(Entity entityIn, AxisAlignedBB aabb) {
		return delegate == null ? constructingDelegate.get().getCollisionBoxes(entityIn, aabb)
				: delegate.getCollisionBoxes(entityIn, aabb);
	}

	@Override
	public boolean func_191503_g(Entity p_191503_1_) {
		return delegate == null ? constructingDelegate.get().func_191503_g(p_191503_1_)
				: delegate.func_191503_g(p_191503_1_);
	}

	@Override
	public float getCelestialAngle(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getCelestialAngle(partialTicks)
				: delegate.getCelestialAngle(partialTicks);
	}

	@Override
	public float getCurrentMoonPhaseFactor() {
		return delegate == null ? constructingDelegate.get().getCurrentMoonPhaseFactor()
				: delegate.getCurrentMoonPhaseFactor();
	}

	@Override
	public float getCelestialAngleRadians(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getCelestialAngleRadians(partialTicks)
				: delegate.getCelestialAngleRadians(partialTicks);
	}

	@Override
	public Vec3d getCloudColour(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getCloudColour(partialTicks)
				: delegate.getCloudColour(partialTicks);
	}

	@Override
	public Vec3d getCloudColorBody(float partialTicks) {
		return delegate == null ? constructingDelegate.get().getCloudColorBody(partialTicks)
				: delegate.getCloudColorBody(partialTicks);
	}

	@Override
	public float getBlockDensity(Vec3d vec, AxisAlignedBB bb) {
		return delegate == null ? constructingDelegate.get().getBlockDensity(vec, bb)
				: delegate.getBlockDensity(vec, bb);
	}

	@Override
	public boolean extinguishFire(EntityPlayer player, BlockPos pos, EnumFacing side) {
		return delegate == null ? constructingDelegate.get().extinguishFire(player, pos, side)
				: delegate.extinguishFire(player, pos, side);
	}

	@Override
	public <T extends Entity> T findNearestEntityWithinAABB(Class<? extends T> entityType, AxisAlignedBB aabb,
			T closestTo) {
		return delegate == null ? constructingDelegate.get().findNearestEntityWithinAABB(entityType, aabb, closestTo)
				: delegate.findNearestEntityWithinAABB(entityType, aabb, closestTo);
	}

	@Override
	public EntityPlayer getClosestPlayerToEntity(Entity entityIn, double distance) {
		return delegate == null ? constructingDelegate.get().getClosestPlayerToEntity(entityIn, distance)
				: delegate.getClosestPlayerToEntity(entityIn, distance);
	}

	@Override
	public EntityPlayer getClosestPlayer(double posX, double posY, double posZ, double distance, boolean spectator) {
		return delegate == null ? constructingDelegate.get().getClosestPlayer(posX, posY, posZ, distance, spectator)
				: delegate.getClosestPlayer(posX, posY, posZ, distance, spectator);
	}

	@Override
	public EntityPlayer getClosestPlayer(double x, double y, double z, double p_190525_7_,
			Predicate<Entity> p_190525_9_) {
		return delegate == null ? constructingDelegate.get().getClosestPlayer(x, y, z, p_190525_7_, p_190525_9_)
				: delegate.getClosestPlayer(x, y, z, p_190525_7_, p_190525_9_);
	}

	@Override
	public int getActualHeight() {
		return delegate == null ? constructingDelegate.get().getActualHeight() : delegate.getActualHeight();
	}

	@Override
	public Calendar getCurrentDate() {
		return delegate == null ? constructingDelegate.get().getCurrentDate() : delegate.getCurrentDate();
	}

	@Override
	public int getBlockLightOpacity(BlockPos pos) {
		return delegate == null ? constructingDelegate.get().getBlockLightOpacity(pos)
				: delegate.getBlockLightOpacity(pos);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return delegate == null ? constructingDelegate.get().hasCapability(capability, facing)
				: delegate.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return delegate == null ? constructingDelegate.get().getCapability(capability, facing)
				: delegate.getCapability(capability, facing);
	}

	@Override
	public MapStorage getPerWorldStorage() {
		return delegate == null ? constructingDelegate.get().getPerWorldStorage() : delegate.getPerWorldStorage();
	}

	@Override
	public LootTableManager getLootTableManager() {
		return delegate == null ? constructingDelegate.get().getLootTableManager() : delegate.getLootTableManager();
	}

	@Override
	public BlockPos findNearestStructure(String p_190528_1_, BlockPos p_190528_2_, boolean p_190528_3_) {
		return delegate == null ? constructingDelegate.get().findNearestStructure(p_190528_1_, p_190528_2_, p_190528_3_)
				: delegate.findNearestStructure(p_190528_1_, p_190528_2_, p_190528_3_);
	}

	@Override
	public int hashCode() {
		return delegate == null ? constructingDelegate.get().hashCode() : delegate.hashCode();
	}

	public void invalidateBlockReceiveRegion(int x1, int y1, int z1, int x2, int y2, int z2) {
		if (delegate == null) {
			constructingDelegate.get().invalidateBlockReceiveRegion(x1, y1, z1, x2, y2, z2);
		} else {
			delegate.invalidateBlockReceiveRegion(x1, y1, z1, x2, y2, z2);
		}
	}

	@Override
	public World init() {
		return delegate == null ? constructingDelegate.get().init() : delegate.init();
	}

	@Override
	public void initialize(WorldSettings settings) {
		if (delegate == null) {
			constructingDelegate.get().initialize(settings);
		} else {
			delegate.initialize(settings);
		}
	}

	@Override
	public void immediateBlockTick(BlockPos pos, IBlockState state, Random random) {
		if (delegate == null) {
			constructingDelegate.get().immediateBlockTick(pos, state, random);
		} else {
			delegate.immediateBlockTick(pos, state, random);
		}
	}

	@Override
	public String toString() {
		return delegate == null ? constructingDelegate.get().toString() : delegate.toString();
	}

	@Override
	public void unloadEntities(Collection<Entity> entityCollection) {
		if (delegate == null) {
			constructingDelegate.get().unloadEntities(entityCollection);
		} else {
			delegate.unloadEntities(entityCollection);
		}
	}

	@Override
	public void updateAllPlayersSleepingFlag() {
		if (delegate == null) {
			constructingDelegate.get().updateAllPlayersSleepingFlag();
		} else {
			delegate.updateAllPlayersSleepingFlag();
		}
	}

	@Override
	public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {
		if (delegate == null) {
			constructingDelegate.get().updateComparatorOutputLevel(pos, blockIn);
		} else {
			delegate.updateComparatorOutputLevel(pos, blockIn);
		}
	}

}
