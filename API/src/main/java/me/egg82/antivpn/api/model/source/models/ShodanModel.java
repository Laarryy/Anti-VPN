package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.List;
import java.util.Objects;

public class ShodanModel implements SourceModel {
    private String error;
    @JSON(name = "region_code")
    private String regionCode;
    private String ip;
    @JSON(name = "area_code")
    private String areaCode;
    @JSON(name = "country_code")
    private String countryCode;
    @JSON(name = "country_code3")
    private String countryCode3;
    @JSON(name = "country_name")
    private String country;
    @JSON(name = "postal_code")
    private String postalCode;
    @JSON(name = "dma_code")
    private String dmaCode;
    private String city;
    private double latitude;
    private double longitude;
    private String os;
    private List<Integer> ports;
    private List<String> tags;

    public ShodanModel() {
        this.error = null;
        this.regionCode = null;
        this.ip = null;
        this.areaCode = null;
        this.countryCode = null;
        this.countryCode3 = null;
        this.country = null;
        this.postalCode = null;
        this.dmaCode = null;
        this.city = null;
        this.latitude = Double.NaN;
        this.longitude = Double.NaN;
        this.os = null;
        this.ports = null;
        this.tags = null;
    }

    public String getError() { return error; }

    public void setError(String error) { this.error = error; }

    @JSON(name = "region_code")
    public String getRegionCode() { return regionCode; }

    @JSON(name = "region_code")
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    @JSON(name = "area_code")
    public String getAreaCode() { return areaCode; }

    @JSON(name = "area_code")
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }

    @JSON(name = "country_code")
    public String getCountryCode() { return countryCode; }

    @JSON(name = "country_code")
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    @JSON(name = "country_code3")
    public String getCountryCode3() { return countryCode3; }

    @JSON(name = "country_code3")
    public void setCountryCode3(String countryCode3) { this.countryCode3 = countryCode3; }

    @JSON(name = "country_name")
    public String getCountry() { return country; }

    @JSON(name = "country_name")
    public void setCountry(String country) { this.country = country; }

    @JSON(name = "postal_code")
    public String getPostalCode() { return postalCode; }

    @JSON(name = "postal_code")
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    @JSON(name = "dma_code")
    public String getDmaCode() { return dmaCode; }

    @JSON(name = "dma_code")
    public void setDmaCode(String dmaCode) { this.dmaCode = dmaCode; }

    public String getCity() { return city; }

    public void setCity(String city) { this.city = city; }

    public double getLatitude() { return latitude; }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getOs() { return os; }

    public void setOs(String os) { this.os = os; }

    public List<Integer> getPorts() { return ports; }

    public void setPorts(List<Integer> ports) { this.ports = ports; }

    public List<String> getTags() { return tags; }

    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShodanModel)) return false;
        ShodanModel that = (ShodanModel) o;
        return Double.compare(that.latitude, latitude) == 0 && Double.compare(that.longitude, longitude) == 0 && Objects.equals(error, that.error) && Objects.equals(regionCode, that.regionCode) && Objects.equals(ip, that.ip) && Objects.equals(areaCode, that.areaCode) && Objects.equals(countryCode, that.countryCode) && Objects.equals(countryCode3, that.countryCode3) && Objects.equals(country, that.country) && Objects.equals(postalCode, that.postalCode) && Objects.equals(dmaCode, that.dmaCode) && Objects.equals(city, that.city) && Objects.equals(os, that.os) && Objects.equals(ports, that.ports) && Objects.equals(tags, that.tags);
    }

    public int hashCode() { return Objects.hash(error, regionCode, ip, areaCode, countryCode, countryCode3, country, postalCode, dmaCode, city, latitude, longitude, os, ports, tags); }

    public String toString() {
        return "ShodanModel{" +
                "error='" + error + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", ip='" + ip + '\'' +
                ", areaCode='" + areaCode + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", countryCode3='" + countryCode3 + '\'' +
                ", country='" + country + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", dmaCode='" + dmaCode + '\'' +
                ", city='" + city + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", os='" + os + '\'' +
                ", ports=" + ports +
                ", tags=" + tags +
                '}';
    }
}
