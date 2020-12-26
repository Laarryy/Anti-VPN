package me.egg82.antivpn.api.model.source.models;

import java.io.Serializable;
import java.util.Objects;

public class IPInfoModel implements SourceModel {
    private int status;
    private Error error;
    private boolean vpn;
    private boolean proxy;
    private boolean tor;
    private boolean hosting;

    public IPInfoModel() {
        this.status = -1;
        this.error = null;
        this.vpn = false;
        this.proxy = false;
        this.tor = false;
        this.hosting = false;
    }

    public int getStatus() { return status; }

    public void setStatus(int status) { this.status = status; }

    public Error getError() { return error; }

    public void setError(Error error) { this.error = error; }

    public boolean isVpn() { return vpn; }

    public void setVpn(boolean vpn) { this.vpn = vpn; }

    public boolean isProxy() { return proxy; }

    public void setProxy(boolean proxy) { this.proxy = proxy; }

    public boolean isTor() { return tor; }

    public void setTor(boolean tor) { this.tor = tor; }

    public boolean isHosting() { return hosting; }

    public void setHosting(boolean hosting) { this.hosting = hosting; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPInfoModel)) return false;
        IPInfoModel that = (IPInfoModel) o;
        return status == that.status && vpn == that.vpn && proxy == that.proxy && tor == that.tor && hosting == that.hosting && Objects.equals(error, that.error);
    }

    public int hashCode() { return Objects.hash(status, error, vpn, proxy, tor, hosting); }

    public String toString() {
        return "IPInfoModel{" +
                "status=" + status +
                ", error=" + error +
                ", vpn=" + vpn +
                ", proxy=" + proxy +
                ", tor=" + tor +
                ", hosting=" + hosting +
                '}';
    }

    public static final class Error implements Serializable {
        private String title;
        private String message;

        public Error() {
            this.title = null;
            this.message = null;
        }

        public String getTitle() { return title; }

        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }

        public void setMessage(String message) { this.message = message; }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Error)) return false;
            Error error = (Error) o;
            return Objects.equals(title, error.title) && Objects.equals(message, error.message);
        }

        public int hashCode() { return Objects.hash(title, message); }

        public String toString() {
            return "Error{" +
                    "title='" + title + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
