package com.warfactory.medical.core;

import java.util.EnumMap;

import com.warfactory.medical.api.Limb;
import com.warfactory.medical.api.LimbType;
import com.warfactory.medical.api.Patient;

public final class PatientImpl implements Patient {
    private final EnumMap<LimbType, LimbImpl> limbs = new EnumMap<>(LimbType.class);

    public PatientImpl() {
        for (LimbType limbType : LimbType.VALUES) {
            limbs.put(limbType, new LimbImpl(limbType));
        }
    }

    @Override
    public Limb getLimb(LimbType limbType) {
        return limbs.get(limbType);
    }
    
}
