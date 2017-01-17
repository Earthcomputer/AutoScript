package net.earthcomputer.autoscript.fakeplayer;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityCreature;

public class EntityPlayerDelegate extends EntityCreature {

	private EntityPlayerSP player;

	public EntityPlayerDelegate(EntityPlayerSP player) {
		super(player.world);
		this.player = player;
		copyLocationAndAnglesFrom(player);
	}

	@Override
	public void onUpdate() {
		copyLocationAndAnglesFrom(player);
		world.isRemote = false;
		super.onUpdate();
		world.isRemote = true;
		player.moveForward = moveForward;
		player.moveStrafing = moveStrafing;
		player.setJumping(isJumping);
		player.setSneaking(isSneaking());
		player.setSprinting(isSprinting());
		player.setLocationAndAngles(player.posX, player.posY, player.posZ, rotationYaw, rotationPitch);
	}

}
