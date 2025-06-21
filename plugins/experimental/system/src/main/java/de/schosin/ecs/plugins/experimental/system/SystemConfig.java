package de.schosin.ecs.plugins.experimental.system;

import java.util.concurrent.ExecutorService;

import de.schosin.ecs.api.Plugin.PluginConfig;

public record SystemConfig(ExecutorService executor) implements PluginConfig {
}
