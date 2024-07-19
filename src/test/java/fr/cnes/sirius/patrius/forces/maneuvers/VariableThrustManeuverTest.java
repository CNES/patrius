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
 * VERSION:4.11:DM:DM-3242:22/05/2023:[PATRIUS] Possibilite de definir les parametres circulaires adaptes pour des orbites hyperboliques
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:19/09/2013:New ForceModel interface
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::FA:93:01/04/2014:changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:293:01/10/2014:Allowed users to define a maneuver by a direction in any frame
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:388:19/02/2015:Restored deprecated constructor + raised exception if SpacecraftFrame as input.
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:465:16/06/2015:Added analytical computation of partial derivatives
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:453:13/11/2015:Handling propagation starting during a maneuver
 * VERSION::FA:487:06/11/2015:Start/Stop maneuver correction
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.VariableISP;
import fr.cnes.sirius.patrius.forces.VariablePressureThrust;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.events.NthOccurrenceDetector;
import fr.cnes.sirius.patrius.propagation.events.NullMassDetector;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Tests for the ContinuousThrustManeuverTest class.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: VariableThrustManeuverTest.java 18392 2017-11-17 13:14:35Z bignon $
 * 
 * @since 1.2
 * 
 */
public class VariableThrustManeuverTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle variable thrust maneuver validation
         * 
         * @featureDescription test the variable thrust maneuver
         * 
         * @coveredRequirements DV-PROPU_20, DV-PROPU_30, DV-PROPU_70
         */
        VARIABLE_THRUST_MANEUVER_VALIDATION
    }

    /** a */
    private static double a = 1.01;
    /** b */
    private static double b = 0.889;
    /** p0 */
    private static double p0 = 20;
    /** dp */
    private static double dp = 1. / 60.;
    /** isp0 */
    private static double isp0 = 200;
    /** disp */
    private static double disp = 10. / 60.;

    /** The initial orbit. */
    private Orbit orbit;

    /** The initial mass. */
    private double mass;

    /** The initial state. */
    private SpacecraftState initialState;

    /** The fire state. */
    private SpacecraftState fireState;

    /** The attitude law provider. */
    private AttitudeProvider law;

    /** The thrust. */
    private IDependentVariable<SpacecraftState> thrust;

    /** The thrust direction. */
    private IDependentVectorVariable<SpacecraftState> direction;

    /** The isp. */
    private IDependentVariable<SpacecraftState> isp;

    /** The maneuver. */
    private ContinuousThrustManeuver maneuver;

    /** The second maneuver. */
    private ContinuousThrustManeuver maneuver2;

    /** The starting date of the maneuver. */
    private AbsoluteDate fireDate;
    private MassProvider model1, model2;
    private TankProperty tankA, tankB;

    private double mu;

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#getParametersNames()}
     * @testedMethod {@link ContinuousThrustManeuver#getParameter(String)}
     * @testedMethod {@link ContinuousThrustManeuver#setParameter()}
     * 
     * @description Test for the parameters
     * 
     * @input a parameter
     * 
     * @output its value
     * 
     * @testPassCriteria the parameter value is as expected exactly (0 ulp difference)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamList() {

        double k = 5;
        Assert.assertEquals(0, this.maneuver.getParameters().size());
        final ArrayList<Parameter> paramList = this.maneuver.getParameters();
        for (int i = 0; i < paramList.size(); i++) {
            paramList.get(i).setValue(k);
            Assert.assertTrue(Precision.equals(k, paramList.get(i).getValue(), 0));
            k++;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double,
     *               IDependentVariable<SpacecraftState>, IDependentVariable<SpacecraftState>,
     *               IDependentVectorVariable<SpacecraftState>, Frame, MassProvider, String)}
     * 
     * @description Test the negative duration
     * 
     * @input a variable thrust maneuver with negative duration
     * 
     * @output the g functions sign at different dates
     * 
     * @testPassCriteria the sign of the g function is the expected one
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testNegativeDuration() throws PatriusException {

        // Test the negative duration on the second constructor :
        this.maneuver = new ContinuousThrustManeuver(this.fireDate, -10, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, this.model1, new TankProperty(this.model1.getMass("thruster")),
            FramesFactory.getGCRF());
        EventDetector[] switches = this.maneuver.getEventsDetectors();

        Orbit o1 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-11.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-9.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);

        // Test the negative duration on the constructor with LOFType :
        this.maneuver = new ContinuousThrustManeuver(this.fireDate, -10, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, this.model1, new TankProperty(this.model1.getMass("thruster")), LOFType.TNW);
        switches = this.maneuver.getEventsDetectors();

        o1 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-11.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        o2 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-9.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        o3 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        o4 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);

        // Test the negative duration on the constructor with the properties
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(this.fireDate, -10,
            new PropulsiveProperty(this.thrust, this.isp), this.direction, this.model1, new TankProperty(this.mass));
        switches = maneuver.getEventsDetectors();

        o1 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-11.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        o2 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-9.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        o3 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(-1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        o4 = VariableThrustManeuverTest.dummyOrbit(this.fireDate.shiftedBy(1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    /**
     * @testType UT
     * 
     * @description Test when mass is null
     * 
     * @referenceVersion 3.1
     */
    @Test
    public void testNullMassFiring() throws PatriusException {

        // Test when firing and mass is null :
        final SimpleMassModel modeNullMass = new SimpleMassModel(0., "null mass");
        final TankProperty tank = new TankProperty(0.);
        tank.setPartName("null mass");
        this.maneuver = new ContinuousThrustManeuver(this.fireDate, 10, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, modeNullMass, tank, FramesFactory.getGCRF());

        // Initialization
        final AbsoluteDate initialDate = this.fireDate;
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0.,
            0., 1., PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate,
            Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // Propagation
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, modeNullMass);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(this.maneuver);
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(modeNullMass);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        Assert.assertEquals(
            propagator1.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()))
                .getMass("null mass"), 0., 0.);
    }

    /**
     * @return a dummy orbit for tests
     */
    private static CircularOrbit dummyOrbit(final AbsoluteDate date) throws IllegalArgumentException,
        PatriusException {
    	final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        return new CircularOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date,
        		CelestialBodyFactory.getEarth().getGM());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable)}
     * @testedMethod {@link ContinuousThrustManeuver#getThrust(SpacecraftState)}
     * @testedMethod {@link ContinuousThrustManeuver#getDirection(SpacecraftState)}
     * @testedMethod {@link ContinuousThrustManeuver#getISP(SpacecraftState)}
     * @testedMethod {@link ContinuousThrustManeuver#getFlowRate(SpacecraftState)}
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * @testedMethod {@link ContinuousThrustManeuver#getStartDate()}
     * @testedMethod {@link ContinuousThrustManeuver#getEndDate()}
     * 
     * @description tests the methods of the class without performing a propagation.
     * 
     * @input a variable thrust maneuver
     * 
     * @output the parameters of the maneuvers at different dates
     * 
     * @testPassCriteria the ISP, the thrust (module and direction), the acceleration should be the
     *                   expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testNoPropagation() throws PatriusException {

        double currentThrust = this.maneuver.getThrust(this.fireState);
        double expectedThrust = a * (MathLib.pow(p0, b));
        Vector3D currentDirection = this.maneuver.getDirection(this.fireState);
        Vector3D expectedDirection = Vector3D.PLUS_I;
        double currentIsp = this.maneuver.getISP(this.fireState);
        double expectedIsp = isp0;
        this.maneuver.setFiring(true);
        Vector3D currentAcceleration = this.maneuver.computeAcceleration(this.fireState);
        Vector3D expectedAcceleration = new Vector3D(expectedThrust / this.mass, expectedDirection);
        Assert.assertEquals(expectedThrust, currentThrust, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedDirection, currentDirection);
        Assert.assertEquals(expectedIsp, currentIsp, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedAcceleration, currentAcceleration);

        Assert.assertEquals(this.fireDate, this.maneuver.getStartDate());
        Assert.assertEquals(this.fireDate.shiftedBy(600.), this.maneuver.getEndDate());

        final double duration = 600;
        final SpacecraftState finalState = this.fireState.shiftedBy(duration);
        currentThrust = this.maneuver.getThrust(finalState);
        expectedThrust = a * (MathLib.pow(p0 + 10, b));
        currentDirection = this.maneuver.getDirection(finalState);
        expectedDirection = Vector3D.PLUS_I;
        currentIsp = this.maneuver.getISP(finalState);
        expectedIsp = isp0 + disp * duration;
        currentAcceleration = this.maneuver.computeAcceleration(finalState);
        // no propagation: the final mass is the same!
        expectedAcceleration = new Vector3D(expectedThrust / this.mass, expectedDirection);
        Assert.assertEquals(expectedThrust, currentThrust, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedDirection, currentDirection);
        Assert.assertEquals(expectedIsp, currentIsp, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedAcceleration, currentAcceleration);

        // check maneuver1 and maneuver2 are the same:
        Assert.assertEquals(this.maneuver.getThrust(finalState), this.maneuver2.getThrust(finalState),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(this.maneuver.getDirection(finalState), this.maneuver2.getDirection(finalState));
        Assert.assertEquals(this.maneuver.getISP(finalState), this.maneuver2.getISP(finalState),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable)}
     * @testedMethod {@link ContinuousThrustManeuver#addContribution(SpacecraftState, TimeDerivativesEquations)}
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * @testedMethod {@link ContinuousThrustManeuver#getEventsDetectors()}
     * 
     * @description tests the methods of the class performing a propagation.
     * 
     * @input a variable thrust maneuver, a propagator
     * 
     * @output the parameters of the maneuvers during the propagation and the mass
     * 
     * @testPassCriteria the ISP, the thrust (module and direction), the acceleration and the final
     *                   mass should be the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testPropagation() throws PatriusException {

        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(this.fireDate, 600,
            new PropulsiveProperty(this.thrust, this.isp), this.direction, this.model1, this.tankA);

        final double propagationT = 650;
        // sets the integrator:
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000,
            absTolerance, relTolerance);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(this.fireState);
        propagator.setAttitudeProvider(this.law);
        propagator.addForceModel(maneuver);
        propagator.setMassProviderEquation(this.model1);

        final Set<SpacecraftState> states = new HashSet<>();
        final double[] loggedISP = new double[(int) (propagationT + 2)];
        final double[] loggedThrust = new double[(int) (propagationT + 2)];
        final double[] loggedFlowRate = new double[(int) (propagationT + 2)];
        final Vector3D[] loggedDirection = new Vector3D[(int) (propagationT + 2)];
        propagator.setMasterMode(1, new PatriusFixedStepHandler(){

            /** Serializable UID. */
            private static final long serialVersionUID = -8335479220930638422L;

            int i = 0;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                // record the parameters at every time step:
                states.add(currentState);
                loggedISP[this.i] = maneuver.getISP(currentState);
                loggedThrust[this.i] = maneuver.getThrust(currentState);
                loggedFlowRate[this.i] = maneuver.getFlowRate(currentState);
                loggedDirection[this.i] = maneuver.getDirection(currentState);
                this.i++;
            }
        });

        // A logger is created to log detected events:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector((maneuver.getEventsDetectors()[0])));
        propagator.addEventDetector(logger.monitorDetector((maneuver.getEventsDetectors()[1])));

        // Propagation:
        final SpacecraftState finalorb = propagator.propagate(this.fireDate.shiftedBy(-1),
            this.fireDate.shiftedBy(propagationT));

        double dMass = 0;
        for (int j = 0; j < (propagationT); j++) {
            // for every time step, check the thrust, the ISP and the flow rate:
            final double expectedISP = isp0 + disp * j;
            Assert.assertEquals(expectedISP, loggedISP[j + 1], 1E-11);
            final double expectedThrust = a * MathLib.pow((p0 + dp * j), b);
            Assert.assertEquals(expectedThrust, loggedThrust[j + 1], 1E-11);
            final Vector3D expectedDirection = Vector3D.PLUS_I;
            Assert.assertEquals(expectedDirection, loggedDirection[j + 1]);
            final double expectedFlowRate = -expectedThrust
                    / (Constants.G0_STANDARD_GRAVITY * expectedISP);
            Assert.assertEquals(expectedFlowRate, loggedFlowRate[j + 1], 1E-11);
            // compute the mass variation:
            if (j < 600) {
                // after 600 s the engine is off:
                dMass += expectedFlowRate;
            }
        }

        // check the spacecraft global mass variation:
        Assert.assertEquals(4000. + dMass, finalorb.getMass("thruster"), 1E-3);
        // Two events should have been logged: the start and the end of the maneuver:
        Assert.assertEquals(2, logger.getLoggedEvents().size());
        // Beginning of the variable thrust maneuver:
        Assert.assertEquals(this.fireDate, logger.getLoggedEvents().get(0).getState().getDate());
        // Ending of the variable thrust maneuver:
        Assert.assertEquals(this.fireDate.shiftedBy(600), logger.getLoggedEvents().get(1).getState()
            .getDate());
    }

    /**
     * 
     * FA 293 : Allowed users to define a maneuver by a direction in any frame.
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable, fr.cnes.sirius.patrius.propagation.MassProvider, String, fr.cnes.sirius.patrius.frames.Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * 
     * @description test the acceleration computation using a direction in a frame defined by user.
     * 
     * @input a variable thrust maneuver with a direction defined in a frame defined by user.
     * 
     * @output the acceleration
     * 
     * @testPassCriteria the acceleration should be the expected one
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testAnyDirectionFrame() throws PatriusException {
        final Frame eme2000 = FramesFactory.getEME2000();
        final double duration = 600;
        final SpacecraftState finalState = this.fireState.shiftedBy(duration);

        // sets the maneuver:
        final TankProperty tank = new TankProperty(this.model1.getMass("thruster"));
        tank.setPartName("thruster");
        this.maneuver = new ContinuousThrustManeuver(this.fireDate, 600, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, this.model1, tank, eme2000);
        this.maneuver.setFiring(true);
        final Vector3D acc = this.maneuver.computeAcceleration(finalState);

        final Vector3D expectedAcc = new Vector3D(this.thrust.value(finalState)
                / this.initialState.getMass("thruster"), this.direction.value(finalState));
        Assert.assertNotNull(acc);
        Assert.assertEquals(acc, expectedAcc);
        Assert.assertEquals(eme2000, this.maneuver.getFrame());
    }

    /**
     * 
     * FA 373 : (left from DM 200): handling of negative or null mass.
     * 
     * @testType UT
     * @throws PatriusException if propagation failed
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * 
     * @description tests proper handling of negative or null mass. Two maneuvers are added in
     *              opposite direction since only one leads to hyperbolic orbit (acceleration when
     *              total mass becomes 0, becomes infinite). In real case this will not happen since
     *              final mass will not be null in such cases.
     * 
     * @testPassCriteria propagation terminates without error
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testNullMassPartDectector() throws IllegalArgumentException, PatriusException {

        // mass model
        final double mass = 1000;
        final String partName = "tank";
        final MassProvider massModel = new SimpleMassModel(mass, partName);
        /*
         * Spacecraft state
         */
        // frame and date
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate d0 = new AbsoluteDate();
        final double shift1 = 100.;
        final AbsoluteDate d1 = d0.shiftedBy(shift1);

        // orbit
        final double muValue = Constants.GRIM5C1_EARTH_MU;
        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 400e3;
        final double e = .0001;
        final double i = 60 * 3.14 / 180;
        final double raan = 0;
        final double pa = 270 * 3.14 / 180;
        final double w = 0;

        // state
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE,
            gcrf, d0, muValue);
        final SpacecraftState spc = new SpacecraftState(orbit, massModel);

        final TankProperty tank = new TankProperty(massModel.getMass(partName));
        tank.setPartName(partName);

        // 2 maneuvers opposite thrust
        final double duration = 30000.0;
        final double dv = 100;
        final double isp = 300;
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(d1, duration,
            new PropulsiveProperty(new IDependentVariable<SpacecraftState>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -3535336147440700774L;

                @Override
                public double value(final SpacecraftState x) {
                    return dv;
                }
            }, new IDependentVariable<SpacecraftState>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -4264967763965369018L;

                @Override
                public double value(final SpacecraftState x) {
                    return isp;
                }
            }), new IDependentVectorVariable<SpacecraftState>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -2730540368558146621L;

                @Override
                public Vector3D value(final SpacecraftState x) {
                    return Vector3D.MINUS_I;
                }
            }, massModel, tank, FramesFactory.getGCRF());
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(d1, duration,
            new PropulsiveProperty(new IDependentVariable<SpacecraftState>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -3329319365445209910L;

                @Override
                public double value(final SpacecraftState x) {
                    return dv;
                }
            }, new IDependentVariable<SpacecraftState>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -8518616122092769464L;

                @Override
                public double value(final SpacecraftState x) {
                    return isp;
                }
            }), new IDependentVectorVariable<SpacecraftState>(){
                /** Serializable UID. */
                private static final long serialVersionUID = 1647593693929197844L;

                @Override
                public Vector3D value(final SpacecraftState x) {
                    return Vector3D.PLUS_I;
                }
            }, massModel, tank, FramesFactory.getGCRF());

        // numerical propagator
        final double[][] tol = NumericalPropagator.tolerances(1., orbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(.001, 60, tol[0], tol[1]);
        // final FirstOrderIntegrator dop = new ClassicalRungeKuttaIntegrator(30.);

        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(spc);
        propagator.setAttitudeProvider(new BodyCenterPointing(FramesFactory.getEME2000()));
        propagator.setMassProviderEquation(massModel);
        propagator.addForceModel(maneuver1);
        propagator.addForceModel(maneuver2);

        // Propagation
        propagator.propagate(d0.shiftedBy(30000));
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String, LOFType)}
     * @description Test propagation with impulse maneuver in TNW frame following velocity in
     *              spacecraft frame is equal to propagation with impulse maneuver in TNW.
     * @referenceVersion 2.3.1
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testManeuverWithLOF() throws PatriusException, IOException, ParseException {
        // mass model
        final String thruster = "thruster";
        MassProvider massModel = new SimpleMassModel(1000., thruster);
        // initial orbit
        final double a = 6900e3;
        final double e = .001;
        final double i = 51.4 * FastMath.PI / 180;
        final double pa = 270 * FastMath.PI / 180;
        final double raan = 170 * FastMath.PI / 180;
        final double w = 30 * FastMath.PI / 180;

        // Echelle de temps TAI
        final TimeScale tai = TimeScalesFactory.getTAI();

        // Start date
        final AbsoluteDate date0 = new AbsoluteDate(2005, 1, 1, 6, 0, 0, tai);
        final AbsoluteDate fire = date0.shiftedBy(100.);
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // inertial frame
        final Frame gcrf = FramesFactory.getGCRF();
        // initial orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE,
            gcrf, date0, mu);

        // keplerian period
        final double T = orbit.getKeplerianPeriod();
        // Final date
        final AbsoluteDate finalDate = date0.shiftedBy(T * 20);

        // attitude provider
        final AttitudeProvider attProv = new LofOffset(gcrf, LOFType.TNW);

        // attitude initiale
        final Attitude initialAttitude = attProv.getAttitude(orbit, date0, orbit.getFrame());

        // tol
        final double[][] tol = NumericalPropagator.tolerances(1, orbit, OrbitType.CARTESIAN);

        // integrateur
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1, 7200, tol[0], tol[1]);

        // bulletin initial
        final SpacecraftState etat = new SpacecraftState(orbit, initialAttitude, massModel);

        // propagateur
        final NumericalPropagator prop1 = new NumericalPropagator(dop853, etat.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        // initialisation du propagateur
        prop1.setInitialState(etat);
        prop1.clearEventsDetectors();
        prop1.setAttitudeProvider(attProv);
        prop1.setMassProviderEquation(massModel);

        // sets the maneuver:
        // sets the thrust:
        final double[] coeffs = new double[2];
        coeffs[0] = 1.01;
        coeffs[1] = 0.889;
        this.thrust = new VariablePressureThrust(fire, 20, 1. / 60., coeffs);
        this.direction = new ConstantDirection(Vector3D.PLUS_I);
        // sets the ISP:
        this.isp = new VariableISP(fire, 200, 10. / 60.);

        final TankProperty tank1 = new TankProperty(massModel.getMass("thruster"));
        tank1.setPartName("thruster");
        this.maneuver = new ContinuousThrustManeuver(fire, 1, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, massModel, tank1);

        // Ajout manoeuvre impulsionnelle
        prop1.addForceModel(this.maneuver);

        // propagation
        final SpacecraftState endStateNullFrame = prop1.propagate(finalDate);
        Assert.assertTrue((endStateNullFrame.getMass(thruster)) < 1000.);
        // Impulse Maneuver with LOF Type
        // ----------------------------------
        massModel = new SimpleMassModel(1000., thruster);

        // bulletin initial
        final SpacecraftState etat2 = new SpacecraftState(orbit, initialAttitude, massModel);

        // propagateur
        final NumericalPropagator prop2 = new NumericalPropagator(dop853, etat2.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        // initialisation du propagateur
        prop2.setInitialState(etat2);
        prop2.clearEventsDetectors();
        prop2.setAttitudeProvider(attProv);
        prop2.setMassProviderEquation(massModel);

        // impulsion
        final TankProperty tank2 = new TankProperty(massModel.getMass("thruster"));
        tank2.setPartName("thruster");
        this.maneuver = new ContinuousThrustManeuver(fire, 1, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, massModel, tank2, LOFType.TNW);

        // Ajout manoeuvre impulsionnelle
        prop2.addForceModel(this.maneuver);

        // propagation
        final SpacecraftState endStateLOF = prop2.propagate(finalDate);
        Assert.assertTrue((endStateLOF.getMass(thruster)) < 1000.);

        // Comparison
        // --------------------------------------------
        Assert.assertEquals(endStateNullFrame.getA(), endStateLOF.getA(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getEquinoctialEx(), endStateLOF.getEquinoctialEx(),
            1.0e-14);
        Assert.assertEquals(endStateNullFrame.getEquinoctialEy(), endStateLOF.getEquinoctialEy(),
            1.0e-14);
        Assert.assertEquals(endStateNullFrame.getHx(), endStateLOF.getHx(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getHy(), endStateLOF.getHy(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getLM(), endStateLOF.getLM(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getMass(thruster), endStateLOF.getMass(thruster),
            1.0e-14);
    }

    /**
     * @throws PatriusException
     * @testType UT for constructors coverage and validation
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(EventDetector, EventDetector, PropulsiveProperty, IDependentVectorVariable, MassProvider, TankProperty)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, PropulsiveProperty, IDependentVectorVariable, MassProvider, TankProperty, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, PropulsiveProperty, IDependentVectorVariable, MassProvider, TankProperty, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(EventDetector, EventDetector, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(EventDetector, EventDetector, PropulsiveProperty, IDependentVectorVariable, MassProvider, TankProperty, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(EventDetector, EventDetector, PropulsiveProperty, IDependentVectorVariable, MassProvider, TankProperty, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(AbsoluteDate, double, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(EventDetector, EventDetector, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ContinuousThrustManeuver(EventDetector, EventDetector, IDependentVariable, IDependentVariable, IDependentVectorVariable, MassProvider, String, LOFType)}
     * 
     * @description This test aims at verifying that the new {@link ContinuousThrustManeuver} constructors providing
     *              {@link TankProperty} and {@link PropulsiveProperty} have
     *              the same behavior than the former symmetric constructors (i.e providing the same
     *              information in order to build the properties).
     * 
     * @input instances of {@link ContinuousThrustManeuver}
     * 
     * @output instances attributes
     * 
     * @testPassCriteria Instances attributes must provide the same results (isp, thrust, flow
     *                   rate).
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testConstructorsProperties() throws PatriusException {

        // Dummy spacecraft
        final AbsoluteDate date = new AbsoluteDate();
        final Orbit testOrbit = new KeplerianOrbit(10000000., 0.93, MathLib.toRadians(75), 0, 0,
            0, PositionAngle.MEAN, FramesFactory.getGCRF(), date.shiftedBy(100.),
            Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(testOrbit);

        // Input for maneuver constructors
        final String MAIN = "main";
        final double duration = 1.;
        final double mass = 1000.;
        final MassProvider massProv = new SimpleMassModel(mass, MAIN);
        final Frame gcrf = FramesFactory.getGCRF();
        final LOFType lof = LOFType.TNW;
        final double[] coeffs = new double[2];
        coeffs[0] = 1.01;
        coeffs[1] = 0.889;
        // Thrust function
        this.thrust = new VariablePressureThrust(date.shiftedBy(100.), 20, 1. / 60., coeffs);
        this.direction = new ConstantDirection(Vector3D.PLUS_I);
        // ISP function
        this.isp = new VariableISP(date.shiftedBy(100.), 200, 10. / 60.);

        // Event detectors
        final NodeDetector ascendingDetector = new NodeDetector(FramesFactory.getEME2000(),
            NodeDetector.ASCENDING, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, true);
        final NodeDetector descendingDetector = new NodeDetector(FramesFactory.getEME2000(),
            NodeDetector.DESCENDING, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, true);

        // Tank and engine properties
        final TankProperty tank = new TankProperty(mass);
        final PropulsiveProperty engine = new PropulsiveProperty(this.thrust, this.isp);

        // Create all maneuvers : former constructor/new constructor with properties provided
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(date, duration,
            engine, this.direction, massProv, new TankProperty(massProv.getMass(MAIN)));
        final ContinuousThrustManeuver maneuver1Prop = new ContinuousThrustManeuver(date, duration,
            engine, this.direction, massProv, tank);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(date, duration,
            engine, this.direction, massProv, new TankProperty(massProv.getMass(MAIN)), gcrf);
        final ContinuousThrustManeuver maneuver2Prop = new ContinuousThrustManeuver(date, duration,
            engine, this.direction, massProv, tank, gcrf);
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(date, duration,
            engine, this.direction, massProv, new TankProperty(massProv.getMass(MAIN)), lof);
        final ContinuousThrustManeuver maneuver3Prop = new ContinuousThrustManeuver(date, duration,
            engine, this.direction, massProv, tank, lof);
        final ContinuousThrustManeuver maneuver4 = new ContinuousThrustManeuver(ascendingDetector,
            descendingDetector, engine, this.direction, massProv, new TankProperty(
                massProv.getMass(MAIN)));
        final ContinuousThrustManeuver maneuver4Prop = new ContinuousThrustManeuver(
            ascendingDetector, descendingDetector, engine, this.direction, massProv, tank);
        final ContinuousThrustManeuver maneuver5 = new ContinuousThrustManeuver(ascendingDetector,
            descendingDetector, engine, this.direction, massProv, new TankProperty(
                massProv.getMass(MAIN)), gcrf);
        final ContinuousThrustManeuver maneuver5Prop = new ContinuousThrustManeuver(
            ascendingDetector, descendingDetector, engine, this.direction, massProv, tank, gcrf);
        final ContinuousThrustManeuver maneuver6 = new ContinuousThrustManeuver(ascendingDetector,
            descendingDetector, engine, this.direction, massProv, new TankProperty(
                massProv.getMass(MAIN)), lof);
        final ContinuousThrustManeuver maneuver6Prop = new ContinuousThrustManeuver(
            ascendingDetector, descendingDetector, engine, this.direction, massProv, tank, lof);

        // Build a maneuvers list
        final ContinuousThrustManeuver[] maneuvers = new ContinuousThrustManeuver[] { maneuver1,
            maneuver2, maneuver3, maneuver4, maneuver5, maneuver6 };

        final ContinuousThrustManeuver[] maneuversProp = new ContinuousThrustManeuver[] {
            maneuver1Prop, maneuver2Prop, maneuver3Prop, maneuver4Prop, maneuver5Prop,
            maneuver6Prop };

        // Loop on maneuvers list
        double ispVal1;
        double ispVal2;
        double thrustVal1;
        double thrustVal2;
        double flowRate1;
        double flowRate2;

        for (int i = 0; i < maneuvers.length - 1; i++) {

            // Values for usual maneuver
            ispVal1 = maneuvers[i].getISP(state);
            thrustVal1 = maneuvers[i].getThrust(state);
            flowRate1 = maneuvers[i].getFlowRate(state);

            // Properties for maneuver defined directly with TankProperty and PropulsiveProperty
            ispVal2 = maneuversProp[i].getISP(state);
            thrustVal2 = maneuversProp[i].getThrust(state);
            flowRate2 = maneuversProp[i].getFlowRate(state);

            // Perform comparisons
            Assert.assertEquals(ispVal1, ispVal2, 0.);
            Assert.assertEquals(thrustVal1, thrustVal2, 0.);
            Assert.assertEquals(flowRate1, flowRate2, 0.);
            Assert.assertEquals(maneuvers[i].getParameters().size(), maneuversProp[i]
                .getParameters().size(), 0);
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * 
     * @description Test mainly for coverage purpose. Test lines added by FA414. See
     *              MassDetectorsTest for functionality test
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testCoverage() throws PatriusException {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate date0 = new AbsoluteDate(2005, 1, 1, 6, 0, 0, tai);
        final Frame gcrf = FramesFactory.getGCRF();
        final KeplerianOrbit orbit = new KeplerianOrbit(0, 0, 0, 0, 0, 0, PositionAngle.TRUE, gcrf,
            date0, this.mu);

        final String DEFAULT = "default";
        class ZeroMassModel implements MassProvider {
            /** Serializable UID. */
            private static final long serialVersionUID = 5685589406987077419L;

            @Override
            public double getTotalMass() {
                return 0;
            }

            @Override
            public double getTotalMass(final SpacecraftState state) {
                return 0;
            }

            @Override
            public double getMass(final String partName) {
                return 0;
            }

            @Override
            public void updateMass(final String partName, final double mass)
                throws PatriusException {
                // nothing to do
            }

            @Override
            public void setMassDerivativeZero(final String partName) {
                // nothing to do
            }

            @Override
            public void addMassDerivative(final String partName, final double flowRate) {
                // nothing to do
            }

            @Override
            public AdditionalEquations getAdditionalEquation(final String name) {
                return null;
            }

            @Override
            public List<String> getAllPartsNames() {
                final List<String> list = new ArrayList<>();
                list.add(DEFAULT);
                return list;
            }
        }

        final ZeroMassModel massModel = new ZeroMassModel();
        final SpacecraftState s = new SpacecraftState(orbit, massModel);
        final TankProperty tank = new TankProperty(massModel.getMass(DEFAULT));
        tank.setPartName(DEFAULT);
        this.maneuver = new ContinuousThrustManeuver(this.fireDate, 600, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, massModel, tank, gcrf);

        // Cover "else" of "if (s.getMass(partName) != 0) {" from computeAcceleration method
        // The acceleration must be null !
        final Vector3D resNull = this.maneuver.computeAcceleration(s);
        Assert.assertEquals(Vector3D.ZERO, resNull);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * 
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDParam(SpacecraftState, Parameter, double[])}
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDState(SpacecraftState, double[][], double[][])}
     * 
     * @description Test output of partial derivatives computation methods 1/ wrt state 2/ wrt
     *              parameter
     * 
     * @input maneuver
     * 
     * @output 1/ partial derivatives with respect to state 2/ an exception
     * 
     * @testPassCriteria 1/ The returned derivatives vectors should be the same as the input one. 2/
     *                   An exception should be thrown while computing partial derivatives wrt state
     * 
     * @referenceVersion 3.0.1
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testPD() throws PatriusException {
        // Check the returned PD are not modified by calling addDAccDState
        final double[][] dAccdPos = new double[1][1];
        dAccdPos[0][0] = 1.0;
        final double[][] dAccdVel = new double[1][1];
        dAccdVel[0][0] = 2.0;
        this.maneuver.addDAccDState(this.initialState, dAccdPos, dAccdVel);
        Assert.assertEquals(1.0, dAccdPos[0][0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(2.0, dAccdVel[0][0], Precision.DOUBLE_COMPARISON_EPSILON);

        // Check an exception is thrown when calling addDAccDParam
        boolean testOk = false;
        try {
            final double[] dAccdParam = new double[3];
            final boolean firing = this.maneuver.isFiring();
            this.maneuver.setFiring(true);
            this.maneuver.addDAccDParam(this.initialState, new Parameter("default", 0.), dAccdParam);
            this.maneuver.setFiring(firing);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
        }
        Assert.assertTrue(testOk);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @description During a propagation, the partial derivatives are computed only when "firing".
     * 
     * @input a propagator, an orbit and a ContinuousThrustManeuver.
     * 
     * @output The partial derivatives
     * 
     * @testPassCriteria the derivatives are computed only during the firing
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testStepHandler() throws PatriusException {

        // FA-487

        // Initialization

        // Sets the variable thrust
        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7138001317043679640L;

            @Override
            public double value(final SpacecraftState x) {
                return 20.0;
            }
        };
        // Sets the variable ISP
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7938496360093995323L;

            @Override
            public double value(final SpacecraftState x) {
                return 275.0;
            }
        };
        // Sets the variable direction
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2704409781229217175L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_I;
            }
        };

        final AbsoluteDate t0 = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate date = t0.shiftedBy(1800);
        final double duration = 360;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main3");
        builder.addProperty(new MassProperty(0.), "Main3");
        final TankProperty tank = new TankProperty(1000.);
        builder.addPart("Satellite3", "Main3", Transform.IDENTITY);
        builder.addProperty(tank, "Satellite3");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(date, duration,
            new PropulsiveProperty(thrust, isp), direction, massModel, tank,
            FramesFactory.getGCRF());
        final Orbit orbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), t0, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState s = new SpacecraftState(orbit, massModel);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(1);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        final MySH handler = new MySH(maneuver);

        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(s.getMu())));
        propagator.setInitialState(s);
        propagator.setMassProviderEquation(massModel);
        propagator.setMasterMode(10, handler);

        // Propagation
        propagator.propagate(t0.shiftedBy(3600));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @description Test propagation with variable thrust maneuver and 2 date event detectors should
     *              return the same result when compared to constant thrust maneuver with a date and
     *              a duration
     * 
     * @input a propagator, an orbit, 2 event detectors and a ContinuousThrustManeuver
     * 
     * @output final states
     * 
     * @testPassCriteria final states are the same
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEventDetectorsDate() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = this.fireDate;
        final AbsoluteDate maneuverDate = initialDate.shiftedBy(1800);
        final double maneuverDuration = 360;
        final AbsoluteDate endManeuverDate = maneuverDate.shiftedBy(maneuverDuration);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0.,
            0., 0., PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate,
            Constants.EIGEN5C_EARTH_MU);
        final double propagationDuration = 3600;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // final IDependentVariable<SpacecraftState> thrust = new
        // IDependentVariable<SpacecraftState>() {
        // @Override
        // public double value(final SpacecraftState x) { return 20.0; }
        // };
        // final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>()
        // {
        // @Override
        // public double value(final SpacecraftState x) { return 275.0; }
        // };
        // final IDependentVectorVariable<SpacecraftState> direction = new
        // IDependentVectorVariable<SpacecraftState>() {
        // @Override
        // public Vector3D value(final SpacecraftState x) { return Vector3D.PLUS_I; }
        // };

        // First propagation
        final MassProvider massModel1 = new SimpleMassModel(1000., "Satellite");
        final TankProperty tank1 = new TankProperty(massModel1.getMass("Satellite"));
        tank1.setPartName("Satellite");
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(maneuverDate,
            maneuverDuration, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel1, tank1);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate
            .shiftedBy(propagationDuration));

        // Second propagation
        final MassProvider massModel2 = new SimpleMassModel(1000., "Satellite");
        final TankProperty tank2 = new TankProperty(massModel2.getMass("Satellite"));
        tank2.setPartName("Satellite");
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final DateDetector detector1 = new DateDetector(maneuverDate);
        final DateDetector detector2 = new DateDetector(endManeuverDate);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(detector1,
            detector2, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel2, tank2);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2.propagate(initialDate
            .shiftedBy(propagationDuration));

        // Check final states are equals
        Assert.assertEquals(state1.getDate().durationFrom(state2.getDate()), 0., 0.);
        Assert.assertEquals(state1.getA(), state2.getA(), 0.);
        Assert.assertEquals(state1.getEquinoctialEx(), state2.getEquinoctialEx(), 0.);
        Assert.assertEquals(state1.getEquinoctialEy(), state2.getEquinoctialEy(), 0.);
        Assert.assertEquals(state1.getHx(), state2.getHx(), 0.);
        Assert.assertEquals(state1.getHy(), state2.getHy(), 0.);
        Assert.assertEquals(state1.getLM(), state2.getLM(), 0.);
        Assert.assertEquals(state1.getMass("Satellite"), state2.getMass("Satellite"), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @description Test long propagation with variable thrust maneuver and 2 event detectors
     *              (apogee/perigee) included in nth occurrence detectors (detection on 1st
     *              occurrence) should return the same result than a short propagation with variable
     *              thrust maneuver and 2 event detectors (apogee/perigee) with one occurrence of
     *              each event. Second test: maneuver on second occurrence with short propagation
     *              should not perform any maneuver
     * 
     * @input a propagator, an orbit, 2 event detectors and a ContinuousThrustManeuver
     * 
     * @output final states
     * 
     * @testPassCriteria final states are the same
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEventDetectorsOnce() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = this.fireDate;
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0.,
            0., 1., PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate,
            Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        final IDependentVariable<SpacecraftState> thrust = new VariablePressureThrust(this.fireDate, 20,
            1. / 60., new double[] { 0.001, 0.889 });

        // First propagation
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main3");
        builder1.addProperty(new MassProperty(0.), "Main3");
        final TankProperty tank1 = new TankProperty(1000.);
        builder1.addPart("Satellite3", "Main3", Transform.IDENTITY);
        builder1.addProperty(tank1, "Satellite3");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDetector1,
            endDetector1, new PropulsiveProperty(thrust, this.isp), this.direction, massModel1, tank1,
            LOFType.TNW);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(initialOrbit
            .getKeplerianPeriod()));

        // Second propagation
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main4");
        builder2.addProperty(new MassProperty(0.), "Main4");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("Satellite4", "Main4", Transform.IDENTITY);
        builder2.addProperty(tank2, "Satellite4");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final EventDetector startDetector2 = new NthOccurrenceDetector(new ApsideDetector(
            initialOrbit, ApsideDetector.APOGEE), 1, Action.STOP);
        final EventDetector endDetector2 = new NthOccurrenceDetector(new ApsideDetector(
            initialOrbit, ApsideDetector.PERIGEE), 1, Action.STOP);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(startDetector2,
            endDetector2, new PropulsiveProperty(thrust, this.isp), this.direction, massModel2, tank2,
            LOFType.TNW);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2.propagate(initialDate.shiftedBy(initialOrbit
            .getKeplerianPeriod() * 3.));

        // Check final states are equals
        Assert.assertEquals(state1.getA(), state2.getA(), 0.);
        Assert.assertEquals(state1.getEquinoctialEx(), state2.getEquinoctialEx(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state1.getEquinoctialEy(), state2.getEquinoctialEy(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state1.getHx(), state2.getHx(), 0.);
        Assert.assertEquals(state1.getHy(), state2.getHy(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state1.getMass("Satellite3"), state2.getMass("Satellite4"), 0.);

        // Third propagation: maneuver should not start since maneuver is planned in second orbital
        // period
        final MassProvider massModel3 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState3 = new SpacecraftState(initialOrbit, massModel3);
        final EventDetector startDetector3 = new NthOccurrenceDetector(new ApsideDetector(
            initialOrbit, ApsideDetector.APOGEE), 2, Action.STOP);
        final EventDetector endDetector3 = new NthOccurrenceDetector(new ApsideDetector(
            initialOrbit, ApsideDetector.PERIGEE), 2, Action.STOP);
        final TankProperty tank3 = new TankProperty(massModel3.getMass("Satellite"));
        tank3.setPartName("Satellite");
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(startDetector3,
            endDetector3, new PropulsiveProperty(thrust, this.isp), this.direction, massModel3, tank3,
            LOFType.TNW);
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator);
        propagator3.addForceModel(maneuver3);
        propagator3.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState3.getMu())));
        propagator3.setInitialState(initialState3);
        propagator3.setMassProviderEquation(massModel3);
        propagator3.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state3 = propagator3.propagate(initialDate.shiftedBy(initialOrbit
            .getKeplerianPeriod()));

        // Check final state is equal to initial state
        Assert.assertEquals(state3.getA(), initialState3.getA(), 0.);
        Assert.assertEquals(state3.getEquinoctialEx(), initialState3.getEquinoctialEx(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state3.getEquinoctialEy(), initialState3.getEquinoctialEy(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state3.getHx(), initialState3.getHx(), 0.);
        Assert.assertEquals(state3.getHy(), initialState3.getHy(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state3.getMass("Satellite"), initialState3.getMass("Satellite"), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @description Test propagation with several occurrence of variable thrust maneuver. It is
     *              checked that maneuver is performed several times: First propagation with
     *              maneuver between apogee and perigee over one period. Second propagation with
     *              maneuver between apogee and perigee over two period.
     * 
     * @input a propagator, an orbit, 2 event detectors and a ContinuousThrustManeuver
     * 
     * @output final state
     * 
     * @testPassCriteria 2nd maneuver has been performed
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEventDetectorsSeveral() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = this.fireDate;
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0.,
            0., 1., PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate,
            Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation over one period
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main3");
        builder1.addProperty(new MassProperty(0.), "Main3");
        final TankProperty tank1 = new TankProperty(1000.);
        builder1.addPart("Satellite3", "Main3", Transform.IDENTITY);
        builder1.addProperty(tank1, "Satellite3");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDetector1,
            endDetector1, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel1, tank1,
            FramesFactory.getGCRF());
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.setInitialState(initialState1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(initialOrbit
            .getKeplerianPeriod()));

        // Second propagation over two periods
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main4");
        builder2.addProperty(new MassProperty(0.), "Main4");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("Satellite4", "Main4", Transform.IDENTITY);
        builder2.addProperty(tank2, "Satellite4");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final EventDetector startDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(startDetector2,
            endDetector2, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel2, tank2,
            FramesFactory.getGCRF());
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.setInitialState(initialState2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2.propagate(initialDate.shiftedBy(initialOrbit
            .getKeplerianPeriod() * 2.));

        // Check final states are not equals (because 2nd propagation had one more maneuver)
        Assert.assertFalse(state1.getA() - state2.getA() == 0.);
        Assert.assertFalse(state1.getEquinoctialEx() - state2.getEquinoctialEx() == 0.);
        Assert.assertFalse(state1.getEquinoctialEy() - state2.getEquinoctialEy() == 0.);
        Assert.assertFalse(state1.getLM() - state2.getLM() == 0.);
        Assert.assertFalse(state1.getMass("Satellite3") - state2.getMass("Satellite4") == 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test propagation starting with a maneuver in the middle: results should be the
     *              same if the propagation stops/restart during the maneuver. Test performed either
     *              in forward and retro propagation
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustManeuver
     * 
     * @output ephemeris
     * 
     * @testPassCriteria ephemeris are identical
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testPropagationSplitDuringManeuver() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = this.fireDate;
        final AbsoluteDate finalDate = initialDate.shiftedBy(3600);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0.,
            0., 1., PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate,
            Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);
        final EventDetector startDetector = new DateDetector(initialDate.shiftedBy(1000));
        final EventDetector endDetector = new DateDetector(initialDate.shiftedBy(2000));

        final List<SpacecraftState> res1 = new ArrayList<>();
        final List<SpacecraftState> res2 = new ArrayList<>();
        final List<SpacecraftState> res3 = new ArrayList<>();

        // First propagation without split ("reference")

        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main3");
        builder1.addProperty(new MassProperty(0.), "Main3");
        final TankProperty tank1 = new TankProperty(1000.);
        builder1.addPart("Satellite3", "Main3", Transform.IDENTITY);
        builder1.addProperty(tank1, "Satellite3");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);

        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDetector,
            endDetector, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel1, tank1,
            FramesFactory.getGCRF());
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));
        propagator1.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4218267401016163341L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast) {
                try {
                    res1.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator1.propagate(finalDate);

        // Second propagation with split in the middle
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main4");
        builder2.addProperty(new MassProperty(0.), "Main4");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("Satellite4", "Main4", Transform.IDENTITY);
        builder2.addProperty(tank2, "Satellite4");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);

        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(startDetector,
            endDetector, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel2, tank2,
            FramesFactory.getGCRF());
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));
        propagator2.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 4819687001573203686L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res2.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        final SpacecraftState state = propagator2.propagate(initialDate.shiftedBy(1800));

        final AssemblyBuilder builder3 = new AssemblyBuilder();
        builder3.addMainPart("Main5");
        builder3.addProperty(new MassProperty(0.), "Main5");
        final TankProperty tank3 = new TankProperty(state.getMass("Satellite4"));
        builder3.addPart("Satellite5", "Main5", Transform.IDENTITY);
        builder3.addProperty(tank3, "Satellite5");
        final Assembly assembly3 = builder3.returnAssembly();
        final MassProvider massModel3 = new MassModel(assembly3);

        final SpacecraftState initialState3 = new SpacecraftState(state.getOrbit(), state.getAttitude(), massModel3);
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(startDetector,
            endDetector, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel3, tank3,
            FramesFactory.getGCRF());
        maneuver3.setFiring(maneuver2.isFiring());
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator);
        propagator3.addForceModel(maneuver3);
        propagator3.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState3.getMu())));
        propagator3.setInitialState(initialState3);
        propagator3.setMassProviderEquation(massModel3);
        propagator3.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));
        propagator3.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 4548762382319998556L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res2.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator3.propagate(finalDate);

        // Check ephemeris are equals
        for (int i = 0; i < res1.size(); i++) {
            Assert.assertEquals(res1.get(i).getA(), res2.get(i).getA(), 0.);
            Assert.assertEquals(res1.get(i).getEquinoctialEx(), res2.get(i).getEquinoctialEx(), 0.);
            Assert.assertEquals(res1.get(i).getEquinoctialEy(), res2.get(i).getEquinoctialEy(), 0.);
            Assert.assertEquals(res1.get(i).getLM(), res2.get(i).getLM(), 0.);
            double mass2;
            try {
                // First part of propagation
                mass2 = res2.get(i).getMass("Satellite4");
            } catch (final PatriusException e) {
                // Second part of propagation
                mass2 = res2.get(i).getMass("Satellite5");
            }
            Assert.assertEquals(res1.get(i).getMass("Satellite3"), mass2,
                0.);
        }

        // Test in retropolation
        propagator3.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6118244591452407107L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res3.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        // Propagate to middle interval
        final SpacecraftState retroState = propagator3.propagate(initialDate.shiftedBy(1800));

        // Check forward and retro-propagation states
        final double[] stateArray = new double[8];
        final double[] retroStateArray = new double[8];
        state.mapStateToArray(initialOrbit.getType(), PositionAngle.TRUE, stateArray);
        retroState.mapStateToArray(initialOrbit.getType(), PositionAngle.TRUE, retroStateArray);

        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(stateArray[i], retroStateArray[i], 1E-9);
        }

        // Propagate to initialDate
        final AssemblyBuilder builder4 = new AssemblyBuilder();
        builder4.addMainPart("Main6");
        builder4.addProperty(new MassProperty(0.), "Main6");
        final TankProperty tank4 = new TankProperty(state.getMass("Satellite4"));
        builder4.addPart("Satellite6", "Main6", Transform.IDENTITY);
        builder4.addProperty(tank4, "Satellite6");
        final Assembly assembly4 = builder4.returnAssembly();
        final MassProvider massModel4 = new MassModel(assembly4);

        final SpacecraftState initialState4 = new SpacecraftState(retroState.getOrbit(), retroState.getAttitude(),
            massModel4);
        final ContinuousThrustManeuver maneuver4 = new ContinuousThrustManeuver(startDetector,
            endDetector, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel4, tank4,
            FramesFactory.getGCRF());
        maneuver4.setFiring(maneuver3.isFiring());
        final NumericalPropagator propagator4 = new NumericalPropagator(integrator);
        propagator4.addForceModel(maneuver4);
        propagator4.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState4.getMu())));
        propagator4.setInitialState(initialState4);
        propagator4.setMassProviderEquation(massModel4);
        propagator4.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY));
        propagator4.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -8882812805322363087L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res3.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator4.propagate(initialDate);

        // Check ephemeris are equals :
        // thresholds are ajusted for each orbital parameter, validation is done with relative or
        // absolute error
        // computation according the parameter.
        for (int i = 0; i < res1.size(); i++) {

            Assert.assertEquals(0.0, (res1.get(res1.size() - 1 - i).getA() - res3.get(i).getA())
                    / res1.get(res1.size() - 1 - i).getA(), 1.0E-4);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getEquinoctialEx(), res3.get(i)
                .getEquinoctialEx(), 1.0E-5);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getEquinoctialEy(), res3.get(i)
                .getEquinoctialEy(), 1.0E-4);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getLM(), res3.get(i).getLM(), 1.0E-2);
            double mass2;
            try {
                // First part of propagation
                mass2 = res3.get(i).getMass("Satellite5");
            } catch (final PatriusException e) {
                // Second part of propagation
                mass2 = res3.get(i).getMass("Satellite6");
            }
            Assert.assertEquals(
                0.0,
                (res1.get(res1.size() - 1 - i).getMass("Satellite3") - mass2)
                        / res1.get(res1.size() - 1 - i).getMass("Satellite3"), 1.0E-2);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_THRUST_MANEUVER_VALIDATION}
     * 
     * @description Test start/end dates of maneuvers.
     * 
     * @input a variable thrust maneuver
     * 
     * @output maneuver start and end dates
     * 
     * @testPassCriteria date are those expected, null if dates have not been provided
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testStartEndDates() throws PatriusException {

        // Initialization
        final MassProvider massModel = new SimpleMassModel(1000., "Satellite");
        final AbsoluteDate startDate = AbsoluteDate.GALILEO_EPOCH.shiftedBy(1800);
        final AbsoluteDate endDate = startDate.shiftedBy(1800);

        // Case with date and duration
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDate,
            endDate.durationFrom(startDate), new PropulsiveProperty(this.thrust, this.isp), this.direction,
            massModel, new TankProperty(massModel.getMass("Satellite")));
        Assert.assertEquals(maneuver1.getStartDate().durationFrom(startDate), 0., 0.);
        Assert.assertEquals(maneuver1.getEndDate().durationFrom(endDate), 0., 0.);

        // Case with date detectors
        final DateDetector dateDetector1 = new DateDetector(startDate);
        final DateDetector dateDetector2 = new DateDetector(endDate);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(dateDetector1,
            dateDetector2, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel,
            new TankProperty(massModel.getMass("Satellite")));
        Assert.assertEquals(maneuver2.getStartDate().durationFrom(startDate), 0., 0.);
        Assert.assertEquals(maneuver2.getEndDate().durationFrom(endDate), 0., 0.);

        // Case with other detectors
        final EventDetector otherDetector1 = new NullMassDetector(massModel);
        final EventDetector otherDetector2 = new NullMassDetector(massModel);
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(otherDetector1,
            otherDetector2, new PropulsiveProperty(this.thrust, this.isp), this.direction, massModel,
            new TankProperty(massModel.getMass("Satellite")));
        Assert.assertNull(maneuver3.getStartDate());
        Assert.assertNull(maneuver3.getStartDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#computeGradientPosition()}
     * @testedMethod {@link ContinuousThrustManeuver#computeGradientVelocity()}
     * 
     * @description check that no acceleration partial derivatives are handled by this class
     * 
     * @input an instance of {@link ContinuousThrustManeuver}
     * 
     * @output booleans
     * 
     * @testPassCriteria since there are no partial derivatives computation, output booleans must be
     *                   false
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void isComputePDTest() throws PatriusException {
        // Initialization
        final MassProvider massModel = new SimpleMassModel(1000., "Satellite");
        final AbsoluteDate startDate = AbsoluteDate.GALILEO_EPOCH.shiftedBy(1800);
        final AbsoluteDate endDate = startDate.shiftedBy(1800);

        // Case with date and duration
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(startDate,
            endDate.durationFrom(startDate), new PropulsiveProperty(this.thrust, this.isp), this.direction,
            massModel, new TankProperty(massModel.getMass("Satellite")));

        Assert.assertFalse(maneuver.computeGradientPosition());
        Assert.assertFalse(maneuver.computeGradientVelocity());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException {

        final TankProperty tank = new TankProperty(1000.);
        final MassProvider massModel = new SimpleMassModel(1000., "Satellite");
        final double DT = 400;
        final double F = 300;
        final double isp = 300;
        final PropulsiveProperty propulsiveProperty = new PropulsiveProperty(F, isp);
        ContinuousThrustManeuver forceModel = new ContinuousThrustManeuver(
            AbsoluteDate.J2000_EPOCH, DT, propulsiveProperty, Vector3D.PLUS_I, massModel, tank);

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                ContinuousThrustManeuver.class));
        }

        // Check an other constructor
        final AbsoluteDate startDate = AbsoluteDate.GALILEO_EPOCH.shiftedBy(1800);
        final AbsoluteDate endDate = startDate.shiftedBy(1800);
        final DateDetector dateDetector1 = new DateDetector(startDate);
        final DateDetector dateDetector2 = new DateDetector(endDate);
        forceModel = new ContinuousThrustManeuver(dateDetector1, dateDetector2, propulsiveProperty,
            Vector3D.PLUS_I, massModel, tank);

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                ContinuousThrustManeuver.class));
        }
    }

    /**
     * Step handler.
     */
    private class MySH implements PatriusFixedStepHandler {
        /** Serializable UID. */
        private static final long serialVersionUID = 74400842615364899L;

        private final ContinuousThrustManeuver maneuver;
        private AbsoluteDate t0;

        public MySH(final ContinuousThrustManeuver maneuver) {
            this.maneuver = maneuver;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.t0 = s0.getDate();
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast)
            throws PropagationException {

            // Test is here a little different from tests performed for ConstantThrustError and
            // ConstantThrustManeuver:
            // Partial derivatives cannot be computed for ContinuousThrustManeuver. As a result
            // acceleration is checked
            // instead.

            final double dt = currentState.getDate().durationFrom(this.t0);

            try {
                final double[] d = new double[3];
                final TimeDerivativesEquations adder = new TimeDerivativesEquations(){

                    /** Serializable UID. */
                    private static final long serialVersionUID = -1639694273717222165L;

                    @Override
                    public void
                        initDerivatives(final double[] yDot, final Orbit currentOrbit)
                            throws PropagationException {
                        // nothing to do
                    }

                    @Override
                    public void addXYZAcceleration(final double x, final double y, final double z) {
                        // nothing to do
                    }

                    @Override
                    public void
                        addAdditionalStateDerivative(final String name, final double[] pDot) {
                        // nothing to do
                    }

                    @Override
                    public void addAcceleration(final Vector3D gamma, final Frame frame)
                        throws PatriusException {
                        d[0] = gamma.toArray()[0];
                    }
                };

                this.maneuver.addContribution(currentState, adder);
                this.checkData(dt, d[0]);
            } catch (final PatriusException e) {
                Assert.fail();
            }
        }

        /**
         * Check data (0 before and after maneuver, not 0 during the maneuver).
         */
        private void checkData(final double dt, final double data) {
            if (dt < 1800 || dt >= 1800 + 360) {
                Assert.assertEquals(0., data, 0.);
            } else {
                Assert.assertTrue(data != 0.);
            }
        }
    }

    /**
     * 
     * Set up method before running the test.
     * 
     * @throws PatriusException
     * @throws IllegalArgumentException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        // initial date:
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
            new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC());
        // sets the starting date of the maneuver:
        this.fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 15,
            34.080), TimeScalesFactory.getUTC());
        // initial mass:
        this.mass = 4000;

        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        this.tankA = new TankProperty(this.mass);
        builder1.addPart("thruster", "Main", Transform.IDENTITY);
        builder1.addProperty(this.tankA, "thruster");
        final Assembly assembly1 = builder1.returnAssembly();
        this.model1 = new MassModel(assembly1);

        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main2");
        builder2.addProperty(new MassProperty(0.), "Main2");
        this.tankB = new TankProperty(this.mass);
        builder2.addPart("thruster2", "Main2", Transform.IDENTITY);
        builder2.addProperty(this.tankB, "thruster2");
        final Assembly assembly2 = builder2.returnAssembly();
        this.model2 = new MassModel(assembly2);

        // orbit:
        this.mu = CelestialBodyFactory.getEarth().getGM();
        this.orbit = new CircularOrbit(7178000, .0, .0, MathLib.toRadians(98), .0, .0,
            PositionAngle.MEAN, FramesFactory.getEME2000(), initDate, this.mu);
        // inertial attitude law:
        this.law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(Vector3D.PLUS_I,
            Vector3D.PLUS_I)));
        // initial state:
        this.initialState = new SpacecraftState(this.orbit, this.law.getAttitude(this.orbit, this.orbit.getDate(),
            this.orbit.getFrame()), this.model1);
        this.fireState = this.initialState.shiftedBy(this.fireDate.durationFrom(initDate));

        // sets the thrust:
        final double[] coeffs = new double[2];
        coeffs[0] = 1.01;
        coeffs[1] = 0.889;
        this.thrust = new VariablePressureThrust(this.fireDate, 20, 1. / 60., coeffs);
        this.direction = new ConstantDirection(Vector3D.PLUS_I);
        // sets the ISP:
        this.isp = new VariableISP(this.fireDate, 200, 10. / 60.);

        // sets the maneuver:
        this.maneuver = new ContinuousThrustManeuver(this.fireDate, 600, new PropulsiveProperty(this.thrust, this.isp),
            this.direction, this.model1, this.tankA);
        // the second maneuver should be identical to the first:
        this.maneuver2 = new ContinuousThrustManeuver(this.fireDate.shiftedBy(600), -600,
            new PropulsiveProperty(this.thrust, this.isp), this.direction, this.model2, this.tankB);
    }
}
