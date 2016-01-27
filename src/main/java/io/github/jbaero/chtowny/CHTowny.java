package io.github.jbaero.chtowny;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCPlugin;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.abstraction.bukkit.BukkitMCWorld;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CREInvalidPluginException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class CHTowny {

	public static String docs() {
		return "Functions for Towny";
	}
	
	public static Towny getTowny(Target t) {
		Static.checkPlugin("Towny", t);
		MCPlugin twny = Static.getServer().getPluginManager().getPlugin("Towny");
		if (twny.isInstanceOf(Towny.class)) {
			return (Towny) twny.getHandle();
		} else {
			throw new CREInvalidPluginException("Plugin named Towny was not Towny", t);
		}
	}
	
	public static TownyDataSource getTownyData(Target t) {
		Static.checkPlugin("Towny", t);
		return TownyUniverse.getDataSource();
	}
	
	public static Resident getResident(String name, Target t) {
		try {
			return getTownyData(t).getResident(name);
		} catch (NotRegisteredException nre) {
			throw new CREPluginInternalException("Unknown resident " + name, t);
		}
	}
	
	public abstract static class TownyFunction extends AbstractFunction {

		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREInvalidPluginException.class};
		}

		public boolean isRestricted() {
			return true;
		}

		public Boolean runAsync() {
			return false;
		}

		public Version since() {
			return CHVersion.V3_3_1;
		}

		public String getName() {
			String cname = this.getClass().getName();
			return cname.substring(cname.indexOf("$") + 1, cname.length());
		}
	}
	

	public static CArray get_town_info(Town town, Target t) {
		CArray town_array = new CArray(t);
		CArray atown = new CArray(t);
		atown.set("mayor", new CString(town.getMayor().getName(), t), t);
		CArray assis = new CArray(t);
		for (Resident res : town.getAssistants()) {
			assis.push(new CString(res.getName(), t), t);
		}
		atown.set("assistants", assis, t);
		CArray resis = new CArray(t);
		for (Resident res : town.getResidents()) {
			assis.push(new CString(res.getName(), t), t);
		}
		atown.set("residents", resis, t);
		Construct nation;
		try {
			nation = new CString(town.getNation().getName(), t);
		} catch (NotRegisteredException nre) {
			nation = CNull.NULL;
		}
		atown.set("nation", nation, t);
		Construct spawn;
		try {
			spawn = ObjectGenerator.GetGenerator().location(new BukkitMCLocation(town.getSpawn()));
		} catch (TownyException e) {
			spawn = CNull.NULL;
		}
		atown.set("spawn", spawn, t);	
		return town_array;
	}
	
	public static TownBlock block_from_locArray(Construct array, Target t) {
		MCLocation loc = ObjectGenerator.GetGenerator().location(array, new BukkitMCWorld(Bukkit.getWorlds().get(0)), t);
		String world_name = loc.getWorld().getName();
		int X = (int)Math.floor(loc.getX() / 16);
		int Z = (int)Math.floor(loc.getZ() / 16);
		return new TownBlock(X, Z, new TownyWorld(world_name));
	}
	
	public static CArray chunk_array(TownBlock block, Target t) {
		CArray chunkArray = new CArray(t);
		chunkArray.push(new CInt(block.getX(), t), t);
		chunkArray.push(new CInt(block.getZ(), t), t);
		chunkArray.push(new CString(block.getWorld().getName(), t), t);
		return chunkArray;
	}

	@api
	public static class towny_towns extends TownyFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			TownyDataSource data = getTownyData(t);
			CArray ret = new CArray(t);
			for (Town town : data.getTowns()) {
				CArray atown = get_town_info(town, t);
				ret.set(town.getName(), atown, t);
			}
			return ret;
		}

		public Integer[] numArgs() {
			return new Integer[]{0};
		}

		public String docs() {
			return "array {} Returns an associative array with the names of all towns as keys."
					+ " At each key is an array of information about the town.";
		}
	}
		
	@api
	public static class towny_town_at extends TownyFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			TownyDataSource data = getTownyData(t);
			TownBlock block = block_from_locArray(args[0], t);
			List<TownBlock> blocks = data.getAllTownBlocks();
			for(int i = 0; i < blocks.size(); i++) {
				if(blocks.get(i) == block) {
					try {
						return new CString(blocks.get(i).getTown().getName(), t);
					} catch (NotRegisteredException e) {
						return CNull.NULL;
					}
				}
			}

			return CNull.NULL;
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		public String docs() {
			return "array {locArray} Returns the name of the town at the location, "
					+ "or null, if no towns are present.";
		}
		
	}
	
	@api
	public static class towny_town_blocks extends TownyFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			
			String townName = args[0].val();
			TownyDataSource data = getTownyData(t);
			Town town;
			
			try {
				town = data.getTown(townName);
			} catch (NotRegisteredException e1) {
				return new CArray(t);
			}
			
			CArray chunks = new CArray(t);
			
			for(TownBlock block: town.getTownBlocks()) {
				chunks.push(chunk_array(block, t), t);
			}
			
			return chunks;
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		public String docs() {
			return "array {townName} Returns an array of chunk arrays that the town owns."
					+ " The array will be empty if townName does not exist.";
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public static boolean construct(Construct cplayer, Construct cloc, ActionType action, Target t) {
		Location loc = (Location) ObjectGenerator.GetGenerator().location(cloc, null, t).getHandle();
		Player player = (Player) Static.GetPlayer(cplayer, t).getHandle();
		if (TownyUniverse.getTownBlock(loc) == null)
			return true;
		return PlayerCacheUtil.getCachePermission(player, loc, loc.getBlock().getTypeId(), action);
	}
	
	@api
	public static class towny_canbuild extends TownyFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			return CBoolean.get(construct(args[0], args[1], ActionType.BUILD, t));
		}

		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		public String docs() {
			return "boolean {player, location} Checks if a player has build permissions at a location";
		}
	}
	
	@api
	public static class towny_canbreak extends TownyFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			return CBoolean.get(construct(args[0], args[1], ActionType.DESTROY, t));
		}

		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		public String docs() {
			return "boolean {player, location} Checks if a player has destroy permissions at a location";
		}
	}
}
