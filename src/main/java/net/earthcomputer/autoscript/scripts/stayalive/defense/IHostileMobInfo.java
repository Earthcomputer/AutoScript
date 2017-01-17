package net.earthcomputer.autoscript.scripts.stayalive.defense;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.stayalive.StayAliveHelper;
import net.minecraft.entity.Entity;

public interface IHostileMobInfo<T extends Entity> {

	float getThreat(T entity);
	
	Job getDefenseJob(T entity, StayAliveHelper stayAliveHelper);

}
