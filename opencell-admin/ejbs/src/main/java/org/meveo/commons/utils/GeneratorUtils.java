package org.meveo.commons.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class GeneratorUtils {
    
    public static final ThreadLocal<AtomicInteger> COUNTER = ThreadLocal.withInitial(() -> new AtomicInteger(0));
    
    public static void resetCounter() {
        COUNTER.remove();
    }
}
