package mapwriter.tasks;

import java.util.HashMap;

import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class SaveChunkTask extends Task
{
	private static HashMap<Long, SaveChunkTask> chunksUpdating = new HashMap<Long, SaveChunkTask>();
	private MwChunk chunk;
	private RegionManager regionManager;
	private volatile boolean Running = false;

	public SaveChunkTask(MwChunk chunk, RegionManager regionManager)
	{
		this.chunk = chunk;
		this.regionManager = regionManager;
	}

	@Override
	public boolean isDroppable()
	{
		return false;
	}

	@Override
	public boolean CheckForDuplicate()
	{
		Long coords = this.chunk.getCoordIntPair();

		SaveChunkTask task2 = SaveChunkTask.chunksUpdating.get(coords);
		if (task2 == null)
		{
			SaveChunkTask.chunksUpdating.put(coords, this);
			return false;
		}
		if (!task2.Running)
		{
			task2.UpdateChunkData(this.chunk, this.regionManager);
		}
		else
		{
			SaveChunkTask.chunksUpdating.put(coords, this);
			return false;
		}
		return true;
	}

	@Override
	public void onComplete()
	{
		Long coords = this.chunk.getCoordIntPair();
		SaveChunkTask.chunksUpdating.remove(coords);
		this.Running = false;
	}

	@Override
	public void run()
	{
		this.Running = true;
		this.chunk.write(this.regionManager.regionFileCache);
	}

	public void UpdateChunkData(MwChunk chunk, RegionManager regionManager)
	{
		this.chunk = chunk;
		this.regionManager = regionManager;
	}
}