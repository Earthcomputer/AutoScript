package net.earthcomputer.autoscript.scripts.stayalive;

import java.util.Comparator;

import com.google.common.base.Predicate;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class EatJob extends Job {

	private StayAliveHelper stayAliveHelper;
	private int lastHunger;
	private int ticksWaited;

	public EatJob(StayAliveHelper stayAliveHelper) {
		this.stayAliveHelper = stayAliveHelper;
	}

	@Override
	protected void updateExecute() {
		Minecraft mc = Minecraft.getMinecraft();
		int foodLevel = mc.player.getFoodStats().getFoodLevel();
		if (foodLevel > 17) {
			stop();
			return;
		}
		if (foodLevel != lastHunger) {
			lastHunger = foodLevel;
			ticksWaited = 0;
		} else if (ticksWaited++ > 100) {
			fail();
			return;
		}
		if (!mc.inGameHasFocus) {
			mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
		}
		ScriptInput.holdRightClick();
	}

	@Override
	protected void pauseExecute() {
		Minecraft mc = Minecraft.getMinecraft();
		if (!mc.inGameHasFocus) {
			mc.playerController.onStoppedUsingItem(mc.player);
		}
	}

	@Override
	protected void resumeExecute() {
		if (!ScriptInput.selectItemInInventory(Minecraft.getMinecraft().player, new Predicate<ItemStack>() {
			@Override
			public boolean apply(ItemStack stack) {
				return stack.getItem() instanceof ItemFood && ((ItemFood) stack.getItem()).getHealAmount(stack) > 0;
			}
		}, new Comparator<ItemStack>() {
			@Override
			public int compare(ItemStack first, ItemStack second) {
				return ((ItemFood) second.getItem()).getHealAmount(second)
						- ((ItemFood) first.getItem()).getHealAmount(first);
			}
		})) {
			fail();
			return;
		}
		lastHunger = Minecraft.getMinecraft().player.getFoodStats().getFoodLevel();
		ticksWaited = 0;
	}

	@Override
	protected void stopExecute() {
		pauseExecute();
		stayAliveHelper.setEating(false);
	}

}
