package com.dat3m.dartagnan.encoding;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat4wmm.RefinementModel;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;
import com.google.common.base.Preconditions;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.*;

public class Decoder {

    public record Info(List<Event> events, List<EdgeInfo> edges) {

        public Info() {
            this(new ArrayList<>(), new ArrayList<>());
        }

        public void add(Relation rel, Event x, Event y) {
            edges.add(new EdgeInfo(rel, x, y));
        }

        public void add(Event e) {
            events.add(e);
        }

        public void addAll(Collection<Event> newEvents) {
            events.addAll(newEvents);
        }
    }

    private final Map<BooleanFormula, Info> formula2Info = new HashMap<>(1000, 0.5f);
    private final Set<Relation> cutRelations = new HashSet<>();
    EncodingContext ctx;
    RefinementModel refinementModel;

    public Decoder(EncodingContext ctx, RefinementModel refinementModel) {
        this.ctx = ctx;
        this.refinementModel = refinementModel;
    }

    public Info decode(BooleanFormula formula) {
        return Preconditions.checkNotNull(formula2Info.get(formula),
                "No information associated to formula %s", formula);
    }

    public Set<BooleanFormula> getDecodableFormulas() {
        return formula2Info.keySet();
    }

    // requires the provided edge to be contained in the relation's may set
    public BooleanFormula registerEdge(Relation rel, Event x, Event y) {
        final BooleanFormula edgeLiteral = ctx.edge(rel, x, y);
        //final BooleanFormula returnFormula = formula2Info.containsKey(edgeLiteral) ? null : edgeLiteral;
        final Info info = formula2Info.computeIfAbsent(edgeLiteral, key -> new Info());
        info.add(rel, x, y);
        return edgeLiteral;
    }

    public void extractInfo() {
        extractRelationInfo();
        extractExecutionInfo(ctx);
    }

    public void extractRelationInfo() {
        final Set<Relation> boundary = refinementModel.computeBoundaryRelations();
        final RelationAnalysis ra = ctx.getAnalysisContext().requires(RelationAnalysis.class);
        for (Relation rel : boundary) {
            final EventGraph maySet = ra.getKnowledge(rel).getMaySet();
            final Map<Event, Set<Event>> outMap = maySet.getOutMap();
            for (Event x : outMap.keySet()) {
                for (Event y : outMap.get(x)) {
                    final BooleanFormula edgeLiteral = ctx.edge(rel, x, y);
                    final Info info = formula2Info.computeIfAbsent(edgeLiteral, key -> new Info());
                    info.add(rel, x, y);
                }
            }
        }
    }

    public void extractExecutionInfo(EncodingContext ctx) {
        final Program program = ctx.getTask().getProgram();
        final Map<BooleanFormula, List<Event>> lit2EventMap = new HashMap<>();

        for (Event e : program.getThreadEvents()) {
            lit2EventMap.computeIfAbsent(ctx.execution(e), key -> new ArrayList<>()).add(e);
        }

        lit2EventMap.forEach((lit, events) -> formula2Info.computeIfAbsent(lit, k -> new Info()).addAll(events));
    }

    public String printStats() {
        int eventNum = 0;
        int edgeNum = 0;
        for (Map.Entry<BooleanFormula, Info> info : formula2Info.entrySet()) {
            eventNum += info.getValue().events.size();
            edgeNum += info.getValue().edges.size();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Event count: ").append(eventNum).append("\n");
        sb.append("Edge count: ").append(edgeNum).append("\n");
        return sb.toString();
    }

}