package com.example.kevin.kcamera;

import android.util.SparseArray;

import com.example.kevin.kcamera.Interface.ModuleManager;

import java.util.ArrayList;
import java.util.List;

public class ModuleManagerImpl implements ModuleManager {

    private final SparseArray<ModuleAgent> mRegisteredModuleAgents = new
            SparseArray<ModuleAgent>(2);
    private int mDefaultModuleId = MODULE_INDEX_NONE;

    @Override
    public void registerModule(ModuleAgent agent) {
        if (agent == null) {
            throw new NullPointerException("Registering a null ModuleAgent.");
        }
        final int moduleId = agent.getModuleId();
        if (moduleId == MODULE_INDEX_NONE) {
            throw new IllegalArgumentException(
                    "ModuleManager: The module ID can not be " + "MODULE_INDEX_NONE");
        }
        if (mRegisteredModuleAgents.get(moduleId) != null) {
            throw new IllegalArgumentException("Module ID is registered already:" + moduleId);
        }
        mRegisteredModuleAgents.put(moduleId, agent);
    }

    @Override
    public boolean unregisterModule(int moduleId) {
        if (mRegisteredModuleAgents.get(moduleId) == null) {
            return false;
        }
        mRegisteredModuleAgents.delete(moduleId);
        if (moduleId == mDefaultModuleId) {
            mDefaultModuleId = -1;
        }
        return true;    }

    @Override
    public List<ModuleAgent> getRegisteredModuleAgents() {
        List<ModuleAgent> agents = new ArrayList<ModuleAgent>();
        for (int i = 0; i < mRegisteredModuleAgents.size(); i++) {
            agents.add(mRegisteredModuleAgents.valueAt(i));
        }
        return agents;    }

    @Override
    public List<Integer> getSupportedModeIndexList() {
        List<Integer> modeIndexList = new ArrayList<Integer>();
        for (int i = 0; i < mRegisteredModuleAgents.size(); i++) {
            modeIndexList.add(mRegisteredModuleAgents.keyAt(i));
        }
        return modeIndexList;    }

    @Override
    public boolean setDefaultModuleIndex(int moduleId) {
        if (mRegisteredModuleAgents.get(moduleId) != null) {
            mDefaultModuleId = moduleId;
            return true;
        }
        return false;    }

    @Override
    public int getDefaultModuleIndex() {
        return mDefaultModuleId;
    }

    @Override
    public ModuleAgent getModuleAgent(int moduleId) {
        ModuleAgent agent = mRegisteredModuleAgents.get(moduleId);
        if (agent == null) {
            return mRegisteredModuleAgents.get(mDefaultModuleId);
        }
        return agent;    }
}
