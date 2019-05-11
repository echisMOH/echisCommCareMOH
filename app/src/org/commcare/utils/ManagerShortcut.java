package org.commcare.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import org.commcare.activities.AppManagerActivity;
import org.commcare.dalvik.R;

public class ManagerShortcut extends Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent shortcutIntent = new Intent(getApplicationContext(), AppManagerActivity.class);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.manager_activity_name));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);
        finish();
    }

}
