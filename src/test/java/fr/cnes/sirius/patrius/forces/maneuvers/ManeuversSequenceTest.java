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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:699:26/01/2017:Remove overlapping check for maneuvers defined by generic events
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.util.HashSet;
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
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModelsDataTest;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AltitudeDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for the ManeuversSequence class.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class ManeuversSequenceTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle maneuvers list validation
         * 
         * @featureDescription test the list of maneuvers
         * 
         * @coveredRequirements DV-PROPU_110, DV-PROPU_120, DV-PROPU_130
         */
        MANEUVERS_LIST_VALIDATION
    }

    /** The first constant thrust maneuver. */
    private ContinuousThrustManeuver constant1;

    /** The second constant thrust maneuver. */
    private ContinuousThrustManeuver constant2;

    /** The first variable thrust maneuver. */
    private ContinuousThrustManeuver variable1;

    /** The second variable thrust maneuver. */
    private ContinuousThrustManeuver variable2;

    /** The first impulse maneuver. */
    private ImpulseManeuver impulse1;

    /** The second impulse maneuver. */
    private ImpulseManeuver impulse2;

    /** The third impulse maneuver. */
    private ImpulseManeuver impulse3;

    /** The fourth impulse maneuver. */
    private ImpulseManeuver impulse4;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MANEUVERS_LIST_VALIDATION}
     * 
     * @testedMethod {@link ManeuversSequence#ManeuversSequence(double, double)}
     * @testedMethod {@link ManeuversSequence#add(ContinuousThrustManeuver)}
     * @testedMethod {@link ManeuversSequence#add(ImpulseManeuver)}
     * @testedMethod {@link ManeuversSequence#remove(ContinuousThrustManeuver)}
     * @testedMethod {@link ManeuversSequence#remove(ImpulseManeuver)}
     * @testedMethod {@link ManeuversSequence#getSize()}
     * 
     * @description tests the creation of the sequence and the methods adding and removing maneuvers in the list
     * 
     * @input some continue and impulse maneuvers
     * 
     * @output the list
     * 
     * @testPassCriteria the number of elements in the list should be the expected one.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testCreateSequence() {
        // creates the sequence:
        final ManeuversSequence sequence = new ManeuversSequence(10.0, 20.0);
        // tests the adding of maneuvers in the list:
        Assert.assertEquals(0, sequence.getSize());
        Assert.assertTrue(sequence.add(this.constant1));
        Assert.assertEquals(1, sequence.getSize());
        Assert.assertFalse(sequence.add(this.constant2));
        Assert.assertEquals(1, sequence.getSize());
        Assert.assertTrue(sequence.add(this.variable1));
        Assert.assertEquals(2, sequence.getSize());
        Assert.assertTrue(sequence.add(this.variable2));
        Assert.assertEquals(3, sequence.getSize());
        Assert.assertFalse(sequence.add(this.impulse1));
        Assert.assertEquals(3, sequence.getSize());
        Assert.assertTrue(sequence.add(this.impulse2));
        Assert.assertEquals(4, sequence.getSize());
        Assert.assertFalse(sequence.add(this.impulse3));
        Assert.assertEquals(4, sequence.getSize());
        Assert.assertTrue(sequence.add(this.impulse4));
        Assert.assertEquals(5, sequence.getSize());
        // tests the removing of maneuvers from the list:
        Assert.assertFalse(sequence.remove(this.constant2));
        Assert.assertEquals(5, sequence.getSize());
        Assert.assertTrue(sequence.remove(this.constant1));
        Assert.assertEquals(4, sequence.getSize());
        Assert.assertTrue(sequence.remove(this.variable2));
        Assert.assertEquals(3, sequence.getSize());
        Assert.assertFalse(sequence.remove(this.impulse3));
        Assert.assertEquals(3, sequence.getSize());
        Assert.assertTrue(sequence.remove(this.impulse2));
        Assert.assertEquals(2, sequence.getSize());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MANEUVERS_LIST_VALIDATION}
     * 
     * @testedMethod {@link ManeuversSequence#ManeuversSequence(double, double)}
     * @testedMethod {@link ManeuversSequence#applyTo(NumericalPropagator)}
     * 
     * @description tests the propagation using the sequence of maneuvers
     * 
     * @input some continuous and impulse maneuvers and two propagators
     * 
     * @output the propagation output
     * 
     * @testPassCriteria the two propagators (only one set up with a sequence of maneuvers)
     *                   returns the same spacecraft state once the propagation finished.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     */
    @Test
    public void testPropagateWithManeuversSequence() throws PatriusException {
        // The initial date:
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 14, 0.0),
            TimeScalesFactory.getUTC());
        // The orbit:
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new CircularOrbit(7100000, .0, .0, MathLib.toRadians(98), .0, .0, PositionAngle.MEAN,
            FramesFactory.getEME2000(), date0, mu);
        // Sets the integrator:
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        final MassProvider massModel = new SimpleMassModel(1000., "thruster");
        propagator1.setInitialState(new SpacecraftState(orbit, massModel));
        propagator1.setMassProviderEquation(massModel);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));

        // Propagator 1: sequence of maneuvers
        final ManeuversSequence sequence = new ManeuversSequence(0.0, 0.0);
        // add the maneuvers to the sequence (they do not overlap):
        sequence.add(this.constant1);
        sequence.add(this.constant2);
        sequence.add(this.variable1);
        sequence.add(this.variable2);
        sequence.add(this.impulse2);
        sequence.add(this.impulse3);
        sequence.add(this.impulse4);
        sequence.applyTo(propagator1);
        final SpacecraftState finalState1 = propagator1.propagate(date0.shiftedBy(1000000));
        // Propagator 2: maneuvers added to the propagator one by one:
        this.setUp();
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.setInitialState(new SpacecraftState(orbit, massModel));
        propagator2.setMassProviderEquation(massModel);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator2.addForceModel(this.constant1);
        propagator2.addForceModel(this.constant2);
        propagator2.addForceModel(this.variable1);
        propagator2.addForceModel(this.variable2);
        propagator2.addEventDetector(this.impulse2);
        propagator2.addEventDetector(this.impulse3);
        propagator2.addEventDetector(this.impulse4);
        final SpacecraftState finalState2 = propagator2.propagate(date0.shiftedBy(1000000));
        // Check the two propagations produced the same final spacecraft states:
        Assert.assertEquals(finalState1.getA(), finalState2.getA(), 0.0);
        Assert.assertEquals(finalState1.getE(), finalState2.getE(), 0.0);
        Assert.assertEquals(finalState1.getI(), finalState2.getI(), 0.0);
        Assert.assertEquals(finalState1.getLv(), finalState2.getLv(), 0.0);
        Assert.assertEquals(finalState1.getMass("thruster"), finalState2.getMass("thruster"), 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MANEUVERS_LIST_VALIDATION}
     * 
     * @testedMethod {@link ManeuversSequence#add(ContinuousThrustManeuver)}
     * 
     * @description tests the adding of maneuvers defined by generic events
     * 
     * @input some continuous maneuvers
     * 
     * @output no exception, maneuver properly added
     * 
     * @testPassCriteria no exception is thrown, the maneuver has been properly added
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testGenericEventsManeuver() throws PatriusException {
        // creates the sequence (with one constant and one variable maneuver)
        final ManeuversSequence sequence = new ManeuversSequence(10.0, 20.0);
        final EventDetector detector1 = new LocalTimeAngleDetector(1.);
        final EventDetector detector2 = new LocalTimeAngleDetector(2.);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("tank");
        builder.addProperty(new MassProperty(12.), "tank");
        final Assembly assembly = builder.returnAssembly();
        // Constant maneuver
        final ContinuousThrustManeuver maneuver1 =
            new ContinuousThrustManeuver(detector1, detector2, new PropulsiveProperty(300, 200),
                Vector3D.PLUS_I, new MassModel(assembly), new TankProperty(12), FramesFactory.getGCRF());
        Assert.assertTrue(sequence.add(maneuver1));

        // Variable maneuver
        final MassModel massModel = new MassModel(assembly);
        final IDependentVariable<SpacecraftState> thrust = null;
        final IDependentVectorVariable<SpacecraftState> direction = null;
        final IDependentVariable<SpacecraftState> isp = null;
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(detector1, detector2,
            new PropulsiveProperty(thrust, isp),
            direction, massModel, new TankProperty(massModel.getMass("tank")), FramesFactory.getGCRF());
        Assert.assertTrue(sequence.add(maneuver2));
    }

    /**
     * @throws PatriusException
     *         maneuver definition
     * @testType UT
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        // creates the sequence:
        final ManeuversSequence sequence = new ManeuversSequence(10.0, 20.0);
        this.constant1 = new ContinuousThrustManeuver(AbsoluteDate.J2000_EPOCH, 20., new PropulsiveProperty(20, 30),
            Vector3D.PLUS_I, null, new TankProperty(300));
        sequence.add(this.constant1);
        this.constant2 =
            new ContinuousThrustManeuver(AbsoluteDate.J2000_EPOCH.shiftedBy(600), 40., new PropulsiveProperty(
                20, 80),
                Vector3D.PLUS_I, null, new TankProperty(350));
        sequence.add(this.constant2);
        this.impulse1 = new ImpulseManeuver(new AltitudeDetector(150000, new OneAxisEllipsoid(
            Constants.CNES_STELA_AE,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getCIRF())), Vector3D.MINUS_I, 30, new MassModel(
            ForceModelsDataTest.getAssembly()), "Tank");

        sequence.add(this.impulse1);
        this.impulse2 = new ImpulseManeuver(new AltitudeDetector(120000, new OneAxisEllipsoid(
            Constants.CNES_STELA_AE,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getCIRF())), Vector3D.MINUS_I, 50, new MassModel(
            ForceModelsDataTest.getAssembly()), "Tank");
        sequence.add(this.impulse2);

        final Set<ContinuousThrustManeuver> continuousList = new HashSet<>();
        continuousList.add(this.constant1);
        continuousList.add(this.constant2);

        final Set<ImpulseManeuver> impulseList = new HashSet<>();
        impulseList.add(this.impulse1);
        impulseList.add(this.impulse2);

        Assert.assertEquals(10.0, sequence.getConstraintContinuous(), 0);
        Assert.assertEquals(20.0, sequence.getConstraintImpulsive(), 0);
        Assert.assertTrue(sequence.getContinueManeuversList().equals(continuousList));
        Assert.assertTrue(sequence.getImpulseManeuversList().equals(impulseList));
    }

    /**
     * Set up method before running the test.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        // sets the variable thrust:
        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1819709485971875799L;

            @Override
            public double value(final SpacecraftState x) {
                return 20.0;
            }
        };

        // sets the variable ISP:
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 6767959928000643244L;

            @Override
            public double value(final SpacecraftState x) {
                return 275.0;
            }
        };

        // sets the variable direction:
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6236212452905529222L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_K;
            }
        };

        // sets the maneuvers with constant thrust:
        final AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 15, 30.0),
            TimeScalesFactory.getUTC());

        final MassProvider model = new SimpleMassModel(5000., "thruster");
        final TankProperty tankProperty = new TankProperty(5000.);
        tankProperty.setPartName("thruster");

        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, model,
                tankProperty);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(105), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                model,
                tankProperty);
        final AbsoluteDate date2 = date1.shiftedBy(260);
        // sets the maneuvers with variable thrust:
        this.variable1 = new ContinuousThrustManeuver(date2, 500, new PropulsiveProperty(thrust, isp),
            direction, model, tankProperty);
        this.variable2 = new ContinuousThrustManeuver(date2.shiftedBy(600), 500, new PropulsiveProperty(thrust, isp),
            direction, model, tankProperty);
        // sets the impulse maneuvers:
        this.impulse1 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(109)), Vector3D.PLUS_K, 40, model, "thruster");
        this.impulse2 =
            new ImpulseManeuver(new DateDetector(date2.shiftedBy(1500)), Vector3D.PLUS_K, 80, model, "thruster");
        this.impulse3 =
            new ImpulseManeuver(new DateDetector(date2.shiftedBy(1515)), Vector3D.PLUS_K, 70, model, "thruster");
        this.impulse4 =
            new ImpulseManeuver(new NodeDetector(FramesFactory.getEME2000(), 2, 600, 1e-6), Vector3D.PLUS_K, 40,
                model, "thruster");
    }
}
