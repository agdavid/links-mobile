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

package com.nafundi.taskforce.collect.android.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.model.xform.XPathReference;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.nafundi.taskforce.collect.android.activities.FormEntryActivity;
import com.nafundi.taskforce.collect.android.application.Collect;
import com.nafundi.taskforce.collect.android.listeners.FormSavedListener;
import com.nafundi.taskforce.collect.android.logic.FormController;
import com.nafundi.taskforce.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI;
import com.nafundi.taskforce.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.nafundi.taskforce.collect.android.provider.LookupProviderAPI.LookupColumns;

/**
 * Background task for loading a form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SaveToDiskTask extends AsyncTask<Void, String, Integer> {
	private final static String t = "SaveToDiskTask";

	private FormSavedListener mSavedListener;
	private Boolean mSave;
	private Boolean mMarkCompleted;
	private Uri mUri;
	private String mInstanceName;

	private String mSaveError;

	public static final int SAVED = 500;
	public static final int SAVE_ERROR = 501;
	public static final int VALIDATE_ERROR = 502;
	public static final int VALIDATED = 503;
	public static final int SAVED_AND_EXIT = 504;
	public static final int DB_ERROR = 505;

	public SaveToDiskTask(Uri uri, Boolean saveAndExit, Boolean markCompleted,
			String updatedName) {
		mUri = uri;
		mSave = saveAndExit;
		mMarkCompleted = markCompleted;
		mInstanceName = updatedName;
	}

	/**
	 * Initialize {@link FormEntryController} with {@link FormDef} from binary
	 * or from XML. If given an instance, it will be used to fill the
	 * {@link FormDef}.
	 */
	@Override
	protected Integer doInBackground(Void... nothing) {

		// validation failed, pass specific failure
		int validateStatus = validateAnswers(mMarkCompleted);
		if (validateStatus != VALIDATED) {
			return validateStatus;
		}

		FormEntryActivity.mFormController.postProcessInstance();

		if (mSave && exportData(mMarkCompleted)) {
			return SAVED_AND_EXIT;
		} else if (exportData(mMarkCompleted)) {
			return SAVED;
		}

		return SAVE_ERROR;

	}

	/**
	 * Write's the data to the sdcard, and updates the instances content
	 * provider. In theory we don't have to write to disk, and this is where
	 * you'd add other methods.
	 * 
	 * @param markCompleted
	 * @return
	 */
	public boolean exportData(boolean markCompleted) {
		ByteArrayPayload payload;
		try {

			// assume no binary data inside the model.
			FormInstance datamodel = FormEntryActivity.mFormController
					.getInstance();
			XFormSerializingVisitor serializer = new XFormSerializingVisitor();
			payload = (ByteArrayPayload) serializer
					.createSerializedPayload(datamodel);

			// write out xml
			exportXmlFile(payload, FormEntryActivity.mInstancePath);

		} catch (IOException e) {
			Log.e(t, "Error creating serialized payload");
			e.printStackTrace();
			return false;
		}

		// If FormEntryActivity was started with an Instance, just update that
		// instance
		if (Collect.getInstance().getContentResolver().getType(mUri) == InstanceColumns.CONTENT_ITEM_TYPE) {
			ContentValues values = new ContentValues();
			if (mInstanceName != null) {
				values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
			}
			if (!mMarkCompleted) {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_INCOMPLETE);
			} else {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_COMPLETE);
			}
			Collect.getInstance().getContentResolver()
					.update(mUri, values, null, null);
		} else if (Collect.getInstance().getContentResolver().getType(mUri) == FormsColumns.CONTENT_ITEM_TYPE) {
			// If FormEntryActivity was started with a form, then it's likely
			// the first time we're
			// saving.
			// However, it could be a not-first time saving if the user has been
			// using the manual
			// 'save data' option from the menu. So try to update first, then
			// make a new one if that
			// fails.
			ContentValues values = new ContentValues();
			if (mInstanceName != null) {
				values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
			}
			if (mMarkCompleted) {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_COMPLETE);
			} else {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_INCOMPLETE);
			}

			String where = InstanceColumns.INSTANCE_FILE_PATH + "=?";
			String[] whereArgs = { FormEntryActivity.mInstancePath };
			int updated = Collect
					.getInstance()
					.getContentResolver()
					.update(InstanceColumns.CONTENT_URI, values, where,
							whereArgs);
			if (updated > 1) {
				Log.w(t, "Updated more than one entry, that's not good");
			} else if (updated == 1) {
				Log.i(t, "Instance already exists, updating");
				// already existed and updated just fine
				return true;
			} else {
				Log.e(t, "No instance found, creating");
				// Entry didn't exist, so create it.
				Cursor c = Collect.getInstance().getContentResolver()
						.query(mUri, null, null, null, null);
				c.moveToFirst();
				String jrformid = c.getString(c
						.getColumnIndex(FormsColumns.JR_FORM_ID));
				String formname = c.getString(c
						.getColumnIndex(FormsColumns.DISPLAY_NAME));
				String submissionUri = c.getString(c
						.getColumnIndex(FormsColumns.SUBMISSION_URI));

				values.put(InstanceColumns.INSTANCE_FILE_PATH,
						FormEntryActivity.mInstancePath);
				values.put(InstanceColumns.INSTANCE_FILE_PATH,
						FormEntryActivity.mInstancePath);
				values.put(InstanceColumns.SUBMISSION_URI, submissionUri);
				if (mInstanceName != null) {
					values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
				} else {
					values.put(InstanceColumns.DISPLAY_NAME, formname);
				}
				values.put(InstanceColumns.JR_FORM_ID, jrformid);
				Collect.getInstance().getContentResolver()
						.insert(InstanceColumns.CONTENT_URI, values);
			}
		}

		return true;
	}

	/**
	 * This method actually writes the xml to disk.
	 * 
	 * @param payload
	 * @param path
	 * @return
	 */
	private boolean exportXmlFile(ByteArrayPayload payload, String path) {
		// create data stream
		InputStream is = payload.getPayloadStream();
		int len = (int) payload.getLength();

		// read from data stream
		byte[] data = new byte[len];
		try {
			int read = is.read(data, 0, len);
			if (read > 0) {
				// write xml file
				try {
					// String filename = path + "/" +
					// path.substring(path.lastIndexOf('/') + 1) + ".xml";
					BufferedWriter bw = new BufferedWriter(new FileWriter(path));
					bw.write(new String(data, "UTF-8"));
					bw.flush();
					bw.close();
					return true;

				} catch (IOException e) {
					Log.e(t, "Error writing XML file");
					e.printStackTrace();
					return false;
				}
			}
		} catch (IOException e) {
			Log.e(t, "Error reading from payload data stream");
			e.printStackTrace();
			return false;
		}

		return false;
	}

	@Override
	protected void onPostExecute(Integer result) {
		synchronized (this) {
			if (mSavedListener != null)
				mSavedListener.savingComplete(result);
		}
	}

	public void setFormSavedListener(FormSavedListener fsl) {
		synchronized (this) {
			mSavedListener = fsl;
		}
	}

	/**
	 * Goes through the entire form to make sure all entered answers comply with
	 * their constraints. Constraints are ignored on 'jump to', so answers can
	 * be outside of constraints. We don't allow saving to disk, though, until
	 * all answers conform to their constraints/requirements.
	 * 
	 * @param markCompleted
	 * @return validatedStatus
	 */
	private int validateAnswers(Boolean markCompleted) {
		FormIndex i = FormEntryActivity.mFormController.getFormIndex();
		FormEntryActivity.mFormController.jumpToIndex(FormIndex
				.createBeginningOfFormIndex());

		int event;
		while ((event = FormEntryActivity.mFormController
				.stepToNextEvent(FormController.STEP_OVER_GROUP)) != FormEntryController.EVENT_END_OF_FORM) {
			if (event != FormEntryController.EVENT_QUESTION) {
				continue;
			} else {
				int saveStatus = FormEntryActivity.mFormController
						.answerQuestion(FormEntryActivity.mFormController
								.getQuestionPrompt().getAnswerValue());
				if (markCompleted
						&& saveStatus != FormEntryController.ANSWER_OK) {
					return saveStatus;
				}
			}
		}

		// at this point we know it's all validated, so we can put stuff into
		// the database safely
		// loop through the form looking for db_saves and put it in the DB
		boolean success = true;

		FormEntryActivity.mFormController.jumpToIndex(FormIndex
				.createBeginningOfFormIndex());
		while ((event = FormEntryActivity.mFormController
				.stepToNextEvent(FormController.STEP_OVER_GROUP)) != FormEntryController.EVENT_END_OF_FORM) {
			if (event != FormEntryController.EVENT_QUESTION) {
				continue;
			} else {
				Vector<TreeElement> attrs = FormEntryActivity.mFormController
						.getQuestionPrompt().getBindAttributes();

				// if we get an error looking up a nodeset on save, we save the
				// form, but not the
				// nodeset value to the db.
				// the string widget shows you the error

				HashMap<String, String> inserts = new HashMap<String, String>();

				for (TreeElement te : attrs) {
					String value = null;
					String col = null;
					String node = null;
					if (te.getName() != null
							&& te.getName().startsWith("db_add_col_")) {
						if (".".equalsIgnoreCase(te.getAttributeValue())) {
							// save the value of this node
							value = FormEntryActivity.mFormController
									.getQuestionPrompt().getAnswerValue().getDisplayText();
						} else {
							node = te.getAttributeValue();
							// we're saving another node

							try {
								TreeReference th = XPathReference.getPathExpr(
										node).getReference();
								FormIndex filter_answer = new FormIndex(1, th);
								FormEntryPrompt filter_prompt = FormEntryActivity.mFormController
										.getQuestionPrompt(filter_answer);
								// I'm not sure I like this code, but not sure
								// how
								// else to do it. if we have an xpath reference
								// that references a node that doesn't exist,
								// everything gets created just fine, except
								// when you try to get the answer, it blows up
								// with
								// a null pointer.

								value = filter_prompt.getAnswerValue().getDisplayText();
							} catch (NullPointerException e) {
								e.printStackTrace();
								success = false;
								mSaveError = "Error getting value for node: "
										+ node;
								break;
							} catch (RuntimeException e) {
								e.printStackTrace();
								success = false;
								mSaveError = "Error getting value for node: "
										+ node;
								break;
							}
						}

						col = te.getName().substring("db_add_col_".length(),
								te.getName().length());

						try {
							int colNum = Integer.parseInt(col);
							if (colNum > 10) {
								success = false;
								mSaveError = "Error: col_" + col
										+ " does not exist";
								break;
							}
						} catch (RuntimeException e) {
							e.printStackTrace();
							success = false;
							mSaveError = "Error: col_" + col
									+ " does not exist";
							break;
						}

						inserts.put(col, value);

					}
				}

				if (success && inserts.size() > 0) {
					ContentValues cv = new ContentValues();
					Iterator<String> itr = inserts.keySet().iterator();
					while (itr.hasNext()) {
						String key = itr.next();
						cv.put("col_" + key, inserts.get(key));
					}
					cv.put(LookupColumns.INSTANCE_PATH,
							FormEntryActivity.mInstancePath);
					Collect.getInstance().getContentResolver()
							.insert(LookupColumns.CONTENT_URI, cv);
				}
			}
		}

		FormEntryActivity.mFormController.jumpToIndex(i);
		if (!success) {
			return DB_ERROR;
		}
		return VALIDATED;
	}

	public String getSaveError() {
		return mSaveError;
	}

}
