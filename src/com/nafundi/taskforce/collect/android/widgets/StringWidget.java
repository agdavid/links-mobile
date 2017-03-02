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

import java.util.Vector;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XPathReference;

import android.content.Context;
import android.text.InputFilter;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.activities.FormEntryActivity;

/**
 * The most basic widget that allows for entry of any text.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class StringWidget extends QuestionWidget {

    boolean mReadOnly = false;
    protected EditText mAnswer;


    public StringWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);
        mAnswer = new EditText(context);
        
        // This mess is just a test to see if the bind has an 'add key', and if so
        // if the key is a valid nodeset.  If it is, go on with life.
        // if not, show an error.
        Vector<TreeElement> attrs = prompt.getBindAttributes();
        for (TreeElement te : attrs) {
            if ("db_add_key".equalsIgnoreCase(te.getName())){
                String filter = te.getAttributeValue();
                
                TreeReference th =
                    XPathReference.getPathExpr(filter).getReference();
                FormIndex filter_answer = new FormIndex(1, th);
                FormEntryPrompt filter_prompt = FormEntryActivity.mFormController.getQuestionPrompt(filter_answer);
                // I'm not sure I like this code, but not sure how else to do it.  if we have an xpath reference
                // that references a node that doesn't exist, everything gets created just fine, except
                // when you try to get the answer, it blows up with a null pointer.
                try {
                    @SuppressWarnings("unused")
					String db_filter = filter_prompt.getAnswerText();
                } catch (NullPointerException e) {
                   TextView tv = new TextView(context);
                   tv.setText(context.getString(R.string.lookup_error, filter));
                   tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
                   tv.setGravity(Gravity.CENTER);
                   addView(tv);
                   return;
                }
            }

        }

        String length = null;
        Vector<TreeElement> attributes = prompt.getBindAttributes();
            for (int i = 0; i < attributes.size(); i++) {
                if ("maxLength".equalsIgnoreCase(attributes.get(i).getName())) {
                    length = attributes.get(i).getAttributeValue();
                    break;
                }
            }
        
          
            
        mAnswer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        if (length != null) {
            // add the length
            int len = Integer.valueOf(length);
            InputFilter[] FilterArray = new InputFilter[1];
            FilterArray[0] = new InputFilter.LengthFilter(len);
            mAnswer.setFilters(FilterArray);
        }

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5);
        mAnswer.setLayoutParams(params);
        
        // capitalize the first letter of the sentence
        mAnswer.setKeyListener(new TextKeyListener(Capitalize.SENTENCES, false));

        // needed to make long read only text scroll
        mAnswer.setHorizontallyScrolling(false);
        mAnswer.setSingleLine(false);

        if (prompt != null) {
            mReadOnly = prompt.isReadOnly();
            String s = prompt.getAnswerText();
            if (s != null) {
                mAnswer.setText(s);
            }

            if (mReadOnly) {
                mAnswer.setBackgroundDrawable(null);
                mAnswer.setFocusable(false);
                mAnswer.setClickable(false);
            }
        }

        addView(mAnswer);
    }


    @Override
    public void clearAnswer() {
        mAnswer.setText(null);
    }


    @Override
    public IAnswerData getAnswer() {
        String s = mAnswer.getText().toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            return new StringData(s);
        }
    }


    @Override
    public void setFocus(Context context) {
        // Put focus on text input field and display soft keyboard if appropriate.
        mAnswer.requestFocus();
        InputMethodManager inputManager =
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!mReadOnly) {
            inputManager.showSoftInput(mAnswer, 0);
            /*
             * If you do a multi-question screen after a "add another group" dialog, this won't
             * automatically pop up. It's an Android issue.
             * 
             * That is, if I have an edit text in an activity, and pop a dialog, and in that
             * dialog's button's OnClick() I call edittext.requestFocus() and
             * showSoftInput(edittext, 0), showSoftinput() returns false. However, if the edittext
             * is focused before the dialog pops up, everything works fine. great.
             */
        } else {
            inputManager.hideSoftInputFromWindow(mAnswer.getWindowToken(), 0);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.isAltPressed() == true) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mAnswer.setOnLongClickListener(l);
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mAnswer.cancelLongPress();
    }

}
