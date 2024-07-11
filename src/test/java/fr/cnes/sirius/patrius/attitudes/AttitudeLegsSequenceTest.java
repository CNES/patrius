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
 * HISTORY
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:FA:FA-2337:27/05/2020:Methode truncate() de AbstractLegsSequence 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:96:05/08/2013:updated to match the attitude legs sequence with codes
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.legs.Leg;

/**
 * <p>
 * Tests for the AttitudeLegsSequence class.
 * </p>
 *
 * @author Tiziana Sabatini
 * @author Pierre Cardoso
 *
 * @version $Id: AttitudeLegsSequenceTest.java 17910 2017-09-11 11:58:16Z bignon $
 *
 * @since 1.1
 *
 */
public class AttitudeLegsSequenceTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Attitude laws sequence validation
         *
         * @featureDescription Attitude laws sequence validation
         *
         * @coveredRequirements DV-ATT_70, DV-ATT_90
         */
        ATTITUDE_LEGS_SEQUENCE_VALIDATION
    }

    /** */
    private AbsoluteDate date1i;

    /** */
    private AbsoluteDate date1f;

    /** */
    private AbsoluteDate date2i;

    /** */
    private AbsoluteDate date2f;

    /** */
    private AbsoluteDate date3i;

    /** */
    private AbsoluteDate date3f;

    /** */
    private AbsoluteDate date4i;

    /** */
    private AbsoluteDate date4f;

    /** */
    private AbsoluteDate date5i;

    /** */
    private AbsoluteDate date5f;

    /** */
    private AbsoluteDate date6i;

    /** */
    private AbsoluteDate date6f;

    /** First attitude law */
    private AttitudeLeg law1;

    /** Second attitude law */
    private AttitudeLeg law2;

    /** Third attitude law */
    private AttitudeLeg law3;

    /** Fourth attitude law */
    private AttitudeLeg law4;

    /** Fifth attitude law */
    private AttitudeLeg law5;

    /** Sixth attitude law */
    private AttitudeLeg law6;

    /** Attitude laws sequence */
    private StrictAttitudeLegsSequence<AttitudeLeg> sequence;

    /** Earth. */
    private CelestialBody moon;

    /** Reference frame. */
    private Frame itrf;

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LEGS_SEQUENCE_VALIDATION}
     *
     * @testedMethod {@link StrictAttitudeLegsSequence#AttitudeLegsSequence(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider)}
     * @testedMethod {@link StrictAttitudeLegsSequence#add(AttitudeLeg)}
     * @testedMethod {@link StrictAttitudeLegsSequence#isEmpty()}
     *
     * @description Adds three consecutive attitude laws and checks that the sequence is not empty.
     *
     * @input constructor parameters and AttitudeLeg instances
     *
     * @output the {@link StrictAttitudeLegsSequence} instance
     *
     * @testPassCriteria contents of the {@link StrictAttitudeLegsSequence} as expected (3 laws)
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     *
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testAddAttitude() throws PatriusException {
        // creation of the attitude laws sequence:
        this.sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();

        // No laws:
        // Check the sequence is empty:
        Assert.assertTrue(this.sequence.isEmpty());

        // One law:
        this.sequence.add(this.law1);
        // Check the sequence is not empty:
        Assert.assertFalse(this.sequence.isEmpty());
        // Check there is 1 law in the sequence:
        Assert.assertEquals(1, this.sequence.size());
        Assert.assertEquals(this.date1i, this.sequence.first().getTimeInterval().getLowerData());
        Assert.assertEquals(this.date1f, this.sequence.last().getTimeInterval().getUpperData());

        // Two laws:
        this.sequence.add(this.law2);
        // check there are 2 laws in the sequence:
        Assert.assertEquals(2, this.sequence.size());
        Assert.assertEquals(this.date1i, this.sequence.first().getTimeInterval().getLowerData());
        Assert.assertEquals(this.date2f, this.sequence.last().getTimeInterval().getUpperData());

        // Three laws:
        this.sequence.add(this.law3);
        // check there are 3 laws in the sequence:
        Assert.assertEquals(3, this.sequence.size());
        Assert.assertEquals(this.date1i, this.sequence.first().getTimeInterval().getLowerData());
        Assert.assertEquals(this.date3f, this.sequence.last().getTimeInterval().getUpperData());

        // Four laws:
        this.sequence.add(this.law5);
        // check there are 4 laws in the sequence:
        Assert.assertEquals(4, this.sequence.size());
        Assert.assertEquals(this.date5i, this.sequence.first().getTimeInterval().getLowerData());
        Assert.assertEquals(this.date3f, this.sequence.last().getTimeInterval().getUpperData());

        // This method should not be called (see javadoc), it is called for coverage purpose
        this.sequence.setSpinDerivativesComputation(true);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LEGS_SEQUENCE_VALIDATION}
     *
     * @testedMethod {@link StrictAttitudeLegsSequence#add(AttitudeLeg)}
     *
     * @description Checks errors on calls to add().
     *
     * @input constructor parameters and AttitudeLeg instances
     *
     * @output the {@link StrictAttitudeLegsSequence} instance
     *
     * @testPassCriteria IllegalArgumentException on call to add()
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     *
     */
    @Test
    public final void testAddErrors() throws PatriusException {
        // creation of the attitude laws sequence:
        this.sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();

        // Add laws
        Assert.assertTrue(this.sequence.add(this.law1));
        Assert.assertTrue(this.sequence.add(this.law2));
        Assert.assertTrue(this.sequence.add(this.law3));

        // The 6th law is not contigous with the 1st law, should be normally added
        Assert.assertTrue(this.sequence.add(this.law6));
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LEGS_SEQUENCE_VALIDATION}
     *
     * @testedMethod {@link StrictAttitudeLegsSequence#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link StrictAttitudeLegsSequence#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     *
     * @description Adds three consecutive attitude laws and calls getAttitude on each.
     *
     * @input constructor parameters and AttitudeLeg instances
     *
     * @output the {@link StrictAttitudeLegsSequence} instance
     *
     * @testPassCriteria results of the {@link StrictAttitudeLegsSequence} getAttitude calls as
     *                   expected (3 laws)
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     *
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetAttitude() throws PatriusException {
        // creation of the attitude laws sequence:
        this.sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();

        // Add the laws :
        this.sequence.add(this.law1);
        this.sequence.add(this.law2);
        this.sequence.add(this.law3);

        // Call getAttitude on the laws AND on the sequence,
        // results should be the same.
        final Attitude expected1_1 = this.law1.getAttitude(this.moon, this.date1i, this.itrf);
        final Attitude rez1_1 = this.sequence.getAttitude(this.moon, this.date1i, this.itrf);
        compareAttitudes(expected1_1, rez1_1);
        final Attitude expected1_2 = this.law2.getAttitude(this.moon, this.date1f, this.itrf);
        final Attitude rez1_2 = this.sequence.getAttitude(this.moon, this.date1f, this.itrf);
        compareAttitudes(expected1_2, rez1_2);

        final AbsoluteDate beforeDate2f = this.date2f.shiftedBy(-1.);
        final Attitude expected2_1 = this.law2.getAttitude(this.moon, beforeDate2f, this.itrf);
        // For coverage, other getAttitude method
        final Attitude rez2_1 = this.sequence.getAttitude(this.moon, beforeDate2f, this.itrf);
        compareAttitudes(expected2_1, rez2_1);

        final AbsoluteDate afterDate3i = this.date3i.shiftedBy(1.);
        final Attitude expected3_1 = this.law3.getAttitude(this.moon, afterDate3i, this.itrf);
        // Shows the PvCoordinates parameter is ignored
        final Attitude rez3_1 = this.sequence.getAttitude(CelestialBodyFactory.getEarth(),
                afterDate3i, this.itrf);
        compareAttitudes(expected3_1, rez3_1);

    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LEGS_SEQUENCE_VALIDATION}
     *
     * @testedMethod {@link StrictAttitudeLegsSequence#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider)}
     *
     * @description Adds three consecutive attitude laws and calls getAttitude on each.
     *
     * @input constructor parameters and AttitudeLeg instances
     *
     * @output the {@link StrictAttitudeLegsSequence} instance
     *
     * @testPassCriteria results of the {@link StrictAttitudeLegsSequence} getAttitude calls as
     *                   expected (3 laws)
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     *
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetAttitudeOrbit() throws PatriusException {
        // creation of the orbit:
        final AbsoluteDate t0 = new AbsoluteDate(2008, 04, 07, 12, 15, 10.2,
                TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214,
                MathLib.toRadians(98.28647), MathLib.toRadians(105.332064),
                MathLib.toRadians(108.315415), MathLib.toRadians(89.786767), PositionAngle.MEAN,
                FramesFactory.getGCRF(), t0, Constants.EIGEN5C_EARTH_MU);

        this.sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();
        // Add the laws :
        this.sequence.add(this.law1);
        // Call getAttitude on the laws AND on the sequence,
        // results should be the same.
        final Attitude att = this.sequence.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        final Attitude att2 = this.sequence.getAttitude(orbit);
        compareAttitudes(att, att2);

    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LEGS_SEQUENCE_VALIDATION}
     *
     * @testedMethod {@link StrictAttitudeLegsSequence#head(AbsoluteDate, boolean), @link
     *               StrictAttitudeLegsSequence#tail(AbsoluteDate, boolean), @link
     *               StrictAttitudeLegsSequence#copy(AbsoluteDateInterval, boolean) and @link
     *               StrictAttitudeLegsSequence#sub(AbsoluteDateInterval, boolean)}
     *
     * @description test 3 previous methods methods for different sequences (empty, standard, open
     *              boundaries, holes, infinite boundaries)
     *
     * @output output of methods
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testHeadTailSubCopy() throws PatriusException {

        // Init sequence
        StrictAttitudeLegsSequence<AttitudeLeg> sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();
        final BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(this.itrf);

        // ============================================================
        // ================ Tests on an empty sequence ================
        // ============================================================
        Assert.assertTrue(sequence.head(AbsoluteDate.J2000_EPOCH).isEmpty());
        Assert.assertTrue(sequence.head(AbsoluteDate.J2000_EPOCH, true).isEmpty());
        Assert.assertTrue(sequence.tail(AbsoluteDate.J2000_EPOCH).isEmpty());
        Assert.assertTrue(sequence.tail(AbsoluteDate.J2000_EPOCH, true).isEmpty());
        Assert.assertTrue(sequence.sub(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH)
                .isEmpty());
        Assert.assertTrue(sequence.sub(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH, true)
                .isEmpty());
        Assert.assertTrue(sequence.sub(
                new AbsoluteDateInterval(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH))
                .isEmpty());
        Assert.assertTrue(sequence.sub(
                new AbsoluteDateInterval(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH), true)
                .isEmpty());
        Assert.assertTrue(sequence.isEmpty());

        // ============================================================
        // ================ Tests on a standard sequence =============
        // ============================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDate datehalf12 = AbsoluteDate.J2000_EPOCH.shiftedBy(5.);
        final AbsoluteDate datehalf23 = AbsoluteDate.J2000_EPOCH.shiftedBy(15.);
        final AbsoluteDate datehalf34 = AbsoluteDate.J2000_EPOCH.shiftedBy(25.);
        AttitudeLeg leg1 = new AttitudeLawLeg(earthCenterAttitudeLaw, date1, date2);
        AttitudeLeg leg2 = new AttitudeLawLeg(earthCenterAttitudeLaw, date2, date3);
        AttitudeLeg leg3 = new AttitudeLawLeg(earthCenterAttitudeLaw, date3, date4);
        sequence.add(leg3);
        sequence.add(leg1);
        sequence.add(leg2);
        Assert.assertEquals(2, sequence.head(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.head(leg2.getDate(), true).size());
        Assert.assertEquals(2, sequence.tail(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.tail(leg2.getDate(), true).size());
        Assert.assertEquals(2, sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(2,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());

        // Test for copy method
        StrictAttitudeLegsSequence<AttitudeLeg> truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // For StrictLegsSequence Legs are considered to have closed boundaries, so is must not
        // change whether we ask for strict copy or not
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12, datehalf23), true);
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf23));
        Assert.assertFalse(truncatedSeq.getTimeInterval().contains(datehalf34));
        // Exception
        try {
            // Input interval not included
            sequence.copy(new AbsoluteDateInterval(date1.shiftedBy(-1), datehalf34), true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ========================================================================
        // ================ Tests on a sequence with opened bundaries =============
        // ========================================================================
        // Build sequence ]date1; date2[ U ]date2; date3[ U ]date3; date4[
        sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();
        leg1 = new AttitudeLawLeg(earthCenterAttitudeLaw, new AbsoluteDateInterval(
                IntervalEndpointType.OPEN, date1, date2, IntervalEndpointType.OPEN));
        leg2 = new AttitudeLawLeg(earthCenterAttitudeLaw, new AbsoluteDateInterval(
                IntervalEndpointType.OPEN, date2, date3, IntervalEndpointType.OPEN));
        leg3 = new AttitudeLawLeg(earthCenterAttitudeLaw, new AbsoluteDateInterval(
                IntervalEndpointType.OPEN, date3, date4, IntervalEndpointType.OPEN));
        sequence.add(leg3);
        sequence.add(leg1);
        sequence.add(leg2);
        Assert.assertEquals(2, sequence.head(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.head(leg2.getDate(), true).size());
        Assert.assertEquals(2, sequence.tail(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.tail(leg2.getDate(), true).size());
        Assert.assertEquals(2, sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(2,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());
        // ================ Test Copy method ================
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // For StrictLegsSequence Legs are considered to have closed boundaries, so is must not
        // change whether we ask for strict copy or not
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12, datehalf34), true);
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));

        // =============================================================
        // ================ Tests on a sequence with holes =============
        // =============================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();
        leg1 = new AttitudeLawLeg(earthCenterAttitudeLaw, date1, date2);
        leg3 = new AttitudeLawLeg(earthCenterAttitudeLaw, date3, date4);
        sequence.add(leg3);
        sequence.add(leg1);
        Assert.assertEquals(1, sequence.head(leg1.getDate()).size());
        Assert.assertEquals(0, sequence.head(leg1.getDate(), true).size());
        Assert.assertEquals(1, sequence.tail(leg3.getDate()).size());
        Assert.assertEquals(0, sequence.tail(leg3.getDate(), true).size());
        Assert.assertEquals(1, sequence.sub(leg3.getDate(), leg3.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg3.getDate(), leg3.getDate(), true).size());
        Assert.assertEquals(1,
                sequence.sub(new AbsoluteDateInterval(leg3.getDate(), leg3.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg3.getDate(), leg3.getDate()), true).size());
        // ================ Test Copy method ================
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // For StrictLegsSequence Legs are considered to have closed boundaries, so is must not
        // change whether we ask for strict copy or not
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12, datehalf34), true);
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));

        // ===========================================================================
        // ================ Tests on a sequence with infinite boundaries =============
        // ===========================================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();
        date1 = AbsoluteDate.PAST_INFINITY;
        date4 = AbsoluteDate.FUTURE_INFINITY;
        leg1 = new AttitudeLawLeg(earthCenterAttitudeLaw, date1, date2);
        leg2 = new AttitudeLawLeg(earthCenterAttitudeLaw, date2, date3);
        leg3 = new AttitudeLawLeg(earthCenterAttitudeLaw, date3, date4);
        sequence.add(leg3);
        sequence.add(leg1);
        sequence.add(leg2);
        Assert.assertEquals(2, sequence.head(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.head(leg2.getDate(), true).size());
        Assert.assertEquals(2, sequence.tail(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.tail(leg2.getDate(), true).size());
        Assert.assertEquals(2, sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(2,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());
        // test copy method
        final AbsoluteDate datePast = date2.shiftedBy(-1000);
        final AbsoluteDate dateFuture = date3.shiftedBy(1000);
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datePast, dateFuture));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datePast));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(dateFuture));
        Assert.assertFalse(truncatedSeq.getTimeInterval().contains(date4));

        // ===================================================
        // ================ Tests with null Legs =============
        // ===================================================
        // Exception
        try {
            // Non-existent leg
            sequence.head(null, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Exception
        try {
            // Non-existent leg
            sequence.tail(null, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Exception
        try {
            // Non-existent leg
            sequence.sub(null, null, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Exception
        try {
            // Non-existent leg
            sequence.sub(date1, null, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

    }

    /**
     * Compare Attitude instances. Needed because Attitude has no custom equals() method.
     *
     * @param expected
     *        expected Attitude
     * @param actual
     *        actual Attitude
     */
    private void compareAttitudes(final Attitude expected, final Attitude actual) {

        // final boolean eqAcc = eqNull(expected.getAcceleration(), actual.getAcceleration());
        final boolean eqDate = eqNull(expected.getDate(), actual.getDate());
        // final boolean eqJerk = eqNull(expected.getJerk(), actual.getJerk());
        // final boolean eqOrder = eqNull(expected.getOrder(), actual.getOrder());
        final boolean eqRefF = eqNull(expected.getReferenceFrame(), actual.getReferenceFrame());
        final boolean eqRot = eqNullRot(expected.getRotation(), actual.getRotation());
        // final boolean eqSnap = eqNull(expected.getSnap(), actual.getSnap());
        final boolean eqSpin = eqNull(expected.getSpin(), actual.getSpin());

        // final boolean partEq1 = eqAcc && eqDate && eqJerk;
        // final boolean partEq2 = eqOrder && eqRefF && eqRot;
        // final boolean partEq3 = eqSnap && eqSpin;
        final boolean fullEq = eqDate && eqRefF && eqRot && eqSpin;

        if (!fullEq) {
            Assert.fail("Attitude instances differ.");
        }
    }

    /**
     * Like equals, but managing null.
     *
     * @param a
     *        object a
     * @param b
     *        object b
     * @return true or false
     */
    private boolean eqNull(final Object a, final Object b) {
        boolean rez;
        if ((a == null) && (b == null)) {
            rez = true;
        } else {
            if ((a == null) || (b == null)) {
                rez = false;
            } else {
                rez = a.equals(b);
            }
        }
        return rez;
    }

    /**
     * Like equals, but managing null, for Rotation.
     *
     * @param a
     *        object a
     * @param b
     *        object b
     * @return true or false
     */
    private boolean eqNullRot(final Rotation a, final Rotation b) {
        boolean rez;
        if ((a == null) && (b == null)) {
            rez = true;
        } else {
            if ((a == null) || (b == null)) {
                rez = false;
            } else {
                final boolean eqQ0 = a.getQi()[0] == b.getQi()[0];
                final boolean eqQ1 = a.getQi()[1] == b.getQi()[1];
                final boolean eqQ2 = a.getQi()[2] == b.getQi()[2];
                final boolean eqQ3 = a.getQi()[3] == b.getQi()[3];
                rez = eqQ0 && eqQ1 && eqQ2 && eqQ3;
            }
        }
        return rez;
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#ATTITUDE_LEGS_SEQUENCE_VALIDATION}
     *
     * @testedMethod {@link StrictAttitudeLegsSequence#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link StrictAttitudeLegsSequence#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     *
     * @description Error cases for getAttitude()
     *
     * @input constructor parameters and AttitudeLeg instances
     *
     * @output the {@link StrictAttitudeLegsSequence} instance
     *
     * @testPassCriteria exceptions on getAttitude calls
     *
     * @referenceVersion 1.1
     *
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetAttitudeErrors() throws PatriusException {
        boolean asExpected = false;
        // creation of the attitude laws sequence:
        this.sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();

        // Add the laws :
        this.sequence.add(this.law1);
        this.sequence.add(this.law2);
        this.sequence.add(this.law3);

        // Call getAttitude outside of the range
        final AbsoluteDate outOfRange = new AbsoluteDate(1789, 07, 14, TimeScalesFactory.getTT());
        asExpected = false;
        try {
            this.sequence.getAttitude(this.moon, outOfRange, this.itrf);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Setup.
     *
     * @throws PatriusException
     *         should not happen
     */
    @Before
    public final void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Reference frame = ITRF 2005
        this.itrf = FramesFactory.getITRF();
        // Create earth center pointing attitude provider */
        final BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(this.itrf);

        // Spheric earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., this.itrf);

        // The Moon
        this.moon = CelestialBodyFactory.getMoon();

        // Create nadir pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);
        final AbsoluteDate date0 = new AbsoluteDate(2008, 4, 7, 12, 0, 0.0,
                TimeScalesFactory.getTT());
        final FixedRate fixedRate = new FixedRate(new Attitude(date0, this.itrf, new Rotation(
                false, 0.48, 0.64, 0.36, 0.48), Vector3D.ZERO));

        // Laws 1 to 3 are consecutive
        // Creation of the first law attitude :
        this.date1i = new AbsoluteDate(2008, 4, 7, 12, 0, 0.0, TimeScalesFactory.getTT());
        this.date1f = new AbsoluteDate(2008, 4, 7, 12, 30, 0.0, TimeScalesFactory.getTT());
        this.law1 = new AttitudeLawLeg(earthCenterAttitudeLaw, this.date1i, this.date1f);

        // Creation of the second law attitude :
        this.date2i = this.date1f;
        this.date2f = new AbsoluteDate(2008, 4, 7, 12, 50, 0.0, TimeScalesFactory.getTT());
        this.law2 = new AttitudeLawLeg(nadirAttitudeLaw, this.date2i, this.date2f);

        // Creation of the third law attitude :
        this.date3i = this.date2f;
        this.date3f = new AbsoluteDate(2008, 4, 7, 13, 15, 0.0, TimeScalesFactory.getTT());
        this.law3 = new AttitudeLawLeg(fixedRate, this.date3i, this.date3f);

        // Law 4
        // Creation of the fourth law attitude :
        this.date4i = new AbsoluteDate(2008, 4, 7, 12, 15, 0.0, TimeScalesFactory.getTT());
        this.date4f = new AbsoluteDate(2008, 4, 7, 12, 18, 0.0, TimeScalesFactory.getTT());
        this.law4 = new AttitudeLawLeg(fixedRate, this.date4i, this.date4f);

        // Law 5
        // Creation of the fifth law attitude :
        this.date5i = new AbsoluteDate(2008, 4, 7, 11, 30, 0.0, TimeScalesFactory.getTT());
        this.date5f = this.date1i;
        this.law5 = new AttitudeLawLeg(fixedRate, this.date5i, this.date5f);

        // Law 6
        // Creation of the sixth law attitude :
        this.date6i = new AbsoluteDate(2008, 4, 7, 15, 00, 0.0, TimeScalesFactory.getTT());
        this.date6f = new AbsoluteDate(2008, 4, 7, 15, 30, 0.0, TimeScalesFactory.getTT());
        ;
        this.law6 = new AttitudeLawLeg(fixedRate, this.date6i, this.date6f);
    }

    /**
     * A simple linear a.x + b leg.
     */
    private class LinearLeg implements Leg {

        /** a. */
        private final double a;

        /** b. */
        private final double b;

        /** Time interval. */
        private final AbsoluteDateInterval timeInterval;

        /**
         * Constructor
         *
         * @param timeInterval time interval of the profile
         * @param a of (slope of linear function)
         * @param b of (0-value of linear function)
         */
        public LinearLeg(final AbsoluteDateInterval timeInterval, final double a, final double b) {
            this.a = a;
            this.b = b;
            this.timeInterval = timeInterval;
        }

        @Override
        public AbsoluteDate getDate() {
            return this.timeInterval.getLowerData();
        }

        @Override
        public AbsoluteDateInterval getTimeInterval() {
            return this.timeInterval;
        }

        @Override
        public LinearLeg copy(final AbsoluteDateInterval newInterval) {
            return new LinearLeg(newInterval, this.a,
                    this.b
                            + ((newInterval.getLowerData().durationFrom(getTimeInterval()
                                    .getLowerData())) * this.a));
        }
    }

    @After
    public final void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
