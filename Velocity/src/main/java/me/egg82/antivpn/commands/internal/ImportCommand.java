package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import java.util.Optional;
import java.util.Set;
import me.egg82.antivpn.core.IPResult;
import me.egg82.antivpn.core.PlayerResult;
import me.egg82.antivpn.core.RawMCLeaksResult;
import me.egg82.antivpn.core.RawVPNResult;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.storage.StorageException;
import me.egg82.antivpn.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String masterName;
    private final String slaveName;
    private final String batchMax;

    public ImportCommand(CommandIssuer issuer, String masterName, String slaveName, String batchMax) {
        this.issuer = issuer;
        this.masterName = masterName;
        this.slaveName = slaveName;
        this.batchMax = batchMax;
    }

    public void run() {
        if (masterName == null || masterName.isEmpty()) {
            issuer.sendError(Message.IMPORT__NO_MASTER);
            return;
        }
        if (slaveName == null || slaveName.isEmpty()) {
            issuer.sendError(Message.IMPORT__NO_SLAVE);
            return;
        }

        if (masterName.equalsIgnoreCase(slaveName)) {
            issuer.sendError(Message.IMPORT__SAME_STORAGE);
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            issuer.sendError(Message.ERROR__INTERNAL);
            return;
        }

        int max = batchMax == null ? 50 : Integer.parseInt(batchMax);

        int masterIndex = -1;
        int slaveIndex = -1;
        for (int i = 0; i < cachedConfig.get().getStorage().size(); i++) {
            Storage storage = cachedConfig.get().getStorage().get(i);
            if (masterIndex == -1 && masterName.equalsIgnoreCase(storage.getClass().getSimpleName())) {
                masterIndex = i;
            } else if (slaveIndex == -1 && slaveName.equalsIgnoreCase(storage.getClass().getSimpleName())) { // The elseif helps to ensure we aren't trying to import from master to master
                slaveIndex = i;
            }
        }

        if (masterIndex == -1) {
            issuer.sendError(Message.IMPORT__NO_MASTER);
            return;
        }
        if (slaveIndex == -1) {
            issuer.sendError(Message.IMPORT__NO_SLAVE);
            return;
        }

        issuer.sendInfo(Message.IMPORT__BEGIN);

        Storage master = cachedConfig.get().getStorage().get(masterIndex);
        Storage slave = cachedConfig.get().getStorage().get(slaveIndex);

        issuer.sendInfo(Message.IMPORT__IPS, "{id}", "0");
        long start = 1L;
        Set<IPResult> ips;
        do {
            try {
                ips = master.dumpIPs(start, max);
                slave.loadIPs(ips, start == 1L);
            } catch (StorageException ex) {
                logger.error("Could not import IPs.", ex);
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }
            issuer.sendInfo(Message.IMPORT__IPS, "{id}", String.valueOf(start + ips.size()));
            start += max;
        } while (ips.size() == max);

        issuer.sendInfo(Message.IMPORT__PLAYERS, "{id}", "0");
        start = 1L;
        Set<PlayerResult> players;
        do {
            try {
                players = master.dumpPlayers(start, max);
                slave.loadPlayers(players, start == 1L);
            } catch (StorageException ex) {
                logger.error("Could not import players.", ex);
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }
            issuer.sendInfo(Message.IMPORT__PLAYERS, "{id}", String.valueOf(start + players.size()));
            start += max;
        } while (players.size() == max);

        issuer.sendInfo(Message.IMPORT__VPNS, "{id}", "0");
        start = 1L;
        Set<RawVPNResult> vpns;
        do {
            try {
                vpns = master.dumpVPNValues(start, max);
                slave.loadVPNValues(vpns, start == 1L);
            } catch (StorageException ex) {
                logger.error("Could not import VPN values.", ex);
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }
            issuer.sendInfo(Message.IMPORT__VPNS, "{id}", String.valueOf(start + vpns.size()));
            start += max;
        } while (vpns.size() == max);

        issuer.sendInfo(Message.IMPORT__MCLEAKS, "{id}", "0");
        start = 1L;
        Set<RawMCLeaksResult> mcleaks;
        do {
            try {
                mcleaks = master.dumpMCLeaksValues(start, max);
                slave.loadMCLeaksValues(mcleaks, start == 1L);
            } catch (StorageException ex) {
                logger.error("Could not import MCLeaks values.", ex);
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }
            issuer.sendInfo(Message.IMPORT__MCLEAKS, "{id}", String.valueOf(start + mcleaks.size()));
            start += max;
        } while (mcleaks.size() == max);

        issuer.sendInfo(Message.IMPORT__END);
    }
}
