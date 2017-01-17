package net.earthcomputer.autoscript.crafting;

import net.earthcomputer.autoscript.job.InstantActionJob;
import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.job.MultiJob;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.earthcomputer.autoscript.scripts.ScriptInput.BlockHitRequirements;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class DefaultHelperBlockProvider implements IHelperBlockProvider {

	private boolean allowCraftingTables;
	private boolean allowFurnaces;
	private boolean hasTriedPlaceCraftingTable = false;
	private BlockPos craftingTablePos = null;
	private BlockSearchResult craftingTablePlaceRay = null;
	private boolean hasTriedPlaceFurnace = false;
	private BlockPos furnacePos;
	private BlockSearchResult furnacePlaceRay = null;

	public DefaultHelperBlockProvider() {
		this(true, true);
	}

	public DefaultHelperBlockProvider(boolean allowCraftingTables, boolean allowFurnaces) {
		this.allowCraftingTables = allowCraftingTables;
		this.allowFurnaces = allowFurnaces;
	}

	@Override
	public boolean tryPlaceCraftingTable(Job jobToEnhance) {
		if (!allowCraftingTables) {
			return true;
		}
		if (hasTriedPlaceCraftingTable) {
			return true;
		}
		hasTriedPlaceCraftingTable = true;
		Job job = ScriptInput.selectOrCraftItem(Minecraft.getMinecraft().player, new ItemStack(Blocks.CRAFTING_TABLE),
				new DefaultHelperBlockProvider(false, allowFurnaces));
		if (job == null) {
			return true;
		}
		craftingTablePlaceRay = searchPlacementPos();
		if (craftingTablePlaceRay == null) {
			return true;
		}
		craftingTablePos = craftingTablePlaceRay.placePos;
		jobToEnhance.startChild(new MultiJob(job, new InstantActionJob(new Runnable() {
			@Override
			public void run() {
				EntityPlayerSP player = Minecraft.getMinecraft().player;
				player.setLocationAndAngles(player.posX, player.posY, player.posZ, craftingTablePlaceRay.yaw,
						craftingTablePlaceRay.pitch);
				Minecraft.getMinecraft().playerController.processRightClickBlock(player, Minecraft.getMinecraft().world,
						craftingTablePlaceRay.ray.getBlockPos(), craftingTablePlaceRay.ray.sideHit,
						craftingTablePlaceRay.ray.hitVec, EnumHand.MAIN_HAND);
			}
		})));
		return false;
	}

	@Override
	public BlockPos getCraftingTablePos() {
		return craftingTablePos;
	}

	@Override
	public boolean tryPlaceFurnace(Job jobToEnhance) {
		if (!allowFurnaces) {
			return true;
		}
		if (hasTriedPlaceFurnace) {
			return true;
		}
		hasTriedPlaceFurnace = true;
		Job job = ScriptInput.selectOrCraftItem(Minecraft.getMinecraft().player, new ItemStack(Blocks.FURNACE),
				new DefaultHelperBlockProvider(allowCraftingTables, false));
		if (job == null) {
			return true;
		}
		furnacePlaceRay = searchPlacementPos();
		if (furnacePlaceRay == null) {
			return true;
		}
		furnacePos = furnacePlaceRay.placePos;
		jobToEnhance.startChild(new MultiJob(job, new InstantActionJob(new Runnable() {
			@Override
			public void run() {
				EntityPlayerSP player = Minecraft.getMinecraft().player;
				player.setLocationAndAngles(player.posX, player.posY, player.posZ, furnacePlaceRay.yaw,
						furnacePlaceRay.pitch);
				Minecraft.getMinecraft().playerController.processRightClickBlock(player, Minecraft.getMinecraft().world,
						furnacePlaceRay.ray.getBlockPos(), furnacePlaceRay.ray.sideHit, furnacePlaceRay.ray.hitVec,
						EnumHand.MAIN_HAND);
			}
		})));
		return false;
	}

	@Override
	public BlockPos getFurnacePos() {
		return furnacePos;
	}

	private static BlockSearchResult searchPlacementPos() {
		World world = Minecraft.getMinecraft().world;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		float reachDistance = Minecraft.getMinecraft().playerController.getBlockReachDistance();
		for (int dy = 0; dy <= reachDistance; dy = dy > 0 ? -dy : 1 - dy) {
			for (int dist = 0; dist <= reachDistance; dist++) {
				for (int dx = 0; dx <= reachDistance; dx = dx > 0 ? -dx : 1 - dx) {
					for (int dz = 0; dz <= reachDistance; dz = dz > 0 ? -dz : 1 - dz) {
						BlockPos pos = new BlockPos((int) player.posX + dx, (int) player.posY + player.eyeHeight + dy,
								(int) player.posZ + dz);
						IBlockState state = world.getBlockState(pos);
						if (!state.getBlock().isAir(state, world, pos) && !state.getBlock().isReplaceable(world, pos)) {
							continue;
						}
						BlockHitRequirements hitRequirements = ScriptInput.getBlockHitRequirements(world, player, pos);
						if (hitRequirements != null) {
							return new BlockSearchResult(pos, hitRequirements.getRay(), hitRequirements.getYaw(),
									hitRequirements.getPitch());
						}
					}
				}
			}
		}
		return null;
	}

	private static class BlockSearchResult {
		public BlockPos placePos;
		public RayTraceResult ray;
		public float yaw;
		public float pitch;

		public BlockSearchResult(BlockPos pos, RayTraceResult ray, float yaw, float pitch) {
			this.placePos = pos;
			this.ray = ray;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

}
