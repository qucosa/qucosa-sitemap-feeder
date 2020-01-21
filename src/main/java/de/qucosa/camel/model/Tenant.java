package de.qucosa.camel.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tenant implements Serializable {
    @JsonProperty("name")
    private String name;

    @JsonProperty("small")
    private String small;

    @JsonProperty("host")
    private String host;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSmall() {
        return small;
    }

    public void setSmall(String small) {
        this.small = small;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
