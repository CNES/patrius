/**
 * Copyright 2011-2017 CNES
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
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements <a href="http://mathworld.wolfram.com/SimpsonsRule.html"> Simpson's Rule</a> for the
 * integration of real univariate functions.
 * <p>
 * This method splits the integration interval into two equal parts and computes the value of the integral on each of
 * these parts using a 3 point Simpson's rule. The same method is then applied again on each sub-interval, until the
 * required accuracy is reached or the maximal number of iterations is reached.
 * </p>
 * <p>
 * For reference, see <b>Introduction to Numerical Analysis</b>, ISBN 038795452X, chapter 3.
 * </p>
 *
 * @author GMV
 */
public class AdaptiveSimpsonIntegrator extends BaseAbstractUnivariateIntegrator {

    /** Maximal number of iterations allowed for this method. */
    public static final int MAX_ITERATIONS_COUNT = 64;

    /** Serial UID. */
    private static final long serialVersionUID = -3948135867061821633L;

    /**
     * Build an adaptive Simpson integrator with given accuracies and iterations counts.
     *
     * @param relativeAccuracy relative accuracy of the result
     * @param absoluteAccuracy absolute accuracy of the result
     * @param minimalIterationCount minimum number of iterations
     * @param maximalIterationCount maximum number of iterations (must be less than or equal to
     *        {@link #MAX_ITERATIONS_COUNT})
     * @exception NumberIsTooLargeException if maximal number of iterations is greater than
     *            {@link #MAX_ITERATIONS_COUNT}
     */
    public AdaptiveSimpsonIntegrator(final double relativeAccuracy, final double absoluteAccuracy,
        final int minimalIterationCount, final int maximalIterationCount) {
        super(relativeAccuracy, absoluteAccuracy, minimalIterationCount, maximalIterationCount);

        if (maximalIterationCount > MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount, MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Build an adaptive Simpson integrator with given iteration counts.
     *
     * @param minimalIterationCount minimum number of iterations
     * @param maximalIterationCount maximum number of iterations (must be less than or equal to
     *        {@link #MAX_ITERATIONS_COUNT})
     * @exception NumberIsTooLargeException if maximal number of iterations is greater than
     *            {@link #MAX_ITERATIONS_COUNT}
     */
    public AdaptiveSimpsonIntegrator(final int minimalIterationCount,
        final int maximalIterationCount) {
        super(minimalIterationCount, maximalIterationCount);

        if (maximalIterationCount > MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount, MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Construct an integrator with default settings. (max iteration count set to {@link #MAX_ITERATIONS_COUNT})
     */
    public AdaptiveSimpsonIntegrator() {
        super(DEFAULT_MIN_ITERATIONS_COUNT, MAX_ITERATIONS_COUNT);
    }

    /** {@inheritDoc} */
    @Override
    protected double doIntegrate() throws TooManyEvaluationsException, MaxCountExceededException {
        // Points
        final double a = this.getMin();
        final double b = this.getMax();
        final double m = (a + b) / 2.0;

        // Objective function value at each point
        final double fa = this.computeObjectiveValue(a);
        final double fb = this.computeObjectiveValue(b);
        final double fm = this.computeObjectiveValue(m);

        // Initial segment
        final Segment segment0 = this.buildSegment(a, b, m, fa, fb, fm);

        // Initial value of the integral (3 and 5 points)
        double integral0 = segment0.s0;
        double integral1 = segment0.s1;

        // Increment the iterations count by 2
        // (the initial segment already contains two iterations, 3 and 5 points)
        this.iterations.incrementCount(2);

        // Tree of segments
        List<Segment> list0 = new ArrayList<Segment>();
        list0.add(segment0);

        while (true) {
            // Absolute accuracy limit
            final double aLimit = this.getAbsoluteAccuracy();
            // Relative accuracy limit relatively to the total value of the integral.
            final double rLimit = this.getRelativeAccuracy()
                * (MathLib.abs(integral0) + MathLib.abs(integral1)) / 2.0;

            // Increment the iterations count
            this.iterations.incrementCount();

            // Save the value of the integral and compute the new value
            integral0 = integral1;
            integral1 = 0;

            // New segments list
            final List<Segment> list1 = new ArrayList<Segment>();

            // Global convergence indicator
            boolean globalConvergence = true;

            for (final Segment segment : list0) {
                // Check whether the segment has reached convergence or not
                final double delta = MathLib.abs(segment.s0 - segment.s1);
                final boolean localConvergence = delta <= aLimit || delta <= rLimit;

                // If the segment has reached convergence, keep it as is.
                // Otherwise, split it into two sub-segments.
                if (localConvergence && this.iterations.getCount() >= this.getMinimalIterationCount()) {
                    integral1 += segment.s1;
                    list1.add(segment);
                } else {
                    // Indicate that global convergence was not reached for this iteration
                    globalConvergence = false;

                    // Lower sub-segment: [a, m] with the middle point l
                    final double la = segment.a;
                    final double lb = segment.m;
                    final double lm = segment.l;
                    final double lfa = segment.fa;
                    final double lfb = segment.fm;
                    final double lfm = segment.fl;
                    final Segment segmentL = this.buildSegment(la, lb, lm, lfa, lfb, lfm);

                    // Upper sub-segment: [m, b] with the middle point u
                    final double ua = segment.m;
                    final double ub = segment.b;
                    final double um = segment.u;
                    final double ufa = segment.fm;
                    final double ufb = segment.fb;
                    final double ufm = segment.fu;
                    final Segment segmentU = this.buildSegment(ua, ub, um, ufa, ufb, ufm);

                    // Add the two sub-segment to the new list of segment
                    list1.add(segmentL);
                    list1.add(segmentU);

                    // Add the two subsegments values to the integral
                    integral1 += segmentL.s1 + segmentU.s1;
                }
            }

            // Update the list of segments
            list0 = list1;

            // If the global convergence was reached, return the last value computed
            if (globalConvergence) {
                return integral1;
            }
        }
    }

    /**
     * Build a segment from the specified points and function values.
     *
     * @param a lower bound of the segment
     * @param b upper bound of the segment
     * @param m middle point of the segment (m = (a+b)/2.0)
     * @param fa function value at point a
     * @param fb function value at point b
     * @param fm function value at point m
     * @return built segment
     */
    private Segment buildSegment(final double a, final double b, final double m, final double fa,
                                 final double fb, final double fm) {
        final double l = (a + m) / 2.0;
        final double u = (m + b) / 2.0;

        final double fl = this.computeObjectiveValue(l);
        final double fu = this.computeObjectiveValue(u);

        return new Segment(a, b, m, l, u, fa, fb, fm, fl, fu);
    }

    /**
     * Stores a segment [a, b] of the integrated interval.
     * <p>
     * In addition to the segment's boundaries a and b, this class stores:
     * <ul>
     * <li>the central middle point m = (a+b)/2.0;</li>
     * <li>the lower middle point l = (a+m)/2.0;</li>
     * <li>the upper middle point u = (m+b)/2.0.</li>
     * </ul>
     * It also stores the values of the objective function at each of these points (fa, fb, fm, fu, fl) and the values
     * s0 and s1 of the integral for the 3 points quadrature (a, m, b) or the 5 points quadrature (a, l, m, u, b).
     * </p>
     */
    private static class Segment {

        /** a. */
        private final double a;

        /** b. */
        private final double b;

        /** m. */
        private final double m;

        /** l. */
        private final double l;

        /** u. */
        private final double u;

        /** fa. */
        private final double fa;

        /** fb. */
        private final double fb;

        /** fm. */
        private final double fm;

        /** fl. */
        private final double fl;

        /** fu. */
        private final double fu;

        /** s0. */
        private final double s0;

        /** s1. */
        private final double s1;

        /** sl. */
        private final double sl;

        /** su. */
        private final double su;

        /**
         * Constructor.
         * 
         * @param aIn a
         * @param bIn b
         * @param mIn m
         * @param lIn l
         * @param uIn u
         * @param faIn fa
         * @param fbIn fb
         * @param fmIn fm
         * @param flIn fl
         * @param fuIn fu
         */
        public Segment(final double aIn, final double bIn, final double mIn, final double lIn,
            final double uIn, final double faIn, final double fbIn, final double fmIn, final double flIn,
            final double fuIn) {
            this.a = aIn;
            this.b = bIn;
            this.m = mIn;
            this.l = lIn;
            this.u = uIn;

            this.fa = faIn;
            this.fb = fbIn;
            this.fm = fmIn;
            this.fl = flIn;
            this.fu = fuIn;

            // Simpson integral on [a, b] using 3 points
            this.s0 = (bIn - aIn) / 6.0 * (faIn + 4.0 * fmIn + fbIn);
            // Simpson integral on [a, m]
            this.sl = (mIn - aIn) / 6.0 * (faIn + 4.0 * flIn + fmIn);
            // Simpson integral on [m, b]
            this.su = (bIn - mIn) / 6.0 * (fmIn + 4.0 * fuIn + fbIn);
            // Simpson integral on [a,b] using 5 points
            this.s1 = this.sl + this.su;
        }
    }
}
