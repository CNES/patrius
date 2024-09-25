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
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.11:DM:DM-3242:22/05/2023:[PATRIUS] Possibilite de definir les parametres circulaires adaptes pour des orbites hyperboliques
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:FA:FA-2447:27/05/2020:Mathlib.divide() incomplète 
 * VERSION:4.4:DM:DM-2126:04/10/2019:[PATRIUS] Calcul du DeltaV realise
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::FA:93:01/04/2014:changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:293:01/10/2014:Allowed users to define a maneuver by a direction in any frame
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::FA:388:19/02/2015:Restored deprecated constructor + raised exception if SpacecraftFrame as input.
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:453:13/11/2015:Handling propagation starting during a maneuver
 * VERSION::FA:465:16/06/2015:Added analytical computation of partial derivatives
 * VERSION::FA:487:06/11/2015:Start/Stop maneuver correction
 * VERSION::DM:454:24/11/2015:Add TU for detector suppression after nth occurence detection
 * VERSION::DM:454:01/12/2015:Add TU for detector suppression which should be removed
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1851:18/10/2018:Update the massModel from a SimpleMassModel to an Assembly builder
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.ApsideDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.detectors.NodeDetector;
import fr.cnes.sirius.patrius.events.detectors.NullMassDetector;
import fr.cnes.sirius.patrius.forces.VariablePressureThrust;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisAltitudeParametersTest.features;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
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
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class ConstantThrustManeuverTest {

    /** Body mu. */
    private double mu;

    /**
     * dummyOrbit.
     * 
     * @param date
     *        AbsoluteDate
     * @return CircularOrbit
     */
    private CircularOrbit dummyOrbit(final AbsoluteDate date) {
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        return new CircularOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date,
            this.mu);
    }

    /**
     * Validation test for DM 200 : mass becomes negative or null.
     * 
     * @throws PatriusException
     *         bcs of TimeScalesFactory.getUTC()
     * @throws IllegalArgumentException
     *         bcs of TimeScalesFactory.getUTC()
     * @testType UT
     * @description test g function of Null Mass Part detector provided by maneuver object
     * @referenceVersion 2.3.1
     * @nonRegressionVersion 2.3.1
     */
    @Test(expected = IllegalArgumentException.class)
    public void testgFunctionNullMassPartDectector() throws IllegalArgumentException, PatriusException {

        // context
        final AbsoluteDate date0 = new AbsoluteDate(2004, 01, 01, TimeScalesFactory.getUTC());
        final AbsoluteDate date1 = date0.shiftedBy(500);

        final double thrustDuration = 100.0;
        final double thrustForce = 400.0;
        final double isp = 300.0;

        final String partName = "propellantTank";
        final double initialMass = 100.;
        final MassProvider mass = new SimpleMassModel(initialMass, partName);
        final Vector3D direction = Vector3D.PLUS_K;

        // maneuver
        final TankProperty tank = new TankProperty(100.);
        tank.setPartName(partName);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(date0, thrustDuration,
            new PropulsiveProperty(thrustForce, isp), direction, mass, tank);

        // tests on the g function
        final EventDetector[] switches = maneuver.getEventsDetectors();
        final Orbit o2 = this.dummyOrbit(date1);
        Assert.assertTrue(switches[2].g(new SpacecraftState(o2, mass)) > 0);
        final double negativeMass = -50.;
        new SimpleMassModel(negativeMass, partName);
    }

    /**
     * DM 200: handling of negative or null mass.
     * 
     * @testType UT
     * @throws PatriusException
     *         if propagation failed
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * 
     * @description tests proper handling of negative or null mass. Two maneuvers are added in opposite direction since
     *              only one
     *              leads to hyperbolic orbit (acceleration when total mass becomes 0, becomes infinite). In real case
     *              this will not happen
     *              since final mass will not be null in such cases.
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
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, d0, muValue);
        final SpacecraftState spc = new SpacecraftState(orbit, massModel);

        // 2 maneuvers opposite thrust
        final double duration = 30000.0;
        final double dv = 100;
        final double isp = 300;
        final TankProperty tank = new TankProperty(1000.);
        tank.setPartName(partName);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(d1, duration, new PropulsiveProperty(dv,
            isp), Vector3D.MINUS_I, massModel, tank);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(d1, duration, new PropulsiveProperty(
            dv, isp), Vector3D.PLUS_I, massModel, tank);

        // numerical propagator
        final double[][] tol = NumericalPropagator.tolerances(1., orbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(.001, 60, tol[0], tol[1]);

        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(spc);
        propagator.setAttitudeProvider(new BodyCenterPointing(FramesFactory.getEME2000()));
        propagator.setMassProviderEquation(massModel);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(maneuver2);

        // Propagation
        propagator.propagate(d0.shiftedBy(30000));
    }

    /**
     * FA 3314
     * 
     * @testType UT
     * @throws PatriusException
     *         if the frame is not pseudo-inertial
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * 
     * @description tests proper handling of not pseudo-inertial frame
     * 
     * @testPassCriteria a PatriusException is thrown
     * 
     * @referenceVersion 4.11
     */
    @Test
    public void testNotPseudoInertial() throws PatriusException {

        // mass model
        final double mass = 1000;
        final String partName = "tank";
        final MassProvider massModel = new SimpleMassModel(mass, partName);
        /*
         * Spacecraft state
         */
        // frame and date
        final Frame itrf = FramesFactory.getITRF();
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
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, itrf, d0, muValue);
        final SpacecraftState spc = new SpacecraftState(orbit, massModel);

        // 2 maneuvers opposite thrust
        final double duration = 30000.0;
        final double dv = 100;
        final double isp = 300;
        final TankProperty tank = new TankProperty(1000.);
        tank.setPartName(partName);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(d1, duration, new PropulsiveProperty(dv,
            isp), Vector3D.MINUS_I, massModel, tank, LOFType.LVLH);
        maneuver.setFiring(true);
        // Acceleration computation (error thrown)
        try {
            maneuver.computeAcceleration(spc);
            Assert.fail();
        } catch (final PatriusException pe) {
            Assert.assertEquals(pe.getMessage(), PatriusMessages.NOT_INERTIAL_FRAME.getSourceString());
        }
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct.
     * 
     * @throws PatriusException
     *         OrekitException
     * @throws IOException
     *         IOException
     * @throws ParseException
     *         ParseException
     * @testType UT
     * @description test propagation with positive duration Constant thrust maneuver
     */
    @Test
    public void testParamList() throws PatriusException, IOException, ParseException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
            new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC());
        final ContinuousThrustManeuver model = new ContinuousThrustManeuver(date, -10.0, new PropulsiveProperty(400.,
            300.), Vector3D.PLUS_K, new SimpleMassModel(1000., "Satellite"), new TankProperty(1000.));

        double k = 5;
        Assert.assertEquals(2, model.getParameters().size());
        final ArrayList<Parameter> paramList = model.getParameters();
        for (int i = 0; i < paramList.size(); i++) {
            paramList.get(i).setValue(k);
            Assert.assertTrue(Precision.equals(k, paramList.get(i).getValue(), 0));
            k++;
        }
    }

    /**
     * tests Positive duration.
     * 
     * @throws PatriusException
     *         OrekitException
     * @testType UT
     */
    @Test
    public void testPositiveDuration() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
            new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC());
        final Parameter thrust = new Parameter("thrust", 400.0);
        final Parameter flowRate = new Parameter("flow rate", 400.0);
        final PropulsiveProperty engineProp = new PropulsiveProperty(thrust.getValue(), -thrust.getValue()
                / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue()));
        final TankProperty tankProp = new TankProperty(1000.);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(date, 10.0, engineProp, Vector3D.PLUS_K,
            new SimpleMassModel(1000., "no need"), tankProp);
        final EventDetector[] switches = maneuver.getEventsDetectors();

        Assert.assertEquals(date, maneuver.getStartDate());
        Assert.assertEquals(date.shiftedBy(10.), maneuver.getEndDate());
        flowRate.setValue(45.2);
        Assert.assertEquals(45.2, flowRate.getValue(), 1E-14);
        final Orbit o1 = this.dummyOrbit(date.shiftedBy(-1.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        final Orbit o2 = this.dummyOrbit(date.shiftedBy(1.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        final Orbit o3 = this.dummyOrbit(date.shiftedBy(9.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        final Orbit o4 = this.dummyOrbit(date.shiftedBy(11.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);

        Assert.assertEquals(null, maneuver.getFrame());
    }

    /**
     * tests Negative duration.
     * 
     * @throws PatriusException
     *         OrekitException
     * @testType UT
     * @description test propagation with negative duration Constant thrust maneuver
     */
    @Test
    public void testNegativeDuration() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
            new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC());
        // Test the negative duration on the first constructor :
        ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(date, -10.0,
            new PropulsiveProperty(400., 300.), Vector3D.PLUS_K, new SimpleMassModel(1000., "Satellite"),
            new TankProperty(1000.));
        EventDetector[] switches = maneuver.getEventsDetectors();

        final Orbit o1 = this.dummyOrbit(date.shiftedBy(-11.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        final Orbit o2 = this.dummyOrbit(date.shiftedBy(-9.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        final Orbit o3 = this.dummyOrbit(date.shiftedBy(-1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        final Orbit o4 = this.dummyOrbit(date.shiftedBy(1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);

        // Test the negative duration on the second constructor:
        maneuver = new ContinuousThrustManeuver(date, -10.0, new PropulsiveProperty(400., 300.), Vector3D.PLUS_K,
            new SimpleMassModel(1000., "no need"), new TankProperty(1000), FramesFactory.getGCRF());
        switches = maneuver.getEventsDetectors();
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);

        // Test the negative duration on the constructor with LOFType :
        maneuver = new ContinuousThrustManeuver(date, -10.0, new PropulsiveProperty(400., 300.), Vector3D.PLUS_K,
            new SimpleMassModel(1000., "no need"), new TankProperty(1000.), LOFType.TNW);
        switches = maneuver.getEventsDetectors();
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);

        // Test the negative duration with the flow rate constructor
        final PropulsiveProperty engineProp = new PropulsiveProperty(400., -400.
                / (Constants.G0_STANDARD_GRAVITY * 300.));
        final TankProperty tankProp = new TankProperty(1000.);
        maneuver = new ContinuousThrustManeuver(date, -10.0, engineProp, Vector3D.PLUS_K, new SimpleMassModel(1000.,
            "no need"), tankProp);
        switches = maneuver.getEventsDetectors();
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    /**
     * tests Rough behaviour.
     * 
     * @throws PatriusException
     *         OrekitException
     * @testType UT
     * @description test propagation with Constant thrust maneuver
     */
    @Test
    public void testRoughBehaviour() throws PatriusException {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = MathLib.toRadians(7);
        final double omega = MathLib.toRadians(180);
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final Parameter fParam = new Parameter(ContinuousThrustManeuver.THRUST, f);
        final Parameter flowRateParam = new Parameter("flow rate", -f / (Constants.G0_STANDARD_GRAVITY * isp));
        final double delta = MathLib.toRadians(-7.4978);
        final double alpha = MathLib.toRadians(351);
        final AttitudeProvider law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(Vector3D.PLUS_I,
            new Vector3D(alpha, delta))));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01), new TimeComponents(23, 30,
            00.000), TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, this.mu);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");
        final TankProperty tankProp = new TankProperty(mass);
        builder.addPart("thruster", "Main", Transform.IDENTITY);
        builder.addProperty(tankProp, "thruster");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        final SpacecraftState initialState = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(04, 15,
            34.080), TimeScalesFactory.getUTC());
        final PropulsiveProperty engineProp = new PropulsiveProperty(fParam.getValue(), -fParam.getValue()
                / (Constants.G0_STANDARD_GRAVITY * flowRateParam.getValue()));
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(fireDate, duration, engineProp,
            Vector3D.PLUS_I, massModel, tankProp);
        final ArrayList<Parameter> paramList = maneuver.getParameters();
        for (int m = 0; m < paramList.size(); m++) {
            if (paramList.get(m).equals(ContinuousThrustManeuver.THRUST)) {
                Assert.assertEquals(f, paramList.get(m).getValue(), 1.0e-10);
            }
        }
        Assert.assertEquals(isp, maneuver.getISP(), 1.0e-10);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance,
            relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator.setMassProviderEquation(massModel);
        final SpacecraftState finalorb = propagator.propagate(fireDate.shiftedBy(3800));

        final double massTolerance = MathLib.abs(fParam.getValue() * maneuver.getEventsDetectors()[0].getThreshold());
        Assert.assertEquals(2007.8824544261233, finalorb.getMass("thruster"), massTolerance);
        Assert.assertEquals(2007.8824544261233, massModel.getTotalMass(), massTolerance);
        Assert.assertEquals(2.6872, MathLib.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), FastMath.PI)), 1e-4);
        Assert.assertEquals(28970, finalorb.getA() / 1000, 1);
    }

    /**
     * @testType UT
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, LOFType, MassProvider, String)}
     * @description Test propagation with constant thrust maneuver modifies orbital parameters when maneuver starts.
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testSameBeforeFiring() throws IllegalArgumentException, PatriusException {

        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = MathLib.toRadians(7);
        final double omega = MathLib.toRadians(180);
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = MathLib.toRadians(-7.4978);
        final double alpha = MathLib.toRadians(351);
        final AttitudeProvider law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(Vector3D.PLUS_I,
            new Vector3D(alpha, delta))));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01), new TimeComponents(23, 30,
            00.000), TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, this.mu);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        final TankProperty tank = new TankProperty(2500.);
        builder.addPart("thruster", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "thruster");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main2");
        builder1.addProperty(new MassProperty(mass), "Main2");
        final TankProperty tank1 = new TankProperty(2500.);
        builder1.addPart("thruster", "Main2", Transform.IDENTITY);
        builder1.addProperty(tank1, "thruster");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);

        final SpacecraftState initialState = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel);
        final SpacecraftState initialState1 = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel1);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(04, 15,
            34.080), TimeScalesFactory.getUTC());

        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(fireDate, duration,
            new PropulsiveProperty(f, isp), Vector3D.PLUS_I, massModel, tank);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 0.001 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };

        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance,
            relTolerance);
        final AdaptiveStepsizeIntegrator integrator1 = new DormandPrince853Integrator(0.001, 1000, absTolerance,
            relTolerance);

        final NumericalPropagator propagatorWith = new NumericalPropagator(integrator);
        final NumericalPropagator propagatorWithout = new NumericalPropagator(integrator1);

        propagatorWith.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagatorWith.setInitialState(initialState);
        propagatorWith.setMassProviderEquation(massModel);
        propagatorWith.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagatorWithout.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagatorWithout.setInitialState(initialState1);
        propagatorWithout.setMassProviderEquation(massModel1);
        propagatorWithout.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));

        propagatorWith.addForceModel(maneuver);

        for (AbsoluteDate t = initDate; t.durationFrom(fireDate) < 8000; t = t.shiftedBy(600)) {
            final PVCoordinates with = propagatorWith.propagate(t).getPVCoordinates();
            final PVCoordinates without = propagatorWithout.propagate(t).getPVCoordinates();

            if (t.compareTo(fireDate) < 0) {
                Assert.assertEquals(0, new PVCoordinates(with, without).getPosition().getNorm(),
                    Precision.DOUBLE_COMPARISON_EPSILON);
            } else {
                Assert
                    .assertTrue(new PVCoordinates(with, without).getPosition().getNorm() > Precision.DOUBLE_COMPARISON_EPSILON);
            }
        }
    }

    /**
     * DM 293 : Allowed users to define a maneuver by a direction in any frame.
     * 
     * @testType UT
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, LOFType, MassProvider, String)}
     * @description Test propagation with constant thrust maneuver in any frame following velocity in spacecraft frame
     *              is equal
     *              to propagation with constant thrust maneuver in any frame.
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testAnyDirectionFrame() throws PatriusException {
        final double[] coeffs = new double[2];
        coeffs[0] = 1.01;
        coeffs[1] = 0.889;
        new VariablePressureThrust(AbsoluteDate.J2000_EPOCH, 20, 1. / 60., coeffs);
        final Frame eme2000 = FramesFactory.getEME2000();

        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = MathLib.toRadians(7);
        final double omega = MathLib.toRadians(180);
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = MathLib.toRadians(-7.4978);
        final double alpha = MathLib.toRadians(351);
        final AttitudeProvider law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(Vector3D.PLUS_I,
            new Vector3D(alpha, delta)));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01), new TimeComponents(23, 30,
            00.000), TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE, eme2000, initDate,
            this.mu);
        final String partName = "thruster";
        final MassProvider massModel = new SimpleMassModel(mass, partName);

        final SpacecraftState initialState = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(04, 15,
            34.080), TimeScalesFactory.getUTC());

        final TankProperty tank = new TankProperty(massModel.getMass(partName));
        tank.setPartName(partName);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(fireDate, duration,
            new PropulsiveProperty(f, isp), Vector3D.PLUS_I, massModel, tank, eme2000);
        maneuver.setFiring(true);
        final Vector3D acc = maneuver.computeAcceleration(initialState);
        final Vector3D expectedAcc = new Vector3D(f / mass, Vector3D.PLUS_I);

        Assert.assertEquals(acc, expectedAcc);
        Assert.assertEquals(eme2000, maneuver.getFrame());
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, LOFType, MassProvider, String)}
     * @description Test propagation with impulse maneuver in TNW frame following velocity in spacecraft frame is equal
     *              to propagation with impulse maneuver in TNW.
     * @referenceVersion 2.3.1
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testManeuverWithLOF() throws PatriusException, IOException, ParseException {
        // mass model
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank = new TankProperty(1000.);
        builder1.addPart("thruster", "Main", Transform.IDENTITY);
        builder1.addProperty(tank, "thruster");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel = new MassModel(assembly1);

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
        this.mu = Constants.GRIM5C1_EARTH_MU;

        // inertial frame
        final Frame gcrf = FramesFactory.getGCRF();
        // initial orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, date0, this.mu);

        // keplerian period
        final double T = orbit.getKeplerianPeriod();
        // Final date
        final AbsoluteDate finalDate = date0.shiftedBy(T * 20);

        // attitude provider
        final AttitudeProvider attProv = new LofOffset(gcrf, LOFType.TNW);

        // initial attitude
        final Attitude initialAttitude = attProv.getAttitude(orbit, date0, orbit.getFrame());

        // tol
        final double[][] tol = NumericalPropagator.tolerances(1, orbit, OrbitType.CARTESIAN);

        // integrator
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1, 7200, tol[0], tol[1]);

        // bulletin initial
        final SpacecraftState etat = new SpacecraftState(orbit, initialAttitude, massModel);

        // propagator
        final NumericalPropagator prop1 = new NumericalPropagator(dop853, etat.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        // Propagator initialization
        prop1.setInitialState(etat);
        prop1.clearEventsDetectors();
        prop1.setAttitudeProvider(attProv);
        prop1.setMassProviderEquation(massModel);

        final AbsoluteDate fireDate = date0.shiftedBy(10);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(fireDate, 100, new PropulsiveProperty(
            420, 318), Vector3D.PLUS_I, massModel, tank);
        // Add impulsional maneuver
        prop1.addForceModel(maneuver);
        prop1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(etat.getMu())));

        // propagation
        final SpacecraftState endStateNullFrame = prop1.propagate(finalDate);

        Assert.assertTrue((endStateNullFrame.getMass("thruster")) < 1000.);

        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main2");
        builder2.addProperty(new MassProperty(0.), "Main2");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("thruster", "Main2", Transform.IDENTITY);
        builder2.addProperty(tank2, "thruster");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        // bulletin initial
        final SpacecraftState etat2 = new SpacecraftState(orbit, initialAttitude, massModel2);

        // propagateur
        final NumericalPropagator prop2 = new NumericalPropagator(dop853, etat2.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);
        // Propagator initialization
        prop2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(etat2.getMu())));
        prop2.setInitialState(etat2);
        prop2.clearEventsDetectors();
        prop2.setAttitudeProvider(attProv);
        prop2.setMassProviderEquation(massModel2);

        // impulsion
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(fireDate, 100, new PropulsiveProperty(
            420, 318), Vector3D.PLUS_I, massModel2, tank2, LOFType.TNW);

        // Ajout manoeuvre impulsionnelle
        prop2.addForceModel(maneuver2);

        // propagation
        final SpacecraftState endStateLOF = prop2.propagate(finalDate);
        Assert.assertTrue((endStateLOF.getMass("thruster")) < 1000.);

        // Comparison
        // --------------------------------------------

        // Absolute difference !
        Assert.assertEquals(endStateNullFrame.getA(), endStateLOF.getA(), 1.0e-6);
        Assert.assertEquals(endStateNullFrame.getEquinoctialEx(), endStateLOF.getEquinoctialEx(), 1.0e-13);
        Assert.assertEquals(endStateNullFrame.getEquinoctialEy(), endStateLOF.getEquinoctialEy(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getHx(), endStateLOF.getHx(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getHy(), endStateLOF.getHy(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getLM(), endStateLOF.getLM(), 1.0e-10);
        Assert.assertEquals(endStateNullFrame.getMass("thruster"), endStateLOF.getMass("thruster"), 1.0e-14);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ContinuousThrustManeuver#computeAcceleration(SpacecraftState)}
     * 
     * @description Test for coverage purpose. Test lines added by FA414.
     *              See MassDetectorsTest for functionality test
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
        final KeplerianOrbit orbit = new KeplerianOrbit(0, 0, 0, 0, 0, 0, PositionAngle.TRUE, gcrf, date0, this.mu);

        final String DEFAULT = "default";
        class ZeroMassModel implements MassProvider {
            /** Serializable UID. */
            private static final long serialVersionUID = 8731442811078018560L;

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
            public void updateMass(final String partName,
                                   final double mass) throws PatriusException {
                // nothing to do
            }

            @Override
            public void setMassDerivativeZero(final String partName) {
                // nothing to do
            }

            @Override
            public void addMassDerivative(final String partName,
                                          final double flowRate) {
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
        final TankProperty tank = new TankProperty(0.);
        tank.setPartName(DEFAULT);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(AbsoluteDate.J2000_EPOCH, 100.,
            new PropulsiveProperty(420, 318), Vector3D.PLUS_I, massModel, tank);

        // Cover 'else" of "if (forward ^ firing) {" from FiringStartDetector.eventOccurred method
        Assert.assertTrue(maneuver.getEventsDetectors()[0].getClass().getName().contains("FiringStartDetector"));
        Assert.assertEquals(maneuver.getEventsDetectors()[0].eventOccurred(s, false, false).name(),
            Action.CONTINUE.name());

        // Cover 'else" of "if (forward ^ !firing) {" from FiringStopDetector.eventOccurred method
        Assert.assertTrue(maneuver.getEventsDetectors()[1].getClass().getName().contains("FiringStopDetector"));
        Assert.assertEquals(maneuver.getEventsDetectors()[1].eventOccurred(s, false, true).name(),
            Action.CONTINUE.name());

        // Set "firing" of FiringStartDetector to "true"
        maneuver.getEventsDetectors()[0].eventOccurred(s, false, true);
        Assert.assertEquals(maneuver.getEventsDetectors()[0].eventOccurred(s, false, true).name(),
            Action.RESET_STATE.name());

        // Cover following lines of addContribution method
        // if (massModel.getMass(partName) == 0 && firing) {
        // firing = false;
        // removeMassDv = false;
        // }
        maneuver.addContribution(s, null);
        Assert.assertTrue(!maneuver.isFiring());

        // Cover "else" of "if (s.getMass(partName) != 0) {" from computeAcceleration method
        Assert.assertEquals(maneuver.computeAcceleration(s), Vector3D.ZERO);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDState(SpacecraftState, double[][], double[][])}
     * 
     * @description Test output of partial derivatives computation methods wrt state
     * 
     * @input maneuver
     * 
     * @output partial derivatives with respect to state
     * 
     * @testPassCriteria he returned derivatives vectors should be the same as the input one.
     * 
     * @referenceVersion 3.0.1
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testDAccDState() throws PatriusException {
        final String MAIN = "main";
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(new AbsoluteDate(), 100,
            new PropulsiveProperty(420, 318), Vector3D.PLUS_I, new SimpleMassModel(1000., MAIN), new TankProperty(
                1000.));
        final KeplerianOrbit orbit = new KeplerianOrbit(0, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
            new AbsoluteDate(), this.mu);
        final SpacecraftState s = new SpacecraftState(orbit);

        // Check the returned PD are not modified by calling addDAccDState
        final double[][] dAccdPos = new double[1][1];
        dAccdPos[0][0] = 1.0;
        final double[][] dAccdVel = new double[1][1];
        dAccdVel[0][0] = 2.0;
        maneuver.addDAccDState(s, dAccdPos, dAccdVel);
        Assert.assertEquals(1.0, dAccdPos[0][0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(2.0, dAccdVel[0][0], Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @description Test computation of partial derivatives wrt to thrust parameter
     * 
     * @input maneuver
     * 
     * @output partial derivatives wrt to thrust parameter
     * 
     * @testPassCriteria the computed partial derivatives should be equal to the partial derivatives computed by finite
     *                   difference
     * 
     * @referenceVersion 3.0.1
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testDAccDParam() throws PatriusException, IOException, ParseException {
        this.testDAccDParam(null, LOFType.TNW);
        this.testDAccDParam(null, null);
        this.testDAccDParam(FramesFactory.getGCRF(), null);
    }

    /**
     * Private method
     * 
     * @param frame
     * @param type
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    private final void testDAccDParam(final Frame frame,
                                      final LOFType type) throws PatriusException, IOException, ParseException {
        // mass model
        final String thruster = "thruster";
        final MassProvider massModel = new SimpleMassModel(1000., thruster);
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
        this.mu = Constants.GRIM5C1_EARTH_MU;

        // inertial frame
        final Frame gcrf = FramesFactory.getGCRF();
        // initial orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, date0, this.mu);

        // keplerian period
        final double T = orbit.getKeplerianPeriod();
        date0.shiftedBy(T * 20);

        // attitude provider
        final AttitudeProvider attProv = new LofOffset(gcrf, LOFType.TNW);

        // attitude initiale
        final Attitude initialAttitude = attProv.getAttitude(orbit, date0, orbit.getFrame());

        // tol
        final double[][] tol = NumericalPropagator.tolerances(1, orbit, OrbitType.CARTESIAN);

        // integrateur
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1, 7200, tol[0], tol[1]);

        // bulletin initial
        final SpacecraftState initialState = new SpacecraftState(orbit, initialAttitude, massModel);

        // propagateur
        final NumericalPropagator prop1 = new NumericalPropagator(dop853, initialState.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        // initialisation du propagateur
        prop1.setInitialState(initialState);
        prop1.clearEventsDetectors();
        prop1.setAttitudeProvider(attProv);
        prop1.setMassProviderEquation(massModel);

        final AbsoluteDate fireDate = date0.shiftedBy(10);
        final double f = 420.;
        final double isp = 318.;
        final Parameter thrust = new Parameter("thrust", f);
        final Parameter flowRate = new Parameter("flow rate", -f / (Constants.G0_STANDARD_GRAVITY * isp));

        ContinuousThrustManeuver maneuver = null;
        if (frame == null) {
            if (type == null) {
                final PropulsiveProperty engineProp = new PropulsiveProperty(thrust, new Parameter("Isp",
                    -thrust.getValue() / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue())));
                final TankProperty tankProp = new TankProperty(massModel.getMass(thruster));
                tankProp.setPartName(thruster);
                maneuver = new ContinuousThrustManeuver(fireDate, 100., engineProp, Vector3D.PLUS_I, massModel,
                    tankProp);
            } else {
                final PropulsiveProperty propulsiveProperty = new PropulsiveProperty(thrust, new Parameter("Isp", isp));
                final TankProperty tankProperty = new TankProperty(massModel.getMass(thruster));
                tankProperty.setPartName(thruster);
                maneuver = new ContinuousThrustManeuver(fireDate, 100., propulsiveProperty, Vector3D.PLUS_I, massModel,
                    tankProperty, LOFType.TNW);
            }
        } else {
            final PropulsiveProperty engineProp = new PropulsiveProperty(thrust, new Parameter("Isp",
                -thrust.getValue() / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue())));
            final TankProperty tankProp = new TankProperty(massModel.getMass(thruster));
            tankProp.setPartName("thruster");
            maneuver = new ContinuousThrustManeuver(fireDate, 100., engineProp, Vector3D.PLUS_I, massModel, tankProp,
                frame);
        }

        // Ajout manoeuvre impulsionnelle
        prop1.addForceModel(maneuver);

        prop1.propagate(fireDate.shiftedBy(10.));

        // Partial derivatives computation
        // ---------------------------------
        double[] dAccdParam = new double[3];
        maneuver.addDAccDParam(initialState, thrust, dAccdParam);

        // coefficient difference:
        final double diff = .00001;

        // Thrust parameter
        // Thrust 1
        final Parameter thrust1 = new Parameter("thrust", f - diff / 2.);
        // Thrust 2
        final Parameter thrust2 = new Parameter("thrust", f + diff / 2.);
        ContinuousThrustManeuver maneuver1 = null;
        ContinuousThrustManeuver maneuver2 = null;

        if (frame == null) {
            if (type == null) {
                final PropulsiveProperty engineProp1 = new PropulsiveProperty(thrust1.getValue(), -thrust1.getValue()
                        / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue()));
                final PropulsiveProperty engineProp2 = new PropulsiveProperty(thrust2.getValue(), -thrust2.getValue()
                        / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue()));
                final TankProperty tankProp = new TankProperty(massModel.getMass(thruster));
                tankProp.setPartName(thruster);
                maneuver1 = new ContinuousThrustManeuver(fireDate, 100., engineProp1, Vector3D.PLUS_I, massModel,
                    tankProp);
                maneuver2 = new ContinuousThrustManeuver(fireDate, 100., engineProp2, Vector3D.PLUS_I, massModel,
                    tankProp);
            } else {
                final PropulsiveProperty propulsiveProperty1 = new PropulsiveProperty(thrust1,
                    new Parameter("Isp", isp));
                final PropulsiveProperty propulsiveProperty2 = new PropulsiveProperty(thrust2,
                    new Parameter("Isp", isp));
                final TankProperty tankProperty = new TankProperty(massModel.getMass(thruster));
                tankProperty.setPartName(thruster);
                maneuver1 = new ContinuousThrustManeuver(fireDate, 100., propulsiveProperty1, Vector3D.PLUS_I,
                    massModel, tankProperty, LOFType.TNW);
                maneuver2 = new ContinuousThrustManeuver(fireDate, 100., propulsiveProperty2, Vector3D.PLUS_I,
                    massModel, tankProperty, LOFType.TNW);
            }
        } else {
            final PropulsiveProperty engineProp1 = new PropulsiveProperty(thrust1, new Parameter("Isp",
                -thrust1.getValue() / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue())));
            final PropulsiveProperty engineProp2 = new PropulsiveProperty(thrust2, new Parameter("Isp",
                -thrust2.getValue() / (Constants.G0_STANDARD_GRAVITY * flowRate.getValue())));
            final TankProperty tankProp = new TankProperty(massModel.getMass(thruster));
            tankProp.setPartName(thruster);
            maneuver1 = new ContinuousThrustManeuver(fireDate, 100., engineProp1, Vector3D.PLUS_I, massModel, tankProp,
                frame);
            maneuver2 = new ContinuousThrustManeuver(fireDate, 100., engineProp2, Vector3D.PLUS_I, massModel, tankProp,
                frame);
        }

        maneuver1.setFiring(true);
        maneuver2.setFiring(true);

        // finite differences:
        final Vector3D acc1 = maneuver1.computeAcceleration(initialState);
        final Vector3D acc2 = maneuver2.computeAcceleration(initialState);
        final double[] DPdiff = { (acc2.getX() - acc1.getX()) / diff, (acc2.getY() - acc1.getY()) / diff,
            (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(DPdiff, dAccdParam, 1E-10);

        // Check an exception is thrown when calling addDAccDParam
        boolean testOk = false;
        try {
            dAccdParam = new double[3];
            maneuver.addDAccDParam(initialState, new Parameter("default", 0.), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e1) {
            testOk = true;
        }
        Assert.assertTrue(testOk);
    }

    /**
     * Launched before every tests.
     */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataPBASE");

        // Body mu
        this.mu = 3.9860047e14;
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @testedMethod {@link ContinuousThrustManeuver#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @description During a propagation, the partial derivatives are computed only when "firing".
     * 
     * @input a propagator, an orbit and a ConstantThrustManeuver.
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
        final AbsoluteDate t0 = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate date = t0.shiftedBy(1800);
        final double duration = 360;
        final MassProvider massModel = new SimpleMassModel(1000., "Satellite");
        final Parameter thrust = new Parameter("Thrust", 1.);
        final PropulsiveProperty engineProp = new PropulsiveProperty(thrust, new Parameter("Isp", -thrust.getValue()
                / (Constants.G0_STANDARD_GRAVITY * (-1. / 4000.))));
        final TankProperty tankProp = new TankProperty(massModel.getMass("Satellite"));
        tankProp.setPartName("Satellite");
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(date, duration, engineProp,
            Vector3D.PLUS_I, massModel, tankProp, FramesFactory.getGCRF());
        final Orbit orbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 0., PositionAngle.TRUE,
            FramesFactory.getGCRF(), t0, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState s = new SpacecraftState(orbit, massModel);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(1);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        final MySH handler = new MySH(maneuver, thrust);

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
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test propagation with constant thrust maneuver and 2 date event detectors should return the same
     *              result when compared to
     *              constant thrust maneuver with a date and a duration
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustManeuver
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
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate maneuverDate = initialDate.shiftedBy(1800);
        final double maneuverDuration = 360;
        final AbsoluteDate endManeuverDate = maneuverDate.shiftedBy(maneuverDuration);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final double propagationDuration = 3600;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation
        final MassProvider massModel1 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final TankProperty tank1 = new TankProperty(1000.);
        tank1.setPartName("Satellite");
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(maneuverDate, maneuverDuration,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel1, tank1);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(propagationDuration));

        // Second propagation
        final MassProvider massModel2 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final DateDetector detector1 = new DateDetector(maneuverDate);
        final DateDetector detector2 = new DateDetector(endManeuverDate);
        final TankProperty tank2 = new TankProperty(1000.);
        tank2.setPartName("Satellite");
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(detector1, detector2,
            new PropulsiveProperty(1., 400.), Vector3D.PLUS_I, massModel2, tank2);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2.propagate(initialDate.shiftedBy(propagationDuration));

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
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test long propagation with constant thrust maneuver and 2 event detectors (apogee/perigee) included
     *              in nth occurrence
     *              detectors (detection on 1st occurrence) should return the same result than a short propagation with
     *              constant thrust maneuver and 2 event detectors (apogee/perigee)
     *              with one occurrence of each event. Second test: maneuver on second occurrence with short propagation
     *              should not perform any maneuver
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustManeuver
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
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank1 = new TankProperty(1000.);
        builder1.addPart("Satellite", "Main", Transform.IDENTITY);
        builder1.addProperty(tank1, "Satellite");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);

        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDetector1, endDetector1,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel1, tank1, LOFType.TNW);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

        // Second propagation
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main2");
        builder2.addProperty(new MassProperty(0.), "Main2");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("Satellite", "Main2", Transform.IDENTITY);
        builder2.addProperty(tank2, "Satellite");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final EventDetector startDetector2 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.APOGEE), 1, Action.STOP);
        final EventDetector endDetector2 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.PERIGEE), 1, Action.STOP);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(startDetector2, endDetector2,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel2, tank2, LOFType.TNW);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2
            .propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod() * 3.));

        // Check final states are equals
        Assert.assertEquals(state1.getA(), state2.getA(), 0.);
        Assert.assertEquals(state1.getEquinoctialEx(), state2.getEquinoctialEx(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state1.getEquinoctialEy(), state2.getEquinoctialEy(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state1.getHx(), state2.getHx(), 0.);
        Assert.assertEquals(state1.getHy(), state2.getHy(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state1.getMass("Satellite"), state2.getMass("Satellite"), 0.);

        // Third propagation: maneuver should not start since maneuver is planned in second orbital period
        final MassProvider massModel3 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState3 = new SpacecraftState(initialOrbit, massModel3);
        final EventDetector startDetector3 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.APOGEE), 2, Action.STOP);
        final EventDetector endDetector3 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.PERIGEE), 2, Action.STOP);
        final TankProperty tank = new TankProperty(1000.);
        tank.setPartName("Satellite");
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(startDetector3, endDetector3,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel3, tank, LOFType.TNW);
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator);
        propagator3.addForceModel(maneuver3);
        propagator3.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState3.getMu())));
        propagator3.setInitialState(initialState3);
        propagator3.setMassProviderEquation(massModel3);
        propagator3.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state3 = propagator3.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

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
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test propagation with several occurrence of constant thrust maneuver. It is checked that maneuver is
     *              performed several times:
     *              First propagation with maneuver between apogee and perigee over one period.
     *              Second propagation with maneuver between apogee and perigee over two period.
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustManeuver
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
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation over one period
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank1 = new TankProperty(1000.);
        builder1.addPart("Satellite", "Main", Transform.IDENTITY);
        builder1.addProperty(tank1, "Satellite");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDetector1, endDetector1,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel1, tank1, FramesFactory.getGCRF());
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.setInitialState(initialState1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

        // Second propagation over two periods
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main2");
        builder2.addProperty(new MassProperty(0.), "Main2");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("Satellite", "Main2", Transform.IDENTITY);
        builder2.addProperty(tank2, "Satellite");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final EventDetector startDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(startDetector2, endDetector2,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel2, tank2, FramesFactory.getGCRF());
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.setInitialState(initialState2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2
            .propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod() * 2.));

        // Check final states are not equals (because 2nd propagation had one more maneuver)
        Assert.assertFalse(state1.getA() - state2.getA() == 0.);
        Assert.assertFalse(state1.getEquinoctialEx() - state2.getEquinoctialEx() == 0.);
        Assert.assertFalse(state1.getEquinoctialEy() - state2.getEquinoctialEy() == 0.);
        Assert.assertFalse(state1.getLM() - state2.getLM() == 0.);
        Assert.assertFalse(state1.getMass("Satellite") - state2.getMass("Satellite") == 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test start/end dates of maneuvers.
     * 
     * @input a constant thrust maneuver
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
            endDate.durationFrom(startDate), new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel,
            new TankProperty(1000.));
        Assert.assertEquals(maneuver1.getStartDate().durationFrom(startDate), 0., 0.);
        Assert.assertEquals(maneuver1.getEndDate().durationFrom(endDate), 0., 0.);

        // Case with date detectors
        final DateDetector dateDetector1 = new DateDetector(startDate);
        final DateDetector dateDetector2 = new DateDetector(endDate);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(dateDetector1, dateDetector2,
            new PropulsiveProperty(1., 400.), Vector3D.PLUS_I, massModel, new TankProperty(1000.));
        Assert.assertEquals(maneuver2.getStartDate().durationFrom(startDate), 0., 0.);
        Assert.assertEquals(maneuver2.getEndDate().durationFrom(endDate), 0., 0.);

        // Case with other detectors
        final EventDetector otherDetector1 = new NullMassDetector(massModel);
        final EventDetector otherDetector2 = new NullMassDetector(massModel);
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(otherDetector1, otherDetector2,
            new PropulsiveProperty(1., 400.), Vector3D.PLUS_I, massModel, new TankProperty(1000.));
        Assert.assertNull(maneuver3.getStartDate());
        Assert.assertNull(maneuver3.getStartDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test propagation starting with a maneuver in the middle: results should be the same if the
     *              propagation stops/restart during the maneuver.
     *              Test performed either in forward and retro propagation
     * 
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
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = initialDate.shiftedBy(3600);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);
        final EventDetector startDetector = new DateDetector(initialDate.shiftedBy(1000));
        final EventDetector endDetector = new DateDetector(initialDate.shiftedBy(2000));

        final List<SpacecraftState> res1 = new ArrayList<>();
        final List<SpacecraftState> res2 = new ArrayList<>();
        final List<SpacecraftState> res3 = new ArrayList<>();

        // First propagation without split ("reference")
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank1 = new TankProperty(1000.);
        builder1.addPart("Satellite", "Main", Transform.IDENTITY);
        builder1.addProperty(tank1, "Satellite");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(startDetector, endDetector,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel1, tank1, FramesFactory.getGCRF());

        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator1.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 6497232601789961324L;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) {
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
        builder2.addMainPart("Main2");
        builder2.addProperty(new MassProperty(0.), "Main2");
        final TankProperty tank2 = new TankProperty(1000.);
        builder2.addPart("Satellite", "Main2", Transform.IDENTITY);
        builder2.addProperty(tank2, "Satellite");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final ContinuousThrustManeuver maneuver2 = new ContinuousThrustManeuver(startDetector, endDetector,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel2, tank2, FramesFactory.getGCRF());
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator2.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2228006013029270686L;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                try {
                    res2.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        final SpacecraftState state = propagator2.propagate(initialDate.shiftedBy(1800));

        final AssemblyBuilder builder3 = new AssemblyBuilder();
        builder3.addMainPart("Main3");
        builder3.addProperty(new MassProperty(0.), "Main3");
        final TankProperty tank3 = new TankProperty(state.getMass("Satellite"));
        builder3.addPart("Satellite", "Main3", Transform.IDENTITY);
        builder3.addProperty(tank3, "Satellite");
        final Assembly assembly3 = builder3.returnAssembly();
        final MassProvider massModel3 = new MassModel(assembly3);
        final SpacecraftState initialState3 = new SpacecraftState(state.getOrbit(), state.getAttitude(), massModel3);
        final ContinuousThrustManeuver maneuver3 = new ContinuousThrustManeuver(startDetector, endDetector,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel3, tank3, FramesFactory.getGCRF());
        maneuver3.setFiring(maneuver2.isFiring());
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator);
        propagator3.addForceModel(maneuver3);
        propagator3.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState3.getMu())));
        propagator3.setInitialState(initialState3);
        propagator3.setMassProviderEquation(massModel3);
        propagator3.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator3.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7700228588206677265L;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
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
            Assert.assertEquals(res1.get(i).getMass("Satellite"), res2.get(i).getMass("Satellite"), 0.);
        }

        // Test in retropolation
        propagator3.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5460620985221160057L;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
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
            Assert.assertEquals(stateArray[i], retroStateArray[i], Precision.DOUBLE_COMPARISON_EPSILON);
        }

        // Propagate to initialDate
        final AssemblyBuilder builder4 = new AssemblyBuilder();
        builder4.addMainPart("Main4");
        builder4.addProperty(new MassProperty(0.), "Main4");
        final TankProperty tank4 = new TankProperty(state.getMass("Satellite"));
        builder4.addPart("Satellite", "Main4", Transform.IDENTITY);
        builder4.addProperty(tank4, "Satellite");
        final Assembly assembly4 = builder4.returnAssembly();
        final MassProvider massModel4 = new MassModel(assembly4);
        final SpacecraftState initialState4 = new SpacecraftState(retroState.getOrbit(), retroState.getAttitude(),
            massModel4);
        final ContinuousThrustManeuver maneuver4 = new ContinuousThrustManeuver(startDetector, endDetector,
            new PropulsiveProperty(1., 400), Vector3D.PLUS_I, massModel4, tank4, FramesFactory.getGCRF());
        maneuver4.setFiring(maneuver3.isFiring());
        final NumericalPropagator propagator4 = new NumericalPropagator(integrator);
        propagator4.addForceModel(maneuver4);
        propagator4.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState4.getMu())));
        propagator4.setInitialState(initialState4);
        propagator4.setMassProviderEquation(massModel4);
        propagator4.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator4.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1450121969115383716L;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                try {
                    res3.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator4.propagate(initialDate);

        // Check ephemeris are equals :
        // thresholds are ajusted for each orbital parameter, validation is done with relative or absolute error
        // computation according the parameter.
        for (int i = 0; i < res1.size(); i++) {

            Assert.assertEquals(0.0,
                (res1.get(res1.size() - 1 - i).getA() - res3.get(i).getA()) / res1.get(res1.size() - 1 - i).getA(),
                1.0E-7);

            Assert.assertEquals(res1.get(res1.size() - 1 - i).getEquinoctialEx(), res3.get(i).getEquinoctialEx(),
                1.0E-8);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getEquinoctialEy(), res3.get(i).getEquinoctialEy(),
                1.0E-7);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getLM(), res3.get(i).getLM(), 1.0E-2);

            Assert.assertEquals(
                0.0,
                (res1.get(res1.size() - 1 - i).getMass("Satellite") - res3.get(i).getMass("Satellite"))
                        / res1.get(res1.size() - 1 - i).getMass("Satellite"), 1.0E-3);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Compute a constant thrust maneuver from ascending node to descending node
     *              based on detectors which should be removed after detection. The maneuver should be the same
     *              for a propagation during one orbital period than for a propagation during fourteen orbital periods.
     * 
     * @input numerical propagator with classical LEO, constant attitude, classical force integrator
     * @input ascending node detector
     * @input descending node detector
     * @input constant thrust maneuver of 1 N in x sat axis direction between ascending and descending node
     * 
     * @output propagated spacecraft state at one orbital periods
     * @output propagated spacecraft state at fourteen orbital periods
     * 
     * @testPassCriteria the two spacecraft state orbit have the same semi major axis
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testRemoveDetector() throws PatriusException {

        // initial date, orbit, attitude, spacecraftstate
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(7000E3, 0.001, MathLib.toRadians(70), 0, 0,
            MathLib.toRadians(-1.), PositionAngle.MEAN, FramesFactory.getEME2000(), initDate, this.mu);
        final AttitudeProvider attLaw = new LofOffset(orbit.getFrame(), LOFType.LVLH);
        final Attitude attitude = attLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        // mass model
        final double mass = 1000;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        final TankProperty tank = new TankProperty(1000.);
        builder.addPart("tank", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "tank");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        final SpacecraftState initialState = new SpacecraftState(orbit, attitude, massModel);

        // manoeuvre du noeud ascendant au noeud descendant thrust = 1 N, direction = X sat
        final double thrust = 1;
        final NodeDetector ascendingDetector = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.ASCENDING,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, true);
        final NodeDetector descendingDetector = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.DESCENDING,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, true);

        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(ascendingDetector, descendingDetector,
            new PropulsiveProperty(thrust, 300.), Vector3D.PLUS_I, massModel, tank);

        final NumericalPropagator prop = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10));
        prop.setInitialState(initialState);
        prop.setAttitudeProviderForces(attLaw);
        prop.setMassProviderEquation(massModel);
        prop.addForceModel(maneuver);
        prop.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));

        // propagation 1 * T
        final SpacecraftState ssT = prop.propagate(initDate.shiftedBy(1 * initialState.getKeplerianPeriod()));

        // Reset
        prop.resetInitialState(initialState);

        // propagation 14 * T
        final SpacecraftState ss14T = prop.propagate(initDate.shiftedBy(14 * initialState.getKeplerianPeriod()));

        Assert.assertEquals(ssT.getA(), ss14T.getA(), 1E-14);
    }

    /**
     * @throws PatriusException
     * @testType UT
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
     * @testPassCriteria since there are no partial derivatives computation, output booleans must be false
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void isComputePDTest() throws PatriusException {
        final String MAIN = "main";
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(new AbsoluteDate(), 100,
            new PropulsiveProperty(420, 318), Vector3D.PLUS_I, new SimpleMassModel(1000., MAIN), new TankProperty(
                1000.));

        Assert.assertFalse(maneuver.computeGradientPosition());
        Assert.assertFalse(maneuver.computeGradientVelocity());
    }

    /**
     * @throws PatriusException
     * @testType UT for constructors coverage and validation
     * 
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, PropulsiveProperty, Vector3D, MassProvider, TankProperty)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(EventDetector, EventDetector, PropulsiveProperty, Vector3D, MassProvider, TankProperty)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, MassProvider, String)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, Parameter, Parameter, Vector3D, MassProvider, String)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, PropulsiveProperty, Parameter, Vector3D, MassProvider, TankProperty)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, PropulsiveProperty, Vector3D, MassProvider, TankProperty, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, PropulsiveProperty, Vector3D, MassProvider, TankProperty, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(EventDetector, EventDetector, double, double, Vector3D, MassProvider, String)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(EventDetector, EventDetector, PropulsiveProperty, Vector3D, MassProvider, TankProperty, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(EventDetector, EventDetector, PropulsiveProperty, Vector3D, MassProvider, TankProperty, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, MassProvider, String, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, MassProvider, String, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, Parameter, Parameter, Vector3D, MassProvider, String, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, Parameter, Parameter, Vector3D, MassProvider, String, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, PropulsiveProperty, Parameter, Vector3D, MassProvider, TankProperty, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(AbsoluteDate, double, PropulsiveProperty, Parameter, Vector3D, MassProvider, TankProperty, LOFType)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(EventDetector, EventDetector, double, double, Vector3D, MassProvider, String, Frame)}
     * @testedMethod {@link ContinuousThrustManeuver#ConstantThrustManeuver(EventDetector, EventDetector, double, double, Vector3D, MassProvider, String, LOFType)}
     * 
     * @description This test aims at verifying that the new {@link ContinuousThrustManeuver} constructors providing
     *              {@link TankProperty} and {@link PropulsiveProperty} have the same behavior than the former symmetric
     *              constructors (i.e providing the same information
     *              in order to build the properties).
     * 
     * @input instances of {@link ContinuousThrustManeuver}
     * 
     * @output instances attributes
     * 
     * @testPassCriteria Instances attributes must provide the same results (mass, isp, thrust).
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testConstructorsProperties() throws PatriusException {

        // Input for maneuver constructors
        final String MAIN = "main";
        final String THRUST = "lhmgj";
        final double duration = 100.;
        final double thrust = 420.;
        final double isp = 318.;
        final double mass = 1000.;
        final Vector3D direction = Vector3D.PLUS_I;
        final MassProvider massProv = new SimpleMassModel(mass, MAIN);
        FramesFactory.getGCRF();
        new Parameter("flow rate", -thrust / (Constants.G0_STANDARD_GRAVITY * isp));
        new Parameter(THRUST, thrust);

        new NodeDetector(FramesFactory.getEME2000(), NodeDetector.ASCENDING, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, true);
        new NodeDetector(FramesFactory.getEME2000(), NodeDetector.DESCENDING, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, true);

        // Dummy spacecraft
        final AbsoluteDate date = new AbsoluteDate();
        final Orbit testOrbit = new KeplerianOrbit(10000000., 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date.shiftedBy(100.), Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(testOrbit);

        // Tank and engine properties
        final TankProperty tank = new TankProperty(mass);
        final PropulsiveProperty engine = new PropulsiveProperty(new Parameter("ThrustProp", thrust), new Parameter(
            "Isp", isp));

        // Create all maneuvers : former constructor/new constructor with properties provided
        final ContinuousThrustManeuver maneuver1 = new ContinuousThrustManeuver(date, duration, new PropulsiveProperty(
            thrust, isp), direction, massProv, tank);
        final ContinuousThrustManeuver maneuver1Prop = new ContinuousThrustManeuver(date, duration, engine, direction,
            massProv, tank);

        // Build a maneuvers list
        final ContinuousThrustManeuver[] maneuvers = new ContinuousThrustManeuver[] { maneuver1 };

        final ContinuousThrustManeuver[] maneuversProp = new ContinuousThrustManeuver[] { maneuver1Prop };

        // Tank and propulsive properties
        IPartProperty tankProp1;
        IPartProperty tankProp2;
        IPartProperty engineProp1;
        IPartProperty engineProp2;
        double mass1;
        double mass2;
        double ispVal1;
        double ispVal2;
        double thrustVal1;
        double thrustVal2;
        String nameTank1;
        ArrayList<Parameter> params1;
        ArrayList<Parameter> params2;

        // Loop on maneuvers list
        for (int i = 0; i < maneuvers.length; i++) {

            // Properties for usual maneuver
            tankProp1 = maneuvers[i].getTankProperty();
            engineProp1 = maneuvers[i].getPropulsiveProperty();
            mass1 = ((TankProperty) tankProp1).getMass();
            ispVal1 = maneuvers[i].getISP();
            thrustVal1 = ((PropulsiveProperty) engineProp1).getThrust(state);
            nameTank1 = ((TankProperty) tankProp1).getPartName();
            params1 = maneuvers[i].getParameters();

            // Properties for maneuver defined directly with TankProperty and PropulsiveProperty
            tankProp2 = maneuversProp[i].getTankProperty();
            engineProp2 = maneuversProp[i].getPropulsiveProperty();
            mass2 = ((TankProperty) tankProp2).getMass();
            ispVal2 = maneuversProp[i].getISP();
            thrustVal2 = ((PropulsiveProperty) engineProp2).getThrust(state);
            params2 = maneuversProp[i].getParameters();

            // Comparisons : retrieved mass, isp, thrust,
            // parts name and number of model parameters must be the same
            Assert.assertEquals(mass1, mass2, 0.);

            // ISP is slightly different for constructors for which ISP value is not
            // directly provided (recomputed as -F / (g0 * flow rate)
            Assert.assertEquals(ispVal1, ispVal2, 6.0E-14);
            Assert.assertEquals(thrustVal1, thrustVal2, 0.);

            // Usual maneuver build a TankProperty with main part name
            Assert.assertSame(nameTank1, "");

            // 2 parameters for models : thrust and flow rate, name is as expected
            // In particular, flow rate parameter as the expected name, even if
            // the PropulsiveProperty is built with random name
            Assert.assertEquals(params1.size(), 2, 0);
            Assert.assertEquals(params1.size(), params2.size(), 0);

            // Flow rate param
            Assert.assertEquals(params1.get(0).getName(), params2.get(0).getName());

            // Thrust param : same name for the two instances in the case where
            // the parameter is not provided as input
            Assert.assertEquals(params1.get(1).getName(), "ContinuousThrustManeuver_thrust");
            Assert.assertEquals(params2.get(1).getName(), "ContinuousThrustManeuver_ThrustProp");
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test the instantaneous deltaV computation service featured by the {@link ContinuousThrustManeuver}
     *              class.
     *              A constant thrust maneuver is initialized with a start date set 1 hour after initial date for a
     *              thrust duration of 23 hours.
     *              The deltaV value will be controlled every 1 minute over the whole analysis window.
     *              To be considered as valid, the deltaV value must :
     *              <ul>
     *              <li>Be equal to 0. before the maneuver occurs</li>
     *              <li>Always increase over time</li>
     *              <li>Be equals to an expected final value which is computed by the test and doesn't take in account
     *              all the integration steps but only the initial and final states.</li>
     *              </ul>
     * 
     * @input a propagator, an orbit, a maneuver date & duration and a ConstantThrustManeuver
     * 
     * @output final state
     * 
     * @testPassCriteria all the validity controls are passed for the usedDV value
     * 
     * @referenceVersion 4.4
     */
    @Test
    public void testDeltaVCalculation() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");
        final double initialTankMass = 1000.;
        final TankProperty tank = new TankProperty(initialTankMass);
        builder.addPart("Satellite", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "Satellite");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        final double isp = 400.;
        final PropulsiveProperty engine = new PropulsiveProperty(1., isp);

        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);
        final double maneuverStartDt = 3600;
        final AbsoluteDate maneuverStartDate = initialDate.shiftedBy(maneuverStartDt);
        final double maneuverDuration = 23 * 3600;

        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(maneuverStartDate, maneuverDuration,
            engine, new Vector3D(10., 0., 0.), massModel, tank, LOFType.TNW);

        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator.setInitialState(initialState);
        propagator.setMassProviderEquation(massModel);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final double analysisWindow = maneuverStartDt + maneuverDuration + 10.;
        double usedDVTemp = 0.;

        // Loop over all the analysis window with a 1min step to control the DV value evolution during the maneuver
        for (double dt = 0.; dt <= analysisWindow; dt += 60) {

            propagator.propagate(initialDate.shiftedBy(dt));

            // Consumed dV should be 0. before the maneuver occurs
            if (dt <= maneuverStartDt) {
                Assert.assertEquals(0., maneuver.getUsedDV().getNorm(), 0.);
            }

            // Consumed dV should always increase over time
            if (usedDVTemp > maneuver.getUsedDV().getNorm()) {
                Assert.fail();
            }

            usedDVTemp = maneuver.getUsedDV().getNorm();
        }

        /*
         * Consumed dV should equals the expected value which doesn't take in account all the integration steps but only
         * the initial and final states.
         */
        final double finalDVExpected = isp * Constants.G0_STANDARD_GRAVITY
                * MathLib.log(MathLib.divide(initialTankMass, massModel.getTotalMass()));
        Assert.assertEquals(finalDVExpected, maneuver.getUsedDV().getNorm(), 1e-10);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description checks that the mass model is properly updated all along a propagation containing a continuous
     *              maneuver and then an impulsive maneuver
     * 
     * @input a constant maneuver and an impulse maneuver
     * 
     * @output tank mass inside mass model
     * 
     * @testPassCriteria mass model is prolery update all along the propagation
     * 
     * @referenceVersion 4.5
     */
    @Test
    public void testMassModelUpdate() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");
        final double initialTankMass = 1000.;
        final TankProperty tank = new TankProperty(initialTankMass);
        builder.addPart("Satellite", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "Satellite");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        final PropulsiveProperty engine = new PropulsiveProperty(100., 400.);

        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);
        final AbsoluteDate maneuverStartDate = initialDate.shiftedBy(100);
        final AbsoluteDate impulseDate = initialDate.shiftedBy(1000);

        // Constant maneuver from t0 + 100s to t0 + 800s.
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(maneuverStartDate, 700, engine,
            new Vector3D(10., 0., 0.), massModel, tank, LOFType.TNW);
        // Impulsive maneuver à t0 + 1000s
        final ImpulseManeuver impulse = new ImpulseManeuver(new DateDetector(impulseDate), new Vector3D(100, 0, 0),
            400, massModel, "Satellite");

        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addForceModel(maneuver);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator.addEventDetector(impulse);
        propagator.setInitialState(initialState);
        propagator.setMassProviderEquation(massModel);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        // Master mode
        propagator.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -8671111626069245098L;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                try {
                    // Check mass is the same
                    Assert.assertEquals(interpolator.getInterpolatedState().getMass("Satellite"),
                        massModel.getMass("Satellite"), 0.);
                    // System.out.println(interpolator.getInterpolatedDate().durationFrom(initialDate) + " "
                    // + interpolator.getInterpolatedState().getMass("Satellite") + " "
                    // + massModel.getMass("Satellite"));
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        // Propagation
        propagator.propagate(initialDate.shiftedBy(2000));
    }

    /**
     * @throws PatriusException
     *         Continuous maneuver creation
     * @testType UT
     * 
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

        final ContinuousThrustManeuver man = new ContinuousThrustManeuver(AbsoluteDate.J2000_EPOCH, 20.,
            new PropulsiveProperty(20, 30), Vector3D.PLUS_I, null, new TankProperty(300), LOFType.QSW);
        Assert.assertTrue(man.getLofType().equals(LOFType.QSW));
    }

    /**
     * Step handler.
     */
    private class MySH implements PatriusFixedStepHandler {
        /** Serializable UID. */
        private static final long serialVersionUID = 74400842615364899L;

        private final ContinuousThrustManeuver maneuver;
        private final Parameter thrust;
        private AbsoluteDate t0;

        public MySH(final ContinuousThrustManeuver maneuver,
                    final Parameter thrust) {
            this.maneuver = maneuver;
            this.thrust = thrust;
        }

        @Override
        public void init(final SpacecraftState s0,
                         final AbsoluteDate t) {
            this.t0 = s0.getDate();
        }

        @Override
        public void handleStep(final SpacecraftState currentState,
                               final boolean isLast) throws PropagationException {

            final double dt = currentState.getDate().durationFrom(this.t0);

            final double[] d = new double[3];
            try {
                this.maneuver.addDAccDParam(currentState, this.thrust, d);
                this.checkData(dt, d[0]);
            } catch (final PatriusException e) {
                Assert.fail();
            }
        }

        /**
         * Check data (0 before and after maneuver, not 0 during the maneuver).
         */
        private void checkData(final double dt,
                               final double data) {
            if (dt < 1800 || dt >= 1800 + 360) {
                Assert.assertEquals(0., data, 0.);
            } else {
                Assert.assertTrue(data != 0.);
            }
        }
    }

    /**
     * Local NthOccurrence detector.
     */
    public class NthOccurrenceDetector implements EventDetector {
        /** Serializable UID. */
        private static final long serialVersionUID = 5133919155468114056L;
        /** Event */
        private final EventDetector event;
        /** Occurrence to detect */
        private final int nth;
        /** Behavior upon detection */
        private final Action action;
        /** Current occurrence */
        private int n;
        /** To inhibit further detections */
        private double constant;
        /** action performed when nbr occurences not reached */
        private final Action actionUnderOcc;

        /**
         * Constructor, create an instance to detect the nthOccurrence of event.
         * 
         * @param eventToDetect
         *        event to detect
         * @param occurrence
         *        number of the event occurrence to detect
         * @param actionAtOccurrence
         *        behavior upon detection
         */
        public NthOccurrenceDetector(final EventDetector eventToDetect,
                                     final int occurrence,
                                     final Action actionAtOccurrence) {
            this.event = eventToDetect;
            this.nth = occurrence;
            this.action = actionAtOccurrence;
            this.n = 1;
            this.actionUnderOcc = Action.CONTINUE;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0,
                         final AbsoluteDate t) {
            this.n = 1;
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            if (this.n <= this.nth) {
                return this.event.g(s);
            }
            return this.constant;
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s,
                                    final boolean increasing,
                                    final boolean forward) throws PatriusException {
            // Check slope is as expected
            final boolean rightSlope = (increasing && this.event.getSlopeSelection() != EventDetector.DECREASING)
                    || (!increasing && this.event.getSlopeSelection() != EventDetector.INCREASING);
            if (rightSlope && this.n++ == this.nth) {
                this.constant = increasing ? 1 : -1;
                return this.action;
            }
            return this.actionUnderOcc;
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            return oldState;
        }

        /** {@inheritDoc} */
        @Override
        public double getThreshold() {
            return this.event.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public double getMaxCheckInterval() {
            return this.event.getMaxCheckInterval();
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            return this.event.getMaxIterationCount();
        }

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return this.event.getSlopeSelection();
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
