package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class IP2ProxyModel implements SourceModel {
    private String response;
    private String countryCode;
    @JSON(name = "countryName")
    private String country;
    @JSON(name = "isProxy")
    private String proxy;

    public IP2ProxyModel() {
        this.response = null;
        this.countryCode = null;
        this.country = null;
        this.proxy = null;
    }

    public String getResponse() { return response; }

    public void setResponse(String response) { this.response = response; }

    public String getCountryCode() { return countryCode; }

    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    @JSON(name = "countryName")
    public String getCountry() { return country; }

    @JSON(name = "countryName")
    public void setCountry(String country) { this.country = country; }

    @JSON(name = "isProxy")
    public String getProxy() { return proxy; }

    @JSON(name = "isProxy")
    public void setProxy(String proxy) { this.proxy = proxy; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IP2ProxyModel)) return false;
        IP2ProxyModel that = (IP2ProxyModel) o;
        return Objects.equals(response, that.response) && Objects.equals(countryCode, that.countryCode) && Objects.equals(country, that.country) && Objects.equals(proxy, that.proxy);
    }

    public int hashCode() { return Objects.hash(response, countryCode, country, proxy); }

    public String toString() {
        return "IP2ProxyModel{" +
                "response='" + response + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", country='" + country + '\'' +
                ", proxy='" + proxy + '\'' +
                '}';
    }
}
