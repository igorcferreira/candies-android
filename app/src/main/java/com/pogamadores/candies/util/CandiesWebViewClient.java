package com.pogamadores.candies.util;

import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CandiesWebViewClient extends WebViewClient {

    private OnProcessStepListener onProcessStepListener;

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        Uri uri = Uri.parse(url);
        if(uri.getAuthority() != null) {
            if(uri.getAuthority().endsWith("paypal.com"))
                return false;
            else if(uri.getAuthority().endsWith(WebServerHelper.SERVER_AUTHORITY)) {
                if(uri.getPath().endsWith(WebServerHelper.OPERATION_SUCCESSFUL_PATH) && onProcessStepListener != null)
                    onProcessStepListener.onProcessFinish();
                return false;
            }
        }
        return true;
    }

    public void setOnProcessStepListener(OnProcessStepListener onProcessStepListener) {
        this.onProcessStepListener = onProcessStepListener;
    }

    public interface OnProcessStepListener
    {
        public void onProcessFinish();
    }
}
