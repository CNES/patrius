/**
 * 
 * Copyright 2011-2018 CNES
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:DM:1872:10/12/2018 MultiAttitudeProvider pour lois d'attitude relatives
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.multi;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.events.sensor.SatToSatMutualVisibilityTest;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Validation class for {@link MultiNumericalPropagatorTestAttitude}
 * </p>
 * 
 * @see SatToSatMutualVisibilityTest
 * @see MultiAttitudeProvider
 * 
 * @author Stefano Crepaldi
 * 
 * @version $Id$
 * 
 * @since 4.2
 * 
 */
public class MultiAttitudeProviderTest {

    /**
     * First state name
     */
    private static final String STATE1 = "state1";

    /**
     * Second state name
     */
    private static final String STATE2 = "state2";

    /**
     * First numerical propagator
     */
    private NumericalPropagator firstPropagator;

    /**
     * Second numerical propagator
     */
    private NumericalPropagator secondPropagator;

    /**
     * Integrator defined for two states.
     */
    private AdaptiveStepsizeIntegrator integratorMultiSat;

    /**
     * Multi-sat numerical propagator
     */
    private MultiNumericalPropagator multiNumericalPropagator;

    /**
     * Initial date
     */
    private AbsoluteDate initialDate;

    /**
     * Final date
     */
    private AbsoluteDate finalDate;

    /**
     * MU
     */
    private double mu;

    /**
     * First orbit
     */
    private Orbit orbit1;

    /**
     * First state
     */
    private SpacecraftState state1;

    /**
     * Second orbit
     */
    private Orbit orbit2;

    /**
     * Second state
     */
    private SpacecraftState state2;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test methods
         * 
         * @featureDescription Test methods behavior (no propagation)
         */
        METHODS,

        /**
         * @featureTitle Multi-sat numerical propagation mode
         * 
         * @featureDescription test multi-sat numerical propagation modes (slave, ephemeris, master)
         * 
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION_MODE,
        /**
         * @featureTitle Events detection in multi-sat numerical propagation
         * 
         * @featureDescription test events detection during multi-sat numerical propagation (events
         *                     added to the propagator and events associated with forces model)
         * 
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION_EVENTS,

        /**
         * @featureTitle Test different integrator
         * 
         * @featureDescription test propagation with a scalar or a vectorial integrator
         * 
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION_INTEGRATOR,

        /**
         * @featureTitle Multi-sat numerical propagation : general features
         * 
         * @featureDescription Test general features of multi-sat numerical propagation
         * 
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION

    }

    @Test
    public final void testMultiAttitudeProvider() throws PatriusException {
        // Initializations
        Utils.setDataRoot("regular-dataPBASE");

        // Numerical propagator definition
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6,
            1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7,
            1.0e-7 };
        final double minstep = 0.001;
        final double maxstep = 200.;
        final AdaptiveStepsizeIntegrator integrator1 = new DormandPrince853Integrator(
            minstep, maxstep, absTolerance, relTolerance);
        final AdaptiveStepsizeIntegrator integrator2 = new DormandPrince853Integrator(
            minstep, maxstep, absTolerance, relTolerance);
        this.integratorMultiSat = new DormandPrince853Integrator(minstep, maxstep,
            absTolerance, relTolerance);

        this.firstPropagator = new NumericalPropagator(integrator1);
        this.secondPropagator = new NumericalPropagator(integrator2);
        this.multiNumericalPropagator = new MultiNumericalPropagator(
            this.integratorMultiSat);

        // Start date
        this.initialDate = AbsoluteDate.J2000_EPOCH;

        // Final date
        double dt = 100.;
        this.finalDate = this.initialDate.shiftedBy(dt);

        // Define MU
        this.mu = Utils.mu;

        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 400e3;
        final double e = .0001;
        final double i = 60 * 3.14 / 180;
        final double raan = 0;
        final double pa = 0 * 3.14 / 180;
        final double w1 = 0;
        this.orbit1 =
            new KeplerianOrbit(a, e, i, pa, raan, w1, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initialDate,
                this.mu);
        this.orbit1.getKeplerianPeriod();
        this.state1 = new SpacecraftState(this.orbit1);

        // Second Spacecraft definition
        // state
        final double w2 = 10 * 3.14 / 180;
        this.orbit2 =
            new KeplerianOrbit(a, e, i, pa, raan, w2, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initialDate,
                this.mu);
        this.state2 = new SpacecraftState(this.orbit2);

        final Map<String, Orbit> orbits = new HashMap<String, Orbit>();
        orbits.put(STATE1, this.orbit1);
        orbits.put(STATE2, this.orbit2);

        // Add initial state
        this.firstPropagator.setInitialState(this.state1);
        this.secondPropagator.setInitialState(this.state2);
        this.multiNumericalPropagator.addInitialState(this.state1, STATE1);
        this.multiNumericalPropagator.addInitialState(this.state2, STATE2);

        // Add attitude provider associated with the second propagator
        this.multiNumericalPropagator.setAttitudeProvider(new MyMultiAttitudeProvider(), STATE1);
        this.multiNumericalPropagator.setAttitudeProvider(new BodyCenterPointing(), STATE2);

        // Propagation 1, final State is with x > 0
        this.multiNumericalPropagator.propagate(this.finalDate);
        final PVCoordinates coordFin11 =
            this.multiNumericalPropagator.getPVCoordinates(this.finalDate, this.orbit1.getFrame(), STATE1);
        final PVCoordinates coordFin21 =
            this.multiNumericalPropagator.getPVCoordinates(this.finalDate, this.orbit2.getFrame(), STATE2);
        final Orbit orbitFin11 = new CartesianOrbit(coordFin11, this.orbit1.getFrame(), this.finalDate, this.mu);
        final Orbit orbitFin21 = new CartesianOrbit(coordFin21, this.orbit2.getFrame(), this.finalDate, this.mu);
        final Map<String, Orbit> orbitsFin1 = new HashMap<String, Orbit>();
        orbitsFin1.put(STATE1, orbitFin11);
        orbitsFin1.put(STATE2, orbitFin21);
        final Rotation att1 =
            this.multiNumericalPropagator.getAttitudeProvider(STATE1).getAttitude(orbitsFin1).getRotation();
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(att1, Rotation.IDENTITY)), 1e-8);

        // Propagation 2, final State is with x < 0
        dt = 2000;
        this.finalDate = this.initialDate.shiftedBy(dt);
        this.multiNumericalPropagator.propagate(this.finalDate);
        final PVCoordinates coordFin12 =
            this.multiNumericalPropagator.getPVCoordinates(this.finalDate, this.orbit1.getFrame(), STATE1);
        final PVCoordinates coordFin22 =
            this.multiNumericalPropagator.getPVCoordinates(this.finalDate, this.orbit2.getFrame(), STATE2);
        final Orbit orbitFin12 = new CartesianOrbit(coordFin12, this.orbit1.getFrame(), this.finalDate, this.mu);
        final Orbit orbitFin22 = new CartesianOrbit(coordFin22, this.orbit2.getFrame(),
            this.finalDate, this.mu);
        final Map<String, Orbit> orbitsFin2 = new HashMap<String, Orbit>();
        orbitsFin2.put(STATE1, orbitFin12);
        orbitsFin2.put(STATE2, orbitFin22);
        final Rotation att2 =
            this.multiNumericalPropagator.getAttitudeProvider(STATE1).getAttitude(orbitsFin2).getRotation();
        final Rotation att2Exp = new BodyCenterPointing().getAttitude(orbitFin12).getRotation();
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(att2Exp, att2)), 1e-8);

        // Same check (for last attitude) but with other method
        final Map<String, PVCoordinatesProvider> pvFin2 = new HashMap<String, PVCoordinatesProvider>();
        pvFin2.put(STATE1, orbitFin12);
        pvFin2.put(STATE2, orbitFin22);
        final Rotation att3 =
            this.multiNumericalPropagator.getAttitudeProvider(STATE1)
                .getAttitude(pvFin2, orbitFin12.getDate(), orbitFin12.getFrame()).getRotation();
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(att2Exp, att3)), 1e-8);
    }

    private class MyMultiAttitudeProvider implements MultiAttitudeProvider {

        @Override
        public Attitude getAttitude(final Map<String, Orbit> orbits)
                                                                    throws PatriusException {
            final Orbit orbit = orbits.get(STATE1);
            if (orbit.getPVCoordinates().getPosition().getX() < 0) {
                return new BodyCenterPointing().getAttitude(orbit);
            } else {
                return new Attitude(orbit.getDate(), orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
            }
        }

        @Override
        public Attitude getAttitude(final Map<String, PVCoordinatesProvider> pvProvs,
                                    final AbsoluteDate date, final Frame frame) throws PatriusException {
            final Orbit orbit = (Orbit) pvProvs.get(STATE1);
            if (orbit.getPVCoordinates().getPosition().getX() < 0) {
                return new BodyCenterPointing().getAttitude(orbit);
            } else {
                return new Attitude(orbit.getDate(), orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
            }
        }
    };

}
