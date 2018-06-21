package org.logstash.dissect;

public class DissectResult {
    private boolean bailed;

    public DissectResult() {
        bailed = false;
    }

    public final void bail() {
        bailed = true;
    }

    public boolean matched() {
        return !bailed;
    }

    public boolean notMatched() {
        return bailed;
    }
}
