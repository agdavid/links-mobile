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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.application.Collect;
import com.nafundi.taskforce.collect.android.preferences.PreferencesActivity;
import com.nafundi.taskforce.collect.android.utilities.FileUtils;

/**
 * Responsible for displaying buttons to launch the major activities. Launches some activities based
 * on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class AdminMenuActivity extends Activity {
    private static final String t = "MainMenuActivity";

    // menu options
	private static final int MENU_MAIN = Menu.FIRST;

    private static final int MENU_PREFERENCES = Menu.FIRST+1;


    // buttons
    private Button mManageFilesButton;
    private Button mGetFormsButton;

    private AlertDialog mAlertDialog;

    private static boolean EXIT = true;

//    Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//          updateButtons();
//       }
//    };

    Cursor c;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // must be at the beginning of any activity that can be called from an external intent
        Log.i(t, "Starting up, creating directories");
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        setContentView(R.layout.admin_menu);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.admin_menu));

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

        // review data button. expects a result.
//        mReviewDataButton = (Button) findViewById(R.id.review_data);
//        mReviewDataButton.setText(getString(R.string.review_data_button));
//        mReviewDataButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
//                startActivity(i);
//            }
//        });

        // manage forms button. no result expected.
        mGetFormsButton = (Button) findViewById(R.id.get_forms);
        mGetFormsButton.setText(getString(R.string.get_forms));
        mGetFormsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), FormDownloadList.class);
                startActivity(i);

            }
        });

        // manage forms button. no result expected.
        mManageFilesButton = (Button) findViewById(R.id.manage_forms);
        mManageFilesButton.setText(getString(R.string.manage_files));
        mManageFilesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), FileManagerTabs.class);
                startActivity(i);
            }
        });
        
//        String selection = InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS + "=?";
//        String selectionArgs[] = {
//                InstanceProviderAPI.STATUS_COMPLETE, InstanceProviderAPI.STATUS_SUBMISSION_FAILED
//        };

//        c = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);
//        startManagingCursor(c);
//        mCompletedCount = c.getCount();
//        c.registerContentObserver (contentObserver);
        
        //updateButtons();
        updateLogo();
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
        menu.add(0, MENU_MAIN, 0, "Admin Logout").setIcon(
                android.R.drawable.ic_menu_revert);
        menu.add(0, MENU_PREFERENCES, 0, getString(R.string.general_preferences)).setIcon(
                android.R.drawable.ic_menu_preferences);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PREFERENCES:
                Intent ig = new Intent(this, PreferencesActivity.class);
                startActivity(ig);
                return true;
            case MENU_MAIN:
            	finish();
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
    
//    private void updateButtons() {
//        c.requery();
//        mCompletedCount = c.getCount();
//        mReviewDataButton.setText(getString(R.string.review_data_button, mCompletedCount));
//    }
    
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
    
    
  
//    private class MyContentObserver extends ContentObserver {
//
//        public MyContentObserver() {
//            super(null);
//        }
//
//        @Override
//        public void onChange(boolean selfChange) {
//            super.onChange(selfChange);
//            handler.sendMessage(handler.obtainMessage());
//
//        }
//
//    }

    //MyContentObserver contentObserver = new MyContentObserver();
    
    @Override
    protected void onResume() {
        super.onResume();
        //updateButtons();
        updateLogo();
    }
    
}


