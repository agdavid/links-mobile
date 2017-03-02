
package com.nafundi.taskforce.collect.android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;

import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.adapters.HierarchyListAdapter;
import com.nafundi.taskforce.collect.android.logic.HierarchyElement;
import com.nafundi.taskforce.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI.InstanceColumns;

public class FormStatsActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.form_stats));

        List <HierarchyElement> formList = new ArrayList<HierarchyElement>();

        
        HashMap<String, String> formID = new HashMap<String, String>();
        
        
        Cursor forms = managedQuery(FormsColumns.CONTENT_URI, null, null, null, FormsColumns.DISPLAY_NAME + " ASC");
        if (forms.getCount() > 0) {
            forms.move(-1);
            while (forms.moveToNext()) {
                String name = forms.getString(forms.getColumnIndex(FormsColumns.DISPLAY_NAME));
                String id = forms.getString(forms.getColumnIndex(FormsColumns.JR_FORM_ID));
                formID.put(id, name);
            }
        }

        
//        int totalsent = 0;
//        int totalunsent = 0;
        
        Set<String> formnames = formID.keySet();
        Iterator<String> itr = formnames.iterator();
        while (itr.hasNext()) {
            String jrid = (String) itr.next();
            
            String selection = InstanceColumns.JR_FORM_ID + "=?";
            String[] selectionArgs = {
                jrid
            };
         
            int sent = 0;
            int unsent = 0;
            Cursor instances = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);
            
            if (instances.getCount() > 0) {
                instances.move(-1);
                
                while (instances.moveToNext()) {
                    String status = instances.getString(instances.getColumnIndex(InstanceColumns.STATUS));
                    if (InstanceProviderAPI.STATUS_SUBMITTED.equalsIgnoreCase(status)) {
                        sent++;
                    } else {
                        unsent++;
                    }
                }
            }
            
//            totalsent+=sent;
//            totalunsent+=unsent;
            String label = formID.get(jrid);
            String subLabel = "Sent: " + sent + "   Unsent: " + unsent;
            HierarchyElement he = new HierarchyElement(label, subLabel);
            formList.add(he);
            
        }
        
//        
//        String selection = InstanceColumns.STATUS + "!=?";
//        String[] selectionArgs = {
//            InstanceProviderAPI.STATUS_SUBMITTED
//        };
//        Cursor c =
//            managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs,
//                InstanceColumns.STATUS + " desc");
//
//        String[] data = new String[] {
//                InstanceColumns.DISPLAY_NAME, InstanceColumns.DISPLAY_SUBTEXT
//        };
//        int[] view = new int[] {
//                R.id.text1, R.id.text2
//        };
//        
//        if (c.getCount() > 0) {
//            c.move(-1);
//            while (c.moveToNext()) {
//                
//            }
//        }
//
////        // render total instance view
////        SimpleCursorAdapter instances =
////            new SimpleCursorAdapter(this, R.layout.two_item, c, data, view);
////        setListAdapter(instances);
//        
//        
//        HierarchyElement t1 = new HierarchyElement("woops", "yeah");
//        HierarchyElement t2 = new HierarchyElement("asdfasdf", "ajiejfie");
//        
//
//        formList.add(t1);
//        formList.add(t2);
        
//        String subtext = "Sent: " + totalsent + "  Unsent: " + totalunsent;
//        HierarchyElement total = new HierarchyElement("OVERALL FORM STATS", subtext);
//        formList.add(0, total);
        
        HierarchyListAdapter itla = new HierarchyListAdapter(this);
        itla.setListItems(formList);
        setListAdapter(itla);
    }

}
