package com.dat3m.dartagnan.parsers.cat.visitors;

import com.dat3m.dartagnan.parsers.CatBaseVisitor;
import com.dat3m.dartagnan.parsers.CatParser;
import com.dat3m.dartagnan.parsers.CatVisitor;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.*;
import com.dat3m.dartagnan.wmm.relation.RecursiveRelation;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.RelationRepository;

import java.util.Set;

public class VisitorBase extends CatBaseVisitor<Object> implements CatVisitor<Object> {

    RelationRepository relationRepository;
    VisitorRelation relationVisitor;
    VisitorFilter filterVisitor;
    Wmm wmm;

    public VisitorBase(){
        this.wmm = new Wmm();
        relationRepository = wmm.getRelationRepository();
        filterVisitor = new VisitorFilter(this);
        relationVisitor = new VisitorRelation(this);
    }

    @Override
    public Object visitMcm(CatParser.McmContext ctx) {
        super.visitMcm(ctx);
        return wmm;
    }

    @Override
    public Object visitAcyclicDefinition(CatParser.AcyclicDefinitionContext ctx) {
        wmm.addAxiom(new Acyclic(ctx.e.accept(relationVisitor), ctx.negate != null, ctx.flag != null));
        return null;
    }

    @Override
    public Object visitIrreflexiveDefinition(CatParser.IrreflexiveDefinitionContext ctx) {
        wmm.addAxiom(new Irreflexive(ctx.e.accept(relationVisitor), ctx.negate != null, ctx.flag != null));
        return null;
    }

    @Override
    public Object visitEmptyDefinition(CatParser.EmptyDefinitionContext ctx) {
        wmm.addAxiom(new Empty(ctx.e.accept(relationVisitor), ctx.negate != null, ctx.flag != null));
        return null;
    }

    @Override
    public Object visitLetDefinition(CatParser.LetDefinitionContext ctx) {
        Relation r = ctx.e.accept(relationVisitor);
        if(r != null){
            wmm.getRelationRepository().nameRelation(r,ctx.n.getText());
        } else {
            FilterAbstract f = ctx.e.accept(filterVisitor);
            f.setName(ctx.n.getText());
            wmm.addFilter(f);
        }
        return null;
    }

    @Override
    public Object visitLetRecDefinition(CatParser.LetRecDefinitionContext ctx) {
        RecursiveRelation[] group = new RecursiveRelation[ctx.NAME().size()];

        for(int i = 0; i < group.length; i++) {
            group[i] = wmm.getRelationRepository().recursive(ctx.NAME().get(i).getText());
        }
        for(int i = 0; i < group.length; i++) {
            group[i].setConcreteRelation(ctx.expression().get(i).accept(relationVisitor));
        }

        return null;
    }
}

