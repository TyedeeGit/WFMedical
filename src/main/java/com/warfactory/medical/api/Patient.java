package com.warfactory.medical.api;

public interface Patient {
    /**
     * @param limbType the type of limb to get
     * @return the limb
     */
    Limb getLimb(LimbType limbType);
}
