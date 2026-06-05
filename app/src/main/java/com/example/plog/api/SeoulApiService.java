package com.example.plog.api;

import com.example.plog.api.model.SeoulResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SeoulApiService {
    @GET("{apiKey}/json/citydata/1/5/{placeName}")
    Call<SeoulResponse> getRealtimeData(
            @Path("apiKey") String apiKey,
            @Path(value = "placeName", encoded = true) String placeName
    );
}