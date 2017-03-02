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

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;

import java.util.Vector;

/**
 * Widget that restricts values to integers.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class StringNumberWidget extends StringWidget {

    public StringNumberWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        mAnswer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mAnswer.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);

        // needed to make long readonly text scroll
        mAnswer.setHorizontallyScrolling(false);
        mAnswer.setSingleLine(false);
        
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

        mAnswer.setKeyListener(new DigitsKeyListener() {
            @Override
            protected char[] getAcceptedChars() {
                char[] accepted = {
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '-', '+', ' ', ','
                };
                return accepted;
            }
        });

        if (prompt.isReadOnly()) {
            setBackgroundDrawable(null);
            setFocusable(false);
            setClickable(false);
        }

        String s = null;
        if (prompt.getAnswerValue() != null)
            s = (String) prompt.getAnswerValue().getValue();

        if (s != null) {
            mAnswer.setText(s);
        }
    }


    @Override
    public IAnswerData getAnswer() {
        String s = mAnswer.getText().toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            try {
                return new StringData(s);
            } catch (Exception NumberFormatException) {
                return null;
            }
        }
    }

}
