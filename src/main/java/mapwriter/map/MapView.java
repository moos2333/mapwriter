package mapwriter.map;

import java.util.List;
import mapwriter.Mw;
import mapwriter.api.IMapMode;
import mapwriter.api.IMapView;
import mapwriter.api.IMwDataProvider;
import mapwriter.api.MwAPI;
import mapwriter.config.Config;

public class MapView implements IMapView
{
	private int zoomLevel = 0;
	private int dimension = 0;
	private int textureSize = 2048;
	private double x = 0, z = 0;
	private int mapW = 0, mapH = 0;
	private int baseW = 1, baseH = 1;
	private double w = 1, h = 1;
	private int minZoom, maxZoom;
	private boolean undergroundMode;
	private boolean fullscreenMap;

	public MapView(Mw mw, boolean FullscreenMap)
	{
		this.minZoom = Config.zoomInLevels;
		this.maxZoom = Config.zoomOutLevels;
		this.undergroundMode = Config.undergroundMode;
		this.fullscreenMap = FullscreenMap;
		if (this.fullscreenMap)
			this.setZoomLevel(Config.fullScreenZoomLevel);
		this.setZoomLevel(Config.overlayZoomLevel);
		this.setViewCentre(mw.playerX, mw.playerZ);
	}

	@Override
	public int adjustZoomLevel(int n)
	{
		return this.setZoomLevel(this.zoomLevel + n);
	}

	@Override
	public int getDimension()
	{
		return this.dimension;
	}

	@Override
	public double getDimensionScaling(int playerDimension)
	{
		if (this.dimension != -1 && playerDimension == -1) return 8.0;
		if (this.dimension == -1 && playerDimension != -1) return 0.125;
		return 1.0;
	}

	@Override
	public double getHeight() { return this.h; }

	@Override
	public double getMaxX() { return this.x + this.w / 2; }

	@Override
	public double getMaxZ() { return this.z + this.h / 2; }

	@Override
	public double getMinX() { return this.x - this.w / 2; }

	@Override
	public double getMinZ() { return this.z - this.h / 2; }

	@Override
	public int getPixelsPerBlock() { return this.mapW / this.baseW; }

	@Override
	public int getRegionZoomLevel() { return Math.max(0, this.zoomLevel); }

	@Override
	public boolean getUndergroundMode() { return this.undergroundMode; }

	@Override
	public double getWidth() { return this.w; }

	@Override
	public double getX() { return this.x; }

	@Override
	public double getZ() { return this.z; }

	@Override
	public int getZoomLevel() { return this.zoomLevel; }

	@Override
	public boolean isBlockWithinView(double bX, double bZ, boolean circular)
	{
		if (!circular)
		{
			return bX >= this.getMinX() && bX <= this.getMaxX() && bZ >= this.getMinZ() && bZ <= this.getMaxZ();
		}
		else
		{
			double dx = bX - this.x;
			double dz = bZ - this.z;
			double r = this.getHeight() / 2;
			return dx * dx + dz * dz < r * r;
		}
	}

	@Override
	public void nextDimension(List<Integer> dimensionList, int n)
	{
		int idx = dimensionList.indexOf(this.dimension);
		if (idx < 0) idx = 0;
		int size = dimensionList.size();
		int newDim = dimensionList.get((idx + size + n) % size);
		this.setDimensionAndAdjustZoom(newDim);
	}

	@Override
	public void panView(double relX, double relZ)
	{
		this.setViewCentre(this.x + relX * this.w, this.z + relZ * this.h);
	}

	@Override
	public void setDimension(int dimension)
	{
		double scale = 1.0;
		if (dimension != this.dimension)
		{
			if (this.dimension != -1 && dimension == -1) scale = 0.125;
			else if (this.dimension == -1 && dimension != -1) scale = 8.0;
			this.dimension = dimension;
			this.setViewCentre(this.x * scale, this.z * scale);
		}
		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
		if (provider != null)
			provider.onDimensionChanged(this.dimension, this);
	}

	@Override
	public void setDimensionAndAdjustZoom(int dimension)
	{
		int delta = 0;
		if (this.dimension != -1 && dimension == -1) delta = -3;
		else if (this.dimension == -1 && dimension != -1) delta = 3;
		this.setZoomLevel(this.getZoomLevel() + delta);
		this.setDimension(dimension);
	}

	@Override
	public void setMapWH(IMapMode mapMode)
	{
		this.setMapWH(mapMode.getWPixels(), mapMode.getHPixels());
	}

	@Override
	public void setMapWH(int w, int h)
	{
		if (this.mapW != w || this.mapH != h)
		{
			this.mapW = w;
			this.mapH = h;
			this.updateBaseWH();
		}
	}

	@Override
	public void setTextureSize(int n)
	{
		if (this.textureSize != n)
		{
			this.textureSize = n;
			this.updateBaseWH();
		}
	}

	@Override
	public void setUndergroundMode(boolean enabled)
	{
		if (enabled && this.zoomLevel >= 0)
			this.setZoomLevel(-1);
		this.undergroundMode = enabled;
	}

	@Override
	public void setViewCentre(double vX, double vZ)
	{
		this.x = vX;
		this.z = vZ;
		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
		if (provider != null)
			provider.onMapCenterChanged(vX, vZ, this);
	}

	@Override
	public void setViewCentreScaled(double vX, double vZ, int playerDimension)
	{
		double scale = this.getDimensionScaling(playerDimension);
		this.setViewCentre(vX * scale, vZ * scale);
	}

	@Override
	public int setZoomLevel(int zoomLevel)
	{
		int prev = this.zoomLevel;
		if (this.undergroundMode)
			this.zoomLevel = Math.min(Math.max(this.minZoom, zoomLevel), 0);
		else
			this.zoomLevel = Math.min(Math.max(this.minZoom, zoomLevel), this.maxZoom);
		if (prev != this.zoomLevel)
			this.updateZoom();
		if (this.fullscreenMap)
			Config.fullScreenZoomLevel = this.zoomLevel;
		Config.overlayZoomLevel = this.zoomLevel;
		return this.zoomLevel;
	}

	@Override
	public void zoomToPoint(int newZoomLevel, double bX, double bZ)
	{
		int prev = this.zoomLevel;
		newZoomLevel = this.setZoomLevel(newZoomLevel);
		double factor = Math.pow(2, newZoomLevel - prev);
		this.setViewCentre(bX - (bX - this.x) * factor, bZ - (bZ - this.z) * factor);
	}

	private void updateBaseWH()
	{
		int w = this.mapW;
		int h = this.mapH;
		int half = this.textureSize / 2;
		while (w > half || h > half)
		{
			w >>= 1;
			h >>= 1;
		}
		this.baseW = w;
		this.baseH = h;
		this.updateZoom();
	}

	private void updateZoom()
	{
		if (this.zoomLevel >= 0)
		{
			this.w = this.baseW << this.zoomLevel;
			this.h = this.baseH << this.zoomLevel;
		}
		else
		{
			this.w = this.baseW >> (-this.zoomLevel);
			this.h = this.baseH >> (-this.zoomLevel);
		}
		IMwDataProvider provider = MwAPI.getCurrentDataProvider();
		if (provider != null)
			provider.onZoomChanged(this.getZoomLevel(), this);
	}
}