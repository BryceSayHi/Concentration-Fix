package dev.concentrationfix;

import com.mojang.blaze3d.platform.Monitor;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.FullscreenMode;
import net.deechael.concentration.config.Config;
import net.deechael.concentration.config.ConfigProvider;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@ConfigEntryPointForge(ConcentrationFix.MOD_ID)
public class ConcentrationFixConfig implements ConfigEntryPoint {

    private static final ResourceLocation SODIUM_FULLSCREEN = ResourceLocation.parse("sodium:general.fullscreen");
    private static final ResourceLocation SODIUM_FULLSCREEN_RESOLUTION = ResourceLocation.parse("sodium:general.fullscreen_resolution");
    private static final ResourceLocation FULLSCREEN = ResourceLocation.fromNamespaceAndPath(ConcentrationFix.MOD_ID, "fullscreen");

    enum FullscreenState {
        OFF(null),
        EXCLUSIVE(FullscreenMode.EXCLUSIVE),
        NATIVE(FullscreenMode.NATIVE),
        BORDERLESS(FullscreenMode.BORDERLESS);

        private final FullscreenMode mode;

        FullscreenState(FullscreenMode mode) {
            this.mode = mode;
        }

        Component label() {
            return this.mode == null ? Component.translatable("options.off") : Component.translatable(this.mode.getKey());
        }

        static FullscreenState of(FullscreenMode mode) {
            for (FullscreenState state : values()) {
                if (state.mode == mode) {
                    return state;
                }
            }
            return BORDERLESS;
        }
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .registerOptionReplacement(SODIUM_FULLSCREEN, builder.createEnumOption(FULLSCREEN, FullscreenState.class)
                        .setName(Component.translatable("options.fullscreen"))
                        .setTooltip(Component.translatable("concentration.option.fullscreen_mode.tooltip"))
                        .setElementNameProvider(FullscreenState::label)
                        .setDefaultValue(FullscreenState.OFF)
                        .setStorageHandler(() -> {
                            config().save();
                            Minecraft.getInstance().options.save();
                        })
                        .setBinding(ConcentrationFixConfig::setState, ConcentrationFixConfig::getState))
                .registerOptionOverlay(SODIUM_FULLSCREEN_RESOLUTION, builder.createIntegerOption(SODIUM_FULLSCREEN_RESOLUTION)
                        .setEnabledProvider(ConcentrationFixConfig::isResolutionEditable, FULLSCREEN));
    }

    private static FullscreenState getState() {
        if (!Minecraft.getInstance().options.fullscreen().get()) {
            return FullscreenState.OFF;
        }
        return FullscreenState.of(config().getFullscreenMode());
    }

    private static void setState(FullscreenState state) {
        Options options = Minecraft.getInstance().options;
        if (state == FullscreenState.OFF) {
            Concentration.toggleFullScreenMode(options, false);
            return;
        }
        config().setFullscreenMode(state.mode);
        Concentration.toggleFullScreenMode(options, true);
    }

    private static boolean isResolutionEditable(ConfigState state) {
        Monitor monitor = Minecraft.getInstance().getWindow().findBestMonitor();
        if (monitor == null || monitor.getModeCount() <= 0) {
            return false;
        }
        Util.OS os = Util.getPlatform();
        return (os == Util.OS.WINDOWS || os == Util.OS.OSX)
                && state.readEnumOption(FULLSCREEN, FullscreenState.class) != FullscreenState.OFF;
    }

    private static Config config() {
        return ConfigProvider.INSTANCE.ensureLoaded();
    }

}
