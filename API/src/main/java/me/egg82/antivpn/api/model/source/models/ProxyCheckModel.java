package me.egg82.antivpn.api.model.source.models;

import java.io.Serializable;
import java.util.Objects;

public class ProxyCheckModel implements SourceModel {
    private String status;
    private String message;

    public ProxyCheckModel() {
        this.status = null;
        this.message = null;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProxyCheckModel)) return false;
        ProxyCheckModel that = (ProxyCheckModel) o;
        return Objects.equals(status, that.status) && Objects.equals(message, that.message);
    }

    public int hashCode() { return Objects.hash(status, message); }

    public String toString() {
        return "ProxyCheckModel{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public static final class IP implements Serializable {
        private String proxy;
        private String type;

        public IP() {
            this.proxy = null;
            this.type = null;
        }

        public String getProxy() { return proxy; }

        public void setProxy(String proxy) { this.proxy = proxy; }

        public String getType() { return type; }

        public void setType(String type) { this.type = type; }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IP)) return false;
            IP ip = (IP) o;
            return Objects.equals(proxy, ip.proxy) && Objects.equals(type, ip.type);
        }

        public int hashCode() { return Objects.hash(proxy, type); }

        public String toString() {
            return "IP{" +
                    "proxy='" + proxy + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
