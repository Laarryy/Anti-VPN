package me.egg82.antivpn.web.models;

import flexjson.JSON;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class MCLeaksResultModel implements Serializable {
    @JSON(name = "isMcLeaks")
    private boolean isMcLeaks = false;
    private String error = null;

    public MCLeaksResultModel() {
    }

    @JSON(name = "isMcLeaks")
    public boolean isMcLeaks() {
        return isMcLeaks;
    }

    @JSON(name = "isMcLeaks")
    public void setMcLeaks(boolean mcLeaks) {
        isMcLeaks = mcLeaks;
    }

    public @Nullable String getError() {
        return error;
    }

    public void setError(@Nullable String error) {
        this.error = error;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MCLeaksResultModel)) {
            return false;
        }
        MCLeaksResultModel that = (MCLeaksResultModel) o;
        return isMcLeaks == that.isMcLeaks && Objects.equals(error, that.error);
    }

    public int hashCode() {
        return Objects.hash(isMcLeaks, error);
    }

    public String toString() {
        return "MCLeaksResultModel{" +
                "isMcLeaks=" + isMcLeaks +
                ", error='" + error + '\'' +
                '}';
    }
}
