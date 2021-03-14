package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class VPNBlockerModel implements SourceModel {
    private String status = null;
    private String msg = null;
    @JSON(name = "package")
    private String packageName = null;
    @JSON(name = "remaining_requests")
    private int requestsRemaining = -1;
    @JSON(name = "ipaddress")
    private String ip = null;
    @JSON(name = "host-ip")
    private boolean host = false;
    private String org = null;

    public VPNBlockerModel() { }

    public @Nullable String getStatus() { return status; }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public @Nullable String getMsg() { return msg; }

    public void setMsg(@Nullable String msg) {
        this.msg = msg;
    }

    @JSON(name = "package")
    public @Nullable String getPackageName() { return packageName; }

    @JSON(name = "package")
    public void setPackageName(@Nullable String packageName) {
        this.packageName = packageName;
    }

    @JSON(name = "remaining_requests")
    public int getRequestsRemaining() { return requestsRemaining; }

    @JSON(name = "remaining_requests")
    public void setRequestsRemaining(int requestsRemaining) {
        this.requestsRemaining = requestsRemaining;
    }

    @JSON(name = "ipaddress")
    public @Nullable String getIp() { return ip; }

    @JSON(name = "ipaddress")
    public void setIp(@Nullable String ip) {
        this.ip = ip;
    }

    @JSON(name = "host-ip")
    public boolean isHost() { return host; }

    @JSON(name = "host-ip")
    public void setHost(boolean host) {
        this.host = host;
    }

    public @Nullable String getOrg() { return org; }

    public void setOrg(@Nullable String org) {
        this.org = org;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VPNBlockerModel)) {
            return false;
        }
        VPNBlockerModel that = (VPNBlockerModel) o;
        return requestsRemaining == that.requestsRemaining && host == that.host && Objects.equals(status, that.status) && Objects.equals(msg, that.msg) && Objects.equals(
                packageName,
                that.packageName
        ) && Objects.equals(ip, that.ip) && Objects.equals(org, that.org);
    }

    @Override
    public int hashCode() { return Objects.hash(status, msg, packageName, requestsRemaining, ip, host, org); }

    @Override
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
