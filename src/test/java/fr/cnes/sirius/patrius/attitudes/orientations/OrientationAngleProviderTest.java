/**
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
 * VERSION:4.13:DM:DM-6:08/12/2023:[PATRIUS] Suppression de l'attribut "nature" dans OrientationAngleLegsSequence
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3152:10/05/2022:[PATRIUS] Suppression de l'attribut "nature" dans OrientationAngleLegsSequence  
 * VERSION:4.9:DM:DM-3188:10/05/2022:[PATRIUS] Rendre generique la classe OrientationAngleLegsSequence
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2847:18/05/2021:Modification de la gestion de la date hors intervalle
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.UnivariateDateFunction;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the orientation package.
 *              </p>
 *
 * @author Emmanuel Bignon
 *
 * @version $Id: AeroAttitudeLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 *
 * @since 4.2
 */
public class OrientationAngleProviderTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle orientation angle provider
         *
         * @featureDescription object describing an orientation angle around a reference axis
         *
         * @coveredRequirements DM-1949
         */
        ORIENTATION
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(OrientationAngleProviderTest.class.getSimpleName(),
                "Orientation angle provider");
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleProvider#getOrientationAngle(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate)}
     *
     * @description Compute orientation angle given constant orientation law
     *
     * @input constant angle
     *
     * @output orientation angle
     *
     * @testPassCriteria angle is as expected (reference: math)
     *
     * @referenceVersion 4.2
     *
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testConstantOrientationAngleLaw() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testConstantOrientationAngleLaw",
                "Orientation angle law - Angle computation", "Math", 1E-14, ComparisonType.ABSOLUTE);

        final double angle = 2.2;
        final OrientationAngleProvider provider = new ConstantOrientationAngleLaw(angle);

        // Computation and check
        final double actual = provider.getOrientationAngle(null, null);
        final double expected = angle;
        Report.printToReport("Angle", expected, actual);
        Assert.assertEquals(expected, actual, 1E-14);
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleProvider#getOrientationAngle(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate)}
     *
     * @description Compute orientation angle given constant orientation leg
     *
     * @input constant angle
     *
     * @output orientation angle
     *
     * @testPassCriteria angle is as expected (reference: math)
     *
     * @referenceVersion 4.2
     *
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testConstantOrientationAngleLeg() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testConstantOrientationAngleLeg",
                "Orientation angle leg - Angle computation", "Math", 1E-14, ComparisonType.ABSOLUTE);

        final double angle = 2.2;
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(AbsoluteDate.JAVA_EPOCH,
                AbsoluteDate.J2000_EPOCH);
        final OrientationAngleLeg provider = new ConstantOrientationAngleLeg(interval, angle);

        // Computation and check
        final double actual = provider.getOrientationAngle(null,
                AbsoluteDate.JAVA_EPOCH.shiftedBy(1));
        final double expected = angle;
        Report.printToReport("Angle", expected, actual);
        Assert.assertEquals(expected, actual, 1E-14);

        // Check time interval
        Assert.assertEquals(0,
                provider.getTimeInterval().getLowerData().durationFrom(AbsoluteDate.JAVA_EPOCH), 0);
        Assert.assertEquals(0,
                provider.getTimeInterval().getUpperData().durationFrom(AbsoluteDate.J2000_EPOCH), 0);

        // Check exception (date outside interval)
        try {
            provider.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(-1));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleProvider#getOrientationAngle(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate)}
     *
     * @description Compute orientation angle given orientation law leg
     *
     * @input constant angle
     *
     * @output orientation angle
     *
     * @testPassCriteria angle is as expected (reference: math)
     *
     * @referenceVersion 4.2
     *
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testOrientationAngleLawLeg() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testOrientationAngleLawLeg",
                "Orientation angle law leg - Angle computation", "Math", 1E-14,
                ComparisonType.ABSOLUTE);

        final double angle = 2.2;
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(AbsoluteDate.JAVA_EPOCH,
                AbsoluteDate.J2000_EPOCH);
        final OrientationAngleLaw orientationLaw = new ConstantOrientationAngleLaw(angle);
        final OrientationAngleLeg provider = new OrientationAngleLawLeg(orientationLaw,
                interval.getLowerData(), interval.getUpperData());

        // Computation and check
        final double actual = provider.getOrientationAngle(null,
                AbsoluteDate.JAVA_EPOCH.shiftedBy(1));
        final double expected = angle;
        Report.printToReport("Angle", expected, actual);
        Assert.assertEquals(expected, actual, 1E-14);

        // Check time interval
        Assert.assertEquals(0,
                provider.getTimeInterval().getLowerData().durationFrom(AbsoluteDate.JAVA_EPOCH), 0);
        Assert.assertEquals(0,
                provider.getTimeInterval().getUpperData().durationFrom(AbsoluteDate.J2000_EPOCH), 0);


        // Check exception (date outside interval)
        try {
            provider.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(-1));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Check no exception if time tolerant
        final OrientationAngleLeg provider2 = new OrientationAngleLawLeg(orientationLaw,
                interval.getLowerData(), interval.getUpperData(), "", true);
        Assert.assertEquals(expected,
                provider2.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(-1)), 1E-14);
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleProvider#getOrientationAngle(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate)}
     *
     * @description Compute orientation angle given orientation angle legs sequence
     *
     * @input orientation angle legs sequence:
     *        - Constant angle on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Linear angle on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant angle on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     *
     * @output orientation angle at different dates
     *
     * @testPassCriteria angle is as expected (reference: math)
     *
     * @referenceVersion 4.2
     *
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testOrientationAngleLegsSequence() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testOrientationAngleLegsSequence",
                "Orientation angle legs sequence - Angle computation", "Math", 1E-14,
                ComparisonType.ABSOLUTE);

        // Build profile
        final OrientationAngleLegsSequence<LinearLeg> provider = new OrientationAngleLegsSequence<>();
        Assert.assertNull(provider.getTimeInterval());
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDate datehalf12 = AbsoluteDate.J2000_EPOCH.shiftedBy(5.);
        final AbsoluteDate datehalf23 = AbsoluteDate.J2000_EPOCH.shiftedBy(15.);
        final AbsoluteDate datehalf34 = AbsoluteDate.J2000_EPOCH.shiftedBy(25.);
        provider.add(new LinearLeg(new AbsoluteDateInterval(date1, date2), 0., 2.5));
        provider.add(new LinearLeg(new AbsoluteDateInterval(date2, date3), 1., 2.5));
        provider.add(new LinearLeg(new AbsoluteDateInterval(date3, date4), 0., 12.5));

        // Computation and check angle on each segment
        final double actual1 = provider.getOrientationAngle(null, date1.shiftedBy(1));
        final double actual2 = provider.getOrientationAngle(null, date2.shiftedBy(1));
        final double actual3 = provider.getOrientationAngle(null, date3.shiftedBy(1));
        final double expected1 = 2.5;
        final double expected2 = 2.5 + 1.;
        final double expected3 = 12.5;
        Report.printToReport("Angle on 1st segment", expected1, actual1);
        Report.printToReport("Angle on 2nd segment", expected2, actual2);
        Report.printToReport("Angle on 3rd segment", expected3, actual3);
        Assert.assertEquals(expected1, actual1, 1E-14);
        Assert.assertEquals(expected2, actual2, 1E-14);
        Assert.assertEquals(expected3, actual3, 1E-14);

        // Check time interval
        Assert.assertEquals(0, provider.getTimeInterval().getLowerData().durationFrom(date1), 0);
        Assert.assertEquals(0, provider.getTimeInterval().getUpperData().durationFrom(date4), 0);

        // Check exception (date outside interval)
        Assert.assertNull(provider.getOrientationAngle(null, date1.shiftedBy(-1)));
        Assert.assertNull(provider.getOrientationAngle(null, date4.shiftedBy(1)));

        // Test copy method
        OrientationAngleLegsSequence<?> truncatedSeq = provider.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        truncatedSeq = provider.copy(new AbsoluteDateInterval(datehalf12, datehalf23), true);
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf23));
        Assert.assertFalse(truncatedSeq.getTimeInterval().contains(datehalf34));
        // Exception
        try {
            // Input interval not included
            provider.copy(new AbsoluteDateInterval(date1.shiftedBy(-1), datehalf34), true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleProvider#getOrientationAngle(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate)}
     *
     * @description Compute orientation angle given orientation angle profile sequence
     *
     * @input orientation angle profile sequence:
     *        - Constant angle on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Linear angle on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant angle on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     *
     * @output orientation angle at different dates
     *
     * @testPassCriteria angle is as expected (reference: math)
     *
     * @referenceVersion 4.2
     *
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testOrientationAngleProfileSequenceLeg() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testOrientationAngleProfileSequenceLeg",
                "Orientation angle profile sequence - Angle computation", "Math", 1E-14,
                ComparisonType.ABSOLUTE);

        // Build profile
        final OrientationAngleProfileSequence provider = new OrientationAngleProfileSequence(
                "Nature");
        Assert.assertNull(provider.getTimeInterval());
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        provider.add(new LinearLeg(new AbsoluteDateInterval(date1, date2), 0., 2.5));
        provider.add(new LinearLeg(new AbsoluteDateInterval(date2, date3), 1., 2.5));
        provider.add(new LinearLeg(new AbsoluteDateInterval(date3, date4), 0., 12.5));

        // Computation and check angle on each segment
        final double actual1 = provider.getOrientationAngle(null, date1.shiftedBy(1));
        final double actual2 = provider.getOrientationAngle(null, date2.shiftedBy(1));
        final double actual3 = provider.getOrientationAngle(null, date3.shiftedBy(1));
        final double expected1 = 2.5;
        final double expected2 = 2.5 + 1.;
        final double expected3 = 12.5;
        Report.printToReport("Angle on 1st segment", expected1, actual1);
        Report.printToReport("Angle on 2nd segment", expected2, actual2);
        Report.printToReport("Angle on 3rd segment", expected3, actual3);
        Assert.assertEquals(expected1, actual1, 1E-14);
        Assert.assertEquals(expected2, actual2, 1E-14);
        Assert.assertEquals(expected3, actual3, 1E-14);

        // Check time interval
        Assert.assertEquals(0, provider.getTimeInterval().getLowerData().durationFrom(date1), 0);
        Assert.assertEquals(0, provider.getTimeInterval().getUpperData().durationFrom(date4), 0);


        // Check exception (date outside interval)
        Assert.assertNull(provider.getOrientationAngle(null, date1.shiftedBy(-1)));
        Assert.assertNull(provider.getOrientationAngle(null, date4.shiftedBy(1)));
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleLegsSequence#head(AbsoluteDate, boolean), @link
     *               OrientationAngleLegsSequence#tail(AbsoluteDate, boolean) and @link
     *               OrientationAngleLegsSequence#sub(AbsoluteDateInterval, boolean)}
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
    public void testHeadTailSubOrientationAngleLegsSequence() {

        // Init sequence
        OrientationAngleLegsSequence<AbstractOrientationAngleLeg> sequence = new OrientationAngleLegsSequence<>();

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
        LinearLeg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        LinearLeg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        LinearLeg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
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

        // ========================================================================
        // ================ Tests on a sequence with opened bundaries =============
        // ========================================================================
        // Build sequence ]date1; date2[ U ]date2; date3[ U ]date3; date4[
        sequence = new OrientationAngleLegsSequence<>();
        leg1 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1, date2,
                IntervalEndpointType.OPEN), 1, 2);
        leg2 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date2, date3,
                IntervalEndpointType.OPEN), 2, 3);
        leg3 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date3, date4,
                IntervalEndpointType.OPEN), 3, 4);
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

        // =============================================================
        // ================ Tests on a sequence with holes =============
        // =============================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        sequence = new OrientationAngleLegsSequence<>();
        leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
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

        // ===========================================================================
        // ================ Tests on a sequence with infinite boundaries =============
        // ===========================================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        sequence = new OrientationAngleLegsSequence<>();
        date1 = AbsoluteDate.PAST_INFINITY;
        date4 = AbsoluteDate.FUTURE_INFINITY;
        leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
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
     * @testType UT
     *
     * @testedFeature {@link features#ORIENTATION}
     *
     * @testedMethod {@link OrientationAngleProfileSequence#head(AbsoluteDate, boolean), @link
     *               OrientationAngleProfileSequence#tail(AbsoluteDate, boolean) and @link
     *               OrientationAngleProfileSequence#sub(AbsoluteDateInterval, boolean)}
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
    public void testHeadTailSubOrientationAngleProfileSequence() {

        // Init sequence
        OrientationAngleProfileSequence sequence = new OrientationAngleProfileSequence("Nature");

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
        LinearLeg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        LinearLeg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        LinearLeg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
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

        // ========================================================================
        // ================ Tests on a sequence with opened bundaries =============
        // ========================================================================
        // Build sequence ]date1; date2[ U ]date2; date3[ U ]date3; date4[
        sequence = new OrientationAngleProfileSequence("Nature");
        leg1 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1, date2,
                IntervalEndpointType.OPEN), 1, 2);
        leg2 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date2, date3,
                IntervalEndpointType.OPEN), 2, 3);
        leg3 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date3, date4,
                IntervalEndpointType.OPEN), 3, 4);
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

        // =============================================================
        // ================ Tests on a sequence with holes =============
        // =============================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        sequence = new OrientationAngleProfileSequence("Nature");
        leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
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

        // ===========================================================================
        // ================ Tests on a sequence with infinite boundaries =============
        // ===========================================================================
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        sequence = new OrientationAngleProfileSequence("Nature");
        date1 = AbsoluteDate.PAST_INFINITY;
        date4 = AbsoluteDate.FUTURE_INFINITY;
        leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
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
     * @testType UT
     * 
     * @testedMethod {@link OrientationAngleProvider#build(UnivariateDateFunction)}
     * @testedMethod {@link OrientationAngleLeg#build(UnivariateDateFunction, AbsoluteDateInterval, String)}
     *
     * @description Evaluate the interfaces static implementations
     *
     * @testPassCriteria The static implementations behave as expected with their implemented methods
     *
     * @referenceVersion 4.11.1
     *
     * @nonRegressionVersion 4.11.1
     */
    @Test
    public void testStaticImplementation() throws PatriusException {

        // --- Evaluate the OrientationAngleProvider.build(function) method
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 2., 3., 4, 5 };
        final PolynomialFunction poly = new PolynomialFunction(coefs);
        final UnivariateDateFunction function = new DatePolynomialFunction(date0, poly);

        final OrientationAngleProvider orientationAngleProvider = OrientationAngleProvider.build(function);

        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date2 = date1.shiftedBy(60.);
        final AbsoluteDate date3 = date2.shiftedBy(60.);

        // Note: the PVCoordinatesProvider parameter isn't used (can be null)
        Assert.assertEquals(function.value(date1), orientationAngleProvider.getOrientationAngle(null, date1), 1E-14);
        Assert.assertEquals(function.value(date2), orientationAngleProvider.getOrientationAngle(null, date2), 1E-14);
        Assert.assertEquals(function.value(date3), orientationAngleProvider.getOrientationAngle(null, date3), 1E-14);

        // --- Evaluate the OrientationAngleLeg.build(function, timeInterval, nature) method
        final AbsoluteDateInterval timeInterval = new AbsoluteDateInterval(date1, date3);
        final String nature = "natureOrientationAngleLeg";

        final OrientationAngleLeg orientationAngleLeg = OrientationAngleLeg.build(function, timeInterval, nature);

        Assert.assertEquals(timeInterval, orientationAngleLeg.getTimeInterval());

        // Note: the PVCoordinatesProvider parameter isn't used (can be null)
        Assert.assertEquals(function.value(date1), orientationAngleLeg.getOrientationAngle(null, date1), 1E-14);
        Assert.assertEquals(function.value(date2), orientationAngleLeg.getOrientationAngle(null, date2), 1E-14);
        Assert.assertEquals(function.value(date3), orientationAngleLeg.getOrientationAngle(null, date3), 1E-14);

        // Evaluate the copy(newInterval) method
        final AbsoluteDateInterval newInterval = new AbsoluteDateInterval(date1, date2);
        final OrientationAngleLeg orientationAngleLegCopy = orientationAngleLeg.copy(newInterval);

        Assert.assertEquals(newInterval, orientationAngleLegCopy.getTimeInterval());

        Assert.assertEquals(function.value(date1), orientationAngleLegCopy.getOrientationAngle(null, date1), 1E-14);
        Assert.assertEquals(function.value(date2), orientationAngleLegCopy.getOrientationAngle(null, date2), 1E-14);
        Assert.assertEquals(function.value(date3), orientationAngleLegCopy.getOrientationAngle(null, date3), 1E-14);
    }

    /**
     * Linear leg: orientation = a.(t - t0) + b.
     */
    private class LinearLeg extends AbstractOrientationAngleProfile implements OrientationAngleLaw {

        /** Serializable UID. */
        private static final long serialVersionUID = 6240515053437818444L;

        /** a. */
        private final double a;

        /** b. */
        private final double b;

        /**
         * Constructor
         *
         * @param timeInterval time interval of the profile
         * @param a of (slope of linear function)
         * @param b of (0-value of linear function)
         */
        public LinearLeg(final AbsoluteDateInterval timeInterval, final double a, final double b) {
            super(timeInterval);
            this.a = a;
            this.b = b;
        }

        @Override
        public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
                final AbsoluteDate date) throws PatriusException {
            return (this.a * date.durationFrom(getTimeInterval().getLowerData())) + this.b;
        }

        @Override
        public LinearLeg copy(final AbsoluteDateInterval newInterval) {
            return new LinearLeg(newInterval, this.a,
                    this.b
                            + ((newInterval.getLowerData().durationFrom(getTimeInterval()
                                    .getLowerData())) * this.a));
        }
    }

    /**
     * Quadratic leg: orientation = a.(t - t0)² + b.(t - t0) + c.
     */
    private class QuadraticLeg extends AbstractOrientationAngleProfile implements
            OrientationAngleLaw {

        /** Serializable UID. */
        private static final long serialVersionUID = -8526072898512858966L;

        /** a. */
        private final double a;

        /** b. */
        private final double b;

        /** c. */
        private final double c;

        /**
         * Constructor
         *
         * @param timeInterval time interval of the profile
         * @param a of (slope of quadratic function)
         * @param b of (slope of linear function)
         * @param c of (0-value of linear function)
         */
        public QuadraticLeg(final AbsoluteDateInterval timeInterval, final double a,
                final double b, final double c) {
            super(timeInterval);
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
                final AbsoluteDate date) throws PatriusException {
            final double dt = date.durationFrom(getTimeInterval().getLowerData());
            return (this.a * dt * dt) + (this.b * dt) + this.c;
        }

        @Override
        public QuadraticLeg copy(final AbsoluteDateInterval newInterval) {
            // Unused
            return null;
        }
    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link ConstantOrientationAngleLeg#copy(AbsoluteDateInterval)}
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
    public void testCopyMethodConstantOrientationAngleLeg() throws PatriusException {

        // Constructor dates
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(50.);

        final double angle = 2.2;
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(startDate, endDate);

        // Intervals creation
        final double offset = 5;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, startDate.shiftedBy(offset),
                endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, startDate.shiftedBy(offset),
                endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        ConstantOrientationAngleLeg provider1 = new ConstantOrientationAngleLeg(interval, angle);
        ConstantOrientationAngleLeg provider2 = new ConstantOrientationAngleLeg(interval, angle);

        final double angleRef = provider1.getOrientationAngle(null, startDate.shiftedBy(5));

        // Test case n°1 : in a standard usage, the interval stored should be updated
        provider1 = provider1.copy(newIntervalOfValidity);
        Assert.assertTrue(provider1.getTimeInterval().equals(newIntervalOfValidity));
        Assert.assertEquals(angleRef, provider1.getOrientationAngle(null, startDate.shiftedBy(5)), 0.);

        // Test case n°2 : when the new interval isn't included, the interval stored should be
        // updated
        provider2 = provider2.copy(newIntervalOfValidityNotIncluded);
        Assert.assertTrue(provider2.getTimeInterval().equals(newIntervalOfValidityNotIncluded));
    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link OrientationAngleLawLeg#copy(AbsoluteDateInterval)}
     *
     * @description Test the new method
     *
     * @input parameters
     *
     * @output AbsoluteDateInterval
     *
     * @testPassCriteria The method behavior is correct
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testCopyMethodOrientationAngleLawLeg() throws PatriusException {

        // Constructor dates
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(50.);

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(startDate, endDate);

        // Intervals creation
        final double offset = 5;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, startDate.shiftedBy(offset),
                endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, startDate.shiftedBy(offset),
                endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        OrientationAngleLawLeg provider1 = new OrientationAngleLawLeg(
                new ConstantOrientationAngleLaw(10), interval.getLowerData(),
                interval.getUpperData());
        OrientationAngleLawLeg provider2 = new OrientationAngleLawLeg(
                new ConstantOrientationAngleLaw(10), interval.getLowerData(),
                interval.getUpperData());

        final double angleRef = provider1.getOrientationAngle(null, startDate.shiftedBy(5));

        // Test case n°1 : in a standard usage, the interval stored should be updated
        provider1 = provider1.copy(newIntervalOfValidity);
        Assert.assertTrue(provider1.getTimeInterval().equals(newIntervalOfValidity));
        Assert.assertEquals(angleRef, provider1.getOrientationAngle(null, startDate.shiftedBy(5)), 0.);

        // Test case n°2 : when the new interval isn't included, the interval stored should be
        // updated
        provider2 = provider2.copy(newIntervalOfValidityNotIncluded);
        Assert.assertTrue(provider2.getTimeInterval().equals(newIntervalOfValidityNotIncluded));
    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link OrientationAngleProfileSequence#copy(AbsoluteDateInterval)}
     *
     * @description Test the new method
     *
     * @input parameters
     *
     * @output AbsoluteDateInterval
     *
     * @testPassCriteria The method behavior is correct
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testCopyMethodOrientationAngleProfileSequence() throws PatriusException {

        // Constructor dates
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(50.);
        final AbsoluteDate endDate2 = startDate.shiftedBy(100.);

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(startDate, endDate);
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(endDate, endDate2);

        // Intervals creation
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, startDate.shiftedBy(55), startDate.shiftedBy(70),
                IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        OrientationAngleProfileSequence provider1 = new OrientationAngleProfileSequence();
        provider1.add(new LinearLeg(new AbsoluteDateInterval(interval.getLowerData(), interval
                .getUpperData()), 0., 2.5));
        provider1.add(new LinearLeg(new AbsoluteDateInterval(interval2.getLowerData(), interval2
                .getUpperData()), 1., 2.5));
        final OrientationAngleLegsSequence<LinearLeg> provider2 = new OrientationAngleLegsSequence<>();
        provider2.add(new LinearLeg(new AbsoluteDateInterval(interval.getLowerData(), interval
                .getUpperData()), 0., 2.5));
        provider2.add(new LinearLeg(new AbsoluteDateInterval(interval2.getLowerData(), interval2
                .getUpperData()), 1., 2.5));

        final double angleRef = provider1.getOrientationAngle(null, startDate.shiftedBy(70));

        // Test case n°1 : in a standard usage, the interval stored should be updated
        provider1 = provider1.copy(newIntervalOfValidity);
        Assert.assertTrue(provider1.getTimeInterval().equals(newIntervalOfValidity));
        Assert.assertEquals(angleRef, provider1.getOrientationAngle(null, startDate.shiftedBy(70)), 0.);

        // Exception
        try {
            // Input interval not included
            provider1.copy(new AbsoluteDateInterval(startDate.shiftedBy(-1), endDate), true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Quick test for coverage: OrientationAngleLegsSequence constructor and nature getter
        OrientationAngleLegsSequence<LinearLeg> provider = new OrientationAngleLegsSequence<>();
        Assert.assertEquals(OrientationAngleLegsSequence.DEFAULT_ORIENTATION_SEQUENCE_NATURE, provider.getNature());

        provider = new OrientationAngleLegsSequence<>("customNature");
        Assert.assertEquals("customNature", provider.getNature());

        Assert.assertEquals("ORIENTATION_ANGLE_LEGS_SEQUENCE",
            OrientationAngleLegsSequence.DEFAULT_ORIENTATION_SEQUENCE_NATURE);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link OrientationAngleProvider#computeSpinDerivativeByFD(PVCoordinatesProvider, AbsoluteDate, double)}
     *
     * @description
     *              <p>
     *              The spin derivative Numerical computer method
     *              </p>
     *              <p>
     *              Two {@link OrientationAngleLeg} and {@link OrientationAngleLaw} are defined.
     *              Their spin derivative are computed numerically and are compared to their
     *              analytical spin derivative on a whole orbital period.
     *              </p>
     *
     * @testPassCriteria spin derivative obtained are equal to the expected ideal spin derivative
     *                   (absolute tolerance: 1E-12)
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testComputeSpinDerivativeByFD() throws PatriusException {

        final double computationStep = 0.1;

        // Propagator
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, 0.1, 0.2, 0.3, 0.4,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,
                Constants.WGS84_EARTH_MU);
        final Propagator pvProv = new KeplerianPropagator(initialOrbit);

        // Test interval
        final SpacecraftState scState = pvProv.getInitialState();
        final AbsoluteDate startInterval = scState.getDate();
        final double duration = scState.getKeplerianPeriod();
        final AbsoluteDate endInterval = startInterval.shiftedBy(duration);

        // Attitude law and leg for testing
        final OrientationAngleLaw attLaw = new QuadraticLeg(new AbsoluteDateInterval(startInterval,
                endInterval), 10., 20., 30.);
        final OrientationAngleLeg attLeg = new OrientationAngleLawLeg(attLaw, startInterval,
                endInterval);

        final double testStep = 60.0;
        AbsoluteDate currentDate = startInterval;
        while (currentDate.compareTo(endInterval) <= 0) {
            // Invoke method to be tested
            final double attLegNumericalAcce = attLeg.computeSpinDerivativeByFD(pvProv,
                    currentDate, computationStep);
            final double attLawNumericalAcc = attLaw.computeSpinDerivativeByFD(pvProv, currentDate,
                    computationStep);

            // Compute expected values
            final double attLegIdealAcc = 20;
            final double attLawIdealAcc = 20;

            // Assert results
            Assert.assertEquals(attLegIdealAcc, attLegNumericalAcce, 1E-5);
            Assert.assertEquals(attLawIdealAcc, attLawNumericalAcc, 1E-5);

            // Advance date
            currentDate = currentDate.shiftedBy(testStep);
        }

        // Invoke method to be tested at the end date of the leg interval
        final double attLegNumericalAccEnd = attLeg.computeSpinDerivativeByFD(pvProv, endInterval,
                computationStep);
        final double attLegIdealAccEnd = 20;

        // Assert result
        Assert.assertEquals(attLegIdealAccEnd, attLegNumericalAccEnd, 1E-4);

        // Degenerated case: leg interval shorter than FD step
        final OrientationAngleLaw attLaw2 = new QuadraticLeg(new AbsoluteDateInterval(
                startInterval, startInterval.shiftedBy(1E-2)), 10., 20, 30.);
        final OrientationAngleLeg attLeg2 = new OrientationAngleLawLeg(attLaw2, startInterval,
                startInterval.shiftedBy(1E-2));
        final AbsoluteDate date = startInterval.shiftedBy(0.5E-2);
        final double attLegNumericalAcce2 = attLeg2.computeSpinDerivativeByFD(pvProv, date,
                computationStep);
        final double attLegIdealAcc2 = 20;
        Assert.assertEquals(attLegIdealAcc2, attLegNumericalAcce2, 1E-5);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link OrientationAngleProvider#computeSpinByFD(PVCoordinatesProvider, AbsoluteDate, double)}
     *
     * @description
     *              <p>
     *              The spin Numerical computer method
     *              </p>
     *              <p>
     *              Two {@link OrientationAngleLeg} and {@link OrientationAngleLaw} are defined.
     *              Their spins are computed numerically and are compared to their analytical spin
     *              on a whole orbital period.
     *              </p>
     *
     * @testPassCriteria spins obtained are equal to the expected ideal rates (absolute tolerance:
     *                   1E-10)
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testComputeSpinByFD() throws PatriusException {

        final double computationStep = 0.1;

        // Propagator
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, 0.1, 0.2, 0.3, 0.4,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,
                Constants.WGS84_EARTH_MU);
        final Propagator pvProv = new KeplerianPropagator(initialOrbit);

        // Test interval
        final SpacecraftState scState = pvProv.getInitialState();
        final AbsoluteDate startInterval = scState.getDate();
        final double duration = scState.getKeplerianPeriod();
        final AbsoluteDate endInterval = startInterval.shiftedBy(duration);

        // Attitude law and leg for testing
        final OrientationAngleLaw attLaw = new LinearLeg(new AbsoluteDateInterval(startInterval,
                endInterval), 10., 20);
        final OrientationAngleLeg attLeg = new OrientationAngleLawLeg(attLaw, startInterval,
                endInterval);

        final double testStep = 60.0;
        AbsoluteDate currentDate = startInterval;
        while (currentDate.compareTo(endInterval) <= 0) {
            // Invoke method to be tested
            final double attLegNumericalAcce = attLeg.computeSpinByFD(pvProv, currentDate,
                    computationStep);
            final double attLawNumericalAcc = attLaw.computeSpinByFD(pvProv, currentDate,
                    computationStep);

            // Compute expected values
            final double attLegIdealAcc = 10;
            final double attLawIdealAcc = 10;

            // Assert results
            Assert.assertEquals(attLegIdealAcc, attLegNumericalAcce, 1E-12);
            Assert.assertEquals(attLawIdealAcc, attLawNumericalAcc, 1E-12);

            // Advance date
            currentDate = currentDate.shiftedBy(testStep);
        }

        // Invoke method to be tested at the end date of the leg interval
        final double attLegNumericalAccEnd = attLeg.computeSpinByFD(pvProv, endInterval,
                computationStep);
        final double attLegIdealAccEnd = 10;

        // Assert result
        Assert.assertEquals(attLegIdealAccEnd, attLegNumericalAccEnd, 1E-12);

        // Degenerated case: leg interval shorter than FD step
        final OrientationAngleLaw attLaw2 = new LinearLeg(new AbsoluteDateInterval(startInterval,
                startInterval.shiftedBy(1E-2)), 10., 20);
        final OrientationAngleLeg attLeg2 = new OrientationAngleLawLeg(attLaw2, startInterval,
                startInterval.shiftedBy(1E-2));
        final AbsoluteDate date = startInterval.shiftedBy(0.5E-2);
        final double attLegNumericalAcce2 = attLeg2.computeSpinByFD(pvProv, date, computationStep);
        final double attLegIdealAcc2 = 10;
        Assert.assertEquals(attLegIdealAcc2, attLegNumericalAcce2, 1E-12);
    }
}
