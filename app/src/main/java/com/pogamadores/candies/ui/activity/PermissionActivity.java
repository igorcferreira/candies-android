package com.pogamadores.candies.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.pogamadores.candies.R;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.database.CandieSQLiteDataSource;
import com.pogamadores.candies.domain.Token;
import com.pogamadores.candies.request.GsonRequest;
import com.pogamadores.candies.service.PaymentService;
import com.pogamadores.candies.util.CandiesWebViewClient;
import com.pogamadores.candies.util.WebServerHelper;

public class PermissionActivity extends Activity {

    private WebView webContent;
    private ProgressBar progress;
    private Bundle receivedExtras;
    private Token token;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(receivedExtras != null)
            outState.putAll(receivedExtras);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        if(savedInstanceState != null)
            receivedExtras = savedInstanceState;

        final CandieSQLiteDataSource dataSource = CandiesApplication.getDatasource();

        webContent = ((WebView) findViewById(R.id.webContent));
        webContent.setVisibility(View.GONE);
        CandiesWebViewClient webViewClient = new CandiesWebViewClient();
        webViewClient.setOnProcessStepListener(new CandiesWebViewClient.OnProcessStepListener() {
            @Override
            public void onProcessFinish() {
                dataSource.saveToken(token);
                Intent paymentIntent = new Intent(getApplicationContext(), PaymentService.class);
                if(receivedExtras != null)
                    paymentIntent.putExtras(receivedExtras);
                getApplicationContext().startService(paymentIntent);
                finish();
            }
        });
        webContent.setWebViewClient(webViewClient);

        //We are enabling Javascript and Cookies for a better experience on the PayPal web site
        webContent.getSettings().setJavaScriptEnabled(true);
        webContent.getSettings().setAppCacheEnabled(true);

        progress = ((ProgressBar) findViewById(R.id.progress));
        progress.setIndeterminate(true);
        progress.setVisibility(View.VISIBLE);

        if(dataSource.getToken() == null) {
            GsonRequest<Token> request = new GsonRequest<>(
                    WebServerHelper.GET_TOKEN_PATH,
                    WebServerHelper.getTokenPost(),
                    new Response.Listener<Token>() {
                        @Override
                        public void onResponse(Token response) {
                            showWebContentForToken(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(CandiesApplication.class.getSimpleName(), Log.getStackTraceString(error));
                            TextView errorMessage = ((TextView) findViewById(R.id.errorMessage));
                            errorMessage.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.GONE);
                        }
                    },
                    Token.class
            );
            CandiesApplication.get().addRequestToQueue(request);
        } else
            showWebContentForToken(dataSource.getToken());
    }

    private void showWebContentForToken(Token token) {
        this.token = token;
        progress.setVisibility(View.GONE);
        webContent.loadUrl(token.getUrl().toString());
        webContent.setVisibility(View.VISIBLE);
    }
}
