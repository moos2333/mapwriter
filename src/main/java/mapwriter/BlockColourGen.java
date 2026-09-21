package mapwriter;

import java.util.HashSet;
import java.util.Set;

import mapwriter.region.BlockColours;
import mapwriter.util.Logging;
import mapwriter.util.Render;
import mapwriter.util.Texture;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.Biome;

// Static class to generate BlockColours.
// This is separate from BlockColours because it needs to run in the GL
// rendering thread
// whereas the generated BlockColours object is used only in the background
// thread.
// So basically split to make it clear that BlockColourGen and the generated
// BlockColours
// must not have any interaction after it is generated.

public class BlockColourGen
{
	private static final String[] META_STRINGS = new String[16];

	static
	{
		for (int i = 0; i < 16; i++)
		{
			META_STRINGS[i] = String.valueOf(i);
		}
	}

	public static void genBlockColours(BlockColours bc)
	{

		Logging.log("generating block map colours from textures");

		// copy terrain texture to MwRender pixel bytebuffer

		// bind the terrain texture
		// Minecraft.getMinecraft().func_110434_K().func_110577_a(TextureMap.field_110575_b);
		// get the bound texture id
		// int terrainTextureId = Render.getBoundTextureId();

		int terrainTextureId = Minecraft.getMinecraft().renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).getGlTextureId();

		// create texture object from the currently bound GL texture
		if (terrainTextureId == 0)
		{
			Logging.log("error: could get terrain texture ID");
			return;
		}
		Texture terrainTexture = new Texture(terrainTextureId);

		double u1Last = 0;
		double u2Last = 0;
		double v1Last = 0;
		double v2Last = 0;
		int blockColourLast = 0;
		int e_count = 0;
		int b_count = 0;
		int s_count = 0;
		Set<String> warnedBlocks = new HashSet<String>();

		for (Object oblock : Block.REGISTRY)
		{
			Block block = (Block) oblock;
			String blockName = block.delegate.name().toString();
			IBlockState defaultState = block.getDefaultState();
			EnumBlockRenderType renderType = block.getRenderType(defaultState);

			for (int dv = 0; dv < 16; dv++)
			{
				int blockColour = 0;

				if (renderType != EnumBlockRenderType.INVISIBLE)
				{

					TextureAtlasSprite icon = null;
					try
					{
						icon = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(block.getStateFromMeta(dv));
					}
					catch (Exception e)
					{
						// MwUtil.log("genFromTextures: exception caught when
						// requesting block texture for %03x:%x",
						// blockID, dv);
						// e.printStackTrace();
						if (warnedBlocks.add(blockName))
						{
							Logging.logWarning("genBlockColours: %s meta=%d: %s", blockName, dv, e.toString());
						}
						e_count++;
					}

					if (icon != null)
					{
						double u1 = icon.getMinU();
						double u2 = icon.getMaxU();
						double v1 = icon.getMinV();
						double v2 = icon.getMaxV();

						if (u1 == u1Last && u2 == u2Last && v1 == v1Last && v2 == v2Last)
						{
							blockColour = blockColourLast;
							s_count++;
						}
						else
						{
							blockColour = getIconMapColour(icon, terrainTexture);

							u1Last = u1;
							u2Last = u2;
							v1Last = v1;
							v2Last = v2;
							blockColourLast = blockColour;
							b_count++;
						}
					}
				}
				bc.setColour(blockName, META_STRINGS[dv], blockColour);
			}
		}

		Logging.log("processed %d block textures, %d skipped, %d exceptions", b_count, s_count, e_count);

		terrainTexture.releasePixelBuffer();

		genBiomeColours(bc);
	}

	private static void genBiomeColours(BlockColours bc)
	{
		// generate array of foliage, grass, and water colour multipliers
		// for each biome.

		for (Biome biome : Biome.REGISTRY)
		{
			if (biome != null)
			{
				double temp = MathHelper.clamp(biome.getDefaultTemperature(), 0.0F, 1.0F);
				double rain = MathHelper.clamp(biome.getRainfall(), 0.0F, 1.0F);
				int grasscolor = ColorizerGrass.getGrassColor(temp, rain);
				int foliagecolor = ColorizerFoliage.getFoliageColor(temp, rain);
				int watercolor = biome.getWaterColorMultiplier();

				bc.setBiomeData(biome.getBiomeName(), watercolor & 0xffffff, grasscolor & 0xffffff, foliagecolor &
						0xffffff);
			}
		}
	}

	private static int getIconMapColour(TextureAtlasSprite icon, Texture terrainTexture)
	{
		// flipped icons have the U and V coords reversed (minU > maxU, minV >
		// maxV).
		// thanks go to taelnia for fixing this.
		int iconX = Math.round(terrainTexture.w * Math.min(icon.getMinU(), icon.getMaxU()));
		int iconY = Math.round(terrainTexture.h * Math.min(icon.getMinV(), icon.getMaxV()));
		int iconWidth = Math.round(terrainTexture.w * Math.abs(icon.getMaxU() - icon.getMinU()));
		int iconHeight = Math.round(terrainTexture.h * Math.abs(icon.getMaxV() - icon.getMinV()));

		int[] pixels = new int[iconWidth * iconHeight];

		// MwUtil.log("(%d, %d) %dx%d", iconX, iconY, iconWidth, iconHeight);

		terrainTexture.getRGB(iconX, iconY, iconWidth, iconHeight, pixels, 0, iconWidth, icon);

		// need to use custom averaging routine rather than scaling down to one
		// pixel to
		// stop transparent pixel colours being included in the average.
		return Render.getAverageColourOfArray(pixels);
	}
}