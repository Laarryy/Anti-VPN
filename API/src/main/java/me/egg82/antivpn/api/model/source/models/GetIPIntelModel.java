package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.util.Objects;

public class GetIPIntelModel implements SourceModel {
    private String status;
    private String result;
    private String message;
    @JSON(name = "queryIP")
    private String ip;
    @JSON(name = "queryFlags")
    private String flags;
    @JSON(name = "queryFormat")
    private String format;
    private String contact;

    public GetIPIntelModel() {
        this.status = null;
        this.result = null;
        this.message = null;
        this.ip = null;
        this.flags = null;
        this.format = null;
        this.contact = null;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getResult() { return result; }

    public void setResult(String result) { this.result = result; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    @JSON(name = "queryIP")
    public String getIp() { return ip; }

    @JSON(name = "queryIP")
    public void setIp(String ip) { this.ip = ip; }

    @JSON(name = "queryFlags")
    public String getFlags() { return flags; }

    @JSON(name = "queryFlags")
    public void setFlags(String flags) { this.flags = flags; }

    @JSON(name = "queryFormat")
    public String getFormat() { return format; }

    @JSON(name = "queryFormat")
    public void setFormat(String format) { this.format = format; }

    public String getContact() { return contact; }

    public void setContact(String contact) { this.contact = contact; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetIPIntelModel)) return false;
        GetIPIntelModel that = (GetIPIntelModel) o;
        return Objects.equals(status, that.status) && Objects.equals(result, that.result) && Objects.equals(message, that.message) && Objects.equals(ip, that.ip) && Objects.equals(flags, that.flags) && Objects.equals(format, that.format) && Objects.equals(contact, that.contact);
    }

    public int hashCode() { return Objects.hash(status, result, message, ip, flags, format, contact); }

    public String toString() {
        return "GetIPIntelModel{" +
                "status='" + status + '\'' +
                ", result='" + result + '\'' +
                ", message='" + message + '\'' +
                ", ip='" + ip + '\'' +
                ", flags='" + flags + '\'' +
                ", format='" + format + '\'' +
                ", contact='" + contact + '\'' +
                '}';
    }
}
