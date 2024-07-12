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
package fr.cnes.sirius.patrius.math.optim.linear;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;

public class SimplexTableauTest {

    @Test
    public void testInitialization() {
        final LinearObjectiveFunction f = createFunction();
        final Collection<LinearConstraint> constraints = createConstraints();
        final SimplexTableau tableau =
            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6);
        final double[][] expectedInitialTableau = {
            { -1, 0, -1, -1, 2, 0, 0, 0, -4 },
            { 0, 1, -15, -10, 25, 0, 0, 0, 0 },
            { 0, 0, 1, 0, -1, 1, 0, 0, 2 },
            { 0, 0, 0, 1, -1, 0, 1, 0, 3 },
            { 0, 0, 1, 1, -2, 0, 0, 1, 4 }
        };
        assertMatrixEquals(expectedInitialTableau, tableau.getData());
    }

    @Test
    public void testDropPhase1Objective() {
        final LinearObjectiveFunction f = createFunction();
        final Collection<LinearConstraint> constraints = createConstraints();
        final SimplexTableau tableau =
            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6);
        final double[][] expectedTableau = {
            { 1, -15, -10, 0, 0, 0, 0 },
            { 0, 1, 0, 1, 0, 0, 2 },
            { 0, 0, 1, 0, 1, 0, 3 },
            { 0, 1, 1, 0, 0, 1, 4 }
        };
        tableau.dropPhase1Objective();
        assertMatrixEquals(expectedTableau, tableau.getData());
    }

    @Test
    public void testTableauWithNoArtificialVars() {
        final LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 15, 10 }, 0);
        final Collection<LinearConstraint> constraints = new ArrayList<>();
        constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { 0, 1 }, Relationship.LEQ, 3));
        constraints.add(new LinearConstraint(new double[] { 1, 1 }, Relationship.LEQ, 4));
        final SimplexTableau tableau =
            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6);
        final double[][] initialTableau = {
            { 1, -15, -10, 25, 0, 0, 0, 0 },
            { 0, 1, 0, -1, 1, 0, 0, 2 },
            { 0, 0, 1, -1, 0, 1, 0, 3 },
            { 0, 1, 1, -2, 0, 0, 1, 4 }
        };
        assertMatrixEquals(initialTableau, tableau.getData());
    }

    @Test
    public void testSerial() {
        final LinearObjectiveFunction f = createFunction();
        final Collection<LinearConstraint> constraints = createConstraints();
        final SimplexTableau tableau =
            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6);
        Assert.assertEquals(tableau, TestUtils.serializeAndRecover(tableau));
    }

    private static LinearObjectiveFunction createFunction() {
        return new LinearObjectiveFunction(new double[] { 15, 10 }, 0);
    }

    private static Collection<LinearConstraint> createConstraints() {
        final Collection<LinearConstraint> constraints = new ArrayList<>();
        constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { 0, 1 }, Relationship.LEQ, 3));
        constraints.add(new LinearConstraint(new double[] { 1, 1 }, Relationship.EQ, 4));
        return constraints;
    }

    private static void assertMatrixEquals(final double[][] expected, final double[][] result) {
        Assert.assertEquals("Wrong number of rows.", expected.length, result.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals("Wrong number of columns.", expected[i].length, result[i].length);
            for (int j = 0; j < expected[i].length; j++) {
                Assert.assertEquals("Wrong value at position [" + i + "," + j + "]", expected[i][j], result[i][j],
                    1.0e-15);
            }
        }
    }
}
