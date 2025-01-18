package com.dat3m.dartagnan.wmm.utils;

import java.util.Map;
import java.util.Set;

public record BaseEdgeEncodingResult(Map<RelationTuple, Set<RelationTuple>> and,
                                     Map<RelationTuple, Set<RelationTuple>> or,
                                     Map<RelationTuple, Set<Set<RelationTuple>>> orAnd) {
}
