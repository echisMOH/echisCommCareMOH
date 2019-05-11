package org.commcare.android.tests.database;

import org.commcare.CommCareTestApplication;
import org.commcare.android.CommCareTestRunner;
import org.commcare.android.util.TestUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.storage.IndexedStorageUtilityTests;
import org.javarosa.core.storage.Shoe;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * SqlStorage implementations for the base Storage Utility tests
 *
 * Created by ctsims on 9/22/2017.
 */

@Config(application = CommCareTestApplication.class)
@RunWith(CommCareTestRunner.class)
public class SqlStorageBaseTests extends IndexedStorageUtilityTests {

    @Override
    protected IStorageUtilityIndexed<Shoe> createStorageUtility() {
        TestUtils.initializeStaticTestStorage();
        return TestUtils.getStorage("ShoeStorage", Shoe.class);
    }

}
