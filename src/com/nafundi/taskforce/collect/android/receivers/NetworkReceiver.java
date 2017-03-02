
package com.nafundi.taskforce.collect.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import com.nafundi.taskforce.collect.android.listeners.InstanceUploaderListener;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.nafundi.taskforce.collect.android.tasks.InstanceUploaderTask;

public class NetworkReceiver extends BroadcastReceiver implements InstanceUploaderListener {

    // turning on wifi often gets two CONNECTED events. we only want to run one thread at a time
    public static boolean running = false;
    InstanceUploaderTask mInstanceUploaderTask;


    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	// make sure sd card is ready
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
    		return;
    	}
    	
        String action = intent.getAction();

        NetworkInfo currentNetworkInfo =
            (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (currentNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                uploadForms(context);
            }
        } else if (action.equals("com.nafundi.taskforce.collect.android.FormSaved")) {
            ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

            if (ni == null || !ni.isConnected()) {
                // not connected, do nothing
            } else {
                uploadForms(context);
            }
        }
    }


    private void uploadForms(Context context) {

        if (!running) {
            running = true;

            String selection = InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS + "=?";
            String selectionArgs[] =
                {
                        InstanceProviderAPI.STATUS_COMPLETE,
                        InstanceProviderAPI.STATUS_SUBMISSION_FAILED
                };

            Cursor c =
                context.getContentResolver().query(InstanceColumns.CONTENT_URI, null, selection,
                    selectionArgs, null);

            ArrayList<Long> toUpload = new ArrayList<Long>();
            if (c != null && c.getCount() > 0) {
                c.move(-1);
                while (c.moveToNext()) {
                    Long l = c.getLong(c.getColumnIndex(InstanceColumns._ID));
                    toUpload.add(new Long(l));
                }

                mInstanceUploaderTask = new InstanceUploaderTask();
                mInstanceUploaderTask.setUploaderListener(this);

                Long[] toSendArray = new Long[toUpload.size()];
                toUpload.toArray(toSendArray);
                mInstanceUploaderTask.execute(toSendArray);
            } else {
                running = false;
            }
        }
    }


    @Override
    public void uploadingComplete(HashMap<String, String> result) {
        // task is done
        mInstanceUploaderTask.setUploaderListener(null);
        running = false;
    }


    @Override
    public void progressUpdate(int progress, int total) {
        // do nothing
    }


    @Override
    public void authRequest(URI url, HashMap<String, String> doneSoFar) {
        // if we get an auth request, just fail
        mInstanceUploaderTask.setUploaderListener(null);
        running = false;
    }
    
}
