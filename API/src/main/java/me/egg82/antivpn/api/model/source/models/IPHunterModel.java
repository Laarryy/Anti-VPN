package me.egg82.antivpn.api.model.source.models;

import flexjson.JSON;
import java.io.Serializable;
import java.util.Objects;

public class IPHunterModel implements SourceModel {
    private String status;
    private String code;
    private Data data;

    public IPHunterModel() {
        this.status = null;
        this.code = null;
        this.data = null;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public Data getData() { return data; }

    public void setData(Data data) { this.data = data; }

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
        private String ip;
        @JSON(name = "ip_num")
        private long ipNumber;
        @JSON(name = "country_code")
        private String countryCode;
        @JSON(name = "country_name")
        private String country;
        private String city;
        private String isp;
        private int block;

        public Data() {
            this.ip = null;
            this.ipNumber = -1L;
            this.countryCode = null;
            this.country = null;
            this.city = null;
            this.isp = null;
            this.block = -1;
        }

        public String getIp() { return ip; }

        public void setIp(String ip) { this.ip = ip; }

        @JSON(name = "ip_num")
        public long getIpNumber() { return ipNumber; }

        @JSON(name = "ip_num")
        public void setIpNumber(long ipNumber) { this.ipNumber = ipNumber; }

        @JSON(name = "country_code")
        public String getCountryCode() { return countryCode; }

        @JSON(name = "country_code")
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        @JSON(name = "country_name")
        public String getCountry() { return country; }

        @JSON(name = "country_name")
        public void setCountry(String country) { this.country = country; }

        public String getCity() { return city; }

        public void setCity(String city) { this.city = city; }

        public String getIsp() { return isp; }

        public void setIsp(String isp) { this.isp = isp; }

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
