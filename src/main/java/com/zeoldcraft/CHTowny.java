package com.zeoldcraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCPlugin;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;

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
			throw new ConfigRuntimeException("Plugin named Towny was not Towny", ExceptionType.InvalidPluginException, t);
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
			throw new ConfigRuntimeException("Unknown resident " + name, ExceptionType.PluginInternalException, t);
		}
	}
	
	public abstract static class TownyFunction extends AbstractFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.InvalidPluginException};
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
	
	@api
	public static class towny_towns extends TownyFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			TownyDataSource data = getTownyData(t);
			CArray ret = new CArray(t);
			for (Town town : data.getTowns()) {
				CArray atown = new CArray(t);
				atown.set("mayor", new CString(town.getMayor().getName(), t), t);
				CArray assis = new CArray(t);
				for (Resident res : town.getAssistants()) {
					assis.push(new CString(res.getName(), t));
				}
				atown.set("assistants", assis, t);
				CArray resis = new CArray(t);
				for (Resident res : town.getResidents()) {
					assis.push(new CString(res.getName(), t));
				}
				atown.set("residents", resis, t);
				Construct nation;
				try {
					nation = new CString(town.getNation().getName(), t);
				} catch (NotRegisteredException nre) {
					nation = new CNull(t);
				}
				atown.set("nation", nation, t);
				Construct spawn;
				try {
					spawn = ObjectGenerator.GetGenerator().location(new BukkitMCLocation(town.getSpawn()));
				} catch (TownyException e) {
					spawn = new CNull(t);
				}
				atown.set("spawn", spawn, t);
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
			return new CBoolean(construct(args[0], args[1], ActionType.BUILD, t), t);
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
			return new CBoolean(construct(args[0], args[1], ActionType.DESTROY, t), t);
		}

		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		public String docs() {
			return "boolean {player, location} Checks if a player has destroy permissions at a location";
		}
	}
}
