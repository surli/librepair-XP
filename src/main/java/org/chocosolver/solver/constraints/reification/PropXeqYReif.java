/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.reification;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.util.ESat;

/**
 * A propagator dedicated to express in a compact way: (x = y) &hArr; b
 *
 * @author Charles Prud'homme
 * @since 03/05/2016.
 */
public class PropXeqYReif extends Propagator<IntVar> {

    public PropXeqYReif(IntVar x, IntVar y, BoolVar r) {
        super(new IntVar[]{x, y, r}, PropagatorPriority.TERNARY, false);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (vars[2].getLB() == 1) {
            if (vars[0].isInstantiated()) {
                setPassive();
                vars[1].instantiateTo(vars[0].getValue(), this);
            } else if (vars[1].isInstantiated()) {
                setPassive();
                vars[0].instantiateTo(vars[1].getValue(), this);
            }
        } else if (vars[2].getUB() == 0) {
            if (vars[0].isInstantiated()) {
                if (vars[1].removeValue(vars[0].getValue(), this) || !vars[1].contains(vars[0].getValue())) {
                    setPassive();
                }
            } else if (vars[1].isInstantiated()) {
                if (vars[0].removeValue(vars[1].getValue(), this) || !vars[0].contains(vars[1].getValue())) {
                    setPassive();
                }
            }
        } else {
            if (vars[0].isInstantiated()) {
                if (vars[1].isInstantiated()) {
                    setPassive();
                    if (vars[0].getValue() == vars[1].getValue()) {
                        vars[2].instantiateTo(1, this);
                    } else {
                        vars[2].instantiateTo(0, this);
                    }
                } else if (!vars[1].contains(vars[0].getValue())) {
                    setPassive();
                    vars[2].instantiateTo(0, this);
                }
            } else {
                if (vars[1].isInstantiated()) {
                    if (!vars[0].contains(vars[1].getValue())) {
                        setPassive();
                        vars[2].instantiateTo(0, this);
                    }
                } else {
                    if (vars[0].getLB() > vars[1].getUB()
                            || vars[1].getLB() > vars[0].getUB()) {
                        setPassive();
                        vars[2].instantiateTo(0, this);
                    }
                }
            }
        }
    }

    @Override
    public ESat isEntailed() {
        if(isCompletelyInstantiated()){
            if(vars[2].isInstantiatedTo(1)){
                return ESat.eval(vars[0].getValue() == vars[1].getValue());
            }else{
                return ESat.eval(vars[0].getValue() != vars[1].getValue());
            }
        }
        return ESat.UNDEFINED;
    }

    @Override
    public boolean why(RuleStore ruleStore, IntVar var, IEventType evt, int value) {
        boolean nrules = ruleStore.addPropagatorActivationRule(this);
        if (var == vars[2]) {
            if (vars[2].isInstantiatedTo(1)) {
                nrules |= ruleStore.addFullDomainRule(vars[0]);
                nrules |= ruleStore.addFullDomainRule(vars[1]);
            } else {
                if (vars[0].isInstantiated()) {
                    nrules |= ruleStore.addRemovalRule(vars[1], vars[0].getValue());
                } else {
                    nrules |= ruleStore.addFullDomainRule(vars[1]);
                }
                if (vars[1].isInstantiated()) {
                    nrules |= ruleStore.addRemovalRule(vars[0], vars[1].getValue());
                } else {
                    nrules |= ruleStore.addFullDomainRule(vars[0]);
                }
            }
        } else {
            if (var == vars[0]) {
                nrules |= ruleStore.addFullDomainRule(vars[1]);
            } else if (var == vars[1]) {
                nrules |= ruleStore.addFullDomainRule(vars[0]);
            }
            nrules |= ruleStore.addFullDomainRule(vars[2]);
        }
        return nrules;
    }

    @Override
    public String toString() {
        return "(" + vars[0].getName() +" = " + vars[1].getName() + ") <=> "+vars[2].getName();
    }
}
