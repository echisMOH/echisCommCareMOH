package org.commcare.models.database;

import org.commcare.interfaces.AppFilePathBuilder;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayOutputStream;

/**
 * Allows toggling how serialized objects are stored (database vs filesystem)
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class UnencryptedHybridFileBackedSqlStorageMock<T extends Persistable>
        extends UnencryptedHybridFileBackedSqlStorage<T> {
    private static boolean storeInFS = false;

    public UnencryptedHybridFileBackedSqlStorageMock(String table,
                                                     Class<? extends T> ctype,
                                                     AndroidDbHelper helper,
                                                     AppFilePathBuilder fsPathBuilder) {
        super(table, ctype, helper, fsPathBuilder);
    }

    public static void alwaysPutInDatabase() {
        storeInFS = false;
    }

    public static void alwaysPutInFilesystem() {
        storeInFS = true;
    }

    @Override
    protected boolean blobFitsInDb(ByteArrayOutputStream bos) {
        return !storeInFS;
    }
}
