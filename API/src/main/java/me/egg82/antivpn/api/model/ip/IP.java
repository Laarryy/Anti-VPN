package me.egg82.antivpn.api.model.ip;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface IP {
    /**
     * Gets the IP.
     *
     * @return the IP
     */
    @NonNull String getIp();

    /**
     * Returns the IP's cascade status.
     *
     * @return true if the cascade result returned positive (VPN/Proxy detected), false if not (no VPN/proxy detected)
     */
    boolean getCascade();

    /**
     * Sets the IP's cascade status.
     *
     * @param status true if the cascade result should be positive (VPN/Proxy detected), false if not (no VPN/proxy detected)
     */
    void setCascade(boolean status);

    /**
     * Returns the IP's consensus status.
     *
     * @return a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     */
    double getConsensus();

    /**
     * Sets the IP's consensus status.
     *
     * @param status a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     */
    void setConsensus(double status);
}
