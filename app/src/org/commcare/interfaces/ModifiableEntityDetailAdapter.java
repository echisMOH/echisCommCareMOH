package org.commcare.interfaces;

import org.commcare.adapters.ListItemViewModifier;

/**
 * Created by jschweers on 9/2/2015.
 */
public interface ModifiableEntityDetailAdapter {
    void setModifier(ListItemViewModifier modifier);
}
