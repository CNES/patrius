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
 * @history created 22/08/2024
 *
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.15:OPENFD-307:21/11/2024:[Patrius] Repère de la vitesse non inertiel (suite)
 * VERSION:4.14:OPENFD-304:22/08/2024: [Patrius] Repere de la vitesse dans le detecteur d'angle d'aspect solaire
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class ContinuousThrustManeuverTest {

    /**
     * @description This test is implemented for FA307. It ensures that in the tested method the frame in which the
     *              SpacecraftState is expressed is inertial and that, if not, an exception is thrown.
     * 
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @throws PatriusException
     */
    @Test
    public void testStateFrameNotPseudoInertial() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        // initial date:
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
            new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC());
        // sets the starting date of the maneuver:
        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 15,
            34.080), TimeScalesFactory.getUTC());
        // initial mass:
        final double mass = 4000;

        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        final TankProperty tankA = new TankProperty(mass);
        builder1.addPart("thruster", "Main", Transform.IDENTITY);
        builder1.addProperty(tankA, "thruster");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassModel model1 = new MassModel(assembly1);

        // orbit:
        final double mu = CelestialBodyFactory.getEarth().getGM();
        // final Orbit orbit = new CircularOrbit(7178000, .0, .0, MathLib.toRadians(98), .0, .0,
        // PositionAngle.MEAN, FramesFactory.getTIRF(), initDate, mu);
        final double ix = 2156444.05;
        final double iy = 3611777.68;
        final double iz = -5316875.46;
        final double ivz = -6579.446110;
        final double ivx = 3916.478783;
        final double ivy = 8.876119;
        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);

        final Orbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getITRF(), initDate, mu);
        // inertial attitude law:
        final ConstantAttitudeLaw law =
            new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(Vector3D.PLUS_I,
                Vector3D.PLUS_I)));
        // initial state:
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
                orbit.getFrame()), model1);

        // sets the thrust:
        final double[] coeffs = new double[2];
        coeffs[0] = 1.01;
        coeffs[1] = 0.889;
//        final VariablePressureThrust thrust = new VariablePressureThrust(fireDate, 20, 1. / 60., coeffs);
        final ConstantDirection direction = new ConstantDirection(Vector3D.PLUS_I);
        // sets the ISP:
//        final VariableISP isp = new VariableISP(fireDate, 200, 10. / 60.);

        // sets the maneuver:
        final double f = 420.;
        final double isp = 318.;
        final Parameter thrust = new Parameter("thrust", f);
        final Parameter flowRate = new Parameter("flow rate", -f / (Constants.G0_STANDARD_GRAVITY * isp));

        final PropulsiveProperty engineProp = new PropulsiveProperty(thrust, new Parameter("Isp",
                    -thrust.getValue() / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue())));

        final ContinuousThrustManeuver maneuver =
            new ContinuousThrustManeuver(fireDate, 600, engineProp,
                direction, model1, tankA, LOFType.LVLH);
        final double[] dAccdParam = new double[3];
        maneuver.setFiring(true);
        try {
            maneuver.addDAccDParam(initialState, thrust, dAccdParam);
            fail();
        } catch (final Exception e) {
            assertEquals(e.getMessage(), PatriusMessages.NOT_INERTIAL_FRAME.getSourceString());
        }

    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
