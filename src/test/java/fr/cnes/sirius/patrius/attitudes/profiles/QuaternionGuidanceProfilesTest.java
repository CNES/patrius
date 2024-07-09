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
* VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
* VERSION:4.7:DM:DM-2914:18/05/2021:Ajout d'un attribut reducedTimes à la classe QuaternionPolynomialSegment
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* VERSION:4.6:DM:DM-2565:27/01/2021:[PATRIUS] Modification de QuaternionPolynomialProfile pour pouvoir definir la nature du profil 
* VERSION:4.6:DM:DM-2656:27/01/2021:[PATRIUS] delTa parametrable utilise pour le calcul de vitesse dans QuaternionPolynomialProfile
* VERSION:4.6:FA:FA-2655:27/01/2021:[PATRIUS] Anomalie dans la classe estimateRateFunction de la classe AbtractOrientationFunction
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierSeries;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialsUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Unit tests for the guidance profiles
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public class QuaternionGuidanceProfilesTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle guidance profiles.
         * 
         * @featureDescription tests the guidance profiles.
         * 
         * @coveredRequirements DV-ATT_410, DV-ATT_420, DV-ATT_450
         * 
         */
        GUIDANCE_PROFILES,

        /**
         * @featureTitle quaternion guidance profile, calculated with Fourier series.
         * 
         * @featureDescription tests the quaternion harmonic guidance profile.
         * 
         * @coveredRequirements DV-ATT_430
         * 
         */
        QUATERNION_HARMONIC_GUIDANCE_PROFILE,

        /**
         * @featureTitle quaternion guidance profile, calculated with polynomials.
         * 
         * @featureDescription tests the quaternion polynomial guidance profile.
         * 
         * @coveredRequirements DV-ATT_430
         * 
         */
        QUATERNION_POLYNOMIAL_GUIDANCE_PROFILE
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     * 
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#QUATERNION_HARMONIC_GUIDANCE_PROFILE}
     * 
     * @testedMethod {@link QuaternionHarmonicProfile#QuaternionHarmonicProfile(AbsoluteDate, Frame, FourierSeries, FourierSeries, FourierSeries, FourierSeries, AbsoluteDateInterval)}
     * @testedMethod {@link QuaternionHarmonicProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link QuaternionHarmonicProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * @testedMethod {@link QuaternionHarmonicProfile#getQ0FourierSeries()}
     * @testedMethod {@link QuaternionHarmonicProfile#getQ1FourierSeries()}
     * @testedMethod {@link QuaternionHarmonicProfile#getQ2FourierSeries()}
     * @testedMethod {@link QuaternionHarmonicProfile#getQ3FourierSeries()}
     * 
     * @description tests the quaternion harmonic guidance profile methods for coverage
     * 
     * @input methods inputs
     * 
     * @output methods outputs
     * 
     * @testPassCriteria the outputs are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testQuaternionHarmonicProfile() throws PatriusException {

        final AbsoluteDate start = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate end = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        // q0 quaternion component as a UnivariateFunction:

        final double omega0 = 1;
        final double[] a0 = new double[] { 1, 2, 3, 4 };
        final double[] b0 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q0fs = new FourierSeries(omega0, 1, a0, b0);
        final double omega1 = 5;
        final double[] a1 = new double[] { 1, 2, 3, 4 };
        final double[] b1 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q1fs = new FourierSeries(omega1, 1, a1, b1);
        final double omega2 = 2;
        final double[] a2 = new double[] { 1, 2, 3, 4 };
        final double[] b2 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q2fs = new FourierSeries(omega2, 1, a2, b2);
        final double omega3 = 2.5;
        final double[] a3 = new double[] { 1, 2, 3, 4 };
        final double[] b3 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q3fs = new FourierSeries(omega3, 1, a3, b3);

        // Time interval of validity
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start, end,
            IntervalEndpointType.CLOSED);

        // build guidance profile
        final QuaternionHarmonicProfile profile = new QuaternionHarmonicProfile(start, FramesFactory.getGCRF(), q0fs,
            q1fs, q2fs, q3fs, interval);

        // test the checkDate method:
        boolean rez = false;
        try {
            profile.checkDate(start.shiftedBy(-2.5));
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        try {
            profile.setSpinDerivativesComputation(true);
            Assert.fail();
        } catch (final PatriusRuntimeException e) {
            Assert.assertTrue(true);
        }

        // test the getters:
        Assert.assertEquals(q0fs, profile.getQ0FourierSeries());
        Assert.assertEquals(q1fs, profile.getQ1FourierSeries());
        Assert.assertEquals(q2fs, profile.getQ2FourierSeries());
        Assert.assertEquals(q3fs, profile.getQ3FourierSeries());

        // test the getAttitude method:
        final AbsoluteDate testDate = start.shiftedBy(10.0);
        final Rotation rotation = profile.getAttitude(null, testDate, FramesFactory.getGCRF()).getRotation();
        Assert.assertEquals(q0fs.value(10.0), rotation.getQi()[0], 0.0);
        Assert.assertEquals(q1fs.value(10.0), rotation.getQi()[1], 0.0);
        Assert.assertEquals(q2fs.value(10.0), rotation.getQi()[2], 0.0);
        Assert.assertEquals(q3fs.value(10.0), rotation.getQi()[3], 0.0);

        // test the getter of the angular frequencies:
        Assert.assertEquals(1., profile.getAngularFrequencies()[0]);
        Assert.assertEquals(5., profile.getAngularFrequencies()[1]);
        Assert.assertEquals(2., profile.getAngularFrequencies()[2]);
        Assert.assertEquals(2.5, profile.getAngularFrequencies()[3]);
        // test the getter of the a0 coefficients:
        Assert.assertEquals(1., profile.getConstants()[0]);
        Assert.assertEquals(1., profile.getConstants()[1]);
        Assert.assertEquals(1., profile.getConstants()[2]);
        Assert.assertEquals(1., profile.getConstants()[3]);
        // test the getter of the cosinus coefficients:
        Assert.assertEquals(3., profile.getCosArrays()[0][2]);
        Assert.assertEquals(4., profile.getCosArrays()[1][3]);
        Assert.assertEquals(1., profile.getCosArrays()[2][0]);
        Assert.assertEquals(2., profile.getCosArrays()[3][1]);
        // test the getter of the sinus coefficients:
        Assert.assertEquals(2., profile.getSinArrays()[0][2]);
        Assert.assertEquals(1., profile.getSinArrays()[1][3]);
        Assert.assertEquals(4., profile.getSinArrays()[2][0]);
        Assert.assertEquals(3., profile.getSinArrays()[3][1]);

        Assert.assertEquals("QUATERNION_HARMONIC_PROFILE", profile.getNature());
    }
    
    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link QuaternionHarmonicProfile#copy(AbsoluteDateInterval)}
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
    public void testQuaternionHarmonicProfileCopyMethod() throws PatriusException {

        final AbsoluteDate start = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate end = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        // q0 quaternion component as a UnivariateFunction:

        final double omega0 = 1;
        final double[] a0 = new double[] { 1, 2, 3, 4 };
        final double[] b0 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q0fs = new FourierSeries(omega0, 1, a0, b0);
        final double omega1 = 5;
        final double[] a1 = new double[] { 1, 2, 3, 4 };
        final double[] b1 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q1fs = new FourierSeries(omega1, 1, a1, b1);
        final double omega2 = 2;
        final double[] a2 = new double[] { 1, 2, 3, 4 };
        final double[] b2 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q2fs = new FourierSeries(omega2, 1, a2, b2);
        final double omega3 = 2.5;
        final double[] a3 = new double[] { 1, 2, 3, 4 };
        final double[] b3 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q3fs = new FourierSeries(omega3, 1, a3, b3);

        // Time interval of validity
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start, end,
            IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start.shiftedBy(5), end.shiftedBy(-5),
                IntervalEndpointType.CLOSED);

        // build guidance profile
        QuaternionHarmonicProfile profile = new QuaternionHarmonicProfile(start, FramesFactory.getGCRF(), q0fs,
            q1fs, q2fs, q3fs, interval);

        final Attitude attitudeRef = profile.getAttitude(null, start.shiftedBy(5), FramesFactory.getGCRF());

        // Test case n°1 : in a standard usage, the interval stored should be updated
        profile = profile.copy(newIntervalOfValidity);
        Assert.assertTrue(profile.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = profile.getAttitude(null, start.shiftedBy(5), FramesFactory.getGCRF());
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(0, attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), 0);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link QuaternionHarmonicProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description tests the quaternion harmonic guidance profile spin computation
     * 
     * @testPassCriteria spin takes into account provided timestep
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testQuaternionHarmonicProfileSpin() throws PatriusException {

        // Initialize guidance profile
        final AbsoluteDate start = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate end = AbsoluteDate.J2000_EPOCH.shiftedBy(50);

        // q0 quaternion component as a UnivariateFunction:
        final double omega0 = 1;
        final double[] a0 = new double[] { 1, 2, 3, 4 };
        final double[] b0 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q0fs = new FourierSeries(omega0, 1, a0, b0);
        final double omega1 = 5;
        final double[] a1 = new double[] { 1, 2, 3, 4 };
        final double[] b1 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q1fs = new FourierSeries(omega1, 1, a1, b1);
        final double omega2 = 2;
        final double[] a2 = new double[] { 1, 2, 3, 4 };
        final double[] b2 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q2fs = new FourierSeries(omega2, 1, a2, b2);
        final double omega3 = 2.5;
        final double[] a3 = new double[] { 1, 2, 3, 4 };
        final double[] b3 = new double[] { 4, 3, 2, 1 };
        final FourierSeries q3fs = new FourierSeries(omega3, 1, a3, b3);

        // Time interval of validity
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start, end,
            IntervalEndpointType.CLOSED);

        // build guidance profile
        final QuaternionHarmonicProfile refProfile = new QuaternionHarmonicProfile(start, FramesFactory.getGCRF(), q0fs,
            q1fs, q2fs, q3fs, interval);
        final QuaternionHarmonicProfile profile1 = new QuaternionHarmonicProfile(start, FramesFactory.getGCRF(), q0fs,
                q1fs, q2fs, q3fs, interval, 0.2);
        final QuaternionHarmonicProfile profile2 = new QuaternionHarmonicProfile(start, FramesFactory.getGCRF(), q0fs,
                q1fs, q2fs, q3fs, interval, 1.);

        // Build orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), start.shiftedBy(10.), Constants.EGM96_EARTH_MU);
        
        // Check result is independent of spin delta-t (constant rotation)
        final Vector3D refSpin = refProfile.getAttitude(orbit).getSpin();
        final Vector3D actSpin1 = profile1.getAttitude(orbit).getSpin();
        final Vector3D actSpin2 = profile2.getAttitude(orbit).getSpin();
        Assert.assertTrue(refSpin.distance(actSpin1) == 0);
        Assert.assertTrue(refSpin.distance(actSpin2) == 0);
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     * 
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#QUATERNION_POLYNOMIAL_GUIDANCE_PROFILE}
     * 
     * @testedMethod {@link QuaternionPolynomialProfile#QuaternionPolynomialProfile(Frame, AbsoluteDateInterval,
     *               List<QuaternionPolynomialSegment>)}
     * @testedMethod {@link QuaternionPolynomialProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link QuaternionPolynomialProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * @testedMethod {@link QuaternionPolynomialProfile#getQ0Coefficients()}
     * @testedMethod {@link QuaternionPolynomialProfile#getQ1Coefficients()}
     * @testedMethod {@link QuaternionPolynomialProfile#getQ2Coefficients()}
     * @testedMethod {@link QuaternionPolynomialProfile#getQ3Coefficients()}
     * @testedMethod {@link QuaternionPolynomialProfile#copy(AbsoluteDateInterval)}
     * 
     * @description tests the quaternion polynomial guidance profile methods for coverage
     * 
     * @input methods inputs
     * 
     * @output methods outputs
     * 
     * @testPassCriteria the outputs are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testQuaternionPolynomialProfile() throws PatriusException {

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(100);

        final PolynomialFunction q0pf1 = PolynomialsUtils.createLegendrePolynomial(4);
        final PolynomialFunction q1pf1 = PolynomialsUtils.createLegendrePolynomial(4);
        final PolynomialFunction q2pf1 = PolynomialsUtils.createLegendrePolynomial(4);
        final PolynomialFunction q3pf1 = PolynomialsUtils.createLegendrePolynomial(4);

        // create the first segment:
        final AbsoluteDateInterval interval1 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date1,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment1 = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1,
            date0, interval1);

        // check the exception is thrown when asking the orientation at an invalid date:
        boolean rez = false;
        try {
            segment1.getOrientation(date0.shiftedBy(-10.));
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        final double[] x = { 50, 60, 70, 80, 90, 100 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm q0pf2 = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm q1pf2 = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm q2pf2 = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] y3 = { 11, 12, 2, 9, 1, 1 };
        final PolynomialFunctionLagrangeForm q3pf2 = new PolynomialFunctionLagrangeForm(x, y3);

        // create the second segment:
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1, date2,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment2 = new QuaternionPolynomialSegment(q0pf2, q1pf2, q2pf2, q3pf2,
            date0, interval2);

        // create the list of segments:
        final List<QuaternionPolynomialSegment> segments = new ArrayList<QuaternionPolynomialSegment>();
        segments.add(segment1);
        segments.add(segment2);
        // create the guidance profile:
        final QuaternionPolynomialProfile profile = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
            interval1.mergeTo(interval2), segments, "My nature");
        
        // Check nature
        Assert.assertEquals("My nature", profile.getNature());

        // check the exception is thrown when asking the orientation at an invalid date:
        rez = false;
        try {
            profile.getAttitude(null, date0.shiftedBy(500.), FramesFactory.getGCRF()).getRotation();
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // tests the coefficients getters:
        Assert.assertEquals(q0pf1.getCoefficients()[0], profile.getQ0Coefficients().get(interval1)[0]);
        Assert.assertEquals(q1pf1.getCoefficients()[1], profile.getQ1Coefficients().get(interval1)[1]);
        Assert.assertEquals(q2pf1.getCoefficients()[2], profile.getQ2Coefficients().get(interval1)[2]);
        Assert.assertEquals(q3pf1.getCoefficients()[3], profile.getQ3Coefficients().get(interval1)[3]);

        Assert.assertEquals(q0pf2.getCoefficients()[3], profile.getQ0Coefficients().get(interval2)[3]);
        Assert.assertEquals(q1pf2.getCoefficients()[2], profile.getQ1Coefficients().get(interval2)[2]);
        Assert.assertEquals(q2pf2.getCoefficients()[1], profile.getQ2Coefficients().get(interval2)[1]);
        Assert.assertEquals(q3pf2.getCoefficients()[0], profile.getQ3Coefficients().get(interval2)[0]);

        final AbsoluteDate testDate = date0.shiftedBy(0.1);
        final Rotation actual = profile.getAttitude(null, testDate, FramesFactory.getGCRF()).getRotation();
        final Rotation expected = segment1.getOrientation(testDate);
        Assert.assertEquals(expected.getAngle(), actual.getAngle(), 0.0);

        Assert.assertEquals("QUATERNION_POLYNOMIAL_PROFILE", new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
                interval1.mergeTo(interval2), segments, 0.2).getNature());
    }
    
    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link QuaternionPolynomialProfile#copy(AbsoluteDateInterval)}
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
    public void testQuaternionPolynomialProfileCopyMethod() throws PatriusException {

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(100);

        final PolynomialFunction q0pf1 = PolynomialsUtils.createLegendrePolynomial(4);
        final PolynomialFunction q1pf1 = PolynomialsUtils.createLegendrePolynomial(4);
        final PolynomialFunction q2pf1 = PolynomialsUtils.createLegendrePolynomial(4);
        final PolynomialFunction q3pf1 = PolynomialsUtils.createLegendrePolynomial(4);

        // create the first segment:
        final AbsoluteDateInterval interval1 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date1,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment1 = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1,
            date0, interval1);

        final double[] x = { 50, 60, 70, 80, 90, 100 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm q0pf2 = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm q1pf2 = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm q2pf2 = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] y3 = { 11, 12, 2, 9, 1, 1 };
        final PolynomialFunctionLagrangeForm q3pf2 = new PolynomialFunctionLagrangeForm(x, y3);

        // create the second segment:
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1, date2,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment2 = new QuaternionPolynomialSegment(q0pf2, q1pf2, q2pf2, q3pf2,
            date0, interval2);

        // create the list of segments:
        final List<QuaternionPolynomialSegment> segments = new ArrayList<QuaternionPolynomialSegment>();
        segments.add(segment1);
        segments.add(segment2);
        // create the guidance profile:
        QuaternionPolynomialProfile profile = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
            interval1.mergeTo(interval2), segments, "My nature");
        profile.setSpinDerivativesComputation(true);

        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0.shiftedBy(5), date1.shiftedBy(10),
                IntervalEndpointType.CLOSED);

        final Attitude attitudeRef = profile.getAttitude(null, date0.shiftedBy(5), FramesFactory.getGCRF());

        // Test case n°1 : in a standard usage, the interval stored should be updated
        profile = profile.copy(newIntervalOfValidity);
        Assert.assertTrue(profile.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = profile.getAttitude(null, date0.shiftedBy(5), FramesFactory.getGCRF());
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(0, attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), 0);
    }

    /**
     * FA-1440
     * @testType UT
     * 
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#QUATERNION_POLYNOMIAL_GUIDANCE_PROFILE}
     * 
     * @testedMethod {@link QuaternionPolynomialProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description checks that the profile spin can be computed on the boundaries (performed by finite differences)
     * 
     * @input methods inputs
     * 
     * @output methods outputs
     * 
     * @testPassCriteria no exception is thrown, output is the expected one (reference: math)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public final void testQuaternionPolynomialProfileBoundary() throws PatriusException {

        // Build a random profile on interval [t0, t0 + 50s]
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);

        final double[] q0 = new Rotation(true, new double[] { 1, 1, 1, 1}).getQi();
        final double[] q1 = new Rotation(true, new double[] { 3, 4, 1, 1}).getQi();
        final double[] q2 = new Rotation(true, new double[] { 6, 3, 1, 1}).getQi();
        final double[] q3 = new Rotation(true, new double[] { 2, 2, 1, 1}).getQi();
        final double[] q4 = new Rotation(true, new double[] { 2, 2, 1, 1}).getQi();
        final double[] q5 = new Rotation(true, new double[] { 2, 2, 1, 1}).getQi();
        
        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { q0[0], q1[0], q2[0], q3[0], q4[0], q5[0] };
        final PolynomialFunctionLagrangeForm q0pf1 = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] y1 = { q0[1], q1[1], q2[1], q3[1], q4[1], q5[1] };
        final PolynomialFunctionLagrangeForm q1pf1 = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] y2 = { q0[2], q1[2], q2[2], q3[2], q4[2], q5[2] };
        final PolynomialFunctionLagrangeForm q2pf1 = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] y3 = { q0[3], q1[3], q2[3], q3[3], q4[3], q5[3] };
        final PolynomialFunctionLagrangeForm q3pf1 = new PolynomialFunctionLagrangeForm(x, y3);

        // create the first segment:
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date1,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1,
            date0, interval);

        final List<QuaternionPolynomialSegment> segments = new ArrayList<QuaternionPolynomialSegment>();
        segments.add(segment);
        final QuaternionPolynomialProfile profile = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
            interval, segments);

        // Check (left side) - Should be close enough to linear interpolation from 2 previous points
        final double spin0 = profile.getAttitude(null, date0, FramesFactory.getGCRF()).getSpin().getNorm();
        final double spin0p1dt = profile.getAttitude(null, date0.shiftedBy(0.2), FramesFactory.getGCRF()).getSpin().getNorm();
        final double spin0p2dt = profile.getAttitude(null, date0.shiftedBy(0.4), FramesFactory.getGCRF()).getSpin().getNorm();
        Assert.assertEquals(spin0p1dt + (spin0p1dt - spin0p2dt), spin0, 1E-2);
        // Check (right side) - Should be close enough to linear interpolation from 2 previous points
        final double spin1 = profile.getAttitude(null, date1, FramesFactory.getGCRF()).getSpin().getNorm();
        final double spin1m1dt = profile.getAttitude(null, date1.shiftedBy(-0.2), FramesFactory.getGCRF()).getSpin().getNorm();
        final double spin1m2dt = profile.getAttitude(null, date1.shiftedBy(-0.4), FramesFactory.getGCRF()).getSpin().getNorm();
        Assert.assertEquals(spin1m1dt + (spin1m1dt - spin1m2dt), spin1, 1E-2);
        
        // Check particular case of a small interval
        final AbsoluteDate date1bis = AbsoluteDate.J2000_EPOCH.shiftedBy(0.1);
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date1bis,
                IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment2 = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1, date0,
                interval2);
        final List<QuaternionPolynomialSegment> segmentsList = new ArrayList<QuaternionPolynomialSegment>();
        segmentsList.add(segment2);
        final QuaternionPolynomialProfile profile2 = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
                interval2, segmentsList);
        try {
            profile2.getAttitude(null, date0.shiftedBy(0.05), FramesFactory.getGCRF()).getSpin().getNorm();
            Assert.assertTrue(true);
        } catch (final Exception e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link QuaternionPolynomialProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description tests the quaternion polynomial guidance profile spin computation
     * 
     * @testPassCriteria spin takes into account provided timestep
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testQuaternionPolynomialProfileSpin() throws PatriusException {

        // Initialize guidance profile
        final AbsoluteDate start = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate end = AbsoluteDate.J2000_EPOCH.shiftedBy(50);

        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 0.1E-2, 0.3E-2, 0.6E-2, 0.2E-2, 0.2E-2, 0.2E-2 };
        final PolynomialFunctionLagrangeForm q0pf1 = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] y1 = { 0.1E-2, 0.4E-2, 0.3E-2, 0.2E-2, 0.2E-2, 0.2E-2 };
        final PolynomialFunctionLagrangeForm q1pf1 = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] y2 = { 0.1E-2, 0.1E-2, 0.1E-2, 0.1E-2, 0.1E-2, 0.1E-2 };
        final PolynomialFunctionLagrangeForm q2pf1 = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] y3 = { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1 };
        final PolynomialFunctionLagrangeForm q3pf1 = new PolynomialFunctionLagrangeForm(x, y3);

        // create the first segment:
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start, end,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1,
            start, interval);

        final List<QuaternionPolynomialSegment> segments = new ArrayList<QuaternionPolynomialSegment>();
        segments.add(segment);

        // build guidance profile
        final QuaternionPolynomialProfile refProfile = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
                interval, segments);
        final QuaternionPolynomialProfile profile1 = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
                interval, segments, 0.2);
        final QuaternionPolynomialProfile profile2 = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
                interval, segments, 1.0);

        // Build orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), start.shiftedBy(10.), Constants.EGM96_EARTH_MU);
        
        // Check spin delta-t is properly taken into account
        final Vector3D refSpin = refProfile.getAttitude(orbit).getSpin();
        final Vector3D actSpin1 = profile1.getAttitude(orbit).getSpin();
        final Vector3D actSpin2 = profile2.getAttitude(orbit).getSpin();
        Assert.assertTrue(refSpin.distance(actSpin1) == 0);
        Assert.assertFalse(refSpin.distance(actSpin2) == 0);
    }

    /**
     * Check that rotation acceleration of {@link QuaternionPolynomialProfile} is properly computed.
     */
    @Test
    public void testQuaternionPolynomialProfileRotationAcceleration() throws PatriusException {

        // Initialize guidance profile
        final AbsoluteDate start = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate end = AbsoluteDate.J2000_EPOCH.shiftedBy(50);

        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 0.1E-2, 0.3E-2, 0.6E-2, 0.2E-2, 0.2E-2, 0.2E-2 };
        final PolynomialFunctionLagrangeForm q0pf1 = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] y1 = { 0.1E-2, 0.4E-2, 0.3E-2, 0.2E-2, 0.2E-2, 0.2E-2 };
        final PolynomialFunctionLagrangeForm q1pf1 = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] y2 = { 0.1E-2, 0.1E-2, 0.1E-2, 0.1E-2, 0.1E-2, 0.1E-2 };
        final PolynomialFunctionLagrangeForm q2pf1 = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] y3 = { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1 };
        final PolynomialFunctionLagrangeForm q3pf1 = new PolynomialFunctionLagrangeForm(x, y3);

        // create the first segment:
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start, end,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1,
            start, interval);

        final List<QuaternionPolynomialSegment> segments = new ArrayList<QuaternionPolynomialSegment>();
        segments.add(segment);

        // build guidance profile
        final QuaternionPolynomialProfile profile = new QuaternionPolynomialProfile(FramesFactory.getGCRF(),
                interval, segments);
        profile.setSpinDerivativesComputation(true);

        // Build orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), start.shiftedBy(25.), Constants.EGM96_EARTH_MU);

        // Actual
        final Vector3D actual = profile.getAttitude(orbit).getRotationAcceleration();
        // Expected
        final Vector3D spin1 = profile.getAttitude(orbit.shiftedBy(-0.1)).getSpin();
        final Vector3D spin2 = profile.getAttitude(orbit.shiftedBy(0.1)).getSpin();
        final Vector3D expected = spin2.subtract(spin1).scalarMultiply(1. / 0.2);
        
        // Check rotation acceleration
        Assert.assertEquals(0., expected.distance(actual), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedMethod {@link QuaternionPolynomialSegment#getOrientation()}
     * 
     * @description tests the reduced time feature of QuaternionPolynomialSegment: check that a segment with reduced time
     * and dilated coefficients returns the same result as a segment with standard time and standard coefficients
     * 
     * @testPassCriteria the outputs are the expected one
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public final void testQuaternionPolynomialSegmentReducedTime() {

        // Build segments
        final double dt = 50;
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(20);

        final double[] y0 = { 1, 2, 3 };
        final PolynomialFunction q0pf1 = new PolynomialFunction(y0);
        final double[] y1 = { 2, 3, 4 };
        final PolynomialFunction q1pf1 = new PolynomialFunction(y1);
        final double[] y2 = { 3, 4, 5 };
        final PolynomialFunction q2pf1 = new PolynomialFunction(y2);
        final double[] y3 = { 4, 5, 6 };
        final PolynomialFunction q3pf1 = new PolynomialFunction(y3);

        final double[] y0r = { 1, 2 * dt, 3 * dt * dt };
        final PolynomialFunction q0pf1r = new PolynomialFunction(y0r);
        final double[] y1r = { 2, 3 * dt, 4 * dt * dt };
        final PolynomialFunction q1pf1r = new PolynomialFunction(y1r);
        final double[] y2r = { 3, 4 * dt, 5 * dt * dt };
        final PolynomialFunction q2pf1r = new PolynomialFunction(y2r);
        final double[] y3r = { 4, 5 * dt, 6 * dt * dt };
        final PolynomialFunction q3pf1r = new PolynomialFunction(y3r);

        // create the first segment:
        final AbsoluteDateInterval interval1 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date1,
            IntervalEndpointType.CLOSED);
        final QuaternionPolynomialSegment segment = new QuaternionPolynomialSegment(q0pf1, q1pf1, q2pf1, q3pf1,
            date0, interval1);
        final QuaternionPolynomialSegment segmentReduced = new QuaternionPolynomialSegment(q0pf1r, q1pf1r, q2pf1r, q3pf1r,
                interval1);

        // Check data on segments
        final Rotation rotation = segment.getOrientation(date);
        final Rotation rotationReduced = segmentReduced.getOrientation(date);
        Assert.assertEquals(Rotation.distance(rotation, rotationReduced), 0, 0);
    }
}
