package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class IPQualityScoreModel implements SourceModel {
    private boolean success;
    private String message;
    @JSON(name = "request_id")
    private String requestId;
    @JSON(name = "ISP")
    private String isp;
    private String organization;
    @JSON(name = "ASN")
    private int asn;
    private String host;
    @JSON(name = "country_code")
    private String countryCode;
    private String city;
    private String region;
    @JSON(name = "is_crawler")
    private boolean crawler;
    @JSON(name = "connection_type")
    private String connectionType;
    private double latitude;
    private double longitude;
    private String timezone;
    private boolean proxy;
    private boolean vpn;
    @JSON(name = "active_vpn")
    private boolean activeVpn;
    private boolean tor;
    @JSON(name = "active_tor")
    private boolean activeTor;
    @JSON(name = "recent_abuse")
    private boolean recentAbuse;
    @JSON(name = "bot_status")
    private boolean bot;
    @JSON(name = "abuse_velocity")
    private String abuseVelocity;
    @JSON(name = "fraud_score")
    private int fraudScore;

    public IPQualityScoreModel() {
        this.success = false;
        this.message = null;
        this.requestId = null;
        this.isp = null;
        this.organization = null;
        this.asn = -1;
        this.host = null;
        this.countryCode = null;
        this.city = null;
        this.region = null;
        this.crawler = false;
        this.connectionType = null;
        this.latitude = Double.NaN;
        this.longitude = Double.NaN;
        this.timezone = null;
        this.proxy = false;
        this.vpn = false;
        this.activeVpn = false;
        this.tor = false;
        this.activeTor = false;
        this.recentAbuse = false;
        this.bot = false;
        this.abuseVelocity = null;
        this.fraudScore = -1;
    }

    public boolean isSuccess() { return success; }

    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    @JSON(name = "request_id")
    public String getRequestId() { return requestId; }

    @JSON(name = "request_id")
    public void setRequestId(String requestId) { this.requestId = requestId; }

    @JSON(name = "ISP")
    public String getIsp() { return isp; }

    @JSON(name = "ISP")
    public void setIsp(String isp) { this.isp = isp; }

    public String getOrganization() { return organization; }

    public void setOrganization(String organization) { this.organization = organization; }

    @JSON(name = "ASN")
    public int getAsn() { return asn; }

    @JSON(name = "ASN")
    public void setAsn(int asn) { this.asn = asn; }

    public String getHost() { return host; }

    public void setHost(String host) { this.host = host; }

    @JSON(name = "country_code")
    public String getCountryCode() { return countryCode; }

    @JSON(name = "country_code")
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCity() { return city; }

    public void setCity(String city) { this.city = city; }

    public String getRegion() { return region; }

    public void setRegion(String region) { this.region = region; }

    @JSON(name = "is_crawler")
    public boolean isCrawler() { return crawler; }

    @JSON(name = "is_crawler")
    public void setCrawler(boolean crawler) { this.crawler = crawler; }

    @JSON(name = "connection_type")
    public String getConnectionType() { return connectionType; }

    @JSON(name = "connection_type")
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public double getLatitude() { return latitude; }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getTimezone() { return timezone; }

    public void setTimezone(String timezone) { this.timezone = timezone; }

    public boolean isProxy() { return proxy; }

    public void setProxy(boolean proxy) { this.proxy = proxy; }

    public boolean isVpn() { return vpn; }

    public void setVpn(boolean vpn) { this.vpn = vpn; }

    @JSON(name = "active_vpn")
    public boolean isActiveVpn() { return activeVpn; }

    @JSON(name = "active_vpn")
    public void setActiveVpn(boolean activeVpn) { this.activeVpn = activeVpn; }

    public boolean isTor() { return tor; }

    public void setTor(boolean tor) { this.tor = tor; }

    @JSON(name = "active_tor")
    public boolean isActiveTor() { return activeTor; }

    @JSON(name = "active_tor")
    public void setActiveTor(boolean activeTor) { this.activeTor = activeTor; }

    @JSON(name = "recent_abuse")
    public boolean isRecentAbuse() { return recentAbuse; }

    @JSON(name = "recent_abuse")
    public void setRecentAbuse(boolean recentAbuse) { this.recentAbuse = recentAbuse; }

    @JSON(name = "bot_status")
    public boolean isBot() { return bot; }

    @JSON(name = "bot_status")
    public void setBot(boolean bot) { this.bot = bot; }

    @JSON(name = "abuse_velocity")
    public String getAbuseVelocity() { return abuseVelocity; }

    @JSON(name = "abuse_velocity")
    public void setAbuseVelocity(String abuseVelocity) { this.abuseVelocity = abuseVelocity; }

    @JSON(name = "fraud_score")
    public int getFraudScore() { return fraudScore; }

    @JSON(name = "fraud_score")
    public void setFraudScore(int fraudScore) { this.fraudScore = fraudScore; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPQualityScoreModel)) return false;
        IPQualityScoreModel that = (IPQualityScoreModel) o;
        return success == that.success && asn == that.asn && crawler == that.crawler && Double.compare(that.latitude, latitude) == 0 && Double.compare(that.longitude, longitude) == 0 && proxy == that.proxy && vpn == that.vpn && activeVpn == that.activeVpn && tor == that.tor && activeTor == that.activeTor && recentAbuse == that.recentAbuse && bot == that.bot && fraudScore == that.fraudScore && Objects.equals(message, that.message) && Objects.equals(requestId, that.requestId) && Objects.equals(isp, that.isp) && Objects.equals(organization, that.organization) && Objects.equals(host, that.host) && Objects.equals(countryCode, that.countryCode) && Objects.equals(city, that.city) && Objects.equals(region, that.region) && Objects.equals(connectionType, that.connectionType) && Objects.equals(timezone, that.timezone) && Objects.equals(abuseVelocity, that.abuseVelocity);
    }

    public int hashCode() { return Objects.hash(success, message, requestId, isp, organization, asn, host, countryCode, city, region, crawler, connectionType, latitude, longitude, timezone, proxy, vpn, activeVpn, tor, activeTor, recentAbuse, bot, abuseVelocity, fraudScore); }

    public String toString() {
        return "IPQualityScoreModel{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", isp='" + isp + '\'' +
                ", organization='" + organization + '\'' +
                ", asn=" + asn +
                ", host='" + host + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", city='" + city + '\'' +
                ", region='" + region + '\'' +
                ", crawler=" + crawler +
                ", connectionType='" + connectionType + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timezone='" + timezone + '\'' +
                ", proxy=" + proxy +
                ", vpn=" + vpn +
                ", activeVpn=" + activeVpn +
                ", tor=" + tor +
                ", activeTor=" + activeTor +
                ", recentAbuse=" + recentAbuse +
                ", bot=" + bot +
                ", abuseVelocity='" + abuseVelocity + '\'' +
                ", fraudScore=" + fraudScore +
                '}';
    }
}
