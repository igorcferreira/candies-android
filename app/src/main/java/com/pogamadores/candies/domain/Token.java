package com.pogamadores.candies.domain;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

/**
 * Created by igorcferreira on 12/24/14.
 */
public class Token
{
    @SerializedName("token")
    private String token;
    @SerializedName("url")
    private String url;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Uri getUrl() {
        if(url == null) return null;
        return Uri.parse(url);
    }

    public void setUrl(Uri url) {
        this.url = (url == null?null:url.toString());
    }
}
