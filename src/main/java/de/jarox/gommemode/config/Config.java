package de.jarox.gommemode.config;

import lombok.Getter;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.text.Text;

public class Config {
    @Getter
    static ConfigBuilder configBuilder = ConfigBuilder.create()
            .setTitle(Text.of("GommeMode"));

    static ConfigCategory generalCategory = configBuilder.getOrCreateCategory(Text.of("general"));

}
