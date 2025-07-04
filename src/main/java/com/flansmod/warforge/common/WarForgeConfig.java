package com.flansmod.warforge.common;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.*;

import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import static com.flansmod.warforge.common.CommonProxy.YIELD_QUALITY_MULTIPLIER;

public class WarForgeConfig
{
	// Config
	public static Configuration configFile;

	// Claims
	public static boolean ENABLE_CITADEL_UPGRADES = false;
	public static final String CATEGORY_CLAIMS = "Claims";
	public static int[] CLAIM_DIM_WHITELIST = new int[]{0};
	public static int CLAIM_STRENGTH_CITADEL = 15;
	public static int CLAIM_STRENGTH_REINFORCED = 10;
	public static int CLAIM_STRENGTH_BASIC = 5;
	public static int SUPPORT_STRENGTH_CITADEL = 3;
	public static int SUPPORT_STRENGTH_REINFORCED = 2;
	public static int SUPPORT_STRENGTH_BASIC = 1;

	public static int ATTACK_STRENGTH_SIEGE_CAMP = 1;
	public static float LEECH_PROPORTION_SIEGE_CAMP = 0.25f;

	// Yields
	public static final String CATEGORY_YIELDS = "Yields";
	public static float YIELD_DAY_LENGTH = 1.0f; // In real-world hours
	// Yield - veins:
	// maps each dimension id to a map of vein keys to veins for quick access

	// Sieges
	public static final String CATEGORY_SIEGES = "Sieges";
	public static int SIEGE_SWING_PER_DEFENDER_DEATH = 1;
	public static int SIEGE_SWING_PER_ATTACKER_DEATH = 1;
	public static int SIEGE_SWING_PER_DAY_ELAPSED_BASE = 1;
	public static int SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS = 1;
	public static int SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS = 1;
	public static float SIEGE_DAY_LENGTH = 24.0f; // In real-world hours
	public static float SIEGE_INFO_RADIUS = 200f;
	public static int SIEGE_SWING_PER_DEFENDER_FLAG = 1;
	public static int SIEGE_SWING_PER_ATTACKER_FLAG = 1;
	public static int SIEGE_DIFFICULTY_PER_DEFENDER_FLAG = 3;
	public static boolean SIEGE_CAPTURE = false;
	public static float SIEGE_COOLDOWN_FAIL = 30f; // actually essentially in ms
	public static float FLAG_COOLDOWN = 1f; // In minutes

	public static int MAX_SIEGES = 3;
	public static int ATTACKER_DESERTION_TIMER = 180; // in seconds
	public static int DEFENDER_DESERTION_TIMER = 300; // in seconds (5 mins by default)
	public static int ATTACKER_CONQUERED_CHUNK_PERIOD = 3600000; // in ms (one hour by default)
	public static int DEFENDER_CONQUERED_CHUNK_PERIOD = 7200000; // in ms (2h by default)
	public static int COMBAT_LOG_THRESHOLD = 10000; // in ms (10s by default)
	public static int LIVE_QUIT_TIMER = 900000; // in ms (15min by default)
	public static int QUITTER_FAIL_TIMER = 300000; // in ms (5 min by default)
	public static int MAX_OFFLINE_PLAYER_COUNT_MINIMUM = 0; // # players which can be on before playerCount dropping to 0 subsequently is marked as a live quit; Exclusively used when negative
	public static float MAX_OFFLINE_PLAYER_PERCENT = 0.5f; // % member count which must be online at some point during a siege before live quit penalties apply
	public static int VERTICAL_SIEGE_DIST = 40; // inclusive distance in blocks siege can be placed/started from/on a potential target claim
	public static int SIEGE_ATTACKER_RADIUS = 1; // number of chunks player can be away from siege chunk in both directions
	public static int SIEGE_DEFENDER_RADIUS = 15;

	// Notoriety
	public static final String CATEGORY_NOTORIETY = "Notoriety";
	public static int NOTORIETY_PER_PLAYER_KILL = 1;
	public static int NOTORIETY_KILL_CAP_PER_PLAYER = 3;
	//public static int NOTORIETY_PER_DRAGON_KILL = 9;
	public static int NOTORIETY_PER_SIEGE_ATTACK_SUCCESS = 7;
	public static int NOTORIETY_PER_SIEGE_DEFEND_SUCCESS = 10;

	// Legacy
	public static final String CATEGORY_LEGACY = "Legacy";
	public static int LEGACY_PER_DAY = 3;
	public static boolean LEGACY_USES_YIELD_TIMER = true;

	// Wealth - Vault blocks
	public static String[] VAULT_BLOCK_IDS = new String[] { "minecraft:gold_block" };
	public static ArrayList<Block> VAULT_BLOCKS = new ArrayList<Block>();

	public static final String CATEGORY_CLIENT = "Client";
	public static float SHOW_NEW_AREA_TIMER = 200.0f;
	public static int RANDOM_BORDER_REDRAW_DENOMINATOR = 5;
	public static int FACTION_NAME_LENGTH_MAX = 32;
	public static boolean BLOCK_ENDER_CHEST = false;
	public static boolean SHOW_YIELD_TIMERS = true;
	public static int CITADEL_MOVE_NUM_DAYS = 7;

	public static boolean DO_FANCY_RENDERING = true;
	public static boolean SHOW_OPPONENT_BORDERS = true;
	public static boolean SHOW_ALLY_BORDERS = true;
    public static long VEIN_MEMBER_DISPLAY_TIME_MS = 1000;

	// Warps
	public static final String CATEGORY_WARPS = "Warps";
	public static boolean ENABLE_F_HOME_COMMAND = true;
	public static boolean ALLOW_F_HOME_BETWEEN_DIMENSIONS = false;
	public static boolean ENABLE_F_HOME_POTION_EFFECT = false; // TODO
	public static int NUM_TICKS_FOR_WARP_COMMANDS = 20 * 20;
	public static boolean ENABLE_SPAWN_COMMAND = true;
	public static boolean ENABLE_SPAWN_POTION_EFFECT = false; // TODO
	public static boolean ALLOW_SPAWN_BETWEEN_DIMENSIONS = false;
	public static boolean ENABLE_TPA_POTIONS = true;


    public static long FACTIONS_BOT_CHANNEL_ID = 799595436154683422L;
    // Permissions
    public static ProtectionConfig UNCLAIMED = new ProtectionConfig();
    public static ProtectionConfig SAFE_ZONE = new ProtectionConfig();
    public static ProtectionConfig WAR_ZONE = new ProtectionConfig();
    public static ProtectionConfig CITADEL_FRIEND = new ProtectionConfig();
    public static ProtectionConfig CITADEL_FOE = new ProtectionConfig();
    public static ProtectionConfig CLAIM_FRIEND = new ProtectionConfig();
    public static ProtectionConfig CLAIM_FOE = new ProtectionConfig();
    public static ProtectionConfig SIEGECAMP_SIEGER = new ProtectionConfig();
    public static ProtectionConfig SIEGECAMP_OTHER = new ProtectionConfig();

    // Init default perms
    static {
        SAFE_ZONE.BREAK_BLOCKS = false;
        SAFE_ZONE.PLACE_BLOCKS = false;
        SAFE_ZONE.INTERACT = false;
        SAFE_ZONE.USE_ITEM = false;
        SAFE_ZONE.PLAYER_TAKE_DAMAGE_FROM_MOB = false;
        SAFE_ZONE.PLAYER_TAKE_DAMAGE_FROM_PLAYER = false;
        SAFE_ZONE.PLAYER_TAKE_DAMAGE_FROM_OTHER = false;
        SAFE_ZONE.PLAYER_DEAL_DAMAGE = false;
        SAFE_ZONE.BLOCK_REMOVAL = false;
        SAFE_ZONE.EXPLOSION_DAMAGE = false;
        SAFE_ZONE.BLOCK_BREAK_EXCEPTION_IDS = new String[]{};
        SAFE_ZONE.BLOCK_PLACE_EXCEPTION_IDS = new String[]{};
        SAFE_ZONE.BLOCK_INTERACT_EXCEPTION_IDS = new String[]{"minecraft:ender_chest", "minecraft:lever", "minecraft:button", "warforge:leaderboard"};
        SAFE_ZONE.ALLOW_MOB_SPAWNS = false;
        SAFE_ZONE.ALLOW_MOB_ENTRY = false;
        SAFE_ZONE.ALLOW_MOUNT_ENTITY = false;
        SAFE_ZONE.ALLOW_DISMOUNT_ENTITY = false;

        WAR_ZONE.BREAK_BLOCKS = false;
        WAR_ZONE.PLACE_BLOCKS = false;
        WAR_ZONE.INTERACT = true;
        WAR_ZONE.USE_ITEM = true;
        WAR_ZONE.PLAYER_TAKE_DAMAGE_FROM_MOB = true;
        WAR_ZONE.PLAYER_TAKE_DAMAGE_FROM_PLAYER = true;
        WAR_ZONE.PLAYER_TAKE_DAMAGE_FROM_OTHER = true;
        WAR_ZONE.PLAYER_DEAL_DAMAGE = true;
        WAR_ZONE.BLOCK_REMOVAL = false;
        WAR_ZONE.EXPLOSION_DAMAGE = false;
        WAR_ZONE.BLOCK_BREAK_EXCEPTION_IDS = new String[]{"minecraft:web", "minecraft:tnt", "minecraft:end_crystal"};
        WAR_ZONE.BLOCK_PLACE_EXCEPTION_IDS = new String[]{"minecraft:web", "minecraft:tnt", "minecraft:end_crystal"};
        WAR_ZONE.BLOCK_INTERACT_EXCEPTION_IDS = new String[]{"minecraft:ender_chest", "minecraft:lever", "minecraft:button", "warforge:leaderboard"};

        CITADEL_FOE.BREAK_BLOCKS = false;
        CITADEL_FOE.PLACE_BLOCKS = false;
        CITADEL_FOE.INTERACT = false;
        CITADEL_FOE.USE_ITEM = false;
        CLAIM_FOE.BREAK_BLOCKS = false;
        CLAIM_FOE.PLACE_BLOCKS = false;
        CLAIM_FOE.INTERACT = false;
        CLAIM_FOE.USE_ITEM = false;
        CITADEL_FOE.ALLOW_MOUNT_ENTITY = false;
        CITADEL_FOE.ALLOW_DISMOUNT_ENTITY = false;
        CLAIM_FOE.ALLOW_MOUNT_ENTITY = false;
        CLAIM_FOE.ALLOW_DISMOUNT_ENTITY = false;

        UNCLAIMED.EXPLOSION_DAMAGE = true;
        WarForgeMod.VEIN_MAP.defaultReturnValue(null);
    }

	public static void initializeVeins() {
		final String VEIN_ENTRY_EXPLANATION = "All veins should be entered in the follow format: \n" +
				"<translation_key>, {<valid dim id's>}, {<vein dim weights 0.0000 - 1.0000>}, {<yield_amount>~<component item id>, ...}, {<component weights 0.0000 - 1.0000>}; \n" +
				"Example: warforge.veins.pure_iron, {-1, 0, 1}, {0.5, 0.4215, 1}, {2~minecraft:iron_ore, 1~minecraft:coal_ore}, {1, 0.2}. \n" +
				"The above is a vein which yields two iron ore and one coal in all dimensions with 3 respective dimension weights and which always gives iron, but only gives coal 20% of the time. \n" +
				"If component weights entry is left as {} [empty], then all are assumed to have a value of 1. All other fields are mandatory and must have equal counts.";

		String[] vein_entries = configFile.getStringList("Vein list", CATEGORY_YIELDS, new String[0], VEIN_ENTRY_EXPLANATION);  // if the default value is null then the returned result is null which causes a npe
		VeinKey.populateVeinMap(WarForgeMod.VEIN_MAP, vein_entries);
	}

	public static void syncConfig(File suggestedFile) {
		configFile = new Configuration(suggestedFile);

        // Protections
        UNCLAIMED.SyncConfig("Unclaimed", "Unclaimed Chunks");
        SAFE_ZONE.SyncConfig("SafeZone", "Safe Zone");
        WAR_ZONE.SyncConfig("WarZone", "War Zone");
        CITADEL_FRIEND.SyncConfig("CitadelFriend", "Citadels of their Faction");
        CITADEL_FOE.SyncConfig("CitadelFoe", "Citadels of other Factions");
        CLAIM_FRIEND.SyncConfig("ClaimFriend", "Claims of their Faction");
        CLAIM_FOE.SyncConfig("ClaimFoe", "Claims of other Factions");
        SIEGECAMP_SIEGER.SyncConfig("Sieger", "Sieges they started");
        SIEGECAMP_OTHER.SyncConfig("SiegeOther", "Other sieges, defending or neutral");

        // Claim Settings
        CLAIM_DIM_WHITELIST = configFile.get(CATEGORY_CLAIMS, "Claim Dimension Whitelist", CLAIM_DIM_WHITELIST, "In which dimensions should player be able to claim chunks").getIntList();
        CLAIM_STRENGTH_CITADEL = configFile.getInt("Citadel Claim Strength", CATEGORY_CLAIMS, CLAIM_STRENGTH_CITADEL, 1, 1024, "The strength of citadel claims");
        CLAIM_STRENGTH_REINFORCED = configFile.getInt("Reinforced Claim Strength", CATEGORY_CLAIMS, CLAIM_STRENGTH_REINFORCED, 1, 1024, "The strength of reinforced claims");
        CLAIM_STRENGTH_BASIC = configFile.getInt("Basic Claim Strength", CATEGORY_CLAIMS, CLAIM_STRENGTH_BASIC, 1, 1024, "The strength of basic claims");
        SUPPORT_STRENGTH_CITADEL = configFile.getInt("Citadel Support Strength", CATEGORY_CLAIMS, SUPPORT_STRENGTH_CITADEL, 1, 1024, "The support strength a citadel gives to adjacent claims");
        SUPPORT_STRENGTH_REINFORCED = configFile.getInt("Reinforced Support Strength", CATEGORY_CLAIMS, SUPPORT_STRENGTH_REINFORCED, 1, 1024, "The support strength a reinforced claim gives to adjacent claims");
        SUPPORT_STRENGTH_BASIC = configFile.getInt("Basic Support Strength", CATEGORY_CLAIMS, SUPPORT_STRENGTH_BASIC, 1, 1024, "The support strength a basic claim gives to adjacent claims");
        CITADEL_MOVE_NUM_DAYS = configFile.getInt("Days Between Citadel Moves", CATEGORY_CLAIMS, CITADEL_MOVE_NUM_DAYS, 0, 1024, "How many days a faction has to wait to move their citadel again");
        ENABLE_CITADEL_UPGRADES = configFile.getBoolean("Enable Citadel Upgrade System", CATEGORY_CLAIMS, false, "Applies claim limits that require upgrading to extend your faction's claim limit");

        // Siege Camp Settings
        ATTACK_STRENGTH_SIEGE_CAMP = configFile.getInt("Siege Camp Attack Strength", CATEGORY_SIEGES, ATTACK_STRENGTH_SIEGE_CAMP, 1, 1024, "How much attack pressure a siege camp exerts on adjacent enemy claims");
        LEECH_PROPORTION_SIEGE_CAMP = configFile.getFloat("Siege Camp Leech Proportion", CATEGORY_SIEGES, LEECH_PROPORTION_SIEGE_CAMP, 0f, 1f, "What proportion of a claim's yields are leeched when a siege camp is set to leech mode");
        MAX_SIEGES = configFile.getInt("Siege Camp Max Count Per Faction", CATEGORY_SIEGES, MAX_SIEGES, 1, 1000, "How many sieges each faction is allowed to have, with any additional siege camps being unable to be placed by members");
        ATTACKER_DESERTION_TIMER = configFile.getInt("Attacker Desertion Timer [s]", CATEGORY_SIEGES, ATTACKER_DESERTION_TIMER, 0, Integer.MAX_VALUE, "The number of seconds a siege can idle with no attackers in it before any action occurs. Setting to 0 results in checks being run every tick.");
        DEFENDER_DESERTION_TIMER = configFile.getInt("Defender Desertion Timer [s]", CATEGORY_SIEGES, DEFENDER_DESERTION_TIMER, 0, Integer.MAX_VALUE, "The number of seconds a siege can be undefended before any action occurs. Setting to 0 results in checks being run every tick.");
        ATTACKER_CONQUERED_CHUNK_PERIOD = configFile.getInt("Attacker Conquered Chunk Grace Period [ms]", CATEGORY_SIEGES, ATTACKER_CONQUERED_CHUNK_PERIOD, 0, Integer.MAX_VALUE, "The number of milliseconds to permit placement within a chunk only by the faction who last won a siege on it. Setting to 0 results in no grace period.");
        DEFENDER_CONQUERED_CHUNK_PERIOD = configFile.getInt("Defender Conquered Chunk Grace Period [ms]", CATEGORY_SIEGES, DEFENDER_CONQUERED_CHUNK_PERIOD, 0, Integer.MAX_VALUE, "The number of milliseconds to deny sieging or claiming in previously sieged chunk in which the siege was won by the defenders. Setting to 0 results in no grace period.");
        COMBAT_LOG_THRESHOLD = configFile.getInt("Time to Combat Log Action [ms]", CATEGORY_SIEGES, COMBAT_LOG_THRESHOLD, 0, Integer.MAX_VALUE, "The number of milliseconds before enforcement action is taken when a player leaves during a siege on any of their claims.");
        LIVE_QUIT_TIMER = configFile.getInt("Time to Live Quit Siege [ms]", CATEGORY_SIEGES, LIVE_QUIT_TIMER, 0, Integer.MAX_VALUE, "The number of milliseconds before a defending team which went offline after a siege against them has begun is considered to have quit, forfeiting the siege.");
        QUITTER_FAIL_TIMER = configFile.getInt("Time to Quitters Failing Offline Siege [ms]", CATEGORY_SIEGES, QUITTER_FAIL_TIMER, 0, Integer.MAX_VALUE, "The number of milliseconds before a defending team which failed a siege defense due to quitting is failed for subsequent sieges, if still offline.");
        MAX_OFFLINE_PLAYER_COUNT_MINIMUM = configFile.getInt("Max Players Before Online Status", CATEGORY_SIEGES, MAX_OFFLINE_PLAYER_COUNT_MINIMUM, Integer.MIN_VALUE, Integer.MAX_VALUE, "A static minimum for the maximum number of players which can have been online at some point during a siege before the faction online player count dropping to 0 indicates a live quit. Negative values override the percent");
        MAX_OFFLINE_PLAYER_PERCENT = configFile.getFloat("Max Player % Before Online Status", CATEGORY_SIEGES, MAX_OFFLINE_PLAYER_PERCENT, 0, 1.0F, "The maximum percent of players in a faction which can be online at some point during a siege before the online count dropping to 0 indicates a live quit.");
        VERTICAL_SIEGE_DIST = configFile.getInt("Maximum Vertical Siege Radius [Inclusive]", CATEGORY_SIEGES, VERTICAL_SIEGE_DIST, 0, Integer.MAX_VALUE, "The number of blocks up or down a siege block can be placed from a potential target, inclusively. Sieges may also only be started on targets within this vertical radius.");
        SIEGE_ATTACKER_RADIUS = configFile.getInt("Attacker Square Chunk Radius From Siege", CATEGORY_SIEGES, SIEGE_ATTACKER_RADIUS, 0, Integer.MAX_VALUE, "The number of chunks in any direction from the siege block that an attacker can be in to prevent siege abandon.");
        SIEGE_DEFENDER_RADIUS = configFile.getInt("Defender Square Chunk Radius From Siege", CATEGORY_SIEGES, SIEGE_DEFENDER_RADIUS, 0, Integer.MAX_VALUE, "The number of chunks in any direction from the siege block that a defender can be in to prevent siege abandon.");

		// Siege swing parameters
		SIEGE_SWING_PER_DEFENDER_DEATH = configFile.getInt("Siege Swing Per Defender Death", CATEGORY_SIEGES, SIEGE_SWING_PER_DEFENDER_DEATH, 0, 1024, "How much a siege progress swings when a defender dies in the siege");
		SIEGE_SWING_PER_ATTACKER_DEATH = configFile.getInt("Siege Swing Per Attacker Death", CATEGORY_SIEGES, SIEGE_SWING_PER_ATTACKER_DEATH, 0, 1024, "How much a siege progress swings when an attacker dies in the siege");
		SIEGE_SWING_PER_DAY_ELAPSED_BASE = configFile.getInt("Siege Swing Per Day", CATEGORY_SIEGES, SIEGE_SWING_PER_DAY_ELAPSED_BASE, 0, 1024, "How much a siege progress swings each day (see below). This happens regardless of logins");
		SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS = configFile.getInt("Siege Swing Per Day Without Attacker Logins", CATEGORY_SIEGES, SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS, 0, 1024, "How much a siege progress swings when no attackers have logged on for a day (see below)");
		SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS = configFile.getInt("Siege Swing Per Day Without Defender Logins", CATEGORY_SIEGES, SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS, 0, 1024, "How much a siege progress swings when no defenders have logged on for a day (see below)");
		SIEGE_DAY_LENGTH = configFile.getFloat("Siege Day Length", CATEGORY_SIEGES, SIEGE_DAY_LENGTH, 0.0001f, 100000f, "The length of a day for siege login purposes, in real-world hours.");
		SIEGE_INFO_RADIUS = configFile.getFloat("Siege Info Radius", CATEGORY_SIEGES, SIEGE_INFO_RADIUS, 1f, 1000f, "The range at which you see siege information. (Capped by the server setting)");
		SIEGE_SWING_PER_DEFENDER_DEATH = configFile.getInt("Siege Swing Per Defender Death", CATEGORY_SIEGES, SIEGE_SWING_PER_DEFENDER_DEATH, 0, 1024, "How much a siege progress swings when a defender dies in the siege");
		SIEGE_SWING_PER_DEFENDER_FLAG = configFile.getInt("Siege Swing Per Defender Flag", CATEGORY_SIEGES, SIEGE_SWING_PER_DEFENDER_FLAG, 0, 1024, "How much the siege swings per defender flag per day");
		SIEGE_COOLDOWN_FAIL = configFile.getFloat("Cooldown between sieges after failure", CATEGORY_SIEGES, SIEGE_COOLDOWN_FAIL, 0, 100000f, "Cooldown between sieges, in minutes");
		FLAG_COOLDOWN = configFile.getFloat("Cooldown between Flag move", CATEGORY_SIEGES, FLAG_COOLDOWN, 0, 100000f, "Cooldown between flag moves, in minutes");
		SIEGE_SWING_PER_ATTACKER_FLAG = configFile.getInt("Siege Swing Per Attacker Flag", CATEGORY_SIEGES, SIEGE_SWING_PER_ATTACKER_FLAG, 0, 1024, "How much the siege swings per attacker flag per day");
		SIEGE_DIFFICULTY_PER_DEFENDER_FLAG = configFile.getInt("Siege Difficulty Reinforcement Per Defender Flag", CATEGORY_SIEGES, SIEGE_DIFFICULTY_PER_DEFENDER_FLAG, 0, 1024, "How much having a defender flag at a base reinforces the difficulty of the siege for the attackers");
		SIEGE_CAPTURE = configFile.getBoolean("Siege Captures", CATEGORY_SIEGES, SIEGE_CAPTURE, "Does a successful siege convert the claim");

		// Vault parameters
		VAULT_BLOCK_IDS = configFile.getStringList("Valuable Blocks", Configuration.CATEGORY_GENERAL, VAULT_BLOCK_IDS, "The block IDs that count towards the value of your citadel's vault");

		// Yield parameters
		YIELD_DAY_LENGTH = configFile.getFloat("Yield Day Length", CATEGORY_YIELDS, YIELD_DAY_LENGTH, 0.0001f, 100000f, "The length of time between yields, in real-world hours.");

		YIELD_QUALITY_MULTIPLIER = configFile.getFloat("Yield Quality Multiplier", CATEGORY_YIELDS, YIELD_QUALITY_MULTIPLIER, 1, 10000, "A number to be used to vary yield amounts for every vein's components, dividing by this for poor quality veins and multiplying for high quality. Set to 1 to effectively have no difference.");
		//initializeVeins();

		// Notoriety
		NOTORIETY_PER_PLAYER_KILL = configFile.getInt("Notoriety gain per PVP kill", CATEGORY_NOTORIETY, NOTORIETY_PER_PLAYER_KILL, 0, 1024, "How much notoriety a player earns for their faction when killing another player");
		NOTORIETY_PER_SIEGE_ATTACK_SUCCESS = configFile.getInt("Notoriety gain per siege attack win", CATEGORY_NOTORIETY, NOTORIETY_PER_SIEGE_ATTACK_SUCCESS, 0, 1024, "How much notoriety a faction earns when successfully winning an siege as attacker");
		NOTORIETY_PER_SIEGE_DEFEND_SUCCESS = configFile.getInt("Notoriety gain per siege defend win", CATEGORY_NOTORIETY, NOTORIETY_PER_SIEGE_DEFEND_SUCCESS, 0, 1024, "How much notoriety a faction earns when successfully defending a siege");
		NOTORIETY_KILL_CAP_PER_PLAYER = configFile.getInt("Max # kills per player", CATEGORY_NOTORIETY, NOTORIETY_KILL_CAP_PER_PLAYER, 0, 1024, "How many times a faction can kill the same player and still get points");
		// Legacy
		LEGACY_PER_DAY = configFile.getInt("Legacy gain per day", CATEGORY_LEGACY, LEGACY_PER_DAY, 0, 1024, "How much legacy a faction gets for having at least one player on");
		LEGACY_USES_YIELD_TIMER = configFile.getBoolean("Legacy uses yield timer", CATEGORY_LEGACY, LEGACY_USES_YIELD_TIMER, "If true, legacy triggers every yield timer. Otherwise, every siege timer");

		// Visual
		SHOW_NEW_AREA_TIMER = configFile.getFloat("New Area Timer", CATEGORY_CLIENT, SHOW_NEW_AREA_TIMER, 0.0f, 1000f, "How many in-game ticks to show the 'You have entered {faction}' message for.");
		FACTION_NAME_LENGTH_MAX = configFile.getInt("Max Faction Name Length", Configuration.CATEGORY_GENERAL, FACTION_NAME_LENGTH_MAX, 3, 128, "How many characters long can a faction name be.");
		SHOW_OPPONENT_BORDERS = configFile.getBoolean("Show Opponent Chunk Borders", Configuration.CATEGORY_GENERAL, SHOW_OPPONENT_BORDERS, "Turns the in-world border rendering on/off for opponent chunks");
		SHOW_ALLY_BORDERS = configFile.getBoolean("Show Ally Chunk Borders", Configuration.CATEGORY_GENERAL, SHOW_ALLY_BORDERS, "Turns the in-world border rendering on/off for ally chunks");
		SHOW_YIELD_TIMERS = configFile.getBoolean("Show yield timers", CATEGORY_CLIENT, SHOW_YIELD_TIMERS, "Whether to show a readout of the time until the next yield / siege in top left of your screen");
        VEIN_MEMBER_DISPLAY_TIME_MS = configFile.getInt("Vein Member Display Time", CATEGORY_CLIENT, (int) VEIN_MEMBER_DISPLAY_TIME_MS, 100, Integer.MAX_VALUE, "The time in milliseconds for which each member of a vein will be displayed when it is being cycled through, to the precision allowed by the client tick system.");

		// Other permissions
		BLOCK_ENDER_CHEST = configFile.getBoolean("Disable Ender Chest", Configuration.CATEGORY_GENERAL, BLOCK_ENDER_CHEST, "Prevent players from opening ender chests");
		ENABLE_TPA_POTIONS = configFile.getBoolean("Enable TPA Potions", Configuration.CATEGORY_GENERAL, ENABLE_TPA_POTIONS, "Allow players to craft and consume /tpa and /tpaccept style potions");

		//Warps
		ENABLE_F_HOME_COMMAND = configFile.getBoolean("Enable /f home Command", CATEGORY_WARPS, ENABLE_F_HOME_COMMAND, "Allow players to use /f home to teleport to their citadel");
		ENABLE_F_HOME_POTION_EFFECT = configFile.getBoolean("Enable /f home Potion", CATEGORY_WARPS, ENABLE_F_HOME_POTION_EFFECT, "Allow players to craft a potion that takes them to their citadel");
		ALLOW_F_HOME_BETWEEN_DIMENSIONS = configFile.getBoolean("Allow /f home across dimensions", CATEGORY_WARPS, ALLOW_F_HOME_BETWEEN_DIMENSIONS, "Allow players to use /f home when in a different dimension to their citadel");
		ENABLE_SPAWN_COMMAND = configFile.getBoolean("Enable /spawn Command", CATEGORY_WARPS, ENABLE_SPAWN_COMMAND, "Allow players to use /spawn to teleport to the world spawn");
		ENABLE_SPAWN_POTION_EFFECT = configFile.getBoolean("Enable /spawn Potion", CATEGORY_WARPS, ENABLE_SPAWN_POTION_EFFECT, "Allow players to craft a potion that takes them to the world spawn");
		ALLOW_SPAWN_BETWEEN_DIMENSIONS = configFile.getBoolean("Allow /spawn across dimensions", CATEGORY_WARPS, ALLOW_SPAWN_BETWEEN_DIMENSIONS, "Allow players to use /spawn when in a different dimension to the world spawn");
		NUM_TICKS_FOR_WARP_COMMANDS = configFile.getInt("Num Ticks for Warps", CATEGORY_WARPS, NUM_TICKS_FOR_WARP_COMMANDS, 0, 20 * 60 * 5, "How many ticks must the player stand still for a warp command to take effect");

        // Graphics controls
        DO_FANCY_RENDERING = configFile.getBoolean("Enable WarForge Fancy Rendering", CATEGORY_CLIENT, DO_FANCY_RENDERING, "Controls whether or not fancy graphics will be enabled for this mod's rendering.");
        RANDOM_BORDER_REDRAW_DENOMINATOR = configFile.getInt("Random Border Redraw Denominator", CATEGORY_CLIENT, RANDOM_BORDER_REDRAW_DENOMINATOR, 1, Integer.MAX_VALUE, "Sets the bound on a random number generated, which when equal to 0 calls the border redraw. Effectively 1/this chance to redraw every frame");

        String botChannelString = configFile.getString("Discord Bot Channel ID", Configuration.CATEGORY_GENERAL, "" + FACTIONS_BOT_CHANNEL_ID, "https://github.com/Chikachi/DiscordIntegration/wiki/IMC-Feature");
        FACTIONS_BOT_CHANNEL_ID = Long.parseLong(botChannelString);

        if (configFile.hasChanged())
            configFile.save();
    }

    public static class ProtectionConfig {
        public boolean BREAK_BLOCKS = true;
        public boolean PLACE_BLOCKS = true;
        public boolean INTERACT = true;
        public boolean USE_ITEM = true;
        public boolean BLOCK_REMOVAL = true;
        public boolean EXPLOSION_DAMAGE = false;
        public boolean PLAYER_TAKE_DAMAGE_FROM_MOB = true;
        public boolean PLAYER_TAKE_DAMAGE_FROM_PLAYER = true;
        public boolean PLAYER_TAKE_DAMAGE_FROM_OTHER = true;
        public boolean PLAYER_DEAL_DAMAGE = true;
        public boolean ALLOW_MOB_SPAWNS = true;
        public boolean ALLOW_MOB_ENTRY = true;
        public boolean ALLOW_MOUNT_ENTITY = true;
        public boolean ALLOW_DISMOUNT_ENTITY = true;
        public List<Block> BLOCK_PLACE_EXCEPTIONS;
        public List<Block> BLOCK_BREAK_EXCEPTIONS;
        public List<Block> BLOCK_INTERACT_EXCEPTIONS;
        public List<Item> ITEM_USE_EXCEPTIONS;
        private String[] BLOCK_PLACE_EXCEPTION_IDS = new String[]{"minecraft:torch"};
        private String[] BLOCK_BREAK_EXCEPTION_IDS = new String[]{"minecraft:torch", "warforge:siegecampblock"};
        private String[] BLOCK_INTERACT_EXCEPTION_IDS = new String[]{"minecraft:ender_chest", "warforge:citadelblock", "warforge:basicclaimblock", "warforge:reinforcedclaimblock", "warforge:siegecampblock"};
        private String[] ITEM_USE_EXCEPTION_IDS = new String[]{"minecraft:snowball"};

        private List<Block> findBlocks(String[] input) {
            List<Block> output = new ArrayList<Block>(input.length);
            for (String blockID : input) {
                Block block = Block.getBlockFromName(blockID);
                if (block != null)
                    output.add(block);
            }
            return output;
        }

        private List<Item> FindItems(String[] input) {
            List<Item> output = new ArrayList<Item>(input.length);
            for (String itemID : input) {
                Item item = Item.getByNameOrId(itemID);
                if (item != null)
                    output.add(item);
            }
            return output;
        }

        public void findBlocks() {
            BLOCK_PLACE_EXCEPTIONS = findBlocks(BLOCK_PLACE_EXCEPTION_IDS);
            BLOCK_BREAK_EXCEPTIONS = findBlocks(BLOCK_BREAK_EXCEPTION_IDS);
            BLOCK_INTERACT_EXCEPTIONS = findBlocks(BLOCK_INTERACT_EXCEPTION_IDS);
            ITEM_USE_EXCEPTIONS = FindItems(ITEM_USE_EXCEPTION_IDS);
        }

        public void SyncConfig(String name, String desc) {

            BREAK_BLOCKS = configFile.getBoolean(name + " - Break Blocks", name, BREAK_BLOCKS, "Can players break blocks in " + desc);
            PLACE_BLOCKS = configFile.getBoolean(name + " - Place Blocks", name, PLACE_BLOCKS, "Can players place blocks in " + desc);
            BLOCK_REMOVAL = configFile.getBoolean(name + " - Block Removal", name, BLOCK_REMOVAL, "Can blocks be removed at all in (including from explosions, mobs etc) " + desc);
            EXPLOSION_DAMAGE = configFile.getBoolean(name + " - Explosion Damage", name, EXPLOSION_DAMAGE, "Can explosions damage blocks in " + desc);
            INTERACT = configFile.getBoolean(name + " - Interact", name, INTERACT, "Can players interact with blocks and entities in " + desc);
            USE_ITEM = configFile.getBoolean(name + " - Use Items", name, USE_ITEM, "Can players use items in " + desc);
            PLAYER_TAKE_DAMAGE_FROM_MOB = configFile.getBoolean(name + " - Take Dmg From Mob", name, PLAYER_TAKE_DAMAGE_FROM_MOB, "Can players take mob damage in " + desc);
            PLAYER_TAKE_DAMAGE_FROM_PLAYER = configFile.getBoolean(name + " - Take Dmg From Player", name, PLAYER_TAKE_DAMAGE_FROM_PLAYER, "Can players take damage from other players in " + desc);
            PLAYER_TAKE_DAMAGE_FROM_OTHER = configFile.getBoolean(name + " - Take Any Other Dmg", name, PLAYER_TAKE_DAMAGE_FROM_OTHER, "Can players take damage from any other source in " + desc);
            PLAYER_DEAL_DAMAGE = configFile.getBoolean(name + " - Deal Damage", name, PLAYER_DEAL_DAMAGE, "Can players deal damage in " + desc);

            BLOCK_PLACE_EXCEPTION_IDS = configFile.getStringList(name + " - Place Exceptions", name, BLOCK_PLACE_EXCEPTION_IDS, "The block IDs that can still be placed. Has no effect if block placement is allowed anyway");
            BLOCK_BREAK_EXCEPTION_IDS = configFile.getStringList(name + " - Break Exceptions", name, BLOCK_BREAK_EXCEPTION_IDS, "The block IDs that can still be broken. Has no effect if block breaking is allowed anyway");
            BLOCK_INTERACT_EXCEPTION_IDS = configFile.getStringList(name + " - Interact Exceptions", name, BLOCK_INTERACT_EXCEPTION_IDS, "The block IDs that can still be interacted with. Has no effect if interacting is allowed anyway");
            ITEM_USE_EXCEPTION_IDS = configFile.getStringList(name + " - Use Exceptions", name, ITEM_USE_EXCEPTION_IDS, "The item IDs that can still be used. Has no effect if interacting is allowed anyway");

            ALLOW_MOB_SPAWNS = configFile.getBoolean(name + " - Allow Mob Spawns", name, ALLOW_MOB_SPAWNS, "Can mobs spawn in " + desc);
            ALLOW_MOB_ENTRY = configFile.getBoolean(name + " - Allow Mob Entry", name, ALLOW_MOB_ENTRY, "Can mobs enter " + desc);

            ALLOW_DISMOUNT_ENTITY = configFile.getBoolean(name + " - Allow Dismount Entity", name, ALLOW_DISMOUNT_ENTITY, "Can players dismount entities " + desc);
            ALLOW_MOUNT_ENTITY = configFile.getBoolean(name + " - Allow Mount Entity", name, ALLOW_MOUNT_ENTITY, "Can players mount entities " + desc);

        }
    }
}
