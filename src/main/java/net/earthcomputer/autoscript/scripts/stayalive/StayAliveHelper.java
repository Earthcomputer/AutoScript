package net.earthcomputer.autoscript.scripts.stayalive;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;

import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.scripts.stayalive.defense.HostileMobRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

public class StayAliveHelper {

	private boolean defending;
	private boolean eating;

	public void modifyJobToStayAlive(Job job) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		if (!eating && player.getFoodStats().getFoodLevel() <= 17) {
			eating = true;
			job.startChild(new EatJob(this));
		}
		Entity mobToDefendAgainst = searchMobsToDefendAgainst();
		if (!defending && mobToDefendAgainst != null) {
			defending = true;
			job.startChild(HostileMobRegistry.getDefenseJob(mobToDefendAgainst, this));
		}
	}

	public boolean isDefending() {
		return defending;
	}

	public void setDefending(boolean defending) {
		this.defending = defending;
	}

	public boolean isEating() {
		return eating;
	}

	public void setEating(boolean eating) {
		this.eating = eating;
	}

	private Entity searchMobsToDefendAgainst() {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		List<Entity> entities = Minecraft.getMinecraft().world.getEntitiesInAABBexcluding(player,
				player.getEntityBoundingBox().expand(32, 32, 32), new Predicate<Entity>() {
					@Override
					public boolean apply(Entity entity) {
						if (HostileMobRegistry.getThreat(entity) <= 0) {
							return false;
						}
						if (Minecraft.getMinecraft().player.canEntityBeSeen(entity)) {
							return true;
						} else {
							return false;
						}
					}
				});
		if (entities.isEmpty()) {
			return null;
		}
		Collections.sort(entities, new Comparator<Entity>() {
			@Override
			public int compare(Entity first, Entity second) {
				return (int) Math.signum(HostileMobRegistry.getThreat(second) - HostileMobRegistry.getThreat(first));
			}
		});
		return entities.get(entities.size() - 1);
	}

}
