package net.earthcomputer.autoscript.scripts;

import java.util.Comparator;

import com.google.common.base.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class StayAliveHelper {

	private EnumAction currentAction;
	private int hungerAtStartEat;

	public void onUpdate() {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		if (currentAction == EnumAction.EAT) {
			if (player.getFoodStats().getFoodLevel() <= hungerAtStartEat) {
				if (!mc.inGameHasFocus) {
					mc.playerController.processRightClick(player, mc.world, EnumHand.MAIN_HAND);
				}
				ScriptMovementInput.holdRightClick();
			} else {
				if (!mc.inGameHasFocus) {
					mc.playerController.onStoppedUsingItem(player);
				}
				currentAction = null;
			}
		}
		if (currentAction == null) {
			if (player.getFoodStats().getFoodLevel() <= 17) {
				if (ScriptMovementInput.selectItemInInventory(player, new Predicate<ItemStack>() {
					@Override
					public boolean apply(ItemStack stack) {
						return stack.getItem() instanceof ItemFood
								&& ((ItemFood) stack.getItem()).getHealAmount(stack) > 0;
					}
				}, new Comparator<ItemStack>() {
					@Override
					public int compare(ItemStack first, ItemStack second) {
						return ((ItemFood) second.getItem()).getHealAmount(second)
								- ((ItemFood) first.getItem()).getHealAmount(first);
					}
				})) {
					currentAction = EnumAction.EAT;
					hungerAtStartEat = player.getFoodStats().getFoodLevel();
					return;
				}
			}
		}
	}

	public static enum EnumAction {
		EAT
	}

}
