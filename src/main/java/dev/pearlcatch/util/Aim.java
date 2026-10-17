package dev.pearlcatch.util;

import dev.pearlcatch.config.ModConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Smoothed camera rotation toward a world position. */
public final class Aim {
    private Aim() {}

    /**
     * Turns the player's view one tick's worth toward {@code target}.
     * smoothness 0 = snap (still capped by the mode's max degrees/tick), 1 = very gradual.
     */
    public static void step(ClientPlayerEntity player, Vec3d target, ModConfig.Speed mode,
                            float smoothness, ModConfig cfg) {
        double dx = target.x - player.getX();
        double dy = target.y - player.getEyeY();
        double dz = target.z - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-3 && Math.abs(dy) < 1.0E-3) return;

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));

        float yawDelta = horizontal < 0.05 ? 0f : MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDelta = targetPitch - player.getPitch();

        float maxStep = mode == ModConfig.Speed.FAST ? cfg.fastAimSpeed : cfg.slowAimSpeed;
        float factor = 1.0f - 0.9f * MathHelper.clamp(smoothness, 0f, 1f);

        float yawStep = MathHelper.clamp(yawDelta * factor, -maxStep, maxStep);
        float pitchStep = MathHelper.clamp(pitchDelta * factor, -maxStep, maxStep);

        player.setYaw(player.getYaw() + yawStep);
        player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90f, 90f));
    }
}
