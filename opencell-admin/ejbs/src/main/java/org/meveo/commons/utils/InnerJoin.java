package org.meveo.commons.utils;

import java.util.LinkedHashSet;
import java.util.Set;

public class InnerJoin {

    private final String alias;
    private String name;
    private Set<InnerJoin> nextInnerJoins = new LinkedHashSet<>();

    public InnerJoin(String name) {
        this.name = name;
        int i = GeneratorUtils.COUNTER.get().incrementAndGet();
        this.alias = name + "_" + i;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public void next(InnerJoin nextInnerJoin) {
        nextInnerJoins.add(nextInnerJoin);
    }

    public Set<InnerJoin> getNextInnerJoins() {
        return nextInnerJoins;
    }
}
