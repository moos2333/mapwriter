package mapwriter.config;

import mapwriter.util.Reference;

public class Config
{
	public static final String[] backgroundModeStringArray = {
			"mw.config.backgroundTextureMode.none",
			"mw.config.backgroundTextureMode.static",
			"mw.config.backgroundTextureMode.panning"
	};

	public static boolean linearTextureScaling = true;
	public static boolean useSavedBlockColours = false;
	public static boolean mapPixelSnapEnabled = true;
	public static String saveDirOverride = "";
	public static String backgroundTextureMode = backgroundModeStringArray[0];
	public static boolean moreRealisticMap = false;
	public static boolean newMarkerDialog = true;
	public static boolean drawMarkersInWorld = false;
	public static boolean drawMarkersNameInWorld = false;
	public static boolean drawMarkersDistanceInWorld = false;
	public static boolean reloadColours = Boolean.parseBoolean(System.getProperty("fml.skipFirstTextureLoad", "true"));

	public static boolean undergroundMode = false;
	public static boolean teleportEnabled = true;
	public static String teleportCommand = "tp";
	public static int defaultTeleportHeight = 80;
	public static int zoomOutLevels = 5;
	public static int zoomInLevels = -5;
	public static int maxChunkSaveDistSq = 128 * 128;
	public static int configTextureSize = 2048;
	public static int maxDeathMarkers = 3;
	public static int chunksPerTick = 5;
	public static boolean portNumberInWorldNameEnabled = true;
	public static boolean regionFileOutputEnabledSP = true;
	public static boolean regionFileOutputEnabledMP = true;

	public static int overlayModeIndex = 0;
	public static int overlayZoomLevel = 0;
	public static int fullScreenZoomLevel = 0;

	public static largeMapModeConfig largeMap = new largeMapModeConfig(Reference.catLargeMapConfig);
	public static smallMapModeConfig smallMap = new smallMapModeConfig(Reference.catSmallMapConfig);
	public static MapModeConfig fullScreenMap = new MapModeConfig(Reference.catFullMapConfig);
}