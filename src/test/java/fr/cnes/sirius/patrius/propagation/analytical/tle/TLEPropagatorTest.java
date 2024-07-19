/**
 * Copyright 2002-2012 CS Communication & Systèmes
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:449:10/08/2015:Added error if attitudeForces == null and attitudeEvents != null
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class TLEPropagatorTest {

    private TLE tle;
    private double period;

    /** Default mass name. */
    private final String DEFAULT = "DEFAULT";

    /** Default mass. */
    private MassProvider mass = new SimpleMassModel(1000.0, this.DEFAULT);

    @Test
    public void testSlaveMode() throws PatriusException {

        final TLEPropagator propagator = TLEPropagator
                .selectExtrapolator(this.tle, null, this.mass);
        final AbsoluteDate initDate = this.tle.getDate();
        final SpacecraftState initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(this.period));

        // Check results
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assert.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);
        Assert.assertEquals(initialState.getMass("DEFAULT"), finalState.getMass("DEFAULT"), 0.0);
    }

    @Test
    public void testEphemerisMode() throws PatriusException {

        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(this.tle);
        propagator.setEphemerisMode();

        final AbsoluteDate initDate = this.tle.getDate();
        final SpacecraftState initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        final AbsoluteDate endDate = initDate.shiftedBy(this.period);
        propagator.propagate(endDate);

        // get the ephemeris
        final BoundedPropagator boundedProp = propagator.getGeneratedEphemeris();

        // get the initial state from the ephemeris and check if it is the same as
        // the initial state from the TLE
        final SpacecraftState boundedState = boundedProp.propagate(initDate);

        // Check results
        Assert.assertEquals(initialState.getA(), boundedState.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), boundedState.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), boundedState.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), boundedState.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), boundedState.getHy(), 0.);
        Assert.assertEquals(initialState.getLM(), boundedState.getLM(), 1e-14);

        final SpacecraftState finalState = boundedProp.propagate(endDate);

        // Check results
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assert.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);
    }

    /**
     * Test if body center belongs to the direction pointed by the satellite. The threshold for the
     * first
     * Assert.assertEquals has been modified (from 2.0e-7 to 4.0e-7) to avoid an error. The
     * threshold was modified again
     * during the D-1006 to 6e-7.
     * 
     * A dedicated validation test with external references exists in patrius-tools package
     * fr.cnes.sirius.patrius.tools.tle.validation. The aforementioned test includes a non
     * regression mechanism and
     * ensures the modifications introduced during the D-1006 have no effect on the test cases
     * implemented therein.
     */
    @Test
    public void testBodyCenterInPointingDirection() throws PatriusException {

        final Frame itrf = FramesFactory.getITRF();
        final DistanceChecker checker = new DistanceChecker(itrf);

        // with Earth pointing attitude, distance should be small
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(this.tle,
                new BodyCenterPointing(itrf), this.mass);
        propagator.setMasterMode(900.0, checker);
        propagator.propagate(this.tle.getDate().shiftedBy(this.period));
        Assert.assertEquals(0.0, checker.getMaxDistance(), 6.0e-7);

        // results should be the same as previous
        propagator = TLEPropagator.selectExtrapolator(this.tle, new BodyCenterPointing(itrf),
                new BodyCenterPointing(itrf), this.mass);
        propagator.setMasterMode(900.0, checker);
        propagator.propagate(this.tle.getDate().shiftedBy(this.period));
        Assert.assertEquals(0.0, checker.getMaxDistance(), 6.0e-7);

        // with default attitude mode, distance should be large
        propagator = TLEPropagator.selectExtrapolator(this.tle);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(),
                Rotation.IDENTITY));
        propagator.setMasterMode(900.0, checker);
        propagator.propagate(this.tle.getDate().shiftedBy(this.period));
        Assert.assertEquals(1.5219e7, checker.getMinDistance(), 1000.0);
        Assert.assertEquals(2.6572e7, checker.getMaxDistance(), 1000.0);
    }

    private static class DistanceChecker implements PatriusFixedStepHandler {

        private static final long serialVersionUID = -7778088499864710110L;

        private final Frame itrf;
        private double minDistance;
        private double maxDistance;

        public DistanceChecker(final Frame itrf) {
            this.itrf = itrf;
        }

        public double getMinDistance() {
            return this.minDistance;
        }

        public double getMaxDistance() {
            return this.maxDistance;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.minDistance = Double.POSITIVE_INFINITY;
            this.maxDistance = Double.NEGATIVE_INFINITY;
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
            try {
                // Get satellite attitude rotation, i.e rotation from inertial frame to satellite
                // frame
                final Rotation rotSat = currentState.getAttitude().getRotation();

                // Transform Z axis from satellite frame to inertial frame
                final Vector3D zSat = rotSat.applyTo(Vector3D.PLUS_K);

                // Transform Z axis from inertial frame to ITRF
                final Transform transform = currentState.getFrame().getTransformTo(this.itrf,
                        currentState.getDate());
                final Vector3D zSatITRF = transform.transformVector(zSat);

                // Transform satellite position/velocity from inertial frame to ITRF
                final PVCoordinates pvSatITRF = transform.transformPVCoordinates(currentState
                        .getPVCoordinates());

                // Line containing satellite point and following pointing direction
                final Line pointingLine = new Line(pvSatITRF.getPosition(), pvSatITRF.getPosition()
                        .add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, zSatITRF));

                final double distance = pointingLine.distance(Vector3D.ZERO);
                this.minDistance = MathLib.min(this.minDistance, distance);
                this.maxDistance = MathLib.max(this.maxDistance, distance);

            } catch (final PatriusException oe) {
                throw new PropagationException(oe);
            }
        }
    }

    // This test checks that a mass change is taken into account by the propagator
    // (the spacecraft mass is updated):
    @Test
    public void propagationWithMassChange() throws PatriusException {
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(this.tle, null, this.mass);
        final AbsoluteDate initDate = this.tle.getDate();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(this.period));

        // Get the initial state:
        SpacecraftState state0 = propagator.getInitialState();
        // Check the value of the final mass as an additional state (no maneuver --> it should not
        // have changed):
        Assert.assertEquals(state0.getMass(this.DEFAULT), finalState.getMass(this.DEFAULT), 0.0);

        // Re-run the same test, adding an impulse maneuver to the propagator:
        this.mass = new SimpleMassModel(1000.0, this.DEFAULT);
        propagator = TLEPropagator.selectExtrapolator(this.tle, new ConstantAttitudeLaw(
                FramesFactory.getEME2000(), Rotation.IDENTITY), this.mass);
        // Get the initial state:
        state0 = propagator.getInitialState();
        // Change mass value;
        this.mass.updateMass(this.DEFAULT, 800.);
        // Perform the propagation:
        final SpacecraftState stateEnd = propagator.propagate(initDate.shiftedBy(this.period));
        // Check final mass value
        // Check the value of the final mass as an additional state:
        Assert.assertEquals(800, stateEnd.getMass(this.DEFAULT), 0.0);
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the TLE propagator serialization / deserialization process.
     *
     * @testPassCriteria The TLE propagator can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final AbsoluteDate initDate = this.tle.getDate();
        final Frame frame = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        final AttitudeProvider attitudeProvider = new BodyCenterPointing(itrf);
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("thruster");
        final TankProperty p1 = new TankProperty(9000.);
        builder.addProperty(p1, "thruster");
        final MassModel massModel = new MassModel(builder.returnAssembly());

        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(this.tle,
                attitudeProvider, attitudeProvider, massModel);
        final TLEPropagator deserializedPropagator = TestUtils.serializeAndRecover(propagator);

        for (int i = 0; i < 10; i++) {
            final AbsoluteDate currentDate = initDate.shiftedBy(i);
            Assert.assertEquals(propagator.getPVCoordinates(currentDate, frame),
                    deserializedPropagator.getPVCoordinates(currentDate, frame));
        }
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // setup a TLE for a GPS satellite
            final String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
            final String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";

            this.tle = new TLE(line1, line2);

            // the period of the GPS satellite
            this.period = 717.97 * 60.0;

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
