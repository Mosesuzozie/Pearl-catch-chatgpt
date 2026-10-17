package dev.pearlcatch.gui;

import dev.pearlcatch.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/** Mod Menu settings screen: four tabs, two columns of options each. */
public class ConfigScreen extends Screen {
    private static final String[] TABS = {"Swapper", "Pearl", "Side Pearl", "General"};
    private static int lastTab = 0;

    private final Screen parent;
    private int slot;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("pearlcatch.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        slot = 0;
        ModConfig c = ModConfig.get();

        int tabW = 74;
        int total = TABS.length * tabW + (TABS.length - 1) * 4;
        int startX = this.width / 2 - total / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            ButtonWidget tab = ButtonWidget.builder(Text.literal(TABS[i]), b -> {
                lastTab = idx;
                this.clearAndInit();
            }).dimensions(startX + i * (tabW + 4), 24, tabW, 20).build();
            tab.active = i != lastTab;
            this.addDrawableChild(tab);
        }

        switch (lastTab) {
            case 0 -> swapperTab(c);
            case 1 -> pearlTab(c);
            case 2 -> sideTab(c);
            default -> generalTab(c);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void swapperTab(ModConfig c) {
        toggle("Auto Swapper", () -> c.swapperEnabled, v -> c.swapperEnabled = v);
        cycle("Activation", ModConfig.Activation.values(), () -> c.swapperActivation, v -> c.swapperActivation = v);
        cycle("Mode", ModConfig.Speed.values(), () -> c.swapperMode, v -> c.swapperMode = v);
        slider("Smoothness", 0, 1, c.swapperSmoothness, ConfigScreen::percent, v -> c.swapperSmoothness = (float) v);
        toggle("Auto Move (look at target)", () -> c.swapperAutoMove, v -> c.swapperAutoMove = v);
        slider("Blocks per swap", 1, 12, c.blocksPerSwap, v -> String.format("%.1f", v), v -> c.blocksPerSwap = (float) v);
        slider("Max swaps", 1, 20, c.maxSwaps, ConfigScreen::whole, v -> c.maxSwaps = (int) Math.round(v));
        slider("Target range", 8, 100, c.maxTargetRange, ConfigScreen::whole, v -> c.maxTargetRange = (float) Math.round(v));
        slider("Fast delay (ticks)", 2, 10, c.fastSwapDelayTicks, ConfigScreen::whole, v -> c.fastSwapDelayTicks = (int) Math.round(v));
        slider("Slow delay (ticks)", 2, 30, c.slowSwapDelayTicks, ConfigScreen::whole, v -> c.slowSwapDelayTicks = (int) Math.round(v));
        toggle("Only when falling", () -> c.requireFalling, v -> c.requireFalling = v);
        toggle("Cap by air time", () -> c.limitToAirTime, v -> c.limitToAirTime = v);
    }

    private void pearlTab(ModConfig c) {
        toggle("Pearl Catch (up)", () -> c.pearlCatchEnabled, v -> c.pearlCatchEnabled = v);
        cycle("Mode", ModConfig.Speed.values(), () -> c.pearlMode, v -> c.pearlMode = v);
        slider("Smoothness", 0, 1, c.pearlSmoothness, ConfigScreen::percent, v -> c.pearlSmoothness = (float) v);
        toggle("Auto Move (track pearl)", () -> c.pearlAutoMove, v -> c.pearlAutoMove = v);
        slider("Up-throw pitch", 30, 85, c.upThrowPitch, v -> Math.round(v) + "\u00b0", v -> c.upThrowPitch = (float) Math.round(v));
    }

    private void sideTab(ModConfig c) {
        toggle("Auto Side Pearl Catch", () -> c.sideCatchEnabled, v -> c.sideCatchEnabled = v);
        cycle("Mode", ModConfig.Speed.values(), () -> c.sideMode, v -> c.sideMode = v);
        slider("Smoothness", 0, 1, c.sideSmoothness, ConfigScreen::percent, v -> c.sideSmoothness = (float) v);
        toggle("Auto Move (look at landing)", () -> c.sideAutoMove, v -> c.sideAutoMove = v);
    }

    private void generalTab(ModConfig c) {
        slider("Fast aim speed", 5, 90, c.fastAimSpeed, v -> Math.round(v) + "\u00b0/t", v -> c.fastAimSpeed = (float) Math.round(v));
        slider("Slow aim speed", 1, 30, c.slowAimSpeed, v -> Math.round(v) + "\u00b0/t", v -> c.slowAimSpeed = (float) Math.round(v));
        toggle("Action bar info", () -> c.showActionBar, v -> c.showActionBar = v);
    }

    // ---- layout + widget helpers ----

    private int[] next() {
        int col = slot % 2;
        int row = slot / 2;
        slot++;
        return new int[]{this.width / 2 - 155 + col * 160, 54 + row * 24};
    }

    private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        int[] p = next();
        this.addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + onOff(get.getAsBoolean())), b -> {
            boolean now = !get.getAsBoolean();
            set.accept(now);
            b.setMessage(Text.literal(label + ": " + onOff(now)));
        }).dimensions(p[0], p[1], 150, 20).build());
    }

    private <E extends Enum<E>> void cycle(String label, E[] values, Supplier<E> get, Consumer<E> set) {
        int[] p = next();
        this.addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + pretty(get.get())), b -> {
            E nxt = values[(get.get().ordinal() + 1) % values.length];
            set.accept(nxt);
            b.setMessage(Text.literal(label + ": " + pretty(nxt)));
        }).dimensions(p[0], p[1], 150, 20).build());
    }

    private void slider(String label, double min, double max, double current,
                        DoubleFunction<String> fmt, DoubleConsumer set) {
        int[] p = next();
        this.addDrawableChild(new ValueSlider(p[0], p[1], 150, 20, label, min, max, current, fmt, set));
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }

    private static String pretty(Enum<?> e) {
        String n = e.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static String percent(double v) {
        return Math.round(v * 100) + "%";
    }

    private static String whole(double v) {
        return String.valueOf(Math.round(v));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) this.client.setScreen(parent);
    }

    private static final class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleFunction<String> fmt;
        private final DoubleConsumer setter;

        ValueSlider(int x, int y, int w, int h, String label, double min, double max, double current,
                    DoubleFunction<String> fmt, DoubleConsumer setter) {
            super(x, y, w, h, Text.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.fmt = fmt;
            this.setter = setter;
            this.updateMessage();
        }

        private double actual() {
            return min + (max - min) * this.value;
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // super constructor may call this before fields are set
            this.setMessage(Text.literal(label + ": " + fmt.apply(actual())));
        }

        @Override
        protected void applyValue() {
            setter.accept(actual());
        }
    }
          }
