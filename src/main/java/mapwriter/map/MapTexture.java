package mapwriter.map;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.opengl.GL11;
import mapwriter.BackgroundExecutor;
import mapwriter.region.Region;
import mapwriter.region.RegionManager;
import mapwriter.tasks.MapUpdateViewTask;
import mapwriter.util.Texture;

public class MapTexture extends Texture
{
	private static class Rect
	{
		final int x, y, w, h;
		Rect(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
	}

	public int textureRegions;
	public int textureSize;
	private MapViewRequest loadedView = null;
	private MapViewRequest requestedView = null;
	private Region[] regionArray;
	private final ConcurrentLinkedQueue<Rect> textureUpdateQueue = new ConcurrentLinkedQueue<>();
	private int lastUpdateX = Integer.MIN_VALUE;
	private int lastUpdateZ = Integer.MIN_VALUE;

	public MapTexture(int textureSize, boolean linearScaling)
	{
		super(textureSize, textureSize, 0x00000000, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
		this.setLinearScaling(linearScaling);
		this.textureRegions = textureSize >> Region.SHIFT;
		this.textureSize = textureSize;
		this.regionArray = new Region[this.textureRegions * this.textureRegions];
	}

	public void addTextureUpdate(int x, int z, int w, int h)
	{
		textureUpdateQueue.add(new Rect(x, z, w, h));
	}

	public int getRegionIndex(int x, int z, int zoomLevel)
	{
		int mask = this.textureRegions - 1;
		int rx = (x >> (Region.SHIFT + zoomLevel)) & mask;
		int rz = (z >> (Region.SHIFT + zoomLevel)) & mask;
		return rz * this.textureRegions + rx;
	}

	public boolean isLoaded(MapViewRequest req)
	{
		return loadedView != null && loadedView.mostlyEquals(req);
	}

	public boolean loadRegion(RegionManager regionManager, int x, int z, int zoomLevel, int dimension)
	{
		int index = this.getRegionIndex(x, z, zoomLevel);
		Region current = this.regionArray[index];
		if (current == null || !current.equals(x, z, zoomLevel, dimension))
		{
			Region newRegion = regionManager.getRegion(x, z, zoomLevel, dimension);
			this.regionArray[index] = newRegion;
			this.updateTextureFromRegion(newRegion, newRegion.x, newRegion.z, newRegion.size, newRegion.size);
			return true;
		}
		return false;
	}

	public int loadRegions(RegionManager regionManager, MapViewRequest req)
	{
		int size = Region.SIZE << req.zoomLevel;
		int loadedCount = 0;
		for (int z = req.zMin; z <= req.zMax; z += size)
		{
			for (int x = req.xMin; x <= req.xMax; x += size)
			{
				if (this.loadRegion(regionManager, x, z, req.zoomLevel, req.dimension))
					loadedCount++;
			}
		}
		return loadedCount;
	}

	public void processTextureUpdates()
	{
		if (textureUpdateQueue.isEmpty())
			return;
		List<Rect> batch = new ArrayList<>();
		Rect r;
		while ((r = textureUpdateQueue.poll()) != null)
			batch.add(r);
		for (Rect rect : batch)
			this.updateTextureArea(rect.x, rect.y, rect.w, rect.h);
	}

	public void requestView(MapViewRequest req, BackgroundExecutor executor, RegionManager regionManager)
	{
		if (requestedView == null || !requestedView.equals(req))
		{
			requestedView = req;
			executor.addTask(new MapUpdateViewTask(this, regionManager, req));
		}
	}

	public void setLoaded(MapViewRequest req)
	{
		this.loadedView = req;
	}

	public synchronized void setRGBOpaque(int x, int y, int w, int h, int[] pixels, int offset, int scanSize)
	{
		int bufOffset = y * this.w + x;
		for (int i = 0; i < h; i++)
		{
			this.setPixelBufPosition(bufOffset + i * this.w);
			int rowOffset = offset + i * scanSize;
			for (int j = 0; j < w; j++)
			{
				int colour = pixels[rowOffset + j];
				if (colour != 0)
					colour |= 0xff000000;
				this.pixelBufPut(colour);
			}
		}
	}

	public void updateArea(RegionManager regionManager, int x, int z, int w, int h, int dimension)
	{
		for (Region region : this.regionArray)
		{
			if (region != null && region.isAreaWithin(x, z, w, h, dimension))
			{
				this.updateTextureFromRegion(region, x, z, w, h);
			}
		}
	}

	public void updateTextureFromRegion(Region region, int x, int z, int w, int h)
	{
		int tx = (x >> region.zoomLevel) & (this.w - 1);
		int ty = (z >> region.zoomLevel) & (this.h - 1);
		int tw = w >> region.zoomLevel;
		int th = h >> region.zoomLevel;
		tw = Math.min(tw, this.w - tx);
		th = Math.min(th, this.h - ty);

		int[] pixels = region.getPixels();
		if (pixels != null)
		{
			this.setRGBOpaque(tx, ty, tw, th, pixels, region.getPixelOffset(x, z), Region.SIZE);
		}
		else
		{
			this.fillRect(tx, ty, tw, th, 0x00000000);
		}
		this.addTextureUpdate(tx, ty, tw, th);
	}
}