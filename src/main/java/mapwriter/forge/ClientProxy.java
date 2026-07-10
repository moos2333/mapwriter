package mapwriter.forge;

import java.io.File;
import mapwriter.Mw;
import mapwriter.api.MwAPI;
import mapwriter.config.ConfigurationHandler;
import mapwriter.overlay.OverlayGrid;
import mapwriter.overlay.OverlaySlime;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy
{
	@Override
	public void load() {}

	@Override
	public void postInit()
	{
		MwAPI.registerDataProvider("Slime", new OverlaySlime());
		MwAPI.registerDataProvider("Grid", new OverlayGrid());
	}

	@Override
	public void preInit(File configFile)
	{
		ConfigurationHandler.init(configFile);
		MinecraftForge.EVENT_BUS.register(new ConfigurationHandler());
		MinecraftForge.EVENT_BUS.register(new EventHandler(Mw.getInstance()));
		MinecraftForge.EVENT_BUS.register(new MwKeyHandler());
	}
}