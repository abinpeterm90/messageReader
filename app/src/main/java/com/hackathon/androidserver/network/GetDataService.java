package com.hackathon.androidserver.network;

import com.hackathon.androidserver.model.FlightList;
import com.hackathon.androidserver.model.Message;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by abin on 09/07/2018.
 */

public interface GetDataService {
    @POST("/air/search")
    @Headers("Content-Type:application/json")
    Call<List<FlightList>> callFlightListingAPI(@Body Message message);
}
