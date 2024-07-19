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
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.bivariate;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.AdaptiveSimpsonIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.UnivariateIntegrator;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for class {@link DelegatedBivariateIntegrator}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DragWrenchTest.java 17918 2017-09-11 13:04:41Z bignon $
 * 
 * @since 4.0
 * 
 */
public class DelegatedBivariateIntegratorTest {

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
     * @testedMethod {@link DelegatedBivariateIntegrator#integrate(int, fr.cnes.sirius.patrius.math.analysis.BivariateFunction, double, double, double, double)}
     * 
     * @description Check integration of sin.sin function with delegated bivariate integrators
     * 
     * @input {@link DelegatedBivariateIntegrator}
     *
     * @output integrated function between lower and upper bounds (double)
     *
     * @testPassCriteria result is as expected (reference: math, threshold: 1E-10 - limited by provided integrator
     *                   accuracy)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testIntegration() {

        // Initialization
        final BivariateFunction f = new BivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 1561928058832649570L;

            @Override
            public double value(final double x, final double y) {
                return MathLib.sin(x) * MathLib.sin(y);
            }
        };

        final UnivariateIntegrator integratorX = new AdaptiveSimpsonIntegrator(1E-12, 1E-12, 1, 63);
        final UnivariateIntegrator integratorY = new AdaptiveSimpsonIntegrator(1E-12, 1E-12, 1, 63);
        final DelegatedBivariateIntegrator delegatedBivariateIntegrator =
            new DelegatedBivariateIntegrator(integratorX, integratorY);

        // Computation (expected result is 4)
        final double actual =
            delegatedBivariateIntegrator.integrate(Integer.MAX_VALUE, f, 0, FastMath.PI, 0, FastMath.PI);
        final double expected = 4.;

        // Check
        Assert.assertEquals(expected, actual, 1E-10);

        // Functional checks
        Assert.assertEquals(564889, delegatedBivariateIntegrator.getEvaluations());
        Assert.assertTrue(delegatedBivariateIntegrator.getIntegratorX().equals(integratorX));
        Assert.assertTrue(delegatedBivariateIntegrator.getIntegratorY().equals(integratorY));
        Assert.assertTrue(delegatedBivariateIntegrator.getFunction().equals(f));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COLOSUS}
     * 
     * @description Check {@link DelegatedBivariateIntegrator} exceptions
     * 
     * @input {@link DelegatedBivariateIntegrator}
     *
     * @output exception
     *
     * @testPassCriteria exceptions thrown as expected
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testExceptions() {

        try {
            // Initialization
            final BivariateFunction f = new BivariateFunction(){
                /** Serializable UID. */
                private static final long serialVersionUID = -9036039305860722749L;

                @Override
                public double value(final double x, final double y) {
                    return MathLib.sin(x) * MathLib.sin(y);
                }
            };

            final UnivariateIntegrator integratorX = new AdaptiveSimpsonIntegrator(1E-15, 1E-15, 1, 63);
            final UnivariateIntegrator integratorY = new AdaptiveSimpsonIntegrator(1E-15, 1E-15, 1, 63);
            final BivariateIntegrator delegatedBivariateIntegrator =
                new DelegatedBivariateIntegrator(integratorX, integratorY);

            // Computation (error is expected due to too high tolerance requirement)
            delegatedBivariateIntegrator.integrate(100, f, 0, FastMath.PI, 0, FastMath.PI);

            Assert.fail();
        } catch (final MaxCountExceededException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }
}
