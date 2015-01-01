package com.pogamadores.candies.ui.activity;

import android.app.Activity;
import android.os.Bundle;
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
import com.pogamadores.candies.util.WebServerHelper;

public class PermissionActivity extends Activity {

    private WebView webContent;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        webContent = ((WebView) findViewById(R.id.webContent));
        webContent.setVisibility(View.GONE);
        progress = ((ProgressBar) findViewById(R.id.progress));
        progress.setIndeterminate(true);
        progress.setVisibility(View.VISIBLE);

        GsonRequest<Token> request = new GsonRequest<>(
                WebServerHelper.GET_TOKEN_PATH,
                WebServerHelper.getTokenPost(),
                new Response.Listener<Token>() {
                    @Override
                    public void onResponse(Token response) {
                        CandieSQLiteDataSource dataSource = CandiesApplication.getDatasource();
                        if(dataSource.getToken() != null)
                            dataSource.updateToken(response);
                        else
                            dataSource.saveToken(response);

                        progress.setVisibility(View.GONE);

                        webContent.loadUrl(response.getUrl().toString());
                        webContent.setVisibility(View.VISIBLE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(CandiesApplication.class.getSimpleName(),Log.getStackTraceString(error));
                        TextView errorMessage = ((TextView) findViewById(R.id.errorMessage));
                        errorMessage.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                    }
                },
                Token.class
        );
        CandiesApplication.get().addRequestToQueue(request);
    }
}
