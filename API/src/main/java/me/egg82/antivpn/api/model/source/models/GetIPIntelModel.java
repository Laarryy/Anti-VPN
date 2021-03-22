package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GetIPIntelModel implements SourceModel {
    private @Nullable String status = null;
    private @Nullable String result = null;
    private @Nullable String message = null;
    @JSON(name = "queryIP")
    private @Nullable String ip = null;
    @JSON(name = "queryFlags")
    private @Nullable String flags = null;
    @JSON(name = "queryFormat")
    private @Nullable String format = null;
    private @Nullable String contact = null;

    @Nullable
    public String getStatus() { return status; }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    @Nullable
    public String getResult() { return result; }

    public void setResult(@Nullable String result) {
        this.result = result;
    }

    @Nullable
    public String getMessage() { return message; }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    @JSON(name = "queryIP")
    @Nullable
    public String getIp() { return ip; }

    @JSON(name = "queryIP")
    public void setIp(@Nullable String ip) {
        this.ip = ip;
    }

    @JSON(name = "queryFlags")
    @Nullable
    public String getFlags() { return flags; }

    @JSON(name = "queryFlags")
    public void setFlags(@Nullable String flags) {
        this.flags = flags;
    }

    @JSON(name = "queryFormat")
    @Nullable
    public String getFormat() { return format; }

    @JSON(name = "queryFormat")
    public void setFormat(@Nullable String format) {
        this.format = format;
    }

    @Nullable
    public String getContact() { return contact; }

    public void setContact(@Nullable String contact) {
        this.contact = contact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetIPIntelModel)) {
            return false;
        }
        GetIPIntelModel that = (GetIPIntelModel) o;
        return Objects.equals(status, that.status) && Objects.equals(result, that.result) && Objects.equals(message, that.message) && Objects.equals(
                ip,
                that.ip
        ) && Objects.equals(flags, that.flags) && Objects.equals(format, that.format) && Objects.equals(contact, that.contact);
    }

    @Override
    public int hashCode() { return Objects.hash(status, result, message, ip, flags, format, contact); }

    @Override
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
