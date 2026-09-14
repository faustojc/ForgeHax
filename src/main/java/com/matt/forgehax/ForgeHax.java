package com.matt.forgehax;

import com.matt.forgehax.events.listeners.WorldListener;
import com.matt.forgehax.util.mod.BaseMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.matt.forgehax.Helper.getFileManager;
import static com.matt.forgehax.Helper.getModManager;

@Mod(ForgeHax.MOD_ID)
public class ForgeHax {

  public static final String MOD_ID = "forgehax";
  public static final String MOD_VERSION = ForgeHaxProperties.getVersion();

  static {
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // NOTE: if you ever change the package name make sure this
    // is updated or mods will not load anymore
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    getModManager().searchPackage("com.matt.forgehax.mods.*");
    getModManager().searchPluginDirectory(getFileManager().getBaseResolve("plugins"));
  }

  public ForgeHax(FMLJavaModLoadingContext context) {
    MinecraftForge.EVENT_BUS.register(new WorldListener());
    context.getModEventBus().addListener(this::clientSetup);
  }

  public static String getWelcomeMessage() {
    return String.format("Running ForgeHax v%s\n Type .help in chat for command instructions", MOD_VERSION);
  }

  private void clientSetup(FMLClientSetupEvent event) {
    // ---- initialize mods ----//
    getModManager().loadAll();

    // add shutdown hook to serialize all binds
    Runtime.getRuntime()
           .addShutdownHook(new Thread(() -> getModManager().forEach(BaseMod::unload)));

    // registerAll mod events
    getModManager().forEach(BaseMod::load);
  }
}
