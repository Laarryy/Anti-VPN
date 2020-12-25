package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class IPHubModel implements SourceModel {
    private String error;
    private String ip;
    private String countryCode;
    @JSON(name = "countryName")
    private String country;
    private int asn;
    private String isp;
    private int block;

    public IPHubModel() {
        this.error = null;
        this.ip = null;
        this.countryCode = null;
        this.country = null;
        this.asn = -1;
        this.isp = null;
        this.block = -1;
    }

    public String getError() { return error; }

    public void setError(String error) { this.error = error; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public String getCountryCode() { return countryCode; }

    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    @JSON(name = "countryName")
    public String getCountry() { return country; }

    @JSON(name = "countryName")
    public void setCountry(String country) { this.country = country; }

    public int getAsn() { return asn; }

    public void setAsn(int asn) { this.asn = asn; }

    public String getIsp() { return isp; }

    public void setIsp(String isp) { this.isp = isp; }

    public int getBlock() { return block; }

    public void setBlock(int block) { this.block = block; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPHubModel)) return false;
        IPHubModel that = (IPHubModel) o;
        return asn == that.asn && block == that.block && Objects.equals(error, that.error) && Objects.equals(ip, that.ip) && Objects.equals(countryCode, that.countryCode) && Objects.equals(country, that.country) && Objects.equals(isp, that.isp);
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
