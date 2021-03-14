package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IPHubModel implements SourceModel {
    private String error = null;
    private String ip = null;
    private String countryCode = null;
    @JSON(name = "countryName")
    private String country = null;
    private int asn = -1;
    private String isp = null;
    private int block = -1;

    public IPHubModel() { }

    public @Nullable String getError() { return error; }

    public void setError(@Nullable String error) {
        this.error = error;
    }

    public @Nullable String getIp() { return ip; }

    public void setIp(@Nullable String ip) {
        this.ip = ip;
    }

    public @Nullable String getCountryCode() { return countryCode; }

    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    @JSON(name = "countryName")
    public @Nullable String getCountry() { return country; }

    @JSON(name = "countryName")
    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    public int getAsn() { return asn; }

    public void setAsn(int asn) {
        this.asn = asn;
    }

    public @Nullable String getIsp() { return isp; }

    public void setIsp(@Nullable String isp) {
        this.isp = isp;
    }

    public int getBlock() { return block; }

    public void setBlock(int block) {
        this.block = block;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPHubModel)) {
            return false;
        }
        IPHubModel that = (IPHubModel) o;
        return asn == that.asn && block == that.block && Objects.equals(error, that.error) && Objects.equals(ip, that.ip) && Objects.equals(
                countryCode,
                that.countryCode
        ) && Objects.equals(country, that.country) && Objects.equals(isp, that.isp);
    }

    public int hashCode() { return Objects.hash(error, ip, countryCode, country, asn, isp, block); }

    public String toString() {
        return "IPHubModel{" +
                "error='" + error + '\'' +
                ", ip='" + ip + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", country='" + country + '\'' +
                ", asn=" + asn +
                ", isp='" + isp + '\'' +
                ", block=" + block +
                '}';
    }
}
