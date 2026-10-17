package dev.pearlcatch;

import dev.pearlcatch.config.ModConfig;
import dev.pearlcatch.pearl.PearlCatcher;
import dev.pearlcatch.swap.AutoSwapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class PearlCatchMod implements ClientModInitializer {
    public static final String MOD_ID = "pearlcatch";

    private static final AutoSwapper SWAPPER = new AutoSwapper();
    private static final PearlCatcher PEARLS = new PearlCatcher();
    private static KeyBinding triggerKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".swap_trigger",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(PearlCatchMod::tick);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            SWAPPER.reset();
            PEARLS.reset();
            return;
        }
        if (mc.currentScreen != null) {
            SWAPPER.reset();
            return;
        }

        ModConfig cfg = ModConfig.get();
        // Pearl tracking goes first; if it is steering the camera, the swapper doesn't fight it.
        boolean pearlAimed = PEARLS.tick(mc, cfg);
        SWAPPER.tick(mc, cfg, triggerKey.isPressed(), !pearlAimed);
    }
}
