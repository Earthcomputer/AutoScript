package net.earthcomputer.autoscript.scripts.stayalive.defense;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.stayalive.StayAliveHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;

public class MobInfoAlwaysHostile<T extends EntityMob> implements IHostileMobInfo<T> {

	@Override
	public float getThreat(T entity) {
		return (float) entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
	}

	@Override
	public Job getDefenseJob(T entity, StayAliveHelper stayAliveHelper) {
		return new BasicDefenseJob(stayAliveHelper, entity);
	}

}
