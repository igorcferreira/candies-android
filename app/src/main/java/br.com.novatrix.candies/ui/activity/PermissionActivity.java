package br.com.novatrix.candies.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import br.com.novatrix.candies.R;
import br.com.novatrix.candies.application.CandiesApplication;
import br.com.novatrix.candies.database.CandieSQLiteDataSource;
import br.com.novatrix.candies.domain.Token;
import br.com.novatrix.candies.service.PaymentService;
import br.com.novatrix.candies.util.CandiesWebViewClient;
import br.com.novatrix.candies.util.Util;
import br.com.novatrix.candies.util.WebServerHelper;

/**
 * @author Igor Castañeda Ferreira - github.com/igorcferreira - @igorcferreira
 */
public class PermissionActivity extends ActionBarActivity {

    private WebView webContent;
    private ProgressBar progress;
    private Bundle receivedExtras;
    private Token token;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (receivedExtras != null)
            outState.putAll(receivedExtras);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        NotificationManagerCompat.from(getApplicationContext()).cancel(Util.NOTIFICATION_ID);

        if (savedInstanceState != null)
            receivedExtras = savedInstanceState;

        final CandieSQLiteDataSource dataSource = CandiesApplication.getDatasource();

        webContent = ((WebView) findViewById(R.id.webContent));
        webContent.setVisibility(View.INVISIBLE);
        CandiesWebViewClient webViewClient = new CandiesWebViewClient(this);
        webViewClient.setOnProcessStepListener(new CandiesWebViewClient.OnProcessStepListener() {
            @Override
            public void onProcessStarted() {
                progress.setVisibility(View.GONE);
                webContent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onServerLoading() {
                progress.setVisibility(View.VISIBLE);
                webContent.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onProcessFinished() {
                dataSource.saveToken(token);
                Intent paymentIntent = new Intent(getApplicationContext(), PaymentService.class);
                if (receivedExtras != null)
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

        if (dataSource.getToken() == null) {
            WebServerHelper.requestNewToken(
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
                    });
        } else
            showWebContentForToken(dataSource.getToken());
    }

    private void showWebContentForToken(Token token) {
        this.token = token;
        webContent.loadUrl(token.getUrl().toString());
    }
}
