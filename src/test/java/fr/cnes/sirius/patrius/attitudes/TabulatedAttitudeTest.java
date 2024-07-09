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
 * @history creation 15/02/2012
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:96:05/08/2013:updated to match the attitude legs sequence with codes
 * VERSION::FA:218:17/03/2014:updated to test the spin computation
 * VERSION::DM:282:22/07/2014:added TU for getEphemeris method
 * VERSION::FA:367:04/12/2014:Recette V2.3 corrections (changed getAttitudes return type)
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::DM:455:05/11/2015:Improved accuracy and performance of TabulatedAttitude class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::FA:1771:20/10/2018:correction round-off error
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.slew.ConstantSpinSlewComputer;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularDerivativesFilter;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for the tabulated attitude law.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: TabulatedAttitudeTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class TabulatedAttitudeTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle tabulated attitude creation
         * 
         * @featureDescription creation of a tabulated attitude, test of its time interval and
         *                     change of the time interval, test of attitudes list getting
         * 
         * @coveredRequirements DV-ATT_230, DV-ATT_260
         */
        CREATION,

        /**
         * @featureTitle tabulated attitude computation
         * 
         * @featureDescription creation of a tabulated attitude, and test
         *                     of the returned attitude at several dates.
         * 
         * @coveredRequirements DV-ATT_260
         */
        ATTITUDE_COMPUTATION;
    }

    /** Used epsilon for double comparison */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "Tabulated attitude provider");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CREATION}
     * 
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(java.util.Collection, int)}
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(java.util.Collection)}
     * @testedMethod {@link TabulatedAttitude#setTimeInterval(fr.cnes.sirius.patrius.time.AbsoluteDateInterval)}
     * @testedMethod {@link TabulatedAttitude#getDurationTab()}
     * 
     * @description constructor test, attitude liste getting test, time interval setting test
     * 
     * @input Attitudes with different dates
     * 
     * @output a class instance
     * 
     * @testPassCriteria the time interval of the output law is the one expected (implies
     *                   that the attitude list has been correctly created and ordered)
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     *         if fails
     */
    @Test
    public final void tabulatedAttitudeCreationNewConstructorTest() throws PatriusException {

        // attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.0);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.0);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.0);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(40.0);
        final AbsoluteDate date5 = AbsoluteDate.J2000_EPOCH.shiftedBy(-10.0);
        final AbsoluteDate date6 = AbsoluteDate.J2000_EPOCH.shiftedBy(15.0);
        final AbsoluteDate date7 = AbsoluteDate.J2000_EPOCH.shiftedBy(35.0);

        final Attitude attitude0 = new Attitude(date0, FramesFactory.getEME2000(),
            rot1, Vector3D.ZERO);
        final Attitude attitude1 = new Attitude(date1, FramesFactory.getEME2000(),
            rot2, Vector3D.ZERO);
        final Attitude attitude2 = new Attitude(date2, FramesFactory.getEME2000(),
            rot1, Vector3D.ZERO);
        final Attitude attitude3 = new Attitude(date3, FramesFactory.getEME2000(),
            rot2, Vector3D.ZERO);
        final Attitude attitude4 = new Attitude(date4, FramesFactory.getEME2000(),
            rot1, Vector3D.ZERO);

        // input list creation
        List<Attitude> attList = new ArrayList<Attitude>();
        attList.add(attitude0);
        attList.add(attitude1);
        attList.add(attitude2);
        attList.add(attitude3);
        attList.add(attitude4);

        // attitude leg creation
        final TabulatedAttitude attLeg = new TabulatedAttitude(attList, 4);
        final TabulatedAttitude attLegWithDefauktNbrInter = new TabulatedAttitude(attList);

        // Test durationTab
        final double[] durationTab = attLeg.getDurations();
        Assert.assertEquals(durationTab[0], 0, 0);
        Assert.assertEquals(durationTab[1], 10, 0);
        Assert.assertEquals(durationTab[2], 20, 0);
        Assert.assertEquals(durationTab[3], 30, 0);
        Assert.assertEquals(durationTab[4], 40, 0);

        // test of the date interval
        AbsoluteDateInterval datesInterval = attLeg.getTimeInterval();

        AbsoluteDate lowDate = datesInterval.getLowerData();
        AbsoluteDate upDate = datesInterval.getUpperData();

        Assert.assertEquals(lowDate.compareTo(date0), 0);
        Assert.assertEquals(upDate.compareTo(date4), 0);

        // test of attitudes list getting
        final Attitude resultAtt = attLeg.getAttitudes().get(2);
        final Rotation resultRot = resultAtt.getRotation();
        final Rotation expectedRot = attitude2.getRotation();
        final double nullAngle = resultRot.applyInverseTo(expectedRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, Precision.DOUBLE_COMPARISON_EPSILON);

        final Attitude resultAtt2 = attLegWithDefauktNbrInter.getAttitudes().get(2);
        final Rotation resultRot2 = resultAtt2.getRotation();
        final double nullAngle2 = resultRot2.applyInverseTo(expectedRot).getAngle();
        Assert.assertEquals(nullAngle2, 0.0, Precision.DOUBLE_COMPARISON_EPSILON);

        // setInterval test
        datesInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            date5, date3, IntervalEndpointType.CLOSED);

        try {
            attLeg.setTimeInterval(datesInterval);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        datesInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            date6, date7, IntervalEndpointType.CLOSED);

        try {
            final TabulatedAttitude attLeg2 = attLeg.setTimeInterval(datesInterval);
            datesInterval = attLeg2.getTimeInterval();
            lowDate = datesInterval.getLowerData();
            upDate = datesInterval.getUpperData();

            Assert.assertEquals(lowDate.compareTo(date6), 0);
            Assert.assertEquals(upDate.compareTo(date7), 0);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // with only one attitude test
        try {
            attList = new ArrayList<Attitude>();
            attList.add(attitude2);
            new TabulatedAttitude(attList, 5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // try creation with nbInterpolationPoints > attList.length
        try {
            attList.add(attitude2);
            new TabulatedAttitude(attList, 5);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_COMPUTATION}
     * 
     * @testedMethod {@link TabulatedAttitude#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * @testedMethod {@link TabulatedAttitude#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame, int)}
     * 
     * @description computation test
     * 
     * @input A TabulatedAttitude and a date. The date is : between two attitude's date of the list,
     *        right on one (first, middle or last), out of the time interval
     * 
     * @output Attitude
     * 
     * @testPassCriteria the computed attitude at a given date is the one expected : equal
     *                   to the result of the right slerp interpolation. Throws an IllegalArgumentException if
     *                   the date is out of the time interval
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     */
    @Test
    public final void attitudeComputationNewConstTest() throws PatriusException {

        Report.printMethodHeader("attitudeComputationNewConstTest", "Rotation computation", "Math",
            this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.0);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.0);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.0);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(40.0);
        final AbsoluteDate dateTest1 = AbsoluteDate.J2000_EPOCH.shiftedBy(7.0);
        final AbsoluteDate dateTest2 = AbsoluteDate.J2000_EPOCH.shiftedBy(13.5);
        final AbsoluteDate dateOut = AbsoluteDate.J2000_EPOCH.shiftedBy(50.0);

        final Attitude attitude0 = new Attitude(date0, FramesFactory.getEME2000(),
            rot1, Vector3D.ZERO);
        final Attitude attitude1 = new Attitude(date1, FramesFactory.getEME2000(),
            rot2, Vector3D.ZERO);
        final Attitude attitude2 = new Attitude(date2, FramesFactory.getEME2000(),
            rot1, Vector3D.ZERO);
        final Attitude attitude3 = new Attitude(date3, FramesFactory.getEME2000(),
            rot2, Vector3D.ZERO);
        final Attitude attitude4 = new Attitude(date4, FramesFactory.getEME2000(),
            rot2, Vector3D.ZERO);

        // input list creation
        final List<Attitude> attList = new ArrayList<Attitude>();
        attList.add(attitude0);
        attList.add(attitude1);
        attList.add(attitude2);
        attList.add(attitude3);
        attList.add(attitude4);

        // attitude leg creation
        AttitudeLeg attLeg = new TabulatedAttitude(attList, 2);

        // test between the two first attitudes
        // =====================================
        Attitude resultAtt = attLeg.getAttitude(null, dateTest1, FramesFactory.getEME2000());
        Rotation resultRot = resultAtt.getRotation();

        // Expected rotation
        List<TimeStampedAngularCoordinates> attListAR = new ArrayList<TimeStampedAngularCoordinates>();
        attListAR.add(new TimeStampedAngularCoordinates(date0, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date1, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        Rotation expectedRot = TimeStampedAngularCoordinates.interpolate(dateTest1,
            AngularDerivativesFilter.USE_RR, attListAR).getRotation();

        // comparison
        double nullAngle = expectedRot.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);
        Report.printToReport("Rotation", expectedRot, resultRot);

        // test between the 2 and 3 attitudes
        // =====================================

        resultAtt = attLeg.getAttitude(null, dateTest2, FramesFactory.getEME2000());
        resultRot = resultAtt.getRotation();

        // Expected rotation
        attListAR = new ArrayList<TimeStampedAngularCoordinates>();
        attListAR.add(new TimeStampedAngularCoordinates(date1, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date2, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        expectedRot = TimeStampedAngularCoordinates.interpolate(dateTest2,
            AngularDerivativesFilter.USE_RR, attListAR).getRotation();

        // comparison
        nullAngle = expectedRot.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test right on the last attitude
        // ==================================

        resultAtt = attLeg.getAttitude(null, date4, FramesFactory.getEME2000());
        resultRot = resultAtt.getRotation();

        // comparison
        nullAngle = rot2.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test right on the first attitude
        // ==================================

        resultAtt = attLeg.getAttitude(null, date0, FramesFactory.getEME2000());
        resultRot = resultAtt.getRotation();

        // comparison
        nullAngle = rot1.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test with 3 attitudes to hermite
        // ================================

        // attitude leg creation
        attLeg = new TabulatedAttitude(attList, 3);
        resultAtt = attLeg.getAttitude(null, dateTest2, FramesFactory.getEME2000());
        resultRot = resultAtt.getRotation();

        // Expected rotation
        attListAR = new ArrayList<TimeStampedAngularCoordinates>();
        attListAR.add(new TimeStampedAngularCoordinates(date0, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date1, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date2, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        expectedRot = TimeStampedAngularCoordinates.interpolate(dateTest2,
            AngularDerivativesFilter.USE_RR, attListAR).getRotation();

        // test with 4 attitudes to hermite
        // ================================

        // attitude leg creation
        attLeg = new TabulatedAttitude(attList, 4);
        resultAtt = attLeg.getAttitude(null, dateTest2, FramesFactory.getEME2000());
        resultRot = resultAtt.getRotation();

        // Expected rotation
        attListAR = new ArrayList<TimeStampedAngularCoordinates>();
        attListAR.add(new TimeStampedAngularCoordinates(date0, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date1, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date2, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date3, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        expectedRot = TimeStampedAngularCoordinates.interpolate(dateTest2,
            AngularDerivativesFilter.USE_RR, attListAR).getRotation();

        // comparison
        nullAngle = expectedRot.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // interpOrder = attitude.length
        // ==================================
        attLeg = new TabulatedAttitude(attList, 5);
        resultAtt = attLeg.getAttitude(null, dateTest2, FramesFactory.getEME2000());
        resultRot = resultAtt.getRotation();

        // Expected rotation
        attListAR = new ArrayList<TimeStampedAngularCoordinates>();
        attListAR.add(new TimeStampedAngularCoordinates(date0, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date1, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date2, rot1,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date3, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        attListAR.add(new TimeStampedAngularCoordinates(date4, rot2,
            Vector3D.ZERO, Vector3D.ZERO));
        expectedRot = TimeStampedAngularCoordinates.interpolate(dateTest2,
            AngularDerivativesFilter.USE_RR, attListAR).getRotation();

        // comparison
        nullAngle = expectedRot.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test right out of the time interval
        // ==================================
        try {
            resultAtt = attLeg.getAttitude(null, dateOut, FramesFactory.getEME2000());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // interpOrder > attitue.length
        // ==================================
        try {
            attLeg = new TabulatedAttitude(attList, 6);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_COMPUTATION}
     * 
     * @testedMethod {@link TabulatedAttitude#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * @testedMethod {@link TabulatedAttitude#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame, int)}
     * 
     * @description interpolation precision test
     * 
     * @input 2 seconds sampled attitudes
     * 
     * @output 1 second sampled attitudes
     * 
     * @testPassCriteria The interpolated attitudes must be close enough compared with propagated attitudes
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     */
    @Test
    public final void interpolPrecisionTest() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003");

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loaderSun);
        new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH);

        Assert.assertTrue(true);

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final AbsoluteDate endDate = initDate.shiftedBy(3 * 60 * 60);
        final Orbit initialOrbit = new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5, MathLib.toRadians(1.0),
            FastMath.PI / 4.0, 0., PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);

        // Attitude en Pointage Terre
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(itrf);
        // Attitude en Pointage Soleil
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AttitudeLaw sunPointingAtt = new CelestialBodyPointed(gcrf, sun, Vector3D.PLUS_K, Vector3D.MINUS_K,
            Vector3D.PLUS_I);

        // Date of eclipse IN and OUT
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate eclipseOut1 = new AbsoluteDate("2008-01-01T00:29:26.709", tai);
        final double durationSlew1 = 300;
        final AbsoluteDate eclipseIn1 = new AbsoluteDate("2008-01-01T01:26:11.289", tai);
        final double durationSlew2 = 300;
        final AbsoluteDate eclipseOut2 = new AbsoluteDate("2008-01-01T02:00:25.179", tai);
        final double durationSlew3 = 300;
        final AbsoluteDate eclipseIn2 = new AbsoluteDate("2008-01-01T02:57:10.161", tai);
        final double durationSlew4 = 120;

        // création de l'instance de séquence d'attitude
        // création d'un propagateur képlérien spécifique à la séquence
        final KeplerianPropagator sequenceProvider = new KeplerianPropagator(initialOrbit);
        final StrictAttitudeLegsSequence attSequence = new StrictAttitudeLegsSequence<AttitudeLeg>();

        final AttitudeLeg earthPointingAtt1 = new AttitudeLawLeg(earthPointingAtt, initDate, eclipseOut1);
        final ConstantSpinSlew cstSpinSlew1 = new ConstantSpinSlew(earthPointingAtt.getAttitude(sequenceProvider,
            earthPointingAtt1
                .getTimeInterval().getUpperData(), gcrf), sunPointingAtt.getAttitude(sequenceProvider,
            earthPointingAtt1
                .getTimeInterval().getUpperData().shiftedBy(durationSlew1), gcrf));
        final AttitudeLeg sunPointingAtt1 = new AttitudeLawLeg(sunPointingAtt, cstSpinSlew1.getTimeInterval()
            .getUpperData(), eclipseIn1);
        final ConstantSpinSlew cstSpinSlew2 = new ConstantSpinSlew(sunPointingAtt.getAttitude(sequenceProvider,
            sunPointingAtt1
                .getTimeInterval().getUpperData(), gcrf), earthPointingAtt.getAttitude(sequenceProvider,
            sunPointingAtt1
                .getTimeInterval().getUpperData().shiftedBy(durationSlew2), gcrf));
        final AttitudeLeg earthPointingAtt2 = new AttitudeLawLeg(earthPointingAtt, cstSpinSlew2.getTimeInterval()
            .getUpperData(), eclipseOut2);
        final ConstantSpinSlew cstSpinSlew3 = new ConstantSpinSlew(earthPointingAtt.getAttitude(sequenceProvider,
            earthPointingAtt2
                .getTimeInterval().getUpperData(), gcrf), sunPointingAtt.getAttitude(sequenceProvider,
            earthPointingAtt2
                .getTimeInterval().getUpperData().shiftedBy(durationSlew3), gcrf));
        final AttitudeLeg sunPointingAtt2 = new AttitudeLawLeg(sunPointingAtt, cstSpinSlew3.getTimeInterval()
            .getUpperData(), eclipseIn2);
        final ConstantSpinSlew cstSpinSlew4 = new ConstantSpinSlew(sunPointingAtt.getAttitude(sequenceProvider,
            sunPointingAtt2
                .getTimeInterval().getUpperData(), gcrf), earthPointingAtt.getAttitude(sequenceProvider,
            sunPointingAtt2
                .getTimeInterval().getUpperData().shiftedBy(durationSlew4), gcrf));
        final AttitudeLeg earthPointingAtt3 = new AttitudeLawLeg(earthPointingAtt, cstSpinSlew4.getTimeInterval()
            .getUpperData(), endDate);

        attSequence.add(earthPointingAtt1);
        attSequence.add(cstSpinSlew1);
        attSequence.add(sunPointingAtt1);
        attSequence.add(cstSpinSlew2);
        attSequence.add(earthPointingAtt2);
        attSequence.add(cstSpinSlew3);
        attSequence.add(sunPointingAtt2);
        attSequence.add(cstSpinSlew4);
        attSequence.add(earthPointingAtt3);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, attSequence);
        // ephemeris mode
        propagator.setEphemerisMode();

        // set up samples
        final int duration = 3 * 60 * 60;
        final double freqSample = 2.;
        final List<Attitude> sample = new ArrayList<Attitude>();
        for (double dt = 0; dt < duration + 1; dt += freqSample) {
            sample.add(propagator.propagate(initDate.shiftedBy(dt)).getAttitude());
        }

        // check angular error between porpagated and interpolated
        final int interpOrder = 4;
        final TabulatedAttitude tab = new TabulatedAttitude(sample, interpOrder);
        final double[] listAngularError = new double[duration];
        int count = 0;
        TimeStampedAngularCoordinates lastRot;
        TimeStampedAngularCoordinates nextRot;
        for (double dt = 1; dt < duration; dt += 1.) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Attitude propagated = propagator.propagate(t).getAttitude();
            lastRot = new TimeStampedAngularCoordinates(tab.getAttitudes().get((int) (dt / freqSample)).getDate(),
                tab.getAttitudes().get((int) (dt / freqSample)).getRotation(), Vector3D.ZERO, Vector3D.ZERO);
            nextRot = new TimeStampedAngularCoordinates(tab.getAttitudes().get((int) (dt / freqSample) + 1).getDate(),
                tab.getAttitudes().get((int) (dt / freqSample) + 1).getRotation(), Vector3D.ZERO, Vector3D.ZERO);
            final ArrayList<TimeStampedAngularCoordinates> attForSlerp = new ArrayList<TimeStampedAngularCoordinates>();
            attForSlerp.add(lastRot);
            attForSlerp.add(nextRot);
            final Rotation rotSlerp = TimeStampedAngularCoordinates.interpolate(t, AngularDerivativesFilter.USE_R,
                attForSlerp).getRotation();
            Rotation.distance(propagated.getRotation(), rotSlerp);
            listAngularError[count] = Rotation.distance(propagated.getRotation(), tab.getAttitude(null, t, gcrf)
                .getRotation());
            count++;
        }
        Assert.assertEquals(0.0, StatUtils.mean(listAngularError), 1e-5);

    }

    // Test for rotation acceleration : rotation acceleration computed with Hermite interpolation and
    // rotation acceleration computed with finite differences (using spin) must be close
    @Test
    public final void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            1e-11, ComparisonType.ABSOLUTE);

        Utils.setDataRoot("regular-dataCNES-2003");

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate initDate = new AbsoluteDate(2004, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5, MathLib.toRadians(1.0),
            FastMath.PI / 4.0, 0., PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);

        // Attitude en Pointage Terre
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(itrf);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthPointingAtt);
        // ephemeris mode
        propagator.setEphemerisMode();

        // set up samples
        final int duration = 1600;
        final double freqSample = 2.;
        final List<Attitude> sample = new ArrayList<Attitude>();
        for (double dt = 0; dt < duration; dt += freqSample) {
            sample.add(propagator.propagate(initDate.shiftedBy(dt)).getAttitude());
        }
        final TabulatedAttitude tab = new TabulatedAttitude(sample, 2);

        // Check spin derivatives computation if not performed
        Assert.assertNull(tab.getAttitude(initialOrbit).getRotationAcceleration());

        // Check spin derivatives computation
        tab.setSpinDerivativesComputation(true);

        // Compare acceleration obtained with interpolation and finite differences acceleration
        final Frame frameToCompute = gcrf;
        for (double i = 0.1; i < duration - 2; i += 1) {
            final Vector3D acc = tab.getAttitude(initialOrbit, initDate.shiftedBy(i),
                frameToCompute).getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(tab, null, frameToCompute, initDate.shiftedBy(i))
                .nthDerivative(1).getVector3D(initDate.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-11);
            if (i == 0.1) {
                Report.printToReport("Rotation acceleration", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        tab.setSpinDerivativesComputation(false);
        Assert.assertNull(tab.getAttitude(initialOrbit).getRotationAcceleration());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_COMPUTATION}
     * 
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(java.util.List, int)}
     * @testedMethod {@link TabulatedAttitude#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description test the coherence between the attitude ephemeris generator and the tabulated attitude methods
     * 
     * @input An AttitudeLegsSequence
     * 
     * @output Attitude
     * 
     * @testPassCriteria the computed attitude at a given date (a transition date of the ephemeris sequence) is the
     *                   expected one (the first law since binary search is open-closed)
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     */
    @Test
    public final void tabulatedAttitudeFromEphemeris() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true)); // set the attitude laws sequence:
        final Frame j2000 = FramesFactory.getEME2000();
        final Frame gcrf = FramesFactory.getGCRF();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8, 1e-9 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final NumericalPropagator propagator = new NumericalPropagator(new DormandPrince853Integrator(0.1, 500, absTOL,
            relTOL));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        final AbsoluteDate t0 = new AbsoluteDate(2012, 03, 01, 11, 12, 10.2, TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, gcrf, t0, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState initalState = new SpacecraftState(orbit);
        propagator.resetInitialState(initalState);
        final Rotation rot_inertialLaw1 = Rotation.IDENTITY;
        final Rotation rot_inertialLaw2 = new Rotation(false, 0.5, 0.5, 0.5, 0.5);

        final AttitudeLaw inertialLaw1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), rot_inertialLaw1);
        final AttitudeLaw inertialLaw2 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), rot_inertialLaw2);
        final AbsoluteDate t1 = t0.shiftedBy(50);
        final AttitudeLawLeg law1 = new AttitudeLawLeg(inertialLaw1, t0, t1);
        final Slew rdv = new ConstantSpinSlewComputer((FastMath.PI * 2 / 3) / 30.0).compute(propagator, inertialLaw1,
            t1, inertialLaw2, null);
        final AbsoluteDate t2 = rdv.getTimeInterval().getUpperData();
        final AbsoluteDate t3 = t2.shiftedBy(50);
        final AttitudeLawLeg law2 = new AttitudeLawLeg(inertialLaw2, t2, t3);
        final StrictAttitudeLegsSequence seq = new StrictAttitudeLegsSequence<AttitudeLeg>();
        seq.add(law1);
        seq.add(rdv);
        seq.add(law2);

        // set the attitude ephemeris generator:
        final VariableStepAttitudeEphemerisGenerator generator = new VariableStepAttitudeEphemerisGenerator(seq, 5.0,
            1.0, 0.02, 2, orbit);
        // set the tabulated ephemeris:
        final SortedSet<Attitude> ephemeris = generator.generateEphemeris(j2000);
        final List<Attitude> ephemerisList = new ArrayList<Attitude>();
        ephemerisList.addAll(ephemeris);
        final AttitudeLeg ephemLeg = new TabulatedAttitude(ephemerisList, -1);
        // check the attitude at the transition date t1 (should be the first law since binary search is open-closed)
        Assert.assertEquals(law1.getAttitude(orbit, t1, j2000).getSpin(), ephemLeg.getAttitude(orbit, t1, j2000)
            .getSpin());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_COMPUTATION}
     * 
     * @testedMethod {@link TabulatedAttitude#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * 
     * @description test the getAttitude method when spin and spin derivatives are computed
     * 
     * @input Two attitudes with spin and spin derivatives
     * 
     * @output An interpolated attitude with spin and spin derivatives
     * 
     * @testPassCriteria the computed attitude at a given date is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     */
    @Test
    public final void tabulatedAttitudeWithSpinAndSpinDerivatives() throws PatriusException {
        // attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.0);
        final Vector3D spin1 = new Vector3D(0.5, 0.08, -0.5);
        final Vector3D spin2 = new Vector3D(1.5, 0.02, 0.0);

        final Vector3D spinDer1Att1 = new Vector3D(0., 1., 2.);
        final Vector3D spinDer1Att2 = new Vector3D(2., 3., 4.);

        final Attitude attitude1 = new Attitude(FramesFactory.getEME2000(), new TimeStampedAngularCoordinates(date0,
            rot1, spin1, spinDer1Att1));
        final Attitude attitude2 = new Attitude(FramesFactory.getEME2000(), new TimeStampedAngularCoordinates(date1,
            rot2, spin2, spinDer1Att2));

        // input list creation
        final List<Attitude> attList = new ArrayList<Attitude>();
        attList.add(attitude1);
        attList.add(attitude2);

        // attitude leg creation
        final TabulatedAttitude attLeg = new TabulatedAttitude(attList, -1);
        attLeg.setSpinDerivativesComputation(true);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.0);
        // check the spin:
        Assert.assertEquals(1.0, attLeg.getAttitude(null, date, FramesFactory.getEME2000()).getSpin().getX(), 0.0);
        Assert.assertEquals(0.05, attLeg.getAttitude(null, date, FramesFactory.getEME2000()).getSpin().getY(), 0.0);
        Assert.assertEquals(-0.25, attLeg.getAttitude(null, date, FramesFactory.getEME2000()).getSpin().getZ(), 0.0);
        // check the spin first derivative:
        Assert.assertEquals(1., attLeg.getAttitude(null, date, FramesFactory.getEME2000()).getRotationAcceleration()
            .getX(), 0.0);
        Assert.assertEquals(2., attLeg.getAttitude(null, date, FramesFactory.getEME2000()).getRotationAcceleration()
            .getY(), 0.0);
        Assert.assertEquals(3., attLeg.getAttitude(null, date, FramesFactory.getEME2000()).getRotationAcceleration()
            .getZ(), 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_COMPUTATION}
     * 
     * @testedMethod {@link TabulatedAttitude#getAttitude(PVCoordinatesProvider)}
     * 
     * @description test the getAttitude method when spin and spin derivatives are computed
     * 
     * @input Two attitudes with spin and spin derivatives
     * 
     * @output An interpolated attitude with spin and spin derivatives
     * 
     * @testPassCriteria the computed attitude at a given date is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     */
    @Test
    public final void testGetAttitude() throws PatriusException {

        // attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.0);

        final Attitude attitude0 = new Attitude(date0, FramesFactory.getEME2000(),
            rot1, Vector3D.ZERO);
        final Attitude attitude1 = new Attitude(date1, FramesFactory.getEME2000(),
            rot2, Vector3D.ZERO);

        // input list creation
        final List<Attitude> attList = new ArrayList<Attitude>();
        attList.add(attitude0);
        attList.add(attitude1);

        // attitude leg creation
        final TabulatedAttitude attLeg = new TabulatedAttitude(attList, 2);
        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, FramesFactory.getGCRF(), date0, Constants.EIGEN5C_EARTH_MU);

        final Attitude att = attLeg.getAttitude(orbit);
        final Attitude att2 = attLeg.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        Assert.assertEquals(att.getDate(), att2.getDate());
        Assert.assertEquals(att.getReferenceFrame(), att2.getReferenceFrame());
        Assert.assertTrue(att.getRotation().isEqualTo(att2.getRotation()));
        Assert.assertEquals(0, att.getSpin().getNorm(), att2.getSpin().getNorm());
    }

    /**
     * FA-1771.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_COMPUTATION}
     * 
     * @testedMethod {@link TabulatedAttitude#getAttitude(PVCoordinatesProvider)}
     * 
     * @description check that attitude is properly retrieved with 0-order Hermite interpolation at date
     *              close enough of tabulated date to create round-off errors.
     * 
     * @input one date date1, another date = date1 + 1E-14s
     * 
     * @output attitudes
     * 
     * @testPassCriteria attitudes are as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testRoundOffError() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final Frame refFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final List<Attitude> listAtt = new ArrayList<Attitude>();

        // Initialize 4 attitudes
        final AbsoluteDate date1 = new AbsoluteDate(2040, 10, 04, 23, 57, 15, utc);
        final Rotation rot1 = new Rotation(true, 0.803149987403, 0.099781899985, -0.140521068331, 0.570304742682);
        final TimeStampedAngularCoordinates ang1 =
            new TimeStampedAngularCoordinates(date1, rot1, Vector3D.ZERO, Vector3D.ZERO);

        final AbsoluteDate date2 = new AbsoluteDate(2040, 10, 04, 23, 59, 12, utc);
        final Rotation rot2 = new Rotation(true, 0.901620991411, -0.008738264377, 0.018235224236, 0.432054055854);
        final TimeStampedAngularCoordinates ang2 =
            new TimeStampedAngularCoordinates(date2, rot2, Vector3D.ZERO, Vector3D.ZERO);

        final AbsoluteDate date3 = new AbsoluteDate(2040, 10, 04, 23, 59, 25, utc);
        final Rotation rot3 = new Rotation(true, 0.900696831353, 0.020011758469, -0.041725711170, 0.431976518507);
        final TimeStampedAngularCoordinates ang3 =
            new TimeStampedAngularCoordinates(date3, rot3, Vector3D.ZERO, Vector3D.ZERO);

        final AbsoluteDate date4 = new AbsoluteDate(2040, 10, 04, 23, 59, 38, utc);
        final Rotation rot4 = new Rotation(true, 0.895757238542, 0.048353839344, -0.100661062850, 0.430288538364);
        final TimeStampedAngularCoordinates ang4 =
            new TimeStampedAngularCoordinates(date4, rot4, Vector3D.ZERO, Vector3D.ZERO);

        // Build Tabulated attitude with 0-order interpolator
        listAtt.add(new Attitude(refFrame, ang1));
        listAtt.add(new Attitude(refFrame, ang2));
        listAtt.add(new Attitude(refFrame, ang3));
        listAtt.add(new Attitude(refFrame, ang4));
        final TabulatedAttitude tabAtt = new TabulatedAttitude(listAtt, 1);

        // Check attitude at date exactly on tabulated point
        final Attitude attitude1 = tabAtt.getAttitude(null, date3, refFrame);
        Assert.assertTrue(attitude1.getRotation().isEqualTo(rot3));

        // Check attitude very close (on the right) to tabulated point
        final Attitude attitude2 = tabAtt.getAttitude(null, date3.shiftedBy(1e-14), refFrame);
        Assert.assertTrue(attitude2.getRotation().isEqualTo(rot3));
    }

    /**
     * Local function to provide spin function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @param tab
     *        tab
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final TabulatedAttitude tab, final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return tab.getAttitude(pvProv, date, frame).getSpin();
            }
        };
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(List)}
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(List, int)}
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(List, String)}
     * @testedMethod {@link TabulatedAttitude#TabulatedAttitude(List, int, String)}
     * @testedMethod {@link TabulatedAttitude#getNature()}
     * 
     * @description Test the new constructors which add the "nature" attribute
     * 
     * @input parameters
     * 
     * @output slew
     * 
     * @testPassCriteria The nature attribute is well managed
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void FT2105() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final Frame refFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Initialize 2 attitudes
        final AbsoluteDate date1 = new AbsoluteDate(2040, 10, 04, 23, 57, 15, utc);
        final Rotation rot1 = new Rotation(true, 0.803149987403, 0.099781899985, -0.140521068331,
            0.570304742682);
        final TimeStampedAngularCoordinates ang1 = new TimeStampedAngularCoordinates(date1, rot1,
            Vector3D.ZERO, Vector3D.ZERO);

        final AbsoluteDate date2 = new AbsoluteDate(2040, 10, 04, 23, 59, 12, utc);
        final Rotation rot2 = new Rotation(true, 0.901620991411, -0.008738264377, 0.018235224236,
            0.432054055854);
        final TimeStampedAngularCoordinates ang2 = new TimeStampedAngularCoordinates(date2, rot2,
            Vector3D.ZERO, Vector3D.ZERO);

        // Build Tabulated attitude with 0-order interpolator
        final List<Attitude> listAtt = new ArrayList<Attitude>();
        listAtt.add(new Attitude(refFrame, ang1));
        listAtt.add(new Attitude(refFrame, ang2));

        final String DEFAULT_NATURE = "TABULATED_ATTITUDE";
        final String nature = "testNature";

        // Test all the 4 constructors
        final TabulatedAttitude tabAtt1 = new TabulatedAttitude(listAtt);
        final TabulatedAttitude tabAtt2 = new TabulatedAttitude(listAtt, 1);
        final TabulatedAttitude tabAtt3 = new TabulatedAttitude(listAtt, nature);
        final TabulatedAttitude tabAtt4 = new TabulatedAttitude(listAtt, 1, nature);

        Assert.assertEquals(tabAtt1.getNature(), DEFAULT_NATURE);
        Assert.assertEquals(tabAtt2.getNature(), DEFAULT_NATURE);
        Assert.assertEquals(tabAtt3.getNature(), nature);
        Assert.assertEquals(tabAtt4.getNature(), nature);
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link TabulatedAttitude#setAngularDerivativesFilter(AngularDerivativesFilter)}
     * @testedMethod {@link TabulatedAttitude#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description This test put in evidence a new behavior with the tabulated attitude interpolation: now, the
     *              AngularDerivativesFilter.USE_R filter is selected, the rotation rates are still interpolated with
     *              the rotations only.
     * 
     * @testPassCriteria The tabulated attitude is well interpolated: in this context, a constant rotation rate is
     *                   expected, even at the specified dates (the attitude should be interpolated at the specified
     *                   dates too).
     */
    @Test
    public void FT2992() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        final Frame refFrame = FramesFactory.getEME2000();

        final Rotation rot0 = new Rotation(false, Math.cos(Math.toRadians(0)), Math.sin(Math.toRadians(0)), 0, 0);
        final Rotation rot1 = new Rotation(false, Math.cos(Math.toRadians(15)), Math.sin(Math.toRadians(15)), 0, 0);
        final Rotation rot2 = new Rotation(false, Math.cos(Math.toRadians(30)), Math.sin(Math.toRadians(30)), 0, 0);
        final Rotation rot3 = new Rotation(false, Math.cos(Math.toRadians(45)), Math.sin(Math.toRadians(45)), 0, 0);
        final Rotation rot4 = new Rotation(false, Math.cos(Math.toRadians(60)), Math.sin(Math.toRadians(60)), 0, 0);

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = date0.shiftedBy(10.);
        final AbsoluteDate date2 = date0.shiftedBy(20.);
        final AbsoluteDate date3 = date0.shiftedBy(30.);
        final AbsoluteDate date4 = date0.shiftedBy(40.);

        final List<Attitude> listAtt = new ArrayList<Attitude>();
        listAtt.add(new Attitude(date0, refFrame, rot0, Vector3D.ZERO));
        listAtt.add(new Attitude(date1, refFrame, rot1, Vector3D.ZERO));
        listAtt.add(new Attitude(date2, refFrame, rot2, Vector3D.ZERO));
        listAtt.add(new Attitude(date3, refFrame, rot3, Vector3D.ZERO));
        listAtt.add(new Attitude(date4, refFrame, rot4, Vector3D.ZERO));

        final TabulatedAttitude tabAtt = new TabulatedAttitude(listAtt, 2, "tab");
        tabAtt.setAngularDerivativesFilter(AngularDerivativesFilter.USE_R);

        // Expect a constant rotation rate (even at the specified dates)
        final double expectedSpin = 0.052359877559;

        for (double i = 0.; i <= 40.; i += 0.25) {
            final Attitude interpolatedAttitude = tabAtt.getAttitude(null, date0.shiftedBy(i), refFrame);
            Assert.assertEquals(expectedSpin, interpolatedAttitude.getSpin().getNorm(), 1e-12);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link TabulatedAttitude#copy(AbsoluteDateInterval)}
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
    public void testTabulatedAttitudeCopyMethod() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final Frame refFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Initialize 2 attitudes
        final AbsoluteDate date1 = new AbsoluteDate(2040, 10, 04, 23, 57, 15, utc);
        final Rotation rot1 = new Rotation(true, 0.803149987403, 0.099781899985, -0.140521068331,
            0.570304742682);
        final TimeStampedAngularCoordinates ang1 = new TimeStampedAngularCoordinates(date1, rot1,
            Vector3D.ZERO, Vector3D.ZERO);

        final AbsoluteDate date2 = new AbsoluteDate(2040, 10, 04, 23, 59, 12, utc);
        final Rotation rot2 = new Rotation(true, 0.901620991411, -0.008738264377, 0.018235224236,
            0.432054055854);
        final TimeStampedAngularCoordinates ang2 = new TimeStampedAngularCoordinates(date2, rot2,
            Vector3D.ZERO, Vector3D.ZERO);

        // Build Tabulated attitude with 0-order interpolator
        final List<Attitude> listAtt = new ArrayList<Attitude>();
        listAtt.add(new Attitude(refFrame, ang1));
        listAtt.add(new Attitude(refFrame, ang2));

        // Constructor dates
        final AbsoluteDate startDate = listAtt.get(0).getDate();
        final AbsoluteDate endDate = listAtt.get(1).getDate();

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
        TabulatedAttitude tabAtt1 = new TabulatedAttitude(listAtt);
        tabAtt1.setAngularDerivativesFilter(AngularDerivativesFilter.USE_R); // TODO comprendre pk c'est nécessaire ?
        tabAtt1.setSpinDerivativesComputation(true);
        TabulatedAttitude tabAtt2 = new TabulatedAttitude(listAtt);
        final TabulatedAttitude tabAtt3 = new TabulatedAttitude(listAtt);

        final Attitude attitudeRef = tabAtt1.getAttitude(null, startDate.shiftedBy(5), refFrame);

        // Test case n°1 : in a standard usage, the interval stored should be updated
        tabAtt1 = tabAtt1.copy(newIntervalOfValidity);
        Assert.assertTrue(tabAtt1.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = tabAtt1.getAttitude(null, startDate.shiftedBy(5), refFrame);

        final double threshold = 1e-14;
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), threshold);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), threshold);
        Assert.assertEquals(0,
            attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), threshold);

        // Test case n°2 : if we send an opened interval, it is closed before to process the truncation
        tabAtt2 = tabAtt2.copy(newIntervalOfValidityOpen);
        Assert.assertFalse(tabAtt2.getTimeInterval().equals(newIntervalOfValidityOpen));
        Assert.assertTrue(tabAtt2.getTimeInterval().equals(newIntervalOfValidity));

        // Test case n°3 : when the new interval isn't included, the method copy should throw an exception
        try {
            tabAtt3.copy(newIntervalOfValidityNotIncluded);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link TabulatedAttitude#copy(AbsoluteDateInterval)}
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
    public void testTabulatedSlewCopyMethod() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final Frame refFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Initialize 2 attitudes
        final AbsoluteDate date1 = new AbsoluteDate(2040, 10, 04, 23, 57, 15, utc);
        final Rotation rot1 = new Rotation(true, 0.803149987403, 0.099781899985, -0.140521068331,
            0.570304742682);
        final TimeStampedAngularCoordinates ang1 = new TimeStampedAngularCoordinates(date1, rot1,
            Vector3D.ZERO, Vector3D.ZERO);

        final AbsoluteDate date2 = new AbsoluteDate(2040, 10, 04, 23, 59, 12, utc);
        final Rotation rot2 = new Rotation(true, 0.901620991411, -0.008738264377, 0.018235224236,
            0.432054055854);
        final TimeStampedAngularCoordinates ang2 = new TimeStampedAngularCoordinates(date2, rot2,
            Vector3D.ZERO, Vector3D.ZERO);

        // Build Tabulated attitude with 0-order interpolator
        final List<Attitude> listAtt = new ArrayList<Attitude>();
        listAtt.add(new Attitude(refFrame, ang1));
        listAtt.add(new Attitude(refFrame, ang2));

        // Constructor dates
        final AbsoluteDate startDate = listAtt.get(0).getDate();
        final AbsoluteDate endDate = listAtt.get(1).getDate();

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
        final TabulatedSlew tabSlew1 = new TabulatedSlew(listAtt);
        tabSlew1.setSpinDerivativesComputation(true);
        tabSlew1.setAngularDerivativesFilter(AngularDerivativesFilter.USE_R);
        final TabulatedSlew tabAtt2 = new TabulatedSlew(listAtt);

        final Attitude attitudeRef = tabSlew1.getAttitude(null, startDate.shiftedBy(5), refFrame);

        // Test case n°1 : in a standard usage, the interval stored should be updated
        final TabulatedAttitude tabAtt1 = tabSlew1.copy(newIntervalOfValidity);
        Assert.assertTrue(tabAtt1.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = tabAtt1.getAttitude(null, startDate.shiftedBy(5), refFrame);

        final double threshold = 1e-14;
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), threshold);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), threshold);
        Assert.assertEquals(0,
            attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), threshold);

        // Test case n°3 : when the new interval isn't included, the method copy should throw an exception
        try {
            tabAtt2.copy(newIntervalOfValidityNotIncluded);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
