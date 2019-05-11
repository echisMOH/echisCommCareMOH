package org.commcare.tasks;

import org.commcare.CommCareApp;
import org.commcare.CommCareApplication;
import org.commcare.engine.resource.AndroidResourceManager;
import org.commcare.engine.resource.AppInstallStatus;
import org.commcare.engine.resource.ResourceInstallUtils;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.tasks.templates.CommCareTask;
import org.commcare.utils.AndroidCommCarePlatform;

/**
 * Blocks user while performing installation of staged update table.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class InstallStagedUpdateTask<R>
        extends CommCareTask<Void, int[], AppInstallStatus, R> {

    public InstallStagedUpdateTask(int taskId) {
        this.taskId = taskId;
        TAG = InstallStagedUpdateTask.class.getSimpleName();
    }

    @Override
    protected AppInstallStatus doTaskBackground(Void... params) {
        return installStagedUpdate();
    }

    public static AppInstallStatus installStagedUpdate() {
        CommCareApp app = CommCareApplication.instance().getCurrentApp();
        app.setupSandbox();

        AndroidCommCarePlatform platform = app.getCommCarePlatform();
        AndroidResourceManager resourceManager =
                new AndroidResourceManager(platform);

        if (!resourceManager.isUpgradeTableStaged()) {
            resourceManager.recordUpdateInstallFailure(AppInstallStatus.UnknownFailure);
            return AppInstallStatus.UnknownFailure;
        }

        try {
            resourceManager.upgrade();
        } catch (UnresolvedResourceException e) {
            resourceManager.recordUpdateInstallFailure(e);
            return AppInstallStatus.MissingResources;
        } catch (ResourceInitializationException e) {
            resourceManager.recordUpdateInstallFailure(e);
            return AppInstallStatus.UpdateFailedResourceInit;
        }

        ResourceInstallUtils.initAndCommitApp(app);

        return AppInstallStatus.Installed;
    }
}
