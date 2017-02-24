package com.andrasta.dashi.service;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Log;

import com.andrasta.dashi.openalpr.AlprResult;
import com.andrasta.dashi.openalpr.Plate;
import com.andrasta.dashi.openalpr.PlateResult;
import com.andrasta.dashi.utils.Preconditions;
import com.andrasta.dashiclient.LicensePlate;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.andrasta.dashiclient.DashiApi.licenseService;

public class LicensePlateMatcher {

    private static final String TAG = "LicensePlateMatcher";
    private List<LicensePlate> licensePlates = new ArrayList<>();
    private static final float CONFIDENCE_THRESHOLD = 80.0f;

    public void initialize(){

        Call<List<LicensePlate>> listCall = licenseService.listLicenses();

        listCall.enqueue(new Callback<List<LicensePlate>>() {
            @Override
            public void onResponse(Call<List<LicensePlate>> call, Response<List<LicensePlate>> response) {
                List<LicensePlate> body = response.body();

                if(body != null) {
                    licensePlates = body;
                }
            }

            @Override
            public void onFailure(Call<List<LicensePlate>> call, Throwable t) {
                Log.e(TAG, "Exception in initialization", t);
            }
        });
    }

    private Pair<Plate, LicensePlate> findMatches(@NonNull Plate plate) {
        for (LicensePlate licensePlate : licensePlates) {
            if (licensePlate.matches(plate.getPlate()) && plate.getConfidence() > CONFIDENCE_THRESHOLD) {
                return new Pair<>(plate, licensePlate);
            }
        }

        return null;
    }

    public List<Pair<Plate, LicensePlate>> findMatches(@NonNull AlprResult alprResult) {
        Preconditions.assertParameterNotNull(alprResult, "alprResult");
        List<PlateResult> results = alprResult.getPlates();
        List<Pair<Plate, LicensePlate>> plateMatches = new ArrayList<>();

        if (results.size() > 0) {
            PlateResult plate = results.get(0);
            if (plate != null && plate.getBestPlate() != null) {
                Pair<Plate, LicensePlate> match = findMatches(plate.getBestPlate());

                if(match != null) {
                    plateMatches.add(match);
                }
            }
        }

        return plateMatches;
    }

    public void sendMatch(@NonNull Pair<Plate, LicensePlate> matchingPlatePair, @NonNull byte[] imageAsJpeg, Location lastKnownLocation) {

        Preconditions.assertParameterNotNull(matchingPlatePair, "matchingPlatePair");
        Preconditions.assertParameterNotNull(imageAsJpeg, "imageAsJpeg");

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageAsJpeg);

        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", matchingPlatePair.second.getUuid() + ".jpg", requestFile);

        String descriptionString = "Android device";
        RequestBody description =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, descriptionString);

        String confidenceValue = String.valueOf(matchingPlatePair.first.getConfidence());
        RequestBody confidence =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, confidenceValue);


        double latitudeValue = lastKnownLocation != null ? lastKnownLocation.getLatitude() : 0;
        double longitudeValue = lastKnownLocation != null ? lastKnownLocation.getLongitude() : 0;

        RequestBody latitude =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, String.valueOf(latitudeValue));

        RequestBody longitude =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, String.valueOf(longitudeValue));

        RequestBody speed =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, String.valueOf(10.0)); //dummy speed

        RequestBody bearing =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, String.valueOf(10.0)); // dummy bearing

        Call<ResponseBody> call = licenseService.upload(matchingPlatePair.second.getUuid(), description, confidence, latitude, longitude, speed, bearing, body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                Log.v("Upload", "success");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error:", t.getMessage());
            }
        });
    }
}
