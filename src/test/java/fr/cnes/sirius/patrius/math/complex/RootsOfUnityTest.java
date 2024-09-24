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
package fr.cnes.sirius.patrius.math.complex;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for the {@link RootsOfUnity} class.
 * 
 * @version $Id: RootsOfUnityTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class RootsOfUnityTest {

    @Test(expected = MathIllegalStateException.class)
    public void testMathIllegalState1() {
        final RootsOfUnity roots = new RootsOfUnity();
        roots.getReal(0);
    }

    @Test(expected = MathIllegalStateException.class)
    public void testMathIllegalState2() {
        final RootsOfUnity roots = new RootsOfUnity();
        roots.getImaginary(0);
    }

    @Test(expected = MathIllegalStateException.class)
    public void testMathIllegalState3() {
        final RootsOfUnity roots = new RootsOfUnity();
        roots.isCounterClockWise();
    }

    @Test(expected = ZeroException.class)
    public void testZeroNumberOfRoots() {
        final RootsOfUnity roots = new RootsOfUnity();
        roots.computeRoots(0);
    }

    @Test
    public void testGetNumberOfRoots() {
        final RootsOfUnity roots = new RootsOfUnity();
        Assert.assertEquals("", 0, roots.getNumberOfRoots());
        roots.computeRoots(5);
        Assert.assertEquals("", 5, roots.getNumberOfRoots());
        /*
         * Testing -5 right after 5 is important, as the roots in this case are
         * not recomputed.
         */
        roots.computeRoots(-5);
        Assert.assertEquals("", 5, roots.getNumberOfRoots());
        roots.computeRoots(6);
        Assert.assertEquals("", 6, roots.getNumberOfRoots());
    }

    @Test
    public void testComputeRoots() {
        final RootsOfUnity roots = new RootsOfUnity();
        for (int n = -10; n < 11; n++) {
            /*
             * Testing -n right after n is important, as the roots in this case
             * are not recomputed.
             */
            if (n != 0) {
                roots.computeRoots(n);
                this.doTestComputeRoots(roots);
                roots.computeRoots(-n);
                this.doTestComputeRoots(roots);
            }
        }
    }

    private void doTestComputeRoots(final RootsOfUnity roots) {
        final int n = roots.isCounterClockWise() ? roots.getNumberOfRoots() :
            -roots.getNumberOfRoots();
        final double tol = 10 * Math.ulp(1.0);
        for (int k = 0; k < n; k++) {
            final double t = 2.0 * FastMath.PI * k / n;
            final String msg = String.format("n = %d, k = %d", n, k);
            Assert.assertEquals(msg, MathLib.cos(t), roots.getReal(k), tol);
            Assert.assertEquals(msg, MathLib.sin(t), roots.getImaginary(k), tol);
        }
    }
}
