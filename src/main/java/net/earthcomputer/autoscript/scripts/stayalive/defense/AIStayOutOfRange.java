package net.earthcomputer.autoscript.scripts.stayalive.defense;

import com.google.common.base.Predicates;

import net.earthcomputer.autoscript.fake.EntityPlayerProxy;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.monster.EntityMob;

public class AIStayOutOfRange extends EntityAIAvoidEntity<EntityMob> {

	protected EntityMob opponent;
	protected double minRange;
	protected double maxRange;

	@SuppressWarnings("unchecked")
	public AIStayOutOfRange(EntityPlayerProxy player, EntityMob opponent, double minRange, double maxRange) {
		super(player, (Class<EntityMob>) opponent.getClass(), Predicates.equalTo(opponent), (float) minRange, 1, 1);
		this.opponent = opponent;
		this.minRange = minRange;
		this.maxRange = maxRange;
	}

	@Override
	public boolean continueExecuting() {
		return super.continueExecuting() && theEntity.getDistanceSqToEntity(opponent) <= maxRange * maxRange;
	}

}
