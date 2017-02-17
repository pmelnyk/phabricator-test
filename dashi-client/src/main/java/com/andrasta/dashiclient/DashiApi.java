package com.andrasta.dashiclient;

import java.util.List;
import java.util.UUID;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public class DashiApi {

    static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://35.166.26.91:9000")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public interface LicenseService {
        @GET("/api/licenses")
        Call<List<LicensePlate>> listLicenses();

        @Multipart
        @POST("/api/licenses/match/{id}")
        Call<ResponseBody> upload(
                @Path("id") UUID id,
                @Part("description") RequestBody description,
                @Part("confidence") RequestBody confidence,
                @Part("latitude") RequestBody latitude,
                @Part("longitude") RequestBody longitude,
                @Part("speed") RequestBody speed,
                @Part("bearing") RequestBody bearing,
                @Part MultipartBody.Part file
        );

    }

    public static final LicenseService licenseService = retrofit.create(LicenseService.class);

}
