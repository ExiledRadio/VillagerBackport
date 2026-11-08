package com.exiledradio.villagerbackport.selftest;

/** One area's headless test. */
public interface SelfTest {

    /** The name passed to {@code -Pselftest=} ("all" runs every one). */
    String name();

    void run(Check check) throws Exception;
}
