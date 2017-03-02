package com.nafundi.taskforce.collect.android.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import com.nafundi.taskforce.collect.android.R;

public class WebActivity extends Activity {
	private static final String TAG = "webview";
	private WebView webView;

	@SuppressLint("SetJavaScriptEnabled")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_activity_layout);
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.project_website));

		Uri uri = getIntent().getData();
		Log.i(TAG, "Opening url: " + uri.toString());

		webView = (WebView) findViewById(R.id.webView1);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setAllowFileAccess(true);
		webView.getSettings().setSupportZoom(true);		
		webView.getSettings().setBuiltInZoomControls(true);

		WebViewClient webClient = new WebViewClient() {
			// Override so it only loads urls in our webview
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return false;
			}
		};

		webView.setWebViewClient(webClient);
		webView.loadUrl(uri.toString());

		ImageButton odkButton = (ImageButton) findViewById(R.id.web_exit_button);
		odkButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		ImageButton backButton = (ImageButton) findViewById(R.id.web_back_button);
		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				webView.goBack();
			}
		});
		
		ImageButton forwardButton = (ImageButton) findViewById(R.id.web_forward_button);
		forwardButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				webView.goForward();
			}
		});
		
		ImageButton refreshButton = (ImageButton) findViewById(R.id.web_refresh_button);
		refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				webView.reload();
			}
		});
	}
}
