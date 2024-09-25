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
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers.orbman;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.Vehicle;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ImpulseManeuversOrbParamTest {
    
    private static double sma;
    private static double exc;
    private static double inc;
    private static double pa;
    private static double raan;
    private static double anm;
    private static PositionAngle type;
    private static double mu;
    private static KeplerianParameters parameter;
    
    private static TimeScale timeScale;
    private static Orbit initialOrbit;
    private static String mainShapeName;
    private static MassProvider mm;
    private static MassProvider mm2;
    private static Attitude attitude;
    private static NumericalPropagator propagator;
    private static FirstOrderIntegrator integrator;
    private static EventDetector event;
    private static PropulsiveProperty engine;
    private static TankProperty tank;
    
    private static double dryMass;
    private static double isp;
    private static double dt;
    private static AbsoluteDate initialDate;
    private static AbsoluteDate finalDate;
    
    /**
     * @throws PatriusException
     *         Impulse maneuver creation
     * @testType UT
     * 
     * @description Test the following aspects of the class {@link ImpulseDiManeuver} (restricted to inclination only) :
     *              <ul>
     *              <li>Cover both constructors which implement the parent class {@link ImpulseManeuver} (note : a full
     *              evaluation with propagation is only made on the first constructor (using isp). The second
     *              constructor (using engine & tank) doesn't include propagation evaluation as it depends of the
     *              {@link ImpulseManeuver} parent class and it would be repetitive to fully evaluate both constructors in
     *              this TU)</li>
     *              <li>Check if the {@link ImpulseDiManeuver#computeDV(SpacecraftState)} method has an expected behavior</li>
     *              <li>Check returned reseted state are the same independently of the constructor</li>
     *              <li>Check if the expected maneuver goal on the orbit parameter is reached</li>
     *              </ul>
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria The class extends {@link ImpulseManeuver} correctly and has an expected behavior with the
     *                   maneuver.
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testImpulseDiManeuver() throws PatriusException {
        
        // Orbital inclination goal. We expect to add +1deg inclination to the orbit with the maneuver.
        final double di = FastMath.toRadians(1.);
        
        // Creation of the impulsive maneuver
        final ImpulseManeuver manDi1 = new ImpulseDiManeuver(event, di, isp, mm, mainShapeName, true);
        final ImpulseManeuver manDi2 = new ImpulseDiManeuver(event, di, engine, mm2, tank, true);

        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDi1 = propagationSequence(manDi1);

        // Manual computation of the expected deltaV vector to achieve the maneuver goal
        final double na2 = FastMath.sqrt(mu * sma);
        final double ray = sma * (1. - exc * FastMath.cos(parameter.getAnomaly(PositionAngle.ECCENTRIC)));
        final double cosAol = FastMath.cos(pa + parameter.getAnomaly(PositionAngle.TRUE));
        double dvi = 0.;
        final double dvT = -1.3021950014754111;
        if (FastMath.abs(cosAol) > 0.) {
            dvi = na2 * FastMath.sqrt(1. - exc * exc) * di / (ray * cosAol);
        }

        final Vector3D expectedDeltaVSat = new Vector3D(dvT, 0., dvi);        
        final ImpulseManeuver manDi1Expected = new ImpulseManeuver(event, expectedDeltaVSat, isp, mm, 
            mainShapeName, LOFType.TNW);

        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDi1Expected = propagationSequence(manDi1Expected);
        
        
        // We check the two propagation have produce the same state according to main orbital parameters
        // and deltaV vector
        Assert.assertEquals(stateDi1Expected.getA(), stateDi1.getA(), 1e-8);
        Assert.assertEquals(stateDi1Expected.getE(), stateDi1.getE(), 1e-10);
        Assert.assertEquals(stateDi1Expected.getI(), stateDi1.getI(), 1e-10);
        Assert.assertEquals(expectedDeltaVSat, manDi1.getDeltaVSat());
        
        // We also check the expected orbital parameters value goal. 
        final double expectedI = inc + di;
        Assert.assertEquals(expectedI, stateDi1.getI(), 1e-4);
        
        // Check returned reseted state are the same independently of the constructor
        final SpacecraftState state1 = new SpacecraftState(initialOrbit, attitude, mm);
        
        final SpacecraftState resetState1         = manDi1.resetState(state1);
        final SpacecraftState resetState1Expected = manDi1Expected.resetState(state1);

        Assert.assertEquals(0.,
            resetState1.getPVCoordinates().getPosition().subtract(resetState1Expected.getPVCoordinates().getPosition())
                .getNorm(), 0.);

        // Check tank and propulsive properties
        Assert.assertEquals(engine.getIspParam().getValue(), manDi2.getIsp(), 0);
        Assert.assertEquals(engine.getIspParam().getValue(), manDi2.getPropulsiveProperty().getIspParam().getValue(),
            0);
        Assert.assertEquals(tank.getMass(), manDi2.getTankProperty().getMass(), 0);
    }
    
    /**
     * @throws PatriusException
     *         Impulse maneuver creation
     * @testType UT
     * 
     * @description Test the following aspects of the class {@link ImpulseDiManeuver} (restricted to both inclination
     *              and semi-major axis) :
     *              <ul>
     *              <li>Cover both constructors which implement the parent class {@link ImpulseManeuver} (note : a full
     *              evaluation with propagation is only made on the first constructor (using isp). The second
     *              constructor (using engine & tank) doesn't include propagation evaluation as it depends of the
     *              {@link ImpulseManeuver} parent class and it would be repetitive to fully evaluate both constructors
     *              in this TU)</li>
     *              <li>Check if the {@link ImpulseDiManeuver#computeDV(SpacecraftState)} method has an expected behavior</li>
     *              <li>Check returned reseted state are the same independently of the constructor</li>
     *              <li>Check if the expected maneuver goal on the orbit parameter is reached</li>
     *              </ul>
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria The class extends {@link ImpulseManeuver} correctly and has an expected behavior with the
     *                   maneuver.
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testImpulseDiDaManeuver() throws PatriusException {
        
        // Orbital inclination goal. 
        // We expect to add +1deg inclination and +1000m semi-major axis to the orbit with the maneuver.
        final double di = FastMath.toRadians(1.);
        final double da = 1000.;
        
        // Creation of the impulsive maneuver
        final ImpulseDiManeuver manDiDa1 = new ImpulseDiManeuver(event, di, da, isp, mm, mainShapeName, true);
        final ImpulseManeuver manDiDa2 = new ImpulseDiManeuver(event, di, da, engine, mm2, tank, true);

        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDiDa1 = propagationSequence(manDiDa1);

        // Manual computation of the expected deltaV vector to achieve the maneuver goal
        final double na2 = FastMath.sqrt(mu * sma);
        final double ray = sma * (1. - exc * FastMath.cos(parameter.getAnomaly(PositionAngle.ECCENTRIC)));
        final double twoOnR = 2. / ray;
        final double vBefore = FastMath.sqrt(mu * (twoOnR - (1. / sma)));
        final double cosAol = FastMath.cos(pa + parameter.getAnomaly(PositionAngle.TRUE));

        double dvi = 0.;
        double dvT = 0.;
        if (FastMath.abs(cosAol) > 0.) {
            dvi = na2 * FastMath.sqrt(1. - exc * exc) * di / (ray * cosAol);
            if (da != 0.) {
                final double dvTCompensate = FastMath.sqrt(vBefore * vBefore - dvi * dvi) - vBefore;
                final double vAfter = FastMath.sqrt(mu * (twoOnR - (1. / (sma + da))));
                dvT = dvTCompensate + vAfter - vBefore;
            }
        }

        final Vector3D expectedDeltaVSat = new Vector3D(dvT, 0., dvi);        
        final ImpulseManeuver manDiDa1Expected = new ImpulseManeuver(event, expectedDeltaVSat, isp, mm, 
            mainShapeName, LOFType.TNW);

        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDiDa1Expected = propagationSequence(manDiDa1Expected);
        
        // We check the two propagation have produce the same state according to main orbital parameters
        // and deltaV vector
        Assert.assertEquals(stateDiDa1Expected.getA(), stateDiDa1.getA(), 1e-8);
        Assert.assertEquals(stateDiDa1Expected.getE(), stateDiDa1.getE(), 1e-10);
        Assert.assertEquals(stateDiDa1Expected.getI(), stateDiDa1.getI(), 1e-10);
        Assert.assertEquals(expectedDeltaVSat, manDiDa1.getDeltaVSat());
        
        // We also check the expected orbital parameters value goal. 
        final double expectedA = sma + da;
        final double expectedI = inc + di;
        Assert.assertEquals(expectedA, stateDiDa1.getA(), 1); // 1m tolerance
        Assert.assertEquals(expectedI, stateDiDa1.getI(), 1e-4); // 1e-4 rad tolerance
        
        // Check returned reseted state are the same independently of the constructor
        final SpacecraftState state1 = new SpacecraftState(initialOrbit, attitude, mm);
        
        final SpacecraftState resetState1         = manDiDa1.resetState(state1);
        final SpacecraftState resetState1Expected = manDiDa1Expected.resetState(state1);

        Assert.assertEquals(0.,
            resetState1.getPVCoordinates().getPosition().subtract(resetState1Expected.getPVCoordinates().getPosition())
                .getNorm(), 0.);

        // Check tank and propulsive properties
        Assert.assertEquals(engine.getIspParam().getValue(), manDiDa2.getIsp(), 0);
        Assert.assertEquals(engine.getIspParam().getValue(), manDiDa2.getPropulsiveProperty().getIspParam().getValue(),
            0);
        Assert.assertEquals(tank.getMass(), manDiDa2.getTankProperty().getMass(), 0);
        
        // Test exception case (maneuver not feasible)
        final ImpulseDiManeuver manDe3 = new ImpulseDiManeuver(event, 1.9, -state1.getA(), engine, mm2, tank, true);
        try {
            manDe3.computeDV(state1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        final ImpulseDiManeuver manDe4 = new ImpulseDiManeuver(event, 1.9, -state1.getA(), engine, mm2, tank, false);
        try {
            manDe4.computeDV(state1);
            Assert.assertEquals(0., manDe4.getDeltaVSat().getNorm(), 0.);
        } catch (final PatriusException e) {
            Assert.fail();
        }

        Assert.assertEquals(da, manDiDa1.getDa(), 0.);
        Assert.assertEquals(di, manDiDa1.getDi(), 0.);
    }
    
    /**
     * @throws PatriusException
     *         Impulse maneuver creation
     * @testType UT
     * 
     * @description Test the following aspects of the class {@link ImpulseDaManeuver} :
     *              <ul>
     *              <li>Cover both constructors which implement the parent class {@link ImpulseManeuver} (note : a full
     *              evaluation with propagation is only made on the first constructor (using isp). The second
     *              constructor (using engine & tank) doesn't include propagation evaluation as it depends of the
     *              {@link ImpulseManeuver} parent class and it would be repetitive to fully evaluate both constructors in
     *              this TU)</li>
     *              <li>Check if the {@link ImpulseDiManeuver#computeDV(SpacecraftState)} method has an expected behavior</li>
     *              <li>Check returned reseted state are the same independently of the constructor</li>
     *              <li>Check if the expected maneuver goal on the orbit parameter is reached</li>
     *              </ul>
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria The class extends {@link ImpulseManeuver} correctly and has an expected behavior with the
     *                   maneuver.
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testImpulseDaManeuver() throws PatriusException {
        
        // Orbital inclination goal. 
        // We expect to add +10000m semi-major axis to the orbit with the maneuver.
        final double da = 10000.;
        
        // Creation of the impulsive maneuver
        final ImpulseDaManeuver manDa1 = new ImpulseDaManeuver(event, da, isp, mm, mainShapeName);
        final ImpulseManeuver manDa2 = new ImpulseDaManeuver(event, da, engine, mm2, tank);
        
        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDa1 = propagationSequence(manDa1);

        // Manual computation of the expected deltaV vector to achieve the maneuver goal
        final double ray = sma * (1. - exc * FastMath.cos(parameter.getAnomaly(PositionAngle.ECCENTRIC)));
        final double twoOnR = 2. / ray;

        // DV computation
        final double vBefore = FastMath.sqrt(mu * (twoOnR - (1. / sma)));
        final double vAfter = FastMath.sqrt(mu * (twoOnR - (1. / (sma + da))));

        final Vector3D expectedDeltaVSat = new Vector3D(vAfter - vBefore, 0., 0.);        
        final ImpulseManeuver manDa1Expected = new ImpulseManeuver(event, expectedDeltaVSat, isp, mm, 
            mainShapeName, LOFType.TNW);

        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDa1Expected = propagationSequence(manDa1Expected);
        
        // We check the two propagation have produce the same state according to main orbital parameters
        // and deltaV vector
        Assert.assertEquals(stateDa1Expected.getA(), stateDa1.getA(), 1e-8);
        Assert.assertEquals(stateDa1Expected.getE(), stateDa1.getE(), 1e-10);
        Assert.assertEquals(stateDa1Expected.getI(), stateDa1.getI(), 1e-10);
        Assert.assertEquals(expectedDeltaVSat, manDa1.getDeltaVSat());
        
        // We also check the expected orbital parameters value goal. 
        final double expectedA = sma + da;
        Assert.assertEquals(expectedA, stateDa1.getA(), 1e-6); // 1mm tolerance
        
        // Check returned reseted state are the same independently of the constructor
        final SpacecraftState state1 = new SpacecraftState(initialOrbit, attitude, mm);
        
        final SpacecraftState resetState1         = manDa1.resetState(state1);
        final SpacecraftState resetState1Expected = manDa1Expected.resetState(state1);

        Assert.assertEquals(0.,
            resetState1.getPVCoordinates().getPosition().subtract(resetState1Expected.getPVCoordinates().getPosition())
                .getNorm(), 0.);

        // Check tank and propulsive properties
        Assert.assertEquals(engine.getIspParam().getValue(), manDa2.getIsp(), 0);
        Assert.assertEquals(engine.getIspParam().getValue(), manDa2.getPropulsiveProperty().getIspParam().getValue(),
            0);
        Assert.assertEquals(tank.getMass(), manDa2.getTankProperty().getMass(), 0);
        Assert.assertEquals(da, manDa1.getDa(), 0.);
    }
    
    /**
     * @throws PatriusException
     *         Impulse maneuver creation
     * @testType UT
     * 
     * @description Test the following aspects of the class {@link ImpulseDeManeuver} :
     *              <ul>
     *              <li>Cover both constructors which implement the parent class {@link ImpulseManeuver} (note : a full
     *              evaluation with propagation is only made on the first constructor (using isp). The second
     *              constructor (using engine & tank) doesn't include propagation evaluation as it depends of the
     *              {@link ImpulseManeuver} parent class and it would be repetitive to fully evaluate both constructors in
     *              this TU)</li>
     *              <li>Check if the {@link ImpulseDiManeuver#computeDV(SpacecraftState)} method has an expected behavior</li>
     *              <li>Check returned reseted state are the same independently of the constructor</li>
     *              <li>Check if the expected maneuver goal on the orbit parameter is reached</li>
     *              </ul>
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria The class extends {@link ImpulseManeuver} correctly and has an expected behavior with the
     *                   maneuver.
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testImpulseDeManeuver() throws PatriusException {
        
        // Orbital inclination goal. 
        // We expect to add +0.01 eccentricity and -1000m semi-major axis to the orbit with the maneuver.
        final double de = 0.01;
        final double da = -1000.;
        
        // Creation of the impulsive maneuver
        final ImpulseDeManeuver manDe1 = new ImpulseDeManeuver(event, de, da, isp, mm, mainShapeName, true);
        final ImpulseManeuver manDe2 = new ImpulseDeManeuver(event, de, da, engine, mm2, tank, true);
        
        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDe1 = propagationSequence(manDe1);

        // Manual computation of the expected deltaV vector to achieve the maneuver goal
        final double sma2 = sma + da;
        final double ecc2 = exc + de;
        final double anv = parameter.getAnomaly(PositionAngle.TRUE);
        final double sinv = FastMath.sin(anv);
        final double cosv = FastMath.cos(anv);
        
        final Vector3D velBefore = getVelocityQSW(sma, exc, sinv, cosv, mu);

        final double p1 = sma * (1. - exc * exc);
        final double p2 = sma2 * (1. - ecc2 * ecc2);
        final double cosv2 = (-1. + (1. + exc * cosv) * p2 / p1) / ecc2;

        final double ang1 = FastMath.acos(cosv2);
        final double sin1 = FastMath.sin(ang1);
        final Vector3D velAfter1 = getVelocityQSW(sma2, ecc2, sin1, cosv2, mu);
        final double dv1 = velAfter1.subtract(velBefore).getNorm();

        final double ang2 = -ang1;
        final double sin2 = FastMath.sin(ang2);
        final Vector3D velAfter2 = getVelocityQSW(sma2, ecc2, sin2, cosv2, mu);
        final double dv2 = velAfter2.subtract(velBefore).getNorm();

        final Vector3D expectedDeltaVSat;
        if (dv1 <= dv2) {
            expectedDeltaVSat = velAfter1.subtract(velBefore);
        } else {
            expectedDeltaVSat = velAfter2.subtract(velBefore);
        }
      
        final ImpulseManeuver manDe1Expected = new ImpulseManeuver(event, expectedDeltaVSat, isp, mm, 
            mainShapeName, LOFType.QSW);

        // Calling the propagation sequence using the built maneuver to generate the final state
        final SpacecraftState stateDe1Expected = propagationSequence(manDe1Expected);
        
        // We check the two propagation have produce the same state according to main orbital parameters
        // and deltaV vector
        Assert.assertEquals(stateDe1Expected.getA(), stateDe1.getA(), 1e-8);
        Assert.assertEquals(stateDe1Expected.getE(), stateDe1.getE(), 1e-10);
        Assert.assertEquals(stateDe1Expected.getI(), stateDe1.getI(), 1e-10);
        Assert.assertEquals(expectedDeltaVSat.getX(), manDe1.getDeltaVSat().getX(), 1e-10);
        Assert.assertEquals(expectedDeltaVSat.getY(), manDe1.getDeltaVSat().getY(), 1e-10);
        Assert.assertEquals(expectedDeltaVSat.getZ(), manDe1.getDeltaVSat().getZ(), 1e-10);
        
        // We also check the expected orbital parameters value goal. 
        final double expectedE = exc + de;
        final double expectedA = sma + da;
        Assert.assertEquals(expectedE, stateDe1.getE(), 1e-6); // 1mm tolerance
        Assert.assertEquals(expectedA, stateDe1.getA(), 1e-6); // 1mm tolerance
        
        // Check returned reseted state are the same independently of the constructor
        final SpacecraftState state1 = new SpacecraftState(initialOrbit, attitude, mm);
        
        final SpacecraftState resetState1         = manDe1.resetState(state1);
        final SpacecraftState resetState1Expected = manDe1Expected.resetState(state1);

        Assert.assertEquals(0.,
            resetState1.getPVCoordinates().getPosition().subtract(resetState1Expected.getPVCoordinates().getPosition())
                .getNorm(), 0.);

        // Check tank and propulsive properties
        Assert.assertEquals(engine.getIspParam().getValue(), manDe2.getIsp(), 0);
        Assert.assertEquals(engine.getIspParam().getValue(), manDe2.getPropulsiveProperty().getIspParam().getValue(),
            0);
        Assert.assertEquals(tank.getMass(), manDe2.getTankProperty().getMass(), 0);
        
        // Test exception case (maneuver not feasible)
        final ImpulseDeManeuver manDe3 = new ImpulseDeManeuver(event, 1.9, 10000, engine, mm2, tank, true);
        try {
            manDe3.computeDV(state1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        final ImpulseDeManeuver manDe4 = new ImpulseDeManeuver(event, 1.9, 10000, engine, mm2, tank, false);
        try {
            manDe4.computeDV(state1);
            Assert.assertEquals(0., manDe4.getDeltaVSat().getNorm(), 0.);
        } catch (final PatriusException e) {
            Assert.fail();
        }

        Assert.assertEquals(da, manDe1.getDa(), 0.);
        Assert.assertEquals(de, manDe1.getDe(), 0.);
    }
    
    /**
     * This private method is a shortcut for the propagation sequence used by several TU.
     * It takes an impulsive maneuver as input, process a standard propagation sequence and sends the state as output.
     * 
     * @param impulseMan Impulsive maneuver
     * @return final state
     * @throws PatriusException
     */
    private SpacecraftState propagationSequence(final ImpulseManeuver impulseMan) throws PatriusException {
     
        // We create a SpacecratftState
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, mm);

        // Initialization of the propagator
        // Forcing integration using cartesian equations
        propagator = new NumericalPropagator(integrator, initialState.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);
        propagator.resetInitialState(initialState);

        // Adding the impulsive maneuver
        propagator.addEventDetector(impulseMan);
        propagator.setMassProviderEquation(mm);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        // Propagating 100s
        final SpacecraftState finalSc = propagator.propagate(finalDate);
        
        return finalSc;
    }
    
    /**
     * Private method to get the QSW components of the velocity.
     * 
     * @param sma semi major axis (m)
     * @param ecc eccentricity
     * @param sinv sinus of the true anomaly
     * @param cosv cosinus of the true anomaly
     * @param mu Central term of the attraction (m3/s2)
     * @return QSW components of the velocity.
     */
    private Vector3D getVelocityQSW(final double sma, final double ecc, final double sinv, 
        final double cosv, final double mu) {

        final double p = sma * (1. - ecc * ecc);
        final double ray = p / (1. + ecc * cosv);
        final double cin = FastMath.sqrt(mu * p);

        final double vCosGamma = cin / ray;
        final double vSinGamma = ecc * sinv * FastMath.sqrt(mu / p);

        final Vector3D vec = new Vector3D(vSinGamma, vCosGamma, 0.);

        return vec;
    }
    
    
    /**
     * Initialize input parameters.
     * @throws PatriusException - if inertialFrame is not a pseudo-inertial frame
     */
    @Before
    public void setUp() throws PatriusException {

        Utils.setDataRoot("regular-data");

        // Initial orbit
        sma = 7200.e+3;
        exc = 0.01;
        inc = FastMath.toRadians(45.);
        pa = FastMath.toRadians(0.);
        raan = FastMath.toRadians(0.);
        anm = FastMath.toRadians(20.);
        type = PositionAngle.MEAN;
        mu = Constants.WGS84_EARTH_MU;

        parameter = new KeplerianParameters(sma, exc, inc, pa, raan, anm, type, mu);

        mainShapeName = "MAIN";
        dryMass = 1000.;
        isp = 320.;
        dt = 100.;

        // Recovery of the UTC time scale using a "factory" (not to duplicate such unique object)
        timeScale = TimeScalesFactory.getUTC();

        // Date of the orbit given in UTC time scale)
        final AbsoluteDate date = new AbsoluteDate("2010-01-01T12:00:00.000", timeScale);

        // Getting the frame with wich will defined the orbit parameters
        // As for time scale, we will use also a "factory".
        final Frame GCRF = FramesFactory.getGCRF();

        initialOrbit = new KeplerianOrbit(parameter, GCRF, date);

        // Creating a mass model (see also specific example)
        final AssemblyBuilder builder = new AssemblyBuilder();
        final double iniMass = dryMass;
        builder.addMainPart(mainShapeName);
        builder.addProperty(new MassProperty(iniMass), mainShapeName);
        final Assembly assembly = builder.returnAssembly();
        mm = new MassModel(assembly);
        
        initialDate = initialOrbit.getDate();
        finalDate = initialOrbit.getDate().shiftedBy(dt);
        
        // Initialization of the RungeKutta integrator with a 2s step
        final double pasRk = 2.;
        integrator = new ClassicalRungeKuttaIntegrator(pasRk);
        // Event corresponding to the criteria to trigger the impulsive maneuver
        event = new DateDetector(initialDate.shiftedBy(0.));
        
        engine = new PropulsiveProperty(200., isp);
        tank = new TankProperty(1000);

        final Vehicle vehicle = new Vehicle();
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 2.));
        vehicle.setDryMass(2000.);
        vehicle.addEngine("Engine", engine);
        vehicle.addTank("Tank", tank);
        mm2 = new MassModel(vehicle.createAssembly(FramesFactory.getGCRF()));
        
        final AttitudeLaw law = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH);
        attitude = law.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());
    }
}
