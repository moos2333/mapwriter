package mapwriter.map;

import java.util.ArrayList;
import java.util.List;

import mapwriter.Mw;
import mapwriter.config.Config;
import mapwriter.map.mapmode.MapMode;
import mapwriter.overlay.OverlayMobs;

public class MiniMap
{
	public MapMode smallMapMode;
	public MapMode largeMapMode;
	public MapMode guiMapMode;

	public MapView view;

	public MapRenderer smallMap;
	public MapRenderer largeMap;

	private List<MapRenderer> mapList;
	private MapRenderer currentMap = null;

	private final OverlayMobs mobOverlay;

	public MiniMap(Mw mw)
	{
		this.view = new MapView(mw, false);
		this.view.setZoomLevel(Config.overlayZoomLevel);

		this.smallMapMode = new MapMode(Config.smallMap);
		this.smallMap = new MapRenderer(mw, this.smallMapMode, this.view);

		this.largeMapMode = new MapMode(Config.largeMap);
		this.largeMap = new MapRenderer(mw, this.largeMapMode, this.view);

		this.mapList = new ArrayList<MapRenderer>();
		if (this.smallMapMode.getConfig().enabled)
		{
			this.mapList.add(this.smallMap);
		}
		if (this.largeMapMode.getConfig().enabled)
		{
			this.mapList.add(this.largeMap);
		}
		this.mapList.add(null);

		this.nextOverlayMode(0);
		this.currentMap = this.mapList.get(Config.overlayModeIndex);

		this.mobOverlay = new OverlayMobs();
		this.mobOverlay.setEnabled(Config.showMobOverlay);
	}

	public void close()
	{
		this.mapList.clear();
		this.currentMap = null;
	}

	public void drawCurrentMap()
	{
		if (this.currentMap != null)
		{
			this.currentMap.draw();
		}
		if (Config.showMobOverlay && this.currentMap != null)
		{
			this.mobOverlay.draw(this.currentMap.getMapMode(), this.view);
		}
	}

	public void onTick()
	{
		this.mobOverlay.onTick();
	}

	public MapRenderer nextOverlayMode(int increment)
	{
		int size = this.mapList.size();
		Config.overlayModeIndex = (Config.overlayModeIndex + size + increment) % size;
		this.currentMap = this.mapList.get(Config.overlayModeIndex);
		return this.currentMap;
	}
}