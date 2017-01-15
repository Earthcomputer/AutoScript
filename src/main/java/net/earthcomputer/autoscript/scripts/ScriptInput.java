package net.earthcomputer.autoscript.scripts;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

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
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

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
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		double dx = pos.getX() + 0.5 - player.posX;
		double dz = pos.getZ() + 0.5 - player.posZ;
		player.setLocationAndAngles(player.posX, player.posY, player.posZ,
				(float) Math.toDegrees(MathHelper.atan2(dz, dx)),
				(float) Math.toDegrees(MathHelper.atan2(pos.getY() + 0.5 - player.posY, Math.sqrt(dx * dx + dz * dz))));
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

}
