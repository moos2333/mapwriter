package mapwriter.config;

import java.io.File;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ConfigurationHandler
{
	public static Configuration configuration;

	public static void init(File configFile)
	{
		if (configuration == null)
		{
			configuration = new Configuration(configFile);
			setMapModeDefaults();
			loadConfig();
			Config.fullScreenMap.loadConfig();
			Config.largeMap.loadConfig();
			Config.smallMap.loadConfig();

			if (configuration.hasChanged())
			{
				configuration.save();
			}
		}
	}

	public static void loadConfig()
	{
		Config.teleportEnabled = configuration.getBoolean("teleportEnabled", Reference.catOptions, Config.teleportEnabled, "", "mw.config.teleportEnabled");
		Config.teleportCommand = configuration.getString("teleportCommand", Reference.catOptions, Config.teleportCommand, "", "mw.config.teleportCommand");
		Config.maxChunkSaveDistSq = configuration.getInt("maxChunkSaveDistSq", Reference.catOptions, Config.maxChunkSaveDistSq, 1, 256 * 256, "", "mw.config.maxChunkSaveDistSq");
		Config.maxDeathMarkers = configuration.getInt("maxDeathMarkers", Reference.catOptions, Config.maxDeathMarkers, 0, 1000, "", "mw.config.maxDeathMarkers");
		Config.chunksPerTick = configuration.getInt("chunksPerTick", Reference.catOptions, Config.chunksPerTick, 1, 500, "", "mw.config.chunksPerTick");
		Config.portNumberInWorldNameEnabled = configuration.getBoolean("portNumberInWorldNameEnabled", Reference.catOptions, Config.portNumberInWorldNameEnabled, "", "mw.config.portNumberInWorldNameEnabled");
		Config.undergroundMode = configuration.getBoolean("undergroundMode", Reference.catOptions, Config.undergroundMode, "", "mw.config.undergroundMode");
		Config.regionFileOutputEnabledSP = configuration.getBoolean("regionFileOutputEnabledSP", Reference.catOptions, Config.regionFileOutputEnabledSP, "", "mw.config.regionFileOutputEnabledSP");
		Config.regionFileOutputEnabledMP = configuration.getBoolean("regionFileOutputEnabledMP", Reference.catOptions, Config.regionFileOutputEnabledMP, "", "mw.config.regionFileOutputEnabledMP");
		Config.zoomOutLevels = configuration.getInt("zoomOutLevels", Reference.catOptions, Config.zoomOutLevels, 1, 256, "", "mw.config.zoomOutLevels");
		Config.zoomInLevels = -configuration.getInt("zoomInLevels", Reference.catOptions, -Config.zoomInLevels, 1, 256, "", "mw.config.zoomInLevels");
		Config.configTextureSize = configuration.getInt("textureSize", Reference.catOptions, Config.configTextureSize, 1024, 4096, "", "mw.config.textureSize");
		Config.overlayModeIndex = configuration.getInt("overlayModeIndex", Reference.catOptions, Config.overlayModeIndex, 0, 1000, "", "mw.config.overlayModeIndex");
		Config.overlayZoomLevel = configuration.getInt("overlayZoomLevel", Reference.catOptions, Config.overlayZoomLevel, Config.zoomInLevels, Config.zoomOutLevels, "", "mw.config.overlayZoomLevel");
		Config.showMobOverlay = configuration.getBoolean("showMobOverlay", Reference.catOptions, Config.showMobOverlay, "", "mw.config.showMobOverlay");
	}

	public static void setMapModeDefaults()
	{
		Config.fullScreenMap.setDefaults();
		Config.largeMap.setDefaults();
		Config.smallMap.setDefaults();
	}

	@SubscribeEvent
	public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.getModID().equalsIgnoreCase(Reference.MOD_ID))
		{
			if (event.getConfigID().equals(Reference.catOptions))
			{
				loadConfig();
			}
			else if (event.getConfigID().equals(Reference.catFullMapConfig))
			{
				Config.fullScreenMap.loadConfig();
			}
			else if (event.getConfigID().equals(Reference.catLargeMapConfig))
			{
				Config.largeMap.loadConfig();
			}
			else if (event.getConfigID().equals(Reference.catSmallMapConfig))
			{
				Config.smallMap.loadConfig();
			}
			else
			{
				Logging.logError("Unknown config id: %s", event.getConfigID());
			}

			if (configuration.hasChanged())
			{
				configuration.save();
			}
		}
	}
}