package me.egg82.antivpn.api.model.source.models;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class IPInfoModel implements SourceModel {
    private int status = -1;
    private Error error = null;
    private boolean vpn = false;
    private boolean proxy = false;
    private boolean tor = false;
    private boolean hosting = false;

    public IPInfoModel() { }

    public int getStatus() { return status; }

    public void setStatus(int status) {
        this.status = status;
    }

    public @Nullable Error getError() { return error; }

    public void setError(@Nullable Error error) {
        this.error = error;
    }

    public boolean isVpn() { return vpn; }

    public void setVpn(boolean vpn) {
        this.vpn = vpn;
    }

    public boolean isProxy() { return proxy; }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public boolean isTor() { return tor; }

    public void setTor(boolean tor) {
        this.tor = tor;
    }

    public boolean isHosting() { return hosting; }

    public void setHosting(boolean hosting) {
        this.hosting = hosting;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPInfoModel)) {
            return false;
        }
        IPInfoModel that = (IPInfoModel) o;
        return status == that.status && vpn == that.vpn && proxy == that.proxy && tor == that.tor && hosting == that.hosting && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() { return Objects.hash(status, error, vpn, proxy, tor, hosting); }

    @Override
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
        private String title = null;
        private String message = null;

        public Error() { }

        public @Nullable String getTitle() { return title; }

        public void setTitle(@Nullable String title) {
            this.title = title;
        }

        public @Nullable String getMessage() { return message; }

        public void setMessage(@Nullable String message) {
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Error)) {
                return false;
            }
            Error error = (Error) o;
            return Objects.equals(title, error.title) && Objects.equals(message, error.message);
        }

        @Override
        public int hashCode() { return Objects.hash(title, message); }

        @Override
        public String toString() {
            return "Error{" +
                    "title='" + title + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
