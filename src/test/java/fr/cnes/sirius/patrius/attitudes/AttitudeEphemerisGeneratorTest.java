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
 * HISTORY
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:96:05/08/2013:updated to match the attitude legs sequence with codes
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:559:26/02/2016:minor corrections
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.slew.ConstantSpinSlewComputer;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
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

/**
 * <p>
 * Tests for the AttitudeEphemerisGenerator class.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AttitudeEphemerisGeneratorTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class AttitudeEphemerisGeneratorTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Attitude ephemeris generation validation with a fixed time step
         * 
         * @featureDescription test the attitude ephemeris generation with a fixed time step
         * 
         * @coveredRequirements DV-ATT_90
         */
        ATTITUDE_EPHEMERIS_VALIDATION_FIXED_STEP,

        /**
         * @featureTitle Attitude ephemeris generation validation with a variable time step
         * 
         * @featureDescription test the attitude ephemeris generation with a variable time step
         * 
         * @coveredRequirements DV-ATT_90
         */
        ATTITUDE_EPHEMERIS_VALIDATION_VARIABLE_STEP
    }

    /**
     * The start date of the sequence interval of validity.
     */
    private AbsoluteDate date0;

    /** Law 2 start date. */
    private AbsoluteDate date2i;

    /** Law 3 start date. */
    private AbsoluteDate date3i;

    /** Law 4 start date. */
    private AbsoluteDate date4i;

    /** Law 5 start date. */
    private AbsoluteDate date5i;

    /**
     * The end date of the sequence interval of validity.
     */
    private AbsoluteDate dateF;

    /**
     * Fixed step.
     */
    private double fixedStep;

    /**
     * The attitude laws sequence.
     */
    private StrictAttitudeLegsSequence sequence;

    /**
     * A frame.
     */
    private Frame frame;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_FIXED_STEP}
     * 
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#FixedStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double)}
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#generateEphemeris(Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a fixed time step and nominal parameters.
     * 
     * @input constructor parameters and a fixed time step
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set should contain the correct number of elements; these elements should
     *                   correspond to the attitudes directly computed from the corresponding attitude laws at the same
     *                   date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testFixedStepNominal() throws PatriusException {
        /* Test - nominal case: */
        final FixedStepAttitudeEphemerisGenerator generator = new FixedStepAttitudeEphemerisGenerator(this.sequence,
            this.fixedStep, null);
        final Set<Attitude> ephemeris = generator.generateEphemeris(this.frame);
        // check the size of the attitude ephemeris is the expected one:
        Assert.assertEquals(((int) (this.dateF.durationFrom(this.date0) / this.fixedStep + 1)), ephemeris.size());
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size(); i++) {
            final AbsoluteDate date = this.date0.shiftedBy(i * this.fixedStep);
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
    }

    /**
     * FA-559.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_FIXED_STEP}
     * 
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#FixedStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double)}
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#generateEphemeris(Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence has same time span as the
     *              ephemeris
     *              generated with the generator; the ephemeris generator is set with
     *              a fixed time step and nominal parameters.
     * 
     * @input constructor parameters and a fixed time step
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the generated ephemeris covers the full initial time span.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testFixedStepNominal2() throws PatriusException {
        // Initialization
        final AbsoluteDate t0 = new AbsoluteDate(2005, 6, 25, TimeScalesFactory.getTAI());
        final AbsoluteDate t1 = t0.shiftedBy(360);
        final Orbit orb = new KeplerianOrbit(6978000, 0.00095, MathLib.toRadians(75.), MathLib.toRadians(90.), 0, 0,
            PositionAngle.MEAN,
            FramesFactory.getGCRF(), t0, Constants.EIGEN5C_EARTH_MU);

        // Attitude Law, Leg and Sequence definitions
        final AttitudeLaw geoPointing = new BodyCenterPointing();
        final AttitudeLeg attLeg = new AttitudeLawLeg(geoPointing, t0, t1);
        final StrictAttitudeLegsSequence attSeq = new StrictAttitudeLegsSequence<AttitudeLeg>();
        attSeq.add(attLeg);

        // Attitude ephemeris generation, and conversion to TabulatedAttitude
        final AbstractAttitudeEphemerisGenerator ephemGen = new FixedStepAttitudeEphemerisGenerator(attSeq, 10, orb);
        final SortedSet<Attitude> ephemAtt = ephemGen.generateEphemeris(FramesFactory.getGCRF());
        final List<Attitude> ephemAttList = new LinkedList<Attitude>(ephemAtt);
        final TabulatedAttitude tabAttLeg = new TabulatedAttitude(ephemAttList);

        // Check intervals are equals
        Assert.assertTrue(attLeg.getTimeInterval().equals(tabAttLeg.getTimeInterval()));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_FIXED_STEP}
     * 
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#FixedStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double)}
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#generateEphemeris(AbsoluteDateInterval, Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a fixed time step and a time of validity smaller than the sequence time interval.
     * 
     * @input constructor parameters, a fixed time step and a time interval
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set should contain the correct number of elements; these elements should
     *                   correspond to the attitudes directly computed from the corresponding attitude laws at the same
     *                   date. Raise an exception when the ephemeris time interval is not valid.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testFixedStepSmallerInterval() throws PatriusException {
        /* Test - restricted time interval: */
        final AbsoluteDate testDate0 = this.date0.shiftedBy(2 * 60 + 7);
        final AbsoluteDate testDateF = this.dateF.shiftedBy(-55);
        final AbsoluteDateInterval testInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, testDate0,
            testDateF, IntervalEndpointType.OPEN);
        FixedStepAttitudeEphemerisGenerator generator =
            new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep, null);
        final Set<Attitude> ephemeris = generator.generateEphemeris(testInterval, this.frame);
        // check the size of the attitude ephemeris is the expected one:
        Assert.assertEquals(((int) (testDateF.durationFrom(testDate0) / this.fixedStep + 2)), ephemeris.size());
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = testDate0.shiftedBy(i * this.fixedStep);
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        final Attitude lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        final Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);

        /* Test - invalid time interval: */
        final AbsoluteDate testInvalidDate0 = this.date0.shiftedBy(-1);
        final AbsoluteDate testInvalidDateF = this.dateF.shiftedBy(-5);
        final AbsoluteDateInterval testInvalidInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            testInvalidDate0, testInvalidDateF, IntervalEndpointType.OPEN);
        boolean asExpected = false;
        try {
            generator = new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep, null);
            generator.generateEphemeris(testInvalidInterval, this.frame);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_FIXED_STEP}
     * 
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#FixedStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, int)}
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#generateEphemeris(Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a fixed time step and a nominal time interval; the start/end transition point parameter is taken
     *              into account.
     * 
     * @input constructor parameters and a fixed time step
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set should contain the correct number of elements; these elements should
     *                   correspond to the attitudes directly computed from the corresponding attitude laws at the same
     *                   date. Raise an exception when the transition points parameter is not valid.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testFixedStepTransitionPoints() throws PatriusException {
        /* Test - transition points generation (start points): */
        FixedStepAttitudeEphemerisGenerator generator =
            new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep,
                AbstractAttitudeEphemerisGenerator.START_TRANSITIONS, null);
        Set<Attitude> ephemeris = generator.generateEphemeris(this.frame);
        // check the size of the attitude ephemeris is the expected one:
        Assert.assertEquals(((int) (this.dateF.durationFrom(this.date0) / this.fixedStep + 1)) + 2, ephemeris.size());
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            Attitude expected =
                this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            if (date.equals(this.date2i) || date.equals(this.date3i) || date.equals(this.date4i)
                || date.equals(this.date5i)) {
                // transition point:
                expected = this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            }
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        Attitude lastExpected = this.sequence.getAttitude(null, this.dateF, this.frame);
        Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);

        /* Test - transition points generation (start and end points): */
        generator = new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep,
            AbstractAttitudeEphemerisGenerator.START_END_TRANSITIONS, null);
        ephemeris = generator.generateEphemeris(this.frame);
        // check the size of the attitude ephemeris is the expected one:
        Assert.assertEquals(((int) (this.dateF.durationFrom(this.date0) / this.fixedStep + 1)) + 6, ephemeris.size());
        int flag = 0;
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            Attitude expected =
                this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            if ((date.equals(this.date2i) || date.equals(this.date3i) || date.equals(this.date4i) || date
                .equals(this.date5i)) && flag == 0) {
                // transition point:
                expected = generator.getPreviousAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(),
                    this.frame);
                flag = 1;
            } else if (flag == 1) {
                flag = 0;
            }
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        lastExpected = this.sequence.getAttitude(null, this.dateF, this.frame);
        lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);

        /* Test - invalid transition points parameter: */
        boolean asExpected = false;
        try {
            generator = new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep, 5, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_FIXED_STEP}
     * 
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#FixedStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, int)}
     * @testedMethod {@link FixedStepAttitudeEphemerisGenerator#generateEphemeris(AbsoluteDateInterval, Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a fixed time step and a smaller time interval; the start/end transition point parameter is taken
     *              into account.
     * 
     * @input constructor parameters, a fixed time step and a time interval
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set should contain the correct number of elements; these elements should
     *                   correspond to the attitudes directly computed from the corresponding attitude laws at the same
     *                   date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testFixedStepTransitionPointsAndSmallerInterval() throws PatriusException {
        /* Test - transition points generation (start points) + restricted time interval: */
        /* Test - restricted time interval: */
        final AbsoluteDate testDate0 = this.date0.shiftedBy(2 * 60 + 7);
        final AbsoluteDate testDateF = this.dateF.shiftedBy(-55);
        final AbsoluteDateInterval testInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, testDate0,
            testDateF, IntervalEndpointType.OPEN);
        FixedStepAttitudeEphemerisGenerator generator =
            new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep,
                AbstractAttitudeEphemerisGenerator.START_TRANSITIONS, null);
        Set<Attitude> ephemeris = generator.generateEphemeris(testInterval, this.frame);
        // check the size of the attitude ephemeris is the expected one:
        Assert.assertEquals(((int) (testDateF.durationFrom(testDate0) / this.fixedStep + 2)), ephemeris.size());
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        Attitude lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);

        /* Test - transition points generation (start and end points) + restricted time interval: */
        generator = new FixedStepAttitudeEphemerisGenerator(this.sequence, this.fixedStep,
            AbstractAttitudeEphemerisGenerator.START_END_TRANSITIONS, null);
        ephemeris = generator.generateEphemeris(testInterval, this.frame);
        // check the size of the attitude ephemeris is the expected one:
        Assert.assertEquals(((int) (testDateF.durationFrom(testDate0) / this.fixedStep + 5)), ephemeris.size());
        int flag = 0;
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            Attitude expected =
                this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            if ((date.equals(this.date2i) || date.equals(this.date3i) || date.equals(this.date4i) || date
                .equals(this.date5i)) && flag == 0) {
                // transition point:
                expected = generator.getPreviousAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(),
                    this.frame);
                flag = 1;
            } else if (flag == 1) {
                flag = 0;
            }
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_VARIABLE_STEP}
     * 
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#VariableStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, double, double)}
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#generateEphemeris(Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator uses a
     *              variable time step.
     * 
     * @input constructor parameters and a variable time step
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set elements should correspond to the attitudes directly computed from the
     *                   corresponding attitude laws at the same date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testVariableStep() throws PatriusException {

        /* Test 1 - no transition points, angMax = 0.15 */
        final VariableStepAttitudeEphemerisGenerator generator =
            new VariableStepAttitudeEphemerisGenerator(this.sequence,
                10, 1, 0.15, null);
        final Set<Attitude> ephemeris = generator.generateEphemeris(this.frame);
        AbsoluteDate previousDate = ((Attitude) ephemeris.toArray()[0]).getDate();
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
            // check the maximum step is 10:
            Assert.assertTrue(date.durationFrom(previousDate) <= 10);
            previousDate = date;
        }
        // check the last attitude:
        final Attitude lastExpected = this.sequence.getAttitude(null, this.dateF, this.frame);
        final Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_VARIABLE_STEP}
     * 
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#VariableStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, double, double)}
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#generateEphemeris(AbsoluteDateInterval, Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a variable time step and a time of validity smaller than the sequence time interval. A small dMin is
     *              entered for coverage purposes.
     * 
     * @input constructor parameters, a variable time step and a time interval
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set elements should correspond to the attitudes directly computed from the
     *                   corresponding attitude laws at the same date.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testVariableStepSmallerInterval() throws PatriusException {
        /* Test - restricted time interval, angMax = 0.15 */
        final AbsoluteDate testDate0 = this.date0.shiftedBy(2 * 60 + 7);
        final AbsoluteDate testDateF = this.dateF.shiftedBy(-55);
        final AbsoluteDateInterval testInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, testDate0,
            testDateF, IntervalEndpointType.OPEN);
        final VariableStepAttitudeEphemerisGenerator generator =
            new VariableStepAttitudeEphemerisGenerator(this.sequence,
                0.5, 1, 0.15, null);
        final Set<Attitude> ephemeris = generator.generateEphemeris(testInterval, this.frame);
        AbsoluteDate previousDate = ((Attitude) ephemeris.toArray()[0]).getDate();
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
            // check the maximum step is 10:
            Assert.assertTrue(date.durationFrom(previousDate) <= 10);
            previousDate = date;
        }
        // check the last attitude:
        final Attitude lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        final Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_VARIABLE_STEP}
     * 
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#VariableStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, double, double)}
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#generateEphemeris(AbsoluteDateInterval, Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a variable time step and a time of validity smaller than the sequence time interval.
     * 
     * @input constructor parameters, a variable time step and a time interval
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set elements should correspond to the attitudes directly computed from the
     *                   corresponding attitude laws at the same date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testVariableStepSmallerInterval2() throws PatriusException {
        /* Test - restricted time interval, angMax = 0.15 */
        final AbsoluteDate testDate0 = this.date0.shiftedBy(2 * 60 + 7);
        final AbsoluteDate testDateF = this.dateF.shiftedBy(-55);
        final AbsoluteDateInterval testInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, testDate0,
            testDateF, IntervalEndpointType.OPEN);
        final VariableStepAttitudeEphemerisGenerator generator =
            new VariableStepAttitudeEphemerisGenerator(this.sequence,
                10, 1, 0.15, null);
        final Set<Attitude> ephemeris = generator.generateEphemeris(testInterval, this.frame);
        AbsoluteDate previousDate = ((Attitude) ephemeris.toArray()[0]).getDate();
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
            // check the maximum step is 10:
            Assert.assertTrue(date.durationFrom(previousDate) <= 10);
            previousDate = date;
        }
        // check the last attitude:
        final Attitude lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        final Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_VARIABLE_STEP}
     * 
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#VariableStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, double, double, int)}
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#generateEphemeris(Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a variable time step and a nominal time interval; the start/end transition point parameter is taken
     *              into account.
     * 
     * @input constructor parameters and a variable time step
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set elements should correspond to the attitudes directly computed from the
     *                   corresponding attitude laws at the same date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testVariableStepTransitionPoints() throws PatriusException {
        /* Test - transition points generation (start points), angMax = 0.15 */
        VariableStepAttitudeEphemerisGenerator generator =
            new VariableStepAttitudeEphemerisGenerator(this.sequence, 10, 1,
                0.15, AbstractAttitudeEphemerisGenerator.START_TRANSITIONS, null);
        Set<Attitude> ephemeris = generator.generateEphemeris(this.frame);
        AbsoluteDate previousDate = ((Attitude) ephemeris.toArray()[0]).getDate();
        final Set<Attitude> ephemeris_start = ephemeris;
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            final Attitude expected =
                this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
            // check the maximum step is 10:
            Assert.assertTrue(date.durationFrom(previousDate) <= 10);
            previousDate = date;
        }
        // check the last attitude:
        Attitude lastExpected = this.sequence.getAttitude(null, this.dateF, this.frame);
        Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);

        /* Test - transition points generation (start and end points). */
        generator = new VariableStepAttitudeEphemerisGenerator(this.sequence, 10, 1, 0.15,
            AbstractAttitudeEphemerisGenerator.START_END_TRANSITIONS, null);
        ephemeris = generator.generateEphemeris(this.frame);
        previousDate = ((Attitude) ephemeris.toArray()[0]).getDate();
        final Set<Attitude> ephemeris5 = ephemeris;
        int flag = 0;
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            Attitude expected =
                this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            if ((date.equals(this.date2i) || date.equals(this.date3i) || date.equals(this.date4i) || date
                .equals(this.date5i)) && flag == 0) {
                // transition point:
                expected = generator.getPreviousAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(),
                    this.frame);
                flag = 1;
            } else if (flag == 1) {
                flag = 0;
            }
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
            // check the maximum step is 10:
            Assert.assertTrue(date.durationFrom(previousDate) <= 10);
            previousDate = date;
        }
        // check the last attitude:
        lastExpected = this.sequence.getAttitude(null, this.dateF, this.frame);
        lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);
        // the size of the start-end ephemeris should be equal to the size of the start only ephemeris + 2 elements:
        Assert.assertEquals(ephemeris5.size(), ephemeris_start.size() + 4);

        /* Test - invalid transition points parameter: */
        boolean asExpected = false;
        try {
            generator = new VariableStepAttitudeEphemerisGenerator(this.sequence, 10, 1, 0.15, 3, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_EPHEMERIS_VALIDATION_VARIABLE_STEP}
     * 
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#VariableStepAttitudeEphemerisGenerator(StrictAttitudeLegsSequence, double, double, double, int)}
     * @testedMethod {@link VariableStepAttitudeEphemerisGenerator#generateEphemeris(AbsoluteDateInterval, Frame)}
     * 
     * @description checks that the attitude ephemeris generated from an attitude law sequence are equal to the
     *              attitudes directly computed from the corresponding attitude law; the ephemeris generator is set with
     *              a variable time step and a smaller time interval; the start/end transition point parameter is taken
     *              into account.
     * 
     * @input constructor parameters, a time interval and a variable time step
     * 
     * @output a set of {@link Attitude} instances
     * 
     * @testPassCriteria the ephemeris set elements should correspond to the attitudes directly computed from the
     *                   corresponding attitude laws at the same date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testVariableStepTransitionPointsAndSmallerInterval() throws PatriusException {
        /* Test - transition points generation (start points) + restricted time interval: */
        /* Test - restricted time interval: */
        final AbsoluteDate testDate0 = this.date0.shiftedBy(2 * 60 + 7);
        final AbsoluteDate testDateF = this.dateF.shiftedBy(-55);
        final AbsoluteDateInterval testInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, testDate0,
            testDateF, IntervalEndpointType.OPEN);
        VariableStepAttitudeEphemerisGenerator generator =
            new VariableStepAttitudeEphemerisGenerator(this.sequence, 10, 1,
                0.15, AbstractAttitudeEphemerisGenerator.START_TRANSITIONS, null);
        Set<Attitude> ephemeris = generator.generateEphemeris(testInterval, this.frame);
        final Set<Attitude> ephemeris_start = ephemeris;
        // check the attitudes in the sequence:
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            final Attitude expected = this.sequence.getAttitude(null, date, this.frame);
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        Attitude lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        Attitude lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);

        /* Test - transition points generation (start and end points) + restricted time interval: */
        generator = new VariableStepAttitudeEphemerisGenerator(this.sequence, 10, 1, 0.15,
            AbstractAttitudeEphemerisGenerator.START_END_TRANSITIONS, null);
        ephemeris = generator.generateEphemeris(testInterval, this.frame);
        // check the attitudes in the sequence:
        int flag = 0;
        for (int i = 0; i < ephemeris.size() - 1; i++) {
            final AbsoluteDate date = ((Attitude) ephemeris.toArray()[i]).getDate();
            Attitude expected =
                this.sequence.getAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(), this.frame);
            if ((date.equals(this.date2i) || date.equals(this.date3i) || date.equals(this.date4i) || date
                .equals(this.date5i)) && flag == 0) {
                // transition point:
                expected = generator.getPreviousAttitude(null, ((Attitude) ephemeris.toArray()[i]).getDate(),
                    this.frame);
                flag = 1;
            } else if (flag == 1) {
                flag = 0;
            }
            final Attitude rez = (Attitude) ephemeris.toArray()[i];
            this.compareAttitudes(expected, rez);
        }
        // check the last attitude:
        lastExpected = this.sequence.getAttitude(null, testDateF, this.frame);
        lastRez = (Attitude) ephemeris.toArray()[ephemeris.size() - 1];
        this.compareAttitudes(lastExpected, lastRez);
        // the size of the start-end ephemeris should be equal to the size of the start only ephemeris + 3 elements:
        Assert.assertEquals(ephemeris.size(), ephemeris_start.size() + 3);
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
        final boolean eqDate = this.eqNull(expected.getDate(), actual.getDate());
        // final boolean eqJerk = eqNull(expected.getJerk(), actual.getJerk());
        // final boolean eqOrder = eqNull(expected.getOrder(), actual.getOrder());
        final boolean eqRefF = this.eqNull(expected.getReferenceFrame(), actual.getReferenceFrame());
        final boolean eqRot = this.eqNullRot(expected.getRotation(), actual.getRotation());
        // final boolean eqSnap = eqNull(expected.getSnap(), actual.getSnap());
        final boolean eqSpin = this.eqNull(expected.getSpin(), actual.getSpin());

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
        if (a == null && b == null) {
            rez = true;
        } else {
            if (a == null || b == null) {
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
        if (a == null && b == null) {
            rez = true;
        } else {
            if (a == null || b == null) {
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
        this.frame = FramesFactory.getITRF();
        this.date0 = new AbsoluteDate(2012, 4, 26, 9, 0, 0.0, TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.02, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), this.date0, Constants.EGM96_EARTH_MU);
        this.sequence = new StrictAttitudeLegsSequence<AttitudeLeg>();

        // Attitude laws in the sequence:
        // Creation of the first law attitude (inertial law):
        final AbsoluteDate date1i = this.date0;
        final AbsoluteDate date1f = this.date0.shiftedBy(60 * 2 + 5);
        final ConstantAttitudeLaw inertialLaw1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(
            true, 1, 0, 0, 0)));
        final AttitudeLawLeg law1 = new AttitudeLawLeg(inertialLaw1, date1i, date1f);

        // Creation of the third law attitude (inertial law):
        this.date3i = date1f.shiftedBy(10);
        final AbsoluteDate date3f = this.date3i.shiftedBy(49);
        final ConstantAttitudeLaw inertialLaw2 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(
            true, .5, .5, .5, .5)));
        final AttitudeLawLeg law3 = new AttitudeLawLeg(inertialLaw2, this.date3i, date3f);

        // Creation of the second law attitude (slerp):
        final double duration = 10;
        final ConstantSpinSlew slerp1 = new ConstantSpinSlew(inertialLaw1.getAttitude(orbit, date1f, FramesFactory.getEME2000()),
                inertialLaw2.getAttitude(orbit, date1f.shiftedBy(duration), FramesFactory.getEME2000()));
        this.date2i = date1f;

        // Creation of the fourth and fifth law attitude (inertial law):
        final ConstantAttitudeLaw inertialLaw3 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(
            true, .5, -.5, -.5, -.5)));
        final double angVelocity = .01;
        final ConstantSpinSlew slerp2 = new ConstantSpinSlewComputer(angVelocity).compute(orbit, inertialLaw2, date3f, inertialLaw3, null);
        this.date4i = date3f;
        final AbsoluteDate date4f = slerp2.getTimeInterval().getUpperData();
        this.date5i = date4f;
        this.dateF = this.date5i.shiftedBy(60 + 20.24);
        final AttitudeLawLeg law5 = new AttitudeLawLeg(inertialLaw3, this.date5i, this.dateF);

        this.sequence.add(law1);
        this.sequence.add(slerp1);
        this.sequence.add(law3);
        this.sequence.add(slerp2);
        this.sequence.add(law5);

        this.fixedStep = 10;
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
