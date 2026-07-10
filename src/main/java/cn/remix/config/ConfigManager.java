package cn.remix.config;

import cn.remix.config.impl.ModuleConfig;
import cn.remix.util.IMinecraft;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConfigManager implements IMinecraft {
    @Getter
    private final List<Config> configs = new ArrayList<>();

    public ConfigManager() {
        instance.getEventManager().register(this);

        addConfigs(
                new ModuleConfig()
        );

        loadAll();
    }

    public Config getConfig(final String name) {
        return this.configs.stream()
                .filter(config -> config.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void addConfigs(final Config... configsArray) {
        configs.addAll(Arrays.asList(configsArray));
    }

    public void saveAll() {
        configs.forEach(Config::save);
    }

    public void loadAll() {
        configs.forEach(Config::load);
    }
}