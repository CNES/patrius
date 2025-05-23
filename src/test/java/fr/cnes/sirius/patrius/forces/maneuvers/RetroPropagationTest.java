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
 * @history creation 29/07/2014
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::FA:1851:18/10/2018:Update the massModel from a SimpleMassModel to an Assembly builder
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.io.IOException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.ApsideDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.AbstractHarmonicGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description Validation class for maneuvers when the integration variable (time) decreases during integration. This
 *              test class performs go and return propagation. The final position/velocity vectors and the final mass
 *              should be equal to the initial ones.
 * 
 * @concurrency not thread-safe
 * 
 * @author François Desclaux
 * @author Charlotte Maggiorani
 * 
 * @version $Id$
 * 
 * @since 2.3
 * 
 */
public class RetroPropagationTest {

    /**
     * Mass model
     */
    private static MassModel massModel;

    private static TankProperty tank1;

    /**
     * Numerical Propagator
     */
    private static NumericalPropagator numPropag;

    /**
     * Keplerian Propagator
     */
    private static KeplerianPropagator kepPropag;

    /**
     * Initial date
     */
    private static AbsoluteDate T0;

    /**
     * Maneuver date
     */
    private static AbsoluteDate T1;

    /**
     * Propagation date
     */
    private static AbsoluteDate T2;

    private static double MASS_EPSILON = 1e-14;
    private static double MASS_INCREASING = 0.4;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Maneuver Tests in case of retro-propagation (go and return propagation)
         * 
         * @featureDescription Validation tests for retro-propagation.
         * 
         * @coveredRequirements
         */
        MANEUVERS_RETRO_PROPAGATION
    }

    /**
     * @throws PatriusException
     * @testType TV
     * 
     * @testedFeature {@link features#MANEUVERS_RETRO_PROPAGATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D)}
     * 
     * @description Test ConstantThrustManeuver in case of retro-propagation with maneuver defined by date and duration
     * 
     * @input orbital parameters, constant thrust maneuver, and a numerical propagator
     * 
     * @output initial position, velocity and mass
     * 
     * @testPassCriteria same position, velocity and mass after go and return propagation
     * 
     * @comments No constant thrust maneuver in analytical propagation
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testConstantThrustManeuver() throws PatriusException {
        // Constant thrust maneuver at T1
        final ContinuousThrustManeuver ctMan =
            new ContinuousThrustManeuver(T1, 10, new PropulsiveProperty(150, 321), new Vector3D(
                0, -1, 0), massModel, tank1);

        // Numerical propagation
        numPropag.addForceModel(ctMan);

        final SpacecraftState s0 = numPropag.getInitialState();
        final SpacecraftState s2 = numPropag.propagate(T2);
        final SpacecraftState sR = numPropag.propagate(T0);

        // Test mass decreases after first propagation
        Assert.assertTrue(s0.getMass("BODY") > (s2.getMass("BODY") + MASS_INCREASING));
        // Test final mass is equal to the initial mass after propagation and retro-propagation
        Assert.assertEquals(s0.getMass("BODY"), sR.getMass("BODY"), MASS_EPSILON);
        // test PVCoordinates
        checkPVs(s0.getPVCoordinates(), sR.getPVCoordinates(), 1e-8, 1e-11);
    }

    /**
     * @throws PatriusException
     * @testType TU
     * 
     * @testedFeature {@link features#MANEUVERS_RETRO_PROPAGATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D)}
     * 
     * @description Test ConstantThrustManeuver in case of retro-propagation with maneuver defined by event detectors
     * 
     * @input orbital parameters, constant thrust maneuver, events detectors, and a numerical propagator
     * 
     * @output initial/final position, velocity and mass
     * 
     * @testPassCriteria same position, velocity and mass after go and return propagation
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testConstantThrustManeuverEvents() throws PatriusException {

        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = initialDate.shiftedBy(3600.);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank = new TankProperty(1000.);
        builder.addPart("Satellite", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "Satellite");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);

        // Constant thrust maneuver at maneuver date
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver =
            new ContinuousThrustManeuver(startDetector1, endDetector1, new PropulsiveProperty(150., 321.),
                new Vector3D(0, -1, 0), massModel, tank);

        // Numerical propagation
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(initialState);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator.setMassProviderEquation(massModel);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState s0 = propagator.getInitialState();
        propagator.propagate(finalDate);
        final SpacecraftState sR = propagator.propagate(initialDate);

        // Check (PVCoordinates and mass)
        Assert.assertEquals(s0.getMass("Satellite"), sR.getMass("Satellite"), 1E-8);
        checkPVs(s0.getPVCoordinates(), sR.getPVCoordinates(), 1e-4, 1e-8);
    }

    /**
     * @throws PatriusException
     * @testType TU
     * 
     * @testedFeature {@link features#MANEUVERS_RETRO_PROPAGATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, double, double, Vector3D)}
     * 
     * @description Test VariableThrustManeuver in case of retro-propagation with maneuver defined by event detectors
     * 
     * @input orbital parameters, variable thrust maneuver, events detectors, and a numerical propagator
     * 
     * @output initial/final position, velocity and mass
     * 
     * @testPassCriteria same position, velocity and mass after go and return propagation
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testVariableThrustManeuverEvents() throws PatriusException {

        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = initialDate.shiftedBy(3600.);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank = new TankProperty(1000.);
        builder.addPart("Satellite", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "Satellite");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);

        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8108469081183801146L;

            @Override
            public double value(final SpacecraftState x) {
                return 1.;
            }
        };
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -55612671415281539L;

            @Override
            public double value(final SpacecraftState x) {
                return 200.;
            }
        };
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 1632029955419595914L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_I;
            }
        };

        // Constant thrust maneuver at maneuver date
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(startDetector1, endDetector1,
            new PropulsiveProperty(thrust, isp),
            direction, massModel, tank);

        // Numerical propagation
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(initialState);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator.setMassProviderEquation(massModel);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState s0 = propagator.getInitialState();
        propagator.propagate(finalDate);
        final SpacecraftState sR = propagator.propagate(initialDate);

        // Check (PVCoordinates and mass)
        Assert.assertEquals(s0.getMass("Satellite"), sR.getMass("Satellite"), 1E-8);
        checkPVs(s0.getPVCoordinates(), sR.getPVCoordinates(), 1e-4, 1e-8);
    }

    /**
     * @throws PatriusException
     * @testType TU
     * 
     * @testedFeature {@link features#MANEUVERS_RETRO_PROPAGATION}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, double, double, double)}
     * 
     * @description Test ConstantThrustError in case of retro-propagation with maneuver defined by event detectors
     * 
     * @input orbital parameters, constant thrust error, events detectors, and a numerical propagator
     * 
     * @output initial/final position, velocity and mass
     * 
     * @testPassCriteria same position, velocity and mass after go and return propagation
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testConstantThrustErrorEvents() throws PatriusException {

        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = initialDate.shiftedBy(3600.);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank = new TankProperty(1000.);
        builder.addPart("Satellite", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "Satellite");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);

        // Constant thrust maneuver at maneuver date
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ConstantThrustError maneuver =
            new ConstantThrustError(startDetector1, endDetector1, new ConstantFunction(2.),
                new ConstantFunction(2.), new ConstantFunction(2.));

        // Numerical propagation
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(initialState);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator.setMassProviderEquation(massModel);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState s0 = propagator.getInitialState();
        propagator.propagate(finalDate);
        final SpacecraftState sR = propagator.propagate(initialDate);

        // Check (PVCoordinates and mass)
        Assert.assertEquals(s0.getMass("Satellite"), sR.getMass("Satellite"), 1E-8);
        checkPVs(s0.getPVCoordinates(), sR.getPVCoordinates(), 1e-4, 1e-8);
    }

    /**
     * @throws PatriusException
     * @testType TV
     * 
     * @testedFeature {@link features#MANEUVERS_RETRO_PROPAGATION}
     * 
     * @testedMethod {@link ImpulseManeuver#ImpulseManeuver(EventDetector, Vector3D, double)}
     * 
     * @description Test ImpulseManeuver in case of retro-propagation
     * 
     * @input orbital parameters, impulse force, analytical propagator
     * 
     * @output initial position, velocity and mass
     * 
     * @testPassCriteria same position, velocity and mass after go and return propagation
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testImpulseManeuverKepProp() throws PatriusException {
        // Impulse maneuver at T1
        final EventDetector det1 = new DateDetector(T1, 100, 1.e-8);
        final ImpulseManeuver impulse = new ImpulseManeuver(det1, new Vector3D(0, -15, 0), 321.,
            massModel, "BODY");

        // Keplerian propagation
        kepPropag.addEventDetector(impulse);
        kepPropag.addAdditionalStateProvider(massModel);
        final SpacecraftState s0 = kepPropag.getInitialState();
        final SpacecraftState s2 = kepPropag.propagate(T2);
        final SpacecraftState sR = kepPropag.propagate(T0);

        // Test mass decreases after first propagation
        Assert.assertTrue(s0.getMass("BODY") > (s2.getMass("BODY") + MASS_INCREASING));
        // Test final mass is equal to the initial mass after propagation and retro-propagation
        Assert.assertEquals(s0.getMass("BODY"), sR.getMass("BODY"), MASS_EPSILON);
        // Test PVCoordinates
        checkPVs(s0.getPVCoordinates(), sR.getPVCoordinates(), 1e-8, 1e-12);
    }

    /**
     * @throws PatriusException
     * @testType TV
     * 
     * @testedFeature {@link features#MANEUVERS_RETRO_PROPAGATION}
     * 
     * @testedMethod {@link ImpulseManeuver#ImpulseManeuver(EventDetector, Vector3D, double)}
     * 
     * @description Test ImpulseManeuver in case of retro-propagation
     * 
     * @input orbital parameters, impulse force, numerical propagator
     * 
     * @output initial position, velocity and mass
     * 
     * @testPassCriteria same position, velocity and mass after go and return propagation
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testImpulseManeuverNumProp() throws PatriusException {

        // Impulse maneuver at T1
        final EventDetector det1 = new DateDetector(T1, 100, 1.e-8);
        final ImpulseManeuver impulse = new ImpulseManeuver(det1, new Vector3D(0, -15, 0), 321.,
            massModel, "BODY");

        // Numerical propagation
        numPropag.addEventDetector(impulse);

        final SpacecraftState s0 = numPropag.getInitialState();
        final SpacecraftState s2 = numPropag.propagate(T2);
        final SpacecraftState sR = numPropag.propagate(T0);

        // Test mass decreases after first propagation
        Assert.assertTrue(s0.getMass("BODY") > (s2.getMass("BODY") + MASS_INCREASING));
        // Test final mass is equal to the initial mass after propagation and retro-propagation
        Assert.assertEquals(s0.getMass("BODY"), sR.getMass("BODY"), MASS_EPSILON);
        // Test PVCoordinates
        checkPVs(s0.getPVCoordinates(), sR.getPVCoordinates(), 2e-8, 1e-11);
    }

    @Before
    public void setUp() throws PatriusException, IOException, ParseException {
        /*
         * Load gravity field
         */
        Utils.setDataRoot("regular-dataPBASE");

        /*
         * Simple spacecraft with mass
         */
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        tank1 = new TankProperty(100.0);
        builder.addProperty(tank1, "BODY");
        builder.addPart("part1", "BODY", new Vector3D(1, 0, 0), new Rotation(false, 1, 0, 0, 0));
        final TankProperty tank2 = new TankProperty(50.0);
        builder.addProperty(tank2, "part1");
        final Assembly spacecraft = builder.returnAssembly();
        // Mass model
        massModel = new MassModel(spacecraft);

        /*
         * Initial orbit
         */
        final Frame gcrf = FramesFactory.getGCRF();
        T0 = new AbsoluteDate("2010-01-23", TimeScalesFactory.getTAI());
        T1 = T0.shiftedBy(120.);
        T2 = T0.shiftedBy(180.);
        final double a = 20000.e3;
        final double e = 0.0001;
        final double inc = MathLib.toRadians(20);
        final double pom = MathLib.toRadians(0);
        final double gom = MathLib.toRadians(0);
        final double M = MathLib.toRadians(0);
        final Orbit initialOrbit = new KeplerianOrbit(a, e, inc, pom, gom, M, PositionAngle.TRUE,
            gcrf, T0, Constants.WGS84_EARTH_MU);

        /*
         * Numerical Propagator
         */

        // Creation of numerical integrator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        // Add forces

        // Add potential
        // add a reader for gravity fields
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr' file
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // we get the data as extracted from the file
        final int n = 60; // degree
        final int m = 60; // order
        final double[][] C = provider.getC(n, m, false);
        final double[][] S = provider.getS(n, m, false);

        // return perturbing force (ITRF2008 central body frame)
        final Frame itrf = FramesFactory.getITRF();
        final DrozinerGravityModel earthGravityModel = new DrozinerGravityModel(itrf, provider.getAe(),
            Constants.WGS84_EARTH_MU, C, S);
        earthGravityModel.setCentralTermContribution(false);
        final ForceModel potentiel = new DirectBodyAttraction(earthGravityModel);

        // Add third body attraction
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);

        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER,
            loaderSSB);

        final CelestialBody sun = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.SUN);
        final CelestialBody moon = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.MOON);


        final GravityModel sunGravityModel = sun.getGravityModel();
        ((AbstractHarmonicGravityModel) sunGravityModel).setCentralTermContribution(false);
        final ForceModel sunAttraction = new ThirdBodyAttraction(sunGravityModel);
        final GravityModel moonGravityModel = moon.getGravityModel();
        ((AbstractHarmonicGravityModel) moonGravityModel).setCentralTermContribution(false);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moonGravityModel);

        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);

        // Creation of numerical propagator
        numPropag = new NumericalPropagator(dop, initialState.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);

        // Add attitude law
        final AttitudeProvider lofPointingAtt = new LofOffset(gcrf, LOFType.QSW);
        numPropag.setAttitudeProvider(lofPointingAtt);

        // Initialize numerical propagator
        numPropag.setMassProviderEquation(massModel);

        numPropag.resetInitialState(initialState);

        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu()));

        numPropag.addForceModel(potentiel);
        numPropag.addForceModel(sunAttraction);
        numPropag.addForceModel(moonAttraction);
        numPropag.addForceModel(newtonianAttraction);

        /*
         * PROPAGATEUR KEPLERIEN
         */
        kepPropag = new KeplerianPropagator(initialOrbit, lofPointingAtt,
            Constants.EGM96_EARTH_MU, massModel);
    }

    @After
    public void tearDown() {
        massModel = null;
        numPropag = null;
        kepPropag = null;
        T0 = null;
        T1 = null;
        T2 = null;
    }

    /**
     * Check position and velocity vectors
     * 
     * @param PV1
     *        expected PVCoordinates
     * @param PV2
     *        PVCoordinates to test against PV1
     * @param eps1
     *        the maximum delta between actual and expected position vector
     * @param eps2
     *        the maximum delta between actual and expected velocity vector
     * 
     * @since 2.3
     */
    private static
        void
            checkPVs(final PVCoordinates PV1, final PVCoordinates PV2, final double eps1, final double eps2) {
        checkVectors(PV1.getPosition(), PV2.getPosition(), eps1);
        checkVectors(PV1.getVelocity(), PV2.getVelocity(), eps2);
    }

    /**
     * Check two vectors
     * 
     * @param vector1
     *        expected vector
     * @param vector2
     *        vector to test against vector1
     * @param eps
     *        the maximum delta between actual and expected vector coordinates
     * 
     * @since 2.3
     */
    private static void checkVectors(final Vector3D vector1, final Vector3D vector2, final double eps) {
        Assert.assertEquals(vector1.getX(), vector2.getX(), eps);
        Assert.assertEquals(vector1.getY(), vector2.getY(), eps);
        Assert.assertEquals(vector1.getZ(), vector2.getZ(), eps);
    }
}
