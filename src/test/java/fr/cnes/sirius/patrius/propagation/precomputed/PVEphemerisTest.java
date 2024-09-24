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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BasicAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris.SearchMethod;
import fr.cnes.sirius.patrius.tools.cache.FIFOThreadSafeCache;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Unit test class for the {@link PVEphemeris} class.
 *
 * @author bonitt
 */
public class PVEphemerisTest {

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Builds a PV ephemeris and compare its interpolation to a keplerian orbit.
     *
     * @testedMethod {@link PVEphemeris#PVEphemeris(List, int, Frame, double, CartesianDerivativesFilter)}
     * @testedMethod {@link PVEphemeris#PVEphemeris(List, int, Frame, double, CartesianDerivativesFilter, AttitudeProvider)}
     * @testedMethod {@link PVEphemeris#PVEphemeris(List, int, Frame, double, CartesianDerivativesFilter, AttitudeProvider, boolean)}
     * @testedMethod {@link PVEphemeris#PVEphemeris(List, int, Frame, double, CartesianDerivativesFilter, AttitudeProvider, boolean, int)}
     * @testedMethod {@link PVEphemeris#propagate(AbsoluteDate)}
     * @testedMethod {@link PVEphemeris#propagateOrbit(AbsoluteDate)}
     * @testedMethod {@link PVEphemeris#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link PVEphemeris#getSpacecraftState(AbsoluteDate)}
     * @testedMethod {@link PVEphemeris#getInitialState()}
     * @testedMethod {@link PVEphemeris#getFirstTimeStampedPVCoordinates()}
     * @testedMethod {@link PVEphemeris#getLastTimeStampedPVCoordinates()}
     * @testedMethod {@link PVEphemeris#getMinDate()}
     * @testedMethod {@link PVEphemeris#getMaxDate()}
     * @testedMethod {@link PVEphemeris#getTimeStampedPVCoordinatesSize()}
     * @testedMethod {@link PVEphemeris#getTimeStampedPVCoordinates(boolean)}
     * @testedMethod {@link PVEphemeris#getCacheReusabilityRatio()}
     * @testedMethod {@link PVEphemeris#isAcceptOutOfOptimalRange()}
     * @testedMethod {@link PVEphemeris#getSearchMethod()}
     * @testedMethod {@link PVEphemeris#setSearchMethod(SearchMethod)}
     *
     * @testPassCriteria The ephemeris is well interpolated compared to the reference.
     */
    @Test
    public void testInterpolation() throws PatriusException {

        // Initialize the reference ephemeris
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initialDate = new AbsoluteDate("2019-01-01T00:00:00", TimeScalesFactory.getTAI());
        final double duration = Constants.JULIAN_DAY;
        final double mu = Constants.EGM96_EARTH_MU;
        final Vector3D pos = new Vector3D(7179992.82, 2276.519, -14178.396);
        final Vector3D vel = new Vector3D(7.450848, -1181.198684, 7356.62864);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel), gcrf, initialDate, mu);
        final PVCoordinatesProvider refEphemeris = new KeplerianPropagator(orbit);
        final CartesianDerivativesFilter filter = CartesianDerivativesFilter.USE_PVA;
        final int order = 8;

        final double thresholdPos = 1e-4; // Expected accuracy on the positions
        final double thresholdVel = 1e-7; // Expected accuracy on the velocities
        final double thresholdAcc = 1e-9; // Expected accuracy on the accelerations

        // Initialize the PVTs used for interpolation described on [initialDate ; initialDate + duration]
        final int size = 100;
        final List<TimeStampedPVCoordinates> pvts = new ArrayList<>(size);
        for (int i = 0; i <= size; i++) {
            final AbsoluteDate currentDate = initialDate.shiftedBy(i * duration / size);
            pvts.add(new TimeStampedPVCoordinates(currentDate, refEphemeris.getPVCoordinates(currentDate, gcrf)));
        }

        // Build the PV ephemeris
        PVEphemeris pvEphemeris =
            new PVEphemeris(pvts, order, gcrf, mu, filter, null, true, FIFOThreadSafeCache.DEFAULT_MAX_SIZE);

        // Evaluate the min/max dates and the initial state
        Assert.assertEquals(pvts.get(0), pvEphemeris.getFirstTimeStampedPVCoordinates());
        Assert.assertEquals(pvts.get(pvts.size() - 1), pvEphemeris.getLastTimeStampedPVCoordinates());
        Assert.assertEquals(initialDate, pvEphemeris.getMinDate());
        Assert.assertEquals(initialDate.shiftedBy(duration), pvEphemeris.getMaxDate());
        Assert.assertEquals(initialDate, pvEphemeris.getInitialState().getDate());
        Assert.assertEquals(pvts.size(), pvEphemeris.getTimeStampedPVCoordinatesSize());
        Assert.assertTrue(Arrays.equals(pvts.toArray(), pvEphemeris.getTimeStampedPVCoordinates(false)));
        Assert.assertTrue(pvEphemeris.isAcceptOutOfOptimalRange());
         Assert.assertEquals(gcrf, pvEphemeris.getFrame());
         Assert.assertEquals(gcrf, pvEphemeris.getNativeFrame(null, FramesFactory.getEME2000()));
        Assert.assertEquals(SearchMethod.PROPORTIONAL, pvEphemeris.getSearchMethod()); // PROPORTIONAL should be by default
        pvEphemeris.setSearchMethod(SearchMethod.DICHOTOMY);
        Assert.assertEquals(SearchMethod.DICHOTOMY, pvEphemeris.getSearchMethod());

        PVCoordinates refEphemerisPV;
        PVCoordinates pvEphemerisPV;

        // Interpolate with a (10 time) lower step on all the duration and compare the PV ephemeris with the reference ephemeris
        final int sizeBis = size * 10;
        for (int i = 0; i <= sizeBis; i++) {
            final AbsoluteDate currentDate = initialDate.shiftedBy(i * duration / sizeBis);
            refEphemerisPV = refEphemeris.getPVCoordinates(currentDate, gcrf);

            // #1: Evaluate the propagate(AbsoluteDate) method
            pvEphemerisPV = pvEphemeris.propagate(currentDate).getPVCoordinates(gcrf);

            Assert.assertTrue(refEphemerisPV.getPosition().distance(pvEphemerisPV.getPosition()) < thresholdPos);
            Assert.assertTrue(refEphemerisPV.getVelocity().distance(pvEphemerisPV.getVelocity()) < thresholdVel);
            Assert.assertTrue(refEphemerisPV.getAcceleration().distance(pvEphemerisPV.getAcceleration()) < thresholdAcc);

            // #2: Evaluate the propagateOrbit(AbsoluteDate) method
            pvEphemerisPV = pvEphemeris.propagateOrbit(currentDate).getPVCoordinates(gcrf);

            Assert.assertTrue(refEphemerisPV.getPosition().distance(pvEphemerisPV.getPosition()) < thresholdPos);
            Assert.assertTrue(refEphemerisPV.getVelocity().distance(pvEphemerisPV.getVelocity()) < thresholdVel);
            Assert.assertTrue(refEphemerisPV.getAcceleration().distance(pvEphemerisPV.getAcceleration()) < thresholdAcc);

            // #3: Evaluate the getPVCoordinates(AbsoluteDate, Frame) method
            pvEphemerisPV = pvEphemeris.getPVCoordinates(currentDate, gcrf);

            Assert.assertTrue(refEphemerisPV.getPosition().distance(pvEphemerisPV.getPosition()) < thresholdPos);
            Assert.assertTrue(refEphemerisPV.getVelocity().distance(pvEphemerisPV.getVelocity()) < thresholdVel);
            Assert.assertTrue(refEphemerisPV.getAcceleration().distance(pvEphemerisPV.getAcceleration()) < thresholdAcc);

            // #4: Evaluate the getSpacecraftState(AbsoluteDate) method
            pvEphemerisPV = pvEphemeris.getSpacecraftState(currentDate).getPVCoordinates(gcrf);

            Assert.assertTrue(refEphemerisPV.getPosition().distance(pvEphemerisPV.getPosition()) < thresholdPos);
            Assert.assertTrue(refEphemerisPV.getVelocity().distance(pvEphemerisPV.getVelocity()) < thresholdVel);
            Assert.assertTrue(refEphemerisPV.getAcceleration().distance(pvEphemerisPV.getAcceleration()) < thresholdAcc);
        }

        // Evaluate the cache reusability ratio value (non regression)
        Assert.assertEquals(0.9776357827476039, pvEphemeris.getCacheReusabilityRatio(), 0.);

        // Evaluate the getPVCoordinates(AbsoluteDate, Frame) method with a frame transformation
        final AbsoluteDate date = initialDate.shiftedBy(153.);

        refEphemerisPV = refEphemeris.getPVCoordinates(date, gcrf);
        pvEphemerisPV = pvEphemeris.getPVCoordinates(date, gcrf);

        Assert.assertTrue(refEphemerisPV.getPosition().distance(pvEphemerisPV.getPosition()) < thresholdPos);
        Assert.assertTrue(refEphemerisPV.getVelocity().distance(pvEphemerisPV.getVelocity()) < thresholdVel);
        Assert.assertTrue(refEphemerisPV.getAcceleration().distance(pvEphemerisPV.getAcceleration()) < thresholdAcc);

        // Evaluate the getPVCoordinates(AbsoluteDate, Frame) method with a null frame
        // Shouldn't transform the ephemeris, default frame of expression of the pvts : GCRF
        refEphemerisPV = refEphemeris.getPVCoordinates(date, gcrf);
        pvEphemerisPV = pvEphemeris.getPVCoordinates(date, null);

        Assert.assertTrue(refEphemerisPV.getPosition().distance(pvEphemerisPV.getPosition()) < thresholdPos);
        Assert.assertTrue(refEphemerisPV.getVelocity().distance(pvEphemerisPV.getVelocity()) < thresholdVel);
        Assert.assertTrue(refEphemerisPV.getAcceleration().distance(pvEphemerisPV.getAcceleration()) < thresholdAcc);

        // Check the attitude provider storage (should be null in the first ephemeris, should be initialized then)
        Assert.assertNull(pvEphemeris.getAttitudeProvider());

        final AttitudeProvider attitudeProv = new BasicAttitudeProvider(new Attitude(initialDate, gcrf, Rotation.IDENTITY, Vector3D.ZERO));
        pvEphemeris = new PVEphemeris(pvts, order, gcrf, mu, filter, attitudeProv);
        Assert.assertEquals(attitudeProv, pvEphemeris.getAttitudeProvider());
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the PV ephemeris exception cases.
     *
     * @testedMethod {@link PVEphemeris#PVEphemeris(List, int, Frame, double, CartesianDerivativesFilter)}
     * @testedMethod {@link PVEphemeris#PVEphemeris(List, int, Frame, double, CartesianDerivativesFilter, AttitudeProvider, boolean, int)}
     * @testedMethod {@link PVEphemeris#resetInitialState(SpacecraftState)}
     * @testedMethod {@link PVEphemeris#propagate(AbsoluteDate)}
     * @testedMethod {@link PVEphemeris#propagateOrbit(AbsoluteDate)}
     * @testedMethod {@link PVEphemeris#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link PVEphemeris#getSpacecraftState(AbsoluteDate)}
     * @testedMethod {@link PVEphemeris#setOrbitFrame(Frame)}
     *
     * @testPassCriteria The expected exceptions are thrown.
     */
    @Test
    public void testPVEphemerisException() throws PatriusException {

        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate initialDate = new AbsoluteDate("2019-01-01T00:00:00", TimeScalesFactory.getTAI());
        final double mu = Constants.EGM96_EARTH_MU;
        final CartesianDerivativesFilter filter = CartesianDerivativesFilter.USE_PVA;
        final List<TimeStampedPVCoordinates> pvts =
            new ArrayList<>(Arrays.asList(new TimeStampedPVCoordinates(initialDate, PVCoordinates.ZERO),
                new TimeStampedPVCoordinates(initialDate.shiftedBy(1.), PVCoordinates.ZERO)));

        // Try to build a PV ephemeris with a null inputs (should fail)
        try {
            new PVEphemeris(null, 2, frame, mu, filter);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PVEphemeris(pvts, 2, null, mu, filter);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PVEphemeris(pvts, 2, frame, mu, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use an order with an odd number or lower than 2 (should fail)
        try {
            new PVEphemeris(pvts, 3, frame, mu, filter);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PVEphemeris(pvts, 0, frame, mu, filter);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use a tabulated time-stamped PVCoordinates array with its length lower than the order (should fail)
        try {
            new PVEphemeris(pvts, 4, frame, mu, filter);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to build a PV ephemeris with a negative cache size (should fail)
        try {
            new PVEphemeris(pvts, 2, frame, mu, filter, null, true, -1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        final PVEphemeris pvEphemeris = new PVEphemeris(pvts, 2, frame, mu, filter); // (shouldn't fail)

        // Try to interpolate the PV ephemeris out of the supported interval (should fail)
        final AbsoluteDate shiftedDate = initialDate.shiftedBy(2.); // Outside the supported interval
        try {
            pvEphemeris.propagate(shiftedDate);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            pvEphemeris.propagateOrbit(shiftedDate);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            pvEphemeris.getPVCoordinates(shiftedDate, frame);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            pvEphemeris.getSpacecraftState(shiftedDate);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to reset the state (not supported feature, should fail)
        final SpacecraftState state = SpacecraftState.getSpacecraftStateLight(AbsoluteDate.J2000_EPOCH);
        try {
            pvEphemeris.resetInitialState(state);
            Assert.fail();
        } catch (final PropagationException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to sort pvt with same dates (should fail)
        final List<TimeStampedPVCoordinates> pvts2 =
            new ArrayList<>(Arrays.asList(new TimeStampedPVCoordinates(initialDate, PVCoordinates.ZERO),
                new TimeStampedPVCoordinates(initialDate.shiftedBy(1.), PVCoordinates.ZERO),
                new TimeStampedPVCoordinates(initialDate.shiftedBy(1.), PVCoordinates.ZERO)));

        try {
            new PVEphemeris(pvts2, 2, frame, mu, filter);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to call the setOrbitFrame(frame) method (not supported, should fail)
        try {
            pvEphemeris.setOrbitFrame(frame);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * The following code is executed once before all the tests : Patrius dataset initialization and frames configuration.
     * 
     * @throws PatriusException
     *         if the EOP data cannot be loaded
     */
    @BeforeClass
    public static void setupClass() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }
}
