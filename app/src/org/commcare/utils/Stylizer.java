package org.commcare.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Stylizer {
    private String globalStyleString;

    public Stylizer(Context c) {
        globalStyleString = "";

        ArrayList<String> mStyles = new ArrayList<>();

        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(c.getAssets().open("app_styles.txt")));
            ArrayList<String> values = new ArrayList<>();
            String line = bReader.readLine();
            while (line != null) {
                values.add(line);
                line = bReader.readLine();
            }
            bReader.close();
            for (String v : values) {
                mStyles.add(v);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < mStyles.size(); i++) {
            String style = mStyles.get(i);
            String key = style.substring(0, style.indexOf("="));
            String val = style.substring(style.indexOf('=') + 1);
            globalStyleString += MarkupUtil.formatKeyVal(key, val);
        }
    }

    public String getStyleString() {
        return globalStyleString;
    }
}
