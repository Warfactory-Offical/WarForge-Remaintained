package com.flansmod.warforge.client.effect;

import java.util.HashMap;
import java.util.Map;

public class EffectRegistry {

   public static Map<String,IEffect> EFFECT_REGISTRY =  new HashMap<>();
   public static void init(){
       EFFECT_REGISTRY.put("upgrade", new EffectUpgrade());
       EFFECT_REGISTRY.put("disband", new EffectDisband());


   }
}
