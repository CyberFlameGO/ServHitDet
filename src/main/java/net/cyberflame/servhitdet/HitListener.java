package net.cyberflame.servhitdet;

import net.cyberflame.servhitdet.utils.AABB;
import net.cyberflame.servhitdet.utils.ConfigHelper;
import net.cyberflame.servhitdet.utils.Ray;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class HitListener implements Listener {

    private ServHitDet plugin;
    private LagCompensator lagCompensator;
    private final AABB playerBoundingBox;
    private final float reach;
    private final boolean debug;

    HitListener(ServHitDet plugin) {
        this.plugin = plugin;
        lagCompensator = plugin.getLagCompensator();
        double length = Math.abs(ConfigHelper.getOrSetDefault(0.9, plugin.getConfig(), "hitDetection.boxLength"));
        double height = Math.abs(ConfigHelper.getOrSetDefault(1.8, plugin.getConfig(), "hitDetection.boxHeight"));
        reach = (float) Math.abs(ConfigHelper.getOrSetDefault(4.0, plugin.getConfig(), "hitDetection.reach"));
        debug = ConfigHelper.getOrSetDefault(false, plugin.getConfig(), "hitDetection.debug");
        playerBoundingBox = new AABB(new Vector(-length/2, 0, -length/2), new Vector(length/2, height , length/2));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void cancelDefaultHit(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void detectHit(PlayerAnimationEvent e) {
        Player p = e.getPlayer();
        Location attackerLoc = p.getLocation();
        Vector attackerPos = attackerLoc.toVector().add(new Vector(0, p.isSneaking() ? 1.52625 : 1.62, 0));
        List<Entity> nearbyEntites = p.getNearbyEntities(reach + 2, reach + 2, reach + 2);
        AABB victimBox = playerBoundingBox.clone();
        Vector boxOffset = playerBoundingBox.getMin();
        Ray ray = null;
        double hitDistance = Double.MAX_VALUE;
        Player victim = null;
        int ping = 0;
        try {
            Object entityPlayer = p.getClass().getMethod("getHandle").invoke(p);
            ping = (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | NoSuchFieldException exception) {
            exception.printStackTrace();
        }

        for(Entity chkEntity : nearbyEntites) {

            if(!(chkEntity instanceof Player))
                continue;

            Player chkVictim = (Player) chkEntity;
            ray = new Ray(attackerPos, attackerLoc.getDirection());
            victimBox.translateTo(lagCompensator.getHistoryLocation(ping, chkVictim).toVector());
            victimBox.translate(boxOffset);
            Vector intersection = victimBox.intersectsRay(ray, 0, reach);

            if(debug)
                victimBox.highlight(plugin, p.getWorld(), 4.01);

            if(intersection == null)
                continue;
            double chkHitDistance = intersection.distance(attackerPos);

            if(chkHitDistance < hitDistance) {
                hitDistance = chkHitDistance;
                victim = chkVictim;
            }

        }

        if(debug && ray != null)
            ray.highlight(plugin, p.getWorld(), reach, 0.3);

        if(victim == null)
            return;

        int blockIterIterations = (int)hitDistance;
        if(blockIterIterations != 0) {
            BlockIterator iter = new BlockIterator(p.getWorld(), attackerPos, ray.getDirection(), 0, blockIterIterations);
            while(iter.hasNext()) {
                Block chkBlock = iter.next();
                if(chkBlock.getType().isSolid())
                    return;
            }
        }

        Bukkit.getPluginManager().callEvent(new ServerSidePlayerHitEvent(p, victim));

    }
}
