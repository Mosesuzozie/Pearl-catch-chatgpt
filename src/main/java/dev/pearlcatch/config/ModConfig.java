package dev.pearlcatch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, stored as config/pearlcatch.json. Edited through the Mod Menu screen. */
public class ModConfig {
    public enum Speed { FAST, SLOW }
    public enum Activation { ALWAYS, KEY_HELD }

    // ---- Auto Swapper ----
    public boolean swapperEnabled = true;
    public Activation swapperActivation = Activation.ALWAYS;
    public Speed swapperMode = Speed.FAST;
    public float swapperSmoothness = 0.35f;
    public boolean swapperAutoMove = false;
    /** GLFW key code that gets "pressed" for each swap. 71 = G. */
    public int swapKeyCode = 71;
    /** Distance one swap is assumed to cover; swaps = distance / blocksPerSwap. */
    public float blocksPerSwap = 4.0f;
    public int maxSwaps = 8;
    public float maxTargetRange = 40.0f;
    public int fastSwapDelayTicks = 2;
    public int slowSwapDelayTicks = 6;
    public boolean requireFalling = true;
    public boolean limitToAirTime = true;

    // ---- Pearl catch (pearl thrown up) ----
    public boolean pearlCatchEnabled = true;
    public Speed pearlMode = Speed.FAST;
    public float pearlSmoothness = 0.35f;
    public boolean pearlAutoMove = false;
    /** Throw pitch (degrees, looking up) at or above which a pearl counts as "up". */
    public float upThrowPitch = 60.0f;

    // ---- Side pearl catch ----
    public boolean sideCatchEnabled = true;
    public Speed sideMode = Speed.FAST;
    public float sideSmoothness = 0.35f;
    public boolean sideAutoMove = false;

    // ---- General ----
    /** Max degrees turned per tick in FAST / SLOW mode. */
    public float fastAimSpeed = 40.0f;
    public float slowAimSpeed = 8.0f;
    public boolean showActionBar = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("pearlcatch.json");
    }

    public static ModConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        ModConfig loaded = null;
        Path p = path();
        if (Files.exists(p)) {
            try (BufferedReader r = Files.newBufferedReader(p)) {
                loaded = GSON.fromJson(r, ModConfig.class);
            } catch (Exception ignored) {
                loaded = null;
            }
        }
        instance = loaded != null ? loaded : new ModConfig();
        instance.sanitize();
        save();
    }

    public static void save() {
        if (instance == null) return;
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(instance));
        } catch (IOException ignored) {
        }
    }

    private void sanitize() {
        if (swapperActivation == null) swapperActivation = Activation.ALWAYS;
        if (swapperMode == null) swapperMode = Speed.FAST;
        if (pearlMode == null) pearlMode = Speed.FAST;
        if (sideMode == null) sideMode = Speed.FAST;

        swapperSmoothness = MathHelper.clamp(swapperSmoothness, 0f, 1f);
        pearlSmoothness = MathHelper.clamp(pearlSmoothness, 0f, 1f);
        sideSmoothness = MathHelper.clamp(sideSmoothness, 0f, 1f);

        blocksPerSwap = MathHelper.clamp(blocksPerSwap, 0.5f, 20f);
        maxSwaps = MathHelper.clamp(maxSwaps, 1, 50);
        maxTargetRange = MathHelper.clamp(maxTargetRange, 4f, 128f);
        fastSwapDelayTicks = MathHelper.clamp(fastSwapDelayTicks, 2, 40);
        slowSwapDelayTicks = MathHelper.clamp(slowSwapDelayTicks, 2, 40);
        upThrowPitch = MathHelper.clamp(upThrowPitch, 20f, 89f);
        fastAimSpeed = MathHelper.clamp(fastAimSpeed, 1f, 180f);
        slowAimSpeed = MathHelper.clamp(slowAimSpeed, 0.5f, 90f);
        if (swapKeyCode <= 0) swapKeyCode = 71;
    }
}
