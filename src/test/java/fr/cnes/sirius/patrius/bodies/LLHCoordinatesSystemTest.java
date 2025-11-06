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
 * VERSION:4.15:OPENFD-351:21/11/2024:[PATRIUS] Calcul de la dérivée des coordonnées LLH
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link LLHCoordinatesSystem} class.
 * 
 * @author Thibaut BONIT
 *
 * @version $Id$
 *
 * @since 4.13
 */
public class LLHCoordinatesSystemTest {

    /**
     * @description Coverage test for the basic getters.
     *
     * @testedMethod {@link LLHCoordinatesSystem#getLatLongSystemLabel()}
     * @testedMethod {@link LLHCoordinatesSystem#getHeightSystemLabel()}
     * @testedMethod {@link LLHCoordinatesSystem#ELLIPSODETIC}
     * @testedMethod {@link LLHCoordinatesSystem#BODYCENTRIC_RADIAL}
     * @testedMethod {@link LLHCoordinatesSystem#BODYCENTRIC_NORMAL}
     * 
     * @testPassCriteria The basic getters return the expected data.
     */
    @Test
    public void testGetters() {

        // ELLIPSODETIC
        LLHCoordinatesSystem llhSys = LLHCoordinatesSystem.ELLIPSODETIC;
        Assert.assertEquals("surface ellipsodetic coord", llhSys.getLatLongSystemLabel());
        Assert.assertEquals("normal height", llhSys.getHeightSystemLabel());

        // BODYCENTRIC_RADIAL
        llhSys = LLHCoordinatesSystem.BODYCENTRIC_RADIAL;
        Assert.assertEquals("surface bodycentric coord", llhSys.getLatLongSystemLabel());
        Assert.assertEquals("radial height", llhSys.getHeightSystemLabel());

        // BODYCENTRIC_NORMAL
        llhSys = LLHCoordinatesSystem.BODYCENTRIC_NORMAL;
        Assert.assertEquals("surface bodycentric coord", llhSys.getLatLongSystemLabel());
        Assert.assertEquals("normal height", llhSys.getHeightSystemLabel());
    }

    /**
     * Tests the jacobian matrix obtained with {@link LLHCoordinatesSystem#jacobianToCartesian(BodyPoint)} in
     * ELLIPSODETIC mode.
     * - References for the results: MSLIB (specific implementation with a OneAxisEllipsoid)
     * - Generic implementation evaluation compared to the specific one, using a similar ThreeAxisEllipsoid
     * 
     * @testedMethod {@link LLHCoordinatesSystem#jacobianToCartesian(BodyPoint)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testJacobianToCartesianEllipsodetic() throws PatriusException {

        // #1: Standard case: evaluate the jacobianToCartesian feature against reference results from MSLIB in
        // ELLIPSODETIC mode with an OneAxisEllipsoid (to use the specific implementation)
        // Reference jacobian from MSLIB
        final double[][] jacobian = {
            { -0.479311467789823 * 1e7, -0.177908184625686 * 1e6, 0.657529466860734 },
            { -0.202941785964951 * 1e6, 0.420186669291890 * 1e7, 0.0278399774043822 },
            { 0.419339100580230 * 1e7, 0., 0.752914295167758 }
        };

        // Build the point and compute the jacobian to cartesian
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378137., 1 / 298.257222101, frame);
        final double lat = 0.852479154923577;
        final double lon = 0.0423149994747243;
        final double alt = 111.6;
        final EllipsoidPoint nsp = new EllipsoidPoint(earth, LLHCoordinatesSystem.ELLIPSODETIC, lat, lon, alt, "");
        final double[][] computedJacobian = LLHCoordinatesSystem.ELLIPSODETIC.jacobianToCartesian(nsp);

        // Check if the computed matrix and the expected one are the same
        checkMatrix(computedJacobian, jacobian, Utils.epsilonTest, 1e-8);

        // #2: Use a ThreeAxisEllipsoid in ELLIPSODETIC mode so the generic implementation of the jacobianToCartesian
        // feature will be used, and compare the two computed jacobians which should produce similar results
        final ThreeAxisEllipsoid earthBis = new ThreeAxisEllipsoid(earth.getARadius(), earth.getARadius(),
            earth.getCRadius(), frame);
        final EllipsoidPoint nspBis = new EllipsoidPoint(earthBis, LLHCoordinatesSystem.ELLIPSODETIC, lat, lon, alt, "");
        final double[][] computedJacobianBis = LLHCoordinatesSystem.ELLIPSODETIC.jacobianToCartesian(nspBis);

        // Check if the computed matrix with the generic implementation (computedJacobianBis) and the one computed with
        // the specific implementation (computedJacobian) are similar
        checkMatrix(computedJacobianBis, computedJacobian, 2.e-5, 80.); // Absolute accuracy not great but relative OK
    }

    /**
     * Evaluate the jacobianToCartesian feature in BODYCENTRIC_RADIAL & BODYCENTRIC_NORMAL modes against ELLIPSODETIC
     * mode with the generic implementation which is already validated in testJacobianToCartesianEllipsodetic().
     * 
     * @testedMethod {@link LLHCoordinatesSystem#jacobianToCartesian(BodyPoint)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testJacobianToCartesian() throws PatriusException {

        // Build the point and compute the reference jacobian to cartesian in ELLIPSODETIC mode with a
        // ThreeAxisEllipsoid so the generic implementation of the jacobianToCartesian feature will be used
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378137., 0., frame);
        final ThreeAxisEllipsoid earthBis = new ThreeAxisEllipsoid(earth.getARadius(), earth.getARadius(),
            earth.getCRadius(), frame);
        final double lat = 0.852479154923577;
        final double lon = 0.0423149994747243;
        final double alt = 111.6;
        final EllipsoidPoint nsp = new EllipsoidPoint(earthBis, LLHCoordinatesSystem.ELLIPSODETIC, lat, lon, alt, "");
        final double[][] jacobian = LLHCoordinatesSystem.ELLIPSODETIC.jacobianToCartesian(nsp);

        // Compute the jacobian to cartesian in BODYCENTRIC_RADIAL mode on the same point and compare it to the ref
        double[][] computedJacobian = LLHCoordinatesSystem.BODYCENTRIC_RADIAL.jacobianToCartesian(nsp);
        checkMatrix(computedJacobian, jacobian, 4e-10, 5e-8);

        // Compute the jacobian to cartesian in BODYCENTRIC_NORMAL mode on the same point and compare it to the ref
        computedJacobian = LLHCoordinatesSystem.BODYCENTRIC_NORMAL.jacobianToCartesian(nsp);
        checkMatrix(computedJacobian, jacobian, Utils.epsilonTest, 5e-8);
    }

    /**
     * Tests the jacobian matrix obtained with {@link LLHCoordinatesSystem#jacobianFromCartesian(BodyPoint)} in
     * ELLIPSODETIC mode.
     * - References for the results: MSLIB (specific implementation with a OneAxisEllipsoid)
     * - Generic implementation evaluation compared to the specific one, using a similar ThreeAxisEllipsoid
     * 
     * @testedMethod {@link LLHCoordinatesSystem#jacobianFromCartesian(BodyPoint)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testJacobianFromCartesianEllipsodetic() throws PatriusException {

        // #1: Standard case: evaluate the jacobianFromCartesian feature against reference results from MSLIB in
        // ELLIPSODETIC mode with an OneAxisEllipsoid (to use the specific implementation)
        // Reference jacobian from MSLIB
        final double[][] jacobian =
        { { -0.107954488167401 * 1e-6, -0.282449738801487 * 1e-8, 0.114080171824722 * 1e-6 },
            { -0.563745742240173 * 1e-8, 0.215468009700879 * 1e-6, 0. },
            { 0.725972822728309, 0.0189941926118555, 0.687461039846560 }
        };

        // Build the point and compute the jacobian from cartesian
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378137., 1 / 298.257222101, frame);
        final EllipsoidPoint nsp = new EllipsoidPoint(earth, new Vector3D(4637885.347, 121344.608, 4362452.869), frame,
            date, "");
        final double[][] computedJacobian = LLHCoordinatesSystem.ELLIPSODETIC.jacobianFromCartesian(nsp);

        // Check if the computed matrix and the expected one are the same
        checkMatrix(computedJacobian, jacobian, Utils.epsilonTest, Utils.epsilonTest);

        // #2: Use a ThreeAxisEllipsoid in ELLIPSODETIC mode so the generic implementation of the jacobianFromCartesian
        // feature will be used, and compare the two computed jacobians which should produce similar results
        final ThreeAxisEllipsoid earthBis = new ThreeAxisEllipsoid(earth.getARadius(), earth.getARadius(),
            earth.getCRadius(), frame);
        final EllipsoidPoint nspBis = new EllipsoidPoint(earthBis, new Vector3D(4637885.347, 121344.608, 4362452.869),
            frame, date, "");
        final double[][] computedJacobianBis = LLHCoordinatesSystem.ELLIPSODETIC.jacobianFromCartesian(nspBis);

        // Check if the computed matrix with the generic implementation (computedJacobianBis) and the one computed with
        // the specific implementation (computedJacobian) are similar
        checkMatrix(computedJacobianBis, computedJacobian, 1e-6, 1e-8);

        // #3: Error case, try with a point on the pole (should fail)
        try {
            final EllipsoidPoint nspPole = new EllipsoidPoint(earth, new Vector3D(0., 0., 7000000.), frame, date, "");
            LLHCoordinatesSystem.ELLIPSODETIC.jacobianFromCartesian(nspPole);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // #4: Error case, try with a point too close to the ellipsoid center (should fail)
        try {
            final EllipsoidPoint nspPole =
                    new EllipsoidPoint(earth, new Vector3D(1000., 0., 0.), frame, date, "");
            LLHCoordinatesSystem.ELLIPSODETIC.jacobianFromCartesian(nspPole);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Evaluate the jacobianFromCartesian feature in BODYCENTRIC_RADIAL & BODYCENTRIC_NORMAL modes against ELLIPSODETIC
     * mode with the generic implementation which is already validated in testJacobianFromCartesianEllipsodetic().
     * 
     * @testedMethod {@link LLHCoordinatesSystem#jacobianFromCartesian(BodyPoint)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testJacobianFromCartesian() throws PatriusException {

        // Build the point and compute the reference jacobian from cartesian in ELLIPSODETIC mode with a
        // ThreeAxisEllipsoid so the generic implementation of the jacobianFromCartesian feature will be used
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378137., 0., frame);
        final ThreeAxisEllipsoid earthBis = new ThreeAxisEllipsoid(earth.getARadius(), earth.getARadius(),
            earth.getCRadius(), frame);
        final double lat = 0.852479154923577;
        final double lon = 0.0423149994747243;
        final double alt = 111.6;
        final EllipsoidPoint nsp = new EllipsoidPoint(earthBis, LLHCoordinatesSystem.ELLIPSODETIC, lat, lon, alt, "");
        final double[][] jacobian = LLHCoordinatesSystem.ELLIPSODETIC.jacobianFromCartesian(nsp);

        // Compute the jacobian from cartesian in BODYCENTRIC_RADIAL mode on the same point and compare it to the ref
        double[][] computedJacobian = LLHCoordinatesSystem.BODYCENTRIC_RADIAL.jacobianFromCartesian(nsp);
        checkMatrix(computedJacobian, jacobian, 3e-7, 1e-8);

        // Compute the jacobian from cartesian in BODYCENTRIC_NORMAL mode on the same point and compare it to the ref
        computedJacobian = LLHCoordinatesSystem.BODYCENTRIC_NORMAL.jacobianFromCartesian(nsp);
        checkMatrix(computedJacobian, jacobian, 3e-7, Utils.epsilonTest);
    }

    /**
     * Evaluate the rates of the LLH coordinates from the Cartesian position and velocity on a
     * {@link OneAxisEllipsoid} using the analytical formula.
     * 
     * @testedMethod {@link LLHCoordinatesSystem#computeLLHRates(BodyShape, PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testRatesComputationOneAxisEllipsoidAnalytical() throws PatriusException {
        // Create Earth OneAxisEllipsoid
        final double ae = 6378137.0;// GRS80 constant
        final double flattening = 1.0 / 298.257222101;// GRS80 constant
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, flattening, itrf);
        final AbsoluteDate date = new AbsoluteDate(2010, 7, 9, 20, 30, 0);

        // Create orbit around the Earth in GCRF
        final KeplerianOrbit orbit = createKeplerianEarthOrbit(gcrf, date);

        // Compute the rates using the analytical implementation
        final double[] rates = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, orbit.getPVCoordinates(), gcrf, date);

        // Check result
        TestUtils.assertEquals(8.851239240946895E-4, rates[0], Utils.epsilonTest);
        TestUtils.assertEquals(-4.846251522455342E-4, rates[1], Utils.epsilonTest);
        TestUtils.assertEquals(3.433926875845139, rates[2], Utils.epsilonTest);
    }

    /**
     * Evaluate the rates of the LLH coordinates from the Cartesian position and velocity on a
     * {@link OneAxisEllipsoid} using the finite differences estimation.
     * 
     * @testedMethod {@link LLHCoordinatesSystem#computeLLHRates(BodyShape, PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testRatesComputationOneAxisEllipsoidFiniteDifferences() throws PatriusException {
        // Create Earth OneAxisEllipsoid
        final double ae = 6378137.0;// GRS80 constant
        final double flattening = 1.0 / 298.257222101;// GRS80 constant
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, flattening, itrf);
        final AbsoluteDate date = new AbsoluteDate(2010, 7, 9, 20, 30, 0);

        // Create orbit around the Earth in GCRF
        final KeplerianOrbit orbit = createKeplerianEarthOrbit(gcrf, date);

        // Compute the rates using the finite differences implementation
        LLHCoordinatesSystem.ELLIPSODETIC.forceFiniteDifference = true;
        final double[] rates = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, orbit.getPVCoordinates(), gcrf, date);

        // Check result
        TestUtils.assertEquals(8.851239731405558E-4, rates[0], Utils.epsilonTest);
        TestUtils.assertEquals(-4.846251523402123E-4, rates[1], Utils.epsilonTest);
        TestUtils.assertEquals(3.433926892466843, rates[2], Utils.epsilonTest);
    }

    /**
     * Evaluate the rates of the LLH coordinates from the Cartesian position and velocity on a
     * {@link ThreeAxisEllipsoid} using the finite differences estimation.
     * 
     * @testedMethod {@link LLHCoordinatesSystem#computeLLHRates(BodyShape, PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testRatesComputationThreeAxisEllipsoidFiniteDifferences() throws PatriusException {
        // Create Phobos ThreeAxisEllipsoid
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate(2010, 7, 9, 20, 30, 0);
        final ThreeAxisEllipsoid phobos = new ThreeAxisEllipsoid(26.8e3, 22.4e3, 18.4e3, itrf);

        // Create Keplerian orbit around Phobos
        final KeplerianOrbit orbit = createKeplerianPhobosOrbit(gcrf, date);

        // Compute the rates on using the generic interface
        final double[] rates = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(phobos, orbit.getPVCoordinates(), gcrf, date);
        
        // Check results
        TestUtils.assertEquals(2.0589672214410548E-5, rates[0], Utils.epsilonTest);
        TestUtils.assertEquals(-6.679475892668041E-5, rates[1], Utils.epsilonTest);
        TestUtils.assertEquals(0.22773075634177076, rates[2], Utils.epsilonTest);
    }
    
    /**
     * Evaluate the rates of the LLH coordinates from the Cartesian position and velocity on a
     * {@link OneAxisEllipsoid} using the finite differences estimation and the analytical formula.
     * The two results must agree within a reasonable tolerance.
     * The input PV is expressed in GCRF so no further transformation is required during the finite differences computation.
     * 
     * @testedMethod {@link LLHCoordinatesSystem#computeLLHRates(BodyShape, PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testRatesComputationOneAxisEllipsoidAnalyticalVsFiniteDifferencesGCRF()
            throws PatriusException {
        // Create Earth OneAxisEllipsoid
        final double ae = 6378137.0;// GRS80 constant
        final double flattening = 1.0 / 298.257222101;// GRS80 constant
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, flattening, itrf);
        final AbsoluteDate date = new AbsoluteDate(2010, 7, 9, 20, 30, 0);

        // Create orbit around the Earth in GCRF
        final KeplerianOrbit orbit = createKeplerianEarthOrbit(gcrf, date);

        // Compute the rates using the analytical implementation
        final double[] ratesAnalytical = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, orbit.getPVCoordinates(), gcrf, date);

        // Compute the rates using the finite differences implementation
        LLHCoordinatesSystem.ELLIPSODETIC.forceFiniteDifference = true;
        final double[] ratesFiniteDifferences = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, orbit.getPVCoordinates(), gcrf, date);

        // Check that the two results agree with sufficient precision
        for (int i = 0; i < 3; i++) {
            TestUtils.assertEquals(ratesAnalytical[i], ratesFiniteDifferences[i], 1e-7);
        }
    }

    /**
     * Evaluate the rates of the LLH coordinates from the Cartesian position and velocity on a
     * {@link OneAxisEllipsoid} using the finite differences estimation and the analytical formula.
     * The two results must agree within a reasonable tolerance.
     * The input PV is expressed in ITRF.
     * 
     * @testedMethod {@link LLHCoordinatesSystem#computeLLHRates(BodyShape, PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testRatesComputationOneAxisEllipsoidAnalyticalVsFiniteDifferencesITRF()
            throws PatriusException {
        // Create Earth OneAxisEllipsoid
        final double ae = 6378137.0;// GRS80 constant
        final double flattening = 1.0 / 298.257222101;// GRS80 constant
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, flattening, itrf);
        final AbsoluteDate date = new AbsoluteDate(2010, 7, 9, 20, 30, 0);

        // Create orbit around the Earth in GCRF
        final KeplerianOrbit orbit = createKeplerianEarthOrbit(gcrf, date);

        // Create transformation from GCRF to ITRF
        final Transform gcrfToItrf = gcrf.getTransformTo(itrf, date);
        final PVCoordinates pvItrf = gcrfToItrf.transformPVCoordinates(orbit.getPVCoordinates());

        // Compute the rates using the analytical implementation
        final double[] ratesAnalytical = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, pvItrf, itrf, date);

        // Compute the rates using the finite differences implementation
        LLHCoordinatesSystem.ELLIPSODETIC.forceFiniteDifference = true;
        final double[] ratesFiniteDifferences = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, pvItrf, itrf, date);

        // Check that the two results agree with sufficient precision
        for (int i = 0; i < 3; i++) {
            TestUtils.assertEquals(ratesAnalytical[i], ratesFiniteDifferences[i], 1e-7);
        }
    }

    /**
     * Evaluate the rates of the LLH coordinates from the Cartesian position and velocity on a
     * {@link OneAxisEllipsoid} and a {@link ThreeAxisEllipsoid} using the public interface.
     * This tests is mostly here for coverage purposes. The numerical values are checked in other
     * unit test methods.
     * 
     * @testedMethod {@link LLHCoordinatesSystem#computeLLHRates(BodyShape, PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     */
    @Test
    public void testRatesComputation() throws PatriusException {
        // Create Earth OneAxisEllipsoid
        final double ae = 6378137.0;// GRS80 constant
        final double flattening = 1.0 / 298.257222101;// GRS80 constant
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, flattening, itrf);
        final AbsoluteDate date = new AbsoluteDate(2010, 7, 9, 20, 30, 0);

        // Create orbit around the Earth in GCRF
        final KeplerianOrbit orbit = createKeplerianEarthOrbit(gcrf, date);

        // Compute the rates using the generic interface
        final double[] ratesOneAxisEllipsoid = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(earth, orbit.getPVCoordinates(), gcrf, date);

        // ----------------------------------
        
        // Create Phobos ThreeAxisEllipsoid
        final ThreeAxisEllipsoid phobos = new ThreeAxisEllipsoid(26.8e3, 22.4e3, 18.4e3, itrf);

        // Create Keplerian orbit around Phobos
        final KeplerianOrbit orbitAroundPhobos = createKeplerianPhobosOrbit(gcrf, date);

        // Compute the rates on using the generic interface
        final double[] ratesThreeAxisEllipsoid = LLHCoordinatesSystem.ELLIPSODETIC
                .computeLLHRates(phobos, orbitAroundPhobos.getPVCoordinates(), gcrf, date);
    }

    /**
     * Creates a new circular Keplerian orbit around the Earth with an altitude of 700 km at the
     * given date.
     * @param frame Frame in whih the orbit is expressed
     * @param date Date of the initial bulletin
     * @return KeplerianOrbit object
     */
    private KeplerianOrbit createKeplerianEarthOrbit(final CelestialBodyFrame frame,
            final AbsoluteDate date) {
        final double mu = 3.986005e14;// GRS80 constant
        final double ae = 6378137.0;// GRS80 constant
        return new KeplerianOrbit(ae + 700e3, 0.0, 0.5, 1.2, 1.1, 2.3, PositionAngle.TRUE, frame,
                date, mu);
    }

    /**
     * Creates a new circular Keplerian orbit around Phobos with an altitude of around 15 km at the
     * given date.
     * @param frame Frame in whih the orbit is expressed
     * @param date Date of the initial bulletin
     * @return KeplerianOrbit object
     */
    private KeplerianOrbit createKeplerianPhobosOrbit(final CelestialBodyFrame frame,
            final AbsoluteDate date) {
        final double muPhobos = 1.072e16 * 6.6743015e-11;// mass * G
        return new KeplerianOrbit(40e3, 0.0, 0.5, 1.2, 1.1, 2.3, PositionAngle.TRUE, frame, date,
                muPhobos);
    }
    
    /** The following code is executed once before each test: load the resources and set the frames configuration. */
    @Before
    public void setUp() throws PatriusException {
        Utils.clear();
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Comparison component by component of the two matrices.
     * 
     * @param a
     *        first matrix
     * @param b
     *        second matrix
     * @param relTol
     *        relative threshold to use
     * @param absTol
     *        absolute threshold to use
     */
    private static void checkMatrix(final double[][] a, final double[][] b, final double relTol, final double absTol) {
        Assert.assertEquals(a.length, b.length);
        double maxRelDiff = 0.;
        double maxAbsDiff = 0.;
        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i].length, b[i].length);
            for (int j = 0; j < b.length; j++) {
                final double currentA = a[i][j];
                final double currentB = b[i][j];

                final double relDiff;
                if (currentA == 0. || currentB == 0.) {
                    relDiff = 0.;
                } else {
                    final double absMax = MathLib.max(MathLib.abs(currentA), MathLib.abs(currentA));
                    relDiff = MathLib.abs(MathLib.divide((currentA - currentB), absMax));
                }
                if (relDiff > maxRelDiff) {
                    maxRelDiff = relDiff;
                }

                final double absDiff = MathLib.abs(currentA - currentB);
                if (absDiff > maxAbsDiff) {
                    maxAbsDiff = absDiff;
                }
            }
        }
        if (maxRelDiff > relTol || maxAbsDiff > absTol) {
            System.out.println("maxRelDiff: " + maxRelDiff + "\t" + "maxAbsDiff: " + maxAbsDiff);
            Assert.fail();
        }
    }
}
