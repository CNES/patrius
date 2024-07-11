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
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author chabaudp
 */
public class AbstractAttitudeProviderTest {

    /**
     * Spin numerical computation step = step<sub>AOCS</sub> / 4
     */
    private static final double COMPUTATION_STEP = 0.0625;

    /**
     * Central Tolerance : O(h<sup>2</sup>)
     */
    private static final double TOLERANCE_CENTRAL = 5.E-12;

    /** attitude provider. */
    private final MyAttitudeProvider attProv = new MyAttitudeProvider();
    /** date. */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** Frame. */
    private final Frame frame = FramesFactory.getGCRF();
    /** Keplerian orbit. */
    private final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(60), 0, 0, 0,
        PositionAngle.TRUE, this.frame, this.date, Constants.EGM96_EARTH_MU);

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw#getSpin(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, fr.cnes.sirius.patrius.time.AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * .
     * 
     * @throws PatriusException
     *         should not happens
     */
    @Test
    public final void testGetSpin() throws PatriusException {

        final Vector3D spin = this.attProv.getAttitude(this.orbit, this.date, this.frame).getSpin();
        Assert.assertEquals(0, spin.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, fr.cnes.sirius.patrius.time.AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * .
     * 
     * @throws PatriusException
     *         should not happens
     */
    @Test
    public final void testGetAttitudeWithoutSpinDerivatives() throws PatriusException {
        final Attitude att = this.attProv.getAttitude(this.orbit, this.date, this.frame);

        Assert.assertTrue(att.getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertEquals(0, att.getSpin().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        try {
            att.getRotationAcceleration();
        } catch (final PatriusException e) {
            Assert.assertEquals("Exception attendue : rotation acceleration not available",
                "Spin derivatives are not available for this attitude", e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, fr.cnes.sirius.patrius.time.AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * .
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetAttitudeWithSpinDerivatives() throws PatriusException {
        this.attProv.setSpinDerivativesComputation(true);
        final Attitude att = this.attProv.getAttitude(this.orbit, this.date, this.frame);

        Assert.assertTrue(att.getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertEquals(0, att.getSpin().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
        final Vector3D attAcceleration = att.getRotationAcceleration();
        Assert.assertEquals(0, attAcceleration.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider)}
     * .
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetAttitude() throws PatriusException {

        final Attitude att = this.attProv.getAttitude(this.orbit);
        final Attitude att2 = this.attProv.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame());

        Assert.assertEquals(att.getDate(), att2.getDate());
        Assert.assertEquals(att.getReferenceFrame(), att2.getReferenceFrame());
        Assert.assertTrue(att.getRotation().isEqualTo(att2.getRotation()));
        Assert.assertEquals(0, att.getSpin().getNorm(), att2.getSpin().getNorm());
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link AttitudeProvider#computeSpinDerivativeByFD(PVCoordinatesProvider, Frame, AbsoluteDate, double)}
     * 
     * @description
     *              <p>
     *              The Rotation Acceleration Numerical computer method
     *              </p>
     *              <p>
     *              Two AttitudeLegs and AttitudeLaws are defined. Their rotation accelerations are computed numerically and are compared to
     *              their analytical rotation acceleration on a whole orbital period.
     *              </p>
     *
     * @testPassCriteria Acceleration vectors obtained are equal to the expected ideal accelerations (absolute tolerance: 1E-12)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testComputeSpinDerivativeByFD() throws PatriusException {

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
        final BodyShape body = new OneAxisEllipsoid(6378, 0.001, FramesFactory.getGCRF());
        final AttitudeLaw attLaw = new NadirPointing(body);
        final AttitudeLeg attLeg = new AttitudeLawLeg(attLaw, startInterval, endInterval);

        // Set the computation of the acceleration to true for the ideal law and leg
        attLaw.setSpinDerivativesComputation(true);
        attLeg.setSpinDerivativesComputation(true);

        final double testStep = 60.0;
        final Frame frame = FramesFactory.getEME2000();
        AbsoluteDate currentDate = startInterval;
        while (currentDate.compareTo(endInterval) <= 0) {
            // Invoke method to be tested
            final Vector3D attLegNumericalAcce =
                    attLeg.computeSpinDerivativeByFD(pvProv, frame, currentDate, COMPUTATION_STEP);
            final Vector3D attLawNumericalAcc =
                    attLaw.computeSpinDerivativeByFD(pvProv, frame, currentDate, COMPUTATION_STEP);

            // Compute expected values
            final Vector3D attLegIdealAcc = attLeg.getAttitude(pvProv, currentDate, frame).getRotationAcceleration();
            final Vector3D attLawIdealAcc = attLaw.getAttitude(pvProv, currentDate, frame).getRotationAcceleration();

            // Assert results
            Assert.assertEquals(0., attLegIdealAcc.distance(attLegNumericalAcce), TOLERANCE_CENTRAL);
            Assert.assertEquals(0., attLawIdealAcc.distance(attLawNumericalAcc), TOLERANCE_CENTRAL);

            // Advance date
            currentDate = currentDate.shiftedBy(testStep);
        }

        // Invoke method to be tested at the end date of the leg interval
        final Vector3D attLegNumericalAccEnd =
                attLeg.computeSpinDerivativeByFD(pvProv, frame, endInterval, COMPUTATION_STEP);
        final Vector3D attLegIdealAccEnd = attLeg.getAttitude(pvProv, endInterval, frame).getRotationAcceleration();

        // Assert result
        Assert.assertEquals(0., attLegIdealAccEnd.distance(attLegNumericalAccEnd), TOLERANCE_CENTRAL);
        
        // Degenerated case: leg interval shorter than FD step
        final AttitudeLeg attLeg2 = new AttitudeLawLeg(attLaw, startInterval, startInterval.shiftedBy(1E-2));
        final AbsoluteDate date = startInterval.shiftedBy(0.5E-2);
        final Vector3D attLegNumericalAcce2 =
                attLeg2.computeSpinDerivativeByFD(pvProv, frame, date, COMPUTATION_STEP);
        final Vector3D attLegIdealAcc2 = attLeg2.getAttitude(pvProv, date, frame).getRotationAcceleration();
        Assert.assertEquals(0., attLegIdealAcc2.distance(attLegNumericalAcce2), TOLERANCE_CENTRAL);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link AttitudeProvider#computeSpinByFD(PVCoordinatesProvider, Frame, AbsoluteDate, double)}
     * 
     * @description
     *              <p>
     *              The spin Numerical computer method
     *              </p>
     *              <p>
     *              Two AttitudeLegs and AttitudeLaws are defined. Their spins are computed numerically and are compared to
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
        final BodyShape body = new OneAxisEllipsoid(6378, 0.001, FramesFactory.getGCRF());
        final AttitudeLaw attLaw = new NadirPointing(body);
        final AttitudeLeg attLeg = new AttitudeLawLeg(attLaw, startInterval, endInterval);

        // Set the computation of the acceleration to true for the ideal law and leg
        attLaw.setSpinDerivativesComputation(true);
        attLeg.setSpinDerivativesComputation(true);

        final double testStep = 60.0;
        final Frame frame = FramesFactory.getEME2000();
        AbsoluteDate currentDate = startInterval;
        while (currentDate.compareTo(endInterval) <= 0) {
            // Invoke method to be tested
            final Vector3D attLegNumericalAcce =
                    attLeg.computeSpinByFD(pvProv, frame, currentDate, COMPUTATION_STEP);
            final Vector3D attLawNumericalAcc =
                    attLaw.computeSpinByFD(pvProv, frame, currentDate, COMPUTATION_STEP);

            // Compute expected values
            final Vector3D attLegIdealAcc = attLeg.getAttitude(pvProv, currentDate, frame).getSpin();
            final Vector3D attLawIdealAcc = attLaw.getAttitude(pvProv, currentDate, frame).getSpin();

            // Assert results
            Assert.assertEquals(0., attLegIdealAcc.distance(attLegNumericalAcce), 1E-10);
            Assert.assertEquals(0., attLawIdealAcc.distance(attLawNumericalAcc), 1E-10);

            // Advance date
            currentDate = currentDate.shiftedBy(testStep);
        }

        // Invoke method to be tested at the end date of the leg interval
        final Vector3D attLegNumericalAccEnd =
                attLeg.computeSpinByFD(pvProv, frame, endInterval, COMPUTATION_STEP);
        final Vector3D attLegIdealAccEnd = attLeg.getAttitude(pvProv, endInterval, frame).getSpin();

        // Assert result
        Assert.assertEquals(0., attLegIdealAccEnd.distance(attLegNumericalAccEnd), 1E-10);
        
        // Degenerated case: leg interval shorter than FD step
        final AttitudeLeg attLeg2 = new AttitudeLawLeg(attLaw, startInterval, startInterval.shiftedBy(1E-2));
        final AbsoluteDate date = startInterval.shiftedBy(0.5E-2);
        final Vector3D attLegNumericalAcce2 =
                attLeg2.computeSpinByFD(pvProv, frame, date, COMPUTATION_STEP);
        final Vector3D attLegIdealAcc2 = attLeg2.getAttitude(pvProv, date, frame).getSpin();
        Assert.assertEquals(0., attLegIdealAcc2.distance(attLegNumericalAcce2), 1E-3);
    }

    /**
     * Extends abstract class to test it.
     * 
     * @author chabaudp
     */
    public class MyAttitudeProvider extends AbstractAttitudeLaw {

        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            return new Attitude(date, frame, AngularCoordinates.IDENTITY);
        }

    }
}
