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
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::FA:416:12/02/2015:Changed EcksteinHechlerPropagator constructor signature
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.precomputed;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class EphemerisEventsTest {

    @Test
    public void testEphemKeplerian() throws IllegalArgumentException, PatriusException {
        this.checkEphem(OrbitType.KEPLERIAN);
    }

    @Test
    public void testEphemCircular() throws IllegalArgumentException, PatriusException {
        this.checkEphem(OrbitType.CIRCULAR);
    }

    @Test
    public void testEphemEquinoctial() throws IllegalArgumentException, PatriusException {
        this.checkEphem(OrbitType.EQUINOCTIAL);
    }

    @Test
    public void testEphemCartesian() throws IllegalArgumentException, PatriusException {
        this.checkEphem(OrbitType.CARTESIAN);
    }

    // Coverage test for Ephemeris class
    @Test
    public void testEphemeris() throws IllegalArgumentException, PatriusException {
        this.initDate = new AbsoluteDate();
        final Orbit orbit = new KeplerianOrbit(7188000, 0.01, 1.65, 0.3, 1.23, 0.0, PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.initDate, 3.9860047e14);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state0 = new SpacecraftState(orbit, attitude);
        final SpacecraftState state1 = state0.shiftedBy(100.0);
        final SpacecraftState state2 = state0.shiftedBy(200.0);
        final List<SpacecraftState> states = new ArrayList<>();
        states.add(state0);
        states.add(state1);
        states.add(state2);
        // constructor exception:
        boolean rez = false;
        try {
            new Ephemeris(states, 5);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        final Ephemeris ephms = new Ephemeris(states, 3);
        // propagateOrbit coverage + assert on PV:
        final PVCoordinates pv = ephms.propagateOrbit(this.initDate).getPVCoordinates();
        final PVCoordinates pvExp = orbit.getPVCoordinates();
        Assert.assertEquals(0., pv.getPosition().subtract(pvExp.getPosition()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0., pv.getVelocity().subtract(pvExp.getVelocity()).getNorm(), Precision.EPSILON);

        // getPVCoordinates coverage + assert on PV:
        Assert.assertEquals(0,
            ephms.getPVCoordinates(this.initDate, FramesFactory.getEME2000()).getPosition()
                .subtract(pvExp.getPosition()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0,
            ephms.getPVCoordinates(this.initDate, FramesFactory.getEME2000()).getVelocity()
                .subtract(pvExp.getVelocity()).getNorm(), Precision.EPSILON);
        
        // getSpacecraftState coverage + assert on SpacecraftState:
        final SpacecraftState state = ephms.getSpacecraftState(this.initDate);
        Assert.assertEquals(0,
            ephms.getSpacecraftState(this.initDate).getPVCoordinates().getPosition()
                .subtract(state.getPVCoordinates().getPosition()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0,
            ephms.getSpacecraftState(this.initDate).getPVCoordinates().getVelocity()
                .subtract(state.getPVCoordinates().getVelocity()).getNorm(), Precision.EPSILON);

        // resetInitialState exception:
        rez = false;
        try {
            ephms.resetInitialState(state2);
            Assert.fail();
        } catch (final PropagationException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        
        // basicPropagateOrbit exception coverage:
        rez = false;
        try {
            ephms.basicPropagateOrbit(AbsoluteDate.JULIAN_EPOCH);
            Assert.fail();
        } catch (final PropagationException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    private Ephemeris buildEphem(final OrbitType type)
                                                      throws IllegalArgumentException, PatriusException {

        final double mass = 2500;
        final double a = 7187990.1979844316;
        final double e = 0.5e-4;
        final double i = 1.7105407051081795;
        final double omega = 1.9674147913622104;
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;
        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final double deltaT = this.finalDate.durationFrom(this.initDate);

        final Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.initDate, mu);
        final SimpleMassModel massModel = new SimpleMassModel(mass, "default");
        final int nbIntervals = 720;
        final Propagator propagator =
            new EcksteinHechlerPropagator(transPar, ae, mu, transPar.getFrame(), c20, c30, c40, c50, c60,
                massModel, ParametersType.OSCULATING);

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));

        final List<SpacecraftState> tab = new ArrayList<>(nbIntervals + 1);
        for (int j = 0; j <= nbIntervals; j++) {
            final SpacecraftState state = propagator.propagate(this.initDate.shiftedBy((j * deltaT) / nbIntervals));
            tab.add(new SpacecraftState(type.convertType(state.getOrbit()), state.getAttitudeForces(),
                state.getAttitudeEvents(), state.getAdditionalStates()));
        }

        return new Ephemeris(tab, 2);
    }

    private EclipseDetector buildEclipsDetector(final OrbitType type) throws PatriusException {

        final double sunRadius = 696000000.;
        final double earthRadius = 6400000.;

        final EclipseDetector ecl = new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
            CelestialBodyFactory.getEarth(), earthRadius, 0, 60., 1.e-3){

            /** Serializable UID. */
            private static final long serialVersionUID = 3271690471051105212L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                Assert.assertEquals(type, s.getOrbit().getType());
                if (increasing) {
                    ++EphemerisEventsTest.this.inEclipsecounter;
                } else {
                    ++EphemerisEventsTest.this.outEclipsecounter;
                }
                return Action.CONTINUE;
            }
        };

        return ecl;
    }

    private void checkEphem(final OrbitType type)
                                                 throws IllegalArgumentException, PatriusException {

        this.initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        this.finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final BoundedPropagator ephem = this.buildEphem(type);

        ephem.addEventDetector(this.buildEclipsDetector(type));

        final AbsoluteDate computeEnd = new AbsoluteDate(this.finalDate, -1000.0);

        ephem.setSlaveMode();
        final SpacecraftState state = ephem.propagate(computeEnd);
        Assert.assertEquals(computeEnd, state.getDate());
        Assert.assertEquals(14, this.inEclipsecounter);
        Assert.assertEquals(14, this.outEclipsecounter);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        this.inEclipsecounter = 0;
        this.outEclipsecounter = 0;
    }

    private AbsoluteDate initDate;
    private AbsoluteDate finalDate;
    private int inEclipsecounter;
    private int outEclipsecounter;

}
