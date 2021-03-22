package me.egg82.antivpn.api.model.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * An IP which contains information about various statuses it holds.
 */
public interface IP extends Serializable {
    /**
     * Gets the IP.
     *
     * @return the IP
     */
    @NotNull
    InetAddress getIP();

    /**
     * Returns the IP's {@link AlgorithmMethod} type
     *
     * @return the AlgorithmMethod used for this IP
     */
    @NotNull
    AlgorithmMethod getType();

    /**
     * Sets the IP's {@link AlgorithmMethod} type
     *
     * @param type the AlgorithmMethod to use for this IP
     */
    void setType(@NotNull AlgorithmMethod type);

    /**
     * Returns the IP's cascade status.
     *
     * <p>Returns null if cascade value has not been calculated for this IP.</p>
     *
     * @return true if the cascade result returned positive (VPN/Proxy detected), false if not (no VPN/proxy detected), or null if not calculated
     */
    @Nullable
    Boolean getCascade();

    /**
     * Sets the IP's cascade status.
     *
     * @param status true if the cascade result should be positive (VPN/Proxy detected), false if not (no VPN/proxy detected), or null if not calculated
     */
    void setCascade(@Nullable Boolean status);

    /**
     * Returns the IP's consensus status.
     *
     * <p>Returns null if consensus value has not been calculated for this IP.</p>
     *
     * @return a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy, or null if not calculated
     */
    @Nullable
    Double getConsensus();

    /**
     * Sets the IP's consensus status.
     *
     * @param status a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy, or null if not calculated
     */
    void setConsensus(@Nullable Double status);
}
