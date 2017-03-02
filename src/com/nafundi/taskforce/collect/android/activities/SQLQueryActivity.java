package com.nafundi.taskforce.collect.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.inqbarna.tablefixheaders.TableFixHeaders;
import com.inqbarna.tablefixheaders.adapters.SampleTableAdapter;
import com.inqbarna.tablefixheaders.adapters.TableAdapter;
import com.nafundi.taskforce.collect.android.R;
import com.nafundi.taskforce.collect.android.preferences.PreferencesActivity;

public class SQLQueryActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.table);

		TextView errorField = (TextView) findViewById(R.id.error_field);
		errorField.setVisibility(View.GONE);

		TableFixHeaders tableFixHeaders = (TableFixHeaders) findViewById(R.id.table);

		String DATABASE_NAME = "lookup.db";
		SQLiteDatabase db;
		try {
			db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory() + "/odk/metadata/" + DATABASE_NAME, null, 0);
		} catch (SQLiteException e) {
			errorField.setText(getString(R.string.no_db_error));
			errorField.setVisibility(View.VISIBLE);

			tableFixHeaders.setVisibility(View.GONE);
			return;
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SQLQueryActivity.this);
		String query = sharedPreferences.getString(PreferencesActivity.KEY_REPORT_QUERY, getString(R.string.default_query));

		try {
			Cursor c = db.rawQuery(query, null);

			if (c != null && c.getCount() > 0) {
				String[][] myStringArray = new String[c.getColumnCount()][c.getCount()];
				String[] headers = new String[c.getColumnCount()];
				for (int y = 0; y < c.getCount(); y++) {
					c.moveToPosition(y);
					for (int x = 0; x + 2 < c.getColumnCount(); x++) {
						if (y == 0) {
							// get headers
							headers[x] = c.getColumnName(x + 2);
						}
						myStringArray[x][y] = c.getString(x + 2); // +2 because we skip the first two cols
					}
				}
				tableFixHeaders.setAdapter(new MyAdapter(SQLQueryActivity.this, myStringArray, headers));
				tableFixHeaders.setVisibility(View.VISIBLE);

			}
		} catch (SQLiteException e) {
			errorField.setText(getString(R.string.bad_query_error));
			errorField.setVisibility(View.VISIBLE);

			tableFixHeaders.setVisibility(View.GONE);
		}
	}

	public class MyAdapter extends SampleTableAdapter {

		private final int width;
		private final int height;
        private String[][] mValues;
        private String[] mHeaders;

		public MyAdapter(Context context, String[][] values, String[] headers) {
			super(context);

			Resources resources = context.getResources();

			width = resources.getDimensionPixelSize(R.dimen.table_width);
			height = resources.getDimensionPixelSize(R.dimen.table_height);
            mValues = values;
            mHeaders = headers;
		}

		@Override
		public int getRowCount() {
			return mValues[0].length;
		}

		@Override
		public int getColumnCount() {
			return mValues.length;
		}

		@Override
		public int getWidth(int column) {
			return width;
		}

		@Override
		public int getHeight(int row) {
			return height;
		}

		@Override
		public String getCellString(int row, int column) {
            if (row < 0 && column < 0) {
                return "id";
            } else if (row < 0 && column >= 0) {
                return mHeaders[column];
            } else if (column < 0) {
                return Integer.toString(row);
            }
			return mValues[column][row];
		}

		@Override
		public int getLayoutResource(int row, int column) {
			final int layoutResource;
			switch (getItemViewType(row, column)) {
				case 0:
					layoutResource = R.layout.table_header;
					break;
				case 1:
					layoutResource = R.layout.table_odd_item;
					break;
				case 2:
					layoutResource = R.layout.table_even_item;
					break;
				default:
					throw new RuntimeException("Impossible things are happening.");
			}
			return layoutResource;
		}


		@Override
		public int getItemViewType(int row, int column) {
			if (row < 0) {
				return 0;
			} else if (row % 2 == 0) {
				return 2;
			} else {
				return 1;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 3;
		}
	}
}
