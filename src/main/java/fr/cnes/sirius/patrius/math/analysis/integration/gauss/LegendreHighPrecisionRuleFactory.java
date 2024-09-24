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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import java.math.BigDecimal;
import java.math.MathContext;

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
 * @version $Id: LegendreHighPrecisionRuleFactory.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LegendreHighPrecisionRuleFactory extends BaseRuleFactory<BigDecimal> {
    /** Settings for enhanced precision computations. */
    private final MathContext mContext;
    /** The number {@code 2}. */
    private final BigDecimal two;
    /** The number {@code -1}. */
    private final BigDecimal minusOne;
    /** The number {@code 0.5}. */
    private final BigDecimal oneHalf;

    /**
     * Default precision is {@link MathContext#DECIMAL128 DECIMAL128}.
     */
    public LegendreHighPrecisionRuleFactory() {
        this(MathContext.DECIMAL128);
    }

    /**
     * @param mContextIn
     *        Precision setting for computing the quadrature rules.
     */
    public LegendreHighPrecisionRuleFactory(final MathContext mContextIn) {
        super();
        this.mContext = mContextIn;
        this.two = new BigDecimal("2", mContextIn);
        this.minusOne = new BigDecimal("-1", mContextIn);
        this.oneHalf = new BigDecimal("0.5", mContextIn);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NotStrictlyPositiveException
     *         if {@code numberOfPoints < 1}.
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    protected Pair<BigDecimal[], BigDecimal[]> computeRule(final int numberOfPoints) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (numberOfPoints <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.NUMBER_OF_POINTS,
                numberOfPoints);
        }

        if (numberOfPoints == 1) {
            // Break recursion.
            return new Pair<>(new BigDecimal[] { BigDecimal.ZERO }, new BigDecimal[] { this.two });
        }

        // Get previous rule.
        // If it has not been computed yet it will trigger a recursive call
        // to this method.
        final BigDecimal[] previousPoints = this.getRuleInternal(numberOfPoints - 1).getFirst();

        // Compute next rule.
        final BigDecimal[] points = new BigDecimal[numberOfPoints];
        final BigDecimal[] weights = new BigDecimal[numberOfPoints];

        // Find i-th root of P[n+1] by bracketing.
        final int iMax = numberOfPoints / 2;
        for (int i = 0; i < iMax; i++) {
            // Lower-bound of the interval.
            BigDecimal a = (i == 0) ? this.minusOne : previousPoints[i - 1];
            // Upper-bound of the interval.
            BigDecimal b = (iMax == 1) ? BigDecimal.ONE : previousPoints[i];
            // P[j-1](a)
            BigDecimal pma = BigDecimal.ONE;
            // P[j](a)
            BigDecimal pa = a;
            // P[j-1](b)
            BigDecimal pmb = BigDecimal.ONE;
            // P[j](b)
            BigDecimal pb = b;
            for (int j = 1; j < numberOfPoints; j++) {
                final BigDecimal b2jp1 = new BigDecimal(2 * j + 1, this.mContext);
                final BigDecimal bj = new BigDecimal(j, this.mContext);
                final BigDecimal bjp1 = new BigDecimal(j + 1, this.mContext);

                // Compute P[j+1](a)
                // ppa = ((2 * j + 1) * a * pa - j * pma) / (j + 1);

                BigDecimal tmp1 = a.multiply(b2jp1, this.mContext);
                tmp1 = pa.multiply(tmp1, this.mContext);
                BigDecimal tmp2 = pma.multiply(bj, this.mContext);
                // P[j+1](a)
                BigDecimal ppa = tmp1.subtract(tmp2, this.mContext);
                ppa = ppa.divide(bjp1, this.mContext);

                // Compute P[j+1](b)
                // ppb = ((2 * j + 1) * b * pb - j * pmb) / (j + 1);

                tmp1 = b.multiply(b2jp1, this.mContext);
                tmp1 = pb.multiply(tmp1, this.mContext);
                tmp2 = pmb.multiply(bj, this.mContext);
                // P[j+1](b)
                BigDecimal ppb = tmp1.subtract(tmp2, this.mContext);
                ppb = ppb.divide(bjp1, this.mContext);

                pma = pa;
                pa = ppa;
                pmb = pb;
                pb = ppb;
            }
            // Now pa = P[n+1](a), and pma = P[n](a). Same holds for b.
            // Middle of the interval.
            BigDecimal c = a.add(b, this.mContext).multiply(this.oneHalf, this.mContext);
            // P[j-1](c)
            BigDecimal pmc = BigDecimal.ONE;
            // P[j](c)
            BigDecimal pc = c;
            boolean done = false;
            while (!done) {
                BigDecimal tmp1 = b.subtract(a, this.mContext);
                BigDecimal tmp2 = c.ulp().multiply(BigDecimal.TEN, this.mContext);
                done = tmp1.compareTo(tmp2) <= 0;
                pmc = BigDecimal.ONE;
                pc = c;
                for (int j = 1; j < numberOfPoints; j++) {
                    final BigDecimal b2jp1 = new BigDecimal(2 * j + 1, this.mContext);
                    final BigDecimal bj = new BigDecimal(j, this.mContext);
                    final BigDecimal bjp1 = new BigDecimal(j + 1, this.mContext);

                    // Compute P[j+1](c)
                    tmp1 = c.multiply(b2jp1, this.mContext);
                    tmp1 = pc.multiply(tmp1, this.mContext);
                    tmp2 = pmc.multiply(bj, this.mContext);
                    // P[j+1](c)
                    BigDecimal ppc = tmp1.subtract(tmp2, this.mContext);
                    ppc = ppc.divide(bjp1, this.mContext);

                    pmc = pc;
                    pc = ppc;
                }
                // Now pc = P[n+1](c) and pmc = P[n](c).
                if (!done) {
                    if (pa.signum() * pc.signum() <= 0) {
                        b = c;
                        pmb = pmc;
                        pb = pc;
                    } else {
                        a = c;
                        pma = pmc;
                        pa = pc;
                    }
                    c = a.add(b, this.mContext).multiply(this.oneHalf, this.mContext);
                }
            }
            final BigDecimal nP = new BigDecimal(numberOfPoints, this.mContext);
            BigDecimal tmp1 = pmc.subtract(c.multiply(pc, this.mContext), this.mContext);
            tmp1 = tmp1.multiply(nP);
            tmp1 = tmp1.pow(2, this.mContext);
            BigDecimal tmp2 = c.pow(2, this.mContext);
            tmp2 = BigDecimal.ONE.subtract(tmp2, this.mContext);
            tmp2 = tmp2.multiply(this.two, this.mContext);
            tmp2 = tmp2.divide(tmp1, this.mContext);

            points[i] = c;
            weights[i] = tmp2;

            final int idx = numberOfPoints - i - 1;
            points[idx] = c.negate(this.mContext);
            weights[idx] = tmp2;
        }
        // If "numberOfPoints" is odd, 0 is a root.
        // Note: as written, the test for oddness will work for negative
        // integers too (although it is not necessary here), preventing
        // a FindBugs warning.
        if (numberOfPoints % 2 != 0) {
            BigDecimal pmc = BigDecimal.ONE;
            for (int j = 1; j < numberOfPoints; j += 2) {
                final BigDecimal bj = new BigDecimal(j, this.mContext);
                final BigDecimal bjp1 = new BigDecimal(j + 1, this.mContext);

                // pmc = -j * pmc / (j + 1);
                pmc = pmc.multiply(bj, this.mContext);
                pmc = pmc.divide(bjp1, this.mContext);
                pmc = pmc.negate(this.mContext);
            }

            // 2 / pow(numberOfPoints * pmc, 2);
            final BigDecimal nP = new BigDecimal(numberOfPoints, this.mContext);
            BigDecimal tmp1 = pmc.multiply(nP, this.mContext);
            tmp1 = tmp1.pow(2, this.mContext);
            final BigDecimal tmp2 = this.two.divide(tmp1, this.mContext);

            points[iMax] = BigDecimal.ZERO;
            weights[iMax] = tmp2;
        }

        return new Pair<>(points, weights);
    }
}
