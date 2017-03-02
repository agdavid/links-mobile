/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nafundi.taskforce.collect.android.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for NotePadProvider
 */
public final class LookupProviderAPI {
    public static final String AUTHORITY = "com.nafundi.taskforce.collect.android.provider.odk.lookup";

    // This class cannot be instantiated
    private LookupProviderAPI() {}
       
    /**
     * Notes table
     */
    public static final class LookupColumns implements BaseColumns {
        // This class cannot be instantiated
        private LookupColumns() {}
        
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/lookup");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.odk.lookup";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.odk.lookup";


        // for lookup table
        public static final String INSTANCE_PATH = "instance_path";
        public static final String COLUMN_1 = "col_1";
        public static final String COLUMN_2 = "col_2";
        public static final String COLUMN_3 = "col_3";
        public static final String COLUMN_4 = "col_4";
        public static final String COLUMN_5 = "col_5";
        public static final String COLUMN_6 = "col_6";
        public static final String COLUMN_7 = "col_7";
        public static final String COLUMN_8 = "col_8";
        public static final String COLUMN_9 = "col_9";
        public static final String COLUMN_10 = "col_10";
    }
}
