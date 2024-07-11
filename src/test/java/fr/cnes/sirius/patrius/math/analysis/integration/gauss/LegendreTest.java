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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Cos;
import fr.cnes.sirius.patrius.math.analysis.function.Inverse;
import fr.cnes.sirius.patrius.math.analysis.function.Log;

/**
 * Test of the {@link LegendreRuleFactory}.
 * 
 * @version $Id: LegendreTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LegendreTest {
    private static GaussIntegratorFactory factory = new GaussIntegratorFactory();

    @Test
    public void testCos() {
        final UnivariateFunction cos = new Cos();

        final GaussIntegrator integrator = factory.legendre(7, 0, Math.PI / 2);
        final double s = integrator.integrate(cos);
        // System.out.println("s=" + s + " e=" + 1);
        Assert.assertEquals(1, s, Math.ulp(1d));
    }

    @Test
    public void testInverse() {
        final UnivariateFunction inv = new Inverse();
        final UnivariateFunction log = new Log();

        final double lo = 12.34;
        final double hi = 456.78;

        final GaussIntegrator integrator = factory.legendre(60, lo, hi);
        final double s = integrator.integrate(inv);
        final double expected = log.value(hi) - log.value(lo);
        // System.out.println("s=" + s + " e=" + expected);
        Assert.assertEquals(expected, s, 1e-14);
    }
}
