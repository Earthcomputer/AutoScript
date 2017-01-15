package net.earthcomputer.autoscript.crafting;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.job.MultiJob;
import net.earthcomputer.autoscript.job.NopJob;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class CraftingPlanner {

	private CraftingPlanner() {
	}

	public static Job createCraftingJob(IInventory inventory, ItemStack target, IHelperBlockProvider helperBlocks) {
		return generateCraftTreeNode(new AlreadyTakenInfo(inventory), target, helperBlocks);
	}

	private static Job generateCraftTreeNode(AlreadyTakenInfo alreadyTaken, ItemStack toCreate,
			IHelperBlockProvider helperBlocks) {
		if (alreadyTaken.consumeItemStack(toCreate)) {
			return new NopJob();
		}
		recipeLoop: for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
			if (alreadyTaken.isRecipeAlreadyUsed(recipe)) {
				continue;
			}
			ItemStack recipeOutput = recipe.getRecipeOutput();
			if (recipeOutput.getItem() != toCreate.getItem()
					|| (toCreate.getItemDamage() != OreDictionary.WILDCARD_VALUE
							&& recipeOutput.getItemDamage() != toCreate.getItemDamage())) {
				continue;
			}
			ShapedRecipes shaped = convertToShapedRecipe(recipe);
			if (shaped == null) {
				continue;
			}
			alreadyTaken.useRecipe(recipe);
			int amountCrafted = shaped.getRecipeOutput().getCount();
			int numberOfCrafts = MathHelper.ceil((float) toCreate.getCount() / amountCrafted);
			ItemStack nodeOutput = recipeOutput.copy();
			nodeOutput.setCount(nodeOutput.getCount() * numberOfCrafts);
			List<Job> jobs = Lists.newArrayListWithCapacity(10);
			try {
				AlreadyTakenInfo tmpAlreadyTakenInfo = new AlreadyTakenInfo(alreadyTaken);
				for (int i = 0; i < shaped.recipeItems.length; i++) {
					if (!shaped.recipeItems[i].isEmpty()) {
						ItemStack stackNeeded = shaped.recipeItems[i].copy();
						stackNeeded.setCount(MathHelper
								.ceil((float) (stackNeeded.getCount() * toCreate.getCount()) / amountCrafted));
						Job whatGetsTheItem = generateCraftTreeNode(tmpAlreadyTakenInfo, stackNeeded, helperBlocks);
						if (whatGetsTheItem == null) {
							continue recipeLoop;
						}
						jobs.add(whatGetsTheItem);
					}
				}
				jobs.add(new CraftingJob(shaped, numberOfCrafts, helperBlocks));
				alreadyTaken.copyFrom(tmpAlreadyTakenInfo);
				tmpAlreadyTakenInfo = null;
				ItemStack leftOver = nodeOutput.copy();
				leftOver.setCount(nodeOutput.getCount() - toCreate.getCount());
				alreadyTaken.addItemStack(leftOver);
				return new MultiJob(jobs);
			} finally {
				alreadyTaken.unuseRecipe(recipe);
			}
		}
		for (Map.Entry<ItemStack, ItemStack> inputOutputPair : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
			ItemStack recipeOutput = inputOutputPair.getValue();
			if (recipeOutput.getItem() != toCreate.getItem()
					|| (toCreate.getItemDamage() != OreDictionary.WILDCARD_VALUE
							&& recipeOutput.getItemDamage() != toCreate.getItemDamage())) {
				continue;
			}
			ItemStack recipeInput = inputOutputPair.getKey();
			FurnaceRecipe furnaceRecipe = new FurnaceRecipe(recipeInput, recipeOutput);
			if (alreadyTaken.isFurnaceRecipeAlreadyUsed(furnaceRecipe)) {
				continue;
			}
			alreadyTaken.useFurnaceRecipe(furnaceRecipe);
			ItemStack stackNeeded = recipeInput.copy();
			stackNeeded.setCount(toCreate.getCount());
			try {
				Job whatGetsTheItem = generateCraftTreeNode(alreadyTaken, stackNeeded, helperBlocks);
				if (whatGetsTheItem == null) {
					continue;
				}
				FurnaceJob furnaceNode = new FurnaceJob(furnaceRecipe, toCreate.getCount(), helperBlocks);
				return new MultiJob(whatGetsTheItem, furnaceNode);
			} finally {
				alreadyTaken.unuseFurnaceRecipe(furnaceRecipe);
			}
		}
		return null;
	}

	private static ShapedRecipes convertToShapedRecipe(IRecipe recipe) {
		if (recipe instanceof ShapedRecipes) {
			return (ShapedRecipes) recipe;
		}
		if (recipe instanceof ShapelessRecipes) {
			ShapelessRecipes shapeless = (ShapelessRecipes) recipe;
			int numItems = shapeless.recipeItems.size();
			int recipeWidth = MathHelper.ceil(Math.sqrt(numItems));
			int recipeHeight = MathHelper.ceil((float) numItems / recipeWidth);
			ItemStack[] recipeItems = new ItemStack[recipeWidth * recipeHeight];
			int i;
			for (i = 0; i < numItems; i++) {
				recipeItems[i] = shapeless.recipeItems.get(i);
			}
			for (; i < recipeItems.length; i++) {
				recipeItems[i] = ItemStack.EMPTY;
			}
			return new ShapedRecipes(recipeWidth, recipeHeight, recipeItems, shapeless.getRecipeOutput());
		}
		if (recipe instanceof ShapedOreRecipe) {
			ShapedOreRecipe shapedOre = (ShapedOreRecipe) recipe;
			Object[] shapedOreInput = shapedOre.getInput();
			ItemStack[] recipeItems = new ItemStack[shapedOreInput.length];
			for (int i = 0; i < shapedOreInput.length; i++) {
				if (shapedOreInput[i] instanceof ItemStack) {
					recipeItems[i] = (ItemStack) shapedOreInput[i];
				} else {
					List<?> ores = (List<?>) shapedOreInput[i];
					if (ores.isEmpty()) {
						return null;
					} else {
						recipeItems[i] = (ItemStack) ores.get(0);
					}
				}
			}
			return new ShapedRecipes(shapedOre.getWidth(), shapedOre.getHeight(), recipeItems,
					shapedOre.getRecipeOutput());
		}
		if (recipe instanceof ShapelessOreRecipe) {
			ShapelessOreRecipe shapelessOre = (ShapelessOreRecipe) recipe;
			NonNullList<Object> shapelessOreInput = shapelessOre.getInput();
			int numItems = shapelessOreInput.size();
			int recipeWidth = MathHelper.ceil(Math.sqrt(numItems));
			int recipeHeight = MathHelper.ceil((float) numItems / recipeWidth);
			ItemStack[] recipeItems = new ItemStack[recipeWidth * recipeHeight];
			int i;
			for (i = 0; i < numItems; i++) {
				Object input = shapelessOreInput.get(i);
				if (input instanceof ItemStack) {
					recipeItems[i] = (ItemStack) input;
				} else {
					List<?> ores = (List<?>) input;
					if (ores.isEmpty()) {
						return null;
					} else {
						recipeItems[i] = (ItemStack) ores.get(0);
					}
				}
			}
			for (; i < recipeItems.length; i++) {
				recipeItems[i] = ItemStack.EMPTY;
			}
			return new ShapedRecipes(recipeWidth, recipeHeight, recipeItems, shapelessOre.getRecipeOutput());
		}
		return null;
	}

	private static class AlreadyTakenInfo {
		private Map<Pair<Item, Integer>, Integer> numItems;
		private Map<Item, List<Integer>> itemToDamage;
		private Set<IRecipe> alreadyUsedRecipes = Sets.newIdentityHashSet();
		private Set<FurnaceRecipe> alreadyUsedFurnaceRecipes = Sets.newHashSet();

		public AlreadyTakenInfo(IInventory inv) {
			numItems = Maps.newHashMapWithExpectedSize(inv.getSizeInventory());
			itemToDamage = Maps.newHashMapWithExpectedSize(inv.getSizeInventory());
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				addItemStack(inv.getStackInSlot(i));
			}
		}

		public AlreadyTakenInfo(AlreadyTakenInfo other) {
			this.numItems = Maps.newHashMap(other.numItems);
			this.itemToDamage = Maps.newHashMapWithExpectedSize(other.itemToDamage.size());
			for (Map.Entry<Item, List<Integer>> entry : other.itemToDamage.entrySet()) {
				this.itemToDamage.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
			}
			this.alreadyUsedRecipes.addAll(other.alreadyUsedRecipes);
			this.alreadyUsedFurnaceRecipes.addAll(other.alreadyUsedFurnaceRecipes);
		}

		public void copyFrom(AlreadyTakenInfo other) {
			this.numItems.clear();
			this.numItems.putAll(other.numItems);
			this.itemToDamage.clear();
			this.itemToDamage.putAll(other.itemToDamage);
			this.alreadyUsedRecipes.clear();
			this.alreadyUsedRecipes.addAll(other.alreadyUsedRecipes);
			this.alreadyUsedFurnaceRecipes.clear();
			this.alreadyUsedFurnaceRecipes.addAll(other.alreadyUsedFurnaceRecipes);
		}

		public void addItemStack(ItemStack stack) {
			Pair<Item, Integer> key = Pair.of(stack.getItem(), stack.getItemDamage());
			if (numItems.containsKey(key)) {
				numItems.put(key, numItems.get(key) + stack.getCount());
			} else {
				numItems.put(key, stack.getCount());
			}
			List<Integer> itemDamages = itemToDamage.get(key.getLeft());
			if (itemDamages == null) {
				itemDamages = Lists.newArrayList();
				itemToDamage.put(key.getLeft(), itemDamages);
			}
			if (!itemDamages.contains(key.getRight())) {
				itemDamages.add(key.getRight());
			}
		}

		public boolean consumeItemStack(ItemStack stack) {
			if (stack.isEmpty()) {
				return true;
			}
			List<Integer> damagesToRemove = Lists.newArrayList();
			Pair<Item, Integer> key = Pair.of(stack.getItem(), stack.getItemDamage());
			int count = stack.getCount();
			if (numItems.containsKey(key)) {
				int existingItems = numItems.get(key);
				if (existingItems > count) {
					numItems.put(key, existingItems - count);
					return true;
				} else if (existingItems == count) {
					numItems.remove(key);
					if (itemToDamage.containsKey(key.getLeft())) {
						itemToDamage.get(key.getLeft()).remove(key.getRight());
					}
					return true;
				} else {
					count -= existingItems;
					damagesToRemove.add(key.getRight());
				}
			}
			if (key.getRight() == OreDictionary.WILDCARD_VALUE) {
				if (!itemToDamage.containsKey(key.getLeft())) {
					return false;
				}
				for (Integer damage : itemToDamage.get(key.getLeft())) {
					if (damage == OreDictionary.WILDCARD_VALUE) {
						continue;
					}
					Pair<Item, Integer> otherDamageKey = Pair.of(key.getLeft(), damage);
					if (!numItems.containsKey(otherDamageKey)) {
						continue;
					}
					int existingItems = numItems.get(otherDamageKey);
					boolean successful = false;
					if (existingItems > count) {
						numItems.put(otherDamageKey, existingItems - count);
						successful = true;
					} else if (existingItems == count) {
						damagesToRemove.add(damage);
						successful = true;
					} else {
						count -= existingItems;
						damagesToRemove.add(damage);
					}
					if (successful) {
						for (Integer damageToRemove : damagesToRemove) {
							numItems.remove(Pair.of(key.getLeft(), damageToRemove));
							if (itemToDamage.containsKey(key.getLeft())) {
								itemToDamage.get(key.getLeft()).remove(damageToRemove);
							}
						}
						return true;
					}
				}
			}
			return false;
		}

		public void useRecipe(IRecipe recipe) {
			alreadyUsedRecipes.add(recipe);
		}

		public void unuseRecipe(IRecipe recipe) {
			alreadyUsedRecipes.remove(recipe);
		}

		public boolean isRecipeAlreadyUsed(IRecipe recipe) {
			return alreadyUsedRecipes.contains(recipe);
		}

		public void useFurnaceRecipe(FurnaceRecipe recipe) {
			alreadyUsedFurnaceRecipes.add(recipe);
		}

		public void unuseFurnaceRecipe(FurnaceRecipe recipe) {
			alreadyUsedFurnaceRecipes.remove(recipe);
		}

		public boolean isFurnaceRecipeAlreadyUsed(FurnaceRecipe recipe) {
			return alreadyUsedFurnaceRecipes.contains(recipe);
		}
	}

}
