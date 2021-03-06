package net.cyberflame.servhitdet;

import net.cyberflame.servhitdet.utils.ConfigHelper;
import net.cyberflame.servhitdet.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class LagCompensator implements Listener {

    private final Map<UUID, List<Pair<Location, Long>>> locationTimes;
    private final int historySize;
    private final int pingOffset;
    private static final int TIME_RESOLUTION = 40; //in milliseconds

    LagCompensator(ServHitDet plugin) {
        this.locationTimes = new HashMap<>();
        historySize = ConfigHelper.getOrSetDefault(20, plugin.getConfig(), "lagCompensation.historySize");
        pingOffset = ConfigHelper.getOrSetDefault(175, plugin.getConfig(), "lagCompensation.pingOffset");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    //uses linear interpolation to get the best location
    public Location getHistoryLocation(int rewindMillisecs, Player player) {
        List<Pair<Location, Long>> times = locationTimes.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if (times == null) {
            return player.getLocation();
        }
        int rewindTime = rewindMillisecs + pingOffset; //player a + avg processing time.
        for (int i = times.size() - 1; i >= 0; i--) { //loop backwards
            int elapsedTime = (int) (currentTime - times.get(i).getValue());
            if (elapsedTime >= rewindTime) {
                if (i == times.size() - 1) {
                    return times.get(i).getKey();
                }
                double nextMoveWeight = (elapsedTime - rewindTime) / (double) (elapsedTime - (currentTime - times.get(i + 1).getValue()));
                Location before = times.get(i).getKey().clone();
                Location after = times.get(i + 1).getKey();
                Vector interpolate = after.toVector().subtract(before.toVector());
                interpolate.multiply(nextMoveWeight);
                before.add(interpolate);
                return before;
            }
        }
        return player.getLocation(); //can't find a suitable position
    }

    private void processPosition(Location loc, Player p) {
        List<Pair<Location, Long>> times = locationTimes.getOrDefault(p.getUniqueId(), new ArrayList<>());
        long currTime = System.currentTimeMillis();
        if (times.size() > 0 && currTime - times.get(times.size() - 1).getValue() < TIME_RESOLUTION)
            return;
        times.add(new Pair<>(loc, currTime));
        if (times.size() > historySize) times.remove(0);
        locationTimes.put(p.getUniqueId(), times);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        processPosition(e.getTo(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        processPosition(e.getRespawnLocation(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        processPosition(e.getTo(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        processPosition(e.getPlayer().getLocation(), e.getPlayer());
    }


    public int getHistorySize() {
        return historySize;
    }

    public int getPingOffset() {
        return pingOffset;
    }
}
