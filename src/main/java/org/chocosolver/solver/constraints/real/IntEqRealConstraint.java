/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.real;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

/**
 * Channeling constraint between integers and reals, to avoid views
 *
 * @author Jean-Guillaume Fages
 * @since 07/04/2014
 */
public class IntEqRealConstraint extends Constraint {

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Channeling between integer variables intVars and real variables realVars.
     * Thus, for any i in [0,intVars.length-1], |intVars[i]-realVars[i]|< epsilon.
     * intVars.length must be equal to realVars.length.
     *
     * @param intVars  integer variables
     * @param realVars real variables
     * @param epsilon  precision parameter
     */
    public IntEqRealConstraint(IntVar[] intVars, RealVar[] realVars, double epsilon) {
        super("IntEqReal", new PropIntEqReal(intVars, realVars, epsilon));
    }

    /**
     * Channeling between an integer variable intVar and a real variable realVar.
     * Thus, |intVar-realVar|< epsilon.
     *
     * @param intVar  integer variable
     * @param realVar real variable
     * @param epsilon precision parameter
     */
    public IntEqRealConstraint(final IntVar intVar, final RealVar realVar, final double epsilon) {
        this(new IntVar[]{intVar}, new RealVar[]{realVar}, epsilon);
    }

    private static class PropIntEqReal extends Propagator<Variable> {

        private int n;
        private IntVar[] intVars;
        private RealVar[] realVars;
        private double epsilon;

        public PropIntEqReal(IntVar[] intVars, RealVar[] realVars, double epsilon) {
            super(ArrayUtils.append(intVars, realVars), PropagatorPriority.LINEAR, false);
            this.n = intVars.length;
            this.intVars = intVars;
            this.realVars = realVars;
            this.epsilon = epsilon;
            assert n == realVars.length;
        }

        @Override
        public void propagate(int evtmask) throws ContradictionException {
            for (int i = 0; i < n; i++) {
                IntVar intVar = intVars[i];
                RealVar realVar = realVars[i];
                realVar.updateBounds((double) intVar.getLB() - epsilon, (double) intVar.getUB() + epsilon, this);
                intVar.updateBounds((int) ceil(realVar.getLB() - epsilon), (int) floor(realVar.getUB() + epsilon), this);
                if (intVar.hasEnumeratedDomain()) {
                    realVar.updateBounds((double) intVar.getLB() - epsilon, (double) intVar.getUB() + epsilon, this);
                }
            }
        }

        @Override
        public ESat isEntailed() {
            assert intVars.length == realVars.length;
            boolean allInst = true;
            for (int i = 0; i < n; i++) {
                IntVar intVar = intVars[i];
                RealVar realVar = realVars[i];
                if ((realVar.getLB() < (double) intVar.getLB() - epsilon) || (realVar.getUB() > (double) intVar.getUB() + epsilon)) {
                    return ESat.FALSE;
                }
                if (!(intVar.isInstantiated() && realVar.isInstantiated())) {
                    allInst = false;
                }
            }
            return allInst ? ESat.TRUE : ESat.UNDEFINED;
        }

    }
}
