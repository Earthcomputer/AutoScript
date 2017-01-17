package net.earthcomputer.autoscript.scripts.stayalive.defense;

import java.util.Map;

import com.google.common.collect.Maps;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.stayalive.StayAliveHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;

public class HostileMobRegistry {

	private HostileMobRegistry() {
	}

	private static final Map<Class<? extends Entity>, IHostileMobInfo<?>> customInfo = Maps.newIdentityHashMap();

	public static <T extends Entity> void registerCustomInfo(Class<T> mobClass, IHostileMobInfo<T> customInfo) {
		if (HostileMobRegistry.customInfo.containsKey(mobClass)) {
			throw new IllegalArgumentException("Cannot register duplicate mobClass " + mobClass.getName());
		}
		HostileMobRegistry.customInfo.put(mobClass, customInfo);
	}

	@SuppressWarnings("unchecked")
	private static IHostileMobInfo<Entity> getMobInfo(Entity mob) {
		Class<?> mobClass = mob.getClass();
		while (!customInfo.containsKey(mobClass) && mobClass != Entity.class) {
			mobClass = mobClass.getSuperclass();
		}
		return (IHostileMobInfo<Entity>) customInfo.get(mobClass);
	}

	public static float getThreat(Entity entity) {
		return getMobInfo(entity).getThreat(entity);
	}

	public static Job getDefenseJob(Entity entity, StayAliveHelper stayAliveHelper) {
		return getMobInfo(entity).getDefenseJob(entity, stayAliveHelper);
	}

	static {
		registerCustomInfo(Entity.class, new MobInfoAlwaysPassive());
		registerCustomInfo(EntityMob.class, new MobInfoAlwaysHostile<EntityMob>());
	}

}
