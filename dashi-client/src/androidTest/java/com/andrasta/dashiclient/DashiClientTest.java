package com.andrasta.dashiclient;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DashiClientTest {
    @Test
    public void listRespondsWithRightSize() throws Exception {
        Call<List<LicensePlate>> listCall = DashiApi.licenseService.listLicenses();

        Response<List<LicensePlate>> execute = listCall.execute();
        List<LicensePlate> body = execute.body();

    }
}
