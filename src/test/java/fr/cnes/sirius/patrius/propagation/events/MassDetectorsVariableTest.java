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
 * @history creation 24/03/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:414:24/03/2015: proper handling of mass evolution
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1851:18/10/2018:Ablation force modification to match with the new massModel behavior
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description Test class for the detectors for total mass and for each part's mass. Functional tests. Tests only for
 *              variable thrusts.
 *              Same tests are provided in {@link MassDetectorsTest} for constant thrusts.
 * 
 * @see NullMassDetector and NullMassPartDetector
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MassDetectorsVariableTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle NullMassDetector tests
         * @featureDescription tests the detector for total mass : if negative or null
         */
        NULL_MASS_DETECTOR_TEST,

        /**
         * @featureTitle NullMassPartDetector tests
         * @featureDescription tests the detector for each part's mass
         */
        NULL_MASS_PART_DETECTOR_TEST,

        /**
         * @featureTitle mass detectors
         * @featureDescription tests a mass detector
         */
        MASS_DETECTOR_TEST

    }

    /** Name for the main part. */
    private static final String MAIN = "body";
    /** Name for the thruster. */
    private static final String THRUSTER1 = "thruster1";

    /** Tank 1. */
    private static TankProperty tank1;

    /** Tank 2. */
    private static TankProperty tank2;

    /** Initial orbit. */
    private static Orbit orbit;

    /** Initial date. */
    private static AbsoluteDate initDate;

    /** Main mass. */
    private static double mainMass;

    /** Mass Model. */
    private static MassProvider massModel;

    /** Numerical propagator. */
    private static NumericalPropagator propagator;

    /** Constant thrusy maneuver. */
    private static ContinuousThrustManeuver man1;

    /**
     * @testType UT
     * @testedFeature {@link NULL_MASS_PART_DETECTOR_TEST}
     * 
     * @description
     *              Validation test for DM 200
     *              Two parts (main and tank), one that remains unchanged (main), one that decreases towards zero
     *              (tank). At the end, the null mass part detector for the tank should stop the maneuver.
     * 
     * @testPassCriteria Final mass tank (from mass provider and state vector) is 0. Main mass stays unchanged.
     *                   Total mass is equals to main mass. The propagation goes until final date.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         bcs of MassProperty
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testTwoMassesOneGoesToZero() throws PatriusException, IOException, ParseException {
        // state
        final SpacecraftState spc = new SpacecraftState(orbit, massModel);

        // numerical propagator
        propagator.setInitialState(spc);

        // add mass provider equations
        propagator.setMassProviderEquation(massModel);
        propagator.setAdditionalStateTolerance(SpacecraftState.MASS + MAIN, new double[] { 1 }, new double[] { 1 });
        propagator
            .setAdditionalStateTolerance(SpacecraftState.MASS + THRUSTER1, new double[] { 1 }, new double[] { 1 });

        // Add constant thrust maneuver on thruster part
        propagator.addForceModel(man1);

        // propagation
        final AbsoluteDate finalDate = initDate.shiftedBy(35 * 60);
        final SpacecraftState finalState = propagator.propagate(finalDate);
        // Propagation should end without error
        Assert.assertNotNull(finalState);
        // Thruster final mass should be equal to zero
        Assert.assertEquals(0., finalState.getMass(THRUSTER1), Utils.epsilonTest);
        Assert.assertEquals(0., massModel.getMass(THRUSTER1), Utils.epsilonTest);
        // Main part final mass should be equal to the initial one
        Assert.assertEquals(mainMass, finalState.getMass(MAIN), Utils.epsilonTest);
        Assert.assertEquals(mainMass, massModel.getMass(MAIN), Utils.epsilonTest);
        // Total final mass should be equal to main mass
        Assert.assertEquals(mainMass, massModel.getTotalMass(), Utils.epsilonTest);

        // Check that the final date is equal to the expected one
        Assert.assertTrue(finalDate.equals(finalState.getDate()));
    }

    /**
     * @testType UT
     * @testedFeature {@link NULL_MASS_DETECTOR_TEST}
     * 
     * @description
     *              Validation test for DM 200
     *              Assembly with one part (main) whose mass decreases towards zero.
     *              The final mass part should be equal to zero and the propagation should be stopped.
     *              An ablation force is used in order to not modify spacecraft PV coordinates.
     *              If a ConstantThrustManeuver is used, the numerical propagator will throw an error : the total mass
     *              value will be low and then the acceleration value will be high (orbit becomes not elliptical).
     * 
     * @testPassCriteria final total mass is 0, final part mass (from mass provider and state vector) is 0. Propagation
     *                   should be stopped.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         bcs of MassProperty
     */
    @Test
    public void testOneMassGoesToZero() throws PatriusException {
        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart("Main2");

        // MASSES : 100% BODY
        builder.addProperty(new MassProperty(mainMass), "Main2");
        // mass model
        final MassProvider massModelOnePart = new MassModel(builder.returnAssembly());

        // state
        final SpacecraftState spc = new SpacecraftState(orbit, massModelOnePart);

        // numerical propagator
        propagator.setInitialState(spc);

        // add mass provider equations
        propagator.setMassProviderEquation(massModelOnePart);
        propagator.setAdditionalStateTolerance(SpacecraftState.MASS + "Main2", new double[] { 1 }, new double[] { 1 });

        // Add ablation force on main part
        propagator.addForceModel(new AblationForce(massModelOnePart, "Main2"));

        // propagation
        final AbsoluteDate finalDate = initDate.shiftedBy(35 * 60);
        final SpacecraftState finalState = propagator.propagate(finalDate);
        // Propagation should end without error
        Assert.assertNotNull(finalState);
        // Main part final mass should be equal to zero
        Assert.assertEquals(0., finalState.getMass("Main2"), Utils.epsilonTest);
        Assert.assertEquals(0., massModelOnePart.getMass("Main2"), Utils.epsilonTest);
        // Total final mass should be equal to zero
        Assert.assertEquals(0., massModelOnePart.getTotalMass(), Utils.epsilonTest);

        // Check that the final date is not equal to the expected final date
        Assert.assertFalse(finalDate.equals(finalState.getDate()));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link MASS_DETECTOR_TEST}
     * 
     * @description Propagation with constant maneuver and two parts (main and tank). A mass detector on tank mass
     *              with ACTION.STOP is set to 10kg.
     * 
     * @testPassCriteria tank mass (from mass provider and state vector) at the end of propagation is 10kg. Main mass
     *                   (from mass provider and state vector) is unchanged. Total mass equals main mass + 10kg.
     *                   Propagation should be stopped when tank mass equal to 10kg.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void test10kgMassDetector() throws PatriusException {
        // state
        final SpacecraftState spc = new SpacecraftState(orbit, massModel);

        // numerical propagator
        propagator.setInitialState(spc);

        // add mass provider equations
        propagator.setMassProviderEquation(massModel);
        propagator.setAdditionalStateTolerance(SpacecraftState.MASS + MAIN, new double[] { 1 }, new double[] { 1 });
        propagator
            .setAdditionalStateTolerance(SpacecraftState.MASS + THRUSTER1, new double[] { 1 }, new double[] { 1 });

        // Add EventDetector
        final double targetMass = 10.;
        propagator.addEventDetector(new TargetMassDetector(60., 0.01, targetMass, THRUSTER1));

        // Add constant thrust maneuver on thruster part
        propagator.addForceModel(man1);

        // propagation
        final AbsoluteDate finalDate = initDate.shiftedBy(35 * 60);
        final SpacecraftState finalState = propagator.propagate(finalDate);
        // Propagation should end without error
        Assert.assertNotNull(finalState);
        // Thruster final mass should be equal to target mass
        Assert.assertEquals(targetMass, finalState.getMass(THRUSTER1), Utils.epsilonTest);
        Assert.assertEquals(targetMass, massModel.getMass(THRUSTER1), Utils.epsilonTest);
        // Main part final mass should be equal to the initial one
        Assert.assertEquals(mainMass, finalState.getMass(MAIN), Utils.epsilonTest);
        Assert.assertEquals(mainMass, massModel.getMass(MAIN), Utils.epsilonTest);
        // Total mass should equals main mass + target mass
        Assert.assertEquals(mainMass + targetMass, massModel.getTotalMass(), Utils.epsilonTest);
        // Check that the final date is not equal to the expected final date
        Assert.assertFalse(finalDate.equals(finalState.getDate()));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_DETECTOR_TEST}
     * 
     * @description Propagation with constant maneuver and three parts (main and 2 tanks). A mass detector on tank mass
     *              with ACTION.STOP is set to 10kg.
     * 
     * @testPassCriteria tank mass (from mass provider and state vector) at the end of propagation is 10kg. Main mass
     *                   (from mass provider and state vector) is unchanged. Total mass equals main mass + 10kg.
     *                   Propagation should be stopped when tank mass equal to 10kg.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void test1ConstantManeuverAnd10kgMassDetector() throws IllegalArgumentException, PatriusException,
        IOException, ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 300e3, 0.001,
            MathLib.toRadians(45),
            MathLib.toRadians(3), MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);

        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        final Assembly spacecraft = getSphericalVehicle(1000, 1, 2.3, 0.3, 0.4, 0.3);
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        propagator.addEventDetector(new TargetMassDetector(60., 0.0001, 10., "Reservoir1"));
        final ContinuousThrustManeuver man1 = new ContinuousThrustManeuver(initDate.shiftedBy(600), 147.08,
            new PropulsiveProperty(new Var(400), new Var(20)),
            new VarVect(Vector3D.MINUS_I), massModel, tank1, FramesFactory.getGCRF());
        propagator.addForceModel(man1);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(35 * 60));

        // Check
        checkMasses(finalState, massModel, 500, 10, 200);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#NULL_MASS_PART_DETECTOR_TEST}
     * 
     * @description Propagation with constant maneuver and three parts (main and 2 tanks). Tank 1 is emptied, tank 2 is
     *              partly emptied.
     * 
     * @testPassCriteria tank 1 mass (from mass provider and state vector) at the end of propagation is 0kg. Main mass
     *                   (from mass provider and state vector) is unchanged. Total mass equals main mass + tank 2 left
     *                   mass.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void test3ConstantManeuvers() throws IllegalArgumentException, PatriusException,
        IOException, ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 300e3, 0.001,
            MathLib.toRadians(45),
            MathLib.toRadians(3), MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);

        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        final Assembly spacecraft = getSphericalVehicle(1000, 1, 2.3, 0.3, 0.4, 0.3);
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        final ContinuousThrustManeuver man1 = new ContinuousThrustManeuver(initDate.shiftedBy(600), 150,
            new PropulsiveProperty(new Var(400), new Var(20)),
            new VarVect(Vector3D.MINUS_I), massModel, tank1, FramesFactory.getGCRF());
        final ContinuousThrustManeuver man2 = new ContinuousThrustManeuver(initDate.shiftedBy(600.01), 60,
            new PropulsiveProperty(new Var(400), new Var(300)),
            new VarVect(Vector3D.PLUS_I), massModel, tank2, FramesFactory.getGCRF());
        final ContinuousThrustManeuver man3 = new ContinuousThrustManeuver(initDate.shiftedBy(1600.02), 60,
            new PropulsiveProperty(new Var(400), new Var(100)),
            new VarVect(Vector3D.PLUS_J), massModel, tank2, FramesFactory.getGCRF());
        propagator.addForceModel(man1);
        propagator.addForceModel(man2);
        propagator.addForceModel(man3);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(35 * 60));

        // Check
        checkMasses(finalState, massModel, 500, 0, 200 - 32.63076);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_DETECTOR_TEST}
     * 
     * @description Propagation with constant maneuver and three parts (main and 2 tanks). Tank 2 is partly emptied.
     * 
     * @testPassCriteria Main mass and tank 1 mass (from mass provider and state vector) is unchanged.
     *                   Total mass equals main mass + tank 1 mass + tank 2 left mass.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void test1ConstantManeuver() throws IllegalArgumentException, PatriusException,
        IOException, ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 300e3, 0.001,
            MathLib.toRadians(45),
            MathLib.toRadians(3), MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);

        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        final Assembly spacecraft = getSphericalVehicle(1000, 1, 2.3, 0.3, 0.4, 0.3);
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        final ContinuousThrustManeuver man1 = new ContinuousThrustManeuver(initDate.shiftedBy(600), 389,
            new PropulsiveProperty(new Var(400), new Var(100)),
            new VarVect(Vector3D.MINUS_I), massModel, tank2, FramesFactory.getGCRF());
        propagator.addForceModel(man1);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(35 * 60));

        // Check
        checkMasses(finalState, massModel, 500, 300, 41.74 - 0.408);
    }

    /**
     * Check final state and mass model.
     * 
     * @param finalState
     *        final state
     * @param massModel
     *        mass model
     * @param exp_Body
     *        expected final body mass
     * @param exp_Reservoir1
     *        expected final tank 1 mass
     * @param exp_Reservoir2
     *        expected final tank 2 mass
     * @throws PatriusException
     *         thrown if additional states could not be retrieved
     */
    private static void checkMasses(final SpacecraftState finalState, final MassProvider massModel,
                                    final double exp_Body,
                                    final double exp_Reservoir1, final double exp_Reservoir2) throws PatriusException {
        Assert.assertEquals(massModel.getTotalMass(), exp_Body + exp_Reservoir1 + exp_Reservoir2, 1E-3);
        Assert.assertEquals(massModel.getMass("BODY"), exp_Body, 0);
        Assert.assertEquals(massModel.getMass("Reservoir1"), exp_Reservoir1, 1E-3);
        Assert.assertEquals(massModel.getMass("Reservoir2"), exp_Reservoir2, 1E-3);
        Assert.assertEquals(massModel.getMass("BODY"), finalState.getAdditionalState("MASS_BODY")[0], 0);
        Assert.assertEquals(massModel.getMass("Reservoir1"), finalState.getAdditionalState("MASS_Reservoir1")[0], 0);
        Assert.assertEquals(massModel.getMass("Reservoir2"), finalState.getAdditionalState("MASS_Reservoir2")[0], 0);
    }

    /**
     * Returns a spherical spacecraft with parameters allowing to compute drag and SRP accelerations.
     * 
     * @param mass
     *        vehicle mass
     * @param radius
     *        vehicle radius
     * @param normDragCoef
     *        vehicle normal drag force coefficient
     * @param tangentDragCoef
     *        vehicle tangential force coefficient
     * @param ka
     *        vehicle absorbed coef for SRP
     * @param ks
     *        vehicle specular reflectance coef for SRP
     * @param kd
     *        vehicle diffuse reflectance coef for SRP
     * @return built vehicle
     * @throws PatriusException
     */
    private static Assembly getSphericalVehicle(final double mass, final double radius, final double DragCoef,
                                                final double ka, final double ks, final double kd)
        throws PatriusException {

        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);

        // MASSES : 50% BODY, 30% Reservoir1, 20% Reservoir2
        tank1 = new TankProperty(0.3 * mass);
        tank2 = new TankProperty(0.2 * mass);
        builder.addProperty(new MassProperty(0.5 * mass), "BODY");
        builder.addProperty(tank1, "Reservoir1");
        builder.addProperty(tank2, "Reservoir2");
        builder.addProperty(new AeroSphereProperty(1., DragCoef), "BODY");
        builder.addProperty(new RadiativeSphereProperty(radius), "BODY");
        builder.addProperty(new RadiativeProperty(ka, ks, kd), "BODY");

        return builder.returnAssembly();
    }

    @Before
    public void setUp() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
        final Frame itrf = FramesFactory.getITRF();
        initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());

        // orbit
        final Frame gcrf = FramesFactory.getGCRF();
        final double a = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 300e3;
        final double e = 0.001;
        final double inc = MathLib.toRadians(45);
        final double pom = MathLib.toRadians(3);
        final double gom = MathLib.toRadians(2);
        final double M = MathLib.toRadians(1);
        orbit = new KeplerianOrbit(a, e, inc, pom, gom, M, PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);

        // Assembly
        mainMass = 600;
        final double thrusterMass = 400;
        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart(MAIN);
        builder.addPart(THRUSTER1, MAIN, Transform.IDENTITY);

        // MASSES : 60% BODY, 40% Reservoir1
        builder.addProperty(new MassProperty(mainMass), MAIN);
        final TankProperty tank = new TankProperty(thrusterMass);
        builder.addProperty(tank, THRUSTER1);
        // mass model
        massModel = new MassModel(builder.returnAssembly());

        // numerical propagator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final DormandPrince853Integrator integrator = new DormandPrince853Integrator(1e-5, 50,
            absTOL, relTOL);
        propagator = new NumericalPropagator(integrator);

        // a) Add Gravitational force model
        // add a reader for gravity fields

        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr'
        // file
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // we get the data as extracted from the file
        // degree
        final int n = 60;
        // order
        final int m = 60;
        final double[][] C = provider.getC(n, m, false);
        final double[][] S = provider.getS(n, m, false);

        // return perturbing force (ITRF2008 central body frame)
        final DrozinerAttractionModel potential = new DrozinerAttractionModel(itrf, provider.getAe(), provider.getMu(),
            C, S);
        propagator.addForceModel(potential);

        final AbsoluteDate t1 = initDate.shiftedBy(50);
        man1 = new ContinuousThrustManeuver(t1, 200, new PropulsiveProperty(new Var(400), new Var(20)),
            new VarVect(Vector3D.MINUS_I), massModel, tank, FramesFactory.getGCRF());
    }

    /**
     * Ablation force: only decreases mass from provided part.
     * 
     * @author Sophie Laurens
     * 
     * @version $Id$
     * 
     * @since 2.3
     * 
     */
    private class AblationForce extends Parameterizable implements ForceModel {

        /** Serializable UID. */
        private static final long serialVersionUID = -817705676698074892L;

        /** Mass model. */
        private final MassProvider massModel;

        /** Name of the part ablated. */
        private final String partName;

        /**
         * Constructor.
         * 
         * @param massProvider2
         *        massProvider
         * @param partName2
         *        String
         */
        public AblationForce(final MassProvider massProvider2, final String partName2) {
            this.massModel = massProvider2;
            this.partName = partName2;
        }

        /** {@inheritDoc} */
        @Override
        public void
            addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
                throws PatriusException {

            // compute thrust acceleration in inertial frame
            adder.addAcceleration(this.computeAcceleration(s), s.getFrame());

            // add flow rate to mass variation
            this.massModel.addMassDerivative(this.partName, -1.0);
        }

        /**
         * {@inheritDoc}
         * 
         * @throws PatriusException
         *         if the mass becomes negative
         */
        @Override
        public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
            return Vector3D.ZERO;
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector[] getEventsDetectors() {
            return new EventDetector[] { new NullMassPartDetector(this.massModel, this.partName) };
        }

        /** {@inheritDoc} */
        @Override
        public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
            // Nothing to do
        }
    }

    /**
     * <p>
     * Mass Detector : Stop propagation when mass part reach a specific mass.
     * </p>
     * 
     * @concurrency immutable
     * 
     * @author maggioranic
     * 
     * @version $Id$
     * 
     * @since 3.0
     * 
     */
    private class TargetMassDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 3962244161818198542L;

        /**
         * Target mass
         */
        private final double targetMass;

        /**
         * Name of the part concerned
         */
        private final String part;

        /**
         * Simple constructor
         * 
         * @param maxCheck
         *        maximum checking interval (s)
         * @param threshold
         *        convergence threshold (s)
         * @param mass
         *        the target mass (kg)
         * @param partName
         *        the part name
         * 
         * @since 3.0
         */
        public TargetMassDetector(final double maxCheck, final double threshold, final double mass,
                                  final String partName) {
            super(maxCheck, threshold);
            this.targetMass = mass;
            this.part = partName;
        }

        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
            return Action.STOP;
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            return (s.getMass(this.part) - this.targetMass);
        }

        /**
         *
         */
        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public EventDetector copy() {
            return null;
        }
    }

    /**
     * Constant variable.
     */
    private class Var implements IDependentVariable<SpacecraftState> {

        /** Serializable UID. */
        private static final long serialVersionUID = -8762272472624256500L;

        /** Value. */
        private final double value;

        /**
         * Constructor.
         * 
         * @param value
         *        value
         */
        public Var(final double value) {
            this.value = value;
        }

        @Override
        public double value(final SpacecraftState x) {
            return this.value;
        }
    }

    /**
     * Constant variable vector.
     */
    private class VarVect implements IDependentVectorVariable<SpacecraftState> {

        /** Serializable UID. */
        private static final long serialVersionUID = -9076971483329877568L;

        /** Value. */
        private final Vector3D value;

        /**
         * Constructor.
         * 
         * @param value
         *        value
         */
        public VarVect(final Vector3D value) {
            this.value = value;
        }

        @Override
        public Vector3D value(final SpacecraftState x) {
            return this.value;
        }
    }
}
