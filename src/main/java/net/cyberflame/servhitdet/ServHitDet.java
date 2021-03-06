package net.cyberflame.servhitdet;

import org.bukkit.plugin.java.JavaPlugin;

public class ServHitDet extends JavaPlugin {

    private LagCompensator lagCompensator;

    @Override
    public void onEnable() {
        lagCompensator = new LagCompensator(this);
        new HitListener(this);
        saveConfig();
        getLogger().info("Server-side Hit detection enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Server-side Hit detection disabled");
    }

    public LagCompensator getLagCompensator() {
        return lagCompensator;
    }
}
