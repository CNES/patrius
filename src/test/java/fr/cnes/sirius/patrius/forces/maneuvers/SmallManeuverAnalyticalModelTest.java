/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
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
    public void testJacobian() throws PatriusException {

        final Frame eme2000 = FramesFactory.getEME2000();
        final Orbit leo = new CircularOrbit(7200000.0, -1.0e-2, 2.0e-3,
            MathLib.toRadians(98.0),
            MathLib.toRadians(123.456),
            0.3, PositionAngle.MEAN,
            eme2000,
            new AbsoluteDate(new DateComponents(2004, 01, 01),
                new TimeComponents(23, 30, 00.000),
                TimeScalesFactory.getUTC()),
            Constants.EIGEN5C_EARTH_MU);
        final SimpleMassModel mass = new SimpleMassModel(5600.0, thruster);
        final AbsoluteDate t0 = leo.getDate().shiftedBy(1000.0);
        final Vector3D dV0 = new Vector3D(-0.1, 0.2, 0.3);
        final double f = 400.0;
        final double isp = 315.0;

        for (final OrbitType orbitType : OrbitType.values()) {
            for (final PositionAngle positionAngle : PositionAngle.values()) {
                final NumericalPropagator withoutManeuver = this.getPropagator(orbitType.convertType(leo), mass, t0,
                    Vector3D.ZERO, f, isp);

                final SpacecraftState state0 = withoutManeuver.propagate(t0);
                final SmallManeuverAnalyticalModel model = new SmallManeuverAnalyticalModel(state0, eme2000, dV0, isp,
                    thruster);
                Assert.assertEquals(t0, model.getDate());

                final Vector3D[] velDirs =
                    new Vector3D[] { Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K, Vector3D.ZERO };
                final double[] timeDirs = new double[] { 0, 0, 0, 1 };
                final double h = 1.0;
                final AbsoluteDate t1 = t0.shiftedBy(20.0);
                for (int i = 0; i < 4; ++i) {

                    final SmallManeuverAnalyticalModel[] models = new SmallManeuverAnalyticalModel[] {
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(-4 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, -4 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(-3 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, -3 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(-2 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, -2 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(-1 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, -1 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(+1 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, +1 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(+2 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, +2 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(+3 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, +3 * h, velDirs[i]), isp, thruster),
                        new SmallManeuverAnalyticalModel(withoutManeuver.propagate(t0.shiftedBy(+4 * h
                            * timeDirs[i])),
                            eme2000, new Vector3D(1, dV0, +4 * h, velDirs[i]), isp, thruster),
                    };
                    final double[][] array = new double[models.length][6];

                    final Orbit orbitWithout = withoutManeuver.propagate(t1).getOrbit();

                    // compute reference orbit gradient by finite differences
                    final double c = 1.0 / (840 * h);
                    for (int j = 0; j < models.length; ++j) {
                        orbitType.mapOrbitToArray(models[j].apply(orbitWithout), positionAngle, array[j]);
                    }
                    final double[] orbitGradient = new double[6];
                    for (int k = 0; k < orbitGradient.length; ++k) {
                        final double d4 = array[7][k] - array[0][k];
                        final double d3 = array[6][k] - array[1][k];
                        final double d2 = array[5][k] - array[2][k];
                        final double d1 = array[4][k] - array[3][k];
                        orbitGradient[k] = (-3 * d4 + 32 * d3 - 168 * d2 + 672 * d1) * c;
                    }

                    // analytical Jacobian to check
                    final double[][] jacobian = new double[6][4];
                    model.getJacobian(orbitWithout, positionAngle, jacobian);

                    for (int j = 0; j < orbitGradient.length; ++j) {
                        Assert.assertEquals(orbitGradient[j], jacobian[j][i], 7.0e-6 * MathLib.abs(orbitGradient[j]));
                    }

                }

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

        return propagator;
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private static final String thruster = "thruster";
}
