package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IPHubModel implements SourceModel {
    private @Nullable String error = null;
    private @Nullable String ip = null;
    private @Nullable String countryCode = null;
    @JSON(name = "countryName")
    private @Nullable String country = null;
    private int asn = -1;
    private @Nullable String isp = null;
    private int block = -1;

    @Nullable
    public String getError() { return error; }

    public void setError(@Nullable String error) {
        this.error = error;
    }

    @Nullable
    public String getIp() { return ip; }

    public void setIp(@Nullable String ip) {
        this.ip = ip;
    }

    @Nullable
    public String getCountryCode() { return countryCode; }

    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    @JSON(name = "countryName")
    @Nullable
    public String getCountry() { return country; }

    @JSON(name = "countryName")
    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    public int getAsn() { return asn; }

    public void setAsn(int asn) {
        this.asn = asn;
    }

    @Nullable
    public String getIsp() { return isp; }

    public void setIsp(@Nullable String isp) {
        this.isp = isp;
    }

    public int getBlock() { return block; }

    public void setBlock(int block) {
        this.block = block;
    }

    @Override
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

    @Override
    public int hashCode() { return Objects.hash(error, ip, countryCode, country, asn, isp, block); }

    @Override
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
