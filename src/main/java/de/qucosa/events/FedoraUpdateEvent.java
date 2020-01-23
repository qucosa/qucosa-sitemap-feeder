/*
 * Copyright 2019 Saxon State and University Library Dresden (SLUB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.qucosa.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.nashorn.internal.ir.annotations.Ignore;

import java.io.Serializable;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public class FedoraUpdateEvent implements Serializable {

    @JsonProperty("de.qucosa.event.version")
    private String version;

    @JsonProperty("de.qucosa.event.sourceID")
    private String sourceID;

    @JsonProperty("de.qucosa.event.transactionID")
    private String transactionID;

    @JsonProperty("de.qucosa.event.expires")
    private Long expires;

    @JsonProperty("de.qucosa.event.owner")
    private String owner;

    @JsonProperty("org.fcrepo.jms.user")
    private String user;

    @JsonProperty("org.fcrepo.jms.eventID")
    private String eventID;

    @JsonProperty("org.fcrepo.jms.identifier")
    private String identifier;

    @JsonProperty("org.fcrepo.jms.timestamp")
    private Long timestamp;

    @JsonProperty("org.fcrepo.jms.baseURL")
    private String baseURL;

    @JsonProperty("org.fcrepo.jms.eventType")
    private String eventType;

    @JsonProperty("org.fcrepo.jms.resourceType")
    private String resourceType;

    @JsonProperty("org.fcrepo.jms.userAgent")
    private String userAgent;

    @Ignore
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceID() {
        return sourceID;
    }

    public void setSourceID(String sourceID) {
        this.sourceID = sourceID;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
