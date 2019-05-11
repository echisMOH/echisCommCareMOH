package org.commcare.interfaces;

/**
 * Interface to be implemented by any CommCareActivity that uses a CommCareActivityUIController
 *
 * @author Aliza Stone (astone@dimagi.com)
 */
public interface WithUIController {

    CommCareActivityUIController getUIController();

    void initUIController();

}
