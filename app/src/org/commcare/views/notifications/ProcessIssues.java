package org.commcare.views.notifications;

import org.commcare.activities.LoginActivity;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public enum ProcessIssues implements MessageTag {

    /**
     * Logs successfully submitted
     **/
    BadTransactions("notification.processing.badstructure"),

    /**
     * Logs saved, but not actually submitted
     **/
    StorageRemoved("notification.processing.nosdcard"),

    /**
     * You were logged out while something was occurring
     **/
    LoggedOut("notification.sending.loggedout", LoginActivity.NOTIFICATION_MESSAGE_LOGIN),

    RecordQuarantinedLocalProcessingIssue("notification.quarantine.local", "form_processing"),

    RecordQuarantinedServerIssue("notification.quarantine.server", "form_processing"),

    RecordQuarantinedRecordIssue("notification.quarantine.record.issue", "form_processing"),

    /**
     * FormRecord files cannot be found
     **/
    RecordFilesMissing("notification.formrecord.files.missing");

    ProcessIssues(String root) {
        this(root, "processing");
    }

    ProcessIssues(String root, String category) {
        this.root = root;
        this.category = category;
    }

    private final String root, category;

    @Override
    public String getLocaleKeyBase() {
        return root;
    }

    @Override
    public String getCategory() {
        return category;
    }

}
