package mapwriter.map;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.ARBDepthClamp;
import org.lwjgl.opengl.GL11;
import mapwriter.Mw;
import mapwriter.config.Config;
import mapwriter.map.mapmode.MapMode;
import mapwriter.util.Logging;
import mapwriter.util.Reference;
import mapwriter.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.common.config.Configuration;

public class MarkerManager
{
	public List<Marker> markerList = new ArrayList<Marker>();
	public List<String> groupList = new ArrayList<String>();
	public List<Marker> visibleMarkerList = new ArrayList<Marker>();
	private String visibleGroupName = "none";
	public Marker selectedMarker = null;
	private Map<Integer, Configuration> dimConfigs = new HashMap<Integer, Configuration>();

	public MarkerManager() {}

	private File getWorldDir()
	{
		return Mw.getInstance().worldDir;
	}

	private Configuration getDimConfig(int dimension)
	{
		Configuration config = this.dimConfigs.get(dimension);
		if (config == null)
		{
			File worldDir = this.getWorldDir();
			if (worldDir == null)
			{
				return null;
			}
			File configFile = new File(worldDir, "markers_dim" + dimension + ".cfg");
			config = new Configuration(configFile);
			this.dimConfigs.put(dimension, config);
		}
		return config;
	}

	private List<Marker> loadFromConfig(Configuration config)
	{
		List<Marker> list = new ArrayList<Marker>();
		if (config == null)
		{
			return list;
		}
		String category = Reference.catMarkers;
		if (config.hasCategory(category))
		{
			int markerCount = config.get(category, "markerCount", 0).getInt();
			if (markerCount > 0)
			{
				for (int i = 0; i < markerCount; i++)
				{
					String key = "marker" + i;
					String value = config.get(category, key, "").getString();
					Marker marker = this.stringToMarker(value);
					if (marker != null)
					{
						list.add(marker);
					}
					else
					{
						Logging.log("error: could not load " + key + " from config file");
					}
				}
			}
		}
		return list;
	}

	private void saveToConfig(Configuration config, List<Marker> list)
	{
		if (config == null)
		{
			return;
		}
		String category = Reference.catMarkers;
		config.removeCategory(config.getCategory(category));
		config.get(category, "markerCount", 0).set(list.size());
		int i = 0;
		for (Marker marker : list)
		{
			String key = "marker" + i;
			String value = this.markerToString(marker);
			config.get(category, key, "").set(value);
			i++;
		}
		if (config.hasChanged())
		{
			config.save();
		}
	}

	public void loadAll()
	{
		this.markerList.clear();
		File worldDir = this.getWorldDir();
		if (worldDir == null)
		{
			return;
		}
		File[] files = worldDir.listFiles((dir, name) -> name.startsWith("markers_dim") && name.endsWith(".cfg"));
		if (files != null)
		{
			for (File file : files)
			{
				try
				{
					Configuration config = new Configuration(file);
					List<Marker> loaded = this.loadFromConfig(config);
					this.markerList.addAll(loaded);
				}
				catch (Exception e)
				{
					Logging.logError("Failed to load markers from %s: %s", file.getName(), e.getMessage());
				}
			}
		}
		this.update();
	}

	public void saveAll()
	{
		if (this.getWorldDir() == null)
		{
			return;
		}
		Map<Integer, List<Marker>> dimMap = new HashMap<Integer, List<Marker>>();
		for (Marker marker : this.markerList)
		{
			int dim = marker.dimension;
			List<Marker> list = dimMap.get(dim);
			if (list == null)
			{
				list = new ArrayList<Marker>();
				dimMap.put(dim, list);
			}
			list.add(marker);
		}
		for (Map.Entry<Integer, List<Marker>> entry : dimMap.entrySet())
		{
			int dim = entry.getKey();
			List<Marker> list = entry.getValue();
			Configuration config = this.getDimConfig(dim);
			if (config != null)
			{
				try
				{
					this.saveToConfig(config, list);
				}
				catch (Exception e)
				{
					Logging.logError("Failed to save markers for dimension %d: %s", dim, e.getMessage());
				}
			}
		}
	}

	public void addMarker(Marker marker)
	{
		this.markerList.add(marker);
		this.saveAll();
	}

	public void addMarker(String name, String groupName, int x, int y, int z, int dimension, int colour)
	{
		this.addMarker(new Marker(name, groupName, x, y, z, dimension, colour));
	}

	public boolean delMarker(Marker markerToDelete)
	{
		if (this.selectedMarker == markerToDelete)
		{
			this.selectedMarker = null;
		}
		boolean result = this.markerList.remove(markerToDelete);
		if (result)
		{
			this.saveAll();
		}
		return result;
	}

	public boolean delMarker(String name, String group)
	{
		Marker markerToDelete = null;
		for (Marker marker : this.markerList)
		{
			if ((name == null || marker.name.equals(name)) && (group == null || marker.groupName.equals(group)))
			{
				markerToDelete = marker;
				break;
			}
		}
		return this.delMarker(markerToDelete);
	}

	public void clear()
	{
		this.markerList.clear();
		this.groupList.clear();
		this.visibleMarkerList.clear();
		this.visibleGroupName = "none";
		this.dimConfigs.clear();
	}

	public int countMarkersInGroup(String group)
	{
		int count = 0;
		if (group.equals("all"))
		{
			count = this.markerList.size();
		}
		else
		{
			for (Marker marker : this.markerList)
			{
				if (marker.groupName.equals(group))
				{
					count++;
				}
			}
		}
		return count;
	}

	public void drawBeam(Marker m, float partialTicks)
	{
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder vertexbuffer = tessellator.getBuffer();
		float f2 = Minecraft.getMinecraft().world.getTotalWorldTime() + partialTicks;
		double d3 = f2 * 0.025D * -1.5D;
		double d17 = 255.0D;
		double x = m.x - TileEntityRendererDispatcher.staticPlayerX;
		double y = 0.0D - TileEntityRendererDispatcher.staticPlayerY;
		double z = m.z - TileEntityRendererDispatcher.staticPlayerZ;
		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.depthMask(false);
		vertexbuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		double d4 = 0.2D;
		double d5 = 0.5D + Math.cos(d3 + 2.356194490192345D) * d4;
		double d6 = 0.5D + Math.sin(d3 + 2.356194490192345D) * d4;
		double d7 = 0.5D + Math.cos(d3 + Math.PI / 4D) * d4;
		double d8 = 0.5D + Math.sin(d3 + Math.PI / 4D) * d4;
		double d9 = 0.5D + Math.cos(d3 + 3.9269908169872414D) * d4;
		double d10 = 0.5D + Math.sin(d3 + 3.9269908169872414D) * d4;
		double d11 = 0.5D + Math.cos(d3 + 5.497787143782138D) * d4;
		double d12 = 0.5D + Math.sin(d3 + 5.497787143782138D) * d4;
		float fRed = m.getRed();
		float fGreen = m.getGreen();
		float fBlue = m.getBlue();
		float fAlpha = 0.125f;
		vertexbuffer.pos(x + d5, y + d17, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d5, y, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y + d17, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y + d17, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y + d17, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y + d17, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y + d17, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y + d17, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d5, y, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d5, y + d17, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		tessellator.draw();
		vertexbuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		d4 = 0.5D;
		d5 = 0.5D + Math.sin(d3 + 2.356194490192345D) * d4;
		d6 = 0.5D + Math.cos(d3 + 2.356194490192345D) * d4;
		d7 = 0.5D + Math.sin(d3 + Math.PI / 4D) * d4;
		d8 = 0.5D + Math.cos(d3 + Math.PI / 4D) * d4;
		d9 = 0.5D + Math.sin(d3 + 3.9269908169872414D) * d4;
		d10 = 0.5D + Math.cos(d3 + 3.9269908169872414D) * d4;
		d11 = 0.5D + Math.sin(d3 + 5.497787143782138D) * d4;
		d12 = 0.5D + Math.cos(d3 + 5.497787143782138D) * d4;
		vertexbuffer.pos(x + d5, y + d17, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d5, y, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y + d17, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y + d17, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y + d17, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y + d17, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d7, y, z + d8).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d11, y + d17, z + d12).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y + d17, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d9, y, z + d10).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d5, y, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(x + d5, y + d17, z + d6).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		tessellator.draw();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	public void drawLabel(Marker m)
	{
		float growFactor = 0.17F;
		Minecraft mc = Minecraft.getMinecraft();
		RenderManager renderManager = mc.getRenderManager();
		FontRenderer fontrenderer = mc.fontRenderer;
		double x = 0.5D + m.x - TileEntityRendererDispatcher.staticPlayerX;
		double y = 0.5D + m.y - TileEntityRendererDispatcher.staticPlayerY;
		double z = 0.5D + m.z - TileEntityRendererDispatcher.staticPlayerZ;
		float fRed = m.getRed();
		float fGreen = m.getGreen();
		float fBlue = m.getBlue();
		float fAlpha = 0.2f;
		double distance = m.getDistanceToMarker(renderManager.renderViewEntity);
		String strText = m.name;
		String strDistance = " (" + (int) distance + "m)";
		int strTextWidth = fontrenderer.getStringWidth(strText) / 2;
		int strDistanceWidth = fontrenderer.getStringWidth(strDistance) / 2;
		int offstet = 9;
		float f = (float) (1.0F + distance * growFactor);
		float f1 = 0.016666668F * f;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-f1, -f1, f1);
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		GlStateManager.disableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GL11.glEnable(ARBDepthClamp.GL_DEPTH_CLAMP);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder vertexbuffer = tessellator.getBuffer();
		GlStateManager.disableTexture2D();
		vertexbuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		vertexbuffer.pos(-strTextWidth - 1, -1, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(-strTextWidth - 1, 8, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(strTextWidth + 1, 8, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(strTextWidth + 1, -1, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		tessellator.draw();
		vertexbuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		vertexbuffer.pos(-strDistanceWidth - 1, -1 + offstet, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(-strDistanceWidth - 1, 8 + offstet, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(strDistanceWidth + 1, 8 + offstet, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		vertexbuffer.pos(strDistanceWidth + 1, -1 + offstet, 0.0D).color(fRed, fGreen, fBlue, fAlpha).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);
		fontrenderer.drawString(strText, -strTextWidth, 0, -1);
		fontrenderer.drawString(strDistance, -strDistanceWidth, offstet, -1);
		GL11.glDisable(ARBDepthClamp.GL_DEPTH_CLAMP);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	public void drawMarkers(MapMode mapMode, MapView mapView)
	{
		for (Marker marker : this.visibleMarkerList)
		{
			if (mapView.getDimension() == marker.dimension)
			{
				marker.draw(mapMode, mapView, 0xff000000);
			}
		}
		if (this.selectedMarker != null)
		{
			this.selectedMarker.draw(mapMode, mapView, 0xffffffff);
		}
	}

	public void drawMarkersWorld(float partialTicks)
	{
		if ((!Config.drawMarkersInWorld && !Config.drawMarkersNameInWorld) || Minecraft.getMinecraft().getRenderManager().renderViewEntity == null)
		{
			return;
		}
		for (Marker m : this.visibleMarkerList)
		{
			if (m.dimension == Minecraft.getMinecraft().player.dimension)
			{
				if (Config.drawMarkersInWorld)
				{
					this.drawBeam(m, partialTicks);
				}
				if (Config.drawMarkersNameInWorld)
				{
					this.drawLabel(m);
				}
			}
		}
	}

	public Marker getNearestMarker(int x, int z, int maxDistance)
	{
		int nearestDistance = maxDistance * maxDistance;
		Marker nearestMarker = null;
		for (Marker marker : this.visibleMarkerList)
		{
			int dx = x - marker.x;
			int dz = z - marker.z;
			int d = dx * dx + dz * dz;
			if (d < nearestDistance)
			{
				nearestMarker = marker;
				nearestDistance = d;
			}
		}
		return nearestMarker;
	}

	public Marker getNearestMarkerInDirection(int x, int z, double desiredAngle)
	{
		int nearestDistance = 10000 * 10000;
		Marker nearestMarker = null;
		for (Marker marker : this.visibleMarkerList)
		{
			int dx = marker.x - x;
			int dz = marker.z - z;
			int d = dx * dx + dz * dz;
			double angle = Math.atan2(dz, dx);
			if (Math.cos(desiredAngle - angle) > 0.8D && d < nearestDistance && d > 4)
			{
				nearestMarker = marker;
				nearestDistance = d;
			}
		}
		return nearestMarker;
	}

	public String getVisibleGroupName()
	{
		return this.visibleGroupName;
	}

	public void nextGroup()
	{
		this.nextGroup(1);
	}

	public void nextGroup(int n)
	{
		if (this.groupList.size() > 0)
		{
			int i = this.groupList.indexOf(this.visibleGroupName);
			int size = this.groupList.size();
			if (i != -1)
			{
				i = (i + size + n) % size;
			}
			else
			{
				i = 0;
			}
			this.visibleGroupName = this.groupList.get(i);
		}
		else
		{
			this.visibleGroupName = "none";
			this.groupList.add("none");
		}
	}

	public String markerToString(Marker marker)
	{
		return String.format("%s:%d:%d:%d:%d:%06x:%s", marker.name, marker.x, marker.y, marker.z, marker.dimension, marker.colour & 0xffffff, marker.groupName);
	}

	public Marker stringToMarker(String s)
	{
		String[] split = s.split(":");
		if (split.length != 7)
		{
			split = s.split(" ");
		}
		Marker marker = null;
		if (split.length == 7)
		{
			try
			{
				int x = Integer.parseInt(split[1]);
				int y = Integer.parseInt(split[2]);
				int z = Integer.parseInt(split[3]);
				int dimension = Integer.parseInt(split[4]);
				int colour = 0xff000000 | Integer.parseInt(split[5], 16);
				marker = new Marker(split[0], split[6], x, y, z, dimension, colour);
			}
			catch (NumberFormatException e)
			{
				marker = null;
			}
		}
		else
		{
			Logging.log("Marker.stringToMarker: invalid marker '%s'", s);
		}
		return marker;
	}

	public void selectNextMarker()
	{
		if (this.visibleMarkerList.size() > 0)
		{
			int i = 0;
			if (this.selectedMarker != null)
			{
				i = this.visibleMarkerList.indexOf(this.selectedMarker);
				if (i == -1)
				{
					i = 0;
				}
			}
			i = (i + 1) % this.visibleMarkerList.size();
			this.selectedMarker = this.visibleMarkerList.get(i);
		}
		else
		{
			this.selectedMarker = null;
		}
	}

	public void setVisibleGroupName(String groupName)
	{
		if (groupName != null)
		{
			this.visibleGroupName = Utils.mungeStringForConfig(groupName);
		}
		else
		{
			this.visibleGroupName = "none";
		}
	}

	public void update()
	{
		this.visibleMarkerList.clear();
		this.groupList.clear();
		this.groupList.add("none");
		this.groupList.add("all");
		for (Marker marker : this.markerList)
		{
			if (marker.groupName.equals(this.visibleGroupName) || this.visibleGroupName.equals("all"))
			{
				this.visibleMarkerList.add(marker);
			}
			if (!this.groupList.contains(marker.groupName))
			{
				this.groupList.add(marker.groupName);
			}
		}
		if (!this.groupList.contains(this.visibleGroupName))
		{
			this.visibleGroupName = "none";
		}
	}
}