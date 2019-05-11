package org.commcare.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.commcare.CommCareApplication;
import org.commcare.android.database.user.models.FormRecord;
import org.commcare.utils.StringUtils;

import java.util.HashMap;

// Merged in FormRecord in CommCare 2.44, only used for DB Migration now
public class InstanceProvider extends ContentProvider {
    private static final String t = "InstancesProvider";

    private static final int DATABASE_VERSION = 2;
    private static final String INSTANCES_TABLE_NAME = "instances";

    private static final HashMap<String, String> sInstancesProjectionMap;

    private static final int INSTANCES = 1;
    private static final int INSTANCE_ID = 2;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        // the application id of the CCApp for which this db is storing instances
        private final String appId;

        public DatabaseHelper(Context c, String databaseName, String appId) {
            super(c, databaseName, null, DATABASE_VERSION);
            this.appId = appId;
        }

        public String getAppId() {
            return this.appId;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + INSTANCES_TABLE_NAME + " ("
                    + InstanceProviderAPI.InstanceColumns._ID + " integer primary key, "
                    + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " text not null, "
                    + InstanceProviderAPI.InstanceColumns.SUBMISSION_URI + " text, "
                    + InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE + " text, "
                    + InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + " text not null, "
                    + InstanceProviderAPI.InstanceColumns.JR_FORM_ID + " text not null, "
                    + InstanceProviderAPI.InstanceColumns.STATUS + " text not null, "
                    + InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE + " date not null, "
                    + InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT + " text not null );");
        }


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(t, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS instances");
            onCreate(db);
        }
    }

    private DatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        //This is so stupid.
        return true;
    }

    private void init(String appId) {
        if (mDbHelper == null || !appId.equals(mDbHelper.getAppId())) {
            String dbName = ProviderUtils.getProviderDbName(ProviderUtils.ProviderType.INSTANCES, appId);
            mDbHelper = new DatabaseHelper(CommCareApplication.instance(), dbName, appId);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder qb;

        switch (sUriMatcher.match(uri)) {
            case INSTANCE_ID:
                init(uri.getLastPathSegment());
                qb = new SQLiteQueryBuilder();
                qb.setTables(INSTANCES_TABLE_NAME);
                qb.setProjectionMap(sInstancesProjectionMap);
                qb.appendWhere(InstanceProviderAPI.InstanceColumns._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI for Querying " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        Context context = getContext();
        if (context != null) {
            c.setNotificationUri(context.getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case INSTANCES:
                return InstanceProviderAPI.InstanceColumns.CONTENT_TYPE;

            case INSTANCE_ID:
                return InstanceProviderAPI.InstanceColumns.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * {@inheritDoc}
     * Starting with the ContentValues passed in, finish setting up the
     * instance entry and write it database.
     *
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    @Deprecated
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        throw new IllegalArgumentException("insert not implemented for " + uri + ". Consider using " + FormRecord.class.getName() + " instead");
    }

    /**
     * This method removes the entry from the content provider, and also removes any associated files.
     * files:  form.xml, [formmd5].formdef, formname-media {directory}
     */
    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case INSTANCE_ID:
                init(uri.getLastPathSegment());
                count = db.delete(INSTANCES_TABLE_NAME, InstanceProviderAPI.InstanceColumns._ID + "=?", new String[]{uri.getPathSegments().get(1)});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        db.close();
        notifyChangeSafe(getContext(), uri);
        return count;

    }

    private static void notifyChangeSafe(Context context, Uri uri) {
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
    }

    @Override
    @Deprecated
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new IllegalArgumentException("update not implemented for " + uri + ". Consider using " + FormRecord.class.getName() + " instead");
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(InstanceProviderAPI.AUTHORITY, "instances/#/*", INSTANCE_ID); // # -> instance id, * -> appId

        sInstancesProjectionMap = new HashMap<>();
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns._ID, InstanceProviderAPI.InstanceColumns._ID);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME, InstanceProviderAPI.InstanceColumns.DISPLAY_NAME);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.SUBMISSION_URI, InstanceProviderAPI.InstanceColumns.SUBMISSION_URI);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE, InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH, InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.JR_FORM_ID, InstanceProviderAPI.InstanceColumns.JR_FORM_ID);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.STATUS, InstanceProviderAPI.InstanceColumns.STATUS);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE, InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT, InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT);
    }
}
