/**
 *
 * Copyright 2011-2022 CNES
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
 * @history creation 04/01/2012
  * VERSION::DM:403:20/10/2015:Improving ergonomics
  * VERSION::DM:489:20/11/2015:coverage
  * VERSION::FA:565:03/03/2016:corrections on attitude requirements
  * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2847:18/05/2021:Modification de la gestion de la date hors intervalle
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.4:DM:DM-2209:04/10/2019:[PATRIUS] Amelioration de AttitudeLawLeg
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the AttitudeLawLeg class.
 *              </p>
 *
 * @author Thomas Trapier
 *
 * @version $Id: AttitudeLegTest.java 17910 2017-09-11 11:58:16Z bignon $
 *
 * @since 1.1
 */
public class AttitudeLawLegTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Attitude law leg
         *
         * @featureDescription object describing an attitude law in a
         *                     time interval
         *
         * @coveredRequirements DV-ATT_30, DV-ATT_40, DV-ATT_50
         */
        ATTITUDE_LAW_LEG
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LAW_LEG}
     *
     * @testedMethod {@link AttitudeLawLeg#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link AttitudeLawLeg#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     *
     * @description Instantiation of an attitude law leg and getting of its attitude at a date in a frame.
     *
     * @input the output date and frame
     *
     * @output Attitude
     *
     * @testPassCriteria the output attitude is the one used to create the
     *                   basic attitude provider object : the spin is tested. The data must be equal,
     *                   and no complex computation is involved : the epsilon is the double comparison epsilon.
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetAttitude() throws PatriusException {

        // PV creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        final BasicPVCoordinatesProvider pvProvider = new BasicPVCoordinatesProvider(originPV, frame);

        // attitude creation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D rotAxis = new Vector3D(8.687, 5.6721, -7.54);
        final Rotation rotation = new Rotation(rotAxis, 1.687);
        final Vector3D spin = new Vector3D(0.6875423, 5.684231, 6.685745);
        final Attitude inAtt = new Attitude(date, frame, rotation, spin);

        // attitude law creation
        final BasicAttitudeProvider attLaw = new BasicAttitudeProvider(inAtt);

        // time interval
        final AbsoluteDate endDate = date.shiftedBy(50.0);

        // AttitudeLawLeg creation
        final AttitudeLawLeg lawLeg = new AttitudeLawLeg(attLaw, date.shiftedBy(-1.0), endDate.shiftedBy(1.0));
        // Not used, juste for coverage
        lawLeg.setSpinDerivativesComputation(true);

        // test 1 : with date and frame
        try {
            final Attitude outAtt = lawLeg.getAttitude(pvProvider, endDate, frame);

            final Vector3D outSpin = outAtt.getSpin();

            Assert.assertEquals(spin.getX(), outSpin.getX(), this.comparisonEpsilon);
            Assert.assertEquals(spin.getY(), outSpin.getY(), this.comparisonEpsilon);
            Assert.assertEquals(spin.getZ(), outSpin.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test 2: date outside the interval of validity of the attitude law:
        try {
            final AbsoluteDate outsideDate = date.shiftedBy(100.0);
            lawLeg.getAttitude(pvProvider, outsideDate, frame);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Check date outside interval of validity when time tolerant
        final AttitudeLawLeg lawLeg2 = new AttitudeLawLeg(attLaw, date.shiftedBy(-1.0), endDate.shiftedBy(1.0), "", true);
        Assert.assertEquals(0., Rotation.distance(lawLeg2.getAttitude(pvProvider, endDate, frame).getRotation(), attLaw.getAttitude(pvProvider, endDate, frame).getRotation()), 0.);

    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link AttitudeLawLeg#copy(AbsoluteDateInterval)}
     *
     * @description Test the new method
     *
     * @input parameters
     *
     * @output AbsoluteDateInterval
     *
     * @testPassCriteria The method behavior is correct
     *
     * @referenceVersion 4.4
     *
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testCopyMethod() throws PatriusException {

        // Constructor dates
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(50.);

        // Attitude creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D rotAxis = new Vector3D(8.687, 5.6721, -7.54);
        final Rotation rotation = new Rotation(rotAxis, 1.687);
        final Vector3D spin = new Vector3D(0.6875423, 5.684231, 6.685745);
        final Attitude inAtt = new Attitude(startDate, frame, rotation, spin);
        final BasicAttitudeProvider attLaw = new BasicAttitudeProvider(inAtt);

        // Intervals creation
        final double offset = 5;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityOpen = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.OPEN);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // AttitudeLawLeg creation
        AttitudeLawLeg lawLeg1 = new AttitudeLawLeg(attLaw, startDate, endDate);
        lawLeg1.setSpinDerivativesComputation(true);
        AttitudeLawLeg lawLeg2 = new AttitudeLawLeg(attLaw, startDate, endDate);
        AttitudeLawLeg lawLeg3 = new AttitudeLawLeg(attLaw, startDate, endDate);

        final Attitude attitudeRef = lawLeg1.getAttitude(null, startDate.shiftedBy(5), frame);

        // Test case n°1 : in a standard usage, the interval stored should be updated
        final AttitudeLawLeg tmp = lawLeg1.copy(newIntervalOfValidity);
        lawLeg1 = lawLeg1.copy(newIntervalOfValidity);
        Assert.assertTrue(lawLeg1.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = lawLeg1.getAttitude(null, startDate.shiftedBy(5), frame);
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(0, attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), 0);

        // Test case n°2 : if we send an opened interval, it's stored as it
        lawLeg2 = lawLeg2.copy(newIntervalOfValidityOpen);
        Assert.assertTrue(lawLeg2.getTimeInterval().equals(newIntervalOfValidityOpen));

        // Test case n°3 : when the new interval isn't included, the interval stored should simply be updated
        lawLeg3 = lawLeg3.copy(newIntervalOfValidityNotIncluded);
        Assert.assertTrue(lawLeg3.getTimeInterval().equals(newIntervalOfValidityNotIncluded));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LAW_LEG}
     *
     * @testedMethod {@link AttitudeLawLeg#getAttitude(PVCoordinatesProvider)}
     *
     * @description Instantiation of an attitude law leg and getting of its attitude at a date in a frame.
     *
     * @input the output date and frame
     *
     * @output Attitude
     *
     * @testPassCriteria the output attitude is the one used to create the
     *                   basic attitude provider object : the spin is tested. The data must be equal,
     *                   and no complex computation is involved : the epsilon is the double comparison epsilon.
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetAttitudeOrbit() throws PatriusException {

        // attitude creation
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D rotAxis = new Vector3D(8.687, 5.6721, -7.54);
        final Rotation rotation = new Rotation(rotAxis, 1.687);
        final Vector3D spin = new Vector3D(0.6875423, 5.684231, 6.685745);
        final Attitude inAtt = new Attitude(date, frame, rotation, spin);

        // attitude law creation
        final BasicAttitudeProvider attLaw = new BasicAttitudeProvider(inAtt);

        // time interval
        final AbsoluteDate endDate = date.shiftedBy(50.0);
        new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date, endDate, IntervalEndpointType.CLOSED);

        // AttitudeLawLeg creation
        final AttitudeLawLeg lawLeg = new AttitudeLawLeg(attLaw, date, endDate);

        // creation of the orbit:

        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, FramesFactory.getGCRF(), date, Constants.EIGEN5C_EARTH_MU);

        // getting test 1
        final Attitude att = lawLeg.getAttitude(orbit);
        final Attitude att2 = lawLeg.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        Assert.assertEquals(att.getDate(), att2.getDate());
        Assert.assertEquals(att.getReferenceFrame(), att2.getReferenceFrame());
        Assert.assertTrue(att.getRotation().isEqualTo(att2.getRotation()));
        Assert.assertEquals(0, att.getSpin().getNorm(), att2.getSpin().getNorm());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LAW_LEG}
     *
     * @testedMethod {@link AttitudeLawLeg#getTimeInterval()}
     *
     * @description Instantiation of an attitude law leg and getting of its attitude at a date in a frame.
     *
     * @input the output date and frame
     *
     * @output Attitude
     *
     * @testPassCriteria the output time interval is the one setted to create the
     *                   object : right initial and end date, right end point types. The data must be equal,
     *                   and no complex computation is involved : the epsilon is the double comparison epsilon.
     *
     * @referenceVersion 4.4
     *
     * @nonRegressionVersion 4.4
     */
    @Test
    public final void testTimeInterval() {

        final String defaultNature = "ATTITUDE_LAW_LEG";
        final String customizedNature = "CUST_ATTITUDE_LAW_LEG";

        // attitude creation
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D rotAxis = new Vector3D(8.687, 5.6721, -7.54);
        final Rotation rotation = new Rotation(rotAxis, 1.687);
        final Vector3D spin = new Vector3D(0.6875423, 5.684231, 6.685745);
        final Attitude inAtt = new Attitude(date, frame, rotation, spin);

        // attitude law creation
        final BasicAttitudeProvider attLaw = new BasicAttitudeProvider(inAtt);

        // time interval
        final AbsoluteDate endDate = date.shiftedBy(50.0);
        final AbsoluteDateInterval timeInterval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date, endDate, IntervalEndpointType.CLOSED);

        AttitudeLawLeg lawLeg;
        AbsoluteDateInterval dateInterval;

        //Test constructor 1
        lawLeg = new AttitudeLawLeg(attLaw, date, endDate);
        Assert.assertTrue(lawLeg.getNature().equals(defaultNature));
        Assert.assertTrue(lawLeg.getTimeInterval().includes(timeInterval));
        Assert.assertTrue(timeInterval.includes(lawLeg.getTimeInterval()));

        //Test constructor 2
        lawLeg = new AttitudeLawLeg(attLaw, date, endDate, customizedNature);
        Assert.assertTrue(lawLeg.getNature().equals(customizedNature));
        Assert.assertTrue(lawLeg.getTimeInterval().includes(timeInterval));
        Assert.assertTrue(timeInterval.includes(lawLeg.getTimeInterval()));

        final AbsoluteDateInterval timeIntervalPastOpen = new AbsoluteDateInterval(
            IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY, endDate, IntervalEndpointType.CLOSED);
        lawLeg = new AttitudeLawLeg(attLaw, AbsoluteDate.PAST_INFINITY, endDate, customizedNature);
        Assert.assertTrue(lawLeg.getTimeInterval().equals(timeIntervalPastOpen));

        final AbsoluteDateInterval timeIntervalFutureOpen = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date, AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);
        lawLeg = new AttitudeLawLeg(attLaw, date, AbsoluteDate.FUTURE_INFINITY, customizedNature);
        Assert.assertTrue(lawLeg.getTimeInterval().equals(timeIntervalFutureOpen));

        final AbsoluteDateInterval timeIntervalOpen = new AbsoluteDateInterval(
            IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);
        lawLeg = new AttitudeLawLeg(attLaw, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY, customizedNature);
        Assert.assertTrue(lawLeg.getTimeInterval().equals(timeIntervalOpen));

        //Test constructor 3
        dateInterval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date, endDate, IntervalEndpointType.CLOSED);
        lawLeg = new AttitudeLawLeg(attLaw, dateInterval);
        Assert.assertTrue(lawLeg.getNature().equals(defaultNature));
        Assert.assertTrue(lawLeg.getTimeInterval().includes(timeInterval));
        Assert.assertTrue(timeInterval.includes(lawLeg.getTimeInterval()));

        //Test constructor 4
        dateInterval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date, endDate, IntervalEndpointType.CLOSED);
        lawLeg = new AttitudeLawLeg(attLaw, dateInterval, customizedNature);
        Assert.assertTrue(lawLeg.getNature().equals(customizedNature));
        Assert.assertTrue(lawLeg.getTimeInterval().includes(timeInterval));
        Assert.assertTrue(timeInterval.includes(lawLeg.getTimeInterval()));

        //Test constructor 4 with open input interval -> should stay like this
        dateInterval = new AbsoluteDateInterval(
            IntervalEndpointType.OPEN, date, endDate, IntervalEndpointType.OPEN);
        lawLeg = new AttitudeLawLeg(attLaw, dateInterval, customizedNature);
        Assert.assertTrue(lawLeg.getNature().equals(customizedNature));
        Assert.assertTrue(lawLeg.getTimeInterval().includes(dateInterval));
        Assert.assertTrue(dateInterval.includes(lawLeg.getTimeInterval()));
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LAW_LEG}
     *
     * @testedMethod {@link AttitudeLawLeg#getAttitudeLaw()}
     *
     * @description Instantiation of an attitude law leg and getting of its attitude provider law.
     *
     * @input an interval and an attitude provider law
     *
     * @output AttitudeProvider
     *
     * @testPassCriteria the attitude provider law should be the one given at the construction.
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetAttitudeProvider() {

        // attitude creation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D rotAxis = new Vector3D(8.687, 5.6721, -7.54);
        final Rotation rotation = new Rotation(rotAxis, 1.687);
        final Vector3D spin = new Vector3D(0.6875423, 5.684231, 6.685745);
        final Attitude inAtt = new Attitude(date, FramesFactory.getGCRF(), rotation, spin);

        // attitude law creation
        final BasicAttitudeProvider attLaw = new BasicAttitudeProvider(inAtt);

        // time interval
        final AbsoluteDate endDate = date.shiftedBy(50.0);
        // AttitudeLawLeg creation
        final AttitudeLawLeg lawLeg = new AttitudeLawLeg(attLaw, date, endDate);

        Assert.assertTrue(lawLeg.getAttitudeLaw() == attLaw);
    }
}
