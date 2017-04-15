/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.expression.continuous.relational;

import org.chocosolver.solver.constraints.Constraint;

/**
 * relational expression over continuous variables
 * <p>
 * Project: choco-solver.
 *
 * @author Charles Prud'homme
 * @since 28/04/2016.
 */
public interface CReExpression {

    /**
     * List of available operator for relational expression
     */
    enum Operator {
        /**
         * less than
         */
        LT {
        },
        /**
         * less than or equal to
         */
        LE {
        },
        /**
         * greater than
         */
        GE {
        },
        /**
         * greater than or equal to
         */
        GT {
        },
        /**
         * equal to
         */
        EQ {
        },

    }


    /**
     * @param p the precision to consider when creating intermediate variable is needed
     * @return the topmost constraint representing the expression. If needed, a call to this method
     * creates additional variables and posts additional constraints.
     */
    Constraint ibex(double p);
}
