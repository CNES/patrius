/**
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for parameter methods
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
public class NewtonianAttractionTest {

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     */
    @Test
    public void testParamLists() throws PatriusException, IOException, ParseException {
        final String nameMu = "central attraction coefficient";
        final Parameter mu = new Parameter(nameMu, 5);
        final NewtonianAttraction force = new NewtonianAttraction(mu);

        Assert.assertEquals(1, force.getParameters().size());
        Assert.assertTrue(force.getParameters().get(0).getName().contains(nameMu));

        Assert.assertTrue(Precision.equals(force.getMu(), 5, 0));
        mu.setValue(4e14);
        Assert.assertTrue(Precision.equals(force.getMu(), 4e14, 0));

        final double[] dAccdParam = new double[6];
        final KeplerianOrbit orbit = new KeplerianOrbit(6.7e6, .1, .2, .3, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        force.addDAccDParam(new SpacecraftState(orbit), mu, dAccdParam);

        // something has been computed
        Assert.assertTrue(!Precision.equals(0, this.sum(dAccdParam), 0));

        final double[] absTolerance = {
            1.0e-6, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0., 10, absTolerance, relTolerance);
        integrator.setInitialStepSize(10);

        // Test when adding a Newtonian attraction to the propagator:
        final NumericalPropagator prop = new NumericalPropagator(integrator);
        prop.setMu(100.0);
        Assert.assertEquals(100.0, prop.getMu(), 0.0);
        final NewtonianAttraction model = new NewtonianAttraction(8.0);
        prop.addForceModel(model);
        // Check the gravitational coefficient has been updated:
        Assert.assertEquals(8.0, prop.getMu(), 0.0);
        // Check the NewtonianAttraction force has not been added to the model list:
        Assert.assertEquals(0, prop.getForceModels().size());

    }

    /**
     * Test for coverage purpose
     */
    @Test
    public void testGetEventsDetectors() {
        final NewtonianAttraction force = new NewtonianAttraction(5);
        final EventDetector[] detectors = force.getEventsDetectors();
        Assert.assertEquals(0, detectors.length);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NewtonianAttraction#NewtonianAttraction(double, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link NewtonianAttraction}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testNullPD() throws PatriusException {

        // Instance
        final String nameMu = "central attraction coefficient";
        final Parameter mu = new Parameter(nameMu, 5);
        final NewtonianAttraction force = new NewtonianAttraction(mu, false);

        // Computation is well deactivated
        Assert.assertFalse(force.computeGradientPosition());
        // Partial derivatives wrt velocity are always null
        Assert.assertFalse(force.computeGradientVelocity());

        // Spacecraft
        final KeplerianOrbit orbit = new KeplerianOrbit(6.7e6, .1, .2, .3, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(),
            new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        final SpacecraftState sp = new SpacecraftState(orbit);

        // Compute partial derivatives
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        force.addDAccDState(sp, dAccdPos, dAccdVel);

        // Check that all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link NewtonianAttraction#addDAccDParam(SpacecraftState, Parameter, double[]))}
     * 
     * @description Test exception thrown in case of unsupported parameter
     * 
     * @input an unsupported parameter
     * 
     * @output an exception
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test(expected = PatriusException.class)
    public final void testUnsupportedParam() throws PatriusException {
        final NewtonianAttraction force = new NewtonianAttraction(5);
        final double[] dAccdParam = new double[3];
        final KeplerianOrbit orbit = new KeplerianOrbit(6.7e6, .1, .2, .3, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        force.addDAccDParam(new SpacecraftState(orbit), new Parameter("toto", 1), dAccdParam);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NewtonianAttraction#computeAcceleration(SpacecraftState)}
     * 
     * @description compute acceleration with multiplicative factor k
     * 
     * @testPassCriteria acceleration with k = 5 = 5 * acceleration with k = 1
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public final void testMultiplicativeFactor() throws PatriusException {
        final double mu = Constants.WGS84_EARTH_MU;
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,mu));
        final NewtonianAttraction actualModel = new NewtonianAttraction(new Parameter("", mu), false);
        actualModel.setMultiplicativeFactor(5.);
        final NewtonianAttraction expectedModel = new NewtonianAttraction(new Parameter("", mu), false);
        
        // Acceleration
        final Vector3D actual = actualModel.computeAcceleration(state);
        final Vector3D expected = expectedModel.computeAcceleration(state).scalarMultiply(5.);
        Assert.assertEquals(expected, actual);
        // Partial derivatives
        final double[][] dAccdPosActual = new double[3][3];
        final double[][] dAccdVelActual = new double[3][3];
        actualModel.addDAccDState(state, dAccdPosActual, dAccdVelActual);
        final double[][] dAccdPosExpected = new double[3][3];
        final double[][] dAccdVelExpected = new double[3][3];
        expectedModel.addDAccDState(state, dAccdPosExpected, dAccdVelExpected);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(dAccdPosActual[i][j], dAccdPosExpected[i][j] * 5., 0.);
                Assert.assertEquals(dAccdVelActual[i][j], dAccdVelExpected[i][j] * 5., 0.);
            }
        }
        // K value
        Assert.assertEquals(5., actualModel.getMultiplicativeFactor(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ForceModel#enrichParameterDescriptors()}
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() {
        final String nameMu = "central attraction coefficient";
        final Parameter mu = new Parameter(nameMu, 5);
        final NewtonianAttraction forceModel = new NewtonianAttraction(mu);

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    double sum(final double[] d) {
        double temp = 0;
        for (final double dd : d) {
            temp += dd;
        }
        return temp;
    }
}