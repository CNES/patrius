/**
 * 
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
 * 
 * @history 30/09/2015
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:30/09/2015: Creation to test the new EpemerisPvHermite.
 * VERSION::FA:685:16/03/2017:Add the order for Hermite interpolation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
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
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvHermite;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvLagrange;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class tests EphemerisPvHermite which is an implementation of PV Coordinates provider based on a Hermite
 * interpolation of a position velocity ephemeris.
 * </p>
 * 
 * @see EphemerisPvHermite
 * 
 * @author chabaudp
 * 
 * @version $Id: EphemerisPvHermiteTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 3.1
 * 
 */
public class EphemerisPvHermiteTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EphemerisPvHermite constructors
         * 
         * @featureDescription Test the two constructors with and without optional arguments
         * 
         * @coveredRequirements None
         */
        CONSTRUCTOR,

        /**
         * @featureTitle EphemerisPvHermite interpolation
         * 
         * @featureDescription Tests the Hermite interpolation
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
     * Set up : table of spacecraft states given by a propagation performed with a numerical propagator on a LEO orbit
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
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        // Numerical propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        // initial orbit : date = 01/01/2005 00:00:00.000 in TAI, a = 7200000 m, e = 0.001, i = 40 deg, po = 10 deg, go
        // = 15 deg, M = 20 deg in GCRF
        final AbsoluteDate date = new AbsoluteDate(2005, 1, 1, TimeScalesFactory.getTAI());
        frame = FramesFactory.getGCRF();
        anotherFrame = FramesFactory.getICRF();
        mu = Constants.EGM96_EARTH_MU;
        final double a = 7200000;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.001, MathLib.toRadians(40), MathLib.toRadians(10),
            MathLib.toRadians(15), MathLib.toRadians(20), PositionAngle.MEAN, frame, date, mu);

        // constant attitude in local orbital frame
        final AttitudeProvider attProv = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH);
        propagator.setAttitudeProvider(attProv);
        final Attitude initialAttitude = attProv.getAttitude(initialOrbit,
            initialOrbit.getDate(), initialOrbit.getFrame());

        // interval of extrapolation defined by DAYS parameter
        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date, date.shiftedBy(DAYS * 24 * 3600),
            IntervalEndpointType.CLOSED);

        // initialize numerical propagator to produce a Spacecraftstate table with step STEP parameter
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
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(SpacecraftState[], Vector3D[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(PVCoordinates[], Vector3D[], Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvHermite#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Nominal test of the constructor.
     *              Classical instantiation of a EphemerisPvHermite with each constructor.
     *              The interpolation result from each one at the same date should be the same.
     *              The interpolation in another frame should be the same than the transformation from the result.
     * 
     * @input SpacecraftState[],PVCoordinates[],AbsoluteDate[], and Frame, come all from setup numerical extrapolation
     * @input Vector3D[] tabAcc = null : Interpolation with acceleration is tested in another test
     * @input ISearchIndex algo = null : ISearchIndex not null is tested in another test
     * 
     * @output Two EphemerisPvHermite (one of each constructor).
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
    public final void testISearchNullEphemerisPvHermite() throws PatriusException {

        // Test of spacecraft state constructor nominal execution
        final EphemerisPvHermite ephPvHermiteSpacecraftState = new EphemerisPvHermite(tabSpacecraftStates, null, null);
        Assert.assertNotNull(ephPvHermiteSpacecraftState);

        // Test of PV and date tables (extracted from spacecraft states table) constructor nominal execution
        final int length = tabSpacecraftStates.length;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }
        final EphemerisPvHermite ephPvHermite =
            new EphemerisPvHermite(tabPV, null, tabSpacecraftStates[0].getFrame(), tabDate, null);
        Assert.assertNotNull(ephPvHermite);

        // Test if the interpolation from each one to the same date (10 min. 30 s.) give the same result
        PVCoordinates spacecraftstatePV = ephPvHermiteSpacecraftState.getPVCoordinates(
            interval.getLowerData().shiftedBy(10.5 * STEP), null);

        final PVCoordinates pvAndDateTablesPV = ephPvHermite.getPVCoordinates(
            interval.getLowerData().shiftedBy(10.5 * STEP), null);

        // the distance between each position should be 0
        final double distPos = Vector3D.distance(spacecraftstatePV.getPosition(), pvAndDateTablesPV.getPosition());
        Assert.assertEquals(0., distPos, EPS);

        // the distance between each velocity should be 0
        final double distVel = Vector3D.distance(spacecraftstatePV.getVelocity(), pvAndDateTablesPV.getVelocity());
        Assert.assertEquals(0., distVel, EPS);

        // test if the frame conversion from ephemerisPvHermite
        // returns the same result than a frame transformation on the result
        final PVCoordinates spacecraftstatePVICRF = ephPvHermiteSpacecraftState.getPVCoordinates(
            interval.getLowerData().shiftedBy(10.5 * STEP), anotherFrame);
        final Transform t = frame.getTransformTo(anotherFrame, interval.getLowerData().shiftedBy(10.5 * STEP));
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
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(SpacecraftState[], Vector3D[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(PVCoordinates[], Vector3D[], Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvHermite#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Nominal test of the constructor.
     *              Instantiation of a EphemerisPvHermite with each constructor with a user ISearchIndex not null but
     *              the same than the default one.
     *              Instantiation of a EphemerisPvHermite with first constructor and ISearchIndex is null.
     *              The interpolation at the same date from each one should be the same.
     * 
     * @input SpacecraftState[],PVCoordinates[],AbsoluteDate[], and Frame, come all from setup numerical extrapolation
     * @input Vector3D[] tabAcc = null : Interpolation with acceleration is tested in another test
     * @input ISearchIndex algo = new BinarySearchIndexClosedOpen(tabIndex) where tabIndex is a table of duration from
     *        the first Spacecraftstate date
     * 
     * @output Three EphemerisPvHermite (one of each constructor and one more with ISearchIndex = null).
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
    public final void testISearchNotNullEphemerisPvHermite() throws PatriusException {

        // build ISearch from setup spacecraftstates
        final double[] tabIndex = new double[tabSpacecraftStates.length];
        for (int i = 0; i < tabSpacecraftStates.length; i++) {
            tabIndex[i] = tabSpacecraftStates[i].getDate().durationFrom(tabSpacecraftStates[0].getDate());
        }
        final ISearchIndex searchIndex = new BinarySearchIndexClosedOpen(tabIndex);

        // Test of spacecraft state constructor nominal execution without searchIndex
        final EphemerisPvHermite ephPvHermiteSpacecraftState = new EphemerisPvHermite(tabSpacecraftStates, null, null);
        Assert.assertNotNull(ephPvHermiteSpacecraftState);

        // Test of spacecraft state constructor nominal execution with searchIndex
        final EphemerisPvHermite ephPvHermiteSpacecraftStateSearchI =
            new EphemerisPvHermite(tabSpacecraftStates, null, searchIndex);
        Assert.assertNotNull(ephPvHermiteSpacecraftStateSearchI);

        // Test of PV and date tables (extracted from spacecraft states table) constructor nominal execution
        final int length = tabSpacecraftStates.length;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        final AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }
        final EphemerisPvHermite ephPvHermiteSearchI =
            new EphemerisPvHermite(tabPV, null, tabSpacecraftStates[0].getFrame(), tabDate, searchIndex);
        Assert.assertNotNull(ephPvHermiteSearchI);

        // Test if the interpolation from each one to the same date (10 min. 30 s.) give the same result
        final PVCoordinates spacecraftstatePV = ephPvHermiteSpacecraftState.getPVCoordinates(
            interval.getLowerData().shiftedBy(10.5 * STEP),
            tabSpacecraftStates[0].getFrame());

        final PVCoordinates spacecraftstateSearchIPV = ephPvHermiteSpacecraftStateSearchI.getPVCoordinates(
            interval.getLowerData().shiftedBy(10.5 * STEP),
            tabSpacecraftStates[0].getFrame());

        final PVCoordinates pvAndDateTablesSearchIPV = ephPvHermiteSearchI.getPVCoordinates(
            interval.getLowerData().shiftedBy(10.5 * STEP),
            tabSpacecraftStates[0].getFrame());

        // the distance between each position should be 0
        final double distPosSpacecraftStates =
            Vector3D.distance(spacecraftstatePV.getPosition(), spacecraftstateSearchIPV.getPosition());
        Assert.assertEquals(0., distPosSpacecraftStates, EPS);

        // the distance between each velocity should be 0
        final double distVelSpacecraftstates =
            Vector3D.distance(spacecraftstatePV.getVelocity(), spacecraftstateSearchIPV.getVelocity());
        Assert.assertEquals(0., distVelSpacecraftstates, EPS);

        // the distance between each position should be 0
        final double distPosPvAndDates =
            Vector3D.distance(spacecraftstatePV.getPosition(), pvAndDateTablesSearchIPV.getPosition());
        Assert.assertEquals(0., distPosPvAndDates, EPS);

        // the distance between each velocity should be 0
        final double distVelPvAndDates =
            Vector3D.distance(spacecraftstatePV.getVelocity(), pvAndDateTablesSearchIPV.getVelocity());
        Assert.assertEquals(0., distVelPvAndDates, EPS);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(PVCoordinates[], Vector3D[], Frame, AbsoluteDate[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on pvcoordinates and date tables
     *              with incoherent dates and coordinates table size
     * 
     * @input PVCoordinates[] tabPV = extracted from spacecraft states table computed in setup
     * @input Vector3D[] tabAcc = null : Interpolation with acceleration is tested in another test
     * @input Frame frame = extracted from first spacecraft state computed in setup
     * @input AbsoluteDate[] tabDates = extract from the 100 first spacecraftstates
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @output EphemerisPvHermite
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testEphemerisPvHermiteDegradedSize() {

        // Extract PV from all spacecraft states from the table computed in setup
        int length = tabSpacecraftStates.length;
        final PVCoordinates[] tabPV = new PVCoordinates[length];
        AbsoluteDate[] tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabPV[i] = tabSpacecraftStates[i].getPVCoordinates();
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }
        final Vector3D[] tabAcc = new Vector3D[length - 1];
        for (int i = 0; i < length - 1; i++) {
            tabAcc[i] = Vector3D.ZERO;
        }

        // EphemerisPvHermite instantiation from these inputs should raise an exception
        try {
            new EphemerisPvHermite(tabPV, tabAcc, tabSpacecraftStates[0].getFrame(), tabDate, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Extract PV from the first 100 spacecraft states from the table computed in setup
        length = 100;
        tabDate = new AbsoluteDate[length];
        for (int i = 0; i < length; i++) {
            tabDate[i] = tabSpacecraftStates[i].getDate();
        }

        // EphemerisPvHermite instantiation from these inputs should raise an exception
        try {
            new EphemerisPvHermite(tabPV, null, tabSpacecraftStates[0].getFrame(), tabDate, null);
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
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(SpacecraftState[], Vector3D[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on spacecraft states table with table size = 0
     * 
     * @input SpacecraftStates[] tabSpacecraftStates = new SpacecraftStates[0];
     * @input Vector3D[] tabAcc = null : Interpolation with acceleration is tested in another test
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testEphemerisPvHermiteSpacecraftStatesDegradedSize() {

        final SpacecraftState[] tSpacecraftStates = new SpacecraftState[0];

        new EphemerisPvHermite(tSpacecraftStates, null, null);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTOR}
     * 
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(SpacecraftState[], Vector3D[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * 
     * @description Degraded test of the constructor based on spacecraft states table with acceleration table with a
     *              different size
     * 
     * @input SpacecraftStates[] tabSpacecraftStates = new SpacecraftStates[0];
     * @input Vector3D[] tabAcc = Acceleration table with a different size than spacecraft states.
     *        All Vector3D of the table are equals to {1, 1, 1}
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testEphemerisPvHermiteSpacecraftStatesDegradedAccTabSize() {

        // table of acceleration equals to {1, 1, 1} for each point
        final Vector3D[] tabAcc = new Vector3D[200];
        final double[] unitary = { 1.0, 1.0, 1.0 };
        for (int i = 0; i < tabAcc.length; i++) {
            tabAcc[i] = new Vector3D(unitary);
        }

        new EphemerisPvHermite(tabSpacecraftStates, tabAcc, null);
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(SpacecraftState[], Vector3D[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvHermite#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Compare the interpolation results from EphemerisPvHermite
     *              to interpolation results from deprecated HermiteEphemeris class with acceleration at each point
     *              equals to {1, 1, 1}.
     * 
     * @input SpacecraftStates[] tabSpacecraftStates computed in setup
     * @input Vector3D[] tabAcc = Acceleration equal to {1, 1, 1} at each point
     * @input ISearchIndex algo = null : Classical use of this constructor
     * @input AbsoluteDate interpolationDate = Loop on the entire interval with a step of 400 s.
     * @input Frame frame = null : Frame by default
     * 
     * @output table of PVCoordinates from EphemerisPvHermite
     * @output table of PVCoordinates from HermiteEphemeris
     * 
     * @testPassCriteria distance between each position from PVCoordinate from each interpolation
     *                   relative to position norm is equal to 0
     * @testPassCriteria distance between each velocity from PVCoordinate from each interpolation
     *                   relative to velocity norm is equal to 0
     * 
     * @throws PatriusException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testGetPVCoordinatesWithAcc() throws PatriusException {

        // table of acceleration equals to {1, 1, 1} for each point
        final Vector3D[] tabAcc = new Vector3D[tabSpacecraftStates.length];
        final double[] unitary = { 1.0, 1.0, 1.0 };
        for (int i = 0; i < tabAcc.length; i++) {
            tabAcc[i] = new Vector3D(unitary);
        }

        // EphemerisPvHermite instantiation from spacecraft states table computed in setup
        final EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, tabAcc, null);
        // HermiteEphemeris instantiation from spacecraft states table computed in setup
        final EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8, null);

        // loop to interpolate each instantiation and compare results with a step of 400 s
        final int step = 400;
        PVCoordinates pvEphPvHermite;
        PVCoordinates pvEphPvLagrange;
        AbsoluteDate target;
        for (double duration = step; duration <= interval.getDuration() - step; duration = duration + step) {
            target = tabSpacecraftStates[0].getDate().shiftedBy(duration);
            pvEphPvHermite = ephPvHermite.getPVCoordinates(target, null);
            pvEphPvLagrange = ephPvLagrange.getPVCoordinates(target, null);

            // the distance between each position should be 0
            final double distPos = Vector3D.distance(pvEphPvHermite.getPosition(), pvEphPvLagrange.getPosition());
            Assert.assertEquals(0., distPos / pvEphPvHermite.getPosition().getNorm(), 1E-3);

            // the distance between each velocity should be 0
            final double distVel = Vector3D.distance(pvEphPvHermite.getVelocity(), pvEphPvLagrange.getVelocity());
            Assert.assertEquals(0., distVel / pvEphPvHermite.getVelocity().getNorm(), 1E-2);
        }
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link EphemerisPvHermite#EphemerisPvHermite(SpacecraftState[], Vector3D[], fr.cnes.sirius.patrius.math.utils.ISearchIndex)}
     * @testedMethod {@link EphemerisPvHermite#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Compare the interpolation results from EphemerisPvHermite
     *              to interpolation results from deprecated HermiteEphemeris class
     * 
     * @input SpacecraftStates[] tabSpacecraftStates computed in setup
     * @input Vector3D[] tabAcc = null : Interpolation with acceleration is tested in another test
     * @input ISearchIndex algo = null : Classical use of this constructor
     * @input AbsoluteDate interpolationDate = Loop on the entire interval with a step of 400 s.
     * @input Frame frame = null : Frame by default
     * 
     * @output table of PVCoordinates from EphemerisPvHermite
     * @output table of PVCoordinates from HermiteEphemeris
     * 
     * @testPassCriteria distance between each position from PVCoordinate from each interpolation
     *                   relative to position norm is equal to 0
     * @testPassCriteria distance between each velocity from PVCoordinate from each interpolation
     *                   relative to velocity norm is equal to 0
     * 
     * @throws PatriusException
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testGetPVCoordinates() throws PatriusException {

        // EphemerisPvHermite instantiation from spacecraft states table computed in setup
        final EphemerisPvHermite ephPvHermite = new EphemerisPvHermite(tabSpacecraftStates, null, null);
        // HermiteEphemeris instantiation from spacecraft states table computed in setup
        final EphemerisPvLagrange ephPvLagrange = new EphemerisPvLagrange(tabSpacecraftStates, 8, null);

        // loop to interpolate each instantiation and compare results with a step of 400 s
        final int step = 400;
        PVCoordinates pvEphPvHermite;
        PVCoordinates pvEphPvLagrange;
        AbsoluteDate target;
        for (double duration = step; duration <= interval.getDuration() - step; duration = duration + step) {
            target = tabSpacecraftStates[0].getDate().shiftedBy(duration);
            pvEphPvHermite = ephPvHermite.getPVCoordinates(target, null);
            pvEphPvLagrange = ephPvLagrange.getPVCoordinates(target, null);

            // the distance between each position should be 0
            final double distPos = Vector3D.distance(pvEphPvHermite.getPosition(), pvEphPvLagrange.getPosition());
            Assert.assertEquals(0., distPos / pvEphPvHermite.getPosition().getNorm(), 1E-7);

            // the distance between each velocity should be 0
            final double distVel = Vector3D.distance(pvEphPvHermite.getVelocity(), pvEphPvLagrange.getVelocity());
            Assert.assertEquals(0., distVel / pvEphPvHermite.getVelocity().getNorm(), 1E-5);

        }

        // Test date outside intervaal
        try {
            ephPvHermite.getPVCoordinates(tabSpacecraftStates[0].getDate().shiftedBy(-100), null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link EphemerisPvHermite#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description This is a mathematical test : using a function for x position of a satellite
     *              given by sin(x) + sin(2x) + sin(3x), we show an Hermite interpolation lead to more precision when
     *              a higher order is chosen (8 points so order = 8) than the default order (2 points i.e order = 2).
     *              Two cases are considered :
     *              1- accelerations are not provided, interpolators lay only on PV information (spanned 1s)
     *              Interpolation values are computed on a chosen time span with 0.1 step.
     *              2- accelerations are provided but not well computed (add a constant bias).
     * 
     * @input PVCoordinates[] : f(x), f'(x) with 1s step
     * @input Vector3D[] tabAcc = null or provided with bias
     * @input ISearchIndex algo = null : Classical use of this constructor
     * 
     * @output table of PVCoordinates from EphemerisPvHermite
     * 
     * @testPassCriteria Order 8 interpolation should be more precise than default order 2 in both cases.
     * 
     * @throws PatriusException
     * @throws FileNotFoundException
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testInterpolationMathCase() throws PatriusException, FileNotFoundException {

        // Interpolation duration (20 s)
        final int duration = 20;

        // Interpolation order = 8
        final int order = 8;

        // Bias on computed accelerations (constant)
        final double bias = 2.;

        // Mathematical case : function sin(x) + sin(2x) + sin(3x)
        final PVCoordinates[] pv = new PVCoordinates[duration];
        final Vector3D[] acc = new Vector3D[duration];

        for (int i = 0; i < duration; i++) {
            pv[i] = new PVCoordinates(new Vector3D(MathLib.sin(i) + MathLib.sin(2. * i) + MathLib.sin(3. * i), 0.,
                0.),
                new Vector3D(MathLib.cos(i) + 2. * MathLib.cos(2. * i) + 3. * MathLib.cos(3. * i), 0., 0.));
            // Acceleration for second case
            acc[i] = new Vector3D(-MathLib.sin(i) - 4. * MathLib.sin(2. * i) - 9. * MathLib.sin(3. * i) + bias, 0.,
                0.);
        }

        // Dates
        final AbsoluteDate refDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate[] dates = new AbsoluteDate[duration];
        for (int i = 0; i < duration; i++) {
            dates[i] = refDate.shiftedBy(i);
        }

        /**
         * First case : provide no acceleration.
         */

        // EphemerisPvHermite instantiation : order 8 vs default order 2
        final EphemerisPvHermite pvHermite = new EphemerisPvHermite(pv, order, null, frame, dates, null);
        final EphemerisPvHermite pvHermite2 = new EphemerisPvHermite(pv, null, frame, dates, null);

        // Interpolate on subset t = [4s, 10s]
        AbsoluteDate target;
        double interpValue2;
        double interpValue8;
        double realValue;
        final double eps = 1.0e-13;
        for (double i = 4.; i < 10.; i = i + 0.1) {

            // Interpolation date
            target = refDate.shiftedBy(i);

            // Compute function real value
            realValue = MathLib.sin(i) + MathLib.sin(2. * i) + MathLib.sin(3. * i);

            // Compute interpolated value by Hermite order 2, order 8
            interpValue2 = pvHermite2.getPVCoordinates(target, frame).getPosition().getX();
            interpValue8 = pvHermite.getPVCoordinates(target, frame).getPosition().getX();

            if (Precision.equals(i, 4., eps) || Precision.equals(i, 5., eps) || Precision.equals(i, 6., eps)
                || Precision.equals(i, 7., eps)
                || Precision.equals(i, 8., eps) || Precision.equals(i, 9., eps) || Precision.equals(i, 10., eps)) {
                // Good precision for both interpolators around interpolation points
                Assert.assertEquals(realValue, interpValue2, eps);
                Assert.assertEquals(realValue, interpValue8, eps);
            } else {
                // Order 8 interpolator is more precise between interpolation points
                Assert.assertTrue(MathLib.abs(realValue - interpValue8) < MathLib.abs(realValue - interpValue2));
            }
        }

        /**
         * Second case : provide acceleration computed with given epsilon bias (constant).
         */

        // EphemerisPvHermite instantiation : order 8 vs default order 2
        final EphemerisPvHermite pvHermiteBias = new EphemerisPvHermite(pv, order, acc, frame, dates, null);
        final EphemerisPvHermite pvHermite2Bias = new EphemerisPvHermite(pv, acc, frame, dates, null);

        for (double i = 4.; i < 10.; i = i + 0.1) {

            // Interpolation date
            target = refDate.shiftedBy(i);

            // Compute function real value
            realValue = MathLib.sin(i) + MathLib.sin(2. * i) + MathLib.sin(3. * i);

            // Compute interpolated value by Hermite order 2, order 8
            interpValue2 = pvHermiteBias.getPVCoordinates(target, frame).getPosition().getX();
            interpValue8 = pvHermite2Bias.getPVCoordinates(target, frame).getPosition().getX();

            // Order 8 interpolator is more precise than order 2 for each time t
            Assert.assertTrue(MathLib.abs(realValue - interpValue8) < MathLib.abs(realValue - interpValue2));
        }
    }
}
