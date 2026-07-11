package mapwriter.util;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mapwriter.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.chunk.Chunk;

public class Utils
{
	public static String RealmsWorldName = "";

	private static int[] colours = new int[] { 0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff, 0x00ffff, 0xff8000, 0x8000ff };

	public static int colourIndex = 0;

	public static IntBuffer allocateDirectIntBuffer(int size)
	{
		if (size < 1)
		{
			int NewSize = Minecraft.getGLMaximumTextureSize();
			return ByteBuffer.allocateDirect(NewSize * NewSize * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		}
		return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
	}

	@SuppressWarnings("rawtypes")
	public static <K, V> Map<K, V> checkedMapByCopy(Map rawMap, Class<K> keyType, Class<V> valueType, boolean strict) throws ClassCastException
	{
		Map<K, V> m2 = new HashMap<K, V>(rawMap.size() * 4 / 3 + 1);
		Iterator it = rawMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry e = (Map.Entry) it.next();
			try
			{
				m2.put(keyType.cast(e.getKey()), valueType.cast(e.getValue()));
			}
			catch (ClassCastException x)
			{
				if (strict)
				{
					throw x;
				}
				else
				{
					System.out.println("not assignable");
				}
			}
		}
		return m2;
	}

	public static int distToChunkSq(int x, int z, Chunk chunk)
	{
		int dx = (chunk.x << 4) + 8 - x;
		int dz = (chunk.z << 4) + 8 - z;
		return dx * dx + dz * dz;
	}

	public static int getCurrentColour()
	{
		return 0xff000000 | Utils.colours[Utils.colourIndex];
	}

	public static String getCurrentDateString()
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
		return dateFormat.format(new Date());
	}

	public static File getDimensionDir(File worldDir, int dimension)
	{
		File dimDir;
		if (dimension != 0)
		{
			dimDir = new File(worldDir, "DIM" + dimension);
		}
		else
		{
			dimDir = worldDir;
		}
		return dimDir;
	}

	public static File getFreeFilename(File dir, String baseName, String ext)
	{
		int i = 0;
		File outputFile;
		if (dir != null)
		{
			outputFile = new File(dir, baseName + "." + ext);
		}
		else
		{
			outputFile = new File(baseName + "." + ext);
		}
		while (outputFile.exists() && i < 1000)
		{
			if (dir != null)
			{
				outputFile = new File(dir, baseName + "." + i + "." + ext);
			}
			else
			{
				outputFile = new File(baseName + "." + i + "." + ext);
			}
			i++;
		}
		return i < 1000 ? outputFile : null;
	}

	public static int getMaxWidth(String[] arr, String[] arr2)
	{
		FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRenderer;
		int Width = 1;
		for (int i = 0; i < arr.length; i++)
		{
			int w1 = 0;
			int w2 = 0;

			if (i < arr.length)
			{
				String s = I18n.format(arr[i]);
				w1 = fontRendererObj.getStringWidth(s);
			}
			if (arr2 != null && i < arr2.length)
			{
				String s = I18n.format(arr2[i]);
				w2 = fontRendererObj.getStringWidth(s);
				w2 += 65;
			}
			int wTot = w1 > w2 ? w1 : w2;
			Width = Width > wTot ? Width : wTot;
		}
		return Width;
	}

	public static int getNextColour()
	{
		Utils.colourIndex = (Utils.colourIndex + 1) % Utils.getColoursLengt();
		return Utils.getCurrentColour();
	}

	public static int getPrevColour()
	{
		Utils.colourIndex = (Utils.colourIndex + Utils.getColoursLengt() - 1) % Utils.getColoursLengt();
		return Utils.getCurrentColour();
	}

	public static String getWorldName()
	{
		String worldName;

		if (Minecraft.getMinecraft().isIntegratedServerRunning())
		{
			IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
			worldName = server != null ? server.getFolderName() : "sp_world";
		}
		else if (Minecraft.getMinecraft().isConnectedToRealms())
		{
			if (Utils.RealmsWorldName != "")
			{
				worldName = Utils.RealmsWorldName;
			}
			else
			{
				worldName = "Realms";
			}
		}
		else if (Minecraft.getMinecraft().getCurrentServerData() != null)
		{
			worldName = Minecraft.getMinecraft().getCurrentServerData().serverIP;
			if (!Config.portNumberInWorldNameEnabled)
			{
				worldName = worldName.substring(0, worldName.indexOf(":"));
			}
			else
			{
				if (worldName.indexOf(":") == -1)
				{
					worldName += "_25565";
				}
				else
				{
					worldName = worldName.replace(":", "_");
				}
			}
		}
		else
		{
			worldName = "default";
		}

		worldName = mungeString(worldName);

		if (worldName == null || worldName.isEmpty())
		{
			worldName = "default";
		}
		return worldName;
	}

	public static int[] integerListToIntArray(List<Integer> list)
	{
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++)
		{
			array[i] = list.get(i);
		}
		return array;
	}

	public static String mungeString(String s)
	{
		s = s.replace('.', '_');
		s = s.replace('-', '_');
		s = s.replace(' ', '_');
		s = s.replace('/', '_');
		s = s.replace('\\', '_');
		return Reference.patternInvalidChars.matcher(s).replaceAll("");
	}

	public static String mungeStringForConfig(String s)
	{
		return Reference.patternInvalidChars2.matcher(s).replaceAll("");
	}

	public static int nextHighestPowerOf2(int v)
	{
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		return v + 1;
	}

	public static void openWebLink(URI p_175282_1_)
	{
		try
		{
			Class<?> oclass = Class.forName("java.awt.Desktop");
			Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
			oclass.getMethod("browse", new Class[] { URI.class }).invoke(object, new Object[] { p_175282_1_ });
		}
		catch (Throwable throwable)
		{
			Logging.logError("Couldn\'t open link %s", throwable.getStackTrace().toString());
		}
	}

	public static void printBoth(String msg)
	{
		EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
		if (thePlayer != null)
		{
			thePlayer.sendMessage(new TextComponentString(msg));
		}
		Logging.log("%s", msg);
	}

	public static String stringArrayToString(String[] arr)
	{
		StringBuilder builder = new StringBuilder();
		for (String s : arr)
		{
			builder.append(I18n.format(s));
			builder.append("\n");
		}
		return builder.toString();
	}

	private static int getColoursLengt()
	{
		return Utils.colours.length;
	}
}