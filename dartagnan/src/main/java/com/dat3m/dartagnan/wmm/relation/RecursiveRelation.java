package com.dat3m.dartagnan.wmm.relation;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.Tuple;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Florian Furbach
 */
public class RecursiveRelation extends Relation {

    private Relation r1;

    public Relation getInner() {
        return r1;
    }

    @Override
    public List<Relation> getDependencies() {
        return Collections.singletonList(r1);
    }

    public RecursiveRelation(String name) {
        setName(name);
        term = name;
    }

    public static String makeTerm(String name){
        return name;
    }

    public void setConcreteRelation(Relation r1){
        r1.setName(name);
        this.r1 = r1;
        this.term = r1.getTerm();
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        obs.listen(r1, (may,must) -> buf.send(this, may, must));
    }

    @Override
    public void activate(Set<Tuple> news, VerificationTask task, WmmEncoder.Buffer buf) {
        buf.send(r1,news);
    }
}
