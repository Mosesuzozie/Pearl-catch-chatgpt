package dev.pearlcatch.pearl;

import dev.pearlcatch.config.ModConfig;
import dev.pearlcatch.util.Aim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Follows pearls you throw. Pearls thrown steeply upward use the "pearl catch" settings,
 * everything thrown outward uses the "side catch" settings. With auto move on, the camera
 * turns smoothly toward the pearl (up) or toward its predicted landing spot (side).
 */
public final class PearlCatcher {
    private record Prediction(int ticks, Vec3d impact) {}

    private EnderPearlEntity tracked;
    private boolean trackedUp;

    public void reset() {
        tracked = null;
    }

    /** @return true if this tick the camera was steered by pearl tracking. */
    public boolean tick(MinecraftClient mc, ModConfig cfg) {
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;

        if (tracked != null && (tracked.isRemoved() || !tracked.isAlive())) tracked = null;
        if (tracked == null) acquire(player, world, cfg);
        if (tracked == null) return false;

        boolean up = trackedUp;
        ModConfig.Speed mode = up ? cfg.pearlMode : cfg.sideMode;
        float smoothness = up ? cfg.pearlSmoothness : cfg.sideSmoothness;
        boolean autoMove = up ? cfg.pearlAutoMove : cfg.sideAutoMove;

        Vec3d pos = new Vec3d(tracked.getX(), tracked.getY(), tracked.getZ());
        Prediction pred = predict(world, tracked, pos, tracked.getVelocity());

        if (cfg.showActionBar && player.age % 5 == 0) {
            String label = up ? "Pearl catch" : "Side pearl";
            String text = pred.ticks() > 0
                    ? String.format("%s: lands in %.1fs", label, pred.ticks() / 20.0)
                    : label + ": in flight";
            player.sendMessage(Text.literal(text), true);
        }

        if (!autoMove) return false;
        if (tracked.squaredDistanceTo(player) < 2.25) return false; // already on top of us

        Vec3d aimPoint = up || pred.ticks() <= 0
                ? pos.add(0, tracked.getHeight() * 0.5, 0)
                : pred.impact();
        Aim.step(player, aimPoint, mode, smoothness, cfg);
        return true;
    }

    private void acquire(ClientPlayerEntity player, ClientWorld world, ModConfig cfg) {
        if (!cfg.pearlCatchEnabled && !cfg.sideCatchEnabled) return;

        for (Entity e : world.getEntities()) {
            if (!(e instanceof EnderPearlEntity pearl)) continue;
            if (pearl.age > 3) continue;

            Entity owner = pearl.getOwner();
            boolean mine = owner == player || (owner == null && pearl.squaredDistanceTo(player) < 25.0);
            if (!mine) continue;

            Vec3d v = pearl.getVelocity();
            double speed = v.length();
            if (speed < 0.1) continue;

            double pitch = Math.toDegrees(Math.asin(v.y / speed));
            if (pitch < -70) continue; // thrown at the floor, neither kind of catch

            boolean up = pitch >= cfg.upThrowPitch;
            if (up && !cfg.pearlCatchEnabled) continue;
            if (!up && !cfg.sideCatchEnabled) continue;

            tracked = pearl;
            trackedUp = up;
            return;
        }
    }

    /** Simulates vanilla thrown-projectile physics until the pearl hits a block. */
    private static Prediction predict(ClientWorld world, Entity pearl, Vec3d pos, Vec3d vel) {
        for (int t = 1; t <= 400; t++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = world.raycast(new RaycastContext(
                    pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, pearl));
            if (hit.getType() != HitResult.Type.MISS) return new Prediction(t, hit.getPos());
            pos = next;
            vel = new Vec3d(vel.x * 0.99, vel.y * 0.99 - 0.03, vel.z * 0.99);
        }
        return new Prediction(-1, pos);
    }
}
