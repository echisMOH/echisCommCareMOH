/**
 *
 */
package org.commcare.engine.references;

import android.content.Context;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceFactory;
import org.javarosa.core.reference.ReferenceManager;


/**
 * @author ctsims
 */
public class AssetFileRoot implements ReferenceFactory {
    private final Context context;

    public AssetFileRoot(Context context) {
        this.context = context;
    }

    @Override
    public Reference derive(String URI) throws InvalidReferenceException {
        return new AssetFileReference(context, URI.substring("jr://asset/".length()));
    }

    @Override
    public Reference derive(String URI, String context) throws InvalidReferenceException {
        if (context.lastIndexOf('/') != -1) {
            context = context.substring(0, context.lastIndexOf('/') + 1);
        }
        return ReferenceManager.instance().DeriveReference(context + URI);
    }

    @Override
    public boolean derives(String URI) {
        return URI.toLowerCase().startsWith("jr://asset/");
    }
}
