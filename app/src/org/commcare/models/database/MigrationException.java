package org.commcare.models.database;

/**
 * Exception thrown when an error is encountered during global db upgrades
 *
 * @author amstone
 */
public class MigrationException extends RuntimeException {

    private final boolean definiteFailure;

    public MigrationException(boolean b) {
        this.definiteFailure = b;
    }

    public boolean isDefiniteFailure() {
        return this.definiteFailure;
    }

}
