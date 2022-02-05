package com.hustar.smarthelmet.DB;

public class Api {

    private static final String ROOT_URL = "https://d61b-175-120-29-131.ngrok.io/Api/v1/Api.php?apicall=";

    public static final String URL_INSERT_WORKER = ROOT_URL + "insertWorker";
    public static final String URL_INSERT_WORKER_Warning = ROOT_URL + "insertWorkerWarning";
    public static final String URL_SELECT_WORKER = ROOT_URL + "selectWorker";
    public static final String URL_UPDATE_WORKER_STATUS = ROOT_URL + "updateWorkerStatus";
    public static final String URL_DELETE_WORKER = ROOT_URL + "deleteWorker&deviceId=";
}