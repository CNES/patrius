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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.maneuvers.SmallManeuverAnalyticalModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SmallManeuverAnalyticalModelTest {

    @Test
    public void testLowEarthOrbit1() throws PatriusException {

        final Orbit leo = new CircularOrbit(7200000.0, -1.0e-5, 2.0e-4,
            MathLib.toRadians(98.0),
            MathLib.toRadians(123.456),
            0.0, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(new DateComponents(2004, 01, 01),
                new TimeComponents(23, 30, 00.000),
                TimeScalesFactory.getUTC()),
            Constants.EIGEN5C_EARTH_MU);
        final SimpleMassModel mass = new SimpleMassModel(5600.0, thruster);
        final AbsoluteDate t0 = leo.getDate().shiftedBy(1000.0);
        final Vector3D dV = new Vector3D(-0.01, 0.02, 0.03);
        final double f = 20.0;
        final double isp = 315.0;
        final NumericalPropagator withoutManeuver = this.getPropagator(leo, mass, t0, Vector3D.ZERO, f, isp);
        final NumericalPropagator withManeuver = this.getPropagator(leo, mass, t0, dV, f, isp);
        final SmallManeuverAnalyticalModel model =
            new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0), dV, isp, thruster);
        Assert.assertEquals(t0, model.getDate());

        for (AbsoluteDate t = t0; t.compareTo(t0.shiftedBy(leo.getKeplerianPeriod() * 5)) < 0; t = t.shiftedBy(60.0)) {
            final PVCoordinates pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvWith = withManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvModel = model.apply(withoutManeuver.propagate(t)).getPVCoordinates(leo.getFrame());
            final double nominalDeltaP = new PVCoordinates(pvWith, pvWithout).getPosition().getNorm();
            final double modelError = new PVCoordinates(pvWith, pvModel).getPosition().getNorm();
            if (t.compareTo(t0) < 0) {
                // before maneuver, all positions should be equal
                Assert.assertEquals(0, nominalDeltaP, 1.0e-10);
                Assert.assertEquals(0, modelError, 1.0e-10);
            } else {
                // after maneuver, model error should be less than 0.8m,
                // despite nominal deltaP exceeds 1 kilometer after less than 3 orbits
                if (t.durationFrom(t0) > 0.1 * leo.getKeplerianPeriod()) {
                    Assert.assertTrue(modelError < 0.009 * nominalDeltaP);
                }
                Assert.assertTrue(modelError < 0.8);
            }
        }

    }

    @Test
    public void testLowEarthOrbit2() throws PatriusException {

        final Orbit leo = new CircularOrbit(7200000.0, -1.0e-5, 2.0e-4,
            MathLib.toRadians(98.0),
            MathLib.toRadians(123.456),
            0.0, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(new DateComponents(2004, 01, 01),
                new TimeComponents(23, 30, 00.000),
                TimeScalesFactory.getUTC()),
            Constants.EIGEN5C_EARTH_MU);
        final SimpleMassModel mass = new SimpleMassModel(5600.0, thruster);
        final AbsoluteDate t0 = leo.getDate().shiftedBy(1000.0);
        final Vector3D dV = new Vector3D(-0.01, 0.02, 0.03);
        final double f = 20.0;
        final double isp = 315.0;
        final NumericalPropagator withoutManeuver = this.getPropagator(leo, mass, t0, Vector3D.ZERO, f, isp);
        final NumericalPropagator withManeuver = this.getPropagator(leo, mass, t0, dV, f, isp);
        final SmallManeuverAnalyticalModel model =
            new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0), dV, isp, thruster);
        Assert.assertEquals(t0, model.getDate());

        for (AbsoluteDate t = t0; t.compareTo(t0.shiftedBy(5 * leo.getKeplerianPeriod())) < 0; t = t.shiftedBy(60.0)) {
            final PVCoordinates pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvWith = withManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvModel = model.apply(withoutManeuver.propagate(t).getOrbit()).getPVCoordinates(
                leo.getFrame());
            final double nominalDeltaP = new PVCoordinates(pvWith, pvWithout).getPosition().getNorm();
            final double modelError = new PVCoordinates(pvWith, pvModel).getPosition().getNorm();
            if (t.compareTo(t0) < 0) {
                // before maneuver, all positions should be equal
                Assert.assertEquals(0, nominalDeltaP, 1.0e-10);
                Assert.assertEquals(0, modelError, 1.0e-10);
            } else {
                // after maneuver, model error should be less than 0.8m,
                // despite nominal deltaP exceeds 1 kilometer after less than 3 orbits
                if (t.durationFrom(t0) > 0.1 * leo.getKeplerianPeriod()) {
                    Assert.assertTrue(modelError < 0.009 * nominalDeltaP);
                }
                Assert.assertTrue(modelError < 0.8);
            }
        }

    }

    @Test
    public void testEccentricOrbit() throws PatriusException {

        final Orbit heo = new KeplerianOrbit(90000000.0, 0.92, MathLib.toRadians(98.0),
            MathLib.toRadians(12.3456),
            MathLib.toRadians(123.456),
            MathLib.toRadians(1.23456), PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(new DateComponents(2004, 01, 01),
                new TimeComponents(23, 30, 00.000),
                TimeScalesFactory.getUTC()),
            Constants.EIGEN5C_EARTH_MU);
        final SimpleMassModel mass = new SimpleMassModel(5600.0, thruster);
        final AbsoluteDate t0 = heo.getDate().shiftedBy(1000.0);
        final Vector3D dV = new Vector3D(-0.01, 0.02, 0.03);
        final double f = 20.0;
        final double isp = 315.0;
        final NumericalPropagator withoutManeuver = this.getPropagator(heo, mass, t0, Vector3D.ZERO, f, isp);
        final NumericalPropagator withManeuver = this.getPropagator(heo, mass, t0, dV, f, isp);
        final SmallManeuverAnalyticalModel model =
            new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0), dV, isp, thruster);
        Assert.assertEquals(t0, model.getDate());

        for (AbsoluteDate t = t0; t.compareTo(t0.shiftedBy(2 * heo.getKeplerianPeriod())) < 0; t = t.shiftedBy(600.0)) {
            final PVCoordinates pvWithout = withoutManeuver.getPVCoordinates(t, heo.getFrame());
            final PVCoordinates pvWith = withManeuver.getPVCoordinates(t, heo.getFrame());
            final PVCoordinates pvModel = model.apply(withoutManeuver.propagate(t)).getPVCoordinates(heo.getFrame());
            final double nominalDeltaP = new PVCoordinates(pvWith, pvWithout).getPosition().getNorm();
            final double modelError = new PVCoordinates(pvWith, pvModel).getPosition().getNorm();
            if (t.compareTo(t0) < 0) {
                // before maneuver, all positions should be equal
                Assert.assertEquals(0, nominalDeltaP, 1.0e-10);
                Assert.assertEquals(0, modelError, 1.0e-10);
            } else {
                // after maneuver, model error should be less than 1700m,
                // despite nominal deltaP exceeds 300 kilometers at perigee, after 3 orbits
                if (t.durationFrom(t0) > 0.01 * heo.getKeplerianPeriod()) {
                    Assert.assertTrue(modelError < 0.005 * nominalDeltaP);
                }
                Assert.assertTrue(modelError < 1700);
            }
        }

    }

    private NumericalPropagator getPropagator(final Orbit orbit, final MassProvider mass,
                                              final AbsoluteDate t0, final Vector3D dV,
                                              final double f, final double isp)
                                                                               throws PatriusException {

        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.LVLH,
            RotationOrder.XZY, FastMath.PI / 2.0, -FastMath.PI / 2.0, 0.0);

        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        // set up numerical propagator
        final double dP = 1.0;
        final double[][] tolerances = NumericalPropagator.tolerances(dP, orbit, orbit.getType());
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(orbit.getKeplerianPeriod() / 100.0);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(orbit.getType());
        propagator.setInitialState(initialState);
        propagator.addAdditionalEquations(mass.getAdditionalEquation("thruster"));
        propagator.setAttitudeProvider(law);

        if (dV.getNorm() > 1.0e-6) {
            // set up maneuver
            final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
            final double dt = -(mass.getMass(thruster) * vExhaust / f) * MathLib.expm1(-dV.getNorm() / vExhaust);
            final TankProperty tank = new TankProperty(mass.getMass(thruster));
            tank.setPartName(thruster);
            final ContinuousThrustManeuver maneuver =
                new ContinuousThrustManeuver(t0, dt, new PropulsiveProperty(f, isp), dV.normalize(), mass, tank);
            propagator.addForceModel(maneuver);
        }

        return propagator;
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataPBASE");
    }

    private static final String thruster = "thruster";
}
