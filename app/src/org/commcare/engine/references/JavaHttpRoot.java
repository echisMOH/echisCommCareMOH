package org.commcare.engine.references;

import org.commcare.network.CommcareRequestGenerator;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceFactory;

/**
 * @author ctsims
 */
public class JavaHttpRoot implements ReferenceFactory {

    private final CommcareRequestGenerator generator = CommcareRequestGenerator.buildNoAuthGenerator();

    @Override
    public Reference derive(String URI) throws InvalidReferenceException {
        return new JavaHttpReference(URI, generator);
    }

    @Override
    public Reference derive(String URI, String context) throws InvalidReferenceException {
        context = context.substring(0, context.lastIndexOf('/') + 1);
        return new JavaHttpReference(context + URI, generator);
    }

    @Override
    public boolean derives(String URI) {
        URI = URI.toLowerCase();
        return URI.startsWith("http://") || URI.startsWith("https://");
    }
}
