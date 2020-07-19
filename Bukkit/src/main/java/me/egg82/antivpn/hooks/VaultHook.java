package me.egg82.antivpn.hooks;

import me.egg82.antivpn.utils.LogUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class VaultHook extends Permission {

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public Permission permission;

    public VaultHook(final Plugin plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Bukkit.getPluginManager().registerEvents(new VaultListener(), plugin);
            setupVaultPermissions();
        } else {
            return;
        }
    }

    private boolean setupVaultPermissions() {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null || Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                final RegisteredServiceProvider<Permission> permissionProvider =
                        Bukkit.getServicesManager().getRegistration(Permission.class);
                if (permissionProvider != null) {
                    logger.info("Found Permissions Provider");
                    permission = permissionProvider.getProvider();
                } else {
                    logger.info("Vault permissions not detected.");
                    permission = null;
                }
            } else {
                logger.info("Vault was not found.");
                permission = null;
            }
            return (permission != null);
        }

    @Override
    public String getName() {
        return Bukkit.getName();
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Vault");
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return false;
    }

    @Override
    public boolean playerHas(String s, String s1, String s2) {
        return true;
    }

    @Override
    public boolean playerAdd(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean playerRemove(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean groupHas(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean groupAdd(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean groupRemove(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean playerInGroup(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean playerAddGroup(String s, String s1, String s2) {
        return false;
    }

    @Override
    public boolean playerRemoveGroup(String s, String s1, String s2) {
        return false;
    }

    @Override
    public String[] getPlayerGroups(String s, String s1) {
        return new String[0];
    }

    @Override
    public String getPrimaryGroup(String s, String s1) {
        return null;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public boolean hasGroupSupport() {
        return false;
    }



    private class VaultListener implements Listener {
            @EventHandler
            private void vaultEnabled(PluginEnableEvent event) {
                if (event.getPlugin() != null && event.getPlugin().getName().equals("Vault")) {
                    setupVaultPermissions();
                }
            }

            @EventHandler
            private void vaultDisabled(PluginDisableEvent event) {
                if (event.getPlugin() != null && event.getPlugin().getName().equals("Vault")) {
                    logger.info("Vault permissions disabled");
                    permission = null;
                }
            }
        }

        public boolean hasPermission() {
            return permission != null;
        }

        public Permission getPermission() {
            return permission;
        }
    }

