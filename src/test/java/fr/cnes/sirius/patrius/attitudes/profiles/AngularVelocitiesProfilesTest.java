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
 * @history Created 04/04/2013
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:98:12/07/2013:Fixed wrong date parameter given to Attitude and DynamicsElements constructors
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1287:13/11/2017: Integration problem in AngularVelocitiesPolynomialProfile.java
 * VERSION::DM:1951:10/12/2018: Creation Test RotationAcceleratioProfilesTest.java
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLawLeg;
import fr.cnes.sirius.patrius.attitudes.BodyCenterGroundPointing;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.profiles.AbstractAngularVelocitiesAttitudeProfile.AngularVelocityIntegrationType;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierSeries;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialsUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Unit tests for the angular velocity polynomial guidance profile
 *
 * @author Tiziana Sabatini
 *
 * @version $Id: AngularVelocitiesProfilesTest.java 9755 2014-01-06 17:06:13Z
 *          houdroge $
 *
 * @since 1.3
 */
public class AngularVelocitiesProfilesTest {

    /** Doubles comparison */
    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle guidance profiles.
         *
         * @featureDescription tests the guidance profiles.
         *
         * @coveredRequirements DV-ATT_410, DV-ATT_420, DV-ATT_450, DV-ATT_460
         *
         */
        GUIDANCE_PROFILES,

        /**
         * @featureTitle angular velocity guidance profile, calculated with
         *               polynomials.
         *
         * @featureDescription tests the angular velocity polynomial guidance
         *                     profile.
         *
         * @coveredRequirements DV-ATT_430
         *
         */
        ANGULAR_VELOCITY_POLYNOMIAL_GUIDANCE_PROFILE,

        /**
         * @featureTitle angular velocity harmonic profile.
         *
         * @featureDescription tests the angular velocity harmonic guidance
         *                     profile.
         *
         * @coveredRequirements DV-ATT_430
         *
         */
        ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AngularVelocitiesProfilesTest.class.getSimpleName(),
            "Angular velocity profile attitude provider");
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description tests the angular velocities harmonic guidance profile
     *              methods - a Ground pointing attitude law is used
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria the outputs are the expected one : 2e-14° threshold for
     *                   the recomputed attitude, 1e-8 relative difference for the spin
     *
     * @referenceVersion 2.2
     *
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testHarmonicProfileSpinValue() throws PatriusException {

        Report.printMethodHeader("testHarmonicProfileSpinValue", "Spin computation", "Math", 1e-8,
            ComparisonType.RELATIVE);

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(5000);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10000);

        // earth
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame cirf = FramesFactory.getCIRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 0, FramesFactory.getITRF());

        // attitude law
        final BodyCenterGroundPointing law = new BodyCenterGroundPointing(earth);

        // attitude leg
        final AttitudeLawLeg leg = new AttitudeLawLeg(law, date0, date2);

        final KeplerianOrbit orbit = new KeplerianOrbit(6700000, .001, .15, 0, 0, 0,
            PositionAngle.MEAN, gcrf, date0, Constants.EGM96_EARTH_MU);
        final KeplerianPropagator prop = new KeplerianPropagator(orbit);

        final double T = orbit.getKeplerianPeriod();

        final AngularVelocitiesHarmonicProfile profile = GuidanceProfileBuilder
            .computeAngularVelocitiesHarmonicProfile(leg, prop, gcrf, date0, T, 6,
                AngularVelocityIntegrationType.WILCOX_4, 1 / 8.);

        // Attitude
        final Rotation original = law.getAttitude(prop, date1, cirf).getRotation().revert();
        final Rotation computed = profile.getAttitude(prop, date1, cirf).getRotation().revert();
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(original, computed)), 1E-4);

        // Spin
        final Vector3D originalS = leg.getAttitude(prop, date1, cirf).getSpin();
        final Vector3D computedS = profile.getAttitude(prop, date1, cirf).getSpin();
        Assert.assertEquals(0, originalS.subtract(computedS).getNorm() / originalS.getNorm(), 1e-8);

        Report.printToReport("Spin norm", originalS.getNorm(), computedS.getNorm());

        // derivatives
        try {
            profile.setSpinDerivativesComputation(true);
        } catch (final PatriusRuntimeException e) {
            final String expected = "Unable to compute spin derivative for attitude law class "
                    + "fr.cnes.sirius.patrius.guidance.AngularVelocitiesHarmonicProfile";
            Assert.assertTrue(expected.contentEquals(e.getMessage()));
        }
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description tests the angular velocities harmonic guidance profile
     *              methods for coverage - InertialProvider attitude law is used
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria the outputs are the expected one : 1e-14° threshold for
     *                   the recomputed attitude, 1e-15°/s for the spin (spin is
     *                   zero)
     *
     * @referenceVersion 2.2
     *
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testHarmonicProfileOrientationValue() throws PatriusException {

        Report.printMethodHeader("testHarmonicProfileOrientationValue", "Orientation computation",
            "Math", 1e-14, ComparisonType.ABSOLUTE);

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(5000);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10000);

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame cirf = FramesFactory.getCIRF();
        final AttitudeLawLeg law = new AttitudeLawLeg(new ConstantAttitudeLaw(
            FramesFactory.getEME2000(), new Rotation(Vector3D.PLUS_K, .15)), date0, date2);

        final KeplerianOrbit orbit = new KeplerianOrbit(6700000, .001, .15, 0, 0, 0,
            PositionAngle.MEAN, gcrf, date0, Constants.EGM96_EARTH_MU);
        final KeplerianPropagator prop = new KeplerianPropagator(orbit);

        final double T = orbit.getKeplerianPeriod();

        final AngularVelocitiesHarmonicProfile profile = GuidanceProfileBuilder
            .computeAngularVelocitiesHarmonicProfile(law, prop, gcrf, date0, T, 2,
                AngularVelocityIntegrationType.WILCOX_4, 1 / 8.);

        // inertial law
        final Rotation original = law.getAttitude(prop, date1, cirf).getRotation().revert();
        final Rotation computed = profile.getAttitude(prop, date1, cirf).getRotation().revert();
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(original, computed)), 1e-14);
        Report.printToReport("Quaternion", original, computed);

        // no spin
        final Vector3D originalS = law.getAttitude(prop, date1, cirf).getSpin();
        final Vector3D computedS = profile.getAttitude(prop, date1, cirf).getOrientation()
            .getRotationRate();
        Assert.assertEquals(0, originalS.getNorm(), 1e-15);
        Assert.assertEquals(0, computedS.getNorm(), 1e-15);
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getConstants()}
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getCosArrays()}
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getSinArrays()}
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getAngularFrequencies()}
     *
     * @description tests the angular velocities harmonic guidance profile
     *              getter methods for coverage
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria the outputs are the expected one : 1e-14 for profile coefficients
     *
     * @referenceVersion 2.2
     *
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testHarmonicProfileGetters() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(5000);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10000);

        final Frame gcrf = FramesFactory.getGCRF();
        FramesFactory.getCIRF();
        new AttitudeLawLeg(new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(
            Vector3D.PLUS_K, .15)), date0, date2);

        final KeplerianOrbit orbit = new KeplerianOrbit(6700000, .001, .15, 0, 0, 0,
            PositionAngle.MEAN, gcrf, date0, Constants.EGM96_EARTH_MU);
        final KeplerianPropagator prop = new KeplerianPropagator(orbit);

        // tests the getters:
        final double omega0 = 0.5;
        final double[] a0 = new double[] { 0, 2, 2, 4 };
        final double[] b0 = new double[] { 0.3, 8, 2, 1 };
        final FourierSeries xfs = new FourierSeries(omega0, 0.8, a0, b0);
        final double omega1 = 1;
        final double[] a1 = new double[] { 1, 2.6, 3, 4 };
        final double[] b1 = new double[] { 4, 3, 7, 1 };
        final FourierSeries yfs = new FourierSeries(omega1, 0.5, a1, b1);
        final double omega2 = 0.33;
        final double[] a2 = new double[] { 1, 2, 3, 5 };
        final double[] b2 = new double[] { 3, 0, 2, 1 };
        final FourierSeries zfs = new FourierSeries(omega2, 1.02, a2, b2);
        final AngularVelocitiesHarmonicProfile profile2 = new AngularVelocitiesHarmonicProfile(xfs,
            yfs, zfs, gcrf, new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date2,
                IntervalEndpointType.CLOSED), Rotation.IDENTITY, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.15, 100);
        // test the getter of the angular frequencies:
        Assert.assertEquals(0.5, profile2.getAngularFrequencies()[0]);
        Assert.assertEquals(1., profile2.getAngularFrequencies()[1]);
        Assert.assertEquals(0.33, profile2.getAngularFrequencies()[2]);
        // test the getter of the a0 coefficients:
        Assert.assertEquals(0.8, profile2.getConstants()[0]);
        Assert.assertEquals(0.5, profile2.getConstants()[1]);
        Assert.assertEquals(1.02, profile2.getConstants()[2]);
        // test the getter of the cosinus coefficients:
        Assert.assertEquals(a0[2], profile2.getCosArrays()[0][2]);
        Assert.assertEquals(a1[3], profile2.getCosArrays()[1][3]);
        Assert.assertEquals(a2[0], profile2.getCosArrays()[2][0]);
        // test the getter of the sinus coefficients:
        Assert.assertEquals(b0[2], profile2.getSinArrays()[0][2]);
        Assert.assertEquals(b1[3], profile2.getSinArrays()[1][3]);
        Assert.assertEquals(b2[0], profile2.getSinArrays()[2][0]);

        // Check that the size of the FourierSeries3DFunction is 3
        Assert.assertEquals(3, profile2.getSize());
        // Getters
        Assert.assertEquals("ANGULAR_VELOCITIES_HARMONIC_PROFILE", profile2.getNature());

        // Make sure the right date is given back (FA98)
        final Attitude att = profile2.getAttitude(prop, date1, gcrf);
        Assert.assertEquals(0, att.getDate().durationFrom(date1), this.eps);
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#ANGULAR_VELOCITY_POLYNOMIAL_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link
     *               AngularVelocitiesPolynomialProfile#AngularVelocitiesPolynomialProfile
     *               (frame, timeInterval, List<AngularVelocitiesPolynomialSegment> polynomials)}
     * @testedMethod {@link AngularVelocitiesPolynomialProfile#getXCoefficients()}
     * @testedMethod {@link AngularVelocitiesPolynomialProfile#getYCoefficients()}
     * @testedMethod {@link AngularVelocitiesPolynomialProfile#getZCoefficients()}
     *
     * @description tests the angular velocity polynomial guidance profile
     *              methods for coverage the validation tests will be added when
     *              the method to construct the profile from an attitude law
     *              will be implemented
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria the outputs are the expected one (epsilon = 0)
     *
     * @referenceVersion 1.3
     *
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testAngularVelocityPolynomialProfile() throws PatriusException {

        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(100);

        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm xpf1L = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] coeffsX = xpf1L.getCoefficients();
        final PolynomialFunction xpf1 = new PolynomialFunction(coeffsX);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm ypf1L = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] coeffsY = ypf1L.getCoefficients();
        final PolynomialFunction ypf1 = new PolynomialFunction(coeffsY);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm zpf1L = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] coeffsZ = zpf1L.getCoefficients();
        final PolynomialFunction zpf1 = new PolynomialFunction(coeffsZ);

        // create the first segment:
        final Rotation initialRotation = new Rotation(true, 1, 0, 0, 0);
        final AbsoluteDateInterval interval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date0, date1, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment1 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval1, initialRotation, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        final List<AngularVelocitiesPolynomialProfileLeg> segments = new ArrayList<>();
        segments.add(segment1);

        // test the value() method:
        Assert.assertEquals(2.0, segment1.getXCoefficients()[0]);

        final PolynomialFunction xpf2 = PolynomialsUtils.createHermitePolynomial(5);
        final PolynomialFunction ypf2 = PolynomialsUtils.createHermitePolynomial(5);
        final PolynomialFunction zpf2 = PolynomialsUtils.createHermitePolynomial(5);

        // create the second segment:
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            date1, date2, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment2 = new AngularVelocitiesPolynomialProfileLeg(
            xpf2, ypf2, zpf2, gcrf, interval2, initialRotation, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);

        // create the list of segments:
        segments.add(segment2);
        // create the guidance profile:
        final AngularVelocitiesPolynomialProfile profile = new AngularVelocitiesPolynomialProfile(
            segments);

        // check the exception is thrown when asking the spin at an invalid
        // date:
        try {
            profile.getAttitude(null, date0.shiftedBy(500.), gcrf);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        // tests the coefficients getters:
        Assert.assertEquals(xpf1.getCoefficients()[0], profile.getXCoefficients().get(interval1)[0]);
        Assert.assertEquals(ypf1.getCoefficients()[1], profile.getYCoefficients().get(interval1)[1]);
        Assert.assertEquals(zpf1.getCoefficients()[2], profile.getZCoefficients().get(interval1)[2]);

        Assert.assertEquals(xpf2.getCoefficients()[3], profile.getXCoefficients().get(interval2)[3]);
        Assert.assertEquals(ypf2.getCoefficients()[2], profile.getYCoefficients().get(interval2)[2]);
        Assert.assertEquals(zpf2.getCoefficients()[1], profile.getZCoefficients().get(interval2)[1]);

        // tests the getSpin() method:
        final AbsoluteDate testDate = date0.shiftedBy(25.);
        final Vector3D actual = profile.getAttitude(null, testDate, gcrf).getOrientation()
            .getRotationRate();
        final Vector3D expected = segments.get(0).getAttitude(null, testDate, gcrf).getSpin();
        Assert.assertEquals(expected.getX(), actual.getX(), 0.0);
        Assert.assertEquals(expected.getY(), actual.getY(), 0.0);
        Assert.assertEquals(expected.getZ(), actual.getZ(), 0.0);

        final Rotation orientation1 = profile.getAttitude(null, date0.shiftedBy(25.), gcrf)
            .getOrientation().getRotation();
        final AngularVelocitiesPolynomialProfile profile2 = new AngularVelocitiesPolynomialProfile(
            segments);
        profile2.setSpinDerivativesComputation(true);
        final Rotation orientation2 = profile2.getAttitude(null, date0.shiftedBy(25.), gcrf)
            .getOrientation().getRotation();
        // compare the orientation computed with Wilcox 4 with the orientation
        // computed with Edwards:
        Assert.assertEquals(orientation1.getQi()[0], orientation2.getQi()[0], 1E-5);
        Assert.assertEquals(orientation1.getQi()[1], orientation2.getQi()[1], 1E-5);
        Assert.assertEquals(orientation1.getQi()[2], orientation2.getQi()[2], 1E-5);
        Assert.assertEquals(orientation1.getQi()[3], orientation2.getQi()[3], 1E-5);
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#ANGULAR_VELOCITY_POLYNOMIAL_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link
     *               AngularVelocitiesPolynomialSlew#AngularVelocitiesPolynomialProfile
     *               (frame, timeInterval, List<AngularVelocitiesPolynomialSegment> polynomials)}
     * @testedMethod {@link AngularVelocitiesPolynomialSlew#getXCoefficients()}
     * @testedMethod {@link AngularVelocitiesPolynomialSlew#getYCoefficients()}
     * @testedMethod {@link AngularVelocitiesPolynomialSlew#getZCoefficients()}
     *
     * @description tests all the angular velocity polynomial slew methods. The output should be
     *              exactly the same as class angular velocity polynomial profile methods
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria the outputs are the expected one (epsilon = 0)
     *
     * @referenceVersion 4.5
     *
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testAngularVelocityPolynomialSlew() throws PatriusException {

        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(100);

        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm xpf1L = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] coeffsX = xpf1L.getCoefficients();
        final PolynomialFunction xpf1 = new PolynomialFunction(coeffsX);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm ypf1L = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] coeffsY = ypf1L.getCoefficients();
        final PolynomialFunction ypf1 = new PolynomialFunction(coeffsY);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm zpf1L = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] coeffsZ = zpf1L.getCoefficients();
        final PolynomialFunction zpf1 = new PolynomialFunction(coeffsZ);

        // create the first segment:
        final Rotation initialRotation = new Rotation(true, 1, 0, 0, 0);
        final AbsoluteDateInterval interval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date0, date1, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment1 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval1, initialRotation, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        final List<AngularVelocitiesPolynomialProfileLeg> segments = new ArrayList<>();
        segments.add(segment1);

        // test the value() method:
        Assert.assertEquals(2.0, segment1.getXCoefficients()[0]);

        final PolynomialFunction xpf2 = PolynomialsUtils.createHermitePolynomial(5);
        final PolynomialFunction ypf2 = PolynomialsUtils.createHermitePolynomial(5);
        final PolynomialFunction zpf2 = PolynomialsUtils.createHermitePolynomial(5);

        // create the second segment:
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            date1, date2, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment2 = new AngularVelocitiesPolynomialProfileLeg(
            xpf2, ypf2, zpf2, gcrf, interval2, initialRotation, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);

        // create the list of segments:
        segments.add(segment2);
        // create the guidance profile
        final AngularVelocitiesPolynomialSlew profile = new AngularVelocitiesPolynomialSlew(
            segments);

        // Check the default nature
        Assert.assertEquals("ANGULAR_VELOCITIES_POLYNOMIAL_SLEW", profile.getNature());

        // check the exception is thrown when asking the spin at an invalid
        // date:
        try {
            profile.getAttitude(null, date0.shiftedBy(500.), gcrf);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        // tests the coefficients getters:
        Assert.assertEquals(xpf1.getCoefficients()[0], profile.getXCoefficients().get(interval1)[0]);
        Assert.assertEquals(ypf1.getCoefficients()[1], profile.getYCoefficients().get(interval1)[1]);
        Assert.assertEquals(zpf1.getCoefficients()[2], profile.getZCoefficients().get(interval1)[2]);

        Assert.assertEquals(xpf2.getCoefficients()[3], profile.getXCoefficients().get(interval2)[3]);
        Assert.assertEquals(ypf2.getCoefficients()[2], profile.getYCoefficients().get(interval2)[2]);
        Assert.assertEquals(zpf2.getCoefficients()[1], profile.getZCoefficients().get(interval2)[1]);

        // tests the getSpin() method:
        final AbsoluteDate testDate = date0.shiftedBy(25.);
        final Vector3D actual = profile.getAttitude(null, testDate, gcrf).getOrientation()
            .getRotationRate();
        final Vector3D expected = segments.get(0).getAttitude(null, testDate, gcrf).getSpin();
        Assert.assertEquals(expected.getX(), actual.getX(), 0.0);
        Assert.assertEquals(expected.getY(), actual.getY(), 0.0);
        Assert.assertEquals(expected.getZ(), actual.getZ(), 0.0);

        final Rotation orientation1 = profile.getAttitude(date0.shiftedBy(25.), gcrf)
            .getOrientation().getRotation();
        final AngularVelocitiesPolynomialSlew profile2 = new AngularVelocitiesPolynomialSlew("");
        profile2.add(segment1);
        profile2.add(segment2);
        profile2.setSpinDerivativesComputation(true);
        final Rotation orientation2 = profile2.getAttitude(null, date0.shiftedBy(25.), gcrf)
            .getOrientation().getRotation();
        // compare the orientation computed with Wilcox 4 with the orientation
        // computed with Edwards:
        Assert.assertEquals(orientation1.getQi()[0], orientation2.getQi()[0], 1E-5);
        Assert.assertEquals(orientation1.getQi()[1], orientation2.getQi()[1], 1E-5);
        Assert.assertEquals(orientation1.getQi()[2], orientation2.getQi()[2], 1E-5);
        Assert.assertEquals(orientation1.getQi()[3], orientation2.getQi()[3], 1E-5);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     * @testedFeature {@link features#ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#getAttitude(PVCoordinatesProvider)}
     *
     * @description tests the getAttitude method
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria the outputs are the expected one : 2e-14° threshold for
     *                   the recomputed attitude, 1e-8 relative difference for the spin
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testGetAttitude() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final KeplerianOrbit orbit = new KeplerianOrbit(6700000, .001, .15, 0, 0, 0,
            PositionAngle.MEAN, gcrf, date0, Constants.EGM96_EARTH_MU);
        final double T = orbit.getKeplerianPeriod();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(T / 2.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(T);

        final AttitudeLawLeg law = new AttitudeLawLeg(new BodyCenterPointing(), date0, date2);

        final KeplerianPropagator prop = new KeplerianPropagator(orbit);

        final AngularVelocitiesHarmonicProfile profile = GuidanceProfileBuilder
            .computeAngularVelocitiesHarmonicProfile(law, prop, gcrf, date0, T, 8,
                AngularVelocityIntegrationType.WILCOX_4, 1 / 8.);

        // Computation
        final Orbit orbit1 = prop.propagate(date1).getOrbit();
        final Attitude actual = profile.getAttitude(orbit1);
        final Attitude expected = law.getAttitude(orbit1);

        // Check
        Assert.assertEquals(actual.getDate(), expected.getDate());
        Assert.assertEquals(actual.getReferenceFrame(), expected.getReferenceFrame());
        Assert.assertEquals(0,
            MathLib.toDegrees(Rotation.distance(expected.getRotation(), actual.getRotation())),
            1E-4);
        Assert.assertEquals(0, actual.getSpin().getNorm(), expected.getSpin().getNorm());
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#GUIDANCE_PROFILES}
     *
     * @testedMethod {@link AngularVelocitiesPolynomialProfile#getAttitude(PVCoordinatesProvider)}
     *
     * @descriptioncheck that attitude is properly computed with a simple profile (piecewise
     *                   constant)
     *
     * @input methods inputs
     *
     * @output methods outputs
     *
     * @testPassCriteria attitude (angle and axis rotation) is as expected (reference result: math,
     *                   threshold: 1E-7
     *                   limited by Edwards integration step)
     *
     * @referenceVersion 4.0
     *
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testGetAttitudeAngularVelocitiesPolynomialProfile() throws PatriusException {

        // Two segments:
        // - 1st segment from 0s to 500s: rotation around z at 2 rad/s
        // - 2nd segment from 500s to 1000s: rotation around z at 3 rad/s
        // Rotations follow hence second segment starts at the end of first segment
        final AbsoluteDate lowerDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate midDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.);
        final AbsoluteDate upperDate = AbsoluteDate.J2000_EPOCH.shiftedBy(1000.);

        final List<AngularVelocitiesPolynomialProfileLeg> polynomials = new ArrayList<>();

        final AbsoluteDateInterval timeInterval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, lowerDate, midDate, IntervalEndpointType.CLOSED);
        final PolynomialFunctionLagrangeForm x1L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_x1 = x1L.getCoefficients();
        final PolynomialFunction x1 = new PolynomialFunction(c_x1);
        final PolynomialFunctionLagrangeForm y1L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_y1 = y1L.getCoefficients();
        final PolynomialFunction y1 = new PolynomialFunction(c_y1);
        final PolynomialFunctionLagrangeForm z1L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 2, 2 });
        final double[] c_z1 = z1L.getCoefficients();
        final PolynomialFunction z1 = new PolynomialFunction(c_z1);
        final AngularVelocitiesPolynomialProfileLeg segment1 = new AngularVelocitiesPolynomialProfileLeg(
            x1, y1, z1, FramesFactory.getGCRF(), timeInterval1, Rotation.IDENTITY, lowerDate,
            AngularVelocityIntegrationType.EDWARDS, 0.01, 100);
        polynomials.add(segment1);
        final AbsoluteDateInterval timeInterval2 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, midDate, upperDate, IntervalEndpointType.CLOSED);
        final PolynomialFunctionLagrangeForm x2L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_x2 = x2L.getCoefficients();
        final PolynomialFunction x2 = new PolynomialFunction(c_x2);
        final PolynomialFunctionLagrangeForm y2L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_y2 = y2L.getCoefficients();
        final PolynomialFunction y2 = new PolynomialFunction(c_y2);
        final PolynomialFunctionLagrangeForm z2L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 3, 3 });
        final double[] c_z2 = z2L.getCoefficients();
        final PolynomialFunction z2 = new PolynomialFunction(c_z2);
        final AngularVelocitiesPolynomialProfileLeg segment2 = new AngularVelocitiesPolynomialProfileLeg(
            x2, y2, z2, FramesFactory.getGCRF(), timeInterval2,
            segment1.getOrientation(midDate), midDate, AngularVelocityIntegrationType.EDWARDS,
            0.01);
        polynomials.add(segment2);

        final AbsoluteDateInterval timeInterval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, lowerDate, upperDate, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfile profile = new AngularVelocitiesPolynomialProfile(
            polynomials);

        // First segment: check that attitude after 100s has a rotation of 200 rad around z (or -200
        // rad around -z)
        // Accuracy (1E-7) depends on Edwards integration step
        final Attitude attitude1 = profile.getAttitude(null,
            AbsoluteDate.J2000_EPOCH.shiftedBy(100), FramesFactory.getGCRF());
        Assert.assertEquals((2. * FastMath.PI) - ((100. * 2.) % (2. * FastMath.PI)), attitude1
            .getRotation().getAngle(), 1E-7);
        Assert.assertEquals(0, attitude1.getRotation().getAxis().subtract(new Vector3D(0, 0, -1))
            .getNorm(), 0.);

        // Second segment: check that attitude after 600s has a rotation of 1300 rad around z (or
        // -1300 rad around -z)
        // Accuracy (1E-7) depends on Edwards integration step
        final Attitude attitude2 = profile.getAttitude(null,
            AbsoluteDate.J2000_EPOCH.shiftedBy(600), FramesFactory.getGCRF());
        Assert.assertEquals(
            (2. * FastMath.PI) - (((500. * 2.) + (100. * 3.)) % (2. * FastMath.PI)), attitude2
                .getRotation().getAngle(), 1E-6);
        Assert.assertEquals(0, attitude2.getRotation().getAxis().subtract(new Vector3D(0, 0, -1))
            .getNorm(), 1E-15);
    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link AngularVelocitiesHarmonicProfile#copy(AbsoluteDateInterval)}
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
    public void testAngularVelocityHarmonicProfileCopyMethod() throws PatriusException {

        // Constructor dates
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(10000.);

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            startDate, endDate, IntervalEndpointType.CLOSED);
        final Frame gcrf = FramesFactory.getGCRF();

        final double omega0 = 0.5;
        final double[] a0 = new double[] { 0, 2, 2, 4 };
        final double[] b0 = new double[] { 0.3, 8, 2, 1 };
        final FourierSeries xfs = new FourierSeries(omega0, 0.8, a0, b0);
        final double omega1 = 1;
        final double[] a1 = new double[] { 1, 2.6, 3, 4 };
        final double[] b1 = new double[] { 4, 3, 7, 1 };
        final FourierSeries yfs = new FourierSeries(omega1, 0.5, a1, b1);
        final double omega2 = 0.33;
        final double[] a2 = new double[] { 1, 2, 3, 5 };
        final double[] b2 = new double[] { 3, 0, 2, 1 };
        final FourierSeries zfs = new FourierSeries(omega2, 1.02, a2, b2);

        // Intervals creation
        final double offset = 5;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, startDate.shiftedBy(offset),
            endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, startDate.shiftedBy(offset),
            endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // AbstractOrientationAngleLeg creation
        AngularVelocitiesHarmonicProfile profile1 = new AngularVelocitiesHarmonicProfile(xfs, yfs,
            zfs, gcrf, interval, Rotation.IDENTITY, startDate,
            AngularVelocityIntegrationType.WILCOX_4, 0.15);
        profile1.setSpinDerivativesComputation(true);
        final AngularVelocitiesHarmonicProfile profile2 = new AngularVelocitiesHarmonicProfile(xfs,
            yfs, zfs, gcrf, interval, Rotation.IDENTITY, startDate,
            AngularVelocityIntegrationType.WILCOX_4, 0.15);

        final Attitude attitudeRef = profile1.getAttitude(null, startDate.shiftedBy(5), gcrf);

        // Test case n°1 : in a standard usage, the interval stored should be updated
        profile1 = profile1.copy(newIntervalOfValidity);
        Assert.assertTrue(profile1.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = profile1.getAttitude(null, startDate.shiftedBy(5), gcrf);
        Assert.assertEquals(0,
            Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(
            0,
            attitudeActual.getRotationAcceleration().distance(
                attitudeRef.getRotationAcceleration()), 1E-15);

        // Test case n°2 : when the new interval isn't included, the method copy should throw an
        // exception
        try {
            profile2.copy(newIntervalOfValidityNotIncluded);
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
     * @testedMethod {@link AngularVelocitiesPolynomialProfile#copy(AbsoluteDateInterval)}
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
    public void testAngularVelocityPolynomialProfileCopyMethod() throws PatriusException {

        // Two segments:
        // - 1st segment from 0s to 500s: rotation around z at 2 rad/s
        // - 2nd segment from 500s to 1000s: rotation around z at 3 rad/s
        // - 3nd segment from 1000s to 1500s: rotation around z at 3 rad/s
        // Rotations follow hence second segment starts at the end of first segment
        final AbsoluteDate lowerDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate midDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.);
        final AbsoluteDate upperDate = AbsoluteDate.J2000_EPOCH.shiftedBy(1000.);

        final List<AngularVelocitiesPolynomialProfileLeg> polynomials = new ArrayList<>();

        final AbsoluteDateInterval timeInterval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, lowerDate, midDate, IntervalEndpointType.CLOSED);
        final PolynomialFunctionLagrangeForm x1L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_x1 = x1L.getCoefficients();
        final PolynomialFunction x1 = new PolynomialFunction(c_x1);
        final PolynomialFunctionLagrangeForm y1L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_y1 = y1L.getCoefficients();
        final PolynomialFunction y1 = new PolynomialFunction(c_y1);
        final PolynomialFunctionLagrangeForm z1L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 2, 2 });
        final double[] c_z1 = z1L.getCoefficients();
        final PolynomialFunction z1 = new PolynomialFunction(c_z1);
        final AngularVelocitiesPolynomialProfileLeg segment1 = new AngularVelocitiesPolynomialProfileLeg(
            x1, y1, z1, FramesFactory.getGCRF(), timeInterval1, Rotation.IDENTITY, lowerDate,
            AngularVelocityIntegrationType.EDWARDS, 0.01, 100);
        polynomials.add(segment1);
        final AbsoluteDateInterval timeInterval2 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, midDate, upperDate, IntervalEndpointType.CLOSED);
        final PolynomialFunctionLagrangeForm x2L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_x2 = x2L.getCoefficients();
        final PolynomialFunction x2 = new PolynomialFunction(c_x2);
        final PolynomialFunctionLagrangeForm y2L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 0, 0 });
        final double[] c_y2 = y2L.getCoefficients();
        final PolynomialFunction y2 = new PolynomialFunction(c_y2);
        final PolynomialFunctionLagrangeForm z2L = new PolynomialFunctionLagrangeForm(new double[] {
            0, 1000 }, new double[] { 3, 3 });
        final double[] c_z2 = z2L.getCoefficients();
        final PolynomialFunction z2 = new PolynomialFunction(c_z2);
        final AngularVelocitiesPolynomialProfileLeg segment2 = new AngularVelocitiesPolynomialProfileLeg(
            x2, y2, z2, FramesFactory.getGCRF(), timeInterval2,
            segment1.getOrientation(midDate), midDate, AngularVelocityIntegrationType.EDWARDS,
            0.01);
        polynomials.add(segment2);
        final AbsoluteDateInterval timeInterval3 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, upperDate, upperDate.shiftedBy(500), IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment3 = new AngularVelocitiesPolynomialProfileLeg(
            x2, y2, z2, FramesFactory.getGCRF(), timeInterval3,
            segment2.getOrientation(upperDate), upperDate, AngularVelocityIntegrationType.EDWARDS,
            0.01);
        polynomials.add(segment3);

        final AbsoluteDateInterval timeInterval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, lowerDate, upperDate, IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, lowerDate.shiftedBy(5), upperDate.shiftedBy(-10),
            IntervalEndpointType.CLOSED);
        AngularVelocitiesPolynomialProfile profile = new AngularVelocitiesPolynomialProfile(
            polynomials);
        profile.setSpinDerivativesComputation(true);

        final Attitude attitudeRef = profile.getAttitude(null, lowerDate.shiftedBy(5),
            FramesFactory.getGCRF());

        // Test case n°1 : in a standard usage, the interval stored should be updated
        profile = profile.copy(newIntervalOfValidity);
        Assert.assertTrue(profile.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = profile.getAttitude(null, lowerDate.shiftedBy(5),
            FramesFactory.getGCRF());
        Assert.assertEquals(0,
            Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(
            0,
            attitudeActual.getRotationAcceleration().distance(
                attitudeRef.getRotationAcceleration()), 1E-15);

        // Test case n°2 : when the new interval isn't included, the method copy should throw an
        // exception
        try {
            profile.copy(timeInterval);
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
     * @testedMethod {@link AngularVelocitiesPolynomialSlew#copy(AbsoluteDateInterval)}
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
    public void testAngularVelocityPolynomialSlewCopyMethod() throws PatriusException {

        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(100);

        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm xpf1L = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] coeffsX = xpf1L.getCoefficients();
        final PolynomialFunction xpf1 = new PolynomialFunction(coeffsX);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm ypf1L = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] coeffsY = ypf1L.getCoefficients();
        final PolynomialFunction ypf1 = new PolynomialFunction(coeffsY);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm zpf1L = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] coeffsZ = zpf1L.getCoefficients();
        final PolynomialFunction zpf1 = new PolynomialFunction(coeffsZ);

        // create the first segment:
        final Rotation initialRotation = new Rotation(true, 1, 0, 0, 0);
        final AbsoluteDateInterval interval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date0, date1, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment1 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval1, initialRotation, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        final List<AngularVelocitiesPolynomialProfileLeg> segments = new ArrayList<>();
        segments.add(segment1);

        final PolynomialFunction xpf2 = PolynomialsUtils.createHermitePolynomial(5);
        final PolynomialFunction ypf2 = PolynomialsUtils.createHermitePolynomial(5);
        final PolynomialFunction zpf2 = PolynomialsUtils.createHermitePolynomial(5);

        // create the second segment:
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            date1, date2, IntervalEndpointType.CLOSED);
        final AngularVelocitiesPolynomialProfileLeg segment2 = new AngularVelocitiesPolynomialProfileLeg(
            xpf2, ypf2, zpf2, gcrf, interval2, initialRotation, date0,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);

        // create the list of segments:
        segments.add(segment2);
        // create the guidance profile
        final AbsoluteDateInterval timeInterval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date0, date2, IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date0.shiftedBy(5), date1.shiftedBy(-10),
            IntervalEndpointType.CLOSED);
        AngularVelocitiesPolynomialSlew profile = new AngularVelocitiesPolynomialSlew(segments);
        profile.setSpinDerivativesComputation(true);

        final Attitude attitudeRef = profile.getAttitude(null, date0.shiftedBy(5), gcrf);

        // Test case n°1 : in a standard usage, the interval stored should be updated
        profile = profile.copy(newIntervalOfValidity);
        Assert.assertTrue(profile.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = profile.getAttitude(null, date0.shiftedBy(5), gcrf);
        Assert.assertEquals(0,
            Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(
            0,
            attitudeActual.getRotationAcceleration().distance(
                attitudeRef.getRotationAcceleration()), 1E-15);

        // Test case n°2 : when the new interval isn't included, the method copy should throw an
        // exception
        try {
            profile.copy(timeInterval);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link AngularVelocitiesPolynomialProfile#head(AbsoluteDate, boolean), @link
     *               AngularVelocitiesPolynomialProfile#tail(AbsoluteDate, boolean) and @link
     *               AngularVelocitiesPolynomialProfile#sub(AbsoluteDateInterval, boolean)}
     *
     * @description test 3 previous methods methods for different sequences (empty, standard, open
     *              boundaries, holes)
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
    public void testHeadTailSubAngularVelocityPolynomialProfile() throws PatriusException {

        // Init sequence
        AngularVelocitiesPolynomialProfile sequence = new AngularVelocitiesPolynomialProfile("");

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
        final Frame gcrf = FramesFactory.getGCRF();
        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm xpf1L = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] coeffsX = xpf1L.getCoefficients();
        final PolynomialFunction xpf1 = new PolynomialFunction(coeffsX);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm ypf1L = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] coeffsY = ypf1L.getCoefficients();
        final PolynomialFunction ypf1 = new PolynomialFunction(coeffsY);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm zpf1L = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] coeffsZ = zpf1L.getCoefficients();
        final PolynomialFunction zpf1 = new PolynomialFunction(coeffsZ);
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final Rotation initialRotation = new Rotation(true, 1, 0, 0, 0);
        AbsoluteDateInterval interval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date1, date2, IntervalEndpointType.CLOSED);
        AbsoluteDateInterval interval2 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date2, date3, IntervalEndpointType.CLOSED);
        AbsoluteDateInterval interval3 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date3, date4, IntervalEndpointType.CLOSED);
        AngularVelocitiesPolynomialProfileLeg leg1 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval1, initialRotation, date1,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        AngularVelocitiesPolynomialProfileLeg leg2 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval2, initialRotation, date2,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        AngularVelocitiesPolynomialProfileLeg leg3 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval3, initialRotation, date3,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        List<AngularVelocitiesPolynomialProfileLeg> segments = new ArrayList<>();
        segments.add(leg3);
        segments.add(leg2);
        segments.add(leg1);
        sequence = new AngularVelocitiesPolynomialProfile(segments);
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
        interval1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1, date2,
            IntervalEndpointType.OPEN);
        interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date2, date3,
            IntervalEndpointType.OPEN);
        interval3 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date3, date4,
            IntervalEndpointType.OPEN);
        leg1 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval1,
            initialRotation, date1, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg2 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval2,
            initialRotation, date2, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg3 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval3,
            initialRotation, date3, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        segments = new ArrayList<>();
        segments.add(leg3);
        segments.add(leg2);
        segments.add(leg1);
        sequence = new AngularVelocitiesPolynomialProfile(segments);
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
        interval1 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date1, date2,
            IntervalEndpointType.CLOSED);
        interval3 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date3, date4,
            IntervalEndpointType.CLOSED);
        leg1 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval1,
            initialRotation, date1, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg3 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval3,
            initialRotation, date3, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        segments = new ArrayList<>();
        segments.add(leg3);
        segments.add(leg1);
        sequence = new AngularVelocitiesPolynomialProfile(segments);
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
     * @testedFeature {@link features#ANGULAR_VELOCITY_HARMONIC_GUIDANCE_PROFILE}
     *
     * @testedMethod {@link AngularVelocitiesPolynomialSlew#head(AbsoluteDate, boolean), @link
     *               AngularVelocitiesPolynomialSlew#tail(AbsoluteDate, boolean) and @link
     *               AngularVelocitiesPolynomialSlew#sub(AbsoluteDateInterval, boolean)}
     *
     * @description test 3 previous methods methods for different sequences (empty, standard, open
     *              boundaries, holes)
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
    public void testHeadTailSubAngularVelocityPolynomialSlew() throws PatriusException {

        // Init sequence
        AngularVelocitiesPolynomialSlew sequence = new AngularVelocitiesPolynomialSlew("");

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
        final Frame gcrf = FramesFactory.getGCRF();
        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm xpf1L = new PolynomialFunctionLagrangeForm(x, y0);
        final double[] coeffsX = xpf1L.getCoefficients();
        final PolynomialFunction xpf1 = new PolynomialFunction(coeffsX);
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm ypf1L = new PolynomialFunctionLagrangeForm(x, y1);
        final double[] coeffsY = ypf1L.getCoefficients();
        final PolynomialFunction ypf1 = new PolynomialFunction(coeffsY);
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm zpf1L = new PolynomialFunctionLagrangeForm(x, y2);
        final double[] coeffsZ = zpf1L.getCoefficients();
        final PolynomialFunction zpf1 = new PolynomialFunction(coeffsZ);
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final Rotation initialRotation = new Rotation(true, 1, 0, 0, 0);
        AbsoluteDateInterval interval1 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date1, date2, IntervalEndpointType.CLOSED);
        AbsoluteDateInterval interval2 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date2, date3, IntervalEndpointType.CLOSED);
        AbsoluteDateInterval interval3 = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date3, date4, IntervalEndpointType.CLOSED);
        AngularVelocitiesPolynomialProfileLeg leg1 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval1, initialRotation, date1,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        AngularVelocitiesPolynomialProfileLeg leg2 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval2, initialRotation, date2,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        AngularVelocitiesPolynomialProfileLeg leg3 = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval3, initialRotation, date3,
            AngularVelocityIntegrationType.WILCOX_4, 0.001);
        List<AngularVelocitiesPolynomialProfileLeg> segments = new ArrayList<>();
        segments.add(leg3);
        segments.add(leg2);
        segments.add(leg1);
        sequence = new AngularVelocitiesPolynomialSlew(segments);
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
        interval1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1, date2,
            IntervalEndpointType.OPEN);
        interval2 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date2, date3,
            IntervalEndpointType.OPEN);
        interval3 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date3, date4,
            IntervalEndpointType.OPEN);
        leg1 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval1,
            initialRotation, date1, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg2 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval2,
            initialRotation, date2, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg3 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval3,
            initialRotation, date3, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        segments = new ArrayList<>();
        segments.add(leg3);
        segments.add(leg2);
        segments.add(leg1);
        sequence = new AngularVelocitiesPolynomialSlew(segments);
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
        interval1 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date1, date2,
            IntervalEndpointType.CLOSED);
        interval3 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date3, date4,
            IntervalEndpointType.CLOSED);
        leg1 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval1,
            initialRotation, date1, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg3 = new AngularVelocitiesPolynomialProfileLeg(xpf1, ypf1, zpf1, gcrf, interval3,
            initialRotation, date3, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        segments = new ArrayList<>();
        segments.add(leg3);
        segments.add(leg1);
        sequence = new AngularVelocitiesPolynomialSlew(segments);
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

        // Test the size of the leg's polynomial3DFunction
        Assert.assertEquals(3, leg3.getSize());

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
}
