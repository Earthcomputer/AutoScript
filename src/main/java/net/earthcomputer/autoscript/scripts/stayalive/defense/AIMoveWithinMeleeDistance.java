package net.earthcomputer.autoscript.scripts.stayalive.defense;

import net.earthcomputer.autoscript.fakeplayer.EntityPlayerDelegate;
import net.minecraft.entity.ai.EntityAIAttackMelee;

public class AIMoveWithinMeleeDistance extends EntityAIAttackMelee {

	private double closestDistance;

	public AIMoveWithinMeleeDistance(EntityPlayerDelegate player, double closestDistance) {
		super(player, 1, true);
		this.closestDistance = closestDistance;
	}

	@Override
	public boolean continueExecuting() {
		return super.continueExecuting()
				&& attacker.getDistanceSqToEntity(attacker.getAttackTarget()) > closestDistance * closestDistance;
	}

}
