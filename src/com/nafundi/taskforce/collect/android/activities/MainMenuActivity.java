/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nafundi.taskforce.collect.android.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.application.Collect;
import com.nafundi.taskforce.collect.android.preferences.PreferencesActivity;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.nafundi.taskforce.collect.android.utilities.FileUtils;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class MainMenuActivity extends Activity {
	private static final String t = "MainMenuActivity";

	private static final int PASSWORD_DIALOG = 1;

	// menu options
	private static final int MENU_ADMIN = Menu.FIRST;

	// buttons
	private Button mEnterDataButton;
	private Button mSendDataButton;
    private Button mSQLQueryButton;
//	private Button mFormStatsButton;
//	private Button mWebButton;

	private TextView networkStatus;

	private static int mCompletedCount;

	private AlertDialog mAlertDialog;

	private static boolean EXIT = true;

	private IntentFilter mNetworkStateChangedFilter;
	private BroadcastReceiver mNetworkStateIntentReceiver;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateButtons();
		}
	};

	Cursor c;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		Log.i(t, "Starting up, creating directories");
		try {
			Collect.createODKDirs();
		} catch (RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}
		
		String versionName = "";
		PackageInfo pinfo;
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionName = pinfo.versionName;
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}
            

		setContentView(R.layout.main_menu);
		// update version for new releases
		setTitle(getString(R.string.app_name) + " " + versionName + " > "
				+ getString(R.string.main_menu));

		File f = new File(Collect.ODK_ROOT + "/collect.settings");
		if (f.exists()) {
			boolean success = loadSharedPreferencesFromFile(f);
			if (success) {
				Toast.makeText(this,
						"Settings successfully loaded from file",
						Toast.LENGTH_LONG).show();
				f.delete();
			} else {
				Toast.makeText(
						this,
						"Sorry, settings file is corrupt and should be deleted or replaced",
						Toast.LENGTH_LONG).show();
			}
		}

		// enter data button. expects a result.
		mEnterDataButton = (Button) findViewById(R.id.enter_data);
		mEnterDataButton.setText(getString(R.string.enter_data_button));
		mEnterDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainMenuActivity.this,
						FormChooserList.class);
				startActivity(i);
			}
		});

//		// review data button. expects a result.
//		mFormStatsButton = (Button) findViewById(R.id.form_stats);
//		mFormStatsButton.setText(getString(R.string.form_stats));
//		mFormStatsButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent i = new Intent(getApplicationContext(),
//						FormStatsActivity.class);
//				startActivity(i);
//			}
//		});

		SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showReportButton = mSharedPreferences.getBoolean(PreferencesActivity.KEY_SHOW_RUN_REPORT_BUTTON, false);

		mSQLQueryButton = (Button) findViewById(R.id.sql_query);
		mSQLQueryButton.setText(getString(R.string.sql_query));
		mSQLQueryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(),
						SQLQueryActivity.class);
				startActivity(i);
			}
		});
		if (!showReportButton) {
			mSQLQueryButton.setVisibility(View.GONE);
		}


		// send data button. expects a result.
		mSendDataButton = (Button) findViewById(R.id.send_data);
		mSendDataButton.setText(getString(R.string.send_data_button));
		mSendDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(),
						InstanceUploaderList.class);
				startActivity(i);
			}
		});

//		mWebButton = (Button) findViewById(R.id.web_button);
//		mWebButton.setText(getString(R.string.web_button));
//		mWebButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				SharedPreferences sharedPreferences = PreferenceManager
//						.getDefaultSharedPreferences(MainMenuActivity.this);
//				String url = sharedPreferences.getString(
//						PreferencesActivity.KEY_WEB_URL, "");
//				Intent i = new Intent(MainMenuActivity.this, WebActivity.class);
//				try {
//					i.setData(Uri.parse(url));
//					startActivity(i);
//				} catch (ActivityNotFoundException e) {
//					Toast.makeText(MainMenuActivity.this,
//							"Sorry, no activity to handle this action",
//							Toast.LENGTH_SHORT).show();
//				} catch (Exception e) {
//					e.printStackTrace();
//					Toast.makeText(MainMenuActivity.this,
//							"Sorry, not a valid website URL",
//							Toast.LENGTH_SHORT).show();
//				}
//			}
//		});

		// count for saved instances
		String selection = InstanceColumns.STATUS + "=? or "
				+ InstanceColumns.STATUS + "=?";
		String selectionArgs[] = { InstanceProviderAPI.STATUS_COMPLETE,
				InstanceProviderAPI.STATUS_SUBMISSION_FAILED };

		c = managedQuery(InstanceColumns.CONTENT_URI, null, selection,
				selectionArgs, null);
		startManagingCursor(c);
		mCompletedCount = c.getCount();
		c.registerContentObserver(contentObserver);

		updateButtons();
		updateLogo();

		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		boolean connected = false;
		if (ni != null) {
			connected = ni.isConnected();
		}
		networkStatus = (TextView) findViewById(R.id.network_status_bar);

		setNetworkStatus(connected);

		mNetworkStateChangedFilter = new IntentFilter();
		mNetworkStateChangedFilter
				.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		mNetworkStateIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(
						ConnectivityManager.CONNECTIVITY_ACTION)) {
					NetworkInfo info = intent
							.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
					if (info != null && info.isConnected()) {
						setNetworkStatus(true);
					} else {
						setNetworkStatus(false);
					}

				}
			}
		};

		registerReceiver(mNetworkStateIntentReceiver,
				mNetworkStateChangedFilter);

	}

	private void setNetworkStatus(boolean connected) {
		if (connected) {
			networkStatus.setText(getString(R.string.active_connection));
			// black on gray
			networkStatus.setTextColor(Color.BLACK);
			networkStatus.setBackgroundColor(Color.rgb(221, 221, 221));

		} else {
			networkStatus.setText(getString(R.string.inactive_connection));
			// white on red
			networkStatus.setTextColor(Color.WHITE);
			networkStatus.setBackgroundColor(Color.rgb(165, 35, 35));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ADMIN, 0, "Admin Login").setIcon(
				android.R.drawable.ic_menu_more);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADMIN:
			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(MainMenuActivity.this);
			String pw = sharedPreferences.getString(
					PreferencesActivity.KEY_ADMIN_PW, "");
			if (pw.compareTo("") == 0) {
				Intent i = new Intent(getApplicationContext(),
						AdminMenuActivity.class);
				startActivity(i);
			} else {
				showDialog(PASSWORD_DIALOG);
			}

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON1:
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}

	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mNetworkStateIntentReceiver);
	}

	/**
	 * We use Android's dialog management for loading/saving progress dialogs
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PASSWORD_DIALOG:

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog passwordDialog = builder.create();

			passwordDialog.setIcon(android.R.drawable.ic_dialog_info);
			passwordDialog.setTitle("Admin Password");
			final EditText input = new EditText(this);
			input.setText("");
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			input.setTransformationMethod(PasswordTransformationMethod
					.getInstance());
			passwordDialog.setView(input, 20, 10, 20, 10);

			passwordDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// this is really set below
							String value = input.getText().toString();
							SharedPreferences sharedPreferences = PreferenceManager
									.getDefaultSharedPreferences(MainMenuActivity.this);
							String pw = sharedPreferences.getString(
									PreferencesActivity.KEY_ADMIN_PW, "");

							if (pw.compareTo(value) == 0) {
								Intent i = new Intent(getApplicationContext(),
										AdminMenuActivity.class);
								startActivity(i);
								input.setText("");
								passwordDialog.dismiss();
							} else {
								Toast.makeText(MainMenuActivity.this,
										"Sorry, password is incorrect!",
										Toast.LENGTH_SHORT).show();

							}
						}
					});

			passwordDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							input.setText("");
							return;
						}
					});

			return passwordDialog;

		}
		return null;
	}

	private void updateButtons() {
		if (c != null) {
			c.requery();
			mCompletedCount = c.getCount();
			mSendDataButton.setText(getString(R.string.send_data_button,
					mCompletedCount));
		}

		SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showReportButton = mSharedPreferences.getBoolean(PreferencesActivity.KEY_SHOW_RUN_REPORT_BUTTON, false);
		if (!showReportButton) {
			mSQLQueryButton.setVisibility(View.GONE);
		} else {
			mSQLQueryButton.setVisibility(View.VISIBLE);
		}
	}

	private void updateLogo() {
		String logoPath = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(PreferencesActivity.KEY_LOGO_PATH,
						getString(R.string.default_logo_path));
		File f = new File(logoPath);
		if (f.exists()) {
			ImageView iv = (ImageView) findViewById(R.id.logo);
			iv.setImageBitmap(FileUtils.getBitmapScaledToDisplay(f,
					getWindowManager().getDefaultDisplay().getHeight(),
					getWindowManager().getDefaultDisplay().getWidth()));
		}

	}

	private class MyContentObserver extends ContentObserver {

		public MyContentObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			handler.sendMessage(handler.obtainMessage());

		}

	}

	MyContentObserver contentObserver = new MyContentObserver();

	@Override
	protected void onResume() {
		super.onResume();
		updateButtons();
		updateLogo();
	}

	@SuppressWarnings({ "unchecked" })
	private boolean loadSharedPreferencesFromFile(File src) {
		// this should probably be in a thread if it ever gets big
		boolean res = false;
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(src));
			Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(
					this).edit();
			prefEdit.clear();
			Map<String, ?> entries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					prefEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					prefEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					prefEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}
			prefEdit.commit();
			res = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}

}
