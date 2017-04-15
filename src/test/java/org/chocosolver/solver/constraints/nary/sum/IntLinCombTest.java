/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.sum;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Arithmetic;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Operator;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.nary.cnf.PropTrue;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.propagation.PropagationEngineFactory;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;

import static java.util.Arrays.fill;
import static org.chocosolver.solver.Cause.Null;
import static org.chocosolver.solver.constraints.checker.DomainBuilder.buildFullDomains;
import static org.chocosolver.solver.search.strategy.Search.inputOrderLBSearch;
import static org.testng.Assert.*;

/**
 * User : cprudhom<br/>
 * Mail : cprudhom(a)emn.fr<br/>
 * Date : 23 avr. 2010<br/>
 */
public class IntLinCombTest {

    private static String operatorToString(Operator operator) {
        String opSt;
        switch (operator) {
            case EQ:
                opSt = "=";
                break;
            case NQ:
                opSt = "!=";
                break;
            case GE:
                opSt = ">=";
                break;
            case GT:
                opSt = ">";
                break;
            case LE:
                opSt = "<=";
                break;
            case LT:
                opSt = "<";
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return opSt;
    }

    public static void testOp(int n, int min, int max, int cMax, int seed, Operator operator) {
        Random random = new Random(seed);
        Model s = new Model();
        IntVar[] vars = new IntVar[n];
        int[] coeffs = new int[n];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = s.intVar("v_" + i, min, max, false);
            coeffs[i] = random.nextInt(cMax);
        }
        int constant = -random.nextInt(cMax);

        IntVar sum = s.intVar("scal", -99999999, 99999999, true);


        s.scalar(vars, coeffs, "=", sum).post();
        s.arithm(sum, operatorToString(operator), constant).post();

        s.getSolver().setSearch(inputOrderLBSearch(vars));

        while (s.getSolver().solve()) ;
    }

    @Test(groups="1s", timeOut=60000)
    public void testEq() {
        testOp(2, 0, 5, 5, 29091982, Operator.EQ);
    }

    @Test(groups="1s", timeOut=60000)
    public void testGeq() {
        testOp(2, 0, 5, 5, 29091981, Operator.GE);
    }

    @Test(groups="1s", timeOut=60000)
    public void testLeq() {
        testOp(2, 0, 5, 5, 29091981, Operator.LE);
    }

    @Test(groups="1s", timeOut=60000)
    public void testNeq() {
        testOp(2, 0, 5, 5, 29091981, Operator.NQ);
    }


    protected Model sum(int[][] domains, int[] coeffs, int b, int op) {
        Model model = new Model();
        IntVar[] bins = new IntVar[domains.length];
        for (int i = 0; i < domains.length; i++) {
            bins[i] = model.intVar("v_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
        }
        String opname = "=";
        if (op != 0) {
            if (op > 0) {
                opname = ">=";
            } else {
                opname = "<=";
            }
        }
        IntVar sum = model.intVar("scal", -99999999, 99999999, true);
        model.scalar(bins, coeffs, "=", sum).post();
        model.arithm(sum, opname, b).post();
        model.getSolver().setSearch(inputOrderLBSearch(bins));
        return model;
    }

    protected Model intlincomb(int[][] domains, int[] coeffs, int b, int op) {
        Model model = new Model();
        IntVar[] bins = new IntVar[domains.length];
        for (int i = 0; i < domains.length; i++) {
            bins[i] = model.intVar("v_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
        }
        String opname = "=";
        if (op != 0) {
            if (op > 0) {
                opname = ">=";
            } else {
                opname = "<=";
            }
        }
        IntVar sum = model.intVar("scal", -99999999, 99999999, true);
        model.scalar(bins, coeffs, "=", sum).post();
        model.arithm(sum, opname, b).post();
        model.getSolver().setSearch(inputOrderLBSearch(bins));
        return model;
    }

    @Test(groups="10s", timeOut=300000)
    public void testSumvsIntLinCombTest() {
        Random rand = new Random();
        for (int seed = 0; seed < 20; seed++) {
            rand.setSeed(seed);
            int n = 1 + rand.nextInt(6);
            int min = -10 + rand.nextInt(20);
            int max = min + rand.nextInt(20);
            int[][] domains = buildFullDomains(n, min, max, rand, 1.0, false);
            int[] coeffs = new int[n];
            for (int i = 0; i < n; i++) {
                coeffs[i] = -25 + rand.nextInt(50);
            }
            int lb = -50 + rand.nextInt(100);
            int op = -1 + rand.nextInt(3);

            Model sum = sum(domains, coeffs, lb, op);
            Model intlincomb = intlincomb(domains, coeffs, lb, op);

            while (sum.getSolver().solve()) ;
            while (intlincomb.getSolver().solve()) ;
            assertEquals(sum.getSolver().getSolutionCount(), intlincomb.getSolver().getSolutionCount());
            assertEquals(sum.getSolver().getNodeCount(), intlincomb.getSolver().getNodeCount());
        }
    }

    @Test(groups="1s", timeOut=60000)
    public void testUSum1() {
        Model sumleq = sum(new int[][]{{-2, 3}}, new int[]{-2}, -6, -1);
        while (sumleq.getSolver().solve()) ;
    }

    /**
     * Default propagation test:
     * When an opposite var is declared, the lower (resp. upper) bound modification
     * should be transposed in upper (resp. lower) bound event...
     */
    @Test(groups="1s", timeOut=60000)
    public void testUSum2() throws ContradictionException {
        Model sum = sum(new int[][]{{-2, 7}, {-1, 6}, {2}, {-2, 5}, {-2, 4}, {-2, 6}}, new int[]{-7, 13, -3, -18, -24, 1}, 30, 0);
        PropagationEngineFactory.DEFAULT.make(sum);
        Variable[] vars = sum.getVars();
        ((IntVar) vars[0]).instantiateTo(-2, Cause.Null);
        ((IntVar) vars[1]).instantiateTo(-1, Cause.Null);
        sum.getSolver().propagate();
//        sum.getResolver().timeStamp++;
        ((IntVar) vars[2]).removeValue(-2, Cause.Null);
        sum.getSolver().propagate();
        Assert.assertTrue(vars[2].isInstantiated());
    }

    @Test(groups="1s", timeOut=60000)
    public void testIss237_1() {
        Model model = new Model();
        BoolVar[] bs = model.boolVarArray("b", 3);
        model.scalar(bs, new int[]{1, 2, 3}, "=", 2).post();
        model.getSolver().showSolutions();
        while (model.getSolver().solve()) ;
    }

    @Test(groups="1s", timeOut=60000)
    public void testS1_coeff_null() {
        Model model = new Model();
        model.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 0;
            }
        });
        IntVar[] ivars = model.intVarArray("V", 4, 0, 5, false);
        int[] coeffs = new int[]{1, 0, 0, 2};
        IntVar res = model.intVar("R", 0, 10, false);
        Constraint c = model.scalar(ivars, coeffs, "=", res);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropScalar);
        Assert.assertEquals(3, p.getNbVars());
    }

    @Test(groups="1s", timeOut=60000)
    public void testS2_coeff_null() {
        Model model = new Model();
        model.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 0;
            }
        });
        IntVar[] ivars = model.intVarArray("V", 4, 0, 5, false);
        ivars[2] = ivars[1];
        int[] coeffs = new int[]{1, 1, -1, 2};
        IntVar res = model.intVar("R", 0, 10, false);
        Constraint c = model.scalar(ivars, coeffs, "=", res);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropScalar);
        Assert.assertEquals(3, p.getNbVars());
    }

    @Test(groups="1s", timeOut=60000)
    public void testD1() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 4, 0, 5, false);
        int[] coeffs = new int[]{1, 1, 1, 1};
        IntVar res = model.intVar("R", 0, 10, false);
        Constraint c = model.scalar(ivars, coeffs, "=", res);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSum);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD2() {
        Model model = new Model();
        IntVar[] ivars = model.boolVarArray("V", 4);
        int[] coeffs = new int[]{1, 1, 1, 1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSumBool);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD3() {
        Model model = new Model();
        IntVar[] ivars = model.boolVarArray("V", 4);
        int[] coeffs = new int[]{-1, -1, -1, -1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSumBool);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD4() {
        Model model = new Model();
        IntVar[] ivars = model.boolVarArray("V", 4);
        int[] coeffs = new int[]{1, -1, 1, 1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSumBool);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD5() {
        Model model = new Model();
        IntVar[] ivars = model.boolVarArray("V", 4);
        int[] coeffs = new int[]{-1, 1, -1, -1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSumBool);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD6() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 4, 0, 1, false);
        ivars[1] = model.intVar("X", 0, 2, false);
        int[] coeffs = new int[]{1, -1, 1, 1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSumBool);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD7() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 4, 0, 1, false);
        ivars[1] = model.intVar("X", 0, 2, false);
        int[] coeffs = new int[]{-1, 1, -1, -1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSum);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD8() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 4, 0, 1, false);
        ivars[2] = model.intVar("X", 0, 2, false);
        int[] coeffs = new int[]{1, -1, 1, 1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSum);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD9() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 4, 0, 1, false);
        ivars[2] = model.intVar("X", 0, 2, false);
        int[] coeffs = new int[]{-1, 1, -1, -1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropSumBool);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD10() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 2, 0, 2, false);
        int[] coeffs = new int[]{1, 1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertTrue(c instanceof Arithmetic);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD11() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 2, 0, 2, false);
        int[] coeffs = new int[]{1, -1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertTrue(c instanceof Arithmetic);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD12() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 2, 0, 2, false);
        int[] coeffs = new int[]{-1, 1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertTrue(c instanceof Arithmetic);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD13() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 2, 0, 2, false);
        int[] coeffs = new int[]{-1, -1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertTrue(c instanceof Arithmetic);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD14() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 1, 0, 2, false);
        int[] coeffs = new int[]{1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertTrue(c instanceof Arithmetic);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD15() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 1, 0, 2, false);
        int[] coeffs = new int[]{-1};
        Constraint c = model.scalar(ivars, coeffs, "=", 0);
        Assert.assertTrue(c instanceof Arithmetic);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD16() {
        Model model = new Model();
        IntVar[] ivars = model.intVarArray("V", 1, 0, 2, false);
        int[] coeffs = new int[]{1};
        Constraint c = model.scalar(ivars, coeffs, "=", ivars[0]);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropTrue);
    }

    @Test(groups="1s", timeOut=60000)
    public void testD20() {
        Model model = new Model();
        model.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 0;
            }
        });
        IntVar[] ivars = model.intVarArray("V", 4, 0, 5, false);
        int[] coeffs = new int[]{1, 2, 2, 1};
        IntVar res = model.intVar("R", 0, 10, false);
        Constraint c = model.scalar(ivars, coeffs, "=", res);
        Assert.assertEquals(c.getPropagators().length, 1);
        Propagator p = c.getPropagator(0);
        Assert.assertTrue(p instanceof PropScalar);
    }

    @Test(groups="1s", timeOut=60000)
    public void testExt1() {
        Model s1 = new Model();
        s1.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 0;
            }
        });
        {
            BoolVar[] bs = s1.boolVarArray("b", 5);
            s1.sum(bs, "!=", 3).post();
        }
        Model s2 = new Model();
        s2.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 1000;
            }
        });
        {
            BoolVar[] bs = s2.boolVarArray("b", 5);
            s2.sum(bs, "!=", 3).post();
        }
        while (s1.getSolver().solve()) ;
        while (s2.getSolver().solve()) ;
        assertEquals(s2.getSolver().getSolutionCount(), s1.getSolver().getSolutionCount());
        assertEquals(s2.getSolver().getNodeCount(), s1.getSolver().getNodeCount());
    }

    @Test(groups="1s", timeOut=60000)
    public void testExt2() {
        Model s1 = new Model();
        s1.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 0;
            }
        });
        {
            BoolVar[] bs = s1.boolVarArray("b", 5);
            s1.sum(bs, "<=", 3).post();
        }
        Model s2 = new Model();
        s2.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 1000;
            }
        });
        {
            BoolVar[] bs = s2.boolVarArray("b", 5);
            s2.sum(bs, "<=", 3).post();
        }
        while (s1.getSolver().solve()) ;
        while (s2.getSolver().solve()) ;
        assertEquals(s2.getSolver().getSolutionCount(), s1.getSolver().getSolutionCount());
        assertEquals(s2.getSolver().getNodeCount(), s1.getSolver().getNodeCount());
    }

    @Test(groups="1s", timeOut=60000)
    public void testExt3() {
        Model s1 = new Model();
        s1.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 0;
            }
        });
        {
            BoolVar[] bs = s1.boolVarArray("b", 3);
            BoolVar r = s1.boolVar("r");
            s1.scalar(bs, new int[]{-1, -1, -1}, "<=", -2).reifyWith(r);
        }
        Model s2 = new Model();
        s2.set(new Settings() {
            @Override
            public int getMaxTupleSizeForSubstitution() {
                return 1000;
            }
        });
        {
            BoolVar[] bs = s2.boolVarArray("b", 3);
            BoolVar r = s2.boolVar("r");
            s2.scalar(bs, new int[]{-1, -1, -1}, "<=", -2).reifyWith(r);
        }
        s1.getSolver().showDecisions();
        s2.getSolver().showDecisions();
        while (s1.getSolver().solve()) ;
        while (s2.getSolver().solve()) ;
        assertEquals(s2.getSolver().getSolutionCount(), s1.getSolver().getSolutionCount());
        assertEquals(s2.getSolver().getNodeCount(), s1.getSolver().getNodeCount());
    }

    @Test(groups="10s", timeOut=300000)
    public void testB1() {
        Model model = new Model();
        model.set(new Settings() {
            @Override
            public short[] getCoarseEventPriority() {
                return new short[]{0, 0, 0, 0, 1, 2, 3};
            }
        });
        int n = 20;
        BoolVar[] bs = model.boolVarArray("b", n);
        int[] cs = new int[n];
        int k = (int) (n * .7);
        fill(cs, 0, n, 1);
        fill(cs, k, n, -1);
        IntVar sum = model.intVar("S", -n / 2, n / 2, true);
        model.scalar(bs, cs, "=", sum).post();
        model.getSolver().setSearch(inputOrderLBSearch(bs));
//        IOutputFactory.showDecisions(solver);
        while (model.getSolver().solve()) ;
    }


    @Test(groups="1s", timeOut=60000)
    public void testB2() throws ContradictionException {
        Model model = new Model();
        int n = 3;
        BoolVar[] bs = model.boolVarArray("b", n);
        int[] cs = new int[n];
        fill(cs, 0, n, -1);
        model.scalar(bs, cs, "<=", -2).post();
        model.getSolver().propagate();
        bs[2].setToFalse(Null);
        bs[0].setToTrue(Null);
        model.getSolver().propagate();
        assertTrue(bs[1].isInstantiatedTo(1));
    }


    @Test(groups="1s", timeOut=60000)
    public void testB3() {
        Model model = new Model();
        model.scalar(new IntVar[]{model.intVar(1), model.intVar(3)}, new int[]{1, -1}, "!=", 0).post();
        try {
            model.getSolver().propagate();
        } catch (ContradictionException e) {
            fail();
        }
    }

    @Test(groups="1s", timeOut=60000)
    public void testB4() {
        Model model = new Model();
        IntVar[] X = model.intVarArray("X", 1, 1, 3, false);
        model.scalar(X, new int[]{-1}, "<=", 2).post();
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 3);

    }

    @Test(groups="1s", timeOut=60000)
    public void testB5() throws ContradictionException {
        Model model = new Model();
        IntVar[] X = new IntVar[3];
        X[0] = model.intVar("X1", 6, 46, false);
        X[1] = model.intVar("X2", 6, 56, false);
        X[2] = model.intVar("X3", -1140, 1140, true);
        model.scalar(X, new int[]{1, -1, -1}, "=", 0).post();
        model.getSolver().propagate();
        X[1].updateUpperBound(46, Null);
        model.getSolver().propagate();
        assertEquals(X[2].getLB(), -40);
        assertEquals(X[2].getUB(), 40);

    }


    @Test(groups="1s", timeOut=60000)
    public void testB6() throws ContradictionException {
        Model model = new Model();
        IntVar[] X = new IntVar[2];
        X[0] = model.intVar("X1", 1, 3, false);
        X[1] = model.intVar("X2", 2, 5, false);
        model.scalar(X, new int[]{2, 3}, "<=", 10).post();
        model.getSolver().propagate();
        assertEquals(X[0].getLB(), 1);
        assertEquals(X[0].getUB(), 2);
        assertEquals(X[1].getLB(), 2);
        assertEquals(X[1].getUB(), 2);
    }

    @Test(groups="1s", timeOut=60000)
    public void testB61() throws ContradictionException {
        Model model = new Model();
        IntVar[] X = new IntVar[2];
        X[0] = model.intVar("X1", 1, 3, false);
        X[1] = model.intVar("X2", 2, 5, false);
        model.scalar(X, new int[]{-2, -3}, ">=", -10).post();
        model.getSolver().propagate();
        assertEquals(X[0].getLB(), 1);
        assertEquals(X[0].getUB(), 2);
        assertEquals(X[1].getLB(), 2);
        assertEquals(X[1].getUB(), 2);
    }

    @Test(groups="1s", timeOut=60000)
    public void testB7() throws ContradictionException {
        Model model = new Model();
        IntVar[] X = new IntVar[2];
        X[0] = model.intVar("X1", 0, 3, false);
        X[1] = model.intVar("X2", 1, 5, false);
        model.scalar(X, new int[]{2, 3}, ">=", 10).post();
        model.getSolver().propagate();
        assertEquals(X[0].getLB(), 0);
        assertEquals(X[0].getUB(), 3);
        assertEquals(X[1].getLB(), 2);
        assertEquals(X[1].getUB(), 5);
    }

    @Test(groups="1s", timeOut=60000)
    public void testB71() throws ContradictionException {
        Model model = new Model();
        IntVar[] X = new IntVar[2];
        X[0] = model.intVar("X1", 0, 3, false);
        X[1] = model.intVar("X2", 1, 5, false);
        model.scalar(X, new int[]{-2, -3}, ">=", -10).post();
        model.getSolver().propagate();
        assertEquals(X[0].getLB(), 0);
        assertEquals(X[0].getUB(), 3);
        assertEquals(X[1].getLB(), 1);
        assertEquals(X[1].getUB(), 3);
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL1() {
        Model model = new Model();
        model.sum(new IntVar[]{model.intVar(3), model.intVar(-4)}, "<", 0).post();
        assertTrue(model.getSolver().solve());
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL2() {
        Model model = new Model();
        model.sum(new IntVar[]{model.intVar(3), model.intVar(-4)}, "<=", 0).post();
        assertTrue(model.getSolver().solve());
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL3() {
        Model model = new Model();
        model.sum(new IntVar[]{model.intVar(-3), model.intVar(4)}, ">", 0).post();
        assertTrue(model.getSolver().solve());
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL4() {
        Model model = new Model();
        model.sum(new IntVar[]{model.intVar(-3), model.intVar(4)}, ">=", 0).post();
        assertTrue(model.getSolver().solve());
    }

    @Test(groups="1s", timeOut=60000)
    public void testJG1() {
        Model model = new Model("TestChoco 3.3.2 Briot");
        IntVar[] var = model.intVarArray("var", 3, new int[]{30, 60});
        model.sum(new IntVar[]{var[0], var[1], var[2]}, ">=", 60).post();
        model.getSolver().setSearch(inputOrderLBSearch(var));
        model.getSolver().showStatistics();
        model.getSolver().showSolutions();
        model.getSolver().solve();
    }

    @Test(groups="1s", timeOut=60000)
    public void testJG2() {
        Model model = new Model("TestChoco 3.3.2 Briot");
        IntVar[] var = model.intVarArray("var", 3, new int[]{30, 60});
        model.sum(new IntVar[]{var[0], var[1], var[2]}, "<=", 120).post();
        model.getSolver().setSearch(inputOrderLBSearch(var));
        model.getSolver().showStatistics();
        model.getSolver().showSolutions();
        model.getSolver().solve();
    }

    @Test(groups="1s", timeOut=60000)
    public void testRFP1() {
        Model model = new Model("Test reduce frequency propagation");
        IntVar[] var = model.intVarArray("var", 5, 0, 5);
        model.sum(var, "<=", 20).post();
        model.getSolver().setSearch(inputOrderLBSearch(var));
        model.getSolver().findAllSolutions();
        Assert.assertEquals(model.getSolver().getSolutionCount(), 7650);
        Assert.assertEquals(model.getSolver().getNodeCount(), 15299);
    }

    @Test(groups="1s", timeOut=60000)
    public void testRFP2() {
        Model model = new Model("Test reduce frequency propagation");
        IntVar[] var = model.intVarArray("var", 5, 0, 5);
        model.sum(var, ">=", 5).post();
        model.getSolver().setSearch(inputOrderLBSearch(var));
        model.getSolver().findAllSolutions();
        Assert.assertEquals(model.getSolver().getSolutionCount(), 7650);
        Assert.assertEquals(model.getSolver().getNodeCount(), 15299);
    }

    @Test(groups="1s", timeOut=60000)
    public void testRFP3() {
        Model model = new Model("Test reduce frequency propagation");
        IntVar[] var = model.intVarArray("var", 5, 0, 5);
        model.sum(var, "=", 12).post();
        model.getSolver().setSearch(inputOrderLBSearch(var));
        model.getSolver().findAllSolutions();
        Assert.assertEquals(model.getSolver().getSolutionCount(), 780);
        Assert.assertEquals(model.getSolver().getNodeCount(), 1559);
	}

	@Test(groups="1s", timeOut=60000)
	public void testOpp1() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		BoolVar b = m.boolVar();
		Constraint c = m.sum(row, "<=", 5);
		c.reifyWith(b);
		Constraint oc = c.getOpposite();
		Assert.assertTrue(c instanceof SumConstraint);
		Assert.assertTrue(c.getPropagator(0) instanceof PropSum);
		Assert.assertTrue(oc instanceof SumConstraint);
		Assert.assertTrue(oc.getPropagator(0) instanceof PropSum);
		PropSum poc = (PropSum) oc.getPropagator(0);
		Assert.assertEquals(poc.o, Operator.GE);
		Assert.assertEquals(poc.b, 6);
		Assert.assertEquals(oc.getOpposite(), c);
	}

	@Test(groups="1s", timeOut=60000)
	public void testOpp1strict() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		BoolVar b = m.boolVar();
		Constraint c = m.sum(row, "<", 5);
		c.reifyWith(b);
		Constraint oc = c.getOpposite();
		Assert.assertTrue(c instanceof SumConstraint);
		Assert.assertTrue(c.getPropagator(0) instanceof PropSum);
		Assert.assertTrue(oc instanceof SumConstraint);
		Assert.assertTrue(oc.getPropagator(0) instanceof PropSum);
		PropSum poc = (PropSum) oc.getPropagator(0);
		Assert.assertEquals(poc.o, Operator.GE);
		Assert.assertEquals(poc.b, 5);
		Assert.assertEquals(oc.getOpposite(), c);
	}

    @Test(groups="1s", timeOut=60000)
    public void testOpp2() {
        Model m = new Model();
        BoolVar row[] = m.boolVarArray("r", 6);
        BoolVar b = m.boolVar();
        Constraint c = m.sum(row, "=", 1);
        c.reifyWith(b);
        Constraint oc = c.getOpposite();
        Assert.assertTrue(c instanceof SumConstraint);
        Assert.assertTrue(c.getPropagator(0) instanceof PropSumBool);
        Assert.assertTrue(oc instanceof SumConstraint);
        Assert.assertTrue(oc.getPropagator(0) instanceof PropSumBool);
        PropSumBool poc = (PropSumBool) oc.getPropagator(0);
        Assert.assertEquals(poc.o, Operator.NQ);
        Assert.assertEquals(oc.getOpposite(), c);
    }

    @Test(groups="1s", timeOut=60000)
    public void testOpp3() {
        Model m = new Model();
        BoolVar row[] = m.boolVarArray("r", 20);
        BoolVar b = m.boolVar();
        Constraint c = m.sum(row, "!=", 10);
        c.reifyWith(b);
        Constraint oc = c.getOpposite();
        Assert.assertTrue(c instanceof SumConstraint);
        Assert.assertTrue(c.getPropagator(0) instanceof PropSumBoolIncr);
        Assert.assertTrue(oc instanceof SumConstraint);
        Assert.assertTrue(oc.getPropagator(0) instanceof PropSumBoolIncr);
        PropSumBoolIncr poc = (PropSumBoolIncr) oc.getPropagator(0);
        Assert.assertEquals(poc.o, Operator.EQ);
        Assert.assertEquals(oc.getOpposite(), c);
    }

    @Test(groups="1s", timeOut=60000)
    public void testOpp4() {
        Model m = new Model();
        IntVar row[] = m.intVarArray("r", 3, 0, 5);
        BoolVar b = m.boolVar();
        Constraint c = m.scalar(row, new int[]{3,4,5}, ">=", 10);
        c.reifyWith(b);
        Constraint oc = c.getOpposite();
        Assert.assertTrue(c instanceof SumConstraint);
        Assert.assertTrue(c.getPropagator(0) instanceof PropScalar);
        Assert.assertTrue(oc instanceof SumConstraint);
        Assert.assertTrue(oc.getPropagator(0) instanceof PropScalar);
        PropScalar poc = (PropScalar) oc.getPropagator(0);
		Assert.assertEquals(poc.o, Operator.LE);
		Assert.assertEquals(poc.b, 9);
        Assert.assertEquals(oc.getOpposite(), c);
    }

	@Test(groups="1s", timeOut=60000)
	public void testOpp4strict() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		BoolVar b = m.boolVar();
		Constraint c = m.scalar(row, new int[]{3,4,5}, ">", 9);
		c.reifyWith(b);
		Constraint oc = c.getOpposite();
		Assert.assertTrue(c instanceof SumConstraint);
		Assert.assertTrue(c.getPropagator(0) instanceof PropScalar);
		Assert.assertTrue(oc instanceof SumConstraint);
		Assert.assertTrue(oc.getPropagator(0) instanceof PropScalar);
		PropScalar poc = (PropScalar) oc.getPropagator(0);
		Assert.assertEquals(poc.o, Operator.LE);
		Assert.assertEquals(poc.b, 9);
		Assert.assertEquals(oc.getOpposite(), c);
	}

	@Test(groups="1s", timeOut=60000)
	public void testGT() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		m.scalar(row, new int[]{3,4,5}, ">", 9).post();
		while(m.getSolver().solve()){
			int tot = row[0].getValue()*3+row[1].getValue()*4+row[2].getValue()*5;
			Assert.assertTrue(tot>9);
		}
		Assert.assertTrue(m.getSolver().getSolutionCount()>0);
	}

	@Test(groups="1s", timeOut=60000)
	public void testGE() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		m.scalar(row, new int[]{3,4,5}, ">=", 9).post();
		while(m.getSolver().solve()){
			int tot = row[0].getValue()*3+row[1].getValue()*4+row[2].getValue()*5;
			Assert.assertTrue(tot>=9);
		}
		Assert.assertTrue(m.getSolver().getSolutionCount()>0);
	}

	@Test(groups="1s", timeOut=60000)
	public void testEQ() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		m.scalar(row, new int[]{3,4,5}, "=", 9).post();
		while(m.getSolver().solve()){
			int tot = row[0].getValue()*3+row[1].getValue()*4+row[2].getValue()*5;
			Assert.assertTrue(tot==9);
		}
		Assert.assertTrue(m.getSolver().getSolutionCount()==2);
	}

	@Test(groups="1s", timeOut=60000)
	public void testNE() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		m.scalar(row, new int[]{3,4,5}, "!=", 9).post();
		while(m.getSolver().solve()){
			int tot = row[0].getValue()*3+row[1].getValue()*4+row[2].getValue()*5;
			Assert.assertTrue(tot!=9);
		}
		Assert.assertTrue(m.getSolver().getSolutionCount()>0);
	}

	@Test(groups="1s", timeOut=60000)
	public void testLT() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		m.scalar(row, new int[]{3,4,5}, "<", 9).post();
		while(m.getSolver().solve()){
			int tot = row[0].getValue()*3+row[1].getValue()*4+row[2].getValue()*5;
			System.out.println(row[0]+" / "+row[1]+" / "+row[2]);
			System.out.println(tot);
			Assert.assertTrue(tot<9);
		}
		Assert.assertTrue(m.getSolver().getSolutionCount()>0);
	}

	@Test(groups="1s", timeOut=60000)
	public void testLE() {
		Model m = new Model();
		IntVar row[] = m.intVarArray("r", 3, 0, 5);
		m.scalar(row, new int[]{3,4,5}, "<=", 9).post();
		while(m.getSolver().solve()){
			int tot = row[0].getValue()*3+row[1].getValue()*4+row[2].getValue()*5;
			Assert.assertTrue(tot<=9);
		}
		Assert.assertTrue(m.getSolver().getSolutionCount()>0);
	}

    @DataProvider(name = "decomp")
    public Object[][] decomp(){
        return new Object[][]{
                {true, 18},
                {true, 19},
                {true, 20},
                {false, 18},
                {false, 19},
                {false, 20},
        };
    }

    @Test(groups="1s", timeOut=60000, dataProvider = "decomp")
    public void testDec1(boolean decomp, int size) {
        Model m = new Model();
        m.set(new Settings() {
            @Override
            public boolean enableDecompositionOfBooleanSum() {
                return decomp;
            }
        });
        BoolVar row[] = m.boolVarArray("r", size);
        m.sum(row, "<", 10).post();
        m.getSolver().setSearch(Search.inputOrderLBSearch(row));
        m.getSolver().findAllSolutions();
        m.getSolver().printShortStatistics();
    }

    @Test(groups="1s", timeOut=60000, dataProvider = "decomp")
    public void testDec2(boolean decomp, int size) {
        Model m = new Model();
        m.set(new Settings() {
            @Override
            public boolean enableDecompositionOfBooleanSum() {
                return decomp;
            }
        });
        BoolVar row[] = m.boolVarArray("r", size);
        BoolVar b = m.boolVar();
        m.sum(row, "<", 10).reifyWith(b);
        m.getSolver().setSearch(Search.inputOrderLBSearch(row), Search.inputOrderLBSearch(b));
        while(m.getSolver().solve());
        m.getSolver().printShortStatistics();
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL082016() {
        for (int repeat = 0; repeat < 100; repeat++) {
            Model m1 = new Model();
            int c1 = 0;
            {
                IntVar i = m1.intVar("i", -2, -1);
                IntVar j = m1.intVar("j", new int[]{-4, -1, 0, 1, 4});
                IntVar sum = m1.intVar("sum", new int[]{-4, 4});
				m1.sum(new IntVar[]{i, j, m1.intVar(1)}, "!=", sum).post();
                Solver s = m1.getSolver();
                s.setSearch(Search.randomSearch(new IntVar[]{i, j, sum}, repeat));
                while (s.solve()) {
                    c1++;
                }
            }
            Model m2 = new Model();
            int c2 = 0;
            {
                IntVar i = m2.intVar("i", -2, -1);
                IntVar j = m2.intVar("j", new int[]{-4, -1, 0, 1, 4});
                IntVar sum = m2.intVar("sum", new int[]{-4, 4});
                IntVar sumX = m2.intVar("sum", -40, 40);
                m2.sum(new IntVar[]{i, j, m2.intVar(1)}, "=", sumX).post();
                m2.arithm(sum, "!=", sumX).post();
                Solver s = m2.getSolver();
                s.setSearch(Search.randomSearch(new IntVar[]{i, j, sum}, repeat));
                while (s.solve()) {
                    c2++;
                }
			}
			assertEquals(c1,c2);
			assertEquals(c1,18);
        }
    }
}
