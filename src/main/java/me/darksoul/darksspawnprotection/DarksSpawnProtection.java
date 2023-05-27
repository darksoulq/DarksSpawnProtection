package me.darksoul.darksspawnprotection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DarksSpawnProtection extends JavaPlugin implements Listener {
    private Map<UUID, Long> protectedPlayers;

    @Override
    public void onEnable() {
        // Initialize the map to store protected players and their join timestamps
        protectedPlayers = new HashMap<>();

        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Clear the map when the plugin is disabled
        protectedPlayers.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (isUnderSpawnProtection(playerId)) {
            // Player is under spawn protection
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID playerId = player.getUniqueId();

            if (isUnderSpawnProtection(playerId)) {
                // Cancel any damage caused by mobs
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent entityDamageEvent = (EntityDamageByEntityEvent) event;
                    if (entityDamageEvent.getDamager() instanceof Player) {
                        // Prevent player-to-player damage
                        event.setCancelled(true);
                    }
                } else {
                    // Allow damage from other sources (e.g., fall damage, suffocation)
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if the player recently joined or rejoined within 5 minutes
        if (!isRecentlyJoined(playerId)) {
            setSpawnProtection(playerId);
            player.sendMessage("You are now under spawn protection.");
        }
    }

    private boolean isUnderSpawnProtection(UUID playerId) {
        return protectedPlayers.containsKey(playerId);
    }

    private boolean isRecentlyJoined(UUID playerId) {
        Long joinTimestamp = protectedPlayers.get(playerId);
        if (joinTimestamp != null) {
            long currentTime = System.currentTimeMillis();
            return (currentTime - joinTimestamp) <= (300000); // 5 minutes in milliseconds
        }
        return false;
    }

    private void setSpawnProtection(UUID playerId) {
        protectedPlayers.put(playerId, System.currentTimeMillis());
        getServer().getScheduler().runTaskLater(this, () -> {
            protectedPlayers.remove(playerId);
            Player player = getServer().getPlayer(playerId);
            if (player != null) {
                player.sendMessage("Your spawn protection has expired.");
            }
        }, 600); // 5 minutes (300 seconds) converted to ticks (1 tick = 1/20th of a second)
    }
}
