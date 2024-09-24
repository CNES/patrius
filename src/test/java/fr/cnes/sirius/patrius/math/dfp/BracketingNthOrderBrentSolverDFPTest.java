/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.dfp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.solver.AllowedSolution;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * Test case for {@link BracketingNthOrderBrentSolverDFP bracketing n<sup>th</sup> order Brent} solver.
 * 
 * @version $Id: BracketingNthOrderBrentSolverDFPTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BracketingNthOrderBrentSolverDFPTest {

    @Test(expected = NumberIsTooSmallException.class)
    public void testInsufficientOrder3() {
        new BracketingNthOrderBrentSolverDFP(this.relativeAccuracy, this.absoluteAccuracy,
            this.functionValueAccuracy, 1);
    }

    @Test
    public void testConstructorOK() {
        final BracketingNthOrderBrentSolverDFP solver =
            new BracketingNthOrderBrentSolverDFP(this.relativeAccuracy, this.absoluteAccuracy,
                this.functionValueAccuracy, 2);
        Assert.assertEquals(2, solver.getMaximalOrder());
    }

    @Test
    public void testConvergenceOnFunctionAccuracy() {
        final BracketingNthOrderBrentSolverDFP solver =
            new BracketingNthOrderBrentSolverDFP(this.relativeAccuracy, this.absoluteAccuracy,
                this.field.newDfp(1.0e-20), 20);
        final UnivariateDfpFunction f = new UnivariateDfpFunction(){
            @Override
            public Dfp value(final Dfp x) {
                final Dfp one = BracketingNthOrderBrentSolverDFPTest.this.field.getOne();
                final Dfp oneHalf = one.divide(2);
                final Dfp xMo = x.subtract(one);
                final Dfp xMh = x.subtract(oneHalf);
                final Dfp xPh = x.add(oneHalf);
                final Dfp xPo = x.add(one);
                return xMo.multiply(xMh).multiply(x).multiply(xPh).multiply(xPo);
            }
        };

        Dfp result = solver.solve(20, f, this.field.newDfp(0.2), this.field.newDfp(0.9),
            this.field.newDfp(0.4), AllowedSolution.BELOW_SIDE);
        Assert.assertTrue(f.value(result).abs().lessThan(solver.getFunctionValueAccuracy()));
        Assert.assertTrue(f.value(result).negativeOrNull());
        Assert.assertTrue(result.subtract(this.field.newDfp(0.5)).subtract(solver.getAbsoluteAccuracy())
            .positiveOrNull());
        result = solver.solve(20, f, this.field.newDfp(-0.9), this.field.newDfp(-0.2),
            this.field.newDfp(-0.4), AllowedSolution.ABOVE_SIDE);
        Assert.assertTrue(f.value(result).abs().lessThan(solver.getFunctionValueAccuracy()));
        Assert.assertTrue(f.value(result).positiveOrNull());
        Assert.assertTrue(result.add(this.field.newDfp(0.5)).subtract(solver.getAbsoluteAccuracy()).negativeOrNull());
    }

    @Test
    public void testNeta() {

        // the following test functions come from Beny Neta's paper:
        // "Several New Methods for solving Equations"
        // intern J. Computer Math Vol 23 pp 265-282
        // available here: http://www.math.nps.navy.mil/~bneta/SeveralNewMethods.PDF
        for (final AllowedSolution allowed : AllowedSolution.values()) {
            this.check(new UnivariateDfpFunction(){
                @Override
                public Dfp value(final Dfp x) {
                    return DfpMath.sin(x).subtract(x.divide(2));
                }
            }, 200, -2.0, 2.0, allowed);

            this.check(new UnivariateDfpFunction(){
                @Override
                public Dfp value(final Dfp x) {
                    return DfpMath.pow(x, 5).add(x)
                        .subtract(BracketingNthOrderBrentSolverDFPTest.this.field.newDfp(10000));
                }
            }, 200, -5.0, 10.0, allowed);

            this.check(new UnivariateDfpFunction(){
                @Override
                public Dfp value(final Dfp x) {
                    return x.sqrt().subtract(BracketingNthOrderBrentSolverDFPTest.this.field.getOne().divide(x))
                        .subtract(BracketingNthOrderBrentSolverDFPTest.this.field.newDfp(3));
                }
            }, 200, 0.001, 10.0, allowed);

            this.check(new UnivariateDfpFunction(){
                @Override
                public Dfp value(final Dfp x) {
                    return DfpMath.exp(x).add(x).subtract(BracketingNthOrderBrentSolverDFPTest.this.field.newDfp(20));
                }
            }, 200, -5.0, 5.0, allowed);

            this.check(new UnivariateDfpFunction(){
                @Override
                public Dfp value(final Dfp x) {
                    return DfpMath.log(x).add(x.sqrt())
                        .subtract(BracketingNthOrderBrentSolverDFPTest.this.field.newDfp(5));
                }
            }, 200, 0.001, 10.0, allowed);

            this.check(new UnivariateDfpFunction(){
                @Override
                public Dfp value(final Dfp x) {
                    return x.subtract(BracketingNthOrderBrentSolverDFPTest.this.field.getOne()).multiply(x).multiply(x)
                        .subtract(BracketingNthOrderBrentSolverDFPTest.this.field.getOne());
                }
            }, 200, -0.5, 1.5, allowed);
        }

    }

    private void check(final UnivariateDfpFunction f, final int maxEval, final double min, final double max,
                       final AllowedSolution allowedSolution) {
        final BracketingNthOrderBrentSolverDFP solver =
            new BracketingNthOrderBrentSolverDFP(this.relativeAccuracy, this.absoluteAccuracy,
                this.functionValueAccuracy, 20);
        final Dfp xResult = solver.solve(maxEval, f, this.field.newDfp(min), this.field.newDfp(max),
            allowedSolution);
        final Dfp yResult = f.value(xResult);
        switch (allowedSolution) {
            case ANY_SIDE:
                Assert.assertTrue(yResult.abs().lessThan(this.functionValueAccuracy.multiply(2)));
                break;
            case LEFT_SIDE: {
                final boolean increasing = f.value(xResult).add(this.absoluteAccuracy).greaterThan(yResult);
                Assert.assertTrue(increasing ? yResult.negativeOrNull() : yResult.positiveOrNull());
                break;
            }
            case RIGHT_SIDE: {
                final boolean increasing = f.value(xResult).add(this.absoluteAccuracy).greaterThan(yResult);
                Assert.assertTrue(increasing ? yResult.positiveOrNull() : yResult.negativeOrNull());
                break;
            }
            case BELOW_SIDE:
                Assert.assertTrue(yResult.negativeOrNull());
                break;
            case ABOVE_SIDE:
                Assert.assertTrue(yResult.positiveOrNull());
                break;
            default:
                // this should never happen
                throw new MathInternalError(null);
        }
    }

    @Before
    public void setUp() {
        this.field = new DfpField(50);
        this.absoluteAccuracy = this.field.newDfp(1.0e-45);
        this.relativeAccuracy = this.field.newDfp(1.0e-45);
        this.functionValueAccuracy = this.field.newDfp(1.0e-45);
    }

    private DfpField field;
    private Dfp absoluteAccuracy;
    private Dfp relativeAccuracy;
    private Dfp functionValueAccuracy;

}
