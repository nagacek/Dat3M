package com.dat3m.dartagnan.wmm.analysis;

import com.dat3m.dartagnan.program.analysis.AliasAnalysis;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.newRelationAnalysis.Knowledge;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dat3m.dartagnan.configuration.OptionNames.EXTEND_RELATION_ANALYSIS;
import static com.google.common.base.Verify.verify;

@Options
public class RelationAnalysis {

    private static final Logger logger = LogManager.getLogger(RelationAnalysis.class);

    private final VerificationTask task;
    private final Map<Relation,Knowledge> knowledgeMap = new HashMap<>();

    @Option(name=EXTEND_RELATION_ANALYSIS,
            description="Propagate knowledge over indirect violations of single relationships to some axiom of the model.",
            secure = true)
    private boolean doPropagate = true;

    private RelationAnalysis(VerificationTask task) {
        this.task = task;
        Context context = task.getAnalysisContext();
        context.requires(AliasAnalysis.class);
        context.requires(BranchEquivalence.class);
        context.requires(WmmAnalysis.class);
    }

    public static RelationAnalysis fromConfig(VerificationTask task) throws InvalidConfigurationException {
        RelationAnalysis ra = new RelationAnalysis(task);
        task.getConfig().inject(ra);
        logger.info("{}: {}",EXTEND_RELATION_ANALYSIS,ra.doPropagate);
        ra.initialize();
        ra.propagate();
        return ra;
    }

    public VerificationTask getTask() {
        return task;
    }

    public TupleSet getMaxTupleSet(Relation relation) {
        return knowledgeMap.get(relation).getMaySet();
    }

    public TupleSet getMinTupleSet(Relation relation) {
        return knowledgeMap.get(relation).getMustSet();
    }

    public Set<Tuple> getDisabledSet(Relation relation) {
        return knowledgeMap.get(relation).getDisabledSet();
    }

    @FunctionalInterface
    public interface SetBuffer {
        void send(Relation rel, Set<Tuple> may, Set<Tuple> must);
    }

    @FunctionalInterface
    public interface SetListener {
        void notify(TupleSet may, TupleSet must);
    }

    @FunctionalInterface
    public interface SetObservable {
        void listen(Relation relation, SetListener listener);
    }

    @FunctionalInterface
    public interface Buffer {
        void send(Relation rel, Set<Tuple> disable, Set<Tuple> enable);
    }

    @FunctionalInterface
    public interface Listener {
        void notify(TupleSet disable, TupleSet enable);
    }

    @FunctionalInterface
    public interface Observable {
        void listen(Relation relation, Listener listener);
    }

    private void initialize() {
        logger.info("Start initial knowledge");
        // Init data context so that each relation is able to compute its may/must sets.
        Wmm memoryModel = task.getMemoryModel();

        for (Relation rel : memoryModel.getRelationRepository().getRelations()) {
            knowledgeMap.put(rel, new Knowledge());
        }

        // ------------------------------------------------
        Map<Relation,List<SetListener>> listener = new HashMap<>();
        Map<Relation,Knowledge.SetDelta> qGlobal = new HashMap<>();
        Map<Relation,Knowledge.SetDelta> qLocal = new HashMap<>();
        Set<Relation> stratum = new HashSet<>();
        SetBuffer buffer = (rel, may, must) -> {
            if(may.isEmpty() && must.isEmpty()) {
                return;
            }
            Knowledge.SetDelta d = (stratum.contains(rel) ? qLocal : qGlobal).computeIfAbsent(rel, k -> new Knowledge.SetDelta());
            d.getAddedMaySet().addAll(may);
            d.getAddedMustSet().addAll(must);
        };
        SetObservable observable = (rel, lis) -> listener.computeIfAbsent(rel, k->new ArrayList<>()).add(lis);
        for(String relName : Wmm.BASE_RELATIONS){
            memoryModel.getRelationRepository().getRelation(relName).initialize(this,buffer,observable);
        }
        for (Relation rel : memoryModel.getRelationRepository().getRelations()) {
            rel.initialize(this,buffer,observable);
        }

        // ------------------------------------------------
        for(Set<DependencyGraph<Relation>.Node> scc : memoryModel.getRelationDependencyGraph().getSCCs()) {
            verify(qLocal.isEmpty(), "queue for last stratum was not empty");
            stratum.clear();
            scc.stream().map(DependencyGraph.Node::getContent).forEach(stratum::add);
            // move from global queue
            for(Relation relation : stratum) {
                Knowledge.SetDelta delta = qGlobal.remove(relation);
                if(delta != null) {
                    qLocal.put(relation,delta);
                }
            }
            while(!qLocal.isEmpty()) {
                Relation relation = qLocal.keySet().iterator().next();
                Knowledge.SetDelta delta = knowledgeMap.get(relation).joinSet(qLocal.remove(relation));
                for(SetListener l : listener.getOrDefault(relation,List.of())) {
                    // this may invoke buffer and thus fill qGlobal and qLocal
                    l.notify(delta.getAddedMaySet(),delta.getAddedMustSet());
                }
            }
        }
        verify(qGlobal.isEmpty(), "knowledge buildup propagated downwards");
        logger.info("End initial knowledge");
    }

    private void propagate() {
        if(!doPropagate) {
            return;
        }
        logger.info("Start knowledge closure");
        Wmm memoryModel = task.getMemoryModel();
        Map<Relation,List<Listener>> listener = new HashMap<>();
        Map<Relation,Knowledge.Delta> queue = new HashMap<>();
        Buffer buffer = (rel, dis, en) -> {
            if(dis.isEmpty() && en.isEmpty()) {
                return;
            }
            Knowledge.Delta delta = queue.computeIfAbsent(rel, k -> new Knowledge.Delta());
            delta.getDisabledSet().addAll(dis);
            delta.getEnabledSet().addAll(en);
        };
        Observable observable = (rel, lis) -> listener.computeIfAbsent(rel, k -> new ArrayList<>()).add(lis);
        for(Relation relation : memoryModel.getRelationRepository().getRelations()) {
            relation.propagate(this,buffer,observable);
        }
        for(Axiom axiom : memoryModel.getAxioms()) {
            axiom.propagate(this,buffer,observable);
        }
        while(!queue.isEmpty()) {
            Relation relation = queue.keySet().iterator().next();
            Knowledge.Delta delta = knowledgeMap.get(relation).join(queue.remove(relation));
            if(delta.isEmpty()) {
                continue;
            }
            for(Listener l : listener.getOrDefault(relation, List.of())) {
                l.notify(delta.getDisabledSet(), delta.getEnabledSet());
            }
        }
        logger.info("End knowledge closure");
    }
}
