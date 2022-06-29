package com.dat3m.dartagnan.wmm.relation;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;

import java.util.Collections;
import java.util.List;

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
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        obs.listen(r1, (dis, en) -> buf.send(this,dis,en));
        obs.listen(this, (dis, en) -> buf.send(r1,dis,en));
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        obs.listen(this, news -> buf.send(r1,news));
    }
}
