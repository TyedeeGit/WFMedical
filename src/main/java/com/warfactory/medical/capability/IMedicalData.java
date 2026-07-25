package com.warfactory.medical.capability;

import com.warfactory.medical.core.MedicalProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IMedicalData {

    MedicalProfile getProfile();

    void setProfile(MedicalProfile profile);

    boolean isDirty();

    int getRevision();

    int getLastSyncedRevision();

    void bumpRevision();

    void markSynced();

    boolean needsSync();

    CompoundTag save();

    void load(CompoundTag tag);
}
