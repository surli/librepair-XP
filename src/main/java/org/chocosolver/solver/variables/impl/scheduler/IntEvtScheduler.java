/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables.impl.scheduler;

import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.iterators.EvtScheduler;

/**
 * Created by cprudhom on 17/06/15.
 * Project: choco.
 */
public class IntEvtScheduler implements EvtScheduler<IntEventType> {

    private static final int[] DIS = new int[]{
            4, 5, -1, //REMOVE
            1, 2, 3, 5, -1,// INCLOW
            2, 5, -1, // DECUPP
            1, 5, -1, // BOUND
            0, 5, -1, // INSTANTIATE
    };
    private static final int[] IDX = new int[]{-1, 0, 3, 8, 11, 14};
    private int i = 0;

    public void init(IntEventType evt) {
        i = IDX[evt.ordinal()];
    }

    @Override
    public int select(int mask) {
        int b = Integer.lowestOneBit(mask);
        switch (b) {
            case 8: // INSTANTIATE
                return 0;
            case 4: // DECUPP and more
                return 2;
            case 2: // INCLOW (and DECUPP) or more
                b = Integer.lowestOneBit(mask >> 2);
                if (b == 1) { // DECUPP too
                    return 3;
                } else {
                    return 1;
                }
            default:
            case 1:  // REMOVE or more
                return 4;
        }
    }

    @Override
    public boolean hasNext() {
        return DIS[i] > -1;
    }

    @Override
    public int next() {
        return DIS[i++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
