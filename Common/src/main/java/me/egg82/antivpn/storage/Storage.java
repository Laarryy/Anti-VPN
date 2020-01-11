package me.egg82.antivpn.storage;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.egg82.antivpn.core.*;

public interface Storage {
    void close();
    boolean isClosed();

    Set<VPNResult> getVPNQueue() throws StorageException;
    VPNResult getVPNByIP(String ip) throws StorageException;
    default PostVPNResult postVPN(String ip, boolean cascade) throws StorageException { return postVPN(ip, Optional.of(cascade), Optional.empty()); }
    default PostVPNResult postVPN(String ip, double consensus) throws StorageException { return postVPN(ip, Optional.empty(), Optional.of(consensus)); }
    PostVPNResult postVPN(String ip, Optional<Boolean> cascade, Optional<Double> consensus) throws StorageException;

    Set<MCLeaksResult> getMCLeaksQueue() throws StorageException;
    MCLeaksResult getMCLeaksByPlayer(UUID playerID) throws StorageException;
    PostMCLeaksResult postMCLeaks(UUID playerID, boolean value) throws StorageException;

    void setIPRaw(long longIPID, String ip) throws StorageException;
    void setPlayerRaw(long longPlayerID, UUID playerID) throws StorageException;
    void postVPNRaw(long id, long longIPID, Optional<Boolean> cascade, Optional<Double> consensus, long created) throws StorageException;
    void postMCLeaksRaw(long id, long longPlayerID, boolean value, long created) throws StorageException;

    long getLongPlayerID(UUID playerID);
    long getLongIPID(String ip);

    Set<IPResult> dumpIPs(long begin, int size) throws StorageException;
    void loadIPs(Set<IPResult> ips, boolean truncate) throws StorageException;

    Set<PlayerResult> dumpPlayers(long begin, int size) throws StorageException;
    void loadPlayers(Set<PlayerResult> players, boolean truncate) throws StorageException;

    Set<RawVPNResult> dumpVPNValues(long begin, int size) throws StorageException;
    void loadVPNValues(Set<RawVPNResult> values, boolean truncate) throws StorageException;

    Set<RawMCLeaksResult> dumpMCLeaksValues(long begin, int size) throws StorageException;
    void loadMCLeaksValues(Set<RawMCLeaksResult> values, boolean truncate) throws StorageException;
}
