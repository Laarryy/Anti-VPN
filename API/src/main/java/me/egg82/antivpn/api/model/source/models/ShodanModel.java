package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ShodanModel implements SourceModel {
    private String error = null;
    @JSON(name = "region_code")
    private String regionCode = null;
    private long ip = -1L;
    @JSON(name = "ip_str")
    private String ipString = null;
    @JSON(name = "area_code")
    private String areaCode = null;
    @JSON(name = "country_code")
    private String countryCode = null;
    @JSON(name = "country_code3")
    private String countryCode3 = null;
    @JSON(name = "country_name")
    private String country = null;
    @JSON(name = "postal_code")
    private String postalCode = null;
    @JSON(name = "dma_code")
    private String dmaCode = null;
    private String asn = null;
    private String org = null;
    private String isp = null;
    private String city = null;
    private double latitude = -1.0d;
    private double longitude = -1.0d;
    private String os = null;
    private List<Integer> ports = null;
    private List<String> tags = null;
    private List<String> hostnames = null;
    private List<String> domains = null;

    public ShodanModel() {
    }

    public @Nullable String getError() {
        return error;
    }

    public void setError(@Nullable String error) {
        this.error = error;
    }

    @JSON(name = "region_code")
    public @Nullable String getRegionCode() {
        return regionCode;
    }

    @JSON(name = "region_code")
    public void setRegionCode(@Nullable String regionCode) {
        this.regionCode = regionCode;
    }

    public long getIp() {
        return ip;
    }

    public void setIp(long ip) {
        this.ip = ip;
    }

    @JSON(name = "ip_str")
    public @Nullable String getIpString() {
        return ipString;
    }

    @JSON(name = "ip_str")
    public void setIpString(@Nullable String ipString) {
        this.ipString = ipString;
    }

    @JSON(name = "area_code")
    public @Nullable String getAreaCode() {
        return areaCode;
    }

    @JSON(name = "area_code")
    public void setAreaCode(@Nullable String areaCode) {
        this.areaCode = areaCode;
    }

    @JSON(name = "country_code")
    public @Nullable String getCountryCode() {
        return countryCode;
    }

    @JSON(name = "country_code")
    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    @JSON(name = "country_code3")
    public @Nullable String getCountryCode3() {
        return countryCode3;
    }

    @JSON(name = "country_code3")
    public void setCountryCode3(@Nullable String countryCode3) {
        this.countryCode3 = countryCode3;
    }

    @JSON(name = "country_name")
    public @Nullable String getCountry() {
        return country;
    }

    @JSON(name = "country_name")
    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    @JSON(name = "postal_code")
    public @Nullable String getPostalCode() {
        return postalCode;
    }

    @JSON(name = "postal_code")
    public void setPostalCode(@Nullable String postalCode) {
        this.postalCode = postalCode;
    }

    @JSON(name = "dma_code")
    public @Nullable String getDmaCode() {
        return dmaCode;
    }

    @JSON(name = "dma_code")
    public void setDmaCode(@Nullable String dmaCode) {
        this.dmaCode = dmaCode;
    }

    public @Nullable String getAsn() {
        return asn;
    }

    public void setAsn(@Nullable String asn) {
        this.asn = asn;
    }

    public @Nullable String getOrg() {
        return org;
    }

    public void setOrg(@Nullable String org) {
        this.org = org;
    }

    public @Nullable String getIsp() {
        return isp;
    }

    public void setIsp(@Nullable String isp) {
        this.isp = isp;
    }

    public @Nullable String getCity() {
        return city;
    }

    public void setCity(@Nullable String city) {
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public @Nullable String getOs() {
        return os;
    }

    public void setOs(@Nullable String os) {
        this.os = os;
    }

    public @Nullable List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(@Nullable List<Integer> ports) {
        this.ports = ports;
    }

    public @Nullable List<String> getTags() {
        return tags;
    }

    public void setTags(@Nullable List<String> tags) {
        this.tags = tags;
    }

    public @Nullable List<String> getHotnames() {
        return hostnames;
    }

    public void setHotnames(@Nullable List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public @Nullable List<String> getHostnames() {
        return hostnames;
    }

    public void setHostnames(@Nullable List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public @Nullable List<String> getDomains() {
        return domains;
    }

    public void setDomains(@Nullable List<String> domains) {
        this.domains = domains;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShodanModel)) {
            return false;
        }
        ShodanModel that = (ShodanModel) o;
        return ip == that.ip && Double.compare(that.latitude, latitude) == 0 && Double.compare(that.longitude, longitude) == 0 && Objects.equals(
                error,
                that.error
        ) && Objects.equals(regionCode, that.regionCode) && Objects.equals(ipString, that.ipString) && Objects.equals(areaCode, that.areaCode) && Objects.equals(
                countryCode,
                that.countryCode
        ) && Objects.equals(countryCode3, that.countryCode3) && Objects.equals(country, that.country) && Objects.equals(postalCode, that.postalCode) && Objects.equals(
                dmaCode,
                that.dmaCode
        ) && Objects.equals(asn, that.asn) && Objects.equals(org, that.org) && Objects.equals(isp, that.isp) && Objects.equals(city, that.city) && Objects.equals(
                os,
                that.os
        ) && Objects.equals(ports, that.ports) && Objects.equals(tags, that.tags) && Objects.equals(hostnames, that.hostnames) && Objects.equals(domains, that.domains);
    }

    public int hashCode() {
        return Objects.hash(
                error,
                regionCode,
                ip,
                ipString,
                areaCode,
                countryCode,
                countryCode3,
                country,
                postalCode,
                dmaCode,
                asn,
                org,
                isp,
                city,
                latitude,
                longitude,
                os,
                ports,
                tags,
                hostnames,
                domains
        );
    }

    public String toString() {
        return "ShodanModel{" +
                "error='" + error + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", ip=" + ip +
                ", ipString='" + ipString + '\'' +
                ", areaCode='" + areaCode + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", countryCode3='" + countryCode3 + '\'' +
                ", country='" + country + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", dmaCode='" + dmaCode + '\'' +
                ", asn='" + asn + '\'' +
                ", org='" + org + '\'' +
                ", isp='" + isp + '\'' +
                ", city='" + city + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", os='" + os + '\'' +
                ", ports=" + ports +
                ", tags=" + tags +
                ", hostnames=" + hostnames +
                ", domains=" + domains +
                '}';
    }
}
