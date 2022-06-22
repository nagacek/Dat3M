package com.dat3m.dartagnan.verification;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.configuration.Baseline;
import com.dat3m.dartagnan.configuration.Property;
import com.dat3m.dartagnan.encoding.ProgramEncoder;
import com.dat3m.dartagnan.encoding.PropertyEncoder;
import com.dat3m.dartagnan.encoding.SymmetryEncoder;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.witness.WitnessGraph;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.analysis.WmmAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.axiom.Empty;
import com.dat3m.dartagnan.wmm.relation.Relation;
import org.apache.logging.log4j.LogManager;
import com.dat3m.dartagnan.wmm.utils.RelationRepository;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.EnumSet;

import static com.dat3m.dartagnan.configuration.Baseline.*;
import static com.dat3m.dartagnan.configuration.OptionNames.BASELINE;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.*;

/*
 A RefinementTask is a VerificationTask with an additional baseline memory model.
 The intention is that such a task is solved by any solving strategy that starts from the
 baseline memory model and refines it iteratively towards the target memory model.
 Currently, we only have a custom theory solver (CAAT) to solve such tasks but any CEGAR-like approach could be used.
 */
@Options
public class RefinementTask extends VerificationTask {

	private static final Logger logger = LogManager.getLogger(RefinementTask.class);

    private final Wmm baselineModel;
    private WmmEncoder baselineWmmEncoder;
    private final VerificationTask baselineTask;


    // =========================== Configurables ===========================

	@Option(name=BASELINE,
			description="Refinement starts from this baseline WMM.",
			secure=true,
			toUppercase=true)
		private EnumSet<Baseline> baselines = EnumSet.noneOf(Baseline.class);

    // ======================================================================

    private RefinementTask(Program program, Wmm targetMemoryModel, Wmm baselineModel,
    		EnumSet<Property> property, WitnessGraph witness, Configuration config)
    throws InvalidConfigurationException {
        super(program, targetMemoryModel, property, witness, config, Context.create());
        config.inject(this);
        this.baselineModel = baselineModel != null ? baselineModel : createDefaultWmm();
        this.baselineTask = new VerificationTask(program, this.baselineModel, property, witness, config, Context.create(getAnalysisContext()));
    }

    public Wmm getBaselineModel() {
        return baselineModel;
    }

    public WmmEncoder getBaselineWmmEncoder() { return baselineWmmEncoder; }

    @Override
    public void performStaticWmmAnalyses() throws InvalidConfigurationException {
        super.performStaticWmmAnalyses();
        Context baselineContext = baselineTask.getAnalysisContext();
        baselineContext.register(WmmAnalysis.class, WmmAnalysis.fromConfig(baselineModel, getConfig()));
        baselineContext.register(RelationAnalysis.class, RelationAnalysis.fromConfig(baselineTask, baselineContext, getConfig()));
    }

    @Override
    public void initializeEncoders(SolverContext ctx) throws InvalidConfigurationException {
        progEncoder = ProgramEncoder.create(this,ctx);
        propertyEncoder = PropertyEncoder.create(baselineTask,ctx);
        //wmmEncoder = WmmEncoder.fromConfig(this,ctx);
        symmetryEncoder = SymmetryEncoder.create(baselineTask,ctx);
        baselineWmmEncoder = WmmEncoder.create(baselineTask,ctx);

		logger.info("{}: {}", BASELINE, baselines);
    }

    public static RefinementTask fromVerificationTaskWithDefaultBaselineWMM(VerificationTask task)
            throws InvalidConfigurationException {
        return new RefinementTaskBuilder()
                .withWitness(task.getWitness())
                .withConfig(task.getConfig())
                .build(task.getProgram(), task.getMemoryModel(), task.getProperty());
    }

    private Wmm createDefaultWmm() {
        Wmm baseline = new Wmm();
        RelationRepository repo = baseline.getRelationRepository();
        Relation rf = repo.getRelation(RF);

        if(baselines.contains(UNIPROC)) {
	        // ---- acyclic(po-loc | rf) ----
	        Relation poloc = repo.getRelation(POLOC);
	        Relation co = repo.getRelation(CO);
	        Relation fr = repo.getRelation(FR);
	        Relation porf = repo.union(poloc, rf);
	        Relation porfco =repo.union(porf, co);
	        Relation porfcofr = repo.union(porfco, fr);
	        baseline.addAxiom(new Acyclic(porfcofr));
        }
        if(baselines.contains(NO_OOTA)) {
            // ---- acyclic (dep | rf) ----
            Relation data = repo.getRelation(DATA);
            Relation ctrl = repo.getRelation(CTRL);
            Relation addr = repo.getRelation(ADDR);
            Relation dep = repo.union(data, addr);
            Relation hb = repo.union(repo.union(ctrl, dep), rf);
            baseline.addAxiom(new Acyclic(hb));
        }
        if(baselines.contains(ATOMIC_RMW)) {
    		// ---- empty (rmw & fre;coe) ----
            Relation rmw = repo.getRelation(RMW);
            Relation coe = repo.getRelation(COE);
            Relation fre = repo.getRelation(FRE);
            Relation frecoe = repo.composition(fre, coe);
            Relation rmwANDfrecoe = repo.intersection(rmw, frecoe);
            baseline.addAxiom(new Empty(rmwANDfrecoe));
        }
        return baseline;
    }

    // ==================== Builder =====================

    public static class RefinementTaskBuilder extends VerificationTaskBuilder {

        private Wmm baselineModel;

        @Override
        public RefinementTaskBuilder withWitness(WitnessGraph witness) {
            super.withWitness(witness);
            return this;
        }

        @Override
        public RefinementTaskBuilder withTarget(Arch target) {
            super.withTarget(target);
            return this;
        }

        @Override
        public RefinementTaskBuilder withConfig(Configuration config) {
            super.withConfig(config);
            return this;
        }

        public RefinementTaskBuilder withBaselineWMM(Wmm baselineModel) {
            this.baselineModel = baselineModel;
            return this;
        }

        @Override
        public RefinementTask build(Program program, Wmm memoryModel, EnumSet<Property> property) throws InvalidConfigurationException {
            return new RefinementTask(program, memoryModel, baselineModel, property, witness, config.build());
        }
    }
}