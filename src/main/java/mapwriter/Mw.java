package mapwriter;

import java.io.File;

import mapwriter.config.Config;
import mapwriter.config.ConfigurationHandler;
import mapwriter.config.WorldConfig;
import mapwriter.forge.MwForge;
import mapwriter.forge.MwKeyHandler;
import mapwriter.gui.MwGui;
import mapwriter.gui.MwGuiMarkerDialog;
import mapwriter.gui.MwGuiMarkerDialogNew;
import mapwriter.map.MapTexture;
import mapwriter.map.MapView;
import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import mapwriter.map.MiniMap;
import mapwriter.map.Trail;
import mapwriter.map.UndergroundTexture;
import mapwriter.overlay.OverlaySlime;
import mapwriter.region.BlockColours;
import mapwriter.region.RegionManager;
import mapwriter.tasks.CloseRegionManagerTask;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import mapwriter.util.Render;
import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;

public class Mw
{
	private static Mw instance;

	public static Mw getInstance()
	{
		if (Mw.instance == null)
		{
			synchronized (WorldConfig.class)
			{
				if (Mw.instance == null)
				{
					Mw.instance = new Mw();
				}
			}
		}
		return Mw.instance;
	}

	public Minecraft mc = null;
	private final File configDir;
	private final File saveDir;
	public File worldDir = null;
	public File imageDir = null;
	public boolean ready = false;
	public int tickCounter = 0;
	public int textureSize = 2048;
	public double playerX = 0.0;
	public double playerZ = 0.0;
	public double playerY = 0.0;
	public int playerXInt = 0;
	public int playerYInt = 0;
	public int playerZInt = 0;
	public String playerBiome = "";
	public double playerHeading = 0.0;
	public int playerDimension = 0;
	public float mapRotationDegrees = 0.0f;
	public MapTexture mapTexture = null;
	public UndergroundTexture undergroundMapTexture = null;
	public BackgroundExecutor executor = null;
	public MiniMap miniMap = null;
	public MarkerManager markerManager = null;
	public BlockColours blockColours = null;
	public RegionManager regionManager = null;
	public ChunkManager chunkManager = null;
	public Trail playerTrail = null;

	private Mw()
	{
		this.mc = Minecraft.getMinecraft();
		this.saveDir = new File(this.mc.gameDir, "saves");
		this.configDir = new File(this.mc.gameDir, "config");
		this.ready = false;
		RegionManager.logger = MwForge.logger;
		ConfigurationHandler.loadConfig();
	}

	public void close()
	{
		if (!this.ready) return;
		this.ready = false;
		if (this.chunkManager != null)
		{
			this.chunkManager.close();
			this.chunkManager = null;
		}
		if (this.executor != null)
		{
			this.executor.addTask(new CloseRegionManagerTask(this.regionManager));
			this.regionManager = null;
			if (this.executor.close())
				Logging.log("error: timeout waiting for tasks");
		}
		if (this.playerTrail != null) this.playerTrail.close();
		if (this.markerManager != null)
		{
			this.markerManager.saveAll();
			this.markerManager.clear();
		}
		if (this.miniMap != null) this.miniMap.close();
		if (this.undergroundMapTexture != null) this.undergroundMapTexture.close();
		if (this.mapTexture != null) this.mapTexture.close();
		WorldConfig.getInstance().saveWorldConfig();
		this.tickCounter = 0;
		OverlaySlime.reset();
	}

	public void load()
	{
		if (this.ready) return;
		if (this.mc.world == null || this.mc.player == null)
		{
			Logging.log("Mw.load: world or player is null, cannot load yet");
			return;
		}
		Logging.log("Mw.load: loading...");

		File saveDir = this.saveDir;
		if (Config.saveDirOverride.length() > 0)
		{
			File d = new File(Config.saveDirOverride);
			if (d.isDirectory()) saveDir = d;
			else Logging.log("error: no such directory %s", Config.saveDirOverride);
		}
		if (!this.mc.isSingleplayer())
		{
			this.worldDir = new File(new File(saveDir, "mapwriter_mp_worlds"), Utils.getWorldName());
		}
		else
		{
			saveDir = DimensionManager.getCurrentSaveRootDirectory();
			this.worldDir = new File(saveDir, "mapwriter");
		}
		this.imageDir = new File(this.worldDir, "images");
		if (!this.imageDir.exists()) this.imageDir.mkdirs();
		if (!this.imageDir.isDirectory())
			Logging.log("Mapwriter: ERROR: could not create images directory '%s'", this.imageDir.getPath());

		this.tickCounter = 0;

		this.reloadBlockColours();

		this.markerManager = new MarkerManager();
		this.markerManager.loadAll();
		this.playerTrail = new Trail(this, Reference.PlayerTrailName);
		this.executor = new BackgroundExecutor();

		this.regionManager = new RegionManager(
				this.worldDir,
				this.imageDir,
				this.blockColours,
				Config.zoomInLevels,
				Config.zoomOutLevels);

		this.miniMap = new MiniMap(this);
		this.miniMap.view.setDimension(this.mc.player.dimension);

		if (this.chunkManager == null)
			this.chunkManager = new ChunkManager(this);

		this.ready = true;
	}

	public void loadBlockColourOverrides(BlockColours bc)
	{
		File f = new File(this.configDir, Reference.blockColourOverridesFileName);
		if (f.isFile())
		{
			Logging.logInfo("loading block colour overrides file %s", f);
			bc.loadFromFile(f);
		}
		else
		{
			Logging.logInfo("recreating block colour overrides file %s", f);
			BlockColours.writeOverridesFile(f);
			if (f.isFile()) bc.loadFromFile(f);
			else Logging.logError("could not load block colour overrides from file %s", f);
		}
	}

	public void onChunkLoad(Chunk chunk)
	{
		this.load();
		if (chunk != null && chunk.getWorld() instanceof net.minecraft.client.multiplayer.WorldClient)
		{
			if (this.ready) this.chunkManager.addChunk(chunk);
			else Logging.logInfo("missed chunk (%d, %d)", chunk.x, chunk.z);
		}
	}

	public void onChunkUnload(Chunk chunk)
	{
		if (this.ready && chunk != null && chunk.getWorld() instanceof net.minecraft.client.multiplayer.WorldClient)
			this.chunkManager.removeChunk(chunk);
	}

	public void onKeyDown(KeyBinding kb)
	{
		if (this.mc.currentScreen != null || !this.ready) return;

		if (kb == MwKeyHandler.keyMapMode)
			this.miniMap.nextOverlayMode(1);
		else if (kb == MwKeyHandler.keyMapGui)
			this.mc.displayGuiScreen(new MwGui(this));
		else if (kb == MwKeyHandler.keyNewMarker)
		{
			String group = this.markerManager.getVisibleGroupName();
			if (group.equals("none")) group = "group";
			if (Config.newMarkerDialog)
				this.mc.displayGuiScreen(new MwGuiMarkerDialogNew(null, this.markerManager, "", group,
						this.playerXInt, this.playerYInt, this.playerZInt, this.playerDimension));
			else
				this.mc.displayGuiScreen(new MwGuiMarkerDialog(null, this.markerManager, "", group,
						this.playerXInt, this.playerYInt, this.playerZInt, this.playerDimension));
		}
		else if (kb == MwKeyHandler.keyNextGroup)
		{
			this.markerManager.nextGroup();
			this.markerManager.update();
			this.mc.player.sendMessage(new TextComponentTranslation("mw.msg.groupselected", this.markerManager.getVisibleGroupName()));
		}
		else if (kb == MwKeyHandler.keyTeleport)
		{
			Marker marker = this.markerManager.getNearestMarkerInDirection(this.playerXInt, this.playerZInt, this.playerHeading);
			if (marker != null) this.teleportToMarker(marker);
		}
		else if (kb == MwKeyHandler.keyZoomIn)
			this.miniMap.view.adjustZoomLevel(-1);
		else if (kb == MwKeyHandler.keyZoomOut)
			this.miniMap.view.adjustZoomLevel(1);
		else if (kb == MwKeyHandler.keyUndergroundMode)
			this.toggleUndergroundMode();
	}

	public void onPlayerDeath()
	{
		if (!this.ready || Config.maxDeathMarkers <= 0) return;
		this.updatePlayer();
		int deleteCount = this.markerManager.countMarkersInGroup("playerDeaths") - Config.maxDeathMarkers + 1;
		for (int i = 0; i < deleteCount; i++)
			this.markerManager.delMarker(null, "playerDeaths");
		this.markerManager.addMarker(Utils.getCurrentDateString(), "playerDeaths",
				this.playerXInt, this.playerYInt, this.playerZInt, this.playerDimension, 0xffff0000);
		this.markerManager.setVisibleGroupName("playerDeaths");
		this.markerManager.update();
	}

	public void onTick()
	{
		this.load();
		if (!this.ready || this.mc.player == null) return;

		this.setTextureSize();
		this.updatePlayer();

		this.miniMap.view.setUndergroundMode(Config.undergroundMode);
		if (Config.undergroundMode && this.tickCounter % 30 == 0)
			this.undergroundMapTexture.update();

		if (!(this.mc.currentScreen instanceof MwGui))
		{
			this.miniMap.view.setViewCentreScaled(this.playerX, this.playerZ, this.playerDimension);
			this.miniMap.drawCurrentMap();
		}

		int maxTasks = 50;
		while (!this.executor.processTaskQueue() && maxTasks-- > 0) {}
		this.chunkManager.onTick();
		this.mapTexture.processTextureUpdates();
		this.playerTrail.onTick();
		this.tickCounter++;
	}

	public void reloadBlockColours()
	{
		if (this.blockColours == null)
			this.blockColours = new BlockColours();

		BlockColourGen.genBlockColours(this.blockColours);

		File defaultOverrideFile = new File(this.configDir, "mapwriter_block_colours.txt");
		if (defaultOverrideFile.isFile())
			this.blockColours.loadFromFile(defaultOverrideFile);
		else
			BlockColours.writeOverridesFile(defaultOverrideFile);

		File savedColoursFile = new File(this.worldDir, "mapwriter_saved_colours.txt");
		if (savedColoursFile.isFile())
			this.blockColours.loadFromFile(savedColoursFile);

		this.mapTexture = new MapTexture(this.textureSize, Config.linearTextureScaling);
		this.undergroundMapTexture = new UndergroundTexture(this, this.textureSize, Config.linearTextureScaling);
		this.chunkManager = new ChunkManager(this);
	}

	public void reloadMapTexture()
	{
		this.executor.addTask(new CloseRegionManagerTask(this.regionManager));
		this.executor.close();
		MapTexture oldMapTexture = this.mapTexture;
		MapTexture newMapTexture = new MapTexture(this.textureSize, Config.linearTextureScaling);
		this.mapTexture = newMapTexture;
		if (oldMapTexture != null) oldMapTexture.close();

		this.executor = new BackgroundExecutor();
		this.regionManager = new RegionManager(
				this.worldDir,
				this.imageDir,
				this.blockColours,
				Config.zoomInLevels,
				Config.zoomOutLevels);

		UndergroundTexture oldTexture = this.undergroundMapTexture;
		UndergroundTexture newTexture = new UndergroundTexture(this, this.textureSize, Config.linearTextureScaling);
		this.undergroundMapTexture = newTexture;
		if (oldTexture != null) this.undergroundMapTexture.close();
	}

	public void saveBlockColours(BlockColours bc)
	{
		File f = new File(this.configDir, Reference.blockColourSaveFileName);
		Logging.logInfo("saving block colours to '%s'", f);
		bc.saveToFile(f);
	}

	public void setTextureSize()
	{
		if (Config.configTextureSize == this.textureSize) return;
		int maxTextureSize = Render.getMaxTextureSize();
		int textureSize = 1024;
		while (textureSize <= maxTextureSize && textureSize <= Config.configTextureSize)
			textureSize *= 2;
		textureSize /= 2;

		Logging.log("GL reported max texture size = %d", maxTextureSize);
		Logging.log("texture size from config = %d", Config.configTextureSize);
		Logging.log("setting map texture size to = %d", textureSize);

		this.textureSize = textureSize;
		if (this.ready) this.reloadMapTexture();
	}

	public void teleportTo(int x, int y, int z)
	{
		if (Config.teleportEnabled)
			this.mc.player.sendChatMessage(String.format("/%s %d %d %d", Config.teleportCommand, x, y, z));
		else
			Utils.printBoth(I18n.format("mw.msg.tpdisabled"));
	}

	public void teleportToMapPos(MapView mapView, int x, int y, int z)
	{
		if (Config.teleportCommand.equals("warp"))
			Utils.printBoth(I18n.format("mw.msg.warp.error"));
		else
		{
			double scale = mapView.getDimensionScaling(this.playerDimension);
			this.teleportTo((int)(x / scale), y, (int)(z / scale));
		}
	}

	public void teleportToMarker(Marker marker)
	{
		if (Config.teleportCommand.equals("warp"))
			this.warpTo(marker.name);
		else if (marker.dimension == this.playerDimension)
			this.teleportTo(marker.x, marker.y, marker.z);
		else
			Utils.printBoth(I18n.format("mw.msg.tp.dimError"));
	}

	public void toggleMarkerMode()
	{
		this.markerManager.nextGroup();
		this.markerManager.update();
		this.mc.player.sendMessage(new TextComponentTranslation("mw.msg.groupselected", this.markerManager.getVisibleGroupName()));
	}

	public void toggleUndergroundMode()
	{
		Config.undergroundMode = !Config.undergroundMode;
	}

	public void updatePlayer()
	{
		this.playerX = this.mc.player.posX;
		this.playerY = this.mc.player.posY;
		this.playerZ = this.mc.player.posZ;
		this.playerXInt = (int) Math.floor(this.playerX);
		this.playerYInt = (int) Math.floor(this.playerY);
		this.playerZInt = (int) Math.floor(this.playerZ);

		if (this.mc.world != null)
		{
			if (!this.mc.world.getChunk(new BlockPos(this.playerX, 0, this.playerZ)).isEmpty())
				this.playerBiome = this.mc.world.getBiomeForCoordsBody(new BlockPos(this.playerX, 0, this.playerZ)).getBiomeName();
		}

		this.playerHeading = Math.toRadians(this.mc.player.rotationYaw) + Math.PI / 2.0D;
		this.mapRotationDegrees = -this.mc.player.rotationYaw + 180;
		this.playerDimension = this.mc.world.provider.getDimensionType().getId();

		if (this.miniMap.view.getDimension() != this.playerDimension)
		{
			WorldConfig.getInstance().addDimension(this.playerDimension);
			this.miniMap.view.setDimension(this.playerDimension);
		}
	}

	public void warpTo(String name)
	{
		if (Config.teleportEnabled)
			this.mc.player.sendChatMessage(String.format("/warp %s", name));
		else
			Utils.printBoth(I18n.format("mw.msg.tpdisabled"));
	}
}