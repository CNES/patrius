/**
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
 *
 * @history Created 24/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:changed partial derivatives API
 * VERSION::DM:241:01/10/2014:improved tides conception
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.FES2004FormatReader;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsFactory;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link OceanTides}
 * 
 * @author Rami Houdroge, Thomas Trapier
 * 
 * @version $Id: OceanTidesTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class OceanTidesTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ocean tides
         * 
         * @featureDescription ocean tides potential validation
         * 
         * @coveredRequirements DV-MOD_210
         */
        OCEAN_TIDES
    }

    /**
     * Absolute validation threshold for the CZ
     */
    private static final double EPS_CZ = 1.e-18;

    /**
     * Absolute validation threshold for the acceleration
     */
    private static final double EPS_ACC = 1.e-16;

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES}
     * 
     * @testedMethod {@link OceanTides#getParametersNames()}
     * @testedMethod {@link OceanTides#getParameter(String)}
     * @testedMethod {@link OceanTides#setParameter()}
     * 
     * @description Test for the parameters
     * 
     * @input a parameter
     * 
     * @output its value
     * 
     * @testPassCriteria the parameter value is as expected exactly (0 ulp difference)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamList() throws PatriusException, IOException, ParseException {

        // mu from grim4s4_gr potential file
        final double mu = 3.9860043770442000E+14;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getITRF();
        // equatorial radius
        final double eqR = 6.3781360000000000E+06;
        // density
        final double density = 1.025e3;
        // degree and order
        final int degree = 10;
        final int order = 10;
        // coefficients
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // standards
        final TidesStandard standard2004 = TidesStandard.GINS2004;
        // ocean tide data
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider,
            standard2004);

        new AbsoluteDate(2005, 03, 05, 00, 24, 0.0,
            TimeScalesFactory.getTAI());

        // Test without admittance
        // ========================

        final OceanTides model = new OceanTides(referenceFrame, new Parameter("eqR", eqR),
            new Parameter("mu", mu), new Parameter("density", density), degree, order, false,
            dataProvider);

        double k = 5;
        Assert.assertEquals(3, model.getParameters().size());
        for (int i = 0; i < model.getParameters().size(); i++) {
            final Parameter p = model.getParameters().get(i);
            p.setValue(k);
            Assert.assertTrue(Precision.equals(k, model.getParameters().get(i).getValue(), 0));
            k++;
        }

        // coefficients with missing main waves
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes_test"));
        final OceanTidesCoefficientsProvider providerMissing = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        new OceanTidesDataProvider(providerMissing,
            standard2004);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES}
     * 
     * @testedMethod {@link OceanTides#getNormalizedCCoefs(AbsoluteDate)}
     * @testedMethod {@link OceanTides#computeAcceleration(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @description unitary validation of the Cz and CS computation, and of the acceleration for a
     *              given input date and position
     * 
     * @input a date, a spacecraft position
     * 
     * @output the Cz and CS coefficients, the associated acceleration
     * 
     * @testPassCriteria the output values are close enough to the references provided from OBELIX.
     * 
     * @throws PatriusException if fails
     * @throws IOException if fails
     * @throws ParseException if fails
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testUT() throws PatriusException, IOException, ParseException {

        // mu from grim4s4_gr potential file
        final double mu = 3.9860043770442000E+14;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getITRF();
        // equatorial radius
        final double eqR = 6.3781360000000000E+06;
        // density
        final double density = 1.025e3;
        // degree and order
        final int degree = 10;
        final int order = 10;
        // coefficients
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // standards
        final TidesStandard standard2004 = TidesStandard.GINS2004;
        final TidesStandard standard1996 = TidesStandard.IERS1996;
        final TidesStandard standard2003 = TidesStandard.IERS2003;
        // ocean tide data
        OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider, standard2004);

        // initial date from ZOOM reference
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 05, 00, 24, 0.0,
            TimeScalesFactory.getTAI());

        // Test without admittance
        // ========================

        OceanTides tides = new OceanTides(referenceFrame, eqR, mu, density, degree, order, false,
            dataProvider);

        double[][] actNormC = tides.getNormalizedCCoefs(date);

        // expected CZ
        final double[] expectedCz = { 0.0000000000000000E+00, 0.0000000000000000E+00,
            3.2884317321007197E-12, -3.0413126961996677E-11, 1.9991280202169836E-10,
            4.9218114834870894E-11, -8.6358278803254324E-11, -5.1022917402405809E-11,
            -1.0842518888562784E-12, 3.5410175127798223E-11, -1.4831141587431299E-11 };

        for (int i = 0; i < actNormC.length; i++) {
            Assert.assertEquals(expectedCz[i], actNormC[i][0], EPS_CZ);
        }

        // Test with admittance
        // =====================

        tides = new OceanTides(referenceFrame, eqR, mu, density, degree, order, true, dataProvider);

        // pos-vel from ZOOM ephemeris reference
        final Vector3D pos = new Vector3D(-2.79306866434749449e+06, 3.41217220682059508e+06,
            -5.09298279238691647e+06);
        final Vector3D vel = new Vector3D(3.45746908686972165e+03, -4.65772889841155120e+03,
            -5.01812485729848777e+03);

        actNormC = tides.getNormalizedCCoefs(date);

        // expected CZ
        final double[] expectedCzAdm = { 0.0000000000000000E+00, 0.0000000000000000E+00,
            6.1397020793376611E-11, 2.0802124481513053E-11, 1.8191616314201633E-10,
            2.1833262994617135E-11, -6.1681912214707271E-11, -5.4080774260560920E-11,
            -1.3377185229927174E-11, 3.0637445612099374E-11, -2.3739862252292308E-11 };

        for (int i = 0; i < actNormC.length; i++) {
            Assert.assertEquals(expectedCzAdm[i], actNormC[i][0], EPS_CZ);
        }

        // pv
        final PVCoordinates pv = new PVCoordinates(pos, vel);

        // frame
        final Frame frame = FramesFactory.getITRF();

        // acceleration computation
        final Vector3D acc = tides.computeAcceleration(pv, frame, date);

        Assert.assertEquals(-3.2467914146055085E-08, acc.getX(), EPS_ACC);
        Assert.assertEquals(7.8551772350059931E-08, acc.getY(), EPS_ACC);
        Assert.assertEquals(-6.3034511237644326E-08, acc.getZ(), EPS_ACC);

        // COVERAGE UT
        // =============
        // ocean tide data
        dataProvider = new OceanTidesDataProvider(provider, standard1996);

        tides = new OceanTides(referenceFrame, eqR, mu, density, degree, order, true, dataProvider);

        final EventDetector[] eventArray = tides.getEventsDetectors();
        Assert.assertEquals(0, eventArray.length);

        // table creation test
        actNormC = tides.getNormalizedCCoefs(date);
        actNormC = tides.getNormalizedSCoefs(date.shiftedBy(15 * Constants.JULIAN_DAY));
        actNormC = tides.getDenormalizedCCoefs(date.shiftedBy(40 * Constants.JULIAN_DAY));
        actNormC = tides.getDenormalizedSCoefs(date.shiftedBy(-2 * Constants.JULIAN_DAY));

        // coefficients with missing main waves
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes_test"));
        final OceanTidesCoefficientsProvider providerMissing = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // ocean tide data
        final OceanTidesDataProvider dataProvider2004 = new OceanTidesDataProvider(providerMissing,
            standard2004);

        try {
            tides = new OceanTides(referenceFrame, eqR, mu, density, degree, order, true,
                dataProvider2004);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        final OceanTidesDataProvider dataProvider1996 = new OceanTidesDataProvider(providerMissing,
            standard1996);
        final OceanTidesDataProvider dataProvider2003 = new OceanTidesDataProvider(providerMissing,
            standard2003);

        for (int i = 0; i < dataProvider1996.getLoveNumbers().length; i++) {
            Assert.assertEquals(dataProvider1996.getLoveNumbers()[i],
                dataProvider2003.getLoveNumbers()[i], 0.0);
        }
    }

/**
     * @testType UT
     *
     * @testedFeature {@link features#OCEAN_TIDES}
     *
     * @testedMethod {@link OceanTides#OceanTides(Frame, double, double, double, int, int, boolean, IOceanTidesDataProvider, boolean)}
     * @testedMethod {@link OceanTides#OceanTides(Frame, Parameter, Parameter, Parameter, int, int, boolean, IOceanTidesDataProvider, boolean)
     *
     * @description compute acceleration partial derivatives wrt position
     *
     * @input instances of {@link OceanTides}
     *
     * @output partial derivatives
     *
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction
     *
     * @throws PatriusException
     *             when an Orekit error occurs
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testNullPD() throws PatriusException {

        // mu from grim4s4_gr potential file
        final double mu = 3.9860043770442000E+14;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getITRF();
        // equatorial radius
        final double eqR = 6.3781360000000000E+06;
        // density
        final double density = 1.025e3;
        // degree and order
        final int degree = 10;
        final int order = 10;
        // coefficients
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // standards
        final TidesStandard standard2004 = TidesStandard.GINS2004;

        // ocean tide data
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider, standard2004);

        // initial date
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 05, 00, 24, 0.0,
            TimeScalesFactory.getTAI());

        // instantiations
        final OceanTides tides = new OceanTides(referenceFrame, eqR, mu, density, degree, order, 0,
            0, false, dataProvider);
        final OceanTides tides2 = new OceanTides(referenceFrame, new Parameter("eqr", eqR),
            new Parameter("mu", mu), new Parameter("density", density), degree, order, 0, 0,
            false, dataProvider);

        // Check that derivatives computation is deactivated
        Assert.assertFalse(tides.computeGradientPosition());
        // Derivatives wrt velocity are always null for tides
        Assert.assertFalse(tides.computeGradientVelocity());

        // Spacecraft
        final PVCoordinates pv = new PVCoordinates(new Vector3D(2.70303160815657163e+06,
            6.15588486808402184e+06, -1.16119700511837618e+04), new Vector3D(
            -7.06109645777311016e+03, 3.08016738885103905e+03, 1.36108059143140654e+01));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, mu);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final double[][] dAccdPos2 = new double[3][3];
        final double[][] dAccdVel2 = new double[3][3];

        // Compute partial derivatives
        tides.addDAccDState(scr, dAccdPos, dAccdVel);
        tides2.addDAccDState(scr, dAccdPos2, dAccdVel2);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdPos2[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
                Assert.assertEquals(0, dAccdVel2[i][j], 0);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link OceanTides#OceanTides(Frame, double, double, double, int, int, int, int, boolean, IOceanTidesDataProvider)}
     * @testedMethod {@link OceanTides#OceanTides(Frame, Parameter, Parameter, Parameter, int, int, int, int, boolean, IOceanTidesDataProvider)}
     * 
     * @description This test checks that:
     *              <ul>
     *              <li>The numerical propagation of a given orbit using instances of OceanTides with fixed degree/order
     *              (60, 60) for acceleration but different degree/order (60, 60) and (59, 59) for partial derivatives
     *              lead to the same [position, velocity] state but slighty different state transition matrix.</li>
     *              <li>The partial derivatives of model (60, 60) for acceleration and (59, 59) for partial derivatives
     *              are the same than of model (59, 59) for acceleration and (59, 59) for partial derivatives.</li>
     *              <ul>
     * 
     * @input instances of {@link OceanTides}
     * 
     * @output positions, velocities of final orbits, partials derivatives
     * 
     * @testPassCriteria the [positions, velocities] must be equals, state transition matrix
     *                   "almost" the same (relative difference < 1E-2)
     * 
     * @throws PatriusException when an Orekit error occurs
     * @throws ParseException
     * @throws IOException
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationDifferentDegreeOrder() throws PatriusException, IOException,
                                                     ParseException {

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // constants
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // SpacecraftState
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 05, 00, 24, 0.0,
            TimeScalesFactory.getTAI());
        final KeplerianOrbit orbit = new KeplerianOrbit(7E6, 0.001, 0.93, 0, 0, 0,
            PositionAngle.TRUE, gcrf, date, mu);
        SpacecraftState state1 = new SpacecraftState(orbit);
        SpacecraftState state2 = new SpacecraftState(orbit);
        final double t = orbit.getKeplerianPeriod();

        // gravity
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // standards
        final TidesStandard standard2004 = TidesStandard.GINS2004;
        // ocean tide data
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider,
            standard2004);

        // Create 2 instances of OceanTides with different degrees/orders
        final OceanTides model1 = new OceanTides(itrf, 6378E3, mu, 1.025e3, 60, 60, 60, 60, false,
            dataProvider);
        final OceanTides model2 = new OceanTides(itrf, new Parameter("eqr", 6378E3), new Parameter(
            "mu", mu), new Parameter("density", 1.025e3), 60, 60, 59, 59, false, dataProvider);
        final OceanTides model3 = new OceanTides(itrf, 6378E3, mu, 1.025e3, 59, 59, 59, 59, false,
            dataProvider);

        // Propagators
        final double step = 60;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);
        final NumericalPropagator prop1 = new NumericalPropagator(integrator);
        final NumericalPropagator prop2 = new NumericalPropagator(integrator);

        final PartialDerivativesEquations eq1 = new PartialDerivativesEquations("partial", prop1);
        state1 = eq1.setInitialJacobians(state1);
        prop1.setInitialState(state1);
        prop1.addForceModel(model1);
        final PartialDerivativesEquations eq2 = new PartialDerivativesEquations("partial", prop2);
        state2 = eq2.setInitialJacobians(state2);
        prop2.setInitialState(state2);
        prop2.addForceModel(model2);

        // Propagation : final state
        final SpacecraftState FinalState1 = prop1.propagate(date.shiftedBy(t));
        final SpacecraftState FinalState2 = prop2.propagate(date.shiftedBy(t));

        // Positions and velocities must be the same whereas degrees/orders are different for each
        // model
        final Vector3D pos1 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D pos2 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D vel1 = FinalState2.getPVCoordinates().getVelocity();
        final Vector3D vel2 = FinalState2.getPVCoordinates().getVelocity();

        Assert.assertEquals(0., pos1.distance(pos2), 0.);
        Assert.assertEquals(0., vel1.distance(vel2), 0.);

        // Check that partial derivatives are different, but "nearly" the same
        final double epsilon = 2.0E-3;
        final double[] stm1 = FinalState1.getAdditionalState("partial");
        final double[] stm2 = FinalState2.getAdditionalState("partial");
        for (int i = 0; i < stm1.length; i++) {
            Assert.assertEquals(0., (stm1[i] - stm2[i]) / stm1[i], epsilon);
            Assert.assertFalse(stm1[i] == stm2[i]);
        }

        // Check that different instances of OceanTides returns same partial derivatives
        final double[][] dAccdPos = new double[6][6];
        final double[][] dAccdVel = new double[6][6];
        final double[][] dAccdPos2 = new double[6][6];
        final double[][] dAccdVel2 = new double[6][6];
        model2.addDAccDState(state1, dAccdPos, dAccdVel);
        model3.addDAccDState(state1, dAccdPos2, dAccdVel2);

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                Assert.assertEquals(dAccdPos[j][k], dAccdPos2[j][k], 0);
                Assert.assertEquals(dAccdVel[j][k], dAccdVel2[j][k], 0);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES}
     * 
     * @testedMethod {@link OceanTides#addContribution(SpacecraftState, TimeDerivativesEquations)}
     * 
     * @description add force contribution to the numerical propagator
     * 
     * @input corrections related to ocean tides.
     * 
     * @output each corrective component due to ocean tides
     * 
     * @testPassCriteria the tested methods run with no error.
     * 
     * @throws PatriusException when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testContrib() throws PatriusException {

        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes_test"));

        // mu from grim4s4_gr potential file
        final double mu = 3.9860043770442000E+14;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getITRF();
        // equatorial radius
        final double eqR = 6.3781360000000000E+06;
        // density
        final double density = 1.025e3;
        // degree and order
        final int degree = 10;
        final int order = 10;
        // coefficients
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // standard
        final TidesStandard standard = TidesStandard.GINS2004;
        // ocean tide data
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider, standard);

        // initial date from ZOOM reference
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 05, 00, 24, 0.0,
            TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = date.shiftedBy(10.);

        final OceanTides tides = new OceanTides(referenceFrame, eqR, mu, density, degree, order,
            false, dataProvider);

        // test addContribution method
        final PVCoordinates pv = new PVCoordinates(new Vector3D(2.70303160815657163e+06,
            6.15588486808402184e+06, -1.16119700511837618e+04), new Vector3D(
            -7.06109645777311016e+03, 3.08016738885103905e+03, 1.36108059143140654e+01));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, mu);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final NumericalPropagator calc = new NumericalPropagator(new GraggBulirschStoerIntegrator(
            10.0, 30.0, 0, 1.0e-5));
        calc.addForceModel(tides);

        calc.setInitialState(scr);
        calc.propagate(finalDate);
        final PVCoordinates finalPV = calc.getPVCoordinates(finalDate, FramesFactory.getGCRF());
        final double[] actualPV = { finalPV.getPosition().getX(), finalPV.getPosition().getY(),
            finalPV.getPosition().getZ() };

        // OREKIT results
        final double[] expectedPV = { 2632244.916143098, 6186282.151422986, -11475.103427438222 };

        Assert.assertArrayEquals(expectedPV, actualPV, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException model creation
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        // mu from grim4s4_gr potential file
        final double mu = 3.9860043770442000E+14;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getITRF();
        // equatorial radius
        final double eqR = 6.3781360000000000E+06;
        // density
        final double density = 1.025e3;
        // degree and order
        final int degree = 10;
        final int order = 10;
        // coefficients
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
            .getCoefficientsProvider();
        // standards
        final TidesStandard standard2004 = TidesStandard.GINS2004;
        // ocean tide data
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider,
            standard2004);

        new AbsoluteDate(2005, 03, 05, 00, 24, 0.0,
            TimeScalesFactory.getTAI());

        // Test without admittance
        // ========================

        final OceanTides model = new OceanTides(referenceFrame, new Parameter("eqR", eqR),
            new Parameter("mu", mu), new Parameter("density", density), degree, order, false,
            dataProvider);

        Assert.assertEquals(dataProvider.getLoveNumbers()[0], model.getOceanTidesData()
            .getLoveNumbers()[0], 0);

    }

    /**
     * @throws PatriusException
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
    public void testEnrichParameterDescriptors() throws PatriusException {

        // mu from grim4s4_gr potential file
        final double mu = 3.9860043770442000E+14;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getITRF();
        // equatorial radius
        final double eqR = 6.3781360000000000E+06;
        // density
        final double density = 1.025e3;
        // degree and order
        final int degree = 10;
        final int order = 10;
        // coefficients
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
                "fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory
                .getCoefficientsProvider();
        // standards
        final TidesStandard standard2004 = TidesStandard.GINS2004;
        // ocean tide data
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider,
                standard2004);

        OceanTides forceModel = new OceanTides(referenceFrame, new Parameter("eqR", eqR),
                new Parameter("mu", mu), new Parameter("density", density), degree, order, false,
                dataProvider);

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }

        // Also check this other constructor which also needs to call the enrichParameterDescriptors
        // method
        forceModel = new OceanTides(referenceFrame, 6378E3, mu, 1.025e3, 60, 60, 60, 60, false,
                dataProvider);

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    @Before
    public void setup() throws PatriusException {

        // data location

        fr.cnes.sirius.patrius.Utils.setDataRoot("oceanTides");
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());
    }
}
