package mapwriter.overlay;

import java.awt.Point;
import java.util.ArrayList;

import mapwriter.api.IMwChunkOverlay;

final class OverlayChunks
{
	private static final class Chunk implements IMwChunkOverlay
	{
		private final Point coord = new Point(0, 0);
		private final int color;

		Chunk(int color)
		{
			this.color = color;
		}

		void set(int x, int z)
		{
			this.coord.x = x;
			this.coord.y = z;
		}

		@Override
		public int getBorderColor()
		{
			return 0xff000000;
		}

		@Override
		public float getBorderWidth()
		{
			return 0.5f;
		}

		@Override
		public int getColor()
		{
			return this.color;
		}

		@Override
		public Point getCoordinates()
		{
			return this.coord;
		}

		@Override
		public float getFilling()
		{
			return 1.0f;
		}

		@Override
		public boolean hasBorder()
		{
			return true;
		}
	}

	private final int color;
	private final ArrayList<Chunk> pool = new ArrayList<Chunk>();
	private final ArrayList<IMwChunkOverlay> result = new ArrayList<IMwChunkOverlay>();
	private int used;

	OverlayChunks(int color)
	{
		this.color = color;
	}

	void begin()
	{
		this.used = 0;
		this.result.clear();
	}

	void add(int chunkX, int chunkZ)
	{
		Chunk chunk;
		if (this.used < this.pool.size())
		{
			chunk = this.pool.get(this.used);
		}
		else
		{
			chunk = new Chunk(this.color);
			this.pool.add(chunk);
		}
		chunk.set(chunkX, chunkZ);
		this.result.add(chunk);
		this.used++;
	}

	ArrayList<IMwChunkOverlay> end()
	{
		return this.result;
	}
}
