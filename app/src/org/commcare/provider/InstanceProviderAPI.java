package org.commcare.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import org.commcare.dalvik.BuildConfig;

// Merged in FormRecord in CommCare 2.44, only used for DB Migration now
public final class InstanceProviderAPI {
    public static final String AUTHORITY = BuildConfig.ODK_AUTHORITY + ".instances";

    // This class cannot be instantiated
    private InstanceProviderAPI() {
    }

    public static final class InstanceColumns implements BaseColumns {
        // This class cannot be instantiated
        private InstanceColumns() {
        }

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.odk.instance";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.odk.instance";

        // These are the only things needed for an insert
        public static final String DISPLAY_NAME = "displayName";
        public static final String SUBMISSION_URI = "submissionUri";
        public static final String INSTANCE_FILE_PATH = "instanceFilePath";
        public static final String JR_FORM_ID = "jrFormId";

        // these are generated for you (but you can insert something else if you want)
        public static final String STATUS = "status";
        public static final String CAN_EDIT_WHEN_COMPLETE = "canEditWhenComplete";
        public static final String LAST_STATUS_CHANGE_DATE = "date";
        public static final String DISPLAY_SUBTEXT = "displaySubtext";
    }
}
