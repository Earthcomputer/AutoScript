package net.earthcomputer.autoscript.crafting;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.oredict.OreDictionary;

public class CraftingJob extends Job {

	private ShapedRecipes recipe;
	private int numberOfCrafts;
	private IHelperBlockProvider helperBlocks;
	private int ticksWaited = 0;
	private int remainingCrafts;
	private EnumState state;

	public CraftingJob(ShapedRecipes recipe, int numberOfCrafts, IHelperBlockProvider helperBlocks) {
		this.recipe = recipe;
		this.numberOfCrafts = numberOfCrafts;
		this.helperBlocks = helperBlocks;
	}

	public boolean requiresCraftingTable() {
		return recipe.recipeWidth > 2 || recipe.recipeHeight > 2;
	}

	@Override
	protected void startExecute() {
		remainingCrafts = numberOfCrafts;
		resumeExecute();
	}

	@Override
	protected void resumeExecute() {
		if (requiresCraftingTable()) {
			BlockPos craftingTablePos = helperBlocks.getCraftingTablePos();
			ScriptInput.faceBlock(craftingTablePos);
			RayTraceResult rayTraceResult = Minecraft.getMinecraft().player
					.rayTrace(Minecraft.getMinecraft().playerController.getBlockReachDistance(), 0);
			if (rayTraceResult.typeOfHit != RayTraceResult.Type.BLOCK) {
				fail();
			} else if (!rayTraceResult.getBlockPos().equals(craftingTablePos)) {
				fail();
			} else if (Minecraft.getMinecraft().world.getBlockState(craftingTablePos)
					.getBlock() != Blocks.CRAFTING_TABLE) {
				fail();
			} else {
				Minecraft.getMinecraft().playerController.processRightClickBlock(Minecraft.getMinecraft().player,
						Minecraft.getMinecraft().world, craftingTablePos, rayTraceResult.sideHit, rayTraceResult.hitVec,
						EnumHand.MAIN_HAND);
				ticksWaited = 0;
				state = EnumState.WAIT_OPEN_CRAFTING_TABLE;
			}
		} else {
			doCraft(Minecraft.getMinecraft().player.inventoryContainer, 2);
			if (isRunning()) {
				stop();
			}
		}
	}

	@Override
	protected void updateExecute() {
		if (state == EnumState.WAIT_OPEN_CRAFTING_TABLE) {
			if (Minecraft.getMinecraft().player.openContainer instanceof ContainerWorkbench) {
				doCraft(Minecraft.getMinecraft().player.openContainer, 3);
				if (isRunning()) {
					stop();
				}
			} else if (ticksWaited++ > 80) {
				fail();
			}
		}
	}

	private void doCraft(Container craftingContainer, int craftMatrixSize) {
		// Slot 0 is output, slots 1 to craftMatrixSize^2 are inputs
		while (remainingCrafts > 0) {
			int minInputStackSize = Integer.MAX_VALUE;
			for (int i = 0; i < recipe.recipeItems.length; i++) {
				if (recipe.recipeItems[i].isEmpty()) {
					continue;
				}
				int targetSlotIndex = (i % recipe.recipeWidth) + (i / recipe.recipeWidth) * craftMatrixSize + 1;
				Slot targetSlot = craftingContainer.getSlot(targetSlotIndex);
				if (targetSlot.getHasStack()) {
					if (targetSlot.getStack().getCount() < minInputStackSize) {
						minInputStackSize = targetSlot.getStack().getCount();
					}
					continue;
				}
				ItemStack input = recipe.recipeItems[i];
				Slot inputSlot = null;
				for (int j = 1 + craftMatrixSize * craftMatrixSize, e = 1 + craftMatrixSize * craftMatrixSize
						+ 36; j < e; j++) {
					Slot slot = craftingContainer.getSlot(j);
					if (slot.getStack().getItem() == input.getItem()
							&& (input.getItemDamage() == OreDictionary.WILDCARD_VALUE
									|| slot.getStack().getItemDamage() == input.getItemDamage())) {
						inputSlot = slot;
						break;
					}
				}
				if (inputSlot == null) {
					fail();
					return;
				}
				int amountTransferred = ScriptInput.moveSome(craftingContainer, inputSlot, targetSlot, remainingCrafts);
				if (amountTransferred < minInputStackSize) {
					minInputStackSize = amountTransferred;
				}
			}
			if (!ScriptInput.shiftClick(craftingContainer, craftingContainer.getSlot(0))) {
				fail();
				return;
			}
			remainingCrafts -= minInputStackSize;
		}
	}

	@Override
	protected void pauseExecute() {
		if (requiresCraftingTable()) {
			Container openContainer = Minecraft.getMinecraft().player.openContainer;
			if (openContainer instanceof ContainerWorkbench) {
				for (int i = 9; i >= 0; i--) {
					if (!ScriptInput.shiftClick(openContainer, openContainer.getSlot(i))) {
						fail();
						break;
					}
				}
			}
			Minecraft.getMinecraft().player.closeScreen();
		}
	}

	private static enum EnumState {
		WAIT_OPEN_CRAFTING_TABLE
	}

}
