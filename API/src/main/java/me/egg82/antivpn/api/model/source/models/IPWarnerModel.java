package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class IPWarnerModel implements SourceModel {
    private String error;
    @JSON(name = "goodIp")
    private boolean good;
    private String ip;
    private String hostname;
    @JSON(name = "countryIso")
    private String countryCode;
    @JSON(name = "countryName")
    private String country;
    private String continentCode;
    @JSON(name = "continentName")
    private String continent;
    private String postalCode;
    @JSON(name = "cityName")
    private String city;
    private long responseTime;
    @JSON(name = "asnId")
    private int asn;
    @JSON(name = "asnName")
    private String name;
    private String organization;
    private String isp;
    private String plan;

    public IPWarnerModel() {
        this.error = null;
        this.good = false;
        this.ip = null;
        this.hostname = null;
        this.countryCode = null;
        this.country = null;
        this.continentCode = null;
        this.continent = null;
        this.postalCode = null;
        this.city = null;
        this.responseTime = -1L;
        this.asn = -1;
        this.name = null;
        this.organization = null;
        this.isp = null;
        this.plan = null;
    }

    public String getError() { return error; }

    public void setError(String error) { this.error = error; }

    public boolean isGood() { return good; }

    public void setGood(boolean good) { this.good = good; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public String getHostname() { return hostname; }

    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getCountryCode() { return countryCode; }

    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCountry() { return country; }

    public void setCountry(String country) { this.country = country; }

    public String getContinentCode() { return continentCode; }

    public void setContinentCode(String continentCode) { this.continentCode = continentCode; }

    public String getContinent() { return continent; }

    public void setContinent(String continent) { this.continent = continent; }

    public String getPostalCode() { return postalCode; }

    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCity() { return city; }

    public void setCity(String city) { this.city = city; }

    public long getResponseTime() { return responseTime; }

    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

    public int getAsn() { return asn; }

    public void setAsn(int asn) { this.asn = asn; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getOrganization() { return organization; }

    public void setOrganization(String organization) { this.organization = organization; }

    public String getIsp() { return isp; }

    public void setIsp(String isp) { this.isp = isp; }

    public String getPlan() { return plan; }

    public void setPlan(String plan) { this.plan = plan; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPWarnerModel)) return false;
        IPWarnerModel that = (IPWarnerModel) o;
        return good == that.good && responseTime == that.responseTime && asn == that.asn && Objects.equals(error, that.error) && Objects.equals(ip, that.ip) && Objects.equals(hostname, that.hostname) && Objects.equals(countryCode, that.countryCode) && Objects.equals(country, that.country) && Objects.equals(continentCode, that.continentCode) && Objects.equals(continent, that.continent) && Objects.equals(postalCode, that.postalCode) && Objects.equals(city, that.city) && Objects.equals(name, that.name) && Objects.equals(organization, that.organization) && Objects.equals(isp, that.isp) && Objects.equals(plan, that.plan);
    }

    public int hashCode() { return Objects.hash(error, good, ip, hostname, countryCode, country, continentCode, continent, postalCode, city, responseTime, asn, name, organization, isp, plan); }

    public String toString() {
        return "IPWarnerModel{" +
                "error='" + error + '\'' +
                ", good=" + good +
                ", ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", country='" + country + '\'' +
                ", continentCode='" + continentCode + '\'' +
                ", continent='" + continent + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", city='" + city + '\'' +
                ", responseTime=" + responseTime +
                ", asn=" + asn +
                ", name='" + name + '\'' +
                ", organization='" + organization + '\'' +
                ", isp='" + isp + '\'' +
                ", plan='" + plan + '\'' +
                '}';
    }
}
