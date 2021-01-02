package me.egg82.antivpn.api.model.ip;

import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An IP which contains information about various statuses it holds.
 */
public interface IP extends Serializable {
    /**
     * Gets the IP.
     *
     * @return the IP
     */
    @NonNull String getIp();

    /**
     * Returns the IP's {@link AlgorithmMethod} type
     *
     * @return the AlgorithmMethod used for this IP
     */
    @NonNull AlgorithmMethod getType();

    /**
     * Sets the IP's {@link AlgorithmMethod} type
     *
     * @param type the AlgorithmMethod to use for this IP
     */
    void setType(@NonNull AlgorithmMethod type);

    /**
     * Returns the IP's cascade status.
     *
     * @return true if the cascade result returned positive (VPN/Proxy detected), false if not (no VPN/proxy detected)
     */
    @Nullable Boolean getCascade();

    /**
     * Sets the IP's cascade status.
     *
     * @param status true if the cascade result should be positive (VPN/Proxy detected), false if not (no VPN/proxy detected)
     */
    void setCascade(@Nullable Boolean status);

    /**
     * Returns the IP's consensus status.
     *
     * @return a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     */
    @Nullable Double getConsensus();

    /**
     * Sets the IP's consensus status.
     *
     * @param status a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     */
    void setConsensus(@Nullable Double status);

    /**
     * {@inheritDoc}
     */
    boolean equals(Object o);
    /**
     * {@inheritDoc}
     */
    int hashCode();

    /**
     * {@inheritDoc}
     */
    String toString();
}
