package net.earthcomputer.autoscript.crafting;

import net.earthcomputer.autoscript.job.Job;
import net.minecraft.util.math.BlockPos;

public interface IHelperBlockProvider {

	/**
	 * Places the crafting table if there is no crafting table
	 * 
	 * @param jobToEnhance
	 *            - the job to enhance to place the crafting table
	 * @return False if the job was enhanced, true to continue to
	 *         {@link #getCraftingTablePos()}
	 */
	boolean tryPlaceCraftingTable(Job jobToEnhance);

	/**
	 * Gets the position of a crafting table
	 * 
	 * @return The position of the crafting table, or <tt>null</tt> if none
	 *         exists
	 */
	BlockPos getCraftingTablePos();

	/**
	 * Places the furnace if there is no furnace
	 * 
	 * @param jobToEnhance
	 *            - the job to enhance to place the furnace
	 * @return False if the job was enhanced, true to continue to
	 *         {@link #getFurnacePos()}
	 */
	boolean tryPlaceFurnace(Job jobToEnhance);

	/**
	 * Gets the position of the furnace
	 * 
	 * @return The position of the furnace, or <tt>null</tt> if none exists
	 */
	BlockPos getFurnacePos();

}
