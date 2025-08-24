package com.flansmod.warforge.api;

public class ObjectIntPair<T> {
    private T obj;
    private int integer;

    public ObjectIntPair() {
        obj = null;
        integer = 0;
    }

    public ObjectIntPair(T obj, int integer) {
        this.obj = obj;
        this.integer = integer;
    }

    public void setObj(T obj) { this.obj = obj; }
    public T getObj() { return obj; }

    public void setInteger(int integer) { this.integer = integer; }
    public int getInteger() { return integer; }

}
