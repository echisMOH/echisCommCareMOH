package org.commcare.tasks;

/**
 * For reporting update, completion, and cancellation task events.
 */
public interface TaskListener<Progress, Result> {
    void handleTaskUpdate(Progress... updateVals);

    void handleTaskCompletion(Result result);

    void handleTaskCancellation();
}

