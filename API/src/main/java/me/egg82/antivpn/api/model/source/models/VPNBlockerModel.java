package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class VPNBlockerModel implements SourceModel {
    private String status;
    private String msg;
    @JSON(name = "package")
    private String packageName;
    @JSON(name = "remaining_requests")
    private int requestsRemaining;
    @JSON(name = "ipaddress")
    private String ip;
    @JSON(name = "host-ip")
    private boolean host;
    private String org;

    public VPNBlockerModel() {
        this.status = null;
        this.msg = null;
        this.packageName = null;
        this.requestsRemaining = -1;
        this.ip = null;
        this.host = false;
        this.org = null;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getMsg() { return msg; }

    public void setMsg(String msg) { this.msg = msg; }

    @JSON(name = "package")
    public String getPackageName() { return packageName; }

    @JSON(name = "package")
    public void setPackageName(String packageName) { this.packageName = packageName; }

    @JSON(name = "remaining_requests")
    public int getRequestsRemaining() { return requestsRemaining; }

    @JSON(name = "remaining_requests")
    public void setRequestsRemaining(int requestsRemaining) { this.requestsRemaining = requestsRemaining; }

    @JSON(name = "ipaddress")
    public String getIp() { return ip; }

    @JSON(name = "ipaddress")
    public void setIp(String ip) { this.ip = ip; }

    @JSON(name = "host-ip")
    public boolean isHost() { return host; }

    @JSON(name = "host-ip")
    public void setHost(boolean host) { this.host = host; }

    public String getOrg() { return org; }

    public void setOrg(String org) { this.org = org; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VPNBlockerModel)) return false;
        VPNBlockerModel that = (VPNBlockerModel) o;
        return requestsRemaining == that.requestsRemaining && host == that.host && Objects.equals(status, that.status) && Objects.equals(msg, that.msg) && Objects.equals(packageName, that.packageName) && Objects.equals(ip, that.ip) && Objects.equals(org, that.org);
    }

    public int hashCode() { return Objects.hash(status, msg, packageName, requestsRemaining, ip, host, org); }

    public String toString() {
        return "VPNBlockerModel{" +
                "status='" + status + '\'' +
                ", msg='" + msg + '\'' +
                ", packageName='" + packageName + '\'' +
                ", requestsRemaining=" + requestsRemaining +
                ", ip='" + ip + '\'' +
                ", host=" + host +
                ", org='" + org + '\'' +
                '}';
    }
}
