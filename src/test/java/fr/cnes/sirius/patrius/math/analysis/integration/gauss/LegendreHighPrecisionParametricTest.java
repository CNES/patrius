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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test of the {@link LegendreHighPrecisionRuleFactory}.
 * This parameterized test extends the standard test for Gaussian quadrature
 * rule, where each monomial is tested in turn.
 * Parametrization allows to test automatically 0, 1, ... , {@link #MAX_NUM_POINTS} quadrature rules.
 * 
 * @version $Id: LegendreHighPrecisionParametricTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
@RunWith(value = Parameterized.class)
public class LegendreHighPrecisionParametricTest extends GaussianQuadratureAbstractTest {
    private static GaussIntegratorFactory factory = new GaussIntegratorFactory();

    /**
     * The highest order quadrature rule to be tested.
     */
    public static final int MAX_NUM_POINTS = 30;

    /**
     * Creates a new instance of this test, with the specified number of nodes
     * for the Gauss-Legendre quadrature rule.
     * 
     * @param numberOfPoints
     *        Order of integration rule.
     * @param maxDegree
     *        Maximum degree of monomials to be tested.
     * @param eps
     *        Value of &epsilon;.
     * @param numUlps
     *        Value of the maximum relative error (in ulps).
     */
    public LegendreHighPrecisionParametricTest(final int numberOfPoints,
        final int maxDegree,
        final double eps,
        final double numUlps) {
        super(factory.legendreHighPrecision(numberOfPoints),
            maxDegree, eps, numUlps);
    }

    /**
     * Returns the collection of parameters to be passed to the constructor of
     * this class.
     * Gauss-Legendre quadrature rules of order 1, ..., {@link #MAX_NUM_POINTS} will be constructed.
     * 
     * @return the collection of parameters for this parameterized test.
     */
    @Parameters
    public static Collection<Object[]> getParameters() {
        final ArrayList<Object[]> parameters = new ArrayList<>();
        for (int k = 1; k <= MAX_NUM_POINTS; k++) {
            parameters.add(new Object[] { k, 2 * k - 1, Math.ulp(1d), 13d });
        }
        return parameters;
    }

    @Override
    public double getExpectedValue(final int n) {
        if (n % 2 == 1) {
            return 0;
        }
        return 2d / (n + 1);
    }
}
