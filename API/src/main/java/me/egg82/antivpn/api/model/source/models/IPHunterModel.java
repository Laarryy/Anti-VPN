package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.io.Serializable;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class IPHunterModel implements SourceModel {
    private String status = null;
    private String code = null;
    private Data data = null;

    public IPHunterModel() { }

    public @Nullable String getStatus() { return status; }

    public void setStatus(@Nullable String status) { this.status = status; }

    public @Nullable String getCode() { return code; }

    public void setCode(@Nullable String code) { this.code = code; }

    public @Nullable Data getData() { return data; }

    public void setData(@Nullable Data data) { this.data = data; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPHunterModel)) return false;
        IPHunterModel that = (IPHunterModel) o;
        return Objects.equals(status, that.status) && Objects.equals(code, that.code) && Objects.equals(data, that.data);
    }

    public int hashCode() { return Objects.hash(status, code, data); }

    public String toString() {
        return "IPHunterModel{" +
                "status='" + status + '\'' +
                ", code='" + code + '\'' +
                ", data=" + data +
                '}';
    }

    public static final class Data implements Serializable {
        private String ip = null;
        @JSON(name = "ip_num")
        private long ipNumber = -1L;
        @JSON(name = "country_code")
        private String countryCode = null;
        @JSON(name = "country_name")
        private String country = null;
        private String city = null;
        private String isp = null;
        private int block = -1;

        public Data() { }

        public @Nullable String getIp() { return ip; }

        public void setIp(@Nullable String ip) { this.ip = ip; }

        @JSON(name = "ip_num")
        public long getIpNumber() { return ipNumber; }

        @JSON(name = "ip_num")
        public void setIpNumber(long ipNumber) { this.ipNumber = ipNumber; }

        @JSON(name = "country_code")
        public @Nullable String getCountryCode() { return countryCode; }

        @JSON(name = "country_code")
        public void setCountryCode(@Nullable String countryCode) { this.countryCode = countryCode; }

        @JSON(name = "country_name")
        public @Nullable String getCountry() { return country; }

        @JSON(name = "country_name")
        public void setCountry(@Nullable String country) { this.country = country; }

        public @Nullable String getCity() { return city; }

        public void setCity(@Nullable String city) { this.city = city; }

        public @Nullable String getIsp() { return isp; }

        public void setIsp(@Nullable String isp) { this.isp = isp; }

        public int getBlock() { return block; }

        public void setBlock(int block) { this.block = block; }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Data)) return false;
            Data data = (Data) o;
            return ipNumber == data.ipNumber && block == data.block && Objects.equals(ip, data.ip) && Objects.equals(countryCode, data.countryCode) && Objects.equals(country, data.country) && Objects.equals(city, data.city) && Objects.equals(isp, data.isp);
        }

        public int hashCode() { return Objects.hash(ip, ipNumber, countryCode, country, city, isp, block); }

        public String toString() {
            return "Data{" +
                    "ip='" + ip + '\'' +
                    ", ipNumber=" + ipNumber +
                    ", countryCode='" + countryCode + '\'' +
                    ", country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    ", isp='" + isp + '\'' +
                    ", block=" + block +
                    '}';
        }
    }
}
