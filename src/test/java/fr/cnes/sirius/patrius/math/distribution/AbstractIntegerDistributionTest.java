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
package fr.cnes.sirius.patrius.math.distribution;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for AbstractIntegerDistribution default implementations.
 * 
 * @version $Id: AbstractIntegerDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class AbstractIntegerDistributionTest {
    protected final DiceDistribution diceDistribution = new DiceDistribution();
    protected final double p = this.diceDistribution.probability(1);

    @Test
    public void testCumulativeProbabilitiesSingleArguments() {
        for (int i = 1; i < 7; i++) {
            Assert.assertEquals(this.p * i,
                this.diceDistribution.cumulativeProbability(i), Double.MIN_VALUE);
        }
        Assert.assertEquals(0.0,
            this.diceDistribution.cumulativeProbability(0), Double.MIN_VALUE);
        Assert.assertEquals(1.0,
            this.diceDistribution.cumulativeProbability(7), Double.MIN_VALUE);
    }

    @Test
    public void testCumulativeProbabilitiesRangeArguments() {
        int lower = 0;
        int upper = 6;
        for (int i = 0; i < 2; i++) {
            // cum(0,6) = p(0 < X <= 6) = 1, cum(1,5) = 4/6, cum(2,4) = 2/6
            Assert.assertEquals(1 - this.p * 2 * i,
                this.diceDistribution.cumulativeProbability(lower, upper), 1E-12);
            lower++;
            upper--;
        }
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(this.p, this.diceDistribution.cumulativeProbability(i, i + 1), 1E-12);
        }
    }

    /**
     * Simple distribution modeling a 6-sided die
     */
    class DiceDistribution extends AbstractIntegerDistribution {
        public static final long serialVersionUID = 23734213;

        private final double p = 1d / 6d;

        public DiceDistribution() {
            super(null);
        }

        @Override
        public double probability(final int x) {
            if (x < 1 || x > 6) {
                return 0;
            } else {
                return this.p;
            }
        }

        @Override
        public double cumulativeProbability(final int x) {
            if (x < 1) {
                return 0;
            } else if (x >= 6) {
                return 1;
            } else {
                return this.p * x;
            }
        }

        @Override
        public double getNumericalMean() {
            return 3.5;
        }

        @Override
        public double getNumericalVariance() {
            return 12.5 - 3.5 * 3.5; // E(X^2) - E(X)^2
        }

        @Override
        public int getSupportLowerBound() {
            return 1;
        }

        @Override
        public int getSupportUpperBound() {
            return 6;
        }

        @Override
        public final boolean isSupportConnected() {
            return true;
        }
    }
}
