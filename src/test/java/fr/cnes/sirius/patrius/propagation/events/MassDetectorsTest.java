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
 * @history creation 25/04/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:200:28/08/2014:(creation) dealing with a negative mass in the propagator
 * VERSION::DM:300:23/04/2015:Creation multi propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:414:24/03/2015: proper handling of mass evolution
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:593:06/04/2016: corrected event detection (derivatives reinitialisation)
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:706:13/12/2016: synchronisation problem with the Assemby mass
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
import java.util.Map;
import java.util.Set;

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
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudesSequence;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.detectors.NullMassDetector;
import fr.cnes.sirius.patrius.events.detectors.NullMassPartDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.BalminoGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.maneuvers.ManeuversSequence;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisAltitudeParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ReentryParameters;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * @description Test class for the detectors for total mass and for each part's mass. Functional tests.
 *              Same tests are provided in {@link MassDetectorsVariableTest} for variable thrusts.
 * 
 * @see NullMassDetector and NullMassPartDetector
 * 
 * @author Sophie Laurens
 * 
 * @version $Id$
 * 
 * @since 2.3
 * 
 */
public class MassDetectorsTest {

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

    /** First state Id. */
    private static final String STATE1 = "state1";

    /** Second state Id. */
    private static final String STATE2 = "state2";

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

    /** Constant thrust maneuver. */
    private static ContinuousThrustManeuver man1;

    @Test
    public void testAssemblySynchronization1() throws PatriusException {

        // numerical propagator
        propagator.setInitialState(new SpacecraftState(orbit, massModel));

        // add mass provider equations
        propagator.setMasterMode(new PatriusStepHandler(){

            /** Serializable UID. */
            private static final long serialVersionUID = 7534134366153950740L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    final SpacecraftState currentState = interpolator.getInterpolatedState();
                    Assert.assertEquals(currentState.getAdditionalState(SpacecraftState.MASS + THRUSTER1)[0],
                        massModel.getMass(THRUSTER1), Utils.epsilonTest);
                    Assert.assertEquals(currentState.getMass(MAIN) + currentState.getMass(THRUSTER1),
                        massModel.getTotalMass(), Utils.epsilonTest);
                } catch (final PatriusException e) {
                    Assert.fail();
                }
            }
        });

        propagator.setMassProviderEquation(massModel);

        propagator.setAdditionalStateTolerance(SpacecraftState.MASS + MAIN, new double[] { 1 }, new double[] { 1 });
        propagator
            .setAdditionalStateTolerance(SpacecraftState.MASS + THRUSTER1, new double[] { 1 }, new double[] { 1 });

        // Add constant thrust maneuver on thruster part
        propagator.addForceModel(man1);

        // propagation
        final AbsoluteDate finalDate = initDate.shiftedBy(35 * 60);
        propagator.propagate(finalDate);
    }

    @Test
    public void testAssemblySynchronization2() throws PatriusException {

        // numerical propagator
        propagator.setInitialState(new SpacecraftState(orbit, massModel));

        propagator.setMassProviderEquation(massModel);

        // add mass provider equations
        propagator.setMasterMode(new PatriusStepHandler(){

            /** Serializable UID. */
            private static final long serialVersionUID = 8088996028886626054L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    final SpacecraftState currentState = interpolator.getInterpolatedState();
                    Assert.assertEquals(currentState.getAdditionalState(SpacecraftState.MASS + THRUSTER1)[0],
                        massModel.getMass(THRUSTER1), Utils.epsilonTest);
                    Assert.assertEquals(currentState.getMass(MAIN) + currentState.getMass(THRUSTER1),
                        massModel.getTotalMass(), Utils.epsilonTest);
                } catch (final PatriusException e) {
                    Assert.fail();
                }
            }
        });

        propagator.setAdditionalStateTolerance(SpacecraftState.MASS + MAIN, new double[] { 1 }, new double[] { 1 });
        propagator
            .setAdditionalStateTolerance(SpacecraftState.MASS + THRUSTER1, new double[] { 1 }, new double[] { 1 });

        // Add constant thrust maneuver on thruster part
        propagator.addForceModel(man1);

        // propagation
        final AbsoluteDate finalDate = initDate.shiftedBy(35 * 60);
        propagator.propagate(finalDate);
    }

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
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3.1
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
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
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
     * @testType UT
     * @testedFeature {@link NULL_MASS_DETECTOR_TEST}
     * 
     * @description
     *              Validation test for DM 200 in multi spacecraft case
     *              One state with an assembly with one part (main) whose mass decreases towards zero.
     *              The final mass part should be equal to zero and the global propagation should be stopped.
     *              An ablation force is used in order to not modify spacecraft PV coordinates.
     *              If a ConstantThrustManeuver is used, the multi numerical propagator will throw an error : the total
     *              mass
     *              value will be low and then the acceleration value will be high (orbit becomes not elliptical).
     * 
     * @testPassCriteria final total mass of state 1 is 0, final part mass (from mass provider and state vector) is 0.
     *                   Final total mass of state 2 remain the same.
     *                   Propagation should be stopped.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         bcs of MassProperty
     */
    @Test
    public void testOneMassGoesToZeroInMultiProp() throws PatriusException {
        // Assembly
        AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart("Main2");

        // MASSES : 100% BODY
        builder.addProperty(new MassProperty(mainMass), "Main2");
        // mass model
        final MassProvider massModelOnePart1 = new MassModel(builder.returnAssembly());

        // Assembly
        builder = new AssemblyBuilder();

        builder.addMainPart("Main3");

        // MASSES : 100% BODY
        builder.addProperty(new MassProperty(mainMass), "Main3");
        // mass model
        final MassProvider massModelOnePart2 = new MassModel(builder.returnAssembly());

        // state
        final SpacecraftState spc1 = new SpacecraftState(orbit, massModelOnePart1);
        final SpacecraftState spc2 = new SpacecraftState(orbit, massModelOnePart2);

        // multi numerical propagator
        final FirstOrderIntegrator integratorMultiSat = new DormandPrince853Integrator(.1, 60, 1e-9, 1e-9);
        final MultiNumericalPropagator mainPropagator = new MultiNumericalPropagator(integratorMultiSat);
        mainPropagator.addInitialState(spc1, STATE1);
        mainPropagator.addInitialState(spc2, STATE2);

        // add mass provider equations
        mainPropagator.setMassProviderEquation(massModelOnePart1, STATE1);
        mainPropagator.setAdditionalStateTolerance(SpacecraftState.MASS + "Main2", new double[] { 1 },
            new double[] { 1 },
            STATE1);
        mainPropagator.setMassProviderEquation(massModelOnePart2, STATE2);
        // Add ablation force on main part
        mainPropagator.addForceModel(new AblationForce(massModelOnePart1, "Main2"), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator
            .getInitialStates().get(STATE1).getMu())), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator
            .getInitialStates().get(STATE2).getMu())), STATE2);

        // propagation
        final AbsoluteDate finalDate = initDate.shiftedBy(35 * 60);
        final Map<String, SpacecraftState> finalStates = mainPropagator.propagate(finalDate);
        // Propagation should end without error
        Assert.assertNotNull(finalStates);
        // Main part final mass should be equal to zero
        Assert.assertEquals(0., finalStates.get(STATE1).getMass("Main2"), Utils.epsilonTest);
        Assert.assertEquals(0., massModelOnePart1.getMass("Main2"), Utils.epsilonTest);
        // Total final mass should be equal to zero
        Assert.assertEquals(0., massModelOnePart1.getTotalMass(), Utils.epsilonTest);

        // Second state mass should remain the same
        Assert.assertEquals(mainMass, finalStates.get(STATE2).getMass("Main3"), Utils.epsilonTest);
        Assert.assertEquals(mainMass, massModelOnePart2.getMass("Main3"), Utils.epsilonTest);
        Assert.assertEquals(mainMass, massModelOnePart2.getTotalMass(), Utils.epsilonTest);

        // Check that the final date is not equal to the expected final date
        Assert.assertFalse(finalDate.equals(finalStates.get(STATE1).getDate()));
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
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
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

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);
        final TankProperty tank1 = new TankProperty(300);
        final TankProperty tank2 = new TankProperty(200);
        builder.addProperty(new MassProperty(500), "BODY");
        builder.addProperty(tank1, "Reservoir1");
        builder.addProperty(tank2, "Reservoir2");
        final Assembly spacecraft = builder.returnAssembly();
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        propagator.addEventDetector(new TargetMassDetector(60., 0.0001, 10., "Reservoir1"));
        final ContinuousThrustManeuver man1 =
            new ContinuousThrustManeuver(initDate.shiftedBy(600), 147.08, new PropulsiveProperty(400, 20),
                Vector3D.MINUS_I, massModel, tank1, FramesFactory.getGCRF());
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

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);
        final TankProperty tank1 = new TankProperty(300);
        final TankProperty tank2 = new TankProperty(200);
        builder.addProperty(new MassProperty(500), "BODY");
        builder.addProperty(tank1, "Reservoir1");
        builder.addProperty(tank2, "Reservoir2");
        final Assembly spacecraft = builder.returnAssembly();
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        final ContinuousThrustManeuver man1 =
            new ContinuousThrustManeuver(initDate.shiftedBy(600), 150, new PropulsiveProperty(400, 20),
                Vector3D.MINUS_I, massModel, tank1, FramesFactory.getGCRF());
        final ContinuousThrustManeuver man2 =
            new ContinuousThrustManeuver(initDate.shiftedBy(600.01), 60, new PropulsiveProperty(400, 300),
                Vector3D.PLUS_I, massModel, tank2, FramesFactory.getGCRF());
        final ContinuousThrustManeuver man3 =
            new ContinuousThrustManeuver(initDate.shiftedBy(1600.02), 60, new PropulsiveProperty(400, 100),
                Vector3D.PLUS_J, massModel, tank2, FramesFactory.getGCRF());
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

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);
        final TankProperty tank1 = new TankProperty(300);
        final TankProperty tank2 = new TankProperty(200);
        builder.addProperty(new MassProperty(500), "BODY");
        builder.addProperty(tank1, "Reservoir1");
        builder.addProperty(tank2, "Reservoir2");
        final Assembly spacecraft = builder.returnAssembly();
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        final ContinuousThrustManeuver man1 =
            new ContinuousThrustManeuver(initDate.shiftedBy(600), 389, new PropulsiveProperty(400, 100),
                Vector3D.MINUS_I, massModel, tank2, FramesFactory.getGCRF());
        propagator.addForceModel(man1);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(35 * 60));

        // Check
        checkMasses(finalState, massModel, 500, 300, 41.74 - 0.408);
    }

    /**
     * @testType UT
     * 
     * @description Propagation with 10 pieces. This aim of this test is to check masses in mass model are retrieved in
     *              right order
     *              as it turns out on some computer, masses are "randomly" retrieved.
     * 
     * @testPassCriteria mass are retrieved in right order.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testMassOrder() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final String prefix1 = "Body";
        final String prefix2 = "Reservoir";
        final int n = 10;

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0.001, 0.1, 0.2, 0.3, 0.4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart(prefix1);
        builder.addProperty(new MassProperty(100), prefix1);
        for (int i = 1; i <= n; i++) {
            builder.addPart(prefix2 + i, prefix1, Transform.IDENTITY);
            builder.addProperty(new MassProperty(i), prefix2 + i);
        }
        final Assembly spacecraft = builder.returnAssembly();
        final MassProvider massModel = new MassModel(spacecraft);

        // Propagator
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(60.));
        propagator.setMassProviderEquation(massModel);
        propagator.setInitialState(new SpacecraftState(initialOrbit, massModel));
        propagator.setAdditionalStateTolerance("MASS_" + prefix1, new double[] { 1e-06 }, new double[] { 1e-09 });
        for (int i = 1; i <= n; i++) {
            propagator.setAdditionalStateTolerance("MASS_" + prefix2 + i, new double[] { 1e-06 },
                new double[] { 1e-09 });
        }

        // Propagation
        propagator.propagate(initDate.shiftedBy(10));

        // Check
        Assert.assertEquals(100, massModel.getMass(prefix1), 0);
        for (int i = 1; i <= n; i++) {
            Assert.assertEquals(i, massModel.getMass(prefix2 + i), 0);
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_DETECTOR_TEST}
     * 
     * @description Propagation with constant maneuver and dormand prince integrator. Attitude sequence at maneuvers
     *              start/end dates.
     * 
     * @testPassCriteria Mass at the end of maneuver = mass at the end of the propagation.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDormandPrince() throws PatriusException, IOException, ParseException {

        // Patrius Dataset initialization
        Utils.setDataRoot("regular-dataPBASE");

        final Frame frameGCRF = FramesFactory.getGCRF();
        final TimeScale tuc = TimeScalesFactory.getUTC();

        // Initial orbit
        final AbsoluteDate date0 = new AbsoluteDate("2010-01-01T12:00:00.000", tuc);
        final double hp = 300.e+3;
        final double ha = 300.e+3;
        final double inc = MathLib.toRadians(51.6);
        final double pom = MathLib.toRadians(0.);
        final double gom = MathLib.toRadians(0.);
        final double anm = MathLib.toRadians(0.);
        final double mu = Constants.WGS84_EARTH_MU;
        final double ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final ApsisAltitudeParameters par =
            new ApsisAltitudeParameters(hp, ha, inc, pom, gom, anm, PositionAngle.MEAN, mu,
                ae);
        final Orbit orbit = new ApsisOrbit(par, frameGCRF, date0);

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        final double dryMass = 10000.;
        final double ergMass = 500.;
        final String TANK_NAME = "TANK1";
        builder.addMainPart("MAIN");
        builder.addProperty(new MassProperty(dryMass), "MAIN");
        builder.addPart(TANK_NAME, "MAIN", Vector3D.ZERO, Rotation.IDENTITY);
        final TankProperty tankProperty = new TankProperty(ergMass);
        builder.addProperty(tankProperty, TANK_NAME);
        final Assembly assembly = builder.returnAssembly();
        final MassProvider mm = new MassModel(assembly);

        // Manoeuvers
        final ManeuversSequence seq = new ManeuversSequence(10., 10.);

        final double maxCheck = 10.;
        final double threshold = 1.e-3;

        final double thrust = 1000.;
        final double isp = 320;
        final double relStartDate = 100.;
        final double duration = 604.5;
        final Vector3D dir = Vector3D.PLUS_I;
        final AbsoluteDate startThrustDate = date0.shiftedBy(relStartDate);
        final AbsoluteDate endThrustDate = startThrustDate.shiftedBy(duration);
        final EventDetector startThrustEvent = new DateDetector(startThrustDate, maxCheck, threshold);
        final EventDetector endThrustEvent = new DateDetector(endThrustDate, maxCheck, threshold);
        final ContinuousThrustManeuver man =
            new ContinuousThrustManeuver(startThrustEvent, endThrustEvent, new PropulsiveProperty(thrust, isp),
                dir, mm, tankProperty);
        seq.add(man);

        // SpacecraftState
        final SpacecraftState objState = new SpacecraftState(orbit, mm);

        // Integrator
        final double minStep = 0.1;
        final double maxStep = 120.;
        final double[] absTol = { 1.e-3, 1.e-3, 1.e-3, 1.e-6, 1.e-6, 1.e-6 };
        final double[] relTol = { 1.e-6, 1.e-6, 1.e-6, 1.e-6, 1.e-6, 1.e-6 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, absTol, relTol);

        // Propagateur
        final NumericalPropagator propagator = new NumericalPropagator(integrator, objState.getFrame(),
            OrbitType.CARTESIAN, PositionAngle.TRUE);

        propagator.setMassProviderEquation(mm);
        // Mass tolerances
        final double[] absTolMass = { 1.e-3 };
        final double[] relTolMass = { 1.e-6 };
        propagator.setAdditionalStateTolerance("MASS_" + TANK_NAME, absTolMass, relTolMass);

        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();
        final double[][] c = data.getC(36, 36, true);
        final double[][] s = data.getS(36, 36, true);
        final ForceModel potentiel = new DirectBodyAttraction(new BalminoGravityModel(FramesFactory.getITRF(), ae, mu,
            c, s));
        propagator.addForceModel(potentiel);
        propagator.resetInitialState(objState);

        // Adding the maneuver sequence
        seq.applyTo(propagator);

        // Adding the End Of Thrust Event
        final EndOfThrustEventDetector endOfThrustEvent = new EndOfThrustEventDetector(endThrustDate, assembly);
        propagator.addEventDetector(endOfThrustEvent);

        // Adding attitude sequence
        final AttitudesSequence seqAtt = new AttitudesSequence();
        final EventDetector startThrustAtt = new DateDetector(startThrustDate, maxCheck, threshold, Action.RESET_STATE);
        final EventDetector endThrustDateAtt = new DateDetector(endThrustDate, maxCheck, threshold, Action.RESET_STATE);
        final AttitudeLaw law1 = new LofOffset(LOFType.LVLH, RotationOrder.ZYX, 0., 0., 0.);
        final AttitudeLaw law2 = new LofOffset(LOFType.TNW, RotationOrder.ZYX, MathLib.toRadians(180.), 0., 0.);
        seqAtt.addSwitchingCondition(law1, startThrustAtt, true, false, law2);
        seqAtt.addSwitchingCondition(law2, endThrustDateAtt, true, false, law1);
        propagator.setAttitudeProvider(seqAtt);
        seqAtt.registerSwitchEvents(propagator);

        // Adding altitude test
        final ManeuverAltitudeConstraintEventTest altTest = new ManeuverAltitudeConstraintEventTest(1, startThrustDate,
            endThrustDate, 120.e+3, maxCheck, threshold);
        propagator.addEventDetector(altTest);

        // Propagation
        final double propagationDuration = 5000.;
        final AbsoluteDate finalDate = objState.getDate().shiftedBy(propagationDuration);
        final SpacecraftState finalState = propagator.propagate(finalDate);

        // Check
        final double expected = endOfThrustEvent.getMass() - dryMass;
        final double actual = finalState.getMass(TANK_NAME);
        Assert.assertEquals(0., (actual - expected) / expected, 3E-15);
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
        final TankProperty tankProperty = new TankProperty(thrusterMass);
        builder.addProperty(tankProperty, THRUSTER1);
        // mass model
        massModel = new MassModel(builder.returnAssembly());

        // numerical propagator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final DormandPrince853Integrator integrator = new DormandPrince853Integrator(1e-5, 50, absTOL, relTOL);
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
        final DrozinerGravityModel potential = new DrozinerGravityModel(itrf, provider.getAe(), provider.getMu(),
            C, S);
        propagator.addForceModel(new DirectBodyAttraction(potential));

        final AbsoluteDate t1 = initDate.shiftedBy(50);

        // Engine property
        final PropulsiveProperty engine = new PropulsiveProperty(new Parameter("cvbcvb", 400.), new Parameter(
            "isp", 20.));
        man1 = new ContinuousThrustManeuver(t1, 200, engine, Vector3D.MINUS_I, massModel, tankProperty,
            FramesFactory.getGCRF());
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
        private static final long serialVersionUID = -3155170012901158469L;

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
            this.massModel.addMassDerivative(this.partName, -1);
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
     * @since 2.3.1
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
         * @since 2.3.1
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
         * @see fr.cnes.sirius.patrius.events.AbstractDetector#shouldBeRemoved()
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
     * Specific class that corresponds to the end of thrust detector.
     * 
     * @version $Id$
     * 
     * @since 3.2
     */
    private class EndOfThrustEventDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 1715662780823970448L;

        /** Assembly corresponding to the current vehicle */
        private final Assembly assembly;

        /** Mass at the end of thrust */
        private double mass;

        /**
         * Constructor
         * 
         * @param target
         *        date of the event.
         * @param assembly
         *        assembly corresponding to the current vehicle.
         */
        public EndOfThrustEventDetector(final AbsoluteDate target, final Assembly assembly) {
            super(target, 1., 0.01);
            this.assembly = assembly;
            this.mass = 0;
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {

            // Compute total mass
            final Set<String> partNames = this.assembly.getAllPartsNames();
            this.mass = 0.;
            for (final String string : partNames) {
                this.mass = this.mass + s.getMass(string);
            }

            return Action.RESET_STATE;
        }

        /**
         * Method to get the total mass at the end of thrust.
         * 
         * @return total mass at the end of thrust (kg)
         */
        public double getMass() {
            return this.mass;
        }
    }

    /**
     * Class defining and event when reaching an altitude maneuver constraint.
     * 
     * @version $Id$
     * 
     * @since 3.2
     */
    public class ManeuverAltitudeConstraintEventTest implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -5935218129960912555L;

        /** Starting maneuver date */
        private final AbsoluteDate startDate;

        /** End Maneuver date */
        private final AbsoluteDate endDate;

        /** Minimum maneuver altitude (m) */
        private final double minAlt;

        /** Max interval for searching the event (s) */
        private final double maxCheck;
        /** Threshold for searching the event (s) */
        private final double threshold;

        /**
         * Constructor.
         * 
         * @param rank
         *        Rank of the maneuver
         * @param startDate
         *        Starting maneuver date : after which the computation will be done
         * @param endDate
         *        End Maneuver date : date after which the computation will not be done again
         * @param minAlt
         *        minimum maneuver altitude (m)
         * @param maxCheck
         *        Max interval for searching the event (s)
         * @param threshold
         *        Threshold for searching the event (s)
         */
        public ManeuverAltitudeConstraintEventTest(final int rank, final AbsoluteDate startDate,
                                                   final AbsoluteDate endDate,
                                                   final double minAlt, final double maxCheck, final double threshold) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.minAlt = minAlt;
            this.maxCheck = maxCheck;
            this.threshold = threshold;
        }

        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
            return Action.STOP;
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {

            double dAlt = Double.POSITIVE_INFINITY;

            if ((s.getDate().compareTo(this.startDate) >= 0.) && (s.getDate().compareTo(this.endDate) <= 0.)) {
                // Compute reentry parameters
                final CartesianParameters carPar = new CartesianParameters(s.getPVCoordinates(),
                    Constants.WGS84_EARTH_MU);
                final ReentryParameters renParam = carPar.getReentryParameters(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING);
                final double alt = renParam.getAltitude();
                dAlt = alt - this.minAlt;
            }

            return dAlt;
        }

        @Override
        public double getMaxCheckInterval() {
            return this.maxCheck;
        }

        @Override
        public int getSlopeSelection() {
            return 2;
        }

        @Override
        public double getThreshold() {
            return this.threshold;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            return oldState;
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public int getMaxIterationCount() {
            return 20;
        }

        @Override
        public EventDetector copy() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean filterEvent(final SpacecraftState state,
                final boolean increasing,
                final boolean forward) throws PatriusException {
            // Do nothing by default, event is not filtered
            return false;
        }
    }
}
