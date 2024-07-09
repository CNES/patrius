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
 * HISTORY
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

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.FramesFactory;
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
        Report.printClassHeader(OrientationAngleProviderTest.class.getSimpleName(), "Orientation angle provider");
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
    public final void testConstantOrientationAngleLaw() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testConstantOrientationAngleLaw", "Orientation angle law - Angle computation",
            "Math", 1E-14, ComparisonType.ABSOLUTE);

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
    public final void testConstantOrientationAngleLeg() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testConstantOrientationAngleLeg", "Orientation angle leg - Angle computation",
            "Math", 1E-14, ComparisonType.ABSOLUTE);

        final double angle = 2.2;
        final AbsoluteDateInterval interval =
            new AbsoluteDateInterval(AbsoluteDate.JAVA_EPOCH, AbsoluteDate.J2000_EPOCH);
        final OrientationAngleLeg provider = new ConstantOrientationAngleLeg(interval, angle);

        // Computation and check
        final double actual = provider.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(1));
        final double expected = angle;
        Report.printToReport("Angle", expected, actual);
        Assert.assertEquals(expected, actual, 1E-14);

        // Check time interval
        Assert.assertEquals(0, provider.getTimeInterval().getLowerData().durationFrom(AbsoluteDate.JAVA_EPOCH), 0);
        Assert.assertEquals(0, provider.getTimeInterval().getUpperData().durationFrom(AbsoluteDate.J2000_EPOCH), 0);

        // Check nature
        Assert.assertEquals("CONSTANT", provider.getNature());

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
    public final void testOrientationAngleLawLeg() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testOrientationAngleLawLeg", "Orientation angle law leg - Angle computation", "Math",
            1E-14, ComparisonType.ABSOLUTE);

        final double angle = 2.2;
        final AbsoluteDateInterval interval =
            new AbsoluteDateInterval(AbsoluteDate.JAVA_EPOCH, AbsoluteDate.J2000_EPOCH);
        final OrientationAngleLaw orientationLaw = new ConstantOrientationAngleLaw(angle);
        final OrientationAngleLeg provider =
            new OrientationAngleLawLeg(orientationLaw, interval.getLowerData(), interval.getUpperData());

        // Computation and check
        final double actual = provider.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(1));
        final double expected = angle;
        Report.printToReport("Angle", expected, actual);
        Assert.assertEquals(expected, actual, 1E-14);

        // Check time interval
        Assert.assertEquals(0, provider.getTimeInterval().getLowerData().durationFrom(AbsoluteDate.JAVA_EPOCH), 0);
        Assert.assertEquals(0, provider.getTimeInterval().getUpperData().durationFrom(AbsoluteDate.J2000_EPOCH), 0);

        // Check nature
        Assert.assertEquals("ATTITUDE_ORIENTATION_ANGLE_LAW_LEG", provider.getNature());

        // Check exception (date outside interval)
        try {
            provider.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(-1));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        
        // Check no exception if time tolerant
        final OrientationAngleLeg provider2 =
                new OrientationAngleLawLeg(orientationLaw, interval.getLowerData(), interval.getUpperData(), "", true);
        Assert.assertEquals(expected, provider2.getOrientationAngle(null, AbsoluteDate.JAVA_EPOCH.shiftedBy(-1)), 1E-14);
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
    public final void testOrientationAngleLegsSequence() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testOrientationAngleLegsSequence",
            "Orientation angle legs sequence - Angle computation", "Math", 1E-14, ComparisonType.ABSOLUTE);

        // Build profile
        final OrientationAngleLegsSequence provider = new OrientationAngleLegsSequence("Nature");
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

        // Check nature
        Assert.assertEquals("Nature", provider.getNature());

        // Check exception (date outside interval)
        Assert.assertNull(provider.getOrientationAngle(null, date1.shiftedBy(-1)));
        Assert.assertNull(provider.getOrientationAngle(null, date4.shiftedBy(1)));
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
    public final void testOrientationAngleProfileSequenceLeg() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testOrientationAngleProfileSequenceLeg",
            "Orientation angle profile sequence - Angle computation", "Math", 1E-14, ComparisonType.ABSOLUTE);

        // Build profile
        final OrientationAngleProfileSequence provider = new OrientationAngleProfileSequence("Nature");
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

        // Check nature
        Assert.assertEquals("Nature", provider.getNature());

        // Check exception (date outside interval)
        Assert.assertNull(provider.getOrientationAngle(null, date1.shiftedBy(-1)));
        Assert.assertNull(provider.getOrientationAngle(null, date4.shiftedBy(1)));
    }

    /**
     * Linear leg: orientation = a.(t - t0) + b.
     */
    private class LinearLeg extends AbstractOrientationAngleProfile implements OrientationAngleLaw {

        /** Serial UID. */
        private static final long serialVersionUID = 1L;

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
        public LinearLeg(final AbsoluteDateInterval timeInterval,
            final double a, final double b) {
            super(timeInterval);
            this.a = a;
            this.b = b;
        }

        @Override
        public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
                                          final AbsoluteDate date) throws PatriusException {
            return this.a * date.durationFrom(this.getTimeInterval().getLowerData()) + this.b;
        }

        @Override
        public LinearLeg copy(final AbsoluteDateInterval newInterval) {
            return new LinearLeg(newInterval, a, b + (newInterval.getLowerData().durationFrom(getTimeInterval().getLowerData())) * a);
        }
    }

    /**
     * Quadratic leg: orientation = a.(t - t0)² + b.(t - t0) + c.
     */
    private class QuadraticLeg extends AbstractOrientationAngleProfile implements OrientationAngleLaw {

        /** Serial UID. */
        private static final long serialVersionUID = 1L;

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
        public QuadraticLeg(final AbsoluteDateInterval timeInterval,
            final double a, final double b, final double c) {
            super(timeInterval);
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
                                          final AbsoluteDate date) throws PatriusException {
            final double dt = date.durationFrom(this.getTimeInterval().getLowerData());
            return this.a * dt * dt + this.b * dt + this.c;
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
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        ConstantOrientationAngleLeg provider1 = new ConstantOrientationAngleLeg(interval, angle);
        ConstantOrientationAngleLeg provider2 = new ConstantOrientationAngleLeg(interval, angle);

        final double angleRef = provider1.getOrientationAngle(null, startDate.shiftedBy(5));

        // Test case n°1 : in a standard usage, the interval stored should be updated
        provider1 = provider1.copy(newIntervalOfValidity);
        Assert.assertTrue(provider1.getTimeInterval().equals(newIntervalOfValidity));
        Assert.assertEquals(angleRef, provider1.getOrientationAngle(null, startDate.shiftedBy(5)));

        // Test case n°2 : when the new interval isn't included, the interval stored should be updated
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
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        OrientationAngleLawLeg provider1 = new OrientationAngleLawLeg(new ConstantOrientationAngleLaw(10), interval.getLowerData(), interval.getUpperData());
        OrientationAngleLawLeg provider2 = new OrientationAngleLawLeg(new ConstantOrientationAngleLaw(10), interval.getLowerData(), interval.getUpperData());

        final double angleRef = provider1.getOrientationAngle(null, startDate.shiftedBy(5));

        // Test case n°1 : in a standard usage, the interval stored should be updated
        provider1 = provider1.copy(newIntervalOfValidity);
        Assert.assertTrue(provider1.getTimeInterval().equals(newIntervalOfValidity));
        Assert.assertEquals(angleRef, provider1.getOrientationAngle(null, startDate.shiftedBy(5)));

        // Test case n°2 : when the new interval isn't included, the interval stored should be updated
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
        final double offset = 5;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(55), startDate.shiftedBy(70), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), startDate.shiftedBy(+1000), IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        OrientationAngleProfileSequence provider1 = new OrientationAngleProfileSequence();
        provider1.add(new LinearLeg(new AbsoluteDateInterval(interval.getLowerData(), interval.getUpperData()), 0., 2.5));
        provider1.add(new LinearLeg(new AbsoluteDateInterval(interval2.getLowerData(), interval2.getUpperData()), 1., 2.5));
        final OrientationAngleLegsSequence provider2 = new OrientationAngleLegsSequence();
        provider2.add(new LinearLeg(new AbsoluteDateInterval(interval.getLowerData(), interval.getUpperData()), 0., 2.5));
        provider2.add(new LinearLeg(new AbsoluteDateInterval(interval2.getLowerData(), interval2.getUpperData()), 1., 2.5));

        final double angleRef = provider1.getOrientationAngle(null, startDate.shiftedBy(70));

        // Test case n°1 : in a standard usage, the interval stored should be updated
        provider1 = provider1.copy(newIntervalOfValidity);
        Assert.assertTrue(provider1.getTimeInterval().equals(newIntervalOfValidity));
        Assert.assertEquals(angleRef, provider1.getOrientationAngle(null, startDate.shiftedBy(70)));
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
     *              Two {@link OrientationAngleLeg} and {@link OrientationAngleLaw} are defined. Their spin derivative are computed numerically and are compared to
     *              their analytical spin derivative on a whole orbital period.
     *              </p>
     *
     * @testPassCriteria spin derivative obtained are equal to the expected ideal spin derivative (absolute tolerance: 1E-12)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testComputeSpinDerivativeByFD() throws PatriusException {

        final double computationStep = 0.1;
        
        // Propagator
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, 0.1, 0.2, 0.3, 0.4, PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
        final Propagator pvProv = new KeplerianPropagator(initialOrbit);

        // Test interval
        final SpacecraftState scState = pvProv.getInitialState();
        final AbsoluteDate startInterval = scState.getDate();
        final double duration = scState.getKeplerianPeriod();
        final AbsoluteDate endInterval = startInterval.shiftedBy(duration);

        // Attitude law and leg for testing
        final OrientationAngleLaw attLaw = new QuadraticLeg(new AbsoluteDateInterval(startInterval, endInterval), 10., 20., 30.);
        final OrientationAngleLeg attLeg = new OrientationAngleLawLeg(attLaw, startInterval, endInterval);

        final double testStep = 60.0;
        AbsoluteDate currentDate = startInterval;
        while (currentDate.compareTo(endInterval) <= 0) {
            // Invoke method to be tested
            final double attLegNumericalAcce =
                    attLeg.computeSpinDerivativeByFD(pvProv, currentDate, computationStep);
            final double attLawNumericalAcc =
                    attLaw.computeSpinDerivativeByFD(pvProv, currentDate, computationStep);

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
        final double attLegNumericalAccEnd = attLeg.computeSpinDerivativeByFD(pvProv, endInterval, computationStep);
        final double attLegIdealAccEnd = 20;

        // Assert result
        Assert.assertEquals(attLegIdealAccEnd, attLegNumericalAccEnd, 1E-4);
        
        // Degenerated case: leg interval shorter than FD step
        final OrientationAngleLaw attLaw2 = new QuadraticLeg(new AbsoluteDateInterval(startInterval, startInterval.shiftedBy(1E-2)), 10., 20, 30.);
        final OrientationAngleLeg attLeg2 = new OrientationAngleLawLeg(attLaw2, startInterval, startInterval.shiftedBy(1E-2));
        final AbsoluteDate date = startInterval.shiftedBy(0.5E-2);
        final double attLegNumericalAcce2 =
                attLeg2.computeSpinDerivativeByFD(pvProv, date, computationStep);
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
     *              Two {@link OrientationAngleLeg} and {@link OrientationAngleLaw} are defined. Their spins are computed numerically and are compared to
     *              their analytical spin on a whole orbital period.
     *              </p>
     *
     * @testPassCriteria spins obtained are equal to the expected ideal rates (absolute tolerance: 1E-10)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testComputeSpinByFD() throws PatriusException {

        final double computationStep = 0.1;
        
        // Propagator
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, 0.1, 0.2, 0.3, 0.4, PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
        final Propagator pvProv = new KeplerianPropagator(initialOrbit);

        // Test interval
        final SpacecraftState scState = pvProv.getInitialState();
        final AbsoluteDate startInterval = scState.getDate();
        final double duration = scState.getKeplerianPeriod();
        final AbsoluteDate endInterval = startInterval.shiftedBy(duration);

        // Attitude law and leg for testing
        final OrientationAngleLaw attLaw = new LinearLeg(new AbsoluteDateInterval(startInterval, endInterval), 10., 20);
        final OrientationAngleLeg attLeg = new OrientationAngleLawLeg(attLaw, startInterval, endInterval);

        final double testStep = 60.0;
        AbsoluteDate currentDate = startInterval;
        while (currentDate.compareTo(endInterval) <= 0) {
            // Invoke method to be tested
            final double attLegNumericalAcce =
                    attLeg.computeSpinByFD(pvProv, currentDate, computationStep);
            final double attLawNumericalAcc =
                    attLaw.computeSpinByFD(pvProv, currentDate, computationStep);

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
        final double attLegNumericalAccEnd =
                attLeg.computeSpinByFD(pvProv, endInterval, computationStep);
        final double attLegIdealAccEnd = 10;

        // Assert result
        Assert.assertEquals(attLegIdealAccEnd, attLegNumericalAccEnd, 1E-12);
        
        // Degenerated case: leg interval shorter than FD step
        final OrientationAngleLaw attLaw2 = new LinearLeg(new AbsoluteDateInterval(startInterval, startInterval.shiftedBy(1E-2)), 10., 20);
        final OrientationAngleLeg attLeg2 = new OrientationAngleLawLeg(attLaw2, startInterval, startInterval.shiftedBy(1E-2));
        final AbsoluteDate date = startInterval.shiftedBy(0.5E-2);
        final double attLegNumericalAcce2 =
                attLeg2.computeSpinByFD(pvProv, date, computationStep);
        final double attLegIdealAcc2 = 10;
        Assert.assertEquals(attLegIdealAcc2, attLegNumericalAcce2, 1E-12);
    }
}
