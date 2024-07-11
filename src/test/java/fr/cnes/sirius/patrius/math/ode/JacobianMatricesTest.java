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
 * VERSION::FA:306:20/11/2014: coverage
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.JacobianMatrices.MismatchedEquations;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince54Integrator;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class JacobianMatricesTest {

    public static final String CX = "cx";
    public static final String CY = "cy";
    public static final String OMEGA = "omega";

    @Test
    public void testLowAccuracyExternalDifferentiation()
                                                        throws NumberIsTooSmallException, DimensionMismatchException,
                                                        MaxCountExceededException, NoBracketingException {
        // this test does not really test JacobianMatrices,
        // it only shows that WITHOUT this class, attempting to recover
        // the jacobians from external differentiation on simple integration
        // results with low accuracy gives very poor results. In fact,
        // the curves dy/dp = g(b) when b varies from 2.88 to 3.08 are
        // essentially noise.
        // This test is taken from Hairer, Norsett and Wanner book
        // Solving Ordinary Differential Equations I (Nonstiff problems),
        // the curves dy/dp = g(b) are in figure 6.5
        final FirstOrderIntegrator integ =
            new DormandPrince54Integrator(1.0e-8, 100.0, new double[] { 1.0e-4, 1.0e-4 }, new double[] { 1.0e-4,
                1.0e-4 });
        final double hP = 1.0e-12;
        final SummaryStatistics residualsP0 = new SummaryStatistics();
        final SummaryStatistics residualsP1 = new SummaryStatistics();
        for (double b = 2.88; b < 3.08; b += 0.001) {
            final Brusselator brusselator = new Brusselator(b);
            final double[] y = { 1.3, b };
            integ.integrate(brusselator, 0, y, 20.0, y);
            final double[] yP = { 1.3, b + hP };
            integ.integrate(brusselator, 0, yP, 20.0, yP);
            residualsP0.addValue((yP[0] - y[0]) / hP - brusselator.dYdP0());
            residualsP1.addValue((yP[1] - y[1]) / hP - brusselator.dYdP1());
        }
        Assert.assertTrue((residualsP0.getMax() - residualsP0.getMin()) > 500);
        Assert.assertTrue(residualsP0.getStandardDeviation() > 30);
        Assert.assertTrue((residualsP1.getMax() - residualsP1.getMin()) > 700);
        Assert.assertTrue(residualsP1.getStandardDeviation() > 40);
    }

    @Test
    public void testHighAccuracyExternalDifferentiation()
                                                         throws NumberIsTooSmallException, DimensionMismatchException,
                                                         MaxCountExceededException, NoBracketingException,
                                                         UnknownParameterException {
        final FirstOrderIntegrator integ =
            new DormandPrince54Integrator(1.0e-8, 100.0, new double[] { 1.0e-10, 1.0e-10 }, new double[] { 1.0e-10,
                1.0e-10 });
        final double hP = 1.0e-12;
        final SummaryStatistics residualsP0 = new SummaryStatistics();
        final SummaryStatistics residualsP1 = new SummaryStatistics();
        for (double b = 2.88; b < 3.08; b += 0.001) {
            final ParamBrusselator brusselator = new ParamBrusselator(b);
            final double[] y = { 1.3, b };
            integ.integrate(brusselator, 0, y, 20.0, y);
            final double[] yP = { 1.3, b + hP };
            brusselator.setParameter("b", b + hP);
            integ.integrate(brusselator, 0, yP, 20.0, yP);
            residualsP0.addValue((yP[0] - y[0]) / hP - brusselator.dYdP0());
            residualsP1.addValue((yP[1] - y[1]) / hP - brusselator.dYdP1());
        }
        Assert.assertTrue((residualsP0.getMax() - residualsP0.getMin()) > 0.02);
        Assert.assertTrue((residualsP0.getMax() - residualsP0.getMin()) < 0.03);
        Assert.assertTrue(residualsP0.getStandardDeviation() > 0.003);
        Assert.assertTrue(residualsP0.getStandardDeviation() < 0.004);
        Assert.assertTrue((residualsP1.getMax() - residualsP1.getMin()) > 0.04);
        Assert.assertTrue((residualsP1.getMax() - residualsP1.getMin()) < 0.05);
        Assert.assertTrue(residualsP1.getStandardDeviation() > 0.007);
        Assert.assertTrue(residualsP1.getStandardDeviation() < 0.008);
    }

    @Test
    public void testInternalDifferentiation()
                                             throws NumberIsTooSmallException, DimensionMismatchException,
                                             MaxCountExceededException, NoBracketingException,
                                             UnknownParameterException, MismatchedEquations {
        final AbstractIntegrator integ =
            new DormandPrince54Integrator(1.0e-8, 100.0, new double[] { 1.0e-4, 1.0e-4 }, new double[] { 1.0e-4,
                1.0e-4 });
        final double hP = 1.0e-12;
        final double hY = 1.0e-12;
        final SummaryStatistics residualsP0 = new SummaryStatistics();
        final SummaryStatistics residualsP1 = new SummaryStatistics();
        for (double b = 2.88; b < 3.08; b += 0.001) {
            final ParamBrusselator brusselator = new ParamBrusselator(b);
            brusselator.setParameter(ParamBrusselator.B, b);
            final double[] z = { 1.3, b };
            final double[][] dZdZ0 = new double[2][2];
            final double[] dZdP = new double[2];

            final JacobianMatrices jacob =
                new JacobianMatrices(brusselator, new double[] { hY, hY }, ParamBrusselator.B);
            jacob.setParameterizedODE(brusselator);
            jacob.setParameterStep(ParamBrusselator.B, hP);
            jacob.setInitialParameterJacobian(ParamBrusselator.B, new double[] { 0.0, 1.0 });

            final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
            efode.setTime(0);
            efode.setPrimaryState(z);
            jacob.registerVariationalEquations(efode);

            integ.setMaxEvaluations(5000);
            integ.integrate(efode, 20.0);
            jacob.getCurrentMainSetJacobian(dZdZ0);
            jacob.getCurrentParameterJacobian(ParamBrusselator.B, dZdP);
            // Assert.assertEquals(5000, integ.getMaxEvaluations());
            // Assert.assertTrue(integ.getEvaluations() > 1500);
            // Assert.assertTrue(integ.getEvaluations() < 2100);
            // Assert.assertEquals(4 * integ.getEvaluations(), integ.getEvaluations());
            residualsP0.addValue(dZdP[0] - brusselator.dYdP0());
            residualsP1.addValue(dZdP[1] - brusselator.dYdP1());
        }
        Assert.assertTrue((residualsP0.getMax() - residualsP0.getMin()) < 0.02);
        Assert.assertTrue(residualsP0.getStandardDeviation() < 0.003);
        Assert.assertTrue((residualsP1.getMax() - residualsP1.getMin()) < 0.05);
        Assert.assertTrue(residualsP1.getStandardDeviation() < 0.01);
    }

    @Test
    public void testAnalyticalDifferentiation()
                                               throws MaxCountExceededException, DimensionMismatchException,
                                               NumberIsTooSmallException, NoBracketingException,
                                               UnknownParameterException, MismatchedEquations {
        final AbstractIntegrator integ =
            new DormandPrince54Integrator(1.0e-8, 100.0, new double[] { 1.0e-4, 1.0e-4 }, new double[] { 1.0e-4,
                1.0e-4 });
        final SummaryStatistics residualsP0 = new SummaryStatistics();
        final SummaryStatistics residualsP1 = new SummaryStatistics();
        for (double b = 2.88; b < 3.08; b += 0.001) {
            final Brusselator brusselator = new Brusselator(b);
            final double[] z = { 1.3, b };
            final double[][] dZdZ0 = new double[2][2];
            final double[] dZdP = new double[2];

            final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
            jacob.addParameterJacobianProvider(brusselator);
            jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

            final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
            efode.setTime(0);
            efode.setPrimaryState(z);
            jacob.registerVariationalEquations(efode);

            integ.setMaxEvaluations(5000);
            integ.integrate(efode, 20.0);
            jacob.getCurrentMainSetJacobian(dZdZ0);
            jacob.getCurrentParameterJacobian(Brusselator.B, dZdP);
            // Assert.assertEquals(5000, integ.getMaxEvaluations());
            // Assert.assertTrue(integ.getEvaluations() > 350);
            // Assert.assertTrue(integ.getEvaluations() < 510);
            residualsP0.addValue(dZdP[0] - brusselator.dYdP0());
            residualsP1.addValue(dZdP[1] - brusselator.dYdP1());
        }
        Assert.assertTrue((residualsP0.getMax() - residualsP0.getMin()) < 0.014);
        Assert.assertTrue(residualsP0.getStandardDeviation() < 0.003);
        Assert.assertTrue((residualsP1.getMax() - residualsP1.getMin()) < 0.05);
        Assert.assertTrue(residualsP1.getStandardDeviation() < 0.01);
    }

    /**
     * For coverage purposes. Tests the DimensionMismatchException in methods
     * set{Primary/Secondary/Complete}State. In the meantime, also tests the getters
     * get{Primary/Secondary}State{/Dot}.
     */
    @Test
    public void testExpandableStatefulODE() {

        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final double[] z = { 1.3, b };

        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
        efode.setTime(0);
        efode.setPrimaryState(z);
        jacob.registerVariationalEquations(efode);

        // tests method addSecondaryEquations, else of if (components.isEmpty())
        final SecondaryEquations secondary = new SecondaryEquations(){
            @Override
            public int getDimension() {
                return 0;
            }

            @Override
            public void computeDerivatives(final double t, final double[] primary, final double[] primaryDot,
                                           final double[] secondary,
                                           final double[] secondaryDot)
                                                                       throws MaxCountExceededException,
                                                                       DimensionMismatchException {
            }
        };
        final int nbEq = efode.addSecondaryEquations(secondary);
        Assert.assertEquals(1, nbEq);

        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        // tests getPrimaryState
        final double[] primaryState = efode.getPrimaryState();
        Assert.assertArrayEquals(primaryState, z, eps);

        // tests getPrimaryStateDot
        final double[] primaryStateDot = efode.getPrimaryStateDot();
        final double[] primaryStateDotRef = { 0, 0 };
        Assert.assertArrayEquals(primaryStateDot, primaryStateDotRef, eps);

        // tests getSecondaryState
        final double[] secondaryState = efode.getSecondaryState(0);
        final double[] secondaryStateRef = { 1, 0, 0, 1, 0, 1 };
        Assert.assertArrayEquals(secondaryState, secondaryStateRef, eps);

        // tests getSecondaryStateDot
        final double[] secondaryStateDot = efode.getSecondaryStateDot(0);
        final double[] secondaryStateDotRef = { 0, 0, 0, 0, 0, 0 };
        Assert.assertArrayEquals(secondaryStateDot, secondaryStateDotRef, eps);

        // tests if (primaryState.length != this.primaryState.length) in method setPrimaryState
        try {
            final double[] z2 = { 14, 7, b };
            efode.setPrimaryState(z2);

        } catch (final DimensionMismatchException e) {
            // expected behavior
        }
        // tests if (secondaryState.length != localArray.length) in method setSecondaryState
        try {
            final double[] z2 = { 14, 7, b };
            efode.setSecondaryState(0, z2);

        } catch (final DimensionMismatchException e) {
            // expected behavior
        }
        // tests if (completeState.length != getTotalDimension()) in method setCompleteState
        try {
            final double[] z2 = { 14, 7, b };
            efode.setCompleteState(z2);

        } catch (final DimensionMismatchException e) {
            // expected behavior
        }
    }

    @Test
    public void testFinalResult()
                                 throws MaxCountExceededException, DimensionMismatchException,
                                 NumberIsTooSmallException, NoBracketingException,
                                 UnknownParameterException, MismatchedEquations {

        final AbstractIntegrator integ =
            new DormandPrince54Integrator(1.0e-8, 100.0, new double[] { 1.0e-10, 1.0e-10 }, new double[] { 1.0e-10,
                1.0e-10 });
        double[] y = new double[] { 0.0, 1.0 };
        final Circle circle = new Circle(y, 1.0, 1.0, 0.1);

        final JacobianMatrices jacob = new JacobianMatrices(circle, CX, CY, OMEGA);
        jacob.addParameterJacobianProvider(circle);
        jacob.setInitialMainStateJacobian(circle.exactDyDy0(0));
        jacob.setInitialParameterJacobian(CX, circle.exactDyDcx(0));
        jacob.setInitialParameterJacobian(CY, circle.exactDyDcy(0));
        jacob.setInitialParameterJacobian(OMEGA, circle.exactDyDom(0));

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(circle);
        efode.setTime(0);
        efode.setPrimaryState(y);
        jacob.registerVariationalEquations(efode);

        integ.setMaxEvaluations(5000);

        final double t = 18 * FastMath.PI;
        integ.integrate(efode, t);
        y = efode.getPrimaryState();
        for (int i = 0; i < y.length; ++i) {
            Assert.assertEquals(circle.exactY(t)[i], y[i], 1.0e-9);
        }

        final double[][] dydy0 = new double[2][2];
        jacob.getCurrentMainSetJacobian(dydy0);
        for (int i = 0; i < dydy0.length; ++i) {
            for (int j = 0; j < dydy0[i].length; ++j) {
                Assert.assertEquals(circle.exactDyDy0(t)[i][j], dydy0[i][j], 1.0e-9);
            }
        }
        final double[] dydcx = new double[2];
        jacob.getCurrentParameterJacobian(CX, dydcx);
        for (int i = 0; i < dydcx.length; ++i) {
            Assert.assertEquals(circle.exactDyDcx(t)[i], dydcx[i], 1.0e-7);
        }
        final double[] dydcy = new double[2];
        jacob.getCurrentParameterJacobian(CY, dydcy);
        for (int i = 0; i < dydcy.length; ++i) {
            Assert.assertEquals(circle.exactDyDcy(t)[i], dydcy[i], 1.0e-7);
        }
        final double[] dydom = new double[2];
        jacob.getCurrentParameterJacobian(OMEGA, dydom);
        for (int i = 0; i < dydom.length; ++i) {
            Assert.assertEquals(circle.exactDyDom(t)[i], dydom[i], 1.0e-7);
        }
    }

    @Test
    public void testParameterizable()
                                     throws MaxCountExceededException, DimensionMismatchException,
                                     NumberIsTooSmallException, NoBracketingException,
                                     UnknownParameterException, MismatchedEquations {

        final AbstractIntegrator integ =
            new DormandPrince54Integrator(1.0e-8, 100.0, new double[] { 1.0e-10, 1.0e-10 }, new double[] { 1.0e-10,
                1.0e-10 });
        double[] y = new double[] { 0.0, 1.0 };
        final ParameterizedCircle pcircle = new ParameterizedCircle(y, 1.0, 1.0, 0.1);

        final double hP = 1.0e-12;
        final double hY = 1.0e-12;

        final JacobianMatrices jacob = new JacobianMatrices(pcircle, new double[] { hY, hY },
            ParameterizedCircle.CX, ParameterizedCircle.CY,
            ParameterizedCircle.OMEGA);
        jacob.setParameterizedODE(pcircle);
        jacob.setParameterStep(ParameterizedCircle.CX, hP);
        jacob.setParameterStep(ParameterizedCircle.CY, hP);
        jacob.setParameterStep(ParameterizedCircle.OMEGA, hP);
        jacob.setInitialMainStateJacobian(pcircle.exactDyDy0(0));
        jacob.setInitialParameterJacobian(ParameterizedCircle.CX, pcircle.exactDyDcx(0));
        jacob.setInitialParameterJacobian(ParameterizedCircle.CY, pcircle.exactDyDcy(0));
        jacob.setInitialParameterJacobian(ParameterizedCircle.OMEGA, pcircle.exactDyDom(0));

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(pcircle);
        efode.setTime(0);
        efode.setPrimaryState(y);
        jacob.registerVariationalEquations(efode);

        integ.setMaxEvaluations(50000);

        final double t = 18 * FastMath.PI;
        integ.integrate(efode, t);
        y = efode.getPrimaryState();
        for (int i = 0; i < y.length; ++i) {
            Assert.assertEquals(pcircle.exactY(t)[i], y[i], 1.0e-9);
        }

        final double[][] dydy0 = new double[2][2];
        jacob.getCurrentMainSetJacobian(dydy0);
        for (int i = 0; i < dydy0.length; ++i) {
            for (int j = 0; j < dydy0[i].length; ++j) {
                Assert.assertEquals(pcircle.exactDyDy0(t)[i][j], dydy0[i][j], 5.0e-4);
            }
        }

        final double[] dydp0 = new double[2];
        jacob.getCurrentParameterJacobian(ParameterizedCircle.CX, dydp0);
        for (int i = 0; i < dydp0.length; ++i) {
            Assert.assertEquals(pcircle.exactDyDcx(t)[i], dydp0[i], 5.0e-4);
        }

        final double[] dydp1 = new double[2];
        jacob.getCurrentParameterJacobian(ParameterizedCircle.OMEGA, dydp1);
        for (int i = 0; i < dydp1.length; ++i) {
            Assert.assertEquals(pcircle.exactDyDom(t)[i], dydp1[i], 1.0e-2);
        }
    }

    /**
     * For coverage purposes, checks method getParametersNames, isSupported (return false)
     * and complainIfNotSupported (if !isSupported(name))
     */
    @Test(expected = UnknownParameterException.class)
    public void testCoverageAbstractParameterizable() {

        final double[] y = new double[] { 0.0, 1.0 };
        final ParameterizedCircle pcircle = new ParameterizedCircle(y, 1.0, 1.0, 0.1);

        // tests method getParametersNames
        final Collection<String> list = pcircle.getParametersNames();
        Assert.assertEquals(list.size(), 3);
        Assert.assertTrue(list.contains(CX));
        Assert.assertTrue(list.contains(CY));
        Assert.assertTrue(list.contains(OMEGA));

        // tests method isSupported, return false
        final boolean isSupported = pcircle.isSupported("cz");
        Assert.assertFalse(isSupported);

        // tests method complainIfNotSupported if (!isSupported(name)) : exception should occur
        pcircle.complainIfNotSupported("raan");
    }

    /**
     * For coverage purposes, tests if (parameters == null) in second constructor
     */
    @Test
    public void testConstructorNullParameter() {
        final String[] stringNull = null;
        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final JacobianMatrices jacob = new JacobianMatrices(brusselator, stringNull);
        try {
            jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });
            Assert.fail();
        } catch (final NullPointerException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * For coverage purposes, tests if (expandable.getPrimary() != ode) in
     * method registerVariationalEquations.
     */
    @Test(expected = MismatchedEquations.class)
    public void testExceptionRegisterVariationalEquations() {

        final Brusselator brusselator = new Brusselator(1.0);

        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

        final Brusselator brusselator2 = new Brusselator(14.7);
        final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator2);
        jacob.registerVariationalEquations(efode);
    }

    /**
     * For coverage purposes, tests throw new UnknownParameterException(parameter); in
     * method setParameterStep.
     */
    @Test(expected = UnknownParameterException.class)
    public void testExceptionSetParameterStep() {

        final Brusselator brusselator = new Brusselator(1.0);
        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });
        jacob.setParameterStep("CZ", 7);
    }

    /**
     * For coverage purposes, tests if (efode != null) in
     * method setInitialMainStateJacobian.
     */
    @Test
    public void testSetInitialMainStateJacobian() {

        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final double[] z = { 1.3, b };

        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
        efode.setTime(0);
        efode.setPrimaryState(z);
        jacob.registerVariationalEquations(efode);
        final double[][] dYdY0 = { { 1, 0 }, { 0, 1 } };
        jacob.setInitialMainStateJacobian(dYdY0);

        final double[][] dYdY0bis = new double[2][2];
        jacob.getCurrentMainSetJacobian(dYdY0bis);
        Assert.assertArrayEquals(dYdY0, dYdY0bis);
    }

    /**
     * For coverage purposes, tests if (efode != null) in
     * method setInitialParameterJacobian.
     */
    @Test
    public void testSetInitialParameterJacobian() {

        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final double[] z = { 1.3, b };

        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
        efode.setTime(0);
        efode.setPrimaryState(z);
        jacob.registerVariationalEquations(efode);
        final double[] dYdY0 = { 0, 1 };
        jacob.setInitialParameterJacobian(Brusselator.B, dYdY0);

        final double[] dYdY0bis = new double[2];
        jacob.getCurrentParameterJacobian(Brusselator.B, dYdY0bis);
        Assert.assertArrayEquals(dYdY0, dYdY0bis, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * For coverage purposes, tests throw new UnknownParameterException(parameter); in
     * method setInitialParameterJacobian.
     */
    @Test(expected = UnknownParameterException.class)
    public void testExceptionSetInitialParameterJacobian() {
        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final double[] z = { 1.3, b };

        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
        efode.setTime(0);
        efode.setPrimaryState(z);
        jacob.registerVariationalEquations(efode);
        final double[] dYdY0 = { 0, 1 };
        jacob.setInitialParameterJacobian("CZ", dYdY0);
    }

    /**
     * For coverage purposes, tests throw new UnknownParameterException(parameter); in
     * method setInitialParameterJacobian.
     */
    @Test
    public void testExceptionGetCurrentParameterJacobian() {
        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final double[] z = { 1.3, b };

        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 1.0 });

        final ExpandableStatefulODE efode = new ExpandableStatefulODE(brusselator);
        efode.setTime(0);
        efode.setPrimaryState(z);
        jacob.registerVariationalEquations(efode);
        final double[] dYdY0 = { 0, 1 };
        jacob.setInitialParameterJacobian(Brusselator.B, dYdY0);

        final double[] dYdY0bis = new double[2];
        jacob.getCurrentParameterJacobian("CZ", dYdY0bis);

        // since CZ is not a valid parameter for brusselator, it cannot find the column
        // in the jacobian matrix that is associated with, therefore it returns zero
        // BUT it should not ! it should warn the user that the string pname is unknows
        // and throw UnknownParameterException like setInitialParameterJacobian
        final double[] dYdY0bisref = { 0, 0 };
        Assert.assertArrayEquals(dYdY0bisref, dYdY0bis, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * For coverage purposes, tests throw new DimensionMismatchException in
     * private method checkDimension. To call it, we goes through method setInitialMainStateJacobian.
     */
    @Test(expected = DimensionMismatchException.class)
    public void testExceptionCheckDimension() {

        final double b = 2.88;
        final Brusselator brusselator = new Brusselator(b);
        final JacobianMatrices jacob = new JacobianMatrices(brusselator, Brusselator.B);
        jacob.addParameterJacobianProvider(brusselator);
        jacob.setInitialParameterJacobian(Brusselator.B, new double[] { 0.0, 0.0, 1.0 });
    }

    /**
     * For coverage purposes, tests if (hY.length != ode.getDimension()) in
     * constructor of private class MainStateJacobianWrapper.
     * To call it, we goes through the first constructor of JacobianMatrices.
     * 
     */
    @Test(expected = DimensionMismatchException.class)
    public void testExceptionMainStateJacobianWrapper() {

        final double[] y = new double[] { 0.0, 1.0 };
        final ParameterizedCircle pcircle = new ParameterizedCircle(y, 1.0, 1.0, 0.1);

        final double hY = 1.0e-12;
        new JacobianMatrices(pcircle, new double[] { hY, hY, hY },
            ParameterizedCircle.CX, ParameterizedCircle.CY,
            ParameterizedCircle.OMEGA);
    }

    // -----------------------------------------------------------------------
    private static class Brusselator extends AbstractParameterizable
        implements MainStateJacobianProvider, ParameterJacobianProvider {

        public static final String B = "b";

        private final double b;

        public Brusselator(final double b) {
            super(B);
            this.b = b;
        }

        @Override
        public int getDimension() {
            return 2;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            final double prod = y[0] * y[0] * y[1];
            yDot[0] = 1 + prod - (this.b + 1) * y[0];
            yDot[1] = this.b * y[0] - prod;
        }

        @Override
        public void computeMainStateJacobian(final double t, final double[] y, final double[] yDot,
                                             final double[][] dFdY) {
            final double p = 2 * y[0] * y[1];
            final double y02 = y[0] * y[0];
            dFdY[0][0] = p - (1 + this.b);
            dFdY[0][1] = y02;
            dFdY[1][0] = this.b - p;
            dFdY[1][1] = -y02;
        }

        @Override
        public void computeParameterJacobian(final double t, final double[] y, final double[] yDot,
                                             final String paramName, final double[] dFdP) {
            if (this.isSupported(paramName)) {
                dFdP[0] = -y[0];
                dFdP[1] = y[0];
            } else {
                dFdP[0] = 0;
                dFdP[1] = 0;
            }
        }

        public double dYdP0() {
            return -1088.232716447743 + (1050.775747149553 + (-339.012934631828 + 36.52917025056327 * this.b) * this.b)
                * this.b;
        }

        public double dYdP1() {
            return 1502.824469929139 + (-1438.6974831849952 + (460.959476642384 - 49.43847385647082 * this.b) * this.b)
                * this.b;
        }

    }

    private static class ParamBrusselator extends AbstractParameterizable
        implements FirstOrderDifferentialEquations, ParameterizedODE {

        public static final String B = "b";

        private double b;

        public ParamBrusselator(final double b) {
            super(B);
            this.b = b;
        }

        @Override
        public int getDimension() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        public double getParameter(final String name)
                                                     throws UnknownParameterException {
            this.complainIfNotSupported(name);
            return this.b;
        }

        /** {@inheritDoc} */
        @Override
        public void setParameter(final String name, final double value)
                                                                       throws UnknownParameterException {
            this.complainIfNotSupported(name);
            this.b = value;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            final double prod = y[0] * y[0] * y[1];
            yDot[0] = 1 + prod - (this.b + 1) * y[0];
            yDot[1] = this.b * y[0] - prod;
        }

        public double dYdP0() {
            return -1088.232716447743 + (1050.775747149553 + (-339.012934631828 + 36.52917025056327 * this.b) * this.b)
                * this.b;
        }

        public double dYdP1() {
            return 1502.824469929139 + (-1438.6974831849952 + (460.959476642384 - 49.43847385647082 * this.b) * this.b)
                * this.b;
        }

    }

    /** ODE representing a point moving on a circle with provided center and angular rate. */
    private static class Circle extends AbstractParameterizable
        implements MainStateJacobianProvider, ParameterJacobianProvider {

        private final double[] y0;
        private final double cx;
        private final double cy;
        private final double omega;

        public Circle(final double[] y0, final double cx, final double cy, final double omega) {
            super(CX, CY, OMEGA);
            this.y0 = y0.clone();
            this.cx = cx;
            this.cy = cy;
            this.omega = omega;
        }

        @Override
        public int getDimension() {
            return 2;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            yDot[0] = this.omega * (this.cy - y[1]);
            yDot[1] = this.omega * (y[0] - this.cx);
        }

        @Override
        public void computeMainStateJacobian(final double t, final double[] y,
                                             final double[] yDot, final double[][] dFdY) {
            dFdY[0][0] = 0;
            dFdY[0][1] = -this.omega;
            dFdY[1][0] = this.omega;
            dFdY[1][1] = 0;
        }

        @Override
        public void
                computeParameterJacobian(final double t, final double[] y, final double[] yDot,
                                         final String paramName, final double[] dFdP)
                                                                                     throws UnknownParameterException {
            this.complainIfNotSupported(paramName);
            if (paramName.equals(CX)) {
                dFdP[0] = 0;
                dFdP[1] = -this.omega;
            } else if (paramName.equals(CY)) {
                dFdP[0] = this.omega;
                dFdP[1] = 0;
            } else {
                dFdP[0] = this.cy - y[1];
                dFdP[1] = y[0] - this.cx;
            }
        }

        public double[] exactY(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            final double dx0 = this.y0[0] - this.cx;
            final double dy0 = this.y0[1] - this.cy;
            return new double[] {
                this.cx + cos * dx0 - sin * dy0,
                this.cy + sin * dx0 + cos * dy0
            };
        }

        public double[][] exactDyDy0(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            return new double[][] {
                { cos, -sin },
                { sin, cos }
            };
        }

        public double[] exactDyDcx(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            return new double[] { 1 - cos, -sin };
        }

        public double[] exactDyDcy(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            return new double[] { sin, 1 - cos };
        }

        public double[] exactDyDom(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            final double dx0 = this.y0[0] - this.cx;
            final double dy0 = this.y0[1] - this.cy;
            return new double[] { -t * (sin * dx0 + cos * dy0), t * (cos * dx0 - sin * dy0) };
        }

    }

    /** ODE representing a point moving on a circle with provided center and angular rate. */
    private static class ParameterizedCircle extends AbstractParameterizable
        implements FirstOrderDifferentialEquations, ParameterizedODE {

        public static final String CX = "cx";
        public static final String CY = "cy";
        public static final String OMEGA = "omega";

        private final double[] y0;
        private double cx;
        private double cy;
        private double omega;

        public ParameterizedCircle(final double[] y0, final double cx, final double cy, final double omega) {
            super(CX, CY, OMEGA);
            this.y0 = y0.clone();
            this.cx = cx;
            this.cy = cy;
            this.omega = omega;
        }

        @Override
        public int getDimension() {
            return 2;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            yDot[0] = this.omega * (this.cy - y[1]);
            yDot[1] = this.omega * (y[0] - this.cx);
        }

        @Override
        public double getParameter(final String name)
                                                     throws UnknownParameterException {
            if (name.equals(CX)) {
                return this.cx;
            } else if (name.equals(CY)) {
                return this.cy;
            } else if (name.equals(OMEGA)) {
                return this.omega;
            } else {
                throw new UnknownParameterException(name);
            }
        }

        @Override
        public void setParameter(final String name, final double value)
                                                                       throws UnknownParameterException {
            if (name.equals(CX)) {
                this.cx = value;
            } else if (name.equals(CY)) {
                this.cy = value;
            } else if (name.equals(OMEGA)) {
                this.omega = value;
            } else {
                throw new UnknownParameterException(name);
            }
        }

        public double[] exactY(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            final double dx0 = this.y0[0] - this.cx;
            final double dy0 = this.y0[1] - this.cy;
            return new double[] {
                this.cx + cos * dx0 - sin * dy0,
                this.cy + sin * dx0 + cos * dy0
            };
        }

        public double[][] exactDyDy0(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            return new double[][] {
                { cos, -sin },
                { sin, cos }
            };
        }

        public double[] exactDyDcx(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            return new double[] { 1 - cos, -sin };
        }

        public double[] exactDyDcy(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            return new double[] { sin, 1 - cos };
        }

        public double[] exactDyDom(final double t) {
            final double cos = MathLib.cos(this.omega * t);
            final double sin = MathLib.sin(this.omega * t);
            final double dx0 = this.y0[0] - this.cx;
            final double dy0 = this.y0[1] - this.cy;
            return new double[] { -t * (sin * dx0 + cos * dy0), t * (cos * dx0 - sin * dy0) };
        }

    }

}
