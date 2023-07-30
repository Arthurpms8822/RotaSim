package com.example.rotasim;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CrossDocking {

    private CrossDockingVehicle vehicle1;
    private CrossDockingVehicle vehicle2;

    public CrossDocking(CrossDockingVehicle vehicle1, CrossDockingVehicle vehicle2) {
        this.vehicle1 = vehicle1;
        this.vehicle2 = vehicle2;
    }

    public double calculateTimeDifference() {
        double time1 = vehicle1.distanceToArive;
        double time2 = vehicle2.distanceToArive;
        if(time1 > time2) {
            return time1;
        } else return time2;
    }

    public void sendTimeDifference() throws IOException {
        double newTime = calculateTimeDifference();

        Map<String, Object> map = new HashMap<>();
        map.put("newTime", newTime);

        Gson gson = new Gson();
        String json = gson.toJson(map);

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/sendNewTime")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
    }
}
