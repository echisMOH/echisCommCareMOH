package org.commcare.xml;


import org.commcare.CommCareApplication;
import org.commcare.android.database.global.models.AppAvailableToInstall;
import org.commcare.data.xml.TransactionParser;
import org.commcare.models.database.SqlStorage;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by amstone326 on 2/3/17.
 */
public class AvailableAppsParser extends TransactionParser<List<AppAvailableToInstall>> {

    private static final String APPS_TAG = "apps";
    private static final String APP_TAG = "app";
    private static final String DOMAIN_TAG = "domain";
    private static final String APP_NAME_TAG = "name";
    private static final String PROFILE_REF_TAG = "profile";
    private static final String MEDIA_PROFILE_REF_TAG = "media-profile";

    public AvailableAppsParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public List<AppAvailableToInstall> parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
        checkNode(APPS_TAG);
        List<AppAvailableToInstall> appsList = new ArrayList<>();

        parser.next();
        int eventType = parser.getEventType();
        do {
            if (eventType == KXmlParser.START_TAG) {
                String tagName = parser.getName().toLowerCase();
                if (APP_TAG.equals(tagName)) {
                    String domain = parser.getAttributeValue(null, DOMAIN_TAG);
                    String appName = parser.getAttributeValue(null, APP_NAME_TAG);
                    String profileRef = parser.getAttributeValue(null, PROFILE_REF_TAG);
                    String mediaProfileRef = parser.getAttributeValue(null, MEDIA_PROFILE_REF_TAG);
                    appsList.add(new AppAvailableToInstall(domain, appName, profileRef, mediaProfileRef));
                }
            }
            eventType = parser.next();
        } while (eventType != KXmlParser.END_DOCUMENT);

        commit(appsList);
        return appsList;
    }

    @Override
    protected void commit(List<AppAvailableToInstall> parsed) throws IOException, InvalidStructureException {
        SqlStorage<AppAvailableToInstall> storage =
                CommCareApplication.instance().getGlobalStorage(AppAvailableToInstall.STORAGE_KEY,
                        AppAvailableToInstall.class);
        for (AppAvailableToInstall availableApp : parsed) {
            storage.write(availableApp);
        }
    }

}
