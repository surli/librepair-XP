/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables.impl.scheduler;

import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.iterators.EvtScheduler;

/**
 * Created by cprudhom on 17/06/15.
 * Project: choco.
 */
public class SetEvtScheduler implements EvtScheduler<SetEventType> {

    private final int[] DIS = new int[]{
            0,1, 4,5, -1, // ADD_TO_KER
            2,3, 4,5, -1, // REM_FROM_ENV
    };
    private int i = 0;
    private static final int[] IDX = new int[]{-1, 0, 5};

    public void init(SetEventType evt) {
        i = IDX[evt.ordinal()];
    }

    @Override
    public int select(int mask) {
        switch (mask) {
            case 1: // instantiate
                return 0;
            case 2: // lb or more
                return 2;
            case 3:
            case 255: // all
                return 4;
            default:
                throw new UnsupportedOperationException("Unknown case");
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
