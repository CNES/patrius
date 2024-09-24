/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1271:04/11/2017:change Coppola method
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for class {@link FixedStepSimpsonIntegrator}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DragWrenchTest.java 17918 2017-09-11 13:04:41Z bignon $
 * 
 * @since 4.0
 * 
 */
public class FixedStepSimpsonIntegratorTest {

    /** Features description */
    public enum features {
        /**
         * @featureTitle Colosus.
         * 
         * @featureDescription Colosus (collision risk).
         * 
         * @coveredRequirements NA
         */
        COLOSUS;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COLOSUS}
     * 
     * @testedMethod {@link FixedStepSimpsonIntegrator#integrate(int, fr.cnes.sirius.patrius.math.analysis.UnivariateFunction, double, double)}
     * 
     * @description Check integration with fixed-step Simpson integrator is identical to variable-step Simpson
     *              integrator
     * 
     * @input function (sin), FixedStepSimpsonIntegrator with 10000 steps
     *
     * @output integrated function between lower and upper bounds (double)
     *
     * @testPassCriteria result is as expected (reference: variable-step Simpson integrator, threshold: 1E-7)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testIntegration() {

        // Initialization
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2490127289098820524L;

            @Override
            public double value(final double x) {
                return MathLib.sin(x);
            }
        };

        final UnivariateIntegrator simpsonFixed = new FixedStepSimpsonIntegrator(10000);
        final UnivariateIntegrator simpsonVariable = new SimpsonIntegrator();

        // Computation (expected result is 2)
        final double actual = simpsonFixed.integrate(Integer.MAX_VALUE, f, 0, FastMath.PI);
        final double expected = simpsonVariable.integrate(Integer.MAX_VALUE, f, 0, FastMath.PI);

        // Check
        Assert.assertEquals(expected, actual, 1E-7);
    }
}
