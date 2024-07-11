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
package fr.cnes.sirius.patrius.math.analysis.solvers;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.RegulaFalsiSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;

/**
 * Test case for {@link RegulaFalsiSolver Regula Falsi} solver.
 * 
 * @version $Id: RegulaFalsiSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class RegulaFalsiSolverTest extends BaseSecantSolverAbstractTest {
    /** {@inheritDoc} */
    @Override
    protected UnivariateSolver getSolver() {
        return new RegulaFalsiSolver();
    }

    /** {@inheritDoc} */
    @Override
    protected int[] getQuinticEvalCounts() {
        // While the Regula Falsi method guarantees convergence, convergence
        // may be extremely slow. The last test case does not converge within
        // even a million iterations. As such, it was disabled.
        return new int[] { 3, 7, 8, 19, 18, 11, 67, 55, 288, 151, -1 };
    }

    @Test(expected = ConvergenceException.class)
    public void testIssue631() {
        final UnivariateFunction f = new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final UnivariateSolver solver = new RegulaFalsiSolver();
        final double root = solver.solve(3624, f, 1, 10);
        Assert.assertEquals(3.4341896575482003, root, 1e-15);
    }
}
