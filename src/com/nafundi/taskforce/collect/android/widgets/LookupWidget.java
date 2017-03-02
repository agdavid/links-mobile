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

package com.nafundi.taskforce.collect.android.widgets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XPathReference;

import android.content.Context;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.activities.FormEntryActivity;
import com.nafundi.taskforce.collect.android.provider.LookupProviderAPI.LookupColumns;
import com.nafundi.taskforce.collect.android.views.MediaLayout;

/**
 * SelectOneWidgets handles select-one fields using radio buttons.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class LookupWidget extends QuestionWidget {

	Vector<SelectChoice> mItems;

	Vector<RadioButton> buttons;
	Vector<MediaLayout> layout;
	RadioGroup rg;

	public LookupWidget(Context context, FormEntryPrompt prompt) {
		super(context, prompt);

		String projectColumn = null;
		Vector<TreeElement> attrs = prompt.getBindAttributes();

		HashMap<String, String> selectionClause = new HashMap<String, String>();
		for (int i = 0; i < attrs.size(); i++) {
			if ("db_get".equalsIgnoreCase(attrs.get(i).getName())) {
				projectColumn = attrs.get(i).getAttributeValue();
			} else if (attrs.get(i).getName() != null
					&& attrs.get(i).getName().startsWith("db_filter_by_col_")) {
				String col = attrs
						.get(i)
						.getName()
						.substring("db_filter_by_col_".length(),
								attrs.get(i).getName().length());
				String value = attrs.get(i).getAttributeValue();
				selectionClause.put(col, value);
			}
		}

		StringBuilder selectionBuilder = new StringBuilder();
		String[] selectionArgs = new String[selectionClause.size() + 1];
		String[] projection = { projectColumn };

		// always only select non-null columns
		selectionBuilder.append(projectColumn + "<>? and ");
		selectionArgs[0] = "NULL";

		// have to put this before the while statement below
		// in case a nodeset is wrong
		rg = new RadioGroup(context);

		// at this point, we have a map of col/nodeset, what we want is
		// col/answer
		// so use the nodesets, get the answers, and replace the nodesets in the
		// map
		// iterate through the "filter by" statements and build
		Iterator<String> itr = selectionClause.keySet().iterator();
		int i = 1;
		while (itr.hasNext()) {
			String key = itr.next();
			selectionBuilder.append("col_" + key + "=? and ");

			String nodeset = selectionClause.get(key);

			try {
				TreeReference th = XPathReference.getPathExpr(nodeset)
						.getReference();
				FormIndex filter_answer = new FormIndex(1, th);

				FormEntryPrompt filter_prompt = FormEntryActivity.mFormController
						.getQuestionPrompt(filter_answer);

				// I'm not sure I like this code, but not sure how else to do
				// it. if
				// we have an xpath reference
				// that references a node that doesn't exist, everything gets
				// created just fine, except
				// when you try to get the answer, it blows up with a null
				// pointer.
				
				selectionClause.put(key, filter_prompt.getAnswerText());
			} catch (NullPointerException e) {
				TextView tv = new TextView(context);
				tv.setText(context.getString(R.string.lookup_error, nodeset));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
				tv.setGravity(Gravity.CENTER);
				addView(tv);
				return;
			}

			selectionArgs[i] = selectionClause.get(key);
			i++;
		}
		String selection = selectionBuilder.toString();
		// remove the extra " and " added by the loop
		if (selection != null && selection.lastIndexOf(" and ") != -1) {
			// remove the trailing comma if one exists
			selection = selection.substring(0, selection.lastIndexOf(" and "));
		}

		String s = prompt.getAnswerText();
		if (selection.length() == 0) {
			selection = null;
			selectionArgs = null;
		}

		Cursor c = context.getContentResolver().query(
				LookupColumns.CONTENT_URI, projection, selection,
				selectionArgs, null);
		if (c != null) {
			if (c.getCount() == 0) {
				TextView tv = new TextView(context);
				tv.setText(getContext().getString(R.string.no_selects_found));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
				tv.setGravity(Gravity.CENTER);
				addView(tv);
				return;
			}
			c.moveToPosition(-1);
			
			while (c.moveToNext()) {
				String val = c
						.getString(c.getColumnIndexOrThrow(projectColumn));
				RadioButton r1 = new RadioButton(context);
				r1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
				r1.setText(val);
				rg.addView(r1);
				if (val.equals(s)) {
					r1.setChecked(true);
				}
			}
		}

		addView(rg);

	}

	@Override
	public void clearAnswer() {
		rg.clearCheck();
	}

	@Override
	public IAnswerData getAnswer() {
		int answer = rg.getCheckedRadioButtonId();
		if (answer == -1) {
			return null;
		} else {
			RadioButton checked = (RadioButton) findViewById(answer);
			return new StringData((String) checked.getText());
		}

	}

	@Override
	public void setFocus(Context context) {
		// Hide the soft keyboard if it's showing.
		InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		for (int i = 0; i < rg.getChildCount(); i++) {
			RadioButton r = (RadioButton) rg.getChildAt(i);
			r.setOnLongClickListener(l);
		}
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		for (int i = 0; i < rg.getChildCount(); i++) {
			RadioButton r = (RadioButton) rg.getChildAt(i);
			r.cancelLongPress();
		}
	}

}
