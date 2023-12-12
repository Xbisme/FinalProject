package com.example.finalproject.Data;

public class ReadWriteData {
    String organizationID, deviceID, authToken, temperature, humidity;

    public ReadWriteData() {
    }

    public ReadWriteData(String organizationID, String deviceID, String authToken, String temperature, String humidity) {
        this.organizationID = organizationID;
        this.deviceID = deviceID;
        this.authToken = authToken;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(String organizationID) {
        this.organizationID = organizationID;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}
