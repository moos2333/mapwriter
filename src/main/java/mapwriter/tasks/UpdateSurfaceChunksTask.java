package mapwriter.tasks;

import java.util.HashMap;
import java.util.Map;

import mapwriter.Mw;
import mapwriter.map.MapTexture;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
import net.minecraft.util.math.ChunkPos;

public class UpdateSurfaceChunksTask extends Task
{
	private static Map<Long, UpdateSurfaceChunksTask> chunksUpdating = new HashMap<Long, UpdateSurfaceChunksTask>();
	private MwChunk chunk;
	private RegionManager regionManager;
	private MapTexture mapTexture;
	private volatile boolean Running = false;

	public UpdateSurfaceChunksTask(Mw mw, MwChunk chunk)
	{
		this.mapTexture = mw.mapTexture;
		this.regionManager = mw.regionManager;
		this.chunk = chunk;
	}

	@Override
	public boolean CheckForDuplicate()
	{
		Long coords = ChunkPos.asLong(this.chunk.x, this.chunk.z);

		UpdateSurfaceChunksTask task2 = UpdateSurfaceChunksTask.chunksUpdating.get(coords);
		if (task2 == null)
		{
			UpdateSurfaceChunksTask.chunksUpdating.put(coords, this);
			return false;
		}
		if (!task2.Running)
		{
			task2.UpdateChunkData(this.chunk);
		}
		else
		{
			UpdateSurfaceChunksTask.chunksUpdating.put(coords, this);
			return false;
		}
		return true;
	}

	@Override
	public void onComplete()
	{
		Long coords = this.chunk.getCoordIntPair();
		UpdateSurfaceChunksTask.chunksUpdating.remove(coords);
		this.Running = false;
	}

	@Override
	public void run()
	{
		this.Running = true;
		if (this.chunk != null)
		{
			// update the chunk in the region pixels
			this.regionManager.updateChunk(this.chunk);
			// copy updated region pixels to maptexture
			this.mapTexture.updateArea(this.regionManager, this.chunk.x << 4, this.chunk.z << 4, MwChunk.SIZE, MwChunk.SIZE, this.chunk.dimension);
		}
	}

	public void UpdateChunkData(MwChunk chunk)
	{
		this.chunk = chunk;
	}
}