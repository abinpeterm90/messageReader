package com.hackathon.androidserver.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

/**
 * Created by abin on 09/07/2018.
 */

public interface GetDataService {
    @GET("/flights")
    @Headers("Cache-Control: max-age=640000")
    Call<String> callFlightListingAPI();
}
