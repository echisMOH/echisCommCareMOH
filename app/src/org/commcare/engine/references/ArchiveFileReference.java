package org.commcare.engine.references;

import org.javarosa.core.reference.Reference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Associates a GUID and relative path with a corresponding real directory in
 * the filesystem
 *
 * @author wspride
 */
class ArchiveFileReference implements Reference {

    private final String GUID;
    private final String archiveURI;
    private final String localroot;

    public ArchiveFileReference(String localroot, String GUID, String archiveURI) {
        this.archiveURI = archiveURI;
        this.localroot = localroot;
        this.GUID = GUID;
    }

    @Override
    public boolean doesBinaryExist() throws IOException {
        return false;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Archive references are read only!");
    }

    @Override
    public InputStream getStream() throws IOException {
        File file = new File(getLocalURI());

        if (!file.exists()) {
            throw new FileNotFoundException("No file exists at " + file.getAbsolutePath());
        }
        return new FileInputStream(file);

    }

    @Override
    public String getURI() {
        return "jr://archive/" + GUID + "/" + archiveURI;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void remove() throws IOException {
        throw new IOException("Cannot remove files from the archive");
    }

    @Override
    public String getLocalURI() {
        return localroot + "/" + archiveURI;
    }
}
