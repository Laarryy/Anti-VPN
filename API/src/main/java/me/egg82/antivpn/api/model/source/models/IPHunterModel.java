package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class IPHunterModel implements SourceModel {
    private @Nullable String status = null;
    private @Nullable String code = null;
    private @Nullable Data data = null;

    @Nullable
    public String getStatus() { return status; }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    @Nullable
    public String getCode() { return code; }

    public void setCode(@Nullable String code) {
        this.code = code;
    }

    @Nullable
    public Data getData() { return data; }

    public void setData(@Nullable Data data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPHunterModel)) {
            return false;
        }
        IPHunterModel that = (IPHunterModel) o;
        return Objects.equals(status, that.status) && Objects.equals(code, that.code) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() { return Objects.hash(status, code, data); }

    @Override
    public String toString() {
        return "IPHunterModel{" +
                "status='" + status + '\'' +
                ", code='" + code + '\'' +
                ", data=" + data +
                '}';
    }

    public static final class Data implements Serializable {
        private @Nullable String ip = null;
        @JSON(name = "ip_num")
        private long ipNumber = -1L;
        @JSON(name = "country_code")
        private @Nullable String countryCode = null;
        @JSON(name = "country_name")
        private @Nullable String country = null;
        private @Nullable String city = null;
        private @Nullable String isp = null;
        private int block = -1;

        @Nullable
        public String getIp() { return ip; }

        public void setIp(@Nullable String ip) {
            this.ip = ip;
        }

        @JSON(name = "ip_num")
        public long getIpNumber() { return ipNumber; }

        @JSON(name = "ip_num")
        public void setIpNumber(long ipNumber) {
            this.ipNumber = ipNumber;
        }

        @JSON(name = "country_code")
        @Nullable
        public String getCountryCode() { return countryCode; }

        @JSON(name = "country_code")
        public void setCountryCode(@Nullable String countryCode) {
            this.countryCode = countryCode;
        }

        @JSON(name = "country_name")
        @Nullable
        public String getCountry() { return country; }

        @JSON(name = "country_name")
        public void setCountry(@Nullable String country) {
            this.country = country;
        }

        @Nullable
        public String getCity() { return city; }

        public void setCity(@Nullable String city) {
            this.city = city;
        }

        @Nullable
        public String getIsp() { return isp; }

        public void setIsp(@Nullable String isp) {
            this.isp = isp;
        }

        public int getBlock() { return block; }

        public void setBlock(int block) {
            this.block = block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Data)) {
                return false;
            }
            Data data = (Data) o;
            return ipNumber == data.ipNumber && block == data.block && Objects.equals(ip, data.ip) && Objects.equals(countryCode, data.countryCode) && Objects.equals(
                    country,
                    data.country
            ) && Objects.equals(city, data.city) && Objects.equals(isp, data.isp);
        }

        @Override
        public int hashCode() { return Objects.hash(ip, ipNumber, countryCode, country, city, isp, block); }

        @Override
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
