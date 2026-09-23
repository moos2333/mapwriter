package mapwriter;

import java.util.Arrays;
import java.util.Map;

import mapwriter.config.Config;
import mapwriter.region.MwChunk;
import mapwriter.tasks.SaveChunkTask;
import mapwriter.tasks.UpdateSurfaceChunksTask;
import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager
{
	private static final int VISIBLE_FLAG = 0x01;
	private static final int VIEWED_FLAG = 0x02;

	// create MwChunk from Minecraft chunk.
	// only MwChunk's should be used in the background thread.
	// make this a full copy of chunk data to prevent possible race conditions
	// <-- done
	public static MwChunk copyToMwChunk(Chunk chunk)
	{
		Map<BlockPos, TileEntity> TileEntityMap = Utils.checkedMapByCopy(chunk.getTileEntityMap(), BlockPos.class, TileEntity.class, false);
		byte[] biomeSrc = chunk.getBiomeArray();
		byte[] biomeArray = Arrays.copyOf(biomeSrc, biomeSrc.length);
		ExtendedBlockStorage[] dataSrc = chunk.getBlockStorageArray();
		ExtendedBlockStorage[] dataArray = Arrays.copyOf(dataSrc, dataSrc.length);

		return new MwChunk(
				chunk.x,
				chunk.z,
				chunk.getWorld().provider.getDimensionType().getId(),
				dataArray,
				biomeArray,
				TileEntityMap);
	}

	public Mw mw;
	private boolean closed = false;

	private CircularHashMap<Chunk, Integer> chunkMap = new CircularHashMap<Chunk, Integer>();

	public ChunkManager(Mw mw)
	{
		this.mw = mw;
	}

	public synchronized void addChunk(Chunk chunk)
	{
		if (!this.closed && chunk != null)
		{
			this.chunkMap.put(chunk, 0);
		}
	}

	public synchronized void close()
	{
		this.closed = true;
		this.saveChunks();
		this.chunkMap.clear();
	}

	public void onTick()
	{
		if (!this.closed)
		{
			if ((this.mw.tickCounter & 0xf) == 0)
			{
				this.updateUndergroundChunks();
			}
			else
			{
				this.updateSurfaceChunks();
			}
		}
	}

	public synchronized void removeChunk(Chunk chunk)
	{
		if (!this.closed && chunk != null)
		{
			if (!this.chunkMap.containsKey(chunk))
			{
				return; // FIXME: Is this failsafe enough for unloading?
			}
			int flags = this.chunkMap.get(chunk);
			if ((flags & ChunkManager.VIEWED_FLAG) != 0)
			{
				this.addSaveChunkTask(chunk);
			}
			this.chunkMap.remove(chunk);
		}
	}

	public synchronized void saveChunks()
	{
		for (Map.Entry<Chunk, Integer> entry : this.chunkMap.entrySet())
		{
			int flags = entry.getValue();
			if ((flags & ChunkManager.VIEWED_FLAG) != 0)
			{
				this.addSaveChunkTask(entry.getKey());
			}
		}
	}

	public void updateSurfaceChunks()
	{
		int chunksToUpdate = Math.min(this.chunkMap.size(), Config.chunksPerTick);
		for (int i = 0; i < chunksToUpdate; i++)
		{
			Map.Entry<Chunk, Integer> entry = this.chunkMap.getNextEntry();
			if (entry != null)
			{
				// if this chunk is within a certain distance to the player then
				// add it to the viewed set
				Chunk chunk = entry.getKey();

				int flags = entry.getValue();
				if (Utils.distToChunkSq(this.mw.playerXInt, this.mw.playerZInt, chunk) <= Config.maxChunkSaveDistSq)
				{
					flags |= ChunkManager.VISIBLE_FLAG | ChunkManager.VIEWED_FLAG;
				}
				else
				{
					flags &= ~ChunkManager.VISIBLE_FLAG;
				}
				entry.setValue(flags);

				if ((flags & ChunkManager.VISIBLE_FLAG) != 0)
				{
					this.mw.executor.addTask(new UpdateSurfaceChunksTask(this.mw, copyToMwChunk(chunk)));
				}
			}
		}
	}

	public void updateUndergroundChunks()
	{
	}

	private void addSaveChunkTask(Chunk chunk)
	{
		boolean singleplayer = Minecraft.getMinecraft().isSingleplayer();
		if (singleplayer && Config.regionFileOutputEnabledMP ||
				!singleplayer && Config.regionFileOutputEnabledSP)
		{
			if (!chunk.isEmpty())
			{
				this.mw.executor.addTask(new SaveChunkTask(copyToMwChunk(chunk), this.mw.regionManager));
			}
		}
	}
}