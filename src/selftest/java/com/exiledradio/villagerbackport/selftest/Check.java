package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Assertions for the self-tests: counts passes, logs each failure. */
public final class Check {

    private final List<String> failures = new ArrayList<String>();
    private int passed;
    private String section = "";

    void section(String name) {
        this.section = name;
        VillagerBackport.LOGGER.info("SELFTEST --- {} ---", name);
    }

    public boolean ok(boolean condition, String what) {
        if (condition) {
            this.passed++;
        } else {
            fail(what);
        }
        return condition;
    }

    public boolean eq(Object expected, Object actual, String what) {
        return ok(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }

    public void fail(String what) {
        String line = "[" + this.section + "] " + what;
        this.failures.add(line);
        VillagerBackport.LOGGER.error("SELFTEST FAIL {}", line);
    }

    int passed() {
        return this.passed;
    }

    int failed() {
        return this.failures.size();
    }
}
