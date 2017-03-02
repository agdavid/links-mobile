/*
 * Copyright (C) 2011 University of Washington
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

package com.nafundi.taskforce.collect.android.preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore.Images;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.application.Collect;
import com.nafundi.taskforce.collect.android.utilities.UrlUtils;
import com.nafundi.taskforce.collect.android.utilities.WebUtils;

/**
 * @author yanokwa
 */
public class PreferencesActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	protected static final int IMAGE_CHOOSER_SPLASH = 0;
	protected static final int IMAGE_CHOOSER_LOGO = 1;

	public static String KEY_LAST_VERSION = "lastVersion";
	public static String KEY_FIRST_RUN = "firstRun";
	public static String KEY_SHOW_SPLASH = "showSplash";
	public static String KEY_SPLASH_PATH = "splashPath";
	public static String KEY_LOGO_PATH = "logoPath";

	public static String KEY_FONT_SIZE = "font_size";
	public static String KEY_WEB_URL = "web_url";

	public static String KEY_SERVER_URL = "server_url";
	public static String KEY_USERNAME = "username";
	public static String KEY_PASSWORD = "password";

	public static String KEY_PROTOCOL = "protocol";
	public static String KEY_FORMLIST_URL = "formlist_url";
	public static String KEY_SUBMISSION_URL = "submission_url";
	public static String KEY_ADMIN_PW = "admin_pw";
	public static String KEY_REPORT_QUERY = "report_query";
	public static String KEY_SHOW_RUN_REPORT_BUTTON = "show_run_report_button";

	public static String KEY_COMPLETED_DEFAULT = "default_completed";

	private EditTextPreference mSubmissionUrlPreference;
	private EditTextPreference mFormListUrlPreference;
	private EditTextPreference mServerUrlPreference;
	private EditTextPreference mUsernamePreference;
	private EditTextPreference mPasswordPreference;
	private EditTextPreference mAdminPwPreference;
	private EditTextPreference mQueryPreference;

	private Context mContext;

	private static final int SAVE_PREFS_MENU = Menu.FIRST;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mContext = this;

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.general_preferences));

		setupImagePathPreference(KEY_SPLASH_PATH);
		setupImagePathPreference(KEY_LOGO_PATH);

		updateServerUrl();

		updateUsername();
		updatePassword();

		updateAdminPw();
		updateReportQuery();

		updateFormListUrl();
		updateSubmissionUrl();

		updateImagePath(KEY_SPLASH_PATH);
		updateImagePath(KEY_LOGO_PATH);

		updateFontSize();
		updateProtocol();

		updateWebURL();
		
		String versionName = null;
		 PackageInfo pinfo;
            try {
				pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
             versionName = pinfo.versionName;
			} catch (NameNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            
         PreferenceScreen info = (PreferenceScreen) findPreference("info");
         info.setSummary(versionName + " - " + getString(R.string.click_to_web));

		mQueryPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Pattern blackList = Pattern.compile("\\b(ALTER|ANALYZE|ATTACH|BEGIN|COMMIT|CREATE|DELETE|DETACH|DROP|EXPLAIN|INSERT|PRAGMA|REINDEX|RELEASE|REPLACE|ROLLBACK|SAVEPOINT|UPDATE|VACUUM)\\b",  Pattern.CASE_INSENSITIVE);
				Matcher matcher = blackList.matcher(newValue.toString());

				if (matcher.find()) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.dangerous_query_error), Toast.LENGTH_SHORT)
							.show();
					return false;

				}
				return true;
			}
		});


	}

	private void setupImagePathPreference(String key) {

		final PreferenceScreen pathPreference = (PreferenceScreen) findPreference(key);
		final int chooserReturn;

		if (key.equalsIgnoreCase(KEY_LOGO_PATH)) {
			chooserReturn = PreferencesActivity.IMAGE_CHOOSER_LOGO;
		} else {
			chooserReturn = PreferencesActivity.IMAGE_CHOOSER_SPLASH;
		}

		if (pathPreference != null) {
			pathPreference
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						private void launchImageChooser() {
							Intent i = new Intent(Intent.ACTION_GET_CONTENT);
							i.setType("image/*");
							startActivityForResult(i, chooserReturn);
						}

						@Override
						public boolean onPreferenceClick(Preference preference) {
							// if you have a value, you can clear it or select
							// new.
							CharSequence cs = pathPreference.getSummary();
							String key = pathPreference.getKey();

							if (cs != null && cs.toString().contains("/")) {

								final CharSequence[] items = {
										getString(R.string.select_another_image),
										getString(R.string.use_odk_default) };

								AlertDialog.Builder builder = new AlertDialog.Builder(
										mContext);
								OnClickListener clickListener = null;

								if (key.equalsIgnoreCase(KEY_LOGO_PATH)) {
									clickListener = new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int item) {
											if (items[item]
													.equals(getString(R.string.select_another_image))) {
												launchImageChooser();
											} else {
												setImagePath(
														getString(R.string.default_logo_path),
														KEY_LOGO_PATH);
											}
										}
									};
								} else {
									clickListener = new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int item) {
											if (items[item]
													.equals(getString(R.string.select_another_image))) {
												launchImageChooser();
											} else {
												setImagePath(
														getString(R.string.default_splash_path),
														KEY_SPLASH_PATH);
											}
										}
									};
								}

								builder.setTitle(getString(R.string.change_splash_path));
								builder.setNeutralButton(
										getString(R.string.cancel),
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int id) {
												dialog.dismiss();
											}
										});

								builder.setItems(items, clickListener);
								AlertDialog alert = builder.create();
								alert.show();

							} else {
								launchImageChooser();
							}

							return true;
						}
					});
		}

	}

	private void setImagePath(String path, String key) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, path);
		editor.commit();
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED) {
			// request was canceled, so do nothing
			return;
		}

		if (requestCode == IMAGE_CHOOSER_SPLASH
				|| requestCode == IMAGE_CHOOSER_LOGO) {

			String sourcePath = null;

			// get gp of chosen file
			Uri uri = intent.getData();
			if (uri.toString().startsWith("file")) {
				sourcePath = uri.toString().substring(6);
			} else {
				String[] projection = { Images.Media.DATA };
				Cursor c = managedQuery(uri, projection, null, null, null);
				startManagingCursor(c);
				int i = c.getColumnIndexOrThrow(Images.Media.DATA);
				c.moveToFirst();
				sourcePath = c.getString(i);
			}

			// setting image path
			if (requestCode == IMAGE_CHOOSER_LOGO) {
				setImagePath(sourcePath, KEY_LOGO_PATH);
				updateImagePath(KEY_LOGO_PATH);
			} else {
				setImagePath(sourcePath, KEY_SPLASH_PATH);
				updateImagePath(KEY_SPLASH_PATH);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_PROTOCOL)) {
			updateProtocol();
			updateServerUrl();
			updateUsername();
			updatePassword();
			updateAdminPw();
			updateFormListUrl();
			updateSubmissionUrl();
		} else if (key.equals(KEY_SERVER_URL)) {
			updateServerUrl();
		} else if (key.equals(KEY_FORMLIST_URL)) {
			updateFormListUrl();
		} else if (key.equals(KEY_SUBMISSION_URL)) {
			updateSubmissionUrl();
		} else if (key.equals(KEY_USERNAME)) {
			updateUsername();
		} else if (key.equals(KEY_PASSWORD)) {
			updatePassword();
		} else if (key.equals(KEY_ADMIN_PW)) {
			updateAdminPw();
		} else if (key.equals(KEY_SPLASH_PATH)) {
			updateImagePath(KEY_SPLASH_PATH);
		} else if (key.equals(KEY_LOGO_PATH)) {
			updateImagePath(KEY_LOGO_PATH);
		} else if (key.equals(KEY_FONT_SIZE)) {
			updateFontSize();
		} else if (key.equals(KEY_WEB_URL)) {
			updateWebURL();
		} else if (key.equals(KEY_REPORT_QUERY)) {
			updateReportQuery();
		}
	}

	private void validateUrl(EditTextPreference preference) {
		if (preference != null) {
			String url = preference.getText();
			if (url.equals("")) {
				preference.setText(url);
				preference.setSummary(url);
				return;
			}
			if (UrlUtils.isValidUrl(url)) {
				preference.setText(url);
				preference.setSummary(url);
			} else {
				// preference.setText((String) preference.getSummary());
				Toast.makeText(getApplicationContext(),
						getString(R.string.url_error), Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	private void updateServerUrl() {
		mServerUrlPreference = (EditTextPreference) findPreference(KEY_SERVER_URL);
		// remove all trailing "/"s
		while (mServerUrlPreference.getText().endsWith("/")) {
			mServerUrlPreference.setText(mServerUrlPreference.getText()
					.substring(0, mServerUrlPreference.getText().length() - 1));
		}
		validateUrl(mServerUrlPreference);

		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });
	}

	private void updateImagePath(String key) {
		PreferenceScreen preference = (PreferenceScreen) findPreference(key);
		String defaultPath;
		if (key.equalsIgnoreCase(KEY_LOGO_PATH)) {
			defaultPath = getString(R.string.default_logo_path);
		} else {
			defaultPath = getString(R.string.default_splash_path);
		}
		preference.setSummary(preference.getSharedPreferences().getString(key,
				defaultPath));
	}

	private void updateUsername() {
		mUsernamePreference = (EditTextPreference) findPreference(KEY_USERNAME);
		mUsernamePreference.setSummary(mUsernamePreference.getText());

		mUsernamePreference.getEditText().setFilters(
				new InputFilter[] { getWhitespaceFilter() });

		WebUtils.clearAllCredentials();
	}

	private void updatePassword() {
		mPasswordPreference = (EditTextPreference) findPreference(KEY_PASSWORD);
		if (mPasswordPreference.getText() != null
				&& mPasswordPreference.getText().length() > 0) {
			mPasswordPreference.setSummary("********");
		} else {
			mPasswordPreference.setSummary("");
		}
		mPasswordPreference.getEditText().setFilters(
				new InputFilter[] { getWhitespaceFilter() });

		WebUtils.clearAllCredentials();
	}

	private void updateAdminPw() {
		mAdminPwPreference = (EditTextPreference) findPreference(KEY_ADMIN_PW);
		if (mAdminPwPreference.getText() != null
				&& mAdminPwPreference.getText().length() > 0) {
			mAdminPwPreference.setSummary("********");
		} else {
			mAdminPwPreference.setSummary("");

		}
		mAdminPwPreference.getEditText().setFilters(
				new InputFilter[] { getWhitespaceFilter() });
	}

	private void updateReportQuery() {
		mQueryPreference = (EditTextPreference) findPreference(KEY_REPORT_QUERY);
		if (mQueryPreference.getText() != null
				&& mQueryPreference.getText().length() > 0) {
			mQueryPreference.setSummary(mQueryPreference.getText());
		} else {
			mQueryPreference.setSummary("");
		}
	}

	private void updateFormListUrl() {
		mFormListUrlPreference = (EditTextPreference) findPreference(KEY_FORMLIST_URL);
		mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());

		mFormListUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });
	}

	private void updateSubmissionUrl() {
		mSubmissionUrlPreference = (EditTextPreference) findPreference(KEY_SUBMISSION_URL);
		mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());

		mSubmissionUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });
	}

	private void updateFontSize() {
		ListPreference lp = (ListPreference) findPreference(KEY_FONT_SIZE);
		lp.setSummary(lp.getEntry());
	}

	private void updateWebURL() {
		EditTextPreference ep = (EditTextPreference) findPreference(KEY_WEB_URL);
		validateUrl(ep);
	}

	
	private void updateProtocol() {
		ListPreference lp = (ListPreference) findPreference(KEY_PROTOCOL);
		lp.setSummary(lp.getEntry());

		String protocol = lp.getValue();
		if (protocol.equals("odk_default")) {
			if (mServerUrlPreference != null) {
				mServerUrlPreference.setEnabled(true);
			}
			if (mUsernamePreference != null) {
				mUsernamePreference.setEnabled(true);
			}
			if (mPasswordPreference != null) {
				mPasswordPreference.setEnabled(true);
			}
			if (mFormListUrlPreference != null) {
				mFormListUrlPreference.setText(getText(
						R.string.default_odk_formlist).toString());
				mFormListUrlPreference.setEnabled(false);
			}
			if (mSubmissionUrlPreference != null) {
				mSubmissionUrlPreference.setText(getText(
						R.string.default_odk_submission).toString());
				mSubmissionUrlPreference.setEnabled(false);
			}

		} else {
			if (mServerUrlPreference != null) {
				mServerUrlPreference.setEnabled(true);
			}
			if (mUsernamePreference != null) {
				mUsernamePreference.setEnabled(true);
			}
			if (mPasswordPreference != null) {
				mPasswordPreference.setEnabled(true);
			}
			if (mFormListUrlPreference != null) {
				mFormListUrlPreference.setEnabled(true);
			}
			if (mSubmissionUrlPreference != null) {
				mSubmissionUrlPreference.setEnabled(true);
			}

		}

	}

	private InputFilter getWhitespaceFilter() {
		InputFilter whitespaceFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.isWhitespace(source.charAt(i))) {
						return "";
					}
				}
				return null;
			}
		};
		return whitespaceFilter;
	}

	private InputFilter getReturnFilter() {
		InputFilter returnFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.getType((source.charAt(i))) == Character.CONTROL) {
						return "";
					}
				}
				return null;
			}
		};
		return returnFilter;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, SAVE_PREFS_MENU, 0, getString(R.string.save_preferences))
				.setIcon(R.drawable.ic_menu_save);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_PREFS_MENU:
			File writeDir = new File(Collect.ODK_ROOT + "/settings");
			if (!writeDir.exists()) {
				if (!writeDir.mkdirs()) {
					Toast.makeText(
							this,
							"Error creating directory "
									+ writeDir.getAbsolutePath(),
							Toast.LENGTH_SHORT).show();
					return false;
				}
			}

			File dst = new File(writeDir.getAbsolutePath()
					+ "/collect.settings");
			boolean success = saveSharedPreferencesToFile(dst);
			if (success) {
				Toast.makeText(
						this,
						"Settings successfully written to "
								+ dst.getAbsolutePath(), Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(
						this,
						"Error writing settings to "
								+ dst.getAbsolutePath(), Toast.LENGTH_LONG)
						.show();
			}
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	private boolean saveSharedPreferencesToFile(File dst) {
		// this should be in a thread if it gets big, but for now it's tiny
		boolean res = false;
		ObjectOutputStream output = null;
		try {
			output = new ObjectOutputStream(new FileOutputStream(dst));
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			output.writeObject(pref.getAll());

			res = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (output != null) {
					output.flush();
					output.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}

}
