package net.earthcomputer.autoscript.crafting;

import com.google.common.base.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class FurnaceRecipe {
	private ItemStack input;
	private ItemStack output;

	public FurnaceRecipe(ItemStack input, ItemStack output) {
		this.input = input;
		this.output = output;
	}

	public ItemStack getInput() {
		return input;
	}

	public ItemStack getOutput() {
		return output;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(itemStackHashCode(input), itemStackHashCode(output));
	}

	private static int itemStackHashCode(ItemStack stack) {
		return Objects.hashCode(Item.getIdFromItem(stack.getItem()), stack.getItemDamage(), stack.getCount());
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		} else if (!(other instanceof FurnaceRecipe)) {
			return false;
		} else {
			return equals((FurnaceRecipe) other);
		}
	}

	public boolean equals(FurnaceRecipe other) {
		return ItemStack.areItemStacksEqual(input, other.input) && ItemStack.areItemStacksEqual(output, other.output);
	}
}