package net.earthcomputer.autoscript.scripts.stayalive.defense;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.stayalive.StayAliveHelper;
import net.minecraft.entity.Entity;

public final class MobInfoAlwaysPassive implements IHostileMobInfo<Entity> {

	@Override
	public float getThreat(Entity entity) {
		return 0;
	}

	@Override
	public Job getDefenseJob(Entity entity, StayAliveHelper stayAliveHelper) {
		return null;
	}

}
