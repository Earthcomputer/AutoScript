package net.earthcomputer.autoscript.scripts.stayalive.defense;

import java.util.Comparator;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import net.earthcomputer.autoscript.InputBlocker;
import net.earthcomputer.autoscript.crafting.CraftingPlanner;
import net.earthcomputer.autoscript.crafting.DefaultHelperBlockProvider;
import net.earthcomputer.autoscript.fake.EntityPlayerProxy;
import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.earthcomputer.autoscript.scripts.stayalive.StayAliveHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;

public class BasicDefenseJob extends Job {

	protected StayAliveHelper stayAliveHelper;
	protected EntityMob opponent;
	protected EntityPlayerProxy fakePlayer;

	public BasicDefenseJob(StayAliveHelper stayAliveHelper, EntityMob opponent) {
		this.stayAliveHelper = stayAliveHelper;
		this.opponent = opponent;
	}

	@Override
	protected void updateExecute() {
		if (opponent.isDead) {
			stop();
		} else {
			fakePlayer.getLookHelper().setLookPositionWithEntity(opponent, 10, 10);
			RayTraceResult ray = Minecraft.getMinecraft().objectMouseOver;
			if (ray.typeOfHit == RayTraceResult.Type.ENTITY && ray.entityHit == opponent
					&& Minecraft.getMinecraft().player.getCooledAttackStrength(0) == 1) {
				Minecraft.getMinecraft().playerController.attackEntity(Minecraft.getMinecraft().player, opponent);
			}
		}
	}

	@Override
	protected void pauseExecute() {
		InputBlocker.popFakePlayer();
	}

	@Override
	protected void resumeExecute() {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		Predicate<ItemStack> predicate = new Predicate<ItemStack>() {
			@Override
			public boolean apply(ItemStack stack) {
				return getAttackDamage(stack) > 0;
			}
		};
		Comparator<ItemStack> preference = new Comparator<ItemStack>() {
			@Override
			public int compare(ItemStack first, ItemStack second) {
				return (int) Math.signum(getAttackDamage(second) - getAttackDamage(first));
			}
		};
		if (!ScriptInput.selectItemInInventory(player, predicate, preference)) {
			Job craftingJob = CraftingPlanner.createCraftingJob(player.inventory, new ItemStack(Items.IRON_SWORD),
					new DefaultHelperBlockProvider(true, false));
			if (craftingJob == null) {
				craftingJob = CraftingPlanner.createCraftingJob(player.inventory, new ItemStack(Items.STONE_SWORD),
						new DefaultHelperBlockProvider(true, false));
				if (craftingJob == null) {
					craftingJob = CraftingPlanner.createCraftingJob(player.inventory, new ItemStack(Items.WOODEN_SWORD),
							new DefaultHelperBlockProvider(true, false));
				}
			}
			if (craftingJob != null) {
				ScriptInput.selectItemInInventory(player, predicate, preference);
			}
		}
		final double closestDist = player.width + opponent.width + 1;
		fakePlayer = new EntityPlayerProxy(player);
		fakePlayer.tasks.addTask(1, new AIStayOutOfRange(fakePlayer, opponent, closestDist, closestDist * 1.5));
		fakePlayer.tasks.addTask(2, new AIMoveWithinMeleeDistance(fakePlayer, closestDist));
		fakePlayer.setAttackTarget(opponent);
		InputBlocker.pushFakePlayer(fakePlayer);
	}

	@Override
	protected void stopExecute() {
		pauseExecute();
		stayAliveHelper.setDefending(false);
	}

	private double getAttackDamage(ItemStack stack) {
		double attackDamage = 0;
		if (stack.isEmpty()) {
			return attackDamage;
		}
		Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
		for (Map.Entry<String, AttributeModifier> modifier : modifiers.entries()) {
			if (modifier.getKey().equals(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
				attackDamage = modifier.getValue().getAmount();
			}
		}
		attackDamage += EnchantmentHelper.getModifierForCreature(stack, opponent.getCreatureAttribute());
		return attackDamage;
	}

}
