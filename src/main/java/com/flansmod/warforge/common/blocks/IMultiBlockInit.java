package com.flansmod.warforge.common.blocks;

import java.util.ArrayList;
import java.util.List;

public interface IMultiBlockInit {
    public List<MultiBlockColumn> INSTANCES = new ArrayList<>();
    static public void registerMaps(){
        INSTANCES.forEach(IMultiBlockInit::initMap);
    }

    public void initMap();

}
