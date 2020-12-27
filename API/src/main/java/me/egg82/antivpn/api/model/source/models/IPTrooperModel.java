package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class IPTrooperModel implements SourceModel {
    private int code;
    private boolean bad;
    private String asn;
    @JSON(name = "country")
    private String countryCode;
    private String name;
    private String type;

    public IPTrooperModel() {
        this.code = -1;
        this.bad = false;
        this.asn = null;
        this.countryCode = null;
        this.name = null;
        this.type = null;
    }

    public int getCode() { return code; }

    public void setCode(int code) { this.code = code; }

    public boolean isBad() { return bad; }

    public void setBad(boolean bad) { this.bad = bad; }

    public String getAsn() { return asn; }

    public void setAsn(String asn) { this.asn = asn; }

    @JSON(name = "country")
    public String getCountryCode() { return countryCode; }

    @JSON(name = "country")
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPTrooperModel)) return false;
        IPTrooperModel that = (IPTrooperModel) o;
        return code == that.code && bad == that.bad && Objects.equals(asn, that.asn) && Objects.equals(countryCode, that.countryCode) && Objects.equals(name, that.name) && Objects.equals(type, that.type);
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
