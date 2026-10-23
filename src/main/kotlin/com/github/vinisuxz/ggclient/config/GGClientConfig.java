package com.github.vinisuxz.ggclient.config;

import com.github.vinisuxz.ggclient.GGClient;
import com.github.vinisuxz.ggclient.config.categories.MainCategory;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.Config;
import io.github.moulberry.moulconfig.annotations.Category;

public class GGClientConfig extends Config {

    @Override
    public String getTitle() {
        return GGClient.MOD_NAME + " " + GGClient.getVersion() + " by §cbrainage04§r, config by §5Moulberry §rand §5nea89";
    }

    @Override
    public void saveNow() {
        GGClient.configManager.save();
    }

    @Expose
    @Category(name = "Main Category", desc = "This is the main category.")
    public MainCategory mainCategory = new MainCategory();
}
