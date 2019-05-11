package org.commcare.tasks;

import android.net.Uri;

import org.commcare.CommCareApplication;
import org.commcare.tasks.templates.CommCareTask;
import org.commcare.utils.FileUtil;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author ctsims
 */
public class UnzipTask extends CommCareTask<String, String, Integer, UnZipTaskListener> {
    public static final int UNZIP_TASK_ID = 7212435;

    public UnzipTask() {
        this.taskId = UNZIP_TASK_ID;
        TAG = UnzipTask.class.getSimpleName();
    }

    @Override
    protected Integer doTaskBackground(String... params) {
        InputStream inputStream;
        try {
            inputStream = getInputStreamForFile(params[0]);
        } catch (FileNotFoundException e) {
            publishProgress(getInvalidZipFileErrorMessage());
            return -1;
        }
        ZipInputStream zis = new ZipInputStream(inputStream);

        Logger.log(TAG, "Unzipping archive '" + params[0] + "' to  '" + params[1] + "'");
        return unZipFromStream(zis, params[1]);
    }

    @Override
    protected void deliverResult(UnZipTaskListener unZipTaskListener, Integer result) {
        if (result > 0) {
            unZipTaskListener.OnUnzipSuccessful(result);
        } else {
            unZipTaskListener.OnUnzipFailure("");
        }
    }

    @Override
    protected void deliverUpdate(UnZipTaskListener unZipTaskListener, String... update) {
        unZipTaskListener.updateUnzipProgress(update[0], UNZIP_TASK_ID);
    }

    @Override
    protected void deliverError(UnZipTaskListener unZipTaskListener, Exception e) {
        unZipTaskListener.OnUnzipFailure(e.getMessage());
    }

    private InputStream getInputStreamForFile(String filePathOrUri) throws FileNotFoundException {
        if (filePathOrUri.startsWith("content://")) {
            return CommCareApplication.instance().getContentResolver().openInputStream(Uri.parse(filePathOrUri));
        } else {
            return new FileInputStream(new File(filePathOrUri));
        }
    }

    private Integer unZipFromStream(ZipInputStream zis, String destinationPath) {
        File destination = new File(destinationPath);
        int count = 0;
        ZipEntry entry = null;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                publishProgress(Localization.get("mult.install.progress", new String[]{String.valueOf(count)}));
                count++;

                Logger.log(TAG, "Unzipped entry " + entry.getName());

                if (entry.isDirectory()) {
                    FileUtil.createFolder(new File(destination, entry.getName()).toString());
                    //If it's a directory we can move on to the next one
                    continue;
                }

                File outputFile = new File(destination, entry.getName());
                if (!outputFile.getParentFile().exists()) {
                    FileUtil.createFolder(outputFile.getParentFile().toString());
                }
                if (outputFile.exists()) {
                    //Try to overwrite if we can
                    if (!outputFile.delete()) {
                        //If we couldn't, just skip for now
                        continue;
                    }
                }

                if (!copyZipEntryToOutputFile(outputFile, zis)) {
                    return -1;
                }
            }
        } catch (IOException e) {
            publishProgress(Localization.get("mult.install.progress.badentry", new String[]{entry.getName()}));
            return -1;
        } finally {
            StreamsUtil.closeStream(zis);
        }
        Logger.log(TAG, "Successfully unzipped files");
        return count;
    }

    private boolean copyZipEntryToOutputFile(File outputFile, ZipInputStream zipInputStream) {
        BufferedOutputStream outputStream;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (IOException ioe) {
            publishProgress(Localization.get("mult.install.progress.baddest", new String[]{outputFile.getName()}));
            return false;
        }

        /*
          We only get here when the stream is located on a zip entry.
          Now we can read the file data from the stream for this current
          ZipEntry just like a normal input stream
         */
        try {
            StreamsUtil.writeFromInputToOutputUnmanaged(zipInputStream, outputStream);
        } catch (IOException ioe) {
            publishProgress(Localization.get("mult.install.progress.errormoving"));
            return false;
        } finally {
            StreamsUtil.closeStream(outputStream);
        }
        return true;
    }

    protected String getInvalidZipFileErrorMessage() {
        return Localization.get("zip.install.bad");
    }
}
