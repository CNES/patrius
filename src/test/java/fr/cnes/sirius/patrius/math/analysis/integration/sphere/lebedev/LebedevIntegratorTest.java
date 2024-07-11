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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * Unit tests for class {@link LebedevIntegrator} and all subclasses.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DragWrenchTest.java 17918 2017-09-11 13:04:41Z bignon $
 * 
 * @since 4.1
 * 
 */
public class LebedevIntegratorTest {

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
     * @testedMethod {@link LebedevIntegrator#integrate(int, fr.cnes.sirius.patrius.math.analysis.UnivariateFunction, double, double)}
     * 
     * @description Check integration with Lebedev integrator (integration over a sphere) with different configurations
     * 
     * @input LebedevIntegrator
     *
     * @output integrated function between over sphere (double)
     *
     * @testPassCriteria result is as expected (reference: math, threshold: 3E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testIntegration() {

        // Initialization
        final LebedevIntegrator integrator = new LebedevIntegrator();

        // Test different test cases
        for (int i = 0; i < 5; i++) {
            final LebedevFunction f = new SphereTestFunction(0);
            // Test different grids
            for (final LebedevGrid grid : LebedevGridUtils.getAvailableGrids()) {
                // Computation
                final double actual = integrator.integrate(1000000, f, grid);
                final double expected = 4. * FastMath.PI * 1. * 1.;

                // Check
                Assert.assertEquals(0, (expected - actual) / expected, 3E-14);
            }
        }

        // Lebedev integrator functional checks
        Assert.assertEquals(5810, integrator.getEvaluations());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COLOSUS}
     * 
     * @description Check LebedevGrid and LebedevGridPoint methods
     * 
     * @input LebedevGrid, LebedevGridPoint
     *
     * @output getters output
     *
     * @testPassCriteria result is as expected
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGridAndGridPoints() {

        // Initialization
        final LebedevGrid grid = LebedevGridUtils.getAvailableGrids().get(0);

        // Checks on grid
        Assert.assertEquals(6, grid.getSize());
        Assert.assertEquals(1., grid.getTotalWeight(), 1E-15);
        Assert.assertEquals(6, grid.getDuplicates(grid, 0).size());

        // Checks on grid points (with point [1, 0, 0])
        final LebedevGridPoint point = grid.getPoints().get(0);
        Assert.assertTrue(point.isSamePoint(point, 0));
        Assert.assertEquals(1., point.getRadius(), 0.);
        Assert.assertEquals(FastMath.PI / 2., point.getPhi(), 0);
        Assert.assertEquals(0., point.getTheta(), 0.);
    }
}
