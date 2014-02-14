/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.muzei.fivehundredpx;

import java.util.Random;

import retrofit.Endpoints;
import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.miz.muzei.fivehundredpx.FiveHundredPxService.Photo;
import com.miz.muzei.fivehundredpx.FiveHundredPxService.PhotosResponse;

public class FiveHundredPxArtSource extends RemoteMuzeiArtSource {
	
	public static final String ACTION_FORCE_UPDATE = "action_force_update";
	public static final String FEATURE_PREFERENCE = "featurePreference";
	public static final String CATEGORY_PREFERENCE = "categoryPreference";
	public static final String REFRESH_INTERVAL_PREFERENCE = "refreshIntervalPreference";
	public static final String POPULAR = "popular", FRESH = "fresh_today", UPCOMING = "upcoming", EDITORS = "editors";
	
    private static final String TAG = "500px";
    private static final String SOURCE_NAME = "FiveHundredPxArtSource";

    public FiveHundredPxArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            super.onHandleIntent(intent);
            return;
        }

        String action = intent.getAction();
        if (ACTION_FORCE_UPDATE.equals(action)) {
            scheduleUpdate(System.currentTimeMillis() + 1000);
        }

        super.onHandleIntent(intent);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
    	if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.UPDATE_ON_WIFI_ONLY, false) && !MizLib.isWifiConnected(this))
    		return;
    	
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
        
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Endpoints.newFixedEndpoint("https://api.500px.com"))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                    	request.addQueryParam("only", getCategory());
                    	request.addQueryParam("feature", getFeature());
                    	
                    	 // TODO Use exclude
                    	
                        request.addQueryParam("consumer_key", Config.CONSUMER_KEY);
                    }
                })
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                    	if (retrofitError.getResponse() == null)
                    		return new RetryException();
                    	
                        int statusCode = retrofitError.getResponse().getStatus();
                        if (retrofitError.isNetworkError()
                                || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        
                        scheduleUpdate(System.currentTimeMillis() + getRefreshInterval());
                        return retrofitError;
                    }
                })
                .build();

        FiveHundredPxService service = restAdapter.create(FiveHundredPxService.class);
        PhotosResponse response = service.getPhotos();

        if (response == null || response.photos == null) {
            throw new RetryException();
        }

        if (response.photos.size() == 0) {
            Log.w(TAG, "No photos returned from API.");
            scheduleUpdate(System.currentTimeMillis() + getRefreshInterval());
            return;
        }

        Random random = new Random();
        Photo photo;
        String token;
        while (true) {
            photo = response.photos.get(random.nextInt(response.photos.size()));
            token = Integer.toString(photo.id);
            if (response.photos.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
        }

        publishArtwork(new Artwork.Builder()
                .title(photo.name)
                .byline(photo.user.fullname)
                .imageUri(Uri.parse(photo.image_url))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://500px.com/photo/" + photo.id)))
                .build());
        
        scheduleUpdate(System.currentTimeMillis() + getRefreshInterval());
    }
    
    private String getCategory() {
    	return PreferenceManager.getDefaultSharedPreferences(this).getString(CATEGORY_PREFERENCE, "");
    }
    
    private String getFeature() {
    	return PreferenceManager.getDefaultSharedPreferences(this).getString(FEATURE_PREFERENCE, POPULAR);
    }
    
    private int getRefreshInterval() {
    	return PreferenceManager.getDefaultSharedPreferences(this).getInt(REFRESH_INTERVAL_PREFERENCE, 60 * 60 * 1000 * 3);
    }
}