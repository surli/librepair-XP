/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.unary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

/**
 * X >= C
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 16/06/11
 */
public class PropGreaterOrEqualXC extends Propagator<IntVar> {

    private final int constant;

    public PropGreaterOrEqualXC(IntVar var, int cste) {
        super(new IntVar[]{var}, PropagatorPriority.UNARY, false);
        this.constant = cste;
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        vars[0].updateLowerBound(constant, this);
        assert vars[0].getLB() >= constant;
        this.setPassive();
    }

    @Override
    public ESat isEntailed() {
        if (vars[0].getLB() >= constant) {
            return ESat.TRUE;
        } else if (vars[0].getUB() < constant) {
            return ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return vars[0].getName() + " >= " + constant;
    }

    @Override
    public boolean why(RuleStore ruleStore, IntVar var, IEventType evt, int value) {
        return ruleStore.addPropagatorActivationRule(this);
    }

}
