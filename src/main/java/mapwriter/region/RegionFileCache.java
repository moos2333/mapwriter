package mapwriter.region;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import mapwriter.util.Logging;

public class RegionFileCache
{
	private static final int MAX_REGION_FILES_OPEN = 24;

	private class LruCache extends LinkedHashMap<String, RegionFile>
	{
		private static final long serialVersionUID = 1L;

		public LruCache()
		{
			super(MAX_REGION_FILES_OPEN * 2, 0.5f, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, RegionFile> eldest)
		{
			if (size() > MAX_REGION_FILES_OPEN)
			{
				try
				{
					eldest.getValue().close();
				}
				catch (Exception e)
				{
					Logging.logError("Failed to close region file %s: %s", eldest.getKey(), e.getMessage());
				}
				return true;
			}
			return false;
		}
	}

	private final LruCache regionFileCache = new LruCache();
	private final File worldDir;

	public RegionFileCache(File worldDir)
	{
		this.worldDir = worldDir;
	}

	public void close()
	{
		for (RegionFile rf : regionFileCache.values())
		{
			try
			{
				rf.close();
			}
			catch (Exception e)
			{
				Logging.logError("Failed to close region file during cache shutdown: %s", e.getMessage());
			}
		}
		regionFileCache.clear();
	}

	public RegionFile getRegionFile(int x, int z, int dimension)
	{
		File path = getRegionFilePath(x, z, dimension);
		String key = path.toString();
		RegionFile rf = regionFileCache.get(key);
		if (rf == null)
		{
			rf = new RegionFile(path);
			regionFileCache.put(key, rf);
		}
		return rf;
	}

	public File getRegionFilePath(int x, int z, int dimension)
	{
		File dir = this.worldDir;
		if (dimension != 0)
			dir = new File(dir, "DIM" + dimension);
		dir = new File(dir, "region");
		String filename = String.format("r.%d.%d.mca", x >> Region.SHIFT, z >> Region.SHIFT);
		return new File(dir, filename);
	}

	public boolean regionFileExists(int x, int z, int dimension)
	{
		return getRegionFilePath(x, z, dimension).isFile();
	}
}