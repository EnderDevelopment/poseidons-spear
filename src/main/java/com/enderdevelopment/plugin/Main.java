package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private NamespacedKey spearKey;
    private final Map<UUID, Boolean> sneakingThrows = new HashMap<>();

    @Override
    public void onEnable() {
        spearKey = new NamespacedKey(this, "poseidons_spear");
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("poseidonspear") != null) {
            getCommand("poseidonspear").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                ItemStack spear = new ItemStack(Material.TRIDENT);
                ItemMeta meta = spear.getItemMeta();
                if (meta == null) return true;
                meta.displayName(Component.text("Poseidon's Spear", NamedTextColor.AQUA));
                meta.getPersistentDataContainer().set(spearKey, PersistentDataType.BYTE, (byte) 1);
                spear.setItemMeta(meta);
                player.getInventory().addItem(spear).values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage(Component.text("The sea answers your call.", NamedTextColor.AQUA));
                return true;
            });
        }
    }

    private boolean isSpear(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte marked = meta.getPersistentDataContainer().get(spearKey, PersistentDataType.BYTE);
        return marked != null && marked == (byte) 1;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !isSpear(trident.getItem())) return;
        if (trident.getShooter() instanceof Player shooter) {
            sneakingThrows.put(trident.getUniqueId(), shooter.isSneaking());
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !isSpear(trident.getItem())) return;
        Entity hit = event.getHitEntity();
        Location strike = hit != null ? hit.getLocation() : trident.getLocation();
        strike.getWorld().strikeLightning(strike);
        boolean sneakingThrow = sneakingThrows.remove(trident.getUniqueId()) == Boolean.TRUE;
        if (sneakingThrow && hit instanceof Player target) createWaterCage(target);
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !isSpear(player.getInventory().getItemInMainHand())) return;
        event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
    }

    private void createWaterCage(Player target) {
        Location center = target.getLocation().getBlock().getLocation();
        List<Block> changed = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean wall = Math.abs(x) == 1 || Math.abs(z) == 1 || y == 2;
                    if (!wall) continue;
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.WATER, false);
                        changed.add(block);
                    }
                }
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Block block : changed) {
                if (block.getType() == Material.WATER) block.setType(Material.AIR, false);
            }
        }, 100L);
        if (target.isDead()) task.cancel();
        target.sendMessage(Component.text("A water cage surrounds you!", NamedTextColor.AQUA));
    }
}
