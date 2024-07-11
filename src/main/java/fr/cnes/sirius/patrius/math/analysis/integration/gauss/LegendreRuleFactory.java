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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Factory that creates Gauss-type quadrature rule using Legendre polynomials.
 * In this implementation, the lower and upper bounds of the natural interval
 * of integration are -1 and 1, respectively.
 * The Legendre polynomials are evaluated using the recurrence relation
 * presented in <a href="http://en.wikipedia.org/wiki/Abramowitz_and_Stegun"
 * Abramowitz and Stegun, 1964</a>.
 * 
 * @since 3.1
 * @version $Id: LegendreRuleFactory.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LegendreRuleFactory extends BaseRuleFactory<Double> {

    /** 0.5. */
    private static final double HALF = 0.5;

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    protected Pair<Double[], Double[]> computeRule(final int numberOfPoints) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (numberOfPoints <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.NUMBER_OF_POINTS,
                numberOfPoints);
        }

        if (numberOfPoints == 1) {
            // Break recursion.
            return new Pair<Double[], Double[]>(new Double[] { 0d },
                new Double[] { 2d });
        }

        // Get previous rule.
        // If it has not been computed yet it will trigger a recursive call
        // to this method.
        final Double[] previousPoints = this.getRuleInternal(numberOfPoints - 1).getFirst();

        // Compute next rule.
        final Double[] points = new Double[numberOfPoints];
        final Double[] weights = new Double[numberOfPoints];

        // Find i-th root of P[n+1] by bracketing.
        final int iMax = numberOfPoints / 2;
        for (int i = 0; i < iMax; i++) {
            // Lower-bound of the interval.
            double a = (i == 0) ? -1 : previousPoints[i - 1].doubleValue();
            // Upper-bound of the interval.
            double b = (iMax == 1) ? 1 : previousPoints[i].doubleValue();
            // P[j-1](a)
            double pma = 1;
            // P[j](a)
            double pa = a;
            // P[j-1](b)
            double pmb = 1;
            // P[j](b)
            double pb = b;
            for (int j = 1; j < numberOfPoints; j++) {
                final int twojp1 = 2 * j + 1;
                final int jp1 = j + 1;
                // P[j+1](a)
                final double ppa = (twojp1 * a * pa - j * pma) / jp1;
                // P[j+1](b)
                final double ppb = (twojp1 * b * pb - j * pmb) / jp1;
                pma = pa;
                pa = ppa;
                pmb = pb;
                pb = ppb;
            }
            // Now pa = P[n+1](a), and pma = P[n](a) (same holds for b).
            // Middle of the interval.
            double c = HALF * (a + b);
            // P[j-1](c)
            double pmc = 1;
            // P[j](c)
            double pc = c;
            boolean done = false;
            while (!done) {
                done = b - a <= Math.ulp(c);
                pmc = 1;
                pc = c;
                for (int j = 1; j < numberOfPoints; j++) {
                    // P[j+1](c)
                    final double ppc = ((2 * j + 1) * c * pc - j * pmc) / (j + 1);
                    pmc = pc;
                    pc = ppc;
                }
                // Now pc = P[n+1](c) and pmc = P[n](c).
                if (!done) {
                    if (pa * pc <= 0) {
                        b = c;
                        pmb = pmc;
                        pb = pc;
                    } else {
                        a = c;
                        pma = pmc;
                        pa = pc;
                    }
                    c = HALF * (a + b);
                }
            }
            final double d = numberOfPoints * (pmc - c * pc);
            final double w = 2 * (1 - c * c) / (d * d);

            points[i] = c;
            weights[i] = w;

            final int idx = numberOfPoints - i - 1;
            points[idx] = -c;
            weights[idx] = w;
        }
        // If "numberOfPoints" is odd, 0 is a root.
        // Note: as written, the test for oddness will work for negative
        // integers too (although it is not necessary here), preventing
        // a FindBugs warning.
        if (numberOfPoints % 2 != 0) {
            double pmc = 1;
            for (int j = 1; j < numberOfPoints; j += 2) {
                pmc = -j * pmc / (j + 1);
            }
            final double d = numberOfPoints * pmc;
            final double w = 2 / (d * d);

            points[iMax] = 0d;
            weights[iMax] = w;
        }

        return new Pair<Double[], Double[]>(points, weights);
    }
}
