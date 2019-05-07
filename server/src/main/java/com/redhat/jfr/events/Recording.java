package com.redhat.jfr.events;

import org.openjdk.jmc.common.item.IItemCollection;

public class Recording {

    private String filename;
    private IItemCollection events;

    public Recording(String filename) {
        this.filename = filename;
    }

    public String search() {
        return "[]";
    }

    public String query() {
        return "[]";
    }

    public String annotations() {
        return "[]";
    }

}