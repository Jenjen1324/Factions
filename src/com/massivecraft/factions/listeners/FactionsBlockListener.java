package com.massivecraft.factions.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.FPerm;


public class FactionsBlockListener extends BlockListener
{
	public P p;
	public FactionsBlockListener(P p)
	{
		this.p = p;
	}
	
	public static boolean playerCanBuildDestroyBlock(Player player, Block block, String action, boolean justCheck)
	{
		FPlayer me = FPlayers.i.get(player);

		if (me.isAdminBypassing()) return true;

		Location location = block.getLocation();
		FLocation loc = new FLocation(location);
		Faction factionHere = Board.getFactionAt(loc);

		if (FPerm.PAINBUILD.has(me, location))
		{
			if (!justCheck)
			{
				me.msg("<b>It is painful to %s in the territory of %s<b>.", action, factionHere.describeTo(me));
				player.damage(Conf.actionDeniedPainAmount);
			}
			return true;
		}
		
		return FPerm.BUILD.has(me, location, true);
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event)
	{
		if (event.isCancelled()) return;
		if ( ! event.canBuild()) return;
		
		// TODO: Test if this old stuff is still an issue.
		// special case for flint&steel, which should only be prevented by DenyUsage list
		/*if (event.getBlockPlaced().getType() == Material.FIRE)
		{
			return;
		}*/

		if ( ! playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock(), "build", false))
		{
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.isCancelled()) return;

		if ( ! playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock(), "destroy", false))
		{
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event)
	{
		if (event.isCancelled()) return;
		if ( ! event.getInstaBreak()) return;

		if (! playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock(), "destroy", false))
		{
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockPistonExtend(BlockPistonExtendEvent event)
	{
		if (event.isCancelled()) return;
		if ( ! Conf.pistonProtectionThroughDenyBuild) return;

		Faction pistonFaction = Board.getFactionAt(new FLocation(event.getBlock()));

		// target end-of-the-line empty (air) block which is being pushed into, including if piston itself would extend into air
		Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getLength() + 1);

		// if potentially pushing into air in another territory, we need to check it out
		
		 
		if (targetBlock.isEmpty() && ! FPerm.BUILD.has(pistonFaction, targetBlock.getLocation()))
		{
			event.setCancelled(true);
		}

		/*
		 * note that I originally was testing the territory of each affected block, but since I found that pistons can only push
		 * up to 12 blocks and the width of any territory is 16 blocks, it should be safe (and much more lightweight) to test
		 * only the final target block as done above
		 */
	}

	@Override
	public void onBlockPistonRetract(BlockPistonRetractEvent event)
	{
		// if not a sticky piston, retraction should be fine
		if (event.isCancelled() || !event.isSticky() || !Conf.pistonProtectionThroughDenyBuild) return;

		Location targetLoc = event.getRetractLocation();

		// if potentially retracted block is just air, no worries
		if (targetLoc.getBlock().isEmpty()) return;

		Faction pistonFaction = Board.getFactionAt(new FLocation(event.getBlock()));
		
		if ( ! FPerm.BUILD.has(pistonFaction, targetLoc))
		{
			event.setCancelled(true);
		}
	}
}
