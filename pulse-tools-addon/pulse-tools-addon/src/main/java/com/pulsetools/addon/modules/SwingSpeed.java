package com.pulsetools.addon.modules;

import com.pulsetools.addon.PulseTools;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class SwingSpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("speed")
        .description("Hand swing speed. 1 is the slowest, 15 is the fastest.")
        .defaultValue(10)
        .range(1, 15)
        .sliderRange(1, 15)
        .build()
    );

    public SwingSpeed() {
        super(PulseTools.CATEGORY, "swing-speed", "Changes how fast your hand swing animation plays (1 = slowest, 15 = fastest).");
    }

    /** Swing length in ticks. Speed 1 -> 15 ticks, speed 10 -> 6 ticks (vanilla), speed 15 -> 1 tick. */
    public int getSwingDuration() {
        return 16 - speed.get();
    }
}
