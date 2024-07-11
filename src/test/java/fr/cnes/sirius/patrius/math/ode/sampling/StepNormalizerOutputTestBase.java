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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.sampling;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;

/** Base class for step normalizer output tests. */
public abstract class StepNormalizerOutputTestBase
    implements FirstOrderDifferentialEquations, FixedStepHandler {

    /** The normalized output time values. */
    private List<Double> output;

    /**
     * Returns the start time.
     * 
     * @return the start time
     */
    protected abstract double getStart();

    /**
     * Returns the end time.
     * 
     * @return the end time
     */
    protected abstract double getEnd();

    /**
     * Returns the expected normalized output time values for increment mode.
     * 
     * @return the expected normalized output time values for increment mode
     */
    protected abstract double[] getExpInc();

    /**
     * Returns the expected reversed normalized output time values for
     * increment mode.
     * 
     * @return the expected reversed normalized output time values for
     *         increment mode
     */
    protected abstract double[] getExpIncRev();

    /**
     * Returns the expected normalized output time values for multiples mode.
     * 
     * @return the expected normalized output time values for multiples mode
     */
    protected abstract double[] getExpMul();

    /**
     * Returns the expected reversed normalized output time values for
     * multiples mode.
     * 
     * @return the expected reversed normalized output time values for
     *         multiples mode
     */
    protected abstract double[] getExpMulRev();

    /**
     * Returns the offsets for the unit tests below, in the order they are
     * given below. For each test, the left and right offsets are returned.
     * 
     * @return the offsets for the unit tests below, in the order they are
     *         given below
     */
    protected abstract int[][] getO();

    /**
     * Get the array, given left and right offsets.
     * 
     * @param a
     *        the input array
     * @param offsetL
     *        the left side offset
     * @param offsetR
     *        the right side offset
     * @return the modified array
     */
    private double[] getArray(final double[] a, final int offsetL, final int offsetR) {
        final double[] copy = new double[a.length - offsetR - offsetL];
        System.arraycopy(a, offsetL, copy, 0, copy.length);
        return copy;
    }

    @Test
    public void testIncNeither()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpInc(), this.getO()[0][0], this.getO()[0][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.NEITHER, exp, false);
    }

    @Test
    public void testIncNeitherRev()
                                   throws DimensionMismatchException, NumberIsTooSmallException,
                                   MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpIncRev(), this.getO()[1][0], this.getO()[1][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.NEITHER, exp, true);
    }

    @Test
    public void testIncFirst()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpInc(), this.getO()[2][0], this.getO()[2][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.FIRST, exp, false);
    }

    @Test
    public void testIncFirstRev()
                                 throws DimensionMismatchException, NumberIsTooSmallException,
                                 MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpIncRev(), this.getO()[3][0], this.getO()[3][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.FIRST, exp, true);
    }

    @Test
    public void testIncLast()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpInc(), this.getO()[4][0], this.getO()[4][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.LAST, exp, false);
    }

    @Test
    public void testIncLastRev()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpIncRev(), this.getO()[5][0], this.getO()[5][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.LAST, exp, true);
    }

    @Test
    public void testIncBoth()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpInc(), this.getO()[6][0], this.getO()[6][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.BOTH, exp, false);
    }

    @Test
    public void testIncBothRev()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpIncRev(), this.getO()[7][0], this.getO()[7][1]);
        this.doTest(StepNormalizerMode.INCREMENT, StepNormalizerBounds.BOTH, exp, true);
    }

    @Test
    public void testMulNeither()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMul(), this.getO()[8][0], this.getO()[8][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.NEITHER, exp, false);
    }

    @Test
    public void testMulNeitherRev()
                                   throws DimensionMismatchException, NumberIsTooSmallException,
                                   MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMulRev(), this.getO()[9][0], this.getO()[9][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.NEITHER, exp, true);
    }

    @Test
    public void testMulFirst()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMul(), this.getO()[10][0], this.getO()[10][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.FIRST, exp, false);
    }

    @Test
    public void testMulFirstRev()
                                 throws DimensionMismatchException, NumberIsTooSmallException,
                                 MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMulRev(), this.getO()[11][0], this.getO()[11][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.FIRST, exp, true);
    }

    @Test
    public void testMulLast()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMul(), this.getO()[12][0], this.getO()[12][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.LAST, exp, false);
    }

    @Test
    public void testMulLastRev()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMulRev(), this.getO()[13][0], this.getO()[13][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.LAST, exp, true);
    }

    @Test
    public void testMulBoth()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMul(), this.getO()[14][0], this.getO()[14][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.BOTH, exp, false);
    }

    @Test
    public void testMulBothRev()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double[] exp = this.getArray(this.getExpMulRev(), this.getO()[15][0], this.getO()[15][1]);
        this.doTest(StepNormalizerMode.MULTIPLES, StepNormalizerBounds.BOTH, exp, true);
    }

    /**
     * The actual step normalizer output test code, shared by all the unit
     * tests.
     * 
     * @param mode
     *        the step normalizer mode to use
     * @param bounds
     *        the step normalizer bounds setting to use
     * @param expected
     *        the expected output (normalized time points)
     * @param reverse
     *        whether to reverse the integration direction
     * @throws NoBracketingException
     * @throws MaxCountExceededException
     * @throws NumberIsTooSmallException
     * @throws DimensionMismatchException
     */
    private void doTest(final StepNormalizerMode mode, final StepNormalizerBounds bounds,
                        final double[] expected, final boolean reverse)
                                                                       throws DimensionMismatchException,
                                                                       NumberIsTooSmallException,
                                                                       MaxCountExceededException, NoBracketingException {
        // Forward test.
        final FirstOrderIntegrator integ = new GraggBulirschStoerIntegrator(
            1e-8, 1.0, 1e-5, 1e-5);
        integ.addStepHandler(new StepNormalizer(0.5, this, mode, bounds));
        final double[] y = { 0.0 };
        final double start = reverse ? this.getEnd() : this.getStart();
        final double end = reverse ? this.getStart() : this.getEnd();
        this.output = new ArrayList<Double>();
        integ.integrate(this, start, y, end, y);
        final double[] actual = new double[this.output.size()];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = this.output.get(i);
        }
        assertArrayEquals(expected, actual, 1e-5);
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        yDot[0] = y[0];
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final double t, final double[] y, final double[] yDot, final boolean isLast) {
        this.output.add(t);
    }

}
