package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IPTrooperModel implements SourceModel {
    private int code = -1;
    private boolean bad = false;
    private @Nullable String asn = null;
    @JSON(name = "country")
    private @Nullable String countryCode = null;
    private @Nullable String name = null;
    private @Nullable String type = null;

    public int getCode() { return code; }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isBad() { return bad; }

    public void setBad(boolean bad) {
        this.bad = bad;
    }

    @Nullable
    public String getAsn() { return asn; }

    public void setAsn(@Nullable String asn) {
        this.asn = asn;
    }

    @JSON(name = "country")
    @Nullable
    public String getCountryCode() { return countryCode; }

    @JSON(name = "country")
    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    @Nullable
    public String getName() { return name; }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getType() { return type; }

    public void setType(@Nullable String type) {
        this.type = type;
    }

    @Override
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

    @Override
    public int hashCode() { return Objects.hash(code, bad, asn, countryCode, name, type); }

    @Override
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
