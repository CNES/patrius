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
* VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
* VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
* VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:90:15/10/2013:Renamed Droziner to UnnormalizedDroziner
 * VERSION::DM:187:16/12/2013:Deactivated event detection in 'simple propagation' for t0 to tStart
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:364:21/11/2014:Modified contructor from state vector in order to allow null attitude provider for forces computation
 * VERSION::FA:377:08/12/2014:StepHandler initializing anomaly in propagator
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::FA:418:12/03/2015:Corrected problem with setAttitudeProvider
 * VERSION::DM:393:12/03/2015:Constant Attitude Laws
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:350:27/03/2015: Proper handling of first time-step
 * VERSION::FA:379:27/03/2015: Proper handling of first time-step
 * VERSION::FA:325:02/04/2015: problem with end date that does not match required end date
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::FA:492:06/10/2015:Propagation until final date in master mode
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:426:30/10/2015: Tests the new functionalities on orbit definition and orbit propagation
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:706:06/01/2017: synchronisation problem with the Assemby mass
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1852:05/10/201/8: move mass flow rate test in MassEquation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.ParseException;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.CunninghamAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.propagation.sampling.AdaptedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class NumericalPropagatorTest {
    private static final String DEFAULT = "DEFAULT";
    private static final String MASS = "MASS_";
    private double mu;
    private AbsoluteDate initDate;
    private Orbit orbit;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private AdaptiveStepsizeIntegrator integrator;
    private boolean gotHere;
    private MassProvider defaultMassModel;

    /**
     * This test makes sur no events are detected outside the intended propagation interval
     * 
     * @throws PatriusException
     */
    @Test
    public void testNoDetection() throws PatriusException {

        // frame
        final Frame gcrf = FramesFactory.getGCRF();

        // dates
        final AbsoluteDate d0 = new AbsoluteDate();
        final AbsoluteDate d1 = new AbsoluteDate().shiftedBy(3600);
        final AbsoluteDate d2 = new AbsoluteDate().shiftedBy(7200);

        // this date shouldnt be detected
        final AbsoluteDate dInt1 = new AbsoluteDate().shiftedBy(1500);
        final MyDateDetector detector1 = new MyDateDetector(dInt1, 10, 1);

        // this date should be detected
        final AbsoluteDate dInt2 = new AbsoluteDate().shiftedBy(4000);
        final MyDateDetector detector2 = new MyDateDetector(dInt2, 10, 1);

        // orbit
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 400e3;
        final double e = .0001;
        final double i = 60 * 3.14 / 180;
        final double raan = 0;
        final double pa = 270 * 3.14 / 180;
        final double w = 0;

        // state
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE,
            gcrf, d0, mu);
        final SpacecraftState spc = new SpacecraftState(orbit);

        // propagator
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(.1, 60, 1e-9, 1e-9);
        final NumericalPropagator prop = new NumericalPropagator(dop);

        prop.setInitialState(spc);
        prop.addEventDetector(detector1);
        prop.addEventDetector(detector2);

        // propagator
        // the propagator should perform a silent propagation from d0 to d1
        prop.propagate(d1, d2);

        // checks
        Assert.assertEquals(0, detector1.getCount());
        Assert.assertEquals(1, detector2.getCount());

        // another silent propagation from d2 to d0
        // full propagation back to d1, detector1 should detect a date
        prop.propagate(d0, d1);

        // check
        Assert.assertEquals(1, detector1.getCount());

    }

    /**
     * Custom event detector
     */
    class MyDateDetector extends DateDetector {

        private int count = 0;

        public MyDateDetector(final AbsoluteDate target, final double maxCheck,
            final double threshold) {
            super(target, maxCheck, threshold);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            this.count++;
            return super.eventOccurred(s, increasing, forward);
        }

        public int getCount() {
            return this.count;
        }

    }

    /**
     * Custom event detector. This date detector should detect two dates. The returned Action of
     * eventOccured method is entered as a parameter of the constructor. If action = Action.CONTINUE
     * : eventOccurred should be called two times.
     */
    class MyDoubleDateDetector extends DateDetector {

        private int count = 0;
        private final Action action;

        public MyDoubleDateDetector(final AbsoluteDate target1, final AbsoluteDate target2,
            final Action action, final boolean remove) {
            super(target1, 10., 10.e-10, action, remove);
            this.action = action;
            this.addEventDate(target2);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            this.count++;
            return this.action;
        }

        public int getCount() {
            return this.count;
        }
    }

    @Test
    public void testNoExtrapolation() throws PatriusException {

        // Propagate of the initial at the initial date
        final SpacecraftState finalState = this.propagator.propagate(this.initDate);

        // Initial orbit definition
        final Vector3D initialPosition = this.initialState.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = this.initialState.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition = finalState.getPVCoordinates().getPosition();
        final Vector3D finalVelocity = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assert.assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
        Assert.assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
        Assert.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);

    }

    @Test(expected = PatriusException.class)
    public void testNotInitialised() throws PatriusException {
        final NumericalPropagator notInitialised = new NumericalPropagator(
            new ClassicalRungeKuttaIntegrator(10.0));
        notInitialised.propagate(AbsoluteDate.J2000_EPOCH);
    }

    @Test
    public void testKepler() throws PatriusException {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(dt));

        // Check results
        final double n = MathLib.sqrt(this.initialState.getMu() / this.initialState.getA())
            / this.initialState.getA();
        Assert.assertEquals(this.initialState.getA(), finalState.getA(), 1.0e-10);
        Assert.assertEquals(this.initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(this.initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1.0e-10);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1.0e-10);
        Assert.assertEquals(this.initialState.getLM() + n * dt, finalState.getLM(), 2.0e-9);

        // Check null AttitudeProvider
        Assert.assertNull(this.propagator.getAttitudeProviderForces());
        Assert.assertNull(this.propagator.getAttitudeProviderEvents());
        Assert.assertNull(this.propagator.getAttitudeProvider());
    }

    @Test
    public void testCartesian() throws PatriusException {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        this.propagator.setOrbitType(OrbitType.CARTESIAN);
        final PVCoordinates finalState = this.propagator.propagate(this.initDate.shiftedBy(dt))
            .getPVCoordinates();
        final Vector3D pFin = finalState.getPosition();
        final Vector3D vFin = finalState.getVelocity();

        // Check results
        final PVCoordinates reference = this.initialState.shiftedBy(dt).getPVCoordinates();
        final Vector3D pRef = reference.getPosition();
        final Vector3D vRef = reference.getVelocity();
        Assert.assertEquals(0, pRef.subtract(pFin).getNorm(), 2e-4);
        Assert.assertEquals(0, vRef.subtract(vFin).getNorm(), 7e-8);

    }

    @Test
    public void testPropagationTypesElliptical() throws PatriusException, ParseException,
                                                IOException {

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final ForceModel gravityField = new CunninghamAttractionModel(FramesFactory.getITRF(),
            6378136.460, this.mu, provider.getC(5, 5, true), provider.getS(5, 5, true));
        this.propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final PVCoordinates pv = this.initialState.getPVCoordinates();
        final double dP = 0.001;
        final double dV = this.initialState.getMu() * dP
            / (pv.getPosition().getNormSq() * pv.getVelocity().getNorm());

        final PVCoordinates pvcM = this.propagateInType(this.initialState, dP, OrbitType.CARTESIAN,
            PositionAngle.MEAN);
        final PVCoordinates pviM = this.propagateInType(this.initialState, dP, OrbitType.CIRCULAR,
            PositionAngle.MEAN);
        final PVCoordinates pveM = this.propagateInType(this.initialState, dP, OrbitType.EQUINOCTIAL,
            PositionAngle.MEAN);
        final PVCoordinates pvkM = this.propagateInType(this.initialState, dP, OrbitType.KEPLERIAN,
            PositionAngle.MEAN);

        final PVCoordinates pvcE = this.propagateInType(this.initialState, dP, OrbitType.CARTESIAN,
            PositionAngle.ECCENTRIC);
        final PVCoordinates pviE = this.propagateInType(this.initialState, dP, OrbitType.CIRCULAR,
            PositionAngle.ECCENTRIC);
        final PVCoordinates pveE = this.propagateInType(this.initialState, dP, OrbitType.EQUINOCTIAL,
            PositionAngle.ECCENTRIC);
        final PVCoordinates pvkE = this.propagateInType(this.initialState, dP, OrbitType.KEPLERIAN,
            PositionAngle.ECCENTRIC);

        final PVCoordinates pvcT = this.propagateInType(this.initialState, dP, OrbitType.CARTESIAN,
            PositionAngle.TRUE);
        final PVCoordinates pviT = this.propagateInType(this.initialState, dP, OrbitType.CIRCULAR,
            PositionAngle.TRUE);
        final PVCoordinates pveT = this.propagateInType(this.initialState, dP, OrbitType.EQUINOCTIAL,
            PositionAngle.TRUE);
        final PVCoordinates pvkT = this.propagateInType(this.initialState, dP, OrbitType.KEPLERIAN,
            PositionAngle.TRUE);

        Assert.assertEquals(0, pvcM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 5);
        Assert.assertEquals(0, pvcM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2);
        Assert.assertEquals(0, pviM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 2);
        Assert.assertEquals(0, pviM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.7);
        Assert.assertEquals(0, pvkM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.4);
        Assert.assertEquals(0, pvkM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.09);
        Assert.assertEquals(0, pveM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.4);
        Assert.assertEquals(0, pveM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.08);

        Assert.assertEquals(0, pvcE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 5);
        Assert.assertEquals(0, pvcE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2);
        Assert.assertEquals(0, pviE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 2);
        Assert.assertEquals(0, pviE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.7);
        Assert.assertEquals(0, pvkE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvkE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.09);
        Assert.assertEquals(0, pveE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.7);
        Assert.assertEquals(0, pveE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.4);

        Assert.assertEquals(0, pvcT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 5);
        Assert.assertEquals(0, pvcT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2);
        Assert.assertEquals(0, pviT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 2);
        Assert.assertEquals(0, pviT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.7);
        Assert.assertEquals(0, pvkT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.04);
        Assert.assertEquals(0, pvkT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.03);

    }

    @Test
    public void testPropagationTypesHyperbolic() throws PatriusException, ParseException,
                                                IOException {
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(-10000000.0, 2.5, 0.3,
            0, 0, 0.0, PositionAngle.TRUE, FramesFactory.getEME2000(), this.initDate, this.mu),
            this.defaultMassModel);

        // Propagation of the initial at t + dt
        final PVCoordinates pv = state.getPVCoordinates();
        final double dP = 0.001;
        final double dV = state.getMu() * dP
            / (pv.getPosition().getNormSq() * pv.getVelocity().getNorm());

        final PVCoordinates pvcM = this.propagateInType(state, dP, OrbitType.CARTESIAN,
            PositionAngle.MEAN);
        final PVCoordinates pvkM = this.propagateInType(state, dP, OrbitType.KEPLERIAN,
            PositionAngle.MEAN);

        final PVCoordinates pvcE = this.propagateInType(state, dP, OrbitType.CARTESIAN,
            PositionAngle.ECCENTRIC);
        final PVCoordinates pvkE = this.propagateInType(state, dP, OrbitType.KEPLERIAN,
            PositionAngle.ECCENTRIC);

        final PVCoordinates pvcT = this.propagateInType(state, dP, OrbitType.CARTESIAN,
            PositionAngle.TRUE);
        final PVCoordinates pvkT = this.propagateInType(state, dP, OrbitType.KEPLERIAN,
            PositionAngle.TRUE);

        Assert.assertEquals(0, pvcM.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvcM.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.4);
        Assert.assertEquals(0, pvkM.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.09);
        Assert.assertEquals(0, pvkM.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.2);

        Assert.assertEquals(0, pvcE.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvcE.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.4);
        Assert.assertEquals(0, pvkE.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.04);
        Assert.assertEquals(0, pvkE.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.05);

        Assert.assertEquals(0, pvcT.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvcT.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.4);
    }

    private PVCoordinates propagateInType(final SpacecraftState state, final double dP,
                                          final OrbitType type, final PositionAngle angle) throws PropagationException {

        final double dt = 3200;
        final double minStep = 0.001;
        final double maxStep = 1000;

        final double[][] tol = NumericalPropagator.tolerances(dP, state.getOrbit(), type);

        this.propagator.setIntegrator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]));
        this.propagator.setOrbitType(type);
        this.propagator.setPositionAngleType(angle);
        this.propagator.setInitialState(state);
        return this.propagator.propagate(state.getDate().shiftedBy(dt)).getPVCoordinates();

    }

    @Test(expected = PatriusException.class)
    public void testException() throws PatriusException {
        this.propagator.setMasterMode(new PatriusStepHandler(){
            private static final long serialVersionUID = -6857910416285189873L;
            private int countDown = 3;
            private final AbsoluteDate previousCall = null;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public
                    void
                    handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                                throws PropagationException {
                if (this.previousCall != null) {
                    Assert.assertTrue(interpolator.getInterpolatedDate().compareTo(this.previousCall) < 0);
                }
                if (--this.countDown == 0) {
                    throw new PropagationException(PatriusMessages.SIMPLE_MESSAGE, "dummy error");
                }
            }
        });
        this.propagator.propagate(this.initDate.shiftedBy(-3600));
    }

    @Test
    public void testStopEvent() throws PatriusException {
        final AbsoluteDate stopDate = this.initDate.shiftedBy(1000);
        this.propagator.addEventDetector(new DateDetector(stopDate){
            private static final long serialVersionUID = -5024861864672841095L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                NumericalPropagatorTest.this.setGotHere(true);
                return Action.STOP;
            }

            @Override
            public SpacecraftState resetState(final SpacecraftState oldState) {
                return null;
            }
        });
        Assert.assertFalse(this.gotHere);
        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(3200));
        Assert.assertTrue(this.gotHere);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
    }

    @Test
    public void testResetStateEvent() throws PatriusException {
        final AbsoluteDate resetDate = this.initDate.shiftedBy(1000);
        this.propagator.addEventDetector(new DateDetector(resetDate){
            private static final long serialVersionUID = 6453983658076746705L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                NumericalPropagatorTest.this.setGotHere(true);
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(final SpacecraftState oldState)
                                                                             throws PatriusException {
                final double oldPartMass = oldState.getAdditionalState(MASS + DEFAULT)[0];
                final double newPartMass = oldPartMass - 200.0;
                return oldState.updateMass(DEFAULT, newPartMass);
            }
        });
        Assert.assertFalse(this.gotHere);
        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(3200));
        Assert.assertTrue(this.gotHere);
        Assert.assertEquals(this.initialState.getMass(DEFAULT) - 200, finalState.getMass(DEFAULT),
            1.0e-10);
    }

    @Test
    public void testResetDerivativesEvent() throws PatriusException {
        final AbsoluteDate resetDate = this.initDate.shiftedBy(1000);
        this.propagator.addEventDetector(new DateDetector(resetDate){
            private static final long serialVersionUID = 4217482936692909475L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                NumericalPropagatorTest.this.setGotHere(true);
                return Action.RESET_DERIVATIVES;
            }
        });
        final double dt = 3200;
        Assert.assertFalse(this.gotHere);
        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(dt));
        Assert.assertTrue(this.gotHere);
        final double n = MathLib.sqrt(this.initialState.getMu() / this.initialState.getA())
            / this.initialState.getA();
        Assert.assertEquals(this.initialState.getA(), finalState.getA(), 1.0e-10);
        Assert.assertEquals(this.initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(this.initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1.0e-10);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1.0e-10);
        Assert.assertEquals(this.initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    @Test
    public void testContinueEvent() throws PatriusException {
        final AbsoluteDate resetDate = this.initDate.shiftedBy(1000);
        this.propagator.addEventDetector(new DateDetector(resetDate){
            private static final long serialVersionUID = 5959523015368708867L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                NumericalPropagatorTest.this.setGotHere(true);
                return Action.CONTINUE;
            }
        });
        final double dt = 3200;
        Assert.assertFalse(this.gotHere);
        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(dt));
        Assert.assertTrue(this.gotHere);
        final double n = MathLib.sqrt(this.initialState.getMu() / this.initialState.getA())
            / this.initialState.getA();
        Assert.assertEquals(this.initialState.getA(), finalState.getA(), 1.0e-10);
        Assert.assertEquals(this.initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(this.initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1.0e-10);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1.0e-10);
        Assert.assertEquals(this.initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    @Test
    public void testpreviousT() throws PatriusException {
        final double dt = 1000;

        // this date should always be detected
        final AbsoluteDate d1 = this.initDate.shiftedBy(dt);

        // this date should not be detected if eventOccured returns REMOVE_DETECTOR
        // this date should be detected if eventOccured returns CONTINUE
        final AbsoluteDate d2 = this.initDate.shiftedBy(dt * 2.0);

        final MyDoubleDateDetector detectorA = new MyDoubleDateDetector(d1, d2, Action.CONTINUE,
            false);

        this.propagator.addEventDetector(detectorA);
        final double dt_propagation = 3200;
        this.propagator.propagate(this.initDate.shiftedBy(dt_propagation));
        Assert.assertEquals(2, detectorA.getCount());
    }

    @Test
    public void testRemoveDetector() throws PatriusException {
        final double dt = 1000;

        // this date should always be detected
        final AbsoluteDate d1 = this.initDate.shiftedBy(dt);

        // this date should be detected if eventOccured returns CONTINUE
        final AbsoluteDate d2 = this.initDate.shiftedBy(dt * 2.0);

        final MyDoubleDateDetector detectorA = new MyDoubleDateDetector(d1, d2, Action.CONTINUE,
            false);
        final MyDoubleDateDetector detectorB = new MyDoubleDateDetector(d1, d2, Action.CONTINUE,
            true);

        this.propagator.addEventDetector(detectorA);
        final double dt_propagation = 3200;
        this.propagator.propagate(this.initDate.shiftedBy(dt_propagation));
        Assert.assertEquals(2, detectorA.getCount());

        this.setUp();

        this.propagator.addEventDetector(detectorB);
        this.propagator.propagate(this.initDate.shiftedBy(dt_propagation));
        Assert.assertEquals(1, detectorB.getCount());
    }

    /**
     * For coverage purpose, tests the if (currentEvent.evaluateStep(interpolator) &&
     * !isLastDetection ) in method acceptStep of class AbstractIntegrator.
     * 
     * This tests check if the same events occurs twice during the current step, meaning it first
     * triggers the detector, and again in the remaining part of the step The two events should be
     * the same, therefore we use MyDoubleDateDetector, a detector with two dates to detect, and the
     * two dates must be close (less that a step = 200 between them) but no too close (at least 10
     * before each other to allow detection)
     * 
     * @throws PropagationException bcs of function propagate
     */
    @Test
    public void testDetectsTwiceInAStep() throws PropagationException {
        final double dt = 1000;

        // this date should always be detected
        final AbsoluteDate d1 = this.initDate.shiftedBy(dt);

        // this date should be detected
        final AbsoluteDate d2 = this.initDate.shiftedBy(dt + 11.0);

        final MyDoubleDateDetector detectorA = new MyDoubleDateDetector(d1, d2, Action.CONTINUE,
            false);

        this.propagator.addEventDetector(detectorA);
        final double dt_propagation = 3200;
        this.propagator.propagate(this.initDate.shiftedBy(dt_propagation));
        Assert.assertEquals(2, detectorA.getCount());

    }

    private void setGotHere(final boolean gotHere) {
        this.gotHere = gotHere;
    }

    @Test
    public void testCoverage() throws PatriusException {
        // Simple calls to several methods for code coverage
        this.propagator.resetInitialState(this.initialState);
        this.propagator.setAttitudeProvider(null);
        Assert.assertEquals(null, this.propagator.getAttitudeProvider());
        this.propagator.clearEventsDetectors();
        Assert.assertTrue(this.propagator.getEventsDetectors().isEmpty());
        this.propagator.removeForceModels();
        Assert.assertTrue(this.propagator.getForceModels().isEmpty());
        Assert.assertTrue(this.propagator.getNewtonianAttractionForceModel() != null);
        Assert.assertEquals(Propagator.SLAVE_MODE, this.propagator.getMode());
        Assert.assertTrue(this.propagator.getFrame() != null);
        Assert.assertEquals(OrbitType.EQUINOCTIAL, this.propagator.getOrbitType());
        Assert.assertEquals(PositionAngle.TRUE, this.propagator.getPositionAngleType());
        try {
            this.propagator.getGeneratedEphemeris();
        } catch (final IllegalStateException e) {
            final String expectedMessage = PatriusException.createIllegalStateException(
                PatriusMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE)
                .getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }

        // Create a clean propagator for another series of tests:
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.resetInitialState(this.initialState);
        final AttitudeProvider provider1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            new Rotation(true, 0.1, 0.1, 0.5, 0.3));
        this.propagator.setAttitudeProviderForces(provider1);
        Assert.assertEquals(provider1, this.propagator.getAttitudeProvider());
        // Create a clean propagator for another series of tests:
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.resetInitialState(this.initialState);
        this.propagator.setAttitudeProviderEvents(provider1);
        Assert.assertEquals(provider1, this.propagator.getAttitudeProvider());
        // Create a clean propagator for another series of tests:
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.resetInitialState(this.initialState);
        this.propagator.setAttitudeProvider(provider1);
        Assert.assertEquals(provider1, this.propagator.getAttitudeProvider());

        // Test getNativeFrame
        Assert.assertEquals(propagator.getFrame(), propagator.getNativeFrame(null, null));
    }

    @Test
    public void testErrorsCoverage() throws PatriusException {
        final AttitudeProvider provider1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            new Rotation(true, 0.1, 0.1, 0.5, 0.3));
        final AttitudeEquation eqsProviderForces = new AttitudeEquation(
            AttitudeType.ATTITUDE_FORCES){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };
        final AttitudeEquation eqsProviderDefault = new AttitudeEquation(AttitudeType.ATTITUDE){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };
        final AttitudeEquation eqsProviderEvents = new AttitudeEquation(
            AttitudeType.ATTITUDE_EVENTS){
            @Override
            public
                    void
                    computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                                     throws PatriusException {
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        /*
         * TEST 1 : add an attitude provider for forces computation -> A - Try to add an additional
         * equation representing the attitude for forces computation -> B - Try to add an additional
         * equation representing the attitude by default -> C - Try to add an attitude provider by
         * default
         */
        this.propagator.setAttitudeProviderForces(provider1);
        boolean testOk = false;
        // 1-A the test should fail because a force attitude provider is already defined in the
        // propagator:
        try {
            this.propagator.addAttitudeEquation(eqsProviderForces);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);
        final AttitudeProvider forcesProvider = this.propagator.getAttitudeProviderForces();
        Assert.assertNotNull(forcesProvider);

        testOk = false;
        // 1-B the test should fail because a two attitudes treatment is expected
        try {
            this.propagator.addAttitudeEquation(eqsProviderDefault);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // 1-C the test should fail because a two attitudes treatment is expected
        try {
            this.propagator.setAttitudeProvider(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST 2 : add an attitude provider for events computation -> A - Try to add an additional
         * equation representing the attitude for events computation
         */

        this.propagator.setAttitudeProviderEvents(provider1);
        testOk = false;
        // the test should fail because an events attitude provider is already defined in the
        // propagator:
        try {
            this.propagator.addAttitudeEquation(eqsProviderEvents);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);
        final AttitudeProvider eventsProvider = this.propagator.getAttitudeProviderEvents();
        Assert.assertNotNull(eventsProvider);

        // Create a clean propagator for another series of exception tests:
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.resetInitialState(this.initialState);

        /*
         * TEST 3 : add an attitude provider by default -> A - Try to add an additional equation
         * representing the attitude by default -> B - Try to add an attitude provider for forces
         * computation
         */

        this.propagator.setAttitudeProvider(provider1);
        testOk = false;
        // the test should fail because an attitude provider by default is already defined in the
        // propagator:
        try {
            this.propagator.addAttitudeEquation(eqsProviderDefault);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // the test should fail because an attitude provider by default is already defined in the
        // propagator:
        try {
            this.propagator.setAttitudeProviderForces(forcesProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Create a clean propagator for another series of exception tests:
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.resetInitialState(this.initialState);

        /*
         * TEST 4 : add additional equation representing the attitude for forces computation -> A -
         * Try to add an additional equation representing the attitude for forces computation -> B -
         * Try to add a default attitude provider
         */
        this.propagator.addAttitudeEquation(eqsProviderForces);

        testOk = false;
        // the test should fail because the force attitude equation is already defined in the
        // propagator:
        try {
            this.propagator.setAttitudeProviderForces(forcesProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // the test should fail because the force attitude equation is already defined in the
        // propagator:
        try {
            this.propagator.setAttitudeProvider(forcesProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST 5 : add additional equation representing the attitude for events computation -> A -
         * Try to add an additional equation representing the attitude for events computation
         */
        this.propagator.addAttitudeEquation(eqsProviderEvents);
        testOk = false;
        // the test should fail because the events attitude equation is already defined in the
        // propagator:
        try {
            this.propagator.setAttitudeProviderEvents(eventsProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Create a clean propagator for another series of exception tests:
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.resetInitialState(this.initialState);

        /*
         * TEST 6 : add additional equation representing the attitude by default -> A - Try to add
         * an attitude provider representing the attitude for forces computation -> B - Try to add
         * an attitude provider representing the attitude for events computation -> C - Try to add
         * an attitude provider representing the attitude by default -> D - Try to add an additional
         * equation representing the attitude for forces computation -> E - Try to add an additional
         * equation representing the attitude for events computation
         */
        this.propagator.addAttitudeEquation(eqsProviderDefault);
        testOk = false;
        // A - the test should fail because a single attitude treatment is expected:
        try {
            this.propagator.setAttitudeProviderForces(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // B - the test should fail because a single attitude treatment is expected:
        try {
            this.propagator.setAttitudeProviderEvents(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        // C - the test should fail because an additional equation representing the attitude by
        // default is already
        // defined
        try {
            this.propagator.setAttitudeProvider(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        // D - the test should fail because a single attitude treatment is expected
        try {
            this.propagator.addAttitudeEquation(eqsProviderForces);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        // E - the test should fail because a single attitude treatment is expected
        try {
            this.propagator.addAttitudeEquation(eqsProviderEvents);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST X : Other tests
         */
        // Create a clean propagator for another series of exception tests:
        this.propagator = new NumericalPropagator(this.integrator);
        testOk = false;
        // the test should fail because the initial state has not been defined:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION
                .getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        this.initialState = this.initialState.addAdditionalState("New State", new double[] { 0.0, 0.0 });
        this.propagator.resetInitialState(this.initialState);
        testOk = false;
        // the test should fail because the additional states or the additional equations are empty:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);
        this.propagator.addAdditionalEquations(eqsProviderForces);
        testOk = false;
        // the test should fail because the additional states number does not correspond to the
        // additional equations
        // number:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);
        this.propagator.addAdditionalEquations(new AdditionalEquations(){
            @Override
            public String getName() {
                return "New Equation";
            }

            @Override
            public
                    void
                    computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                                     throws PatriusException {
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        });
        this.propagator.setMassProviderEquation(this.defaultMassModel);
        testOk = false;
        // the test should fail because the additional states names do not correspond to the
        // additional equations names:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(
                PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    @Test
    public void testAddStates() throws PatriusException {
        final String aeqName = "bogus";
        final double[] aeqState = { 0., 0. };
        final AdditionalEquations aeq = new AdditionalEquations(){

            /** UID. */
            private static final long serialVersionUID = 5323096745041968839L;

            @Override
            public String getName() {
                return aeqName;
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {

                final double[] pDot = new double[2];
                pDot[0] = 0.1;
                pDot[1] = 0.2;

                adder.addAdditionalStateDerivative(aeqName, pDot);
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        final double[] aeqState2 = { 0., 0., 0. };
        final AdditionalEquations aeq2 = new AdditionalEquations(){

            /** UID. */
            private static final long serialVersionUID = 5323096745041968840L;

            @Override
            public String getName() {
                return aeqName + "2";
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {

                final double[] pDot = new double[3];
                pDot[0] = 0.1;
                pDot[1] = 0.2;
                pDot[2] = -0.2;
                adder.addAdditionalStateDerivative(aeqName + "2", pDot);
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        // Add the additional state:
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), aeqState);
        this.initialState = this.initialState.addAdditionalState(aeq2.getName(), aeqState2);
        // Add the equations:
        this.propagator.addAdditionalEquations(aeq2);
        this.propagator.addAdditionalEquations(aeq);
        final double[] absTol = { 0.1, 0.1 };
        final double[] relTol = { 0.01, 0.01 };
        // Add the tolerances:
        this.propagator.setAdditionalStateTolerance(aeq.getName(), absTol, relTol);
        // Reinitialize the initial state with the additional states:
        this.propagator.resetInitialState(this.initialState);
        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(1000.));
        // See the states were properly propagated:
        final double[] aeqRez = finalState.getAdditionalState(aeqName);
        final double[] aeqRez2 = finalState.getAdditionalState(aeqName + "2");
        Assert.assertEquals(0.1 * 1000., aeqRez[0], 1e-9);
        Assert.assertEquals(0.2 * 1000., aeqRez[1], 1e-9);
        Assert.assertEquals(0.1 * 1000., aeqRez2[0], 1e-9);
        Assert.assertEquals(0.2 * 1000., aeqRez2[1], 1e-9);
        Assert.assertEquals(-0.2 * 1000., aeqRez2[2], 1e-9);
    }

    @Test
    public void testAddStatesEvents() throws PatriusException {
        final String aeqName = "bogus";
        final double[] aeqState = { 0., 0. };
        final AdditionalEquations aeq = new AdditionalEquations(){
            /** UID. */
            private static final long serialVersionUID = 5323096745041968839L;

            @Override
            public String getName() {
                return aeqName;
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                final double[] pDot = new double[2];
                pDot[0] = 0.1;
                pDot[1] = 0.2;
                adder.addAdditionalStateDerivative(aeqName, pDot);
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        final double[] aeqState2 = { 0., 0., 0. };
        final AdditionalEquations aeq2 = new AdditionalEquations(){
            /** UID. */
            private static final long serialVersionUID = 5323096745041968840L;

            @Override
            public String getName() {
                return aeqName + "2";
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                final double[] pDot = new double[3];
                pDot[0] = 0.1;
                pDot[1] = 0.2;
                pDot[2] = -0.2;
                adder.addAdditionalStateDerivative(aeqName + "2", pDot);
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        // Events
        final EventDetector bogusTicker = new EventDetector(){

            /** {@inheritDoc} */
            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // Nothing
            }

            /** {@inheritDoc} */
            @Override
            public double getThreshold() {
                return 1e-9;
            }

            /** {@inheritDoc} */
            @Override
            public int getSlopeSelection() {
                return 2;
            }

            /** {@inheritDoc} */
            @Override
            public int getMaxIterationCount() {
                return 100;
            }

            /** {@inheritDoc} */
            @Override
            public double getMaxCheckInterval() {
                return 1.;
            }

            /** {@inheritDoc} */
            @Override
            public double g(final SpacecraftState s) throws PatriusException {
                // Look for the "bogus" data
                final Map<String, double[]> lsd = s.getAdditionalStates();
                final double[] data = lsd.get("bogus");
                if (data == null) {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
                final double bog0 = data[0];
                // final double bog1 = data[1];
                return MathLib.sin(bog0 * FastMath.PI);
            }

            /** {@inheritDoc} */
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                // Look for the "bogus" data
                final Map<String, double[]> lsd = s.getAdditionalStates();
                final double[] data = lsd.get("bogus");
                if (data == null) {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
                // System.out.println("At "+s.getDate()+", bog0 is " + bog0);
                return Action.CONTINUE;
            }

            /** {@inheritDoc} */
            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public SpacecraftState resetState(final SpacecraftState oldState)
                                                                             throws PatriusException {
                return oldState;
            }

            @Override
            public EventDetector copy() {
                return null;
            }
        };

        // Add the additional state:
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), aeqState);
        this.initialState = this.initialState.addAdditionalState(aeq2.getName(), aeqState2);
        // Add the equations:
        this.propagator.addAdditionalEquations(aeq2);
        this.propagator.addAdditionalEquations(aeq);
        final double[] absTol = { 0.1, 0.1 };
        final double[] relTol = { 0.01, 0.01 };
        // Add the tolerances:
        this.propagator.setAdditionalStateTolerance(aeq.getName(), absTol, relTol);
        // Add events
        this.propagator.addEventDetector(bogusTicker);
        // Reinitialize the initial state with the additional states:
        this.propagator.resetInitialState(this.initialState);

        final SpacecraftState finalState = this.propagator.propagate(this.initDate.shiftedBy(1000.));
        // See the states were properly propagated:
        final double[] aeqRez = finalState.getAdditionalState(aeqName);
        final double[] aeqRez2 = finalState.getAdditionalState(aeqName + "2");
        Assert.assertEquals(0.1 * 1000., aeqRez[0], 1e-9);
        Assert.assertEquals(0.2 * 1000., aeqRez[1], 1e-9);
        Assert.assertEquals(0.1 * 1000., aeqRez2[0], 1e-9);
        Assert.assertEquals(0.2 * 1000., aeqRez2[1], 1e-9);
        Assert.assertEquals(-0.2 * 1000., aeqRez2[2], 1e-9);
    }

    @Test
    public void testAddStatesExceptions() throws PatriusException {
        final String aeqName = "bogus";
        final double[] aeqState = { 0., 0. };
        final AdditionalEquations aeq = new AdditionalEquations(){

            /** UID. */
            private static final long serialVersionUID = 5323096745041968839L;

            @Override
            public String getName() {
                return aeqName;
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                // does nothing.
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        // Add the equation
        this.propagator.addAdditionalEquations(aeq);

        // After this, tolerances may be set

        // Exception : tolerance arrays too big
        final double[] okay = { 1., 1. };
        final double[] tooBig = { 1., 1., 1. };
        try {
            final SpacecraftState currentState = this.initialState.addAdditionalState(aeqName, aeqState);
            this.propagator.resetInitialState(currentState);
            this.propagator.setAdditionalStateTolerance(aeqName, okay, tooBig);
            this.propagator.propagate(currentState.getDate().shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            final String expectedMessage = new PatriusException(
                PatriusMessages.ADDITIONAL_STATE_WRONG_TOLERANCES_SIZE).getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }
        // Exception : tolerance arrays too small
        final double[] tooSmall = { 1. };
        try {
            final SpacecraftState currentState = this.initialState.addAdditionalState(aeqName, aeqState);
            this.propagator.resetInitialState(currentState);
            this.propagator.setAdditionalStateTolerance(aeqName, tooSmall, okay);
            this.propagator.propagate(currentState.getDate().shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            final String expectedMessage = new PatriusException(
                PatriusMessages.ADDITIONAL_STATE_WRONG_TOLERANCES_SIZE).getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }
        // Exception : wrong name for additional state
        try {
            this.propagator.setAdditionalStateTolerance("ploc", new double[] { 0.1, 0., }, okay);
            Assert.fail();
        } catch (final PatriusException e) {
            final String expectedMessage = new PatriusException(
                PatriusMessages.UNKNOWN_ADDITIONAL_EQUATION, "ploc").getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }
    }

    @Test
    public void testEventDetectionBug() throws PatriusException, IOException, ParseException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 1, 1, 0, 0, 0.0, utc);
        double duration = 100000.;
        AbsoluteDate endDate = new AbsoluteDate(initialDate, duration);

        // Initialization of the frame EME2000
        final Frame EME2000 = FramesFactory.getEME2000();

        // Initial orbit
        final double a = 35786000. + 6378137.0;
        final double e = 0.70;
        final double rApogee = a * (1 + e);
        final double vApogee = MathLib.sqrt(this.mu * (1 - e) / (a * (1 + e)));
        final Orbit geo = new CartesianOrbit(new PVCoordinates(new Vector3D(rApogee, 0., 0.),
            new Vector3D(0., vApogee, 0.)), EME2000, initialDate, this.mu);

        duration = geo.getKeplerianPeriod();
        endDate = new AbsoluteDate(initialDate, duration);

        // Numerical Integration
        final double minStep = 0.001;
        final double maxStep = 1000;
        final double initStep = 60;
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };

        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep,
            maxStep, absTolerance, relTolerance);
        integrator.setInitialStepSize(initStep);

        // Numerical propagator based on the integrator
        this.propagator = new NumericalPropagator(integrator);
        final MassProvider mass = new SimpleMassModel(1000., DEFAULT);
        final SpacecraftState iState = new SpacecraftState(geo, mass);
        this.propagator.setInitialState(iState);
        this.propagator.setMassProviderEquation(mass);
        this.propagator.setOrbitType(OrbitType.CARTESIAN);

        // Set the events Detectors
        final ApsideDetector event1 = new ApsideDetector(geo, 2);
        this.propagator.addEventDetector(event1);

        // Set the propagation mode
        this.propagator.setSlaveMode();

        // Propagate
        final SpacecraftState finalState = this.propagator.propagate(endDate);

        // we should stop long before endDate
        Assert.assertTrue(endDate.durationFrom(finalState.getDate()) > 40000.0);
    }

    @Test
    public void testEphemerisGenerationIssue14() throws PatriusException, IOException {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        this.propagator.getInitialState();

        this.propagator.setOrbitType(OrbitType.CARTESIAN);
        this.propagator.setEphemerisMode();
        this.propagator.propagate(this.initDate.shiftedBy(dt));
        final BoundedPropagator ephemeris1 = this.propagator.getGeneratedEphemeris();
        Assert.assertEquals(this.initDate, ephemeris1.getMinDate());
        Assert.assertEquals(this.initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        this.propagator.getPVCoordinates(this.initDate.shiftedBy(2 * dt), FramesFactory.getEME2000());
        this.propagator.getPVCoordinates(this.initDate.shiftedBy(-2 * dt), FramesFactory.getEME2000());

        // the new propagations should not have changed ephemeris1
        Assert.assertEquals(this.initDate, ephemeris1.getMinDate());
        Assert.assertEquals(this.initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        final BoundedPropagator ephemeris2 = this.propagator.getGeneratedEphemeris();
        Assert.assertEquals(this.initDate.shiftedBy(-2 * dt), ephemeris2.getMinDate());
        Assert.assertEquals(this.initDate.shiftedBy(2 * dt), ephemeris2.getMaxDate());

        // generating ephemeris2 should not have changed ephemeris1
        Assert.assertEquals(this.initDate, ephemeris1.getMinDate());
        Assert.assertEquals(this.initDate.shiftedBy(dt), ephemeris1.getMaxDate());

    }

    @Test
    public void testRetropolation() throws PropagationException {

        this.propagator.setMasterMode(60., new PatriusFixedStepHandler(){

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {

            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                // System.out.println(currentState.getDate().offsetFrom(initDate,
                // TimeScalesFactory.getTAI()));
            }

        });

        final myDetector dateDet = new myDetector(this.initDate.shiftedBy(200));
        this.propagator.addEventDetector(dateDet);

        final SpacecraftState state0 = this.propagator.getInitialState();
        // extrapolation => t0 + 400
        final SpacecraftState state1 = this.propagator.propagate(this.initDate.shiftedBy(400.));

        // retropolation => t0
        // we should have the same position/velocity as in state0
        final SpacecraftState state2 = this.propagator.propagate(this.initDate);
        Vector3D res = state2.getPVCoordinates().getPosition()
            .subtract(state0.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        // we propagate again until t0+400
        // we should have the same position/velocity as in state1
        final SpacecraftState state3 = this.propagator.propagate(this.initDate.shiftedBy(400.));
        res = state3.getPVCoordinates().getPosition()
            .subtract(state1.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        // we propagate again until t0
        // we should have the same position/velocity as in state1
        final SpacecraftState state4 = this.propagator.propagate(this.initDate);
        res = state4.getPVCoordinates().getPosition()
            .subtract(state2.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        // we propagate again until t0+400
        // we should have the same position/velocity as in state1
        final SpacecraftState state5 = this.propagator.propagate(this.initDate.shiftedBy(400.));
        res = state5.getPVCoordinates().getPosition()
            .subtract(state3.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        Assert.assertEquals(5, dateDet.getCount());
    }

    // TU for AdaptedStepHandler class; check the initialization of the OrekitStepHandler (Master
    // mode
    // propagation) is automatically executed by the AdaptedStepHandler.
    @Test
    public void testAdaptedStepHandler() throws PatriusException {
        final double duration = 100.;
        final AttitudeEquation eqsProviderForces = new AttitudeEquation(
            AttitudeType.ATTITUDE_FORCES){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };
        this.propagator.addAttitudeEquation(eqsProviderForces);
        final PatriusStepHandler handler = new PatriusStepHandler(){
            AbsoluteDate date;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                this.date = s0.getDate();
                // Check the s0 parameters correspond to the propagation initial state:
                // Compare the date:
                Assert.assertEquals(duration, t.durationFrom(this.date), 0.0);
                // Compare the orbital parameters:
                Assert.assertEquals(NumericalPropagatorTest.this.initialState.getA(), s0.getA(), 0.0);
                Assert.assertEquals(NumericalPropagatorTest.this.initialState.getE(), s0.getE(), 0.0);
                Assert.assertEquals(NumericalPropagatorTest.this.initialState.getI(), s0.getI(), 0.0);
                Assert.assertEquals(NumericalPropagatorTest.this.initialState.getLv(), s0.getLv(), 0.0);
                try {
                    // Compare the attitude:
                    Assert.assertEquals(NumericalPropagatorTest.this.initialState.getAttitude().getRotation()
                        .getQuaternion(),
                        s0.getAttitude().getRotation().getQuaternion());
                    // Compare the additional states (mass):
                    Assert.assertEquals(
                        NumericalPropagatorTest.this.initialState.getAdditionalState(MASS + DEFAULT)[0],
                        s0.getAdditionalState(MASS + DEFAULT)[0], 0.0);
                } catch (final PatriusException e) {
                    Assert.fail();
                }
            }

            @Override
            public
                    void
                    handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                                throws PropagationException {
                // Check the date during propagation:
                Assert.assertTrue(interpolator.getInterpolatedDate().durationFrom(this.date) <= duration);
            }
        };
        this.propagator.setMasterMode(handler);
        final AbsoluteDate finalDate = this.initDate.shiftedBy(duration);
        this.propagator.propagate(finalDate);
        final AdaptedStepHandler shandler = (AdaptedStepHandler) this.integrator.getStepHandlers().toArray()[0];

        Assert.assertEquals(finalDate, shandler.getInterpolatedDate());
        Assert.assertTrue(shandler.isForward());
    }

    // Coverage test for OrekitStepNormalizer class
    @Test
    public void testOrekitStepNormalizer() throws PatriusException {
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
            }

        };
        final PatriusStepNormalizer norm = new PatriusStepNormalizer(1.0, handler);
        Assert.assertTrue(norm.requiresDenseOutput());
    }

    // Coverage test AdditionalStateInfo classes
    @Test
    public void testAdditionalStateCoverage() throws PatriusException {
        final AdditionalStateInfo info = new AdditionalStateInfo(3, 2);
        final AdditionalStateInfo infoClone = info.clone();
        Assert.assertNotSame(infoClone, info);
    }

    // Coverage test for propagate method
    @Test
    public void testPropagateExceptions() throws PatriusException {

        // Cover the throw new
        // PropagationException(PatriusMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION)
        this.propagator.setIntegrator(null);
        boolean rez = false;
        try {
            this.propagator.propagate(this.initDate.shiftedBy(200.0));
        } catch (final PropagationException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    // Test the resizeArray method when the state variables tolerances are represented by a scalar
    // value:
    @Test
    public void testTolerances() throws PatriusException, IOException, ParseException {

        final double abstolScal = 1.0e-1;
        final double reltolScal = 1.0e-2;
        final FirstOrderIntegrator integratorScal = new DormandPrince853Integrator(0.001, 200,
            abstolScal, reltolScal);

        final double[] abstolVec = { 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1 };
        final double[] reltolVec = { 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2 };
        final FirstOrderIntegrator integratorVec = new DormandPrince853Integrator(0.001, 200, abstolVec,
            reltolVec);

        final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(
            Utils.ae, 1.0 / 298.257222101, FramesFactory.getITRF()), 0.0004, 42000.0, 7500.0);
        // Test getSpeedOfSound method from SimpleExponentialAtmosphere
        Assert.assertEquals(MathLib.sqrt(1.4 * 7500.0 * 9.81), atm.getSpeedOfSound(this.initDate,
            this.initialState.getPVCoordinates().getPosition(), this.initialState.getFrame()), 0.0);

        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 0., 0.,
            0., DEFAULT);
        final DragForce drag = new DragForce(atm, spacecraft);

        // First propagation: scalar tolerance:
        this.propagator = new NumericalPropagator(integratorScal);
        this.propagator.setInitialState(this.initialState);
        this.propagator.setMassProviderEquation(this.defaultMassModel);
        this.propagator.addForceModel(drag);
        final double[] absT = { 5.0e-6 };
        final double[] relT = { 5.0e-7 };
        this.propagator.setAdditionalStateTolerance("MASS_DEFAULT", absT, relT);
        final SpacecraftState res1 = this.propagator.propagate(this.initDate.shiftedBy(1E5));

        // Second propagation: vector tolerance (scalar tolerance x 6):
        this.propagator = new NumericalPropagator(integratorVec);
        this.propagator.setInitialState(this.initialState);
        this.propagator.addForceModel(drag);
        this.propagator.setMassProviderEquation(this.defaultMassModel);
        this.propagator.setAdditionalStateTolerance("MASS_DEFAULT", absT, relT);
        final SpacecraftState res2 = this.propagator.propagate(this.initDate.shiftedBy(1E5));

        // Check the results of the two propagations are the same:
        Assert.assertEquals(res1.getA(), res2.getA(), 0.0);
        Assert.assertEquals(res1.getE(), res2.getE(), 0.0);
        Assert.assertEquals(res1.getLv(), res2.getLv(), 0.0);
    }

    // Test the resizeArray method when the state variables tolerances are represented by a scalar
    // value:
    // Test without additional states added to the state
    // Test for coverage purpose.
    @Test
    public void testTolerances2() throws PatriusException, IOException, ParseException {

        final double abstolScal = 1.0e-1;
        final double reltolScal = 1.0e-2;
        final FirstOrderIntegrator integratorScal = new DormandPrince853Integrator(0.001, 200,
            abstolScal, reltolScal);

        final double[] abstolVec = { 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1 };
        final double[] reltolVec = { 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2 };
        final FirstOrderIntegrator integratorVec = new DormandPrince853Integrator(0.001, 200, abstolVec,
            reltolVec);

        final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(
            Utils.ae, 1.0 / 298.257222101, FramesFactory.getITRF()), 0.0004, 42000.0, 7500.0);
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 0., 0.,
            0., DEFAULT);
        new DragForce(atm, spacecraft);

        // First propagation: scalar tolerance:
        this.propagator = new NumericalPropagator(integratorScal);
        this.propagator.setInitialState(new SpacecraftState(this.orbit));
        final SpacecraftState res1 = this.propagator.propagate(this.initDate.shiftedBy(1E5));

        // Second propagation: vector tolerance (scalar tolerance x 6):
        this.propagator = new NumericalPropagator(integratorVec);
        this.propagator.setInitialState(new SpacecraftState(this.orbit));
        final SpacecraftState res2 = this.propagator.propagate(this.initDate.shiftedBy(1E5));

        // Check the results of the two propagations are the same:
        Assert.assertEquals(res1.getA(), res2.getA(), 0.0);
        Assert.assertEquals(res1.getE(), res2.getE(), 0.0);
        Assert.assertEquals(res1.getLv(), res2.getLv(), 0.0);
    }

    /*
     * FA 418 : It should be possible to define a new AttitudeProvider to the propagator after a
     * first propagation.
     */
    @Test
    public void testSingleAttitudeProviderTreatment() throws PatriusException {
        // Set an attitude provider
        final AttitudeProvider provider1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY);
        this.propagator.setAttitudeProvider(provider1);
        // First propagation
        this.propagator.propagate(this.initDate.shiftedBy(10.));
        Assert.assertEquals(provider1.hashCode(), this.propagator.getAttitudeProvider().hashCode());
        // Change the single attitude provider defined
        final AttitudeProvider provider2 = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        this.propagator.setAttitudeProvider(provider2);
        // Second propagation
        this.propagator.propagate(this.initDate.shiftedBy(20.));
        Assert.assertEquals(provider2.hashCode(), this.propagator.getAttitudeProvider().hashCode());
    }

    /**
     * FA350 and FA379: propagation must properly stops during first integration step.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testFirstSmallStep() throws PatriusException, IOException, ParseException {
        // Initialization
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final NumericalPropagator propagator = new NumericalPropagator(
            new ClassicalRungeKuttaIntegrator(60));
        propagator.setInitialState(initialState);

        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(40)){
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                return Action.RESET_STATE;
            }
        });

        // Mode
        propagator.setSlaveMode();

        // Propagation
        final SpacecraftState res = propagator.propagate(initDate.shiftedBy(10));

        Assert.assertEquals(10, res.getDate().durationFrom(initDate), 0);
    }

    /**
     * FA325: propagation must stop exactly at required date in slave mode.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPropagationDurationSlaveMode() throws PatriusException, IOException,
                                                  ParseException {
        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0,
            TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1,
            TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), initialDate, 1.0);
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final NumericalPropagator propagator = new NumericalPropagator(ode);
        propagator.setInitialState(new SpacecraftState(orbit));

        // Propagation
        final SpacecraftState finalState = propagator.propagate(finalDate);

        // Check
        Assert.assertEquals(0, finalState.getDate().durationFrom(finalDate), 0);
    }

    /**
     * FA492: propagation must stop exactly at required date in master mode.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPropagationDurationMasterMode() throws PatriusException, IOException,
                                                   ParseException {
        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0,
            TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1,
            TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), initialDate, 1.0);
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final NumericalPropagator propagator = new NumericalPropagator(ode);
        propagator.setMasterMode(0.1, new PatriusFixedStepHandler(){

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                // Check state is correct (only anomaly is modified during this simple Keplerian
                // propagation)
                final double actual = currentState.getLv();
                final double expected = currentState.getDate().durationFrom(initialDate);
                Assert.assertEquals(expected, actual, 1E-12);
            }
        });
        propagator.setInitialState(new SpacecraftState(orbit));

        // =============================================== FORWARD
        // =============================================== //

        // Propagation
        final SpacecraftState finalState = propagator.propagate(finalDate);

        // Check final date
        Assert.assertEquals(0, finalState.getDate().durationFrom(finalDate), 0);

        // =============================================== BACKWARD
        // =============================================== //

        // Propagation
        final SpacecraftState finalStateb = propagator.propagate(initialDate);

        // Check final date
        Assert.assertEquals(0, finalStateb.getDate().durationFrom(initialDate), 0);
    }

    /**
     * FA476: propagation must stop exactly at required date in ephemeris mode.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPropagationDurationEphemerisMode() throws PatriusException, IOException,
                                                      ParseException {
        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0,
            TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1,
            TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), initialDate, 1.0);
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final NumericalPropagator propagator = new NumericalPropagator(ode);
        propagator.setEphemerisMode();
        propagator.setInitialState(new SpacecraftState(orbit));

        // =============================================== FORWARD
        // =============================================== //

        // Propagation
        final SpacecraftState finalState = propagator.propagate(finalDate);

        // Propagation with generated ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        final SpacecraftState finalState2 = ephemeris.propagate(finalDate);

        // Check (final date and bulletin)
        Assert.assertEquals(0, finalState.getDate().durationFrom(finalDate), 0);
        Assert.assertEquals(0, finalState2.getDate().durationFrom(finalDate), 0);

        final PVCoordinates coord1 = finalState.getPVCoordinates();
        final PVCoordinates coord2 = finalState2.getPVCoordinates();
        final PVCoordinates coord3 = ephemeris.propagate(finalDate).getPVCoordinates();
        Assert.assertEquals(coord1.getPosition().getX(), coord2.getPosition().getX(), 0);
        Assert.assertEquals(coord1.getPosition().getY(), coord2.getPosition().getY(), 0);
        Assert.assertEquals(coord1.getPosition().getZ(), coord2.getPosition().getZ(), 0);
        Assert.assertEquals(coord1.getVelocity().getX(), coord2.getVelocity().getX(), 0);
        Assert.assertEquals(coord1.getVelocity().getY(), coord2.getVelocity().getY(), 0);
        Assert.assertEquals(coord1.getVelocity().getZ(), coord2.getVelocity().getZ(), 0);
        Assert.assertEquals(coord1.getPosition().getX(), coord3.getPosition().getX(), 0);
        Assert.assertEquals(coord1.getPosition().getY(), coord3.getPosition().getY(), 0);
        Assert.assertEquals(coord1.getPosition().getZ(), coord3.getPosition().getZ(), 0);
        Assert.assertEquals(coord1.getVelocity().getX(), coord3.getVelocity().getX(), 0);
        Assert.assertEquals(coord1.getVelocity().getY(), coord3.getVelocity().getY(), 0);
        Assert.assertEquals(coord1.getVelocity().getZ(), coord3.getVelocity().getZ(), 0);

        // =============================================== BACKWARD
        // =============================================== //

        // Propagation
        final SpacecraftState finalStateb = propagator.propagate(initialDate);

        // Propagation with generated ephemeris
        final BoundedPropagator ephemerisb = propagator.getGeneratedEphemeris();
        final SpacecraftState finalStateb2 = ephemerisb.propagate(initialDate);

        // Check (final date and bulletin)
        Assert.assertEquals(0, finalStateb.getDate().durationFrom(initialDate), 0);
        Assert.assertEquals(0, finalStateb2.getDate().durationFrom(initialDate), 0);

        final PVCoordinates coordb1 = finalStateb.getPVCoordinates();
        final PVCoordinates coordb2 = finalStateb2.getPVCoordinates();
        final PVCoordinates coordb3 = ephemerisb.propagate(initialDate).getPVCoordinates();
        Assert.assertEquals(coordb1.getPosition().getX(), coordb2.getPosition().getX(), 0);
        Assert.assertEquals(coordb1.getPosition().getY(), coordb2.getPosition().getY(), 0);
        Assert.assertEquals(coordb1.getPosition().getZ(), coordb2.getPosition().getZ(), 0);
        Assert.assertEquals(coordb1.getVelocity().getX(), coordb2.getVelocity().getX(), 0);
        Assert.assertEquals(coordb1.getVelocity().getY(), coordb2.getVelocity().getY(), 0);
        Assert.assertEquals(coordb1.getVelocity().getZ(), coordb2.getVelocity().getZ(), 0);
        Assert.assertEquals(coordb1.getPosition().getX(), coordb3.getPosition().getX(), 0);
        Assert.assertEquals(coordb1.getPosition().getY(), coordb3.getPosition().getY(), 0);
        Assert.assertEquals(coordb1.getPosition().getZ(), coordb3.getPosition().getZ(), 0);
        Assert.assertEquals(coordb1.getVelocity().getX(), coordb3.getVelocity().getX(), 0);
        Assert.assertEquals(coordb1.getVelocity().getY(), coordb3.getVelocity().getY(), 0);
        Assert.assertEquals(coordb1.getVelocity().getZ(), coordb3.getVelocity().getZ(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NumericalPropagator#setOrbitFrame(Frame)}
     * 
     * @description This test aims at verifying that an exception is risen if a non pseudo-inertial
     *              or non inertial frame is provided for propagation
     * 
     * @input NumericalPropagator
     * @input Frame provided for propagation : TIRF
     * 
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testSetNonInertialFrame() throws PatriusException {

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getITRF(), initDate, Constants.EGM96_EARTH_MU);

        // Propagator
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(.1, 60, 1e-9, 1e-9);
        final NumericalPropagator prop = new NumericalPropagator(dop);

        prop.setInitialState(new SpacecraftState(orbit));
        final Frame from = FramesFactory.getTIRF();

        // An exception should occur here !
        prop.setOrbitFrame(from);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * 
     * @description This test aims at verifying that an exception is risen if the non
     *              pseudo-inertial or non inertial orbit's frame is used for propagation
     * 
     * @input KeplerianOrbit(8.0E6, 0.0, 0.0, 0.0, 0.0, 0.0)
     * @input NumericalPropagator
     * @input Frame ITRF is used to define the orbit and then for propagation
     * 
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testPropagateWithNonInertialFrame() throws PatriusException {

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getITRF(), initDate, Constants.EGM96_EARTH_MU);
        final AbsoluteDate finalDate = orbit.getDate().shiftedBy(1000.0);

        // Propagator
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(.1, 60, 1e-9, 1e-9);
        final NumericalPropagator prop = new NumericalPropagator(dop);
        prop.setInitialState(new SpacecraftState(orbit));

        prop.propagate(finalDate);
    }

    /**
     * @throws PatriusException
     * @testType VT
     * 
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * 
     * @description This test is up to ensure that the propagation of an orbit in a different frame
     *              from the one in which the orbit is defined (not necessary inertial or
     *              pseudo-inertial) provide the same final state.
     * 
     * @input EquinoctialOrbit(9756940.912430497, 0.162193534007533, 0.1424058727481423,
     *        0.26409746136329015, 0.030288649473182046, 1.554396336772627) in ITRF
     * @input NumericalPropagator
     * @input Frame ITRF is used to define the orbit propagation is done in frame EME2000
     * 
     * @output The SpacecraftState final state of propagation
     * @testPassCriteria The orbital elements of the output orbit must be the same as the one
     *                   expected
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void TestKeplerInITRF() throws PatriusException {

        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(
            FramesFactory.getEME2000(), Rotation.IDENTITY);

        // Initial state : orbit is defined in frame TIRF
        final Frame orbitFrame = FramesFactory.getTIRF();
        final Orbit initialOrbit = new EquinoctialOrbit(9756940.912430497, 0.162193534007533,
            0.1424058727481423, 0.26409746136329015, 0.030288649473182046, 1.554396336772627,
            PositionAngle.TRUE, orbitFrame, this.initDate, this.mu);
        final Attitude attitude = attitudeProvider.getAttitude(initialOrbit);
        final OrbitType type = initialOrbit.getType();

        // Propagator definition
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator dop = new DormandPrince853Integrator(0.001, 200,
            absTolerance, relTolerance);
        dop.setInitialStepSize(60);
        final NumericalPropagator prop = new NumericalPropagator(dop);
        Assert.assertTrue(prop.getFrame() == null);
        prop.setInitialState(new SpacecraftState(initialOrbit, attitude));
        Assert.assertTrue(prop.getFrame() == null);
        // Set orbit frame to EME2000
        final Frame propFrame = FramesFactory.getEME2000();
        prop.setOrbitFrame(propFrame);
        Assert.assertTrue(prop.getFrame() == propFrame);

        // Set attitude
        prop.setAttitudeProvider(attitudeProvider);

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final SpacecraftState finalState = prop.propagate(this.initDate.shiftedBy(dt));

        // ============================ Check final state after propagation
        // ==================================//

        // Compute the converted initial orbit in Frame EME2000 in order to allow comparisons
        final Orbit convInitialOrbit = type.convertOrbit(initialOrbit, propFrame);

        final double n = MathLib.sqrt(convInitialOrbit.getMu() / convInitialOrbit.getA())
            / convInitialOrbit.getA();
        Assert.assertEquals(convInitialOrbit.getA(), finalState.getA(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit.getEquinoctialEx(), finalState.getEquinoctialEx(),
            1.0e-10);
        Assert.assertEquals(convInitialOrbit.getEquinoctialEy(), finalState.getEquinoctialEy(),
            1.0e-10);
        Assert.assertEquals(convInitialOrbit.getHx(), finalState.getHx(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit.getHy(), finalState.getHy(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit.getLM() + n * dt, finalState.getLM(), 2.0e-9);

        // Check attitudes
        final Attitude finalAttitude = prop.propagate(this.initDate).getAttitude()
            .withReferenceFrame(orbitFrame);
        Assert.assertEquals(attitude.getRotation().getAngle(), finalAttitude.getRotation()
            .getAngle(), 0.);
        Assert.assertEquals(attitude.getRotation().getAxis().getX(), finalAttitude.getRotation()
            .getAxis().getX(), 0.);
        Assert.assertEquals(attitude.getRotation().getAxis().getY(), finalAttitude.getRotation()
            .getAxis().getY(), 0.);
        Assert.assertEquals(attitude.getRotation().getAxis().getZ(), finalAttitude.getRotation()
            .getAxis().getZ(), 0.);
    }

    @Test
    public void testNumberOfStepHandlers() throws PatriusException {

        // Initial state
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final AbsoluteDate initDate = new AbsoluteDate(2015, 05, 01, 0, 0, 0.,
            TimeScalesFactory.getTAI());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, mu);
        final MassProvider massModel = new SimpleMassModel(1000, "Satellite");
        final SpacecraftState initialState = new SpacecraftState(new CartesianOrbit(orbit),
            massModel);

        // Propagator
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200,
            1E-7, 1E-12);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setAttitudeProvider(new BodyCenterPointing(FramesFactory.getGCRF()));
        propagator.setInitialState(initialState);

        final PatriusStepHandler handler = new PatriusStepHandler(){

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public
                    void
                    handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                                throws PropagationException {
            }
        };

        // 0 step handlers before propagation
        Assert.assertEquals(0, integrator.getStepHandlers().size());

        propagator.setInitialState(initialState);
        propagator.setMassProviderEquation(massModel);

        // 1st propagation (slave mode)
        propagator.setSlaveMode();
        propagator.propagate(initDate.shiftedBy(50.));
        // Mass equations + slave mode : 1 step handler
        Assert.assertEquals(1, integrator.getStepHandlers().size());

        // 2nd propagation (master mode)
        propagator.setMasterMode(handler);
        propagator.propagate(initDate.shiftedBy(100.));
        // Mass equations + master mode : 2 step handlers
        Assert.assertEquals(2, integrator.getStepHandlers().size());

        // 3rd propagation (master mode)
        propagator.propagate(initDate.shiftedBy(100.));
        // Mass equations + master mode : still 2 step handlers
        Assert.assertEquals(2, integrator.getStepHandlers().size());
    }

    /**
     * FA-1852: propagation with positive derivatives is not allowed because flow rate check is
     * in the wrong class.
     * 
     * @testType UT
     * 
     * @testedFeature NA
     * 
     * @description check that propagation with additional equations is allowed event with positive derivatives
     * 
     * @testPassCriteria propagation does not throw exception.
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testAdditionalEquationsPropagation() throws PatriusException {

        // Initialization
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        SpacecraftState state = new SpacecraftState(orbit);
        state = state.addAdditionalState("MASS_TMP", new double[] { 0. });

        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator.setInitialState(state);

        // Add additional equations
        final AdditionalEquations equations = new AdditionalEquations(){

            @Override
            public String getName() {
                return "MASS_TMP";
            }

            @Override
            public
                    void
                    computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                                     throws PatriusException {
                // Positive derivative: should be allowed
                adder.addAdditionalStateDerivative("MASS_TMP", new double[] { 1. });
            }

            /** {@inheritDoc} */
            @Override
            public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
                return new double[] { 0. };
            }

            /** {@inheritDoc} */
            @Override
            public int getFirstOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public int getSecondOrderDimension() {
                // Unused
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double[] buildAdditionalState(final double[] y,
                    final double[] yDot) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractY(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double[] extractYDot(final double[] additionalState) {
                // Unused
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            /** {@inheritDoc} */
            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };
        propagator.addAdditionalEquations(equations);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(120.));
        // Propagation should end without error
        Assert.assertNotNull(finalState);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.mu = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        this.initDate = AbsoluteDate.J2000_EPOCH;
        this.orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.initDate, this.mu);
        this.defaultMassModel = new SimpleMassModel(1000., DEFAULT);
        final Attitude attitude = new LofOffset(this.orbit.getFrame(), LOFType.LVLH).getAttitude(this.orbit,
            this.orbit.getDate(), this.orbit.getFrame());
        this.initialState = new SpacecraftState(this.orbit, attitude, this.defaultMassModel);
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        this.integrator = new DormandPrince853Integrator(0.001, 200, absTolerance, relTolerance);
        this.integrator.setInitialStepSize(60);
        this.propagator = new NumericalPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState);
        this.propagator.setMassProviderEquation(this.defaultMassModel);
        final double[] absT = { 0.01 };
        final double[] relT = { 1.0e-7 };
        this.propagator.setAdditionalStateTolerance("MASS_DEFAULT", absT, relT);
        this.gotHere = false;
    }

    @After
    public void tearDown() throws PatriusException {
        this.initDate = null;
        this.initialState = null;
        this.propagator = null;
        this.gotHere = false;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    class myDetector extends DateDetector {

        private int count = 0;

        public myDetector(final AbsoluteDate target) {
            super(target);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            this.count++;
            return Action.CONTINUE;
        }

        private int getCount() {
            return this.count;
        }

    }

}
