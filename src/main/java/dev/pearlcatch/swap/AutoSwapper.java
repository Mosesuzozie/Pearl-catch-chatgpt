package dev.pearlcatch.swap;

import dev.pearlcatch.config.ModConfig;
import dev.pearlcatch.util.Aim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * While airborne, guesses how many swaps are needed to reach the nearest player and
 * "presses" the swap key (G by default) that many times. The press goes through the normal
 * keybinding system, so whatever client/mod action is bound to G fires.
 */
public final class AutoSwapper {
    private int pending;
    private int cooldown;
    private boolean releasePending;
    private boolean triggered;
    private InputUtil.Key heldKey;

    public void reset() {
        releaseKey();
        pending = 0;
        cooldown = 0;
        triggered = false;
    }

    private void releaseKey() {
        if (releasePending && heldKey != null) {
            KeyBinding.setKeyPressed(heldKey, false);
        }
        releasePending = false;
    }

    public void tick(MinecraftClient mc, ModConfig cfg, boolean triggerHeld, boolean allowAim) {
        ClientPlayerEntity player = mc.player;
        releaseKey();

        boolean air = isAirborne(player);
        if (!air) {
            triggered = false;
            pending = 0;
            cooldown = 0;
        }
        if (!cfg.swapperEnabled || !air) return;

        AbstractClientPlayerEntity target = findTarget(mc, player, cfg.maxTargetRange);
        if (target != null) {
            if (cfg.swapperAutoMove && allowAim) {
                Vec3d aimPoint = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
                Aim.step(player, aimPoint, cfg.swapperMode, cfg.swapperSmoothness, cfg);
            }

            if (!triggered && pending == 0) {
                boolean activated = cfg.swapperActivation == ModConfig.Activation.ALWAYS || triggerHeld;
                boolean falling = !cfg.requireFalling || player.getVelocity().y < -0.05;
                if (activated && falling) {
                    pending = estimateSwaps(mc, player, target, cfg);
                    triggered = true;
                    cooldown = 0;
                    if (cfg.showActionBar) {
                        player.sendMessage(Text.literal("Auto Swapper: " + pending + " swaps"), true);
                    }
                }
            }
        }

        if (pending > 0) pressStep(cfg);
    }

    private void pressStep(ModConfig cfg) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        heldKey = InputUtil.Type.KEYSYM.createFromCode(cfg.swapKeyCode);
        KeyBinding.onKeyPressed(heldKey);
        KeyBinding.setKeyPressed(heldKey, true);
        releasePending = true;
        pending--;
        cooldown = Math.max(2, swapDelay(cfg)) - 1;
    }

    private static int swapDelay(ModConfig cfg) {
        return cfg.swapperMode == ModConfig.Speed.FAST ? cfg.fastSwapDelayTicks : cfg.slowSwapDelayTicks;
    }

    private static boolean isAirborne(ClientPlayerEntity p) {
        return !p.isOnGround() && !p.isTouchingWater() && !p.isClimbing();
    }

    private static AbstractClientPlayerEntity findTarget(MinecraftClient mc, ClientPlayerEntity self, double range) {
        AbstractClientPlayerEntity best = null;
        double bestSq = range * range;
        for (AbstractClientPlayerEntity other : mc.world.getPlayers()) {
            if (other == self || other.isSpectator() || !other.isAlive()) continue;
            double d = other.squaredDistanceTo(self);
            if (d < bestSq) {
                bestSq = d;
                best = other;
            }
        }
        return best;
    }

    /** swaps = distance / blocksPerSwap, clamped, and optionally capped by how long we stay airborne. */
    private int estimateSwaps(MinecraftClient mc, ClientPlayerEntity self, AbstractClientPlayerEntity target, ModConfig cfg) {
        double dx = target.getX() - self.getX();
        double dy = target.getY() - self.getY();
        double dz = target.getZ() - self.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        int swaps = (int) Math.round(dist / Math.max(0.5, cfg.blocksPerSwap));
        swaps = MathHelper.clamp(swaps, 1, cfg.maxSwaps);

        if (cfg.limitToAirTime) {
            int airTicks = estimateAirTicks(mc, self);
            int possible = Math.max(1, airTicks / Math.max(2, swapDelay(cfg)));
            swaps = Math.min(swaps, possible);
        }
        return swaps;
    }

    /** Rough number of ticks until the player lands, simulating vanilla fall physics. */
    private static int estimateAirTicks(MinecraftClient mc, ClientPlayerEntity self) {
        Vec3d pos = new Vec3d(self.getX(), self.getY(), self.getZ());
        Vec3d vel = self.getVelocity();
        for (int t = 1; t <= 200; t++) {
            vel = new Vec3d(vel.x * 0.91, (vel.y - 0.08) * 0.98, vel.z * 0.91);
            Vec3d next = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, self));
            if (hit.getType() != HitResult.Type.MISS) return t;
            pos = next;
        }
        return 200;
    }
}
