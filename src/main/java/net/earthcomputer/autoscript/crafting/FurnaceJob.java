package net.earthcomputer.autoscript.crafting;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public class FurnaceJob extends Job {

	private FurnaceRecipe recipe;
	private int numberOfCrafts;
	private IHelperBlockProvider helperBlocks;
	private int remainingCrafts;
	private int ticksWaited = 0;
	private EnumState state;

	public FurnaceJob(FurnaceRecipe recipe, int numberOfCrafts, IHelperBlockProvider helperBlocks) {
		this.recipe = recipe;
		this.numberOfCrafts = numberOfCrafts;
		this.helperBlocks = helperBlocks;
	}

	@Override
	public void startExecute() {
		remainingCrafts = numberOfCrafts;
		resumeExecute();
	}

	@Override
	protected void resumeExecute() {
		BlockPos furnacePos = helperBlocks.getFurnacePos();
		ScriptInput.faceBlock(furnacePos);
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		RayTraceResult rayTraceResult = player
				.rayTrace(Minecraft.getMinecraft().playerController.getBlockReachDistance(), 0);
		if (rayTraceResult.typeOfHit != RayTraceResult.Type.BLOCK) {
			fail();
		} else if (!rayTraceResult.getBlockPos().equals(furnacePos)) {
			fail();
		} else {
			Block block = Minecraft.getMinecraft().world.getBlockState(furnacePos).getBlock();
			if (block != Blocks.FURNACE || block != Blocks.LIT_FURNACE) {
				fail();
			} else {
				Minecraft.getMinecraft().playerController.processRightClickBlock(player, Minecraft.getMinecraft().world,
						furnacePos, rayTraceResult.sideHit, rayTraceResult.hitVec, EnumHand.MAIN_HAND);
				ticksWaited = 0;
				state = EnumState.WAIT_OPEN_CONTAINER;
			}
		}
	}

	@Override
	protected void updateExecute() {
		switch (state) {
		case WAIT_OPEN_CONTAINER: {
			if (Minecraft.getMinecraft().player.openContainer instanceof ContainerFurnace) {
				state = EnumState.SMELTING;
			} else if (ticksWaited++ > 80) {
				fail();
			}
			break;
		}
		case SMELTING: {
			Container openContainer = Minecraft.getMinecraft().player.openContainer;
			if (!(openContainer instanceof ContainerFurnace)) {
				fail();
			} else if (remainingCrafts <= 0) {
				stop();
			} else {
				Slot fuelSlot = openContainer.getSlot(1);
				Slot inputSlot = openContainer.getSlot(0);
				Slot outputSlot = openContainer.getSlot(2);
				if (outputSlot.getHasStack()) {
					remainingCrafts -= outputSlot.getStack().getCount();
					if (!ScriptInput.shiftClick(openContainer, outputSlot)) {
						fail();
						return;
					}
				}
				if (!TileEntityFurnace.isItemFuel(fuelSlot.getStack())) {
					Slot bestFuelSlot = null;
					int bestFuelBurnTime = Integer.MAX_VALUE;
					for (int i = 3; i < 39; i++) {
						Slot slot = openContainer.getSlot(i);
						int burnTime = TileEntityFurnace.getItemBurnTime(slot.getStack());
						if (0 < burnTime && burnTime < bestFuelBurnTime) {
							bestFuelSlot = slot;
							bestFuelBurnTime = burnTime;
						}
					}
					if (bestFuelSlot == null) {
						fail();
						return;
					}
					ScriptInput.swapSlots(openContainer, bestFuelSlot, fuelSlot);
				}
				if (!inputSlot.getHasStack()) {
					ItemStack input = recipe.getInput();
					Slot bestInputSlot = null;
					for (int i = 3; i < 39; i++) {
						Slot slot = openContainer.getSlot(i);
						if (slot.getStack().getItem() == input.getItem()
								&& slot.getStack().getItemDamage() == input.getItemDamage()) {
							bestInputSlot = slot;
							break;
						}
					}
					if (bestInputSlot == null) {
						fail();
						return;
					}
					ScriptInput.swapSlots(openContainer, bestInputSlot, inputSlot);
				}
			}
			break;
		}
		}
	}

	@Override
	protected void pauseExecute() {
		Container openContainer = Minecraft.getMinecraft().player.openContainer;
		if (!(openContainer instanceof ContainerFurnace)) {
			Minecraft.getMinecraft().player.closeScreen();
			return;
		}
		Slot fuelSlot = openContainer.getSlot(1);
		Slot inputSlot = openContainer.getSlot(0);
		Slot outputSlot = openContainer.getSlot(2);
		if (inputSlot.getHasStack()) {
			ScriptInput.shiftClick(openContainer, inputSlot);
		}
		if (fuelSlot.getHasStack()) {
			ScriptInput.shiftClick(openContainer, fuelSlot);
		}
		if (outputSlot.getHasStack()) {
			ScriptInput.shiftClick(openContainer, outputSlot);
		}
		Minecraft.getMinecraft().player.closeScreen();
	}

	private static enum EnumState {
		WAIT_OPEN_CONTAINER, SMELTING
	}

}
