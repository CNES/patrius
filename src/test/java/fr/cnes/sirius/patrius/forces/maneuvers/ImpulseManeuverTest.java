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
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3217:22/05/2023:[PATRIUS] Modeles broadcast et almanach GNSS
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2126:04/10/2019:[PATRIUS] Calcul du DeltaV realise
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:293:01/10/2014:Allowed users to define a maneuver by a direction in any frame
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:388:19/02/2015:Restored deprecated constructor + raised exception if SpacecraftFrame as input.
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:449:10/08/2015:Added error if attitudeForces == null and attitudeEvents != null
 * VERSION::FA:449:18/12/2015:Suppression of test NoAttitudeDefined
 * VERSION::FA:449:22/12/2015:Coverage test for new attitude handling
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Vehicle;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.AltitudeDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.detectors.NodeDetector;
import fr.cnes.sirius.patrius.forces.ForceModelsDataTest;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class ImpulseManeuverTest {

    @Test
    public void testInclinationManeuver() throws PatriusException {
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, law);
        propagator.addEventDetector(maneuver);
        final SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
        // coverage tests:
        Assert.assertEquals(400.0, maneuver.getIsp(), 0.0);
        Assert.assertEquals(dv, maneuver.getDeltaVSat().getY(), 0.0);
        Assert.assertNotNull(maneuver.getTrigger());
    }

    @Test
    // Test added for coverage purpose
    // Keplerian propagator declared with two attitude provider
        public
        void
            testInclinationManeuver2() throws PatriusException {
        // attitude provider for events computation declared
        KeplerianPropagator prop = new KeplerianPropagator(initialOrbit, law, law, initialOrbit.getMu());
        prop.addEventDetector(maneuver);
        SpacecraftState propagated = prop.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
        // coverage tests:
        Assert.assertEquals(400.0, maneuver.getIsp(), 0.0);
        Assert.assertEquals(dv, maneuver.getDeltaVSat().getY(), 0.0);
        Assert.assertNotNull(maneuver.getTrigger());

        // attitude provider for forces computation declared
        prop = new KeplerianPropagator(initialOrbit, law, null, initialOrbit.getMu());
        prop.addEventDetector(maneuver);
        propagated = prop.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
        // coverage tests:
        Assert.assertEquals(400.0, maneuver.getIsp(), 0.0);
        Assert.assertEquals(dv, maneuver.getDeltaVSat().getY(), 0.0);
        Assert.assertNotNull(maneuver.getTrigger());
    }

    @Test
    public void testInclinationManeuverNumericalPropagatorAdditional() throws PatriusException {
        propagator.setInitialState(new SpacecraftState(initialOrbit, attitude, massModel));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator
            .getInitialState().getMu())));
        propagator.setMassProviderEquation(massModel);

        propagator.addEventDetector(maneuver);
        propagator.setAttitudeProvider(law);

        final SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
        // coverage tests:
        Assert.assertEquals(400.0, maneuver.getIsp(), 0.0);
        Assert.assertEquals(dv, maneuver.getDeltaVSat().getY(), 0.0);
        Assert.assertNotNull(maneuver.getTrigger());
    }

    @Test
    public void testInclinationManeuverNumericalPropagatorEvent() throws PatriusException {
        propagator.setInitialState(new SpacecraftState(initialOrbit, attitude, massModel));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator
            .getInitialState().getMu())));
        propagator.setMassProviderEquation(massModel);
        propagator.addEventDetector(maneuver);
        propagator.setAttitudeProvider(law);

        final SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
        // coverage tests:
        Assert.assertEquals(400.0, maneuver.getIsp(), 0.0);
        Assert.assertEquals(dv, maneuver.getDeltaVSat().getY(), 0.0);
        Assert.assertNotNull(maneuver.getTrigger());
    }

    @Test
    public void testAnyDirectionFrame() throws PatriusException {
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, attitude);
        maneuver = new ImpulseManeuver(new NodeDetector(initialOrbit, eme2000, 2),
            new Vector3D(dv, Vector3D.PLUS_J), eme2000, 400.0, massModel, thruster);
        maneuver.eventOccurred(initialState, true, true);
        // tests
        final PVCoordinates oldPV = initialState.getPVCoordinates();
        final SpacecraftState newState = maneuver.resetState(initialState);
        final PVCoordinates newPV = newState.getPVCoordinates();
        final Vector3D expectedPosition = oldPV.getPosition();
        final Vector3D expectedVelocity = oldPV.getVelocity().add(new Vector3D(dv, Vector3D.PLUS_J));

        Assert.assertEquals(expectedPosition, newPV.getPosition());
        Assert.assertEquals(expectedVelocity, newPV.getVelocity());
        Assert.assertEquals(eme2000, maneuver.getFrame());
    }

    // test coverage
    @Test
    public void testRetropropagation() throws PatriusException {
        final AbsoluteDate T1 = initialDate.shiftedBy(1000.);
        final AbsoluteDate T2 = initialDate.shiftedBy(2000.);
        maneuver = new ImpulseManeuver(new DateDetector(T1, 100, 1.e-8),
            new Vector3D(dv, Vector3D.PLUS_J), eme2000, 400.0, massModel, thruster);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, attitude, massModel);
        // Numerical propagation
        propagator.resetInitialState(initialState);
        propagator.setMassProviderEquation(massModel);
        propagator.addEventDetector(maneuver);

        final SpacecraftState s0 = propagator.getInitialState();
        final SpacecraftState s2 = propagator.propagate(T2);
        final SpacecraftState sR = propagator.propagate(initialDate);

        // Test mass decreases after first propagation
        Assert.assertTrue(s0.getMass(thruster) > (s2.getMass(thruster) + 119));
        // Test final mass is equal to the initial mass after propagation and retro-propagation
        Assert.assertEquals(s0.getMass(thruster), sR.getMass(thruster), 1e-12);
    }

    // test coverage
    @Test(expected = PropagationException.class)
    public void testException() throws PatriusException {
        class myMassModel implements MassProvider {
            /** Serializable UID. */
            private static final long serialVersionUID = -5433905005060304290L;

            @Override
            public double getTotalMass() {
                return 10000;
            }

            @Override
            public double getTotalMass(final SpacecraftState state) {
                return this.getTotalMass();
            }

            @Override
            public double getMass(final String partName) {
                return 500;
            }

            @Override
            public void updateMass(final String partName, final double mass) {
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
                return null;
            }

            @Override
            public void setMassDerivativeZero(final String partName) {
                // nothing to do
            }
        }
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, attitude);
        maneuver = new ImpulseManeuver(new NodeDetector(initialOrbit, eme2000, 2),
            new Vector3D(5, Vector3D.PLUS_J), 1, new myMassModel(), thruster);
        maneuver.eventOccurred(initialState, true, true);
        maneuver.resetState(initialState);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * @testedMethod {@link ImpulseManeuver#ImpulseManeuver(fr.cnes.sirius.patrius.events.EventDetector, Vector3D, double, MassProvider, String, LOFType)}
     * @description Test propagation with impulse maneuver in TNW frame following velocity in spacecraft frame is equal
     *              to propagation with impulse maneuver in TNW.
     * @referenceVersion 2.3.1
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testManeuverWithLOF() throws PatriusException, IOException, ParseException {
        final FramesConfiguration config = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2003Configuration(true));

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
        final AbsoluteDate date0 = new AbsoluteDate(2002, 1, 1, 6, 0, 0,
            tai);
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // inertial frame
        final Frame gcrf = FramesFactory.getGCRF();
        // initial orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa,
            raan, w, PositionAngle.TRUE, gcrf, date0, mu);

        // keplerian period
        final double T = orbit.getKeplerianPeriod();
        // Final date
        final AbsoluteDate finalDate = date0.shiftedBy(T * 20);

        // attitude provider
        final AttitudeProvider attProv = new LofOffset(gcrf, LOFType.TNW);

        // attitude initiale
        final Attitude initialAttitude = attProv.getAttitude(orbit,
            date0, orbit.getFrame());

        // tol
        final double[][] tol = NumericalPropagator.tolerances(1, orbit,
            OrbitType.CARTESIAN);

        // integrateur
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1,
            7200, tol[0], tol[1]);

        // bulletin initial
        final SpacecraftState etat = new SpacecraftState(orbit, initialAttitude, massModel);

        // propagateur
        final NumericalPropagator prop1 = new NumericalPropagator(dop853, etat.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);
        // initialisation du propagateur
        prop1.setInitialState(etat);
        prop1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(etat.getMu())));
        prop1.clearEventsDetectors();
        prop1.setAttitudeProvider(attProv);
        prop1.setMassProviderEquation(massModel);

        final NodeDetector nodeDetector = new NodeDetector(itrf,
            NodeDetector.ASCENDING_DESCENDING,
            orbit.getKeplerianPeriod() / 2, 1e-6);
        final Vector3D impPlane = new Vector3D(20, new Vector3D(1, 0, 0));
        final double isp = 300;
        final ImpulseManeuver impulsePlane = new ImpulseManeuver(nodeDetector,
            impPlane, null, isp, massModel, thruster);

        // Ajout manoeuvre impulsionnelle
        prop1.addEventDetector(impulsePlane);

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
        prop2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(etat2.getMu())));
        prop2.clearEventsDetectors();
        prop2.setAttitudeProvider(attProv);
        prop2.setMassProviderEquation(massModel);

        // impulsion
        final ImpulseManeuver impulsePlane2 = new ImpulseManeuver(nodeDetector,
            impPlane, isp, massModel, thruster, LOFType.TNW);

        // Ajout manoeuvre impulsionnelle
        prop2.addEventDetector(impulsePlane2);

        // propagation
        final SpacecraftState endStateLOF = prop2.propagate(finalDate);
        Assert.assertTrue((endStateLOF.getMass(thruster)) < 1000.);

        // Comparison
        // --------------------------------------------
        Assert.assertEquals(endStateNullFrame.getA(), endStateLOF.getA(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getEquinoctialEx(), endStateLOF.getEquinoctialEx(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getEquinoctialEy(), endStateLOF.getEquinoctialEy(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getHx(), endStateLOF.getHx(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getHy(), endStateLOF.getHy(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getLM(), endStateLOF.getLM(), 1.0e-14);
        Assert.assertEquals(endStateNullFrame.getMass(thruster), endStateLOF.getMass(thruster), 1.0e-14);

        FramesFactory.setConfiguration(config);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ImpulseManeuver#resetState(SpacecraftState)}
     * 
     * @description Test exception raised by the method resetState(state) if state has no attitude forces
     * 
     * @input a SpacecraftState
     * 
     * @output an exception
     * 
     * @testPassCriteria an error should be raised
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testResetStateError() throws PatriusException {
        final SpacecraftState state = new SpacecraftState(initialOrbit);

        // An exception should occur here !
        maneuver.resetState(state);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ImpulseManeuver#resetState(SpacecraftState)}
     * 
     * @description Test tank and propulsive properties
     * 
     * @input an impulsive maneuver
     * 
     * @output reseted state
     * 
     * @testPassCriteria state after maneuver is the same with old and new constructors
     * 
     * @referenceVersion 3.5
     * 
     * @nonRegressionVersion 3.5
     */
    @Test
    public void testTankPropulsiveProperties() throws PatriusException {

        // Data initialization
        final EventDetector detector = new DateDetector(AbsoluteDate.J2000_EPOCH);
        final Vector3D deltaV = new Vector3D(100, 200, 300);
        final Frame frame = FramesFactory.getGCRF();
        final LOFType lof = LOFType.TNW;
        final double isp = 300;
        final PropulsiveProperty engine = new PropulsiveProperty(200., isp);
        final String part = "Tank";
        final TankProperty tank = new TankProperty(1000);

        final Vehicle vehicle = new Vehicle();
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 2.));
        vehicle.setDryMass(2000.);
        vehicle.addEngine("Engine", engine);
        vehicle.addTank(part, tank);
        final MassModel massModel = new MassModel(vehicle.createAssembly(FramesFactory.getGCRF()));

        // Maneuvers initialization
        final ImpulseManeuver manOld1 = new ImpulseManeuver(detector, deltaV, isp, massModel, part);
        final ImpulseManeuver manOld2 = new ImpulseManeuver(detector, deltaV, frame, isp, massModel, part);
        final ImpulseManeuver manOld3 = new ImpulseManeuver(detector, deltaV, isp, massModel, part, lof);
        final ImpulseManeuver manNew1 = new ImpulseManeuver(detector, deltaV, engine, massModel, tank);
        final ImpulseManeuver manNew2 = new ImpulseManeuver(detector, deltaV, frame, engine, massModel, tank);
        final ImpulseManeuver manNew3 = new ImpulseManeuver(detector, deltaV, engine, massModel, tank, lof);

        final SpacecraftState state = new SpacecraftState(initialOrbit, attitude, massModel);

        // Check returned reseted state are the same independently of the constructor
        final SpacecraftState stateOld1 = manOld1.resetState(state);
        final SpacecraftState stateOld2 = manOld2.resetState(state);
        final SpacecraftState stateOld3 = manOld3.resetState(state);
        final SpacecraftState stateNew1 = manNew1.resetState(state);
        final SpacecraftState stateNew2 = manNew2.resetState(state);
        final SpacecraftState stateNew3 = manNew3.resetState(state);

        Assert.assertEquals(0.,
            stateOld1.getPVCoordinates().getPosition().subtract(stateNew1.getPVCoordinates().getPosition())
                .getNorm(), 0.);
        Assert.assertEquals(0.,
            stateOld1.getPVCoordinates().getVelocity().subtract(stateNew1.getPVCoordinates().getVelocity())
                .getNorm(), 0.);
        Assert.assertEquals(0.,
            stateOld2.getPVCoordinates().getPosition().subtract(stateNew2.getPVCoordinates().getPosition())
                .getNorm(), 0.);
        Assert.assertEquals(0.,
            stateOld2.getPVCoordinates().getVelocity().subtract(stateNew2.getPVCoordinates().getVelocity())
                .getNorm(), 0.);
        Assert.assertEquals(0.,
            stateOld3.getPVCoordinates().getPosition().subtract(stateNew3.getPVCoordinates().getPosition())
                .getNorm(), 0.);
        Assert.assertEquals(0.,
            stateOld3.getPVCoordinates().getVelocity().subtract(stateNew3.getPVCoordinates().getVelocity())
                .getNorm(), 0.);

        // Check tank and propulsive properties
        Assert.assertEquals(engine.getIspParam().getValue(), manNew1.getIsp(), 0);
        Assert.assertEquals(engine.getIspParam().getValue(), manNew1.getPropulsiveProperty().getIspParam().getValue(),
            0);
        Assert.assertEquals(tank.getMass(), manNew1.getTankProperty().getMass(), 0);
    }

    /**
     * Test FT-468: ephemeris from analytical propagation including maneuver must be good (forward and backward mode).
     */
    @Test
    public void testEphemerisModeKep() throws PatriusException {
        // Initialization
        final Orbit orbit = new KeplerianOrbit(7200000.0, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN,
            FramesFactory.getCIRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
        final Propagator propagator = new KeplerianPropagator(orbit);
        final double period = orbit.getKeplerianPeriod();
        final EventDetector detector1 = new DateDetector(orbit.getDate().shiftedBy(period));
        propagator.addEventDetector(new ImpulseManeuver(detector1, new Vector3D(1000, 0, 0), FramesFactory.getCIRF(),
            200., new SimpleMassModel(1000., "sat"), "sat"));

        // Check unset ephemeris mode
        try {
            propagator.getGeneratedEphemeris();
            Assert.fail();
        } catch (final IllegalStateException e) {
            // Expected (unset ephemeris mode)
        }

        // Forward propagation
        propagator.setEphemerisMode();
        propagator.propagate(orbit.getDate().shiftedBy(2. * period));

        // Check ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        for (int i = 0; i <= 100; i++) {
            final AbsoluteDate date = orbit.getDate().shiftedBy(i * period / 100.);
            Assert.assertEquals(orbit.getA(), ephemeris.propagate(date).getA(), 0.);
        }
        for (int i = 1; i < 100; i++) {
            final AbsoluteDate date = orbit.getDate().shiftedBy(period + i * period / 100.);
            Assert.assertEquals(7332447.474411614, ephemeris.propagate(orbit.getDate(), date).getA(), 0.);
        }

        // Other checks
        Assert.assertEquals(ephemeris.getMinDate().durationFrom(orbit.getDate()), 0., 0.);
        Assert.assertEquals(ephemeris.getMaxDate().durationFrom(orbit.getDate().shiftedBy(2. * period)), 0., 0.);
        Assert.assertEquals(ephemeris.getFrame(), FramesFactory.getCIRF());
        try {
            ephemeris.propagate(orbit.getDate().shiftedBy(-3. * period));
        } catch (final PropagationException e) {
            // Expected (date out of ephemeris boundaries)
        }
        try {
            ephemeris.propagate(orbit.getDate().shiftedBy(3. * period));
        } catch (final PropagationException e) {
            // Expected (date out of ephemeris boundaries)
        }

        // Backward propagation
        final Propagator propagator2 = new KeplerianPropagator(orbit);
        final EventDetector detector2 = new DateDetector(orbit.getDate().shiftedBy(-period));
        propagator2.addEventDetector(new ImpulseManeuver(detector2, new Vector3D(1000, 0, 0), FramesFactory.getCIRF(),
            200., new SimpleMassModel(1000., "sat"), "sat"));
        propagator2.setEphemerisMode();
        propagator2.propagate(orbit.getDate().shiftedBy(-2. * period));
        final BoundedPropagator ephemeris2 = propagator2.getGeneratedEphemeris();
        for (int i = 0; i <= 100; i++) {
            final AbsoluteDate date = orbit.getDate().shiftedBy(-i * period / 100.);
            Assert.assertEquals(orbit.getA(), ephemeris2.propagate(date).getA(), 0.);
        }
        for (int i = 1; i < 100; i++) {
            final AbsoluteDate date = orbit.getDate().shiftedBy(-period - i * period / 100.);
            Assert.assertEquals(7332447.474411614, ephemeris2.propagate(orbit.getDate(), date).getA(), 0.);
        }

    }

    /**
     * Test FT-468 (simplified): error test case from FT-468: final ephemeris point in Ephemeris Generation Mode must be
     * good and events must be detected.
     */
    @Test
    public void testAnalyticalEphemerisEventDetectionKep() throws PatriusException {

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate, Constants.EGM96_EARTH_MU);

        final MassProvider massModel = new SimpleMassModel(1000, "part");

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator.addEventDetector(new ImpulseManeuver(new DateDetector(initDate.shiftedBy(43200)), new Vector3D(1000,
            0, 0), 400., massModel, "part"));
        propagator.setEphemerisMode();
        propagator.propagate(initDate.shiftedBy(86400.));

        // Ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // Initial state must be good
        Assert.assertEquals(8000E3, ephemeris.propagate(initDate).getA(), 0.);

        // Events must be detected (final state must be event date
        ephemeris.addEventDetector(new DateDetector(initDate.shiftedBy(12000)));
        final SpacecraftState finalStateEvent = ephemeris.propagate(initDate, initDate.shiftedBy(86400.));
        Assert.assertEquals(12000., finalStateEvent.getDate().durationFrom(initDate), 0.);
    }

    // This test checks that an impulse maneuver is taken into account by a Keplerian propagator
    // (the spacecraft mass is updated):
    @Test
    public void propagationWithImpulseManeuverKep() throws PatriusException {

        final double mu = 3.9860047e14;
        // ------------------------------------------------------------
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        final AbsoluteDate initDatePV = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbitPV = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDatePV, mu);

        final String name = "Satellite";
        MassProvider mass = new SimpleMassModel(500.0, name);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbitPV, new ConstantAttitudeLaw(
            FramesFactory.getEME2000(), Rotation.IDENTITY), mu, mass);
        // Get the initial state:
        SpacecraftState state0 = propagator.getInitialState();
        // Perform the propagation:
        SpacecraftState stateEnd = propagator
            .propagate(initDatePV.shiftedBy(0.5 * initialOrbitPV.getKeplerianPeriod()));
        // Check the value of the final mass as an additional state (no maneuver --> it should not have changed):
        Assert.assertEquals(state0.getMass(name), stateEnd.getMass(name), 0.0);

        // Re-run the same test, adding an impulse maneuver to the propagator:
        mass = new SimpleMassModel(800.0, name);
        propagator = new KeplerianPropagator(initialOrbitPV, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY), mu, mass);
        // Get the initial state:
        state0 = propagator.getInitialState();
        // Add an impulse maneuver to the propagator:
        final NodeDetector trigger = new NodeDetector(initialOrbitPV, FramesFactory.getGCRF(), 2);
        final double isp = 500.0;
        final double dv = 20.0;
        final ImpulseManeuver maneuver = new ImpulseManeuver(trigger, new Vector3D(dv, Vector3D.PLUS_J),
            FramesFactory.getGCRF(), isp, mass, name);
        propagator.addEventDetector(maneuver);
        // Perform the propagation:
        stateEnd = propagator.propagate(state0.getDate().shiftedBy(0.5 * initialOrbitPV.getKeplerianPeriod()));

        // Re-compute the mass decrement:
        final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        final double ratio = MathLib.exp(-dv / vExhaust);
        // Check the value of the final mass as an additional state:
        Assert.assertEquals(state0.getMass(name) * ratio, stateEnd.getMass(name), 0.0);
    }

    // This test checks that an impulse maneuver is taken into account by the propagator
    // (the spacecraft mass is updated):
    @Test
    public void propagationWithImpulseManeuverECK() throws PatriusException {

        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final String name = "Satellite";
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.002, 0.1, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), date0, 3.986004415e14);
        MassProvider mass = new SimpleMassModel(500.0, name);
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, ae, mu, orbit.getFrame(), c20, c30, c40, c50, c60, mass,
                ParametersType.OSCULATING);
        // Get the initial state:
        SpacecraftState state0 = propagator.getInitialState();
        // Perform the propagation:
        SpacecraftState stateEnd = propagator.propagate(date0.shiftedBy(0.5 * orbit.getKeplerianPeriod()));
        // Check the value of the final mass as an additional state (no maneuver --> it should not have changed):
        Assert.assertEquals(state0.getMass(name), stateEnd.getMass(name), 0.0);

        // Re-run the same test, adding an impulse maneuver to the propagator:
        mass = new SimpleMassModel(800.0, name);
        final AttitudeLaw law = new LofOffset(orbit.getFrame(), LOFType.LVLH);
        propagator = new EcksteinHechlerPropagator(orbit, law, ae, mu, orbit.getFrame(), c20, c30, c40, c50, c60, mass,
            ParametersType.OSCULATING);
        // Get the initial state:
        state0 = propagator.getInitialState();
        // Add an impulse maneuver to the propagator:
        final NodeDetector trigger = new NodeDetector(orbit, FramesFactory.getGCRF(), 2);
        final double isp = 500.0;
        final double dv = 20.0;
        final ImpulseManeuver maneuver = new ImpulseManeuver(trigger, new Vector3D(dv, Vector3D.PLUS_J),
            FramesFactory.getGCRF(), isp, mass, name);
        propagator.addEventDetector(maneuver);
        // Perform the propagation:
        stateEnd = propagator.propagate(date0.shiftedBy(0.5 * orbit.getKeplerianPeriod()));

        // Re-compute the mass decrement:
        final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        final double ratio = MathLib.exp(-dv / vExhaust);
        // Check the value of the final mass as an additional state:
        Assert.assertEquals(state0.getMass(name) * ratio, stateEnd.getMass(name), 0.0);
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the delta V computation
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria When we propagate the state before the event ({@link DateDetector}, t<=T1), the
     *                   impulse maneuver's deltaV should be {@link Vector3D#ZERO}.
     *                   When the impulse maneuver has occurred (t > T1), the deltaV vector should be deltaVSat.
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testDeltaVComputation() throws PatriusException {

        final Vector3D deltaVSat = new Vector3D(1000, 0, 0);
        final AbsoluteDate T1 = initialDate.shiftedBy(60.);
        final AbsoluteDate T2 = initialDate.shiftedBy(120.);
        maneuver = new ImpulseManeuver(new DateDetector(T1, 100, 1.e-8), deltaVSat, eme2000, 400.0,
            massModel, thruster);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, attitude, massModel);
        // Numerical propagation
        propagator.resetInitialState(initialState);
        propagator.setMassProviderEquation(massModel);
        propagator.addEventDetector(maneuver);

        // t <= T1
        propagator.getInitialState();
        Assert.assertEquals(Vector3D.ZERO, maneuver.getUsedDV());

        propagator.propagate(T1.shiftedBy(-1e-12));
        Assert.assertEquals(Vector3D.ZERO, maneuver.getUsedDV());

        propagator.propagate(T1);
        Assert.assertEquals(Vector3D.ZERO, maneuver.getUsedDV());

        // t > T1
        propagator.propagate(T1.shiftedBy(1e-12));
        Assert.assertEquals(deltaVSat, maneuver.getUsedDV());

        propagator.propagate(T2);
        Assert.assertEquals(deltaVSat, maneuver.getUsedDV());
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        eme2000 = FramesFactory.getEME2000();
        initialDate = new AbsoluteDate(new DateComponents(2005, 06, 23),
            new TimeComponents(14, 18, 37),
            TimeScalesFactory.getUTC());
        initialOrbit = new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0,
            PositionAngle.MEAN, eme2000, initialDate, 3.986004415e14);
        law = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH);
        attitude = law.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());
        final double a = initialOrbit.getA();
        final double e = initialOrbit.getE();
        final double i = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = MathLib.sqrt(mu * (1 - e) / (a * (1 + e)));
        dv = 0.99 * MathLib.tan(i) * vApo;
        massModel = new SimpleMassModel(1000., thruster);
        maneuver = new ImpulseManeuver(new NodeDetector(initialOrbit, eme2000, 2),
            new Vector3D(dv, Vector3D.PLUS_J), 400.0, massModel, thruster);
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        propagator = new NumericalPropagator(new DormandPrince853Integrator(.1, 60, absTolerance, relTolerance));
        itrf = FramesFactory.getITRF();
    }

    /**
     * @throws PatriusException
     *         Impulse maneuver creation
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

        final MassProvider massProvider = new MassModel(ForceModelsDataTest.getAssembly());
        final ImpulseManeuver impulseMan = new ImpulseManeuver(new AltitudeDetector(150000,
            new OneAxisEllipsoid(
                Constants.CNES_STELA_AE, Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getCIRF())),
            Vector3D.MINUS_I, 30, massProvider, "Tank", LOFType.LVLH);

        Assert.assertTrue(impulseMan.getLofType().equals(LOFType.LVLH));
        Assert.assertEquals(impulseMan.getMassProvider(), massProvider);
        Assert.assertFalse(impulseMan.hasFired());
    }

    @Test
    public void testCopy() throws PatriusException {
        final EventDetector detector = new DateDetector(AbsoluteDate.J2000_EPOCH);
        final MassProvider massProvider = new MassModel(ForceModelsDataTest.getAssembly());
        final double isp = 30;
        final Vector3D deltaVSat = Vector3D.MINUS_I;
        // Case frame
        final ImpulseManeuver maneuver1 = new ImpulseManeuver(detector,
            Vector3D.MINUS_I, FramesFactory.getGCRF(), isp, massProvider, "Tank").copy();
        Assert.assertEquals(maneuver1.getTrigger(), detector);
        Assert.assertEquals(maneuver1.getDeltaVSat(), deltaVSat);
        Assert.assertEquals(maneuver1.getIsp(), isp, 0.);
        Assert.assertEquals(maneuver1.getMassProvider(), massProvider);
        Assert.assertEquals(maneuver1.hasFired(), false);
        Assert.assertEquals(maneuver1.getFrame(), FramesFactory.getGCRF());
        Assert.assertEquals(maneuver1.getLofType(), null);
        // Case LOF
        final ImpulseManeuver maneuver2 = new ImpulseManeuver(detector,
            Vector3D.MINUS_I, isp, massProvider, "Tank", LOFType.LVLH).copy();
        Assert.assertEquals(maneuver2.getTrigger(), detector);
        Assert.assertEquals(maneuver2.getDeltaVSat(), deltaVSat);
        Assert.assertEquals(maneuver2.getIsp(), isp, 0.);
        Assert.assertEquals(maneuver2.getMassProvider(), massProvider);
        Assert.assertEquals(maneuver2.hasFired(), false);
        Assert.assertEquals(maneuver2.getLofType(), LOFType.LVLH);
        Assert.assertEquals(maneuver2.getFrame(), null);
        // Case satellite frame
        final ImpulseManeuver maneuver3 = new ImpulseManeuver(detector,
            Vector3D.MINUS_I, isp, massProvider, "Tank").copy();
        Assert.assertEquals(maneuver3.getTrigger(), detector);
        Assert.assertEquals(maneuver3.getDeltaVSat(), deltaVSat);
        Assert.assertEquals(maneuver3.getIsp(), isp, 0.);
        Assert.assertEquals(maneuver3.getMassProvider(), massProvider);
        Assert.assertEquals(maneuver3.hasFired(), false);
        Assert.assertEquals(maneuver3.getFrame(), null);
        Assert.assertEquals(maneuver3.getLofType(), null);
    }

    private static final String thruster = "thruster";
    private static Frame eme2000;
    private static AbsoluteDate initialDate;
    private static Orbit initialOrbit;
    private static AttitudeLaw law;
    private static Attitude attitude;
    private static double dv;
    private static MassProvider massModel;
    private static ImpulseManeuver maneuver;
    private static NumericalPropagator propagator;
    private static Frame itrf;
}
