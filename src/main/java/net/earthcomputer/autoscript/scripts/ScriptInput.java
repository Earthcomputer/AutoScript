package net.earthcomputer.autoscript.scripts;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.earthcomputer.autoscript.crafting.CraftingPlanner;
import net.earthcomputer.autoscript.crafting.IHelperBlockProvider;
import net.earthcomputer.autoscript.job.InstantActionJob;
import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.job.MultiJob;
import net.earthcomputer.autoscript.job.NopJob;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class ScriptInput extends MovementInput {

	public static boolean isRightClickPressed = false;

	public static void holdRightClick() {
		isRightClickPressed = true;
		int keyCode = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode();
		KeyBinding.setKeyBindState(keyCode, true);
		KeyBinding.onTick(keyCode);
	}

	public static void updateTick() {
		isRightClickPressed = false;
	}

	public static void faceBlock(BlockPos pos) {
		facePos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	public static void facePos(double x, double y, double z) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		double dx = x - player.posX;
		double dy = y - player.posY;
		double dz = z - player.posZ;
		player.setLocationAndAngles(player.posX, player.posY, player.posZ,
				(float) Math.toDegrees(MathHelper.atan2(dz, dx)),
				(float) Math.toDegrees(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
	}

	public static Job selectOrCraftItem(final EntityPlayerSP player, final ItemStack toSelect,
			IHelperBlockProvider helperBlocks) {
		final Predicate<ItemStack> predicate = new Predicate<ItemStack>() {
			@Override
			public boolean apply(ItemStack stack) {
				return stack.getItem() == toSelect.getItem()
						&& (toSelect.getItemDamage() == OreDictionary.WILDCARD_VALUE
								|| stack.getItemDamage() == toSelect.getItemDamage());
			}
		};
		final Comparator<ItemStack> comparator = new Comparator<ItemStack>() {
			@Override
			public int compare(ItemStack first, ItemStack second) {
				return 0;
			}
		};
		if (selectItemInInventory(player, predicate, comparator)) {
			return new NopJob();
		}
		Job craftingJob = CraftingPlanner.createCraftingJob(player.inventory, toSelect, helperBlocks);
		if (craftingJob == null) {
			return null;
		}
		return new MultiJob(craftingJob, new InstantActionJob(new Runnable() {
			@Override
			public void run() {
				selectItemInInventory(player, predicate, comparator);
			}
		}));
	}

	public static boolean selectItemInInventory(EntityPlayerSP player, Predicate<ItemStack> predicate,
			final Comparator<ItemStack> preference) {
		List<Slot> items = Lists.newArrayList();
		for (Slot slot : player.inventoryContainer.inventorySlots) {
			if (predicate.apply(slot.getStack())) {
				items.add(slot);
			}
		}
		if (items.isEmpty()) {
			return false;
		}
		Collections.sort(items, new Comparator<Slot>() {
			@Override
			public int compare(Slot first, Slot second) {
				return preference.compare(first.getStack(), second.getStack());
			}
		});
		Minecraft.getMinecraft().playerController.windowClick(player.inventoryContainer.windowId,
				items.get(items.size() - 1).slotNumber, player.inventory.currentItem, ClickType.SWAP, player);
		return true;
	}

	public static void swapSlots(Container openContainer, Slot first, Slot second) {
		PlayerControllerMP playerController = Minecraft.getMinecraft().playerController;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		boolean firstHadStack = first.getHasStack(), secondHadStack = second.getHasStack();
		if (firstHadStack) {
			playerController.windowClick(openContainer.windowId, first.slotNumber, 0, ClickType.PICKUP, player);
		}
		if (firstHadStack || secondHadStack) {
			playerController.windowClick(openContainer.windowId, second.slotNumber, 0, ClickType.PICKUP, player);
		}
		if (secondHadStack) {
			playerController.windowClick(openContainer.windowId, first.slotNumber, 0, ClickType.PICKUP, player);
		}
	}

	public static int moveSome(Container openContainer, Slot from, Slot to, int amount) {
		if (!from.getHasStack()) {
			return 0;
		}
		PlayerControllerMP playerController = Minecraft.getMinecraft().playerController;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		int amountTransfered = amount;
		if (from.getStack().getCount() <= amount) {
			amountTransfered = from.getStack().getCount();
			playerController.windowClick(openContainer.windowId, from.slotNumber, 0, ClickType.PICKUP, player);
		} else {
			for (int i = 0; i < amount; i++) {
				playerController.windowClick(openContainer.windowId, from.slotNumber, 1, ClickType.PICKUP, player);
			}
		}
		playerController.windowClick(openContainer.windowId, to.slotNumber, 0, ClickType.PICKUP, player);
		return amountTransfered;
	}

	public static boolean shiftClick(Container container, Slot slot) {
		ItemStack originalStack = slot.getStack().copy();
		Minecraft.getMinecraft().playerController.windowClick(container.windowId, slot.slotNumber, 1,
				ClickType.QUICK_MOVE, Minecraft.getMinecraft().player);
		return slot.getStack().getItem() != originalStack.getItem()
				|| slot.getStack().getItemDamage() != originalStack.getItemDamage();
	}

	public static BlockHitRequirements getBlockHitRequirements(World world, EntityPlayerSP player, BlockPos pos) {
		Vec3d playerEyePos = player.getPositionEyes(1);
		float reachDistanceSq = Minecraft.getMinecraft().playerController.getBlockReachDistance();
		reachDistanceSq *= reachDistanceSq;
		Vec3d hitPos;
		AxisAlignedBB bounds;
		RayTraceResult ray;

		// Try using the replaceable block's bounding box
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock().isReplaceable(world, pos)) {
			bounds = state.getBoundingBox(world, pos);
			if (bounds != Block.NULL_AABB) {
				for (EnumFacing side : EnumFacing.values()) {
					hitPos = getPosOnSideOfBoundingBox(bounds, side);
					ray = world.rayTraceBlocks(playerEyePos, hitPos, false, false, true);
					if (ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.getBlockPos().equals(pos)) {
						double dx = hitPos.xCoord - playerEyePos.xCoord;
						double dy = hitPos.yCoord - playerEyePos.yCoord;
						double dz = hitPos.zCoord - playerEyePos.zCoord;
						if (dx * dx + dy * dy + dz * dz <= reachDistanceSq) {
							return new BlockHitRequirements((float) Math.toDegrees(MathHelper.atan2(dz, dx)),
									(float) Math.toDegrees(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz))), ray);
						}
					}
				}
			}
		}

		// Try using the surrounding blocks to place against
		for (EnumFacing side : EnumFacing.values()) {
			pos = pos.offset(side);
			state = world.getBlockState(pos);
			side = side.getOpposite();
			bounds = state.getBoundingBox(world, pos);
			if (bounds != Block.NULL_AABB) {
				hitPos = getPosOnSideOfBoundingBox(bounds, side);
				ray = world.rayTraceBlocks(playerEyePos, hitPos, false, true, true);
				if (ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.getBlockPos().equals(pos)
						&& ray.sideHit == side) {
					double dx = hitPos.xCoord - playerEyePos.xCoord;
					double dy = hitPos.yCoord - playerEyePos.yCoord;
					double dz = hitPos.zCoord - playerEyePos.zCoord;
					if (dx * dx + dy * dy + dz * dz <= reachDistanceSq) {
						return new BlockHitRequirements((float) Math.toDegrees(MathHelper.atan2(dz, dx)),
								(float) Math.toDegrees(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz))), ray);
					}
				}
			}
		}

		// No placement is possible
		return null;
	}

	private static Vec3d getPosOnSideOfBoundingBox(AxisAlignedBB box, EnumFacing side) {
		switch (side) {
		case DOWN:
			return new Vec3d(box.minX + (box.maxX - box.minX) / 2, box.minY, box.minZ + (box.maxZ - box.minZ) / 2);
		case EAST:
			return new Vec3d(box.maxX, box.minY + (box.maxY - box.minY) / 2, box.minZ + (box.maxZ - box.minZ) / 2);
		case NORTH:
			return new Vec3d(box.minX + (box.maxX - box.minX) / 2, box.minY + (box.maxY - box.minY) / 2, box.minZ);
		case SOUTH:
			return new Vec3d(box.minX + (box.maxX - box.minX) / 2, box.minY + (box.maxY - box.minY) / 2, box.maxZ);
		case UP:
			return new Vec3d(box.minX + (box.maxX - box.minX) / 2, box.maxY, box.minZ + (box.maxZ - box.minZ) / 2);
		case WEST:
			return new Vec3d(box.minX, box.minY + (box.maxY - box.minY) / 2, box.minZ + (box.maxZ - box.minZ) / 2);
		default:
			throw new IllegalArgumentException("Invalid facing");
		}
	}

	public static void startPlayerInventoryHack(EntityPlayerSP player) {
		if (!(player.inventory instanceof ScriptInventoryPlayer)) {
			ScriptInventoryPlayer newInventory = new ScriptInventoryPlayer(player.inventory);
			updateContainerPlayer((ContainerPlayer) player.inventoryContainer, player.inventory, newInventory);
			player.inventory = newInventory;
		}
	}

	public static void stopPlayerInventoryHack(EntityPlayerSP player) {
		if (player.inventory instanceof ScriptInventoryPlayer) {
			ScriptInventoryPlayer scriptInv = (ScriptInventoryPlayer) player.inventory;
			copyPlayerInventory(scriptInv, scriptInv.parent);
			updateContainerPlayer((ContainerPlayer) player.inventoryContainer, scriptInv, scriptInv.parent);
			player.inventory = scriptInv.parent;
		}
	}

	private static void copyPlayerInventory(InventoryPlayer from, InventoryPlayer to) {
		for (int i = from.getSizeInventory() - 1; i >= 0; i--) {
			to.setInventorySlotContents(i, from.getStackInSlot(i));
		}
		to.currentItem = from.currentItem;
		to.setItemStack(from.getItemStack());
		to.inventoryChanged = from.inventoryChanged;
	}

	private static void updateContainerPlayer(ContainerPlayer containerPlayer, InventoryPlayer oldInventory,
			InventoryPlayer newInventory) {
		for (Slot slot : containerPlayer.inventorySlots) {
			if (slot.inventory == oldInventory) {
				slot.inventory = newInventory;
			}
		}
	}

	private static class ScriptInventoryPlayer extends InventoryPlayer {
		private InventoryPlayer parent;

		public ScriptInventoryPlayer(InventoryPlayer parent) {
			super(parent.player);
			this.parent = parent;
			copyPlayerInventory(parent, this);
		}

		@Override
		public void changeCurrentItem(int direction) {
			// nop, should set the index directly
		}
	}

	public static class BlockHitRequirements {
		private float yaw;
		private float pitch;
		private RayTraceResult ray;

		public BlockHitRequirements(float yaw, float pitch, RayTraceResult ray) {
			this.yaw = yaw;
			this.pitch = pitch;
			this.ray = ray;
		}

		public float getYaw() {
			return yaw;
		}

		public float getPitch() {
			return pitch;
		}

		public RayTraceResult getRay() {
			return ray;
		}
	}

}
