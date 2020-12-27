package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class TeohModel implements SourceModel {
    private String message;
    private String ip;
    private String organization;
    private String asn;
    private String type;
    private String risk;
    @JSON(name = "is_hosting")
    private boolean hosting;
    @JSON(name = "vpn_or_proxy")
    private String vpnOrProxy;

    public TeohModel() {
        this.message = null;
        this.ip = null;
        this.organization = null;
        this.asn = null;
        this.type = null;
        this.risk = null;
        this.hosting = false;
        this.vpnOrProxy = null;
    }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public String getOrganization() { return organization; }

    public void setOrganization(String organization) { this.organization = organization; }

    public String getAsn() { return asn; }

    public void setAsn(String asn) { this.asn = asn; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public String getRisk() { return risk; }

    public void setRisk(String risk) { this.risk = risk; }

    @JSON(name = "is_hosting")
    public boolean isHosting() { return hosting; }

    @JSON(name = "is_hosting")
    public void setHosting(boolean hosting) { this.hosting = hosting; }

    @JSON(name = "vpn_or_proxy")
    public String getVpnOrProxy() { return vpnOrProxy; }

    @JSON(name = "vpn_or_proxy")
    public void setVpnOrProxy(String vpnOrProxy) { this.vpnOrProxy = vpnOrProxy; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeohModel)) return false;
        TeohModel teohModel = (TeohModel) o;
        return hosting == teohModel.hosting && Objects.equals(message, teohModel.message) && Objects.equals(ip, teohModel.ip) && Objects.equals(organization, teohModel.organization) && Objects.equals(asn, teohModel.asn) && Objects.equals(type, teohModel.type) && Objects.equals(risk, teohModel.risk) && Objects.equals(vpnOrProxy, teohModel.vpnOrProxy);
    }

    public int hashCode() { return Objects.hash(message, ip, organization, asn, type, risk, hosting, vpnOrProxy); }

    public String toString() {
        return "TeohModel{" +
                "message='" + message + '\'' +
                ", ip='" + ip + '\'' +
                ", organization='" + organization + '\'' +
                ", asn='" + asn + '\'' +
                ", type='" + type + '\'' +
                ", risk='" + risk + '\'' +
                ", hosting=" + hosting +
                ", vpnOrProxy='" + vpnOrProxy + '\'' +
                '}';
    }
}
