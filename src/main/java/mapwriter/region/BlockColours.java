package mapwriter.region;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import mapwriter.util.Render;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

public class BlockColours
{
	public class BiomeData
	{
		public int waterMultiplier = 0;
		public int grassMultiplier = 0;
		public int foliageMultiplier = 0;
	}

	public class BlockData
	{
		public int color = 0;
		public BlockType type = BlockType.NORMAL;
	}

	public enum BlockType
	{
		NORMAL,
		GRASS,
		LEAVES,
		FOLIAGE,
		WATER,
		OPAQUE
	}

	public static final int MAX_META = 16;
	public static final String biomeSectionString = "[biomes]";
	public static final String blockSectionString = "[blocks]";

	private LinkedHashMap<String, BiomeData> biomeMap = new LinkedHashMap<String, BiomeData>();
	private LinkedHashMap<String, BlockData[]> bcMap = new LinkedHashMap<String, BlockData[]>();
	private Map<String, Integer> grassCache = new HashMap<String, Integer>();
	private Map<String, Integer> foliageCache = new HashMap<String, Integer>();
	private Map<String, Integer> waterCache = new HashMap<String, Integer>();

	public BlockColours() {}

	public static int getColourFromString(String s)
	{
		return (int) (Long.parseLong(s, 16) & 0xffffffffL);
	}

	public static void writeOverridesFile(File f)
	{
		Writer fout = null;
		try
		{
			fout = new OutputStreamWriter(new FileOutputStream(f));
			fout.write(String.format("version: %s\n", Reference.VERSION));
			fout.write("block minecraft:yellow_flower * 60ffff00	# make dandelions more yellow\n" +
					"block minecraft:red_flower 0 60ff0000		# make poppy more red\n" +
					"block minecraft:red_flower 1 601c92d6		# make Blue Orchid more red\n" +
					"block minecraft:red_flower 2 60b865fb		# make Allium more red\n" +
					"block minecraft:red_flower 3 60e4eaf2		# make Azure Bluet more red\n" +
					"block minecraft:red_flower 4 60d33a17		# make Red Tulip more red\n" +
					"block minecraft:red_flower 5 60e17124		# make Orange Tulip more red\n" +
					"block minecraft:red_flower 6 60ffffff		# make White Tulip more red\n" +
					"block minecraft:red_flower 7 60eabeea		# make Pink Tulip more red\n" +
					"block minecraft:red_flower 8 60eae6ad		# make Oxeye Daisy more red\n" +
					"block minecraft:double_plant 0 60ffff00		# make Sunflower more Yellow-orrange\n" +
					"block minecraft:double_plant 1 d09f78a4		# make Lilac more pink\n" +
					"block minecraft:double_plant 4 60ff0000		# make Rose Bush more red\n" +
					"block minecraft:double_plant 5 d0e3b8f7		# make Peony more red\n" +
					"blocktype minecraft:grass * grass			# grass block\n" +
					"blocktype minecraft:flowing_water * water	# flowing water block\n" +
					"blocktype minecraft:water * water			# still water block\n" +
					"blocktype minecraft:leaves * leaves    		# leaves block\n" +
					"blocktype minecraft:leaves2 * leaves    		# leaves block\n" +
					"blocktype minecraft:leaves 1 opaque    		# pine leaves (not biome colorized)\n" +
					"blocktype minecraft:leaves 2 opaque    		# birch leaves (not biome colorized)\n" +
					"blocktype minecraft:tallgrass * grass     	# tall grass block\n" +
					"blocktype minecraft:vine * foliage  			# vines block\n" +
					"blocktype biomesoplenty:grass * grass		# BOP grass block\n" +
					"blocktype biomesoplenty:plant_0 * grass		# BOP plant block\n" +
					"blocktype biomesoplenty:plant_1 * grass		# BOP plant block\n" +
					"blocktype biomesoplenty:leaves_0 * leaves	# BOP Leave block\n" +
					"blocktype biomesoplenty:leaves_1 * leaves	# BOP Leave block\n" +
					"blocktype biomesoplenty:leaves_2 * leaves	# BOP Leave block\n" +
					"blocktype biomesoplenty:leaves_3 * leaves	# BOP Leave block\n" +
					"blocktype biomesoplenty:leaves_4 * leaves	# BOP Leave block\n" +
					"blocktype biomesoplenty:leaves_5 * leaves	# BOP Leave block\n" +
					"blocktype biomesoplenty:tree_moss * foliage	# biomes o plenty tree moss\n" +
					"block candymod:candy_grass_block 0 ffffb6d5\n" +
					"block candymod:candy_grass_block 1 ff754525\n" +
					"block tconstruct:slime_grass 1 ff30db92\n" +
					"block tconstruct:slime_grass 2 ff30db92\n" +
					"block thebetweenlands:swamp_grass 0 ff2a3f24\n" +
					"block tconstruct:slime_grass 8 ffb12fca\n" +
					"block tconstruct:slime_grass 14 fff2bf00\n" +
					"block aether_legacy:aether_grass 0 ff6ca480\n" +
					"block aether_legacy:enchanted_aether_grass 0 ffd2c376\n");
		}
		catch (IOException e)
		{
			Logging.logError("saving block overrides: could not write to '%s'", f);
		}
		finally
		{
			if (fout != null)
			{
				try { fout.close(); } catch (IOException e) {}
			}
		}
	}

	private static int adjustBlockColourFromType(String BlockName, String meta, BlockType type, int blockColour)
	{
		Block block = Block.getBlockFromName(BlockName);
		switch (type)
		{
			case OPAQUE:
				blockColour |= 0xff000000;
			case NORMAL:
				try
				{
					int renderColour = block.getMapColor(block.getStateFromMeta(Integer.parseInt(meta) & 0xf), null, null).colorValue;
					if (renderColour != 0xffffff)
					{
						blockColour = Render.multiplyColours(blockColour, 0xff000000 | renderColour);
					}
				}
				catch (RuntimeException e) {}
				break;
			case LEAVES:
				blockColour |= 0xff000000;
				break;
			case GRASS:
				blockColour = 0xff9b9b9b;
			default:
				break;
		}
		return blockColour;
	}

	private static String getBlockTypeAsString(BlockType blockType)
	{
		String s = "normal";
		switch (blockType)
		{
			case NORMAL: s = "normal"; break;
			case GRASS: s = "grass"; break;
			case LEAVES: s = "leaves"; break;
			case FOLIAGE: s = "foliage"; break;
			case WATER: s = "water"; break;
			case OPAQUE: s = "opaque"; break;
		}
		return s;
	}

	private static BlockType getBlockTypeFromString(String typeString)
	{
		BlockType blockType = BlockType.NORMAL;
		if (typeString.equalsIgnoreCase("normal")) blockType = BlockType.NORMAL;
		else if (typeString.equalsIgnoreCase("grass")) blockType = BlockType.GRASS;
		else if (typeString.equalsIgnoreCase("leaves")) blockType = BlockType.LEAVES;
		else if (typeString.equalsIgnoreCase("foliage")) blockType = BlockType.FOLIAGE;
		else if (typeString.equalsIgnoreCase("water")) blockType = BlockType.WATER;
		else if (typeString.equalsIgnoreCase("opaque")) blockType = BlockType.OPAQUE;
		else Logging.logWarning("unknown block type '%s'", typeString);
		return blockType;
	}

	private static String getMostOccurringKey(Map<String, Integer> map, String defaultItem)
	{
		int maxCount = 1;
		String mostOccurringKey = defaultItem;
		for (Entry<String, Integer> entry : map.entrySet())
		{
			String key = entry.getKey();
			int count = entry.getValue();
			if (count > maxCount)
			{
				maxCount = count;
				mostOccurringKey = key;
			}
		}
		return mostOccurringKey;
	}

	private static void writeMinimalBlockLines(Writer fout, String lineStart, List<String> items, String defaultItem) throws IOException
	{
		Map<String, Integer> frequencyMap = new HashMap<String, Integer>();
		for (String item : items)
		{
			int count = frequencyMap.containsKey(item) ? frequencyMap.get(item) : 0;
			frequencyMap.put(item, count + 1);
		}
		String mostOccurringItem = getMostOccurringKey(frequencyMap, defaultItem);
		if (!mostOccurringItem.equals(defaultItem))
		{
			fout.write(String.format("%s * %s\n", lineStart, mostOccurringItem));
		}
		int meta = 0;
		for (String s : items)
		{
			if (!s.equals(mostOccurringItem) && !s.equals(defaultItem))
			{
				fout.write(String.format("%s %d %s\n", lineStart, meta, s));
			}
			meta++;
		}
	}

	public boolean CheckFileVersion(File fn)
	{
		String lineData = "";
		try
		{
			RandomAccessFile inFile = new RandomAccessFile(fn, "rw");
			lineData = inFile.readLine();
			inFile.close();
		}
		catch (IOException ex)
		{
			System.err.println(ex.getMessage());
		}
		return lineData.equals(String.format("version: %s", Reference.VERSION));
	}

	public String CombineBlockMeta(String BlockName, int meta)
	{
		return BlockName + " " + meta;
	}

	public String CombineBlockMeta(String BlockName, String meta)
	{
		return BlockName + " " + meta;
	}

	public int getBiomeColour(IBlockState BlockState, int biomeId)
	{
		Biome biome = Biome.getBiomeForId(biomeId);
		if (biomeId == 255) biome = Biomes.PLAINS;
		String biomeName = (biome != null) ? biome.getBiomeName() : "";
		Block block = BlockState.getBlock();
		if (block == null || block.delegate == null || block.delegate.name() == null)
		{
			return 0xffffff;
		}
		int meta = block.getMetaFromState(BlockState);
		return this.getBiomeColour(block.delegate.name().toString(), meta, biomeName);
	}

	public int getBiomeColour(String BlockName, int meta, String biomeName)
	{
		if (biomeName == null) biomeName = "";
		BlockData[] arr = bcMap.get(BlockName);
		if (arr == null) return 0xffffff;
		BlockData data = arr[meta & 0xf];
		if (data == null) return 0xffffff;
		switch (data.type)
		{
			case GRASS:
				return this.getGrassColourMultiplier(biomeName);
			case LEAVES:
			case FOLIAGE:
				return this.getFoliageColourMultiplier(biomeName);
			case WATER:
				return this.getWaterColourMultiplier(biomeName);
			default:
				return 0xffffff;
		}
	}

	public BlockType getBlockType(int BlockAndMeta)
	{
		Block block = Block.getBlockById(BlockAndMeta >> 4);
		int meta = BlockAndMeta & 0xf;
		return this.getBlockType(block.delegate.name().toString(), meta);
	}

	public BlockType getBlockType(String BlockName, int meta)
	{
		BlockData[] arr = bcMap.get(BlockName);
		if (arr == null) return BlockType.NORMAL;
		BlockData data = arr[meta & 0xf];
		return (data != null) ? data.type : BlockType.NORMAL;
	}

	public int getColour(IBlockState BlockState)
	{
		Block block = BlockState.getBlock();
		if (block == null || block.delegate == null || block.delegate.name() == null)
		{
			return 0;
		}
		int meta = block.getMetaFromState(BlockState);
		return this.getColour(block.delegate.name().toString(), meta);
	}

	public int getColour(String BlockName, int meta)
	{
		BlockData[] arr = bcMap.get(BlockName);
		if (arr == null) return 0;
		BlockData data = arr[meta & 0xf];
		return (data != null) ? data.color : 0;
	}

	public void loadFromFile(File f)
	{
		Scanner fin = null;
		try
		{
			fin = new Scanner(new FileReader(f));
			while (fin.hasNextLine())
			{
				String line = fin.nextLine().split("#")[0].trim();
				if (line.length() > 0)
				{
					String[] lineSplit = line.split(" ");
					if (lineSplit[0].equals("biome") && lineSplit.length == 5)
						this.loadBiomeLine(lineSplit);
					else if (lineSplit[0].equals("block") && lineSplit.length == 4)
						this.loadBlockLine(lineSplit);
					else if (lineSplit[0].equals("blocktype") && lineSplit.length == 4)
						this.loadBlockTypeLine(lineSplit);
					else if (lineSplit[0].equals("version:")) {}
					else
						Logging.logWarning("invalid map colour line '%s'", line);
				}
			}
		}
		catch (IOException e)
		{
			Logging.logError("loading block colours: no such file '%s'", f);
		}
		finally
		{
			if (fin != null) fin.close();
		}
	}

	public void saveBiomes(Writer fout) throws IOException
	{
		fout.write("biome * ffffff ffffff ffffff\n");
		for (Map.Entry<String, BiomeData> entry : this.biomeMap.entrySet())
		{
			String biomeName = entry.getKey();
			BiomeData data = entry.getValue();
			if (data.waterMultiplier != 0xffffff || data.grassMultiplier != 0xffffff || data.foliageMultiplier != 0xffffff)
			{
				fout.write(String.format("biome %s %06x %06x %06x\n", biomeName, data.waterMultiplier, data.grassMultiplier, data.foliageMultiplier));
			}
		}
	}

	public void saveBlocks(Writer fout) throws IOException
	{
		fout.write("block * * 00000000\n");
		for (Map.Entry<String, BlockData[]> entry : bcMap.entrySet())
		{
			String blockName = entry.getKey();
			BlockData[] arr = entry.getValue();
			List<String> colours = new ArrayList<>(16);
			for (int i = 0; i < 16; i++)
			{
				BlockData data = arr[i];
				colours.add((data != null) ? String.format("%08x", data.color) : "00000000");
			}
			String lineStart = String.format("block %s", blockName);
			writeMinimalBlockLines(fout, lineStart, colours, "00000000");
		}
	}

	public void saveBlockTypes(Writer fout) throws IOException
	{
		fout.write("blocktype * * normal\n");
		for (Map.Entry<String, BlockData[]> entry : bcMap.entrySet())
		{
			String blockName = entry.getKey();
			BlockData[] arr = entry.getValue();
			List<String> types = new ArrayList<>(16);
			for (int i = 0; i < 16; i++)
			{
				BlockData data = arr[i];
				types.add((data != null) ? getBlockTypeAsString(data.type) : "normal");
			}
			String lineStart = String.format("blocktype %s", blockName);
			writeMinimalBlockLines(fout, lineStart, types, "normal");
		}
	}

	public void saveToFile(File f)
	{
		Writer fout = null;
		try
		{
			fout = new OutputStreamWriter(new FileOutputStream(f));
			fout.write(String.format("version: %s\n", Reference.VERSION));
			this.saveBiomes(fout);
			this.saveBlockTypes(fout);
			this.saveBlocks(fout);
		}
		catch (IOException e)
		{
			Logging.logError("saving block colours: could not write to '%s'", f);
		}
		finally
		{
			if (fout != null)
			{
				try { fout.close(); } catch (IOException e) {}
			}
		}
	}

	public void setBiomeData(String biomeName, int waterShading, int grassShading, int foliageShading)
	{
		BiomeData data = new BiomeData();
		data.foliageMultiplier = foliageShading;
		data.grassMultiplier = grassShading;
		data.waterMultiplier = waterShading;
		biomeMap.put(biomeName, data);
		grassCache.remove(biomeName);
		foliageCache.remove(biomeName);
		waterCache.remove(biomeName);
	}

	public void setBlockType(String BlockName, String meta, BlockType type)
	{
		BlockData[] arr = bcMap.get(BlockName);
		if (arr == null)
		{
			arr = new BlockData[16];
			bcMap.put(BlockName, arr);
		}
		if (meta.equals("*"))
		{
			for (int i = 0; i < 16; i++)
			{
				if (arr[i] == null) arr[i] = new BlockData();
				arr[i].type = type;
				arr[i].color = adjustBlockColourFromType(BlockName, String.valueOf(i), type, arr[i].color);
			}
		}
		else
		{
			int m = Integer.parseInt(meta) & 0xf;
			if (arr[m] == null) arr[m] = new BlockData();
			arr[m].type = type;
			arr[m].color = adjustBlockColourFromType(BlockName, meta, type, arr[m].color);
		}
	}

	public void setColour(String BlockName, String meta, int colour)
	{
		BlockData[] arr = bcMap.get(BlockName);
		if (arr == null)
		{
			arr = new BlockData[16];
			bcMap.put(BlockName, arr);
		}
		if (meta.equals("*"))
		{
			for (int i = 0; i < 16; i++)
			{
				if (arr[i] == null) arr[i] = new BlockData();
				arr[i].color = colour;
			}
		}
		else
		{
			int m = Integer.parseInt(meta) & 0xf;
			if (arr[m] == null) arr[m] = new BlockData();
			arr[m].color = colour;
		}
	}

	private int getFoliageColourMultiplier(String biomeName)
	{
		if (foliageCache.containsKey(biomeName))
			return foliageCache.get(biomeName);
		int multiplier = 0xffffff;
		BiomeData data = this.biomeMap.get(biomeName);
		if (data != null) multiplier = data.foliageMultiplier;
		foliageCache.put(biomeName, multiplier);
		return multiplier;
	}

	private int getGrassColourMultiplier(String biomeName)
	{
		if (grassCache.containsKey(biomeName))
			return grassCache.get(biomeName);
		int multiplier = 0xffffff;
		BiomeData data = this.biomeMap.get(biomeName);
		if (data != null) multiplier = data.grassMultiplier;
		grassCache.put(biomeName, multiplier);
		return multiplier;
	}

	private int getWaterColourMultiplier(String biomeName)
	{
		if (waterCache.containsKey(biomeName))
			return waterCache.get(biomeName);
		int multiplier = 0xffffff;
		BiomeData data = this.biomeMap.get(biomeName);
		if (data != null) multiplier = data.waterMultiplier;
		waterCache.put(biomeName, multiplier);
		return multiplier;
	}

	private void loadBiomeLine(String[] split)
	{
		try
		{
			int waterMultiplier = getColourFromString(split[2]) & 0xffffff;
			int grassMultiplier = getColourFromString(split[3]) & 0xffffff;
			int foliageMultiplier = getColourFromString(split[4]) & 0xffffff;
			this.setBiomeData(split[1], waterMultiplier, grassMultiplier, foliageMultiplier);
		}
		catch (NumberFormatException e)
		{
			Logging.logWarning("invalid biome colour line '%s %s %s %s %s'", split[0], split[1], split[2], split[3], split[4]);
		}
	}

	private void loadBlockLine(String[] split)
	{
		try
		{
			int colour = getColourFromString(split[3]);
			this.setColour(split[1], split[2], colour);
		}
		catch (NumberFormatException e)
		{
			Logging.logWarning("invalid block colour line '%s %s %s %s'", split[0], split[1], split[2], split[3]);
		}
	}

	private void loadBlockTypeLine(String[] split)
	{
		try
		{
			BlockType type = getBlockTypeFromString(split[3]);
			this.setBlockType(split[1], split[2], type);
		}
		catch (NumberFormatException e)
		{
			Logging.logWarning("invalid block colour line '%s %s %s %s'", split[0], split[1], split[2], split[3]);
		}
	}
}