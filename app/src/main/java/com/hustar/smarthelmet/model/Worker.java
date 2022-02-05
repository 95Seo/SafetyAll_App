package com.hustar.smarthelmet.model;

public class Worker {

    private String deviceId;
    private String status;
    private String latitude;
    private String longitude;
    private String workerName;
    private String workerPhoneNum;
    private String workerGroup;

    public Worker(String deviceId, String workerName, String workerPhoneNum, String workerGroup) {
        this.deviceId = deviceId;
        this.workerName = workerName;
        this.workerPhoneNum = workerPhoneNum;
        this.workerGroup = workerGroup;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getWorkerPhoneNum() {
        return workerPhoneNum;
    }

    public void setWorkerPhoneNum(String workerPhoneNum) {
        this.workerPhoneNum = workerPhoneNum;
    }

    public String getWorkerGroup() {
        return workerGroup;
    }

    public void setWorkerGroup(String workerPhoneNum) {
        this.workerGroup = workerGroup;
    }
}
