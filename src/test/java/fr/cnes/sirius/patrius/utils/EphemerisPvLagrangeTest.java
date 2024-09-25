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
 * @history 25/09/2015
 *
 * HISTORY
 * VERSION:4.13:FA:FA-140:08/12/2023:[PATRIUS] Imprecision numerique dans EphemerisPvLagrange et EphemerisPvHermite
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:25/09/2015: Creation to test the new EpemerisPvLagrange.
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvLagrange;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class tests EphemerisPvLagrange which is an implementation of PV Coordinates provider based
 * on a lagrange polynome interpolation of a position velocity ephemeris.
 * </p>
 * 
 * @see EphemerisPvLagrange
 * 
 * @author chabaudp
 * 
 * @version $Id: EphemerisPvLagrangeTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 3.1
 * 
 */
public class EphemerisPvLagrangeTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EphemerisPvLagrange constructors
         * 
         * @featureDescription Test the two constructors with and without optional arguments
         * 
         * @coveredRequirements None
         */
        CONSTRUCTOR,

        /**
         * @featureTitle EphemerisPvLagrange interpolation
         * 
         * @featureDescription Tests the lagrange interpolation
         * 
         * @coveredRequirements None
         * 
         */
        INTERPOLATION
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
    /** Another frame. */
    private static Frame anotherFrame;
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
        Utils.setDataRoot("regular-dataCNES-2003");
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
        anotherFrame = FramesFactory.getICRF();
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

        // initialize numerical propagator to produce a Spacecraftstate table with step STEP
        // parameter
        propagator.resetInitialState(new SpacecraftState(initialOrbit, initialAttitude));
        propagator.setSlaveMode();
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
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(SpacecraftState[], int, fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(PVCoordinates[], int, Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvLagrange#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Nominal test of the constructor.
     *              Classical instantiation of a EphemerisPvLagrange with each constructor.
     *              The interpolation result from each one at the same date should be the same.
     *              The interpolation in another frame should be the same than the transformation
     *              from the result.
     * 
     * @input SpacecraftState[],PVCoordinates[],AbsoluteDate[], and Frame, come all from setup
     *        numerical extrapolation
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @output Two EphemerisPvLagrange (one of each constructor).
     * @output Two PVcoordinates (interpolation at the same date from each one).
     * @output A PVCoordinates in anotherframe.
     * 
     * @testPassCriteria class instantiation without exception.
     * @testPassCriteria Distance between position from each interpolation is 0.
     * @testPassCriteria Distance between velocity from each interpolation is 0.
     * @testPassCriteria Distance between position in another frame is 0.
     * @testPassCriteria Distance between velocity in another frame is 0.
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testISearchNullEphemerisPvLagrange() throws PatriusException {

        // Test of spacecraft state constructor nominal execution
        final EphemerisPvLagrange ephPvLagrangeSpacecraftState = new EphemerisPvLagrange(
                tabSpacecraftStates, 8, null);
        Assert.assertNotNull(ephPvLagrangeSpacecraftState);

        // Test of PV and date tables (extracted from spacecraft states table) constructor nominal
        // execution
        final int length = tabSpacecraftStates.length;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }
        final EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabPV, 8,
                tabSpacecraftStates[0].getFrame(), tabDate, null);
        Assert.assertNotNull(ephPvLagrange);

        // Test if the interpolation from each one to the same date (10 min. 30 s.) give the same
        // result
        PVCoordinates spacecraftstatePV = ephPvLagrangeSpacecraftState.getPVCoordinates(interval
                .getLowerData().shiftedBy(10.5 * STEP), null);

        final PVCoordinates pvAndDateTablesPV = ephPvLagrange.getPVCoordinates(interval
                .getLowerData().shiftedBy(10.5 * STEP), null);

        // the distance between each position should be 0
        final double distPos = Vector3D.distance(spacecraftstatePV.getPosition(),
                pvAndDateTablesPV.getPosition());
        Assert.assertEquals(0., distPos, EPS);

        // the distance between each velocity should be 0
        final double distVel = Vector3D.distance(spacecraftstatePV.getVelocity(),
                pvAndDateTablesPV.getVelocity());
        Assert.assertEquals(0., distVel, EPS);

        // test if the frame conversion from ephemerisPvLagrange
        // returns the same result than a frame transformation on the result
        final PVCoordinates spacecraftstatePVICRF = ephPvLagrangeSpacecraftState.getPVCoordinates(
                interval.getLowerData().shiftedBy(10.5 * STEP), anotherFrame);
        final Transform t = frame.getTransformTo(anotherFrame,
                interval.getLowerData().shiftedBy(10.5 * STEP));
        spacecraftstatePV = t.transformPVCoordinates(spacecraftstatePV);
        // the distance between each position should be 0
        final double distPosICRF = Vector3D.distance(spacecraftstatePV.getPosition(),
                spacecraftstatePVICRF.getPosition());
        Assert.assertEquals(0., distPosICRF, EPS);

        // the distance between each velocity should be 0
        final double distVelICRF = Vector3D.distance(spacecraftstatePV.getVelocity(),
                spacecraftstatePVICRF.getVelocity());
        Assert.assertEquals(0., distVelICRF, EPS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(SpacecraftState[], int, fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(PVCoordinates[], int, Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvLagrange#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Nominal test of the constructor.
     *              Instantiation of a EphemerisPvLagrange with each constructor with a user
     *              ISearchIndex not null but
     *              the same than the default one.
     *              Instantiation of a EphemerisPvLagrange with first constructor and ISearchIndex
     *              is null.
     *              The interpolation at the same date from each one should be the same.
     * 
     * @input SpacecraftState[],PVCoordinates[],AbsoluteDate[], and Frame, come all from setup
     *        numerical extrapolation
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input ISearchIndex algo = new BinarySearchIndexClosedOpen(tabIndex) where tabIndex is a
     *        table of duration from
     *        the first Spacecraftstate date
     * 
     * @output Three EphemerisPvLagrange (one of each constructor and one more with ISearchIndex =
     *         null).
     * @output Three PVcoordinates (interpolation at the same date from each one).
     * 
     * @testPassCriteria class instantiation without exception.
     * @testPassCriteria Distance between position from each interpolation is 0.
     * @testPassCriteria Distance between velocity from each interpolation is 0.
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testISearchNotNullEphemerisPvLagrange() throws PatriusException {

        // build ISearch from setup spacecraftstates
        final double[] tabIndex = new double[tabSpacecraftStates.length];
        for (int i = 0; i < tabSpacecraftStates.length; i++) {
            tabIndex[i] = tabSpacecraftStates[i].getDate().durationFrom(
                    tabSpacecraftStates[0].getDate());
        }
        final ISearchIndex searchIndex = new BinarySearchIndexClosedOpen(tabIndex);

        // Test of spacecraft state constructor nominal execution without searchIndex
        final EphemerisPvLagrange ephPvLagrangeSpacecraftState = new EphemerisPvLagrange(
                tabSpacecraftStates, 8, null);
        Assert.assertNotNull(ephPvLagrangeSpacecraftState);

        // Test of spacecraft state constructor nominal execution with searchIndex
        final EphemerisPvLagrange ephPvLagrangeSpacecraftStateSearchI = new EphemerisPvLagrange(
                tabSpacecraftStates, 8, searchIndex);
        Assert.assertNotNull(ephPvLagrangeSpacecraftStateSearchI);

        // Test of PV and date tables (extracted from spacecraft states table) constructor nominal
        // execution
        final int length = tabSpacecraftStates.length;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }
        final EphemerisPvLagrange ephPvLagrangeSearchI = new EphemerisPvLagrange(tabPV, 8,
                tabSpacecraftStates[0].getFrame(), tabDate, searchIndex);
        Assert.assertNotNull(ephPvLagrangeSearchI);

        // Test if the interpolation from each one to the same date (10 min. 30 s.) give the same
        // result
        final PVCoordinates spacecraftstatePV = ephPvLagrangeSpacecraftState.getPVCoordinates(
                interval.getLowerData().shiftedBy(10.5 * STEP), tabSpacecraftStates[0].getFrame());

        final PVCoordinates spacecraftstateSearchIPV = ephPvLagrangeSpacecraftStateSearchI
                .getPVCoordinates(interval.getLowerData().shiftedBy(10.5 * STEP),
                        tabSpacecraftStates[0].getFrame());

        final PVCoordinates pvAndDateTablesSearchIPV = ephPvLagrangeSearchI.getPVCoordinates(
                interval.getLowerData().shiftedBy(10.5 * STEP), tabSpacecraftStates[0].getFrame());

        // the distance between each position should be 0
        final double distPosSpacecraftStates = Vector3D.distance(spacecraftstatePV.getPosition(),
                spacecraftstateSearchIPV.getPosition());
        Assert.assertEquals(0., distPosSpacecraftStates, EPS);

        // the distance between each velocity should be 0
        final double distVelSpacecraftstates = Vector3D.distance(spacecraftstatePV.getVelocity(),
                spacecraftstateSearchIPV.getVelocity());
        Assert.assertEquals(0., distVelSpacecraftstates, EPS);

        // the distance between each position should be 0
        final double distPosPvAndDates = Vector3D.distance(spacecraftstatePV.getPosition(),
                pvAndDateTablesSearchIPV.getPosition());
        Assert.assertEquals(0., distPosPvAndDates, EPS);

        // the distance between each velocity should be 0
        final double distVelPvAndDates = Vector3D.distance(spacecraftstatePV.getVelocity(),
                pvAndDateTablesSearchIPV.getVelocity());
        Assert.assertEquals(0., distVelPvAndDates, EPS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(PVCoordinates[], int, Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on pvcoordinates and date tables
     *              without enough points for this Lagrange polynome order
     * 
     * @input PVCoordinates[] tabPV = table of 6 PV coordinates extracted from spacecraft states
     *        computed in setup
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input Frame frame = frame from the first spacecraft state computed in setup
     * @input AbsoluteDate[] tabDates = dates associated to the tabPV
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @output EphemerisPvLagrange
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEphemerisPvLagrangeDegradedOrder() {
        // Extract the PVCoordinates from the six first spacecraft states
        final int length = 6;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        final SpacecraftState[] tabState = new SpacecraftState[length];
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
            tabState[i] = tabSpacecraftStates[i];
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }

        // Not enough data
        try {
            new EphemerisPvLagrange(tabPV, 8, tabSpacecraftStates[0].getFrame(), tabDate, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            new EphemerisPvLagrange(tabState, 8, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Odd order
        try {
            new EphemerisPvLagrange(tabPV, 5, tabSpacecraftStates[0].getFrame(), tabDate, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            new EphemerisPvLagrange(tabState, 5, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(PVCoordinates[], int, Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on pvcoordinates and date tables
     *              with incoherent dates and coordinates table size
     * 
     * @input PVCoordinates[] tabPV = extracted from spacecraft states table computed in setup
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input Frame frame = extracted from first spacecraft state computed in setup
     * @input AbsoluteDate[] tabDates = extract from the 100 first spacecraftstates
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @output EphemerisPvLagrange
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEphemerisPvLagrangeDegradedSize() {

        // Extract PV from all spacecraft states from the table computed in setup
        int length = tabSpacecraftStates.length;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
        }

        // Extract PV from the first 100 spacecraft states from the table computed in setup
        length = 100;
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }

        new EphemerisPvLagrange(tabPV, 8, tabSpacecraftStates[0].getFrame(), tabDate, null);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(SpacecraftState[], int, fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on spacecraft states table with table
     *              size = 0
     * 
     * @input SpacecraftStates[] tabSpacecraftStates = new SpacecraftStates[0];
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEphemerisPvLagrangeSpacecraftStatesDegradedSize() {

        final SpacecraftState[] tSpacecraftStates = new SpacecraftState[0];

        new EphemerisPvLagrange(tSpacecraftStates, 8, null);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(SpacecraftState[], int, fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on spacecraft states table with table
     *              size = 0
     * 
     * @input SpacecraftStates[] tabSpacecraftStates = new SpacecraftStates[0];
     * @input int order = 9 : illegal order, order shall be even
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEphemerisPvLagrangeEvenOrder() {
        new EphemerisPvLagrange(tabSpacecraftStates, 9, null);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(SpacecraftState[], int, fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvLagrange#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Degraded test of interpolation :
     *              there is not enough point before the interpolated date to compute the Lagrange
     *              interpolation
     * 
     * @input SpacecraftStates[] tabSpacecraftStates computed in setup
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input ISearchIndex algo = null : Classical use of this constructor
     * @input AbsoluteDate interpolationDate = First spacecraft state date + 150 s.
     * @input Frame frame = null : Frame by default
     * 
     * @testPassCriteria OrekitException
     * 
     * @throws PatriusException
     *         as expected
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testGetPVCoordinatesDegradedInterpolationDate() {

        // EphemerisPvLagrange instantiation from spacecraft states table computed in setup
        final EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8, null);

        // Try to interpolate to first date + 150 s.
        try {
            ephPvLagrange.getPVCoordinates(tabSpacecraftStates[0].getDate().shiftedBy(150), null);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected ; FA-114: check the message describes the points values in the error message
            Assert.assertTrue(e.getMessage().contains("7"));
            Assert.assertTrue(e.getMessage().contains("8"));
        }

        // Expected ; FA-114: check the message describes the points values in the error message
        EphemerisPvLagrange ephPvLagrangeBis = new EphemerisPvLagrange(tabSpacecraftStates, 8, null);
        try {
            ephPvLagrangeBis.indexValidity(2);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(e.getMessage().contains("7"));
            Assert.assertTrue(e.getMessage().contains("8"));
        }
        try {
            ephPvLagrangeBis.indexValidity(2876);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(e.getMessage().contains("7"));
            Assert.assertTrue(e.getMessage().contains("8"));
        }

        ephPvLagrangeBis = new EphemerisPvLagrange(tabSpacecraftStates, 2, null);
        try {
            ephPvLagrangeBis.indexValidity(-1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(e.getMessage().contains("1"));
            Assert.assertTrue(e.getMessage().contains("2"));
        }
        try {
            ephPvLagrangeBis.indexValidity(2879);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(e.getMessage().contains("1"));
            Assert.assertTrue(e.getMessage().contains("2"));
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link EphemerisPvLagrange#EphemerisPvLagrange(SpacecraftState[], int, fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvLagrange#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Test of the interpolation with a very long ephemeris (10 years)
     * 
     * @input SpacecraftStates[] tabSpacecraftStates computed in setup
     * @input int order = 8 : Classical value for a lagrange polynome interpolation
     * @input ISearchIndex algo = null : Classical use of this constructor
     * @input AbsoluteDate interpolationDate = First spacecraft state date + 510 s + 1e-8 s.
     * @input Frame frame = null : Frame by default
     * 
     * @output Two EphemerisPvLagrange (one with a short ephemeris and one with a 10 years
     *         ephemeris).
     * @output Three PVcoordinates (interpolation at the same date from each one).
     * 
     * @testPassCriteria class instantiation without exception.
     * @testPassCriteria Distance between position from each interpolation is 0.
     * @testPassCriteria Distance between velocity from each interpolation is 0.
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testEphemerisPvLagrangeNumericalPrecision() throws PatriusException {
        // initial orbit : date = 01/01/2005 00:00:00.000 in TAI, a = 1.5 UA, e = 0.001, i = 40
        // deg, po = 10 deg, go
        // = 15 deg, M = 20 deg in GCRF
        final AbsoluteDate date = new AbsoluteDate(2005, 1, 1, TimeScalesFactory.getTAI());
        final double a = 75e9;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.5, MathLib.toRadians(40),
                MathLib.toRadians(10), MathLib.toRadians(15), MathLib.toRadians(20),
                PositionAngle.MEAN, frame, date, mu);

        final SpacecraftState[] shortTabSpacecraftStates = new SpacecraftState[16];
        final SpacecraftState[] longTabSpacecraftStates = new SpacecraftState[17];

        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        AbsoluteDate currentDate = initialOrbit.getDate();

        // Create a spacecraft state 10 years in the past for the long ephemeris
        longTabSpacecraftStates[0] =
                propagator.getSpacecraftState(initialOrbit.getDate().shiftedBy(-10 * 365 * 86400));

        for (int i = 0; i < shortTabSpacecraftStates.length; i++) {
            final SpacecraftState spacecraftState = propagator.getSpacecraftState(currentDate);
            shortTabSpacecraftStates[i] = spacecraftState;
            longTabSpacecraftStates[i + 1] = spacecraftState;
            currentDate = currentDate.shiftedBy(STEP);
        }

        // EphemerisPvLagrange instantiation from spacecraft states table computed in setup
        final EphemerisPvLagrange shortEphem =
                new EphemerisPvLagrange(shortTabSpacecraftStates, 8, null);
        final EphemerisPvLagrange longEphem =
                new EphemerisPvLagrange(longTabSpacecraftStates, 8, null);

        // Test if the interpolation from each one to the same date give the same result.
        // The date is shifted by 8.5 steps plus a small epsilon,, so that the epsilon is lost if
        // the interpolation in done incorrectly in the long ephemeris.
        final double shift = 8.5 * STEP;
        final double shiftEps = 1e-8;
        final PVCoordinates shortEphemPV =
                shortEphem.getPVCoordinates(date.shiftedBy(shift + shiftEps), null);
        final PVCoordinates longEphemPV =
                longEphem.getPVCoordinates(date.shiftedBy(shift + shiftEps), null);

        // the distance between each position should be 0
        final double distPos =
                Vector3D.distance(shortEphemPV.getPosition(), longEphemPV.getPosition());
        Assert.assertEquals(0., distPos, 0);

        // the distance between each velocity should be 0
        final double distVel =
                Vector3D.distance(shortEphemPV.getVelocity(), longEphemPV.getVelocity());
        Assert.assertEquals(0., distVel, 0);
    }

    /**
     * @throws PatriusException
     *         if date of interpolation is too near from min and max input dates compare to Lagrange
     *         order
     * @description Evaluate the ephemeris serialization / deserialization process.
     *
     * @testPassCriteria The ephemeris can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final double[] tabIndex = new double[tabSpacecraftStates.length];
        for (int i = 0; i < tabSpacecraftStates.length; i++) {
            tabIndex[i] = tabSpacecraftStates[i].getDate().durationFrom(
                    tabSpacecraftStates[0].getDate());
        }
        final ISearchIndex searchIndex = new BinarySearchIndexClosedOpen(tabIndex);

        final EphemerisPvLagrange ephem = new EphemerisPvLagrange(tabSpacecraftStates, 2,
                searchIndex);
        final EphemerisPvLagrange deserializedEphem = TestUtils.serializeAndRecover(ephem);

        for (int i = 0; i < interval.getDuration() / STEP; i++) {
            final AbsoluteDate date = interval.getLowerData().shiftedBy(i * STEP);
            Assert.assertEquals(ephem.getPVCoordinates(date, frame),
                    deserializedEphem.getPVCoordinates(date, frame));
        }

        Assert.assertTrue(Arrays.equals(ephem.getSearchIndex().getTab(), deserializedEphem
                .getSearchIndex().getTab()));
        Assert.assertEquals(ephem.getMinDate(), deserializedEphem.getMinDate());
        Assert.assertEquals(ephem.getMaxDate(), deserializedEphem.getMaxDate());
    }
}
