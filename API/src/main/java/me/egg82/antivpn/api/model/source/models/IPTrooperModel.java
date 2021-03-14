package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IPTrooperModel implements SourceModel {
    private int code = -1;
    private boolean bad = false;
    private String asn = null;
    @JSON(name = "country")
    private String countryCode = null;
    private String name = null;
    private String type = null;

    public IPTrooperModel() { }

    public int getCode() { return code; }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isBad() { return bad; }

    public void setBad(boolean bad) {
        this.bad = bad;
    }

    public @Nullable String getAsn() { return asn; }

    public void setAsn(@Nullable String asn) {
        this.asn = asn;
    }

    @JSON(name = "country")
    public @Nullable String getCountryCode() { return countryCode; }

    @JSON(name = "country")
    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    public @Nullable String getName() { return name; }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable String getType() { return type; }

    public void setType(@Nullable String type) {
        this.type = type;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPTrooperModel)) {
            return false;
        }
        IPTrooperModel that = (IPTrooperModel) o;
        return code == that.code && bad == that.bad && Objects.equals(asn, that.asn) && Objects.equals(countryCode, that.countryCode) && Objects.equals(
                name,
                that.name
        ) && Objects.equals(type, that.type);
    }

    public int hashCode() { return Objects.hash(code, bad, asn, countryCode, name, type); }

    public String toString() {
        return "IPTrooperModel{" +
                "code=" + code +
                ", bad=" + bad +
                ", asn='" + asn + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
