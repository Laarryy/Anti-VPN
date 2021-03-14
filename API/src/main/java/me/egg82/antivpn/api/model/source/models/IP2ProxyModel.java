package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IP2ProxyModel implements SourceModel {
    private String response = null;
    private String countryCode = null;
    @JSON(name = "countryName")
    private String country = null;
    @JSON(name = "isProxy")
    private String proxy = null;

    public IP2ProxyModel() { }

    public @Nullable String getResponse() { return response; }

    public void setResponse(@Nullable String response) {
        this.response = response;
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

    @JSON(name = "isProxy")
    public @Nullable String getProxy() { return proxy; }

    @JSON(name = "isProxy")
    public void setProxy(@Nullable String proxy) {
        this.proxy = proxy;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IP2ProxyModel)) {
            return false;
        }
        IP2ProxyModel that = (IP2ProxyModel) o;
        return Objects.equals(response, that.response) && Objects.equals(countryCode, that.countryCode) && Objects.equals(country, that.country) && Objects.equals(
                proxy,
                that.proxy
        );
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
