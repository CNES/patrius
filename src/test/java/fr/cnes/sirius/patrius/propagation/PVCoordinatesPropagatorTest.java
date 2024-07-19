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
 * @history created 01/10/2015
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:01/10/2015: Creation to test PVCoordinatePropagator
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvHermite;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvLagrange;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class tests PVCoordinatePropagator and SimpleAdditionalStateProvider.
 * </p>
 * 
 * @see PVCoordinatesPropagator
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 3.1
 * 
 */
public class PVCoordinatesPropagatorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle PVCoordinatePropagator constructors
         * 
         * @featureDescription Test the two constructors
         * 
         * @coveredRequirements None
         */
        CONSTRUCTOR,

        /**
         * @featureTitle PVCoordinatePropagator propagation
         * 
         * @featureDescription Tests the propagation method
         * 
         * @coveredRequirements None
         * 
         */
        PROPAGATION

    }

    /** Epsilon. */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;
    /** Propagation duration in days. */
    private static final int DAYS = 2;
    /** Propagation step in s. */
    private static final int STEP = 60;
    /** Table of spacecraft states */
    private static SpacecraftState[] tabSpacecraftStates;
    /** Propagation interval. */
    private static AbsoluteDateInterval interval;
    /** Reference frame. */
    private static Frame frame;
    /** Mu. */
    private static double mu;

    /**
     * Set up : table of spacecraft states given by a propagation performed with a numerical
     * propagator on a LEO orbit
     * 
     * @throws PatriusException
     *         should not happen
     */
    @BeforeClass
    public static final void setUpBeforeClass() throws PatriusException {

        Report.printClassHeader(PVCoordinatesPropagatorTest.class.getSimpleName(),
                "PV coordinates propagator");

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        // Dormand Prince integrator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.1, 500, absTOL,
                relTOL);

        // Numerical propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        // initial orbit : date = 01/01/2005 00:00:00.000 in TAI, a = 7200000 m, e = 0.001, i = 40
        // deg, po = 10 deg, go
        // = 15 deg, M = 20 deg in GCRF
        final AbsoluteDate date = new AbsoluteDate(2005, 1, 1, TimeScalesFactory.getTAI());
        frame = FramesFactory.getGCRF();
        mu = Constants.EGM96_EARTH_MU;
        final double a = 7200000;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.001, MathLib.toRadians(40),
                MathLib.toRadians(10), MathLib.toRadians(15), MathLib.toRadians(20),
                PositionAngle.MEAN, frame, date, mu);

        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu())));

        // constant attitude in local orbital frame
        final AttitudeProvider attProv = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH);
        propagator.setAttitudeProvider(attProv);
        final Attitude initialAttitude = attProv.getAttitude(initialOrbit, initialOrbit.getDate(),
                initialOrbit.getFrame());

        // interval of extrapolation defined by DAYS parameter
        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date,
                date.shiftedBy(DAYS * 24 * 3600), IntervalEndpointType.CLOSED);

        // add an additional state "TANK" with pressure and temperature constant to 21 bar, 18 deg.
        final AdditionalEquations addEquation = new PressureAndTemperatureLinear();
        propagator.addAdditionalEquations(addEquation);

        // initialize numerical propagator to produce a Spacecraftstate table with step STEP
        // parameter
        SpacecraftState initialSpacecraftState = new SpacecraftState(initialOrbit, initialAttitude);
        final double[] pressureTemp = { 21, 18 };
        initialSpacecraftState = initialSpacecraftState.addAdditionalState("TANK", pressureTemp);
        propagator.resetInitialState(initialSpacecraftState);
        propagator.setSlaveMode();

        // compute the tabulated spacecraft states
        tabSpacecraftStates = new SpacecraftState[(int) (interval.getDuration() / STEP)];
        AbsoluteDate currentDate = interval.getLowerData();
        for (int i = 0; i < tabSpacecraftStates.length; i++) {
            tabSpacecraftStates[i] = propagator.propagate(currentDate);
            currentDate = currentDate.shiftedBy(STEP);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link PVCoordinatesPropagator#PVCoordinatePropagator(org.orekit.utils.PVCoordinatesProvider,fr.cnes.sirius.patrius.time.AbsoluteDate, double, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description Test the instantiation without exception of a PVCoordinateProvider from an
     *              EphemerisPvHermite
     *              based on a table of spacecraft states computed with a numerical propagation in
     *              setup
     * 
     * @input EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null,
     *        null);
     * 
     * @output PVCoordinatePropagator pvCoordPropag
     * 
     * @testPassCriteria pvCoordPropag is not Null
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testPVCoordinatePropagator() throws PatriusException {
        final EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null,
                null);
        final PVCoordinatesPropagator pvCoordPropag = new PVCoordinatesPropagator(ephPvHermite,
                tabSpacecraftStates[0].getDate(), tabSpacecraftStates[0].getMu(),
                tabSpacecraftStates[0].getFrame());
        Assert.assertNotNull(pvCoordPropag);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link PVCoordinatesPropagator#PVCoordinatePropagator(org.orekit.utils.PVCoordinatesProvider, fr.cnes.sirius.patrius.time.AbsoluteDate, double, fr.cnes.sirius.patrius.frames.Frame, fr.cnes.sirius.patrius.attitudes.AttitudeProvider, fr.cnes.sirius.patrius.attitudes.AttitudeProvider, java.util.List)}
     * @testedMethod {@link SimpleAdditionalStateProvider#SimpleAdditionalStateProvider(String, AbsoluteDate[], double[][], ISearchIndex)}
     * @testedMethod {@link SimpleAdditionalStateProvider#getName()}
     * @testedMethod {@link SimpleAdditionalStateProvider#getAdditionalState(fr.cnes.sirius.patrius.time.AbsoluteDate)}
     * 
     * @description Test the instantiation without exception of a PVCoordinateProvider from an
     *              EphemerisPvHermite
     *              based on a table of spacecraft states computed with a numerical propagation in
     *              setup and associated
     *              to a table of
     *              additional states named "TANK" with values { 21 bar, 18 deg } with constant
     *              evolution of 1 (bar,
     *              deg) per second.
     *              For SimpleAdditionalStateProvider, test that a propagation to a date between two
     *              step return the
     *              expected result.
     * 
     * @input EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null,
     *        null);
     * @input SimpleAdditionalStateProvider simpleAddStateProv build from an extraction of the
     *        spacecraft states table
     *        computed in setup
     * @input PVCoordinatePropagator pvCoordPropag a propagator based on ephPvHermite and
     *        simpleAddStateProv
     * 
     * @output SpacecraftState sstate : the propagation state at a known date for additional states
     * 
     * @testPassCriteria pvCoordPropag is not Null
     * @testPassCriteria simpleAddStateProv is not Null
     * @testPassCriteria sstate contains <"TANK", {21.0+duration, 18.0+duration}> additional state
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testPVCoordinatePropagatorWithAdditionalStateProvider() throws PatriusException {

        // retrieve the additionnal states and dates from spacecraft states computed in setup
        final int length = tabSpacecraftStates.length;
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        final double[][] tabAdditionalStates = new double[length][];
        for (int i = 0; i < length; i++) {
            tabDate[i] = tabSpacecraftStates[i].getDate();
            tabAdditionalStates[i] = tabSpacecraftStates[i].getAdditionalState("TANK");
        }

        // build a simpleAdditionalStateProvider from these ones
        final SimpleAdditionalStateProvider simpleAddStateProv = new SimpleAdditionalStateProvider(
                "TANK", tabDate, tabAdditionalStates, null);
        // check if it has been correctly instantiate
        Assert.assertNotNull(simpleAddStateProv);

        // Build a list and add the last simpleAdditionalState
        final List<AdditionalStateProvider> listAddStatesProv = new ArrayList<>();
        listAddStatesProv.add(simpleAddStateProv);

        // build an ephPvHermite from the spacecraftstates computed in setup
        final EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null,
                null);

        // build a propagator from all these inputs
        final PVCoordinatesPropagator pvCoordPropag = new PVCoordinatesPropagator(ephPvHermite,
                tabSpacecraftStates[0].getDate(), tabSpacecraftStates[0].getMu(),
                tabSpacecraftStates[0].getFrame(), null, null, listAddStatesProv);
        Assert.assertNotNull(pvCoordPropag);

        // propagate to a date we know there is an additional state (k * STEP).
        // The additional state shall be "TANK" {21+duration, 18+duration}
        final SpacecraftState sstate = pvCoordPropag.propagate(interval.getLowerData().shiftedBy(
                10.5 * STEP));
        final Map<String, double[]> addStatesResult = sstate.getAdditionalStates();
        for (final String key : addStatesResult.keySet()) {
            Assert.assertTrue(key.equalsIgnoreCase("TANK"));
            final double[] addStatesValues = addStatesResult.get(key);
            Assert.assertEquals(addStatesValues.length, 2);
            Assert.assertEquals(0., (addStatesValues[0] - (21.0 + 10.5 * STEP))
                    / addStatesValues[0], EPS);
            Assert.assertEquals(0., (addStatesValues[1] - (18.0 + 10.5 * STEP))
                    / addStatesValues[1], EPS);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROPAGATION}
     * 
     * @testedMethod {@link SimpleAdditionalStateProvider#SimpleAdditionalStateProvider(String, AbsoluteDate[], double[][], ISearchIndex)}
     * @testedMethod {@link SimpleAdditionalStateProvider#getAdditionalState(fr.cnes.sirius.patrius.time.AbsoluteDate)}
     * 
     * @description Test the propagation with a simple additional state provider until a date out of
     *              bounds
     *              of the additional state ephemeris used to build the provider.
     * 
     * @input KeplerianPropagator a keplerian propagator to be able to propagate further than the
     *        setup ephemeris
     * @input SimpleAdditionalStateProvider simpleAddStateProv build from an extraction of the
     *        spacecraft states table
     *        computed in setup
     * 
     * @output SpacecraftState sstate : the propagation state at a known date further than the last
     *         input additional
     *         states
     * 
     * @testPassCriteria sstate contains <"TANK", {21.0+duration, 18.0+duration}> additional state
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testAdditionalStateProviderOutOfBound() throws PatriusException {

        // retrieve the additional states and dates from spacecraft states computed in setup
        final int length = tabSpacecraftStates.length;
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        final double[][] tabAdditionalStates = new double[length][];
        final double[] tabIndex = new double[length];
        for (int i = 0; i < length; i++) {
            tabDate[i] = tabSpacecraftStates[i].getDate();
            tabAdditionalStates[i] = tabSpacecraftStates[i].getAdditionalState("TANK");
            tabIndex[i] = tabDate[i].durationFrom(tabDate[0]);
        }

        // build a simpleAdditionalStateProvider from these ones
        final ISearchIndex searchIndex = new BinarySearchIndexClosedOpen(tabIndex);
        final SimpleAdditionalStateProvider simpleAddStateProv = new SimpleAdditionalStateProvider(
                "TANK", tabDate, tabAdditionalStates, searchIndex);
        // check if it has been correctly instantiate
        Assert.assertNotNull(simpleAddStateProv);

        // Create a keplerian propagator
        final KeplerianPropagator kepProp = new KeplerianPropagator(new KeplerianOrbit(7000E3,
                0.001, MathLib.toRadians(70), 0., 0., 0., PositionAngle.MEAN,
                FramesFactory.getEME2000(), interval.getLowerData(), mu));

        // Add the simpleAdditionalState provider
        kepProp.addAdditionalStateProvider(simpleAddStateProv);

        // propagate to a date further than the setup ephemeris last point
        // The additional state shall be "TANK" {21+duration, 18+duration}
        final SpacecraftState sstate = kepProp.propagate(tabSpacecraftStates[length - 1].getDate()
                .shiftedBy(10 * STEP));
        final Map<String, double[]> addStatesResult = sstate.getAdditionalStates();
        for (final String key : addStatesResult.keySet()) {
            Assert.assertTrue(key.equalsIgnoreCase("TANK"));
            final double[] addStatesValues = addStatesResult.get(key);
            Assert.assertEquals(addStatesValues.length, 2);
            Assert.assertEquals(
                    0.,
                    (addStatesValues[0] - (tabSpacecraftStates[length - 1]
                            .getAdditionalState("TANK")[0] + (10 * STEP))) / addStatesValues[0],
                    EPS);
            Assert.assertEquals(
                    0.,
                    (addStatesValues[1] - (tabSpacecraftStates[length - 1]
                            .getAdditionalState("TANK")[1] + (10 * STEP))) / addStatesValues[1],
                    EPS);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROPAGATION}
     * 
     * @testedMethod {@link PVCoordinatesPropagator#propagateOrbit(fr.cnes.sirius.patrius.time.AbsoluteDate)}
     * 
     * @description Instantiate a PVCoordinatePropagator based on EphemerisPvHermite.
     *              Propagate to a chosen date. Interpolate the Hermite ephemeris to the same date.
     *              The PV Coordinates from the spacecraft state return by propagation should be
     *              equals
     *              to PV Coordinates from interpolation.
     * 
     * @input EphemerisPvHermite ephPvHermite instantiate from spacecraft states table computed in
     *        setup
     * @input PVCoordinatePropagator pvCoordPropagator instantiate from ephPvHermite
     * 
     * @output Orbit propagateOrbit : result of orbit propagation
     * @output PVCoordinates interpolatedPV : result of Hermite interpolation
     * 
     * @testPassCriteria Distance between propagateOrbit.getPVCoordinates() and interpolatedPV
     *                   relative to the norm is equal to zero plus or minus double comparison
     *                   precision
     * 
     * @throws PatriusException
     *         should not happens
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testPropagateOrbit() throws PatriusException {

        Report.printMethodHeader("testPropagateOrbit", "Propagation", "Math", EPS,
                ComparisonType.RELATIVE);

        final EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null,
                null);
        final PVCoordinatesPropagator pvCoordPropagator = new PVCoordinatesPropagator(ephPvHermite,
                tabSpacecraftStates[0].getDate(), tabSpacecraftStates[0].getMu(),
                tabSpacecraftStates[0].getFrame());
        final AbsoluteDate targetDate = interval.getLowerData().shiftedBy(500);
        final PVCoordinates pvCoordInter = ephPvHermite.getPVCoordinates(targetDate, null);
        final PVCoordinates pvCoordProp = pvCoordPropagator.propagate(targetDate)
                .getPVCoordinates();
        Assert.assertEquals(pvCoordInter.getPosition().distance(pvCoordProp.getPosition())
                / pvCoordInter.getPosition().getNorm(), 0.0, EPS);
        Assert.assertEquals(pvCoordInter.getVelocity().distance(pvCoordProp.getVelocity())
                / pvCoordInter.getVelocity().getNorm(), 0.0, EPS);

        Report.printToReport("Position", pvCoordInter.getPosition(), pvCoordProp.getPosition());
        Report.printToReport("Velocity", pvCoordInter.getVelocity(), pvCoordProp.getVelocity());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link PVCoordinatesPropagator#PVCoordinatePropagator(org.orekit.utils.PVCoordinatesProvider, fr.cnes.sirius.patrius.time.AbsoluteDate, double, fr.cnes.sirius.patrius.frames.Frame, fr.cnes.sirius.patrius.attitudes.AttitudeProvider, fr.cnes.sirius.patrius.attitudes.AttitudeProvider, java.util.List)}
     * 
     * @description Test the propagation exception
     * 
     * @input EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8,
     *        null);
     * @input PVCoordinatePropagator pvCoordPropag a propagator based on ephPvLagrange
     * 
     * @testPassCriteria raise an orekit exception
     * 
     * @throws PatriusException
     *         as expected
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testPVCoordinateConstructorException() throws PatriusException {

        // build an ephPvLagrange from the spacecraftstates computed in setup
        final EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8,
                null);

        new PVCoordinatesPropagator(ephPvLagrange, tabSpacecraftStates[0].getDate(),
                tabSpacecraftStates[0].getMu(), tabSpacecraftStates[0].getFrame());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROPAGATION}
     * 
     * @testedMethod {@link PVCoordinatesPropagator#propagateOrbit(fr.cnes.sirius.patrius.time.AbsoluteDate)}
     * 
     * @description Test the propagation exception
     * 
     * @input EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8,
     *        null);
     * @input PVCoordinatePropagator pvCoordPropag a propagator based on ephPvLagrange
     * 
     * @testPassCriteria raise an orekit exception
     * 
     * @throws PatriusException
     *         as expected
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testPVCoordinatePropagatorException() throws PatriusException {

        // build an ephPvLagrange from the spacecraftstates computed in setup
        final EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8,
                null);

        // build a propagator
        final PVCoordinatesPropagator pvCoordPropag = new PVCoordinatesPropagator(ephPvLagrange,
                tabSpacecraftStates[4].getDate(), tabSpacecraftStates[4].getMu(),
                tabSpacecraftStates[4].getFrame());
        Assert.assertNotNull(pvCoordPropag);
        pvCoordPropag.propagate(interval.getLowerData().shiftedBy(100));
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the analytical propagator serialization / deserialization process.
     *
     * @testPassCriteria The analytical propagator can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final AbsoluteDate date = interval.getLowerData();
        final Frame frame = FramesFactory.getGCRF();
        final EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null,
                null);

        final PVCoordinatesPropagator pvPropagator = new PVCoordinatesPropagator(ephPvHermite,
                tabSpacecraftStates[0].getDate(), tabSpacecraftStates[0].getMu(),
                tabSpacecraftStates[0].getFrame());
        final PVCoordinatesPropagator deserializedPVPropagator = TestUtils
                .serializeAndRecover(pvPropagator);

        for (int i = 0; i < 10; i++) {
            final AbsoluteDate currentDate = date.shiftedBy(i * 3600.);
            Assert.assertEquals(pvPropagator.getPVCoordinates(currentDate, frame),
                    deserializedPVPropagator.getPVCoordinates(currentDate, frame));
        }
    }
}

class PressureAndTemperatureLinear implements AdditionalEquations {

    /** Equation name. */
    public static final String KEY = "TANK";

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder)
            throws PatriusException {
        // current value of the additional parameters
        final double[] p = s.getAdditionalState(this.getName());
        // derivatives of the additional parameters
        final double[] pDot = new double[p.length];

        for (int i = 0; i < p.length; i++) {
            // additional states linear
            pDot[i] = 1;
        }
        adder.addAdditionalStateDerivative(this.getName(), pDot);
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
        return new double[] { 0. };
    }

    /** {@inheritDoc} */
    @Override
    public int getFirstOrderDimension() {
        // Unused
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getSecondOrderDimension() {
        // Unused
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double[] buildAdditionalState(final double[] y, final double[] yDot) {
        // Unused
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractY(final double[] additionalState) {
        // Unused
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractYDot(final double[] additionalState) {
        // Unused
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        // Unused
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        // Unused
    }
}
