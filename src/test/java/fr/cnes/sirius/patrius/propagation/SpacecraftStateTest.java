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
* VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] L'attitude des spacecraft state devrait etre initialisee de maniere lazy
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2091:15/05/2019:[PATRIUS] optimisation du SpacecraftState
* VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:86:24/10/2013:New constructors
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:386:05/12/2014: index mutualisation for ephemeris interpolation
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::FA:390:19/02/2015: added addAttitude method for AbstractEphemeris needs
 * VERSION::DM:290:04/03/2015: added toTransform methods
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:449:10/08/2015:Added error if attitudeForces == null and attitudeEvents != null
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:449:18/12/2015:Add coverage tests due to changes in attitude handling
 * VERSION::DM:654:04/08/2016:Add getAttitude(Frame) and getAttitude(LofType)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpacecraftStateTest {

    /**
     * Tests that the method hashCode that has to be implemented if equals is redefined.
     *
     * @throws PatriusException
     *         bcs of spacecraftstate
     */
    @Test
    public void testHashCode() throws PatriusException {
        // SpacecraftState parameters
        final Orbit orb = new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        final Attitude attForces = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF(),
            AngularCoordinates.IDENTITY);
        final Rotation rot = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Attitude attEvents = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF(), rot, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put("state1", new double[] { 1. });

        final SpacecraftState state1 = new SpacecraftState(orb, this.massModel);
        final SpacecraftState state2 = new SpacecraftState(orb, attForces, this.massModel);
        final SpacecraftState state3 = new SpacecraftState(orb, attForces, attEvents, this.massModel, addStates);
        final SpacecraftState state4 = new SpacecraftState(orb, attForces, attEvents, this.massModel);

        // Test getStateVectorSize()
        Assert.assertEquals(7, state1.getStateVectorSize());
        Assert.assertEquals(7, state2.getStateVectorSize());
        Assert.assertEquals(8, state3.getStateVectorSize());
        Assert.assertEquals(7, state4.getStateVectorSize());

        Assert.assertFalse(state1.hashCode() == state2.hashCode());
        Assert.assertFalse(state1.hashCode() == state3.hashCode());
        Assert.assertFalse(state1.hashCode() == state4.hashCode());
        Assert.assertFalse(state2.hashCode() == state3.hashCode());
        Assert.assertFalse(state2.hashCode() == state4.hashCode());
        Assert.assertFalse(state3.hashCode() == state4.hashCode());
    }

    @Test(expected = PatriusException.class)
    public void testNullMassProvider() throws PatriusException {

        // Orbit definition
        final Orbit orb = new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);

        // SpacecraftState constructors
        final SpacecraftState state = new SpacecraftState(orb);
        state.getMass(DEFAULT);
    }

    @Test
    public void testShiftError() throws PatriusException {

        // Create new propagator with two attitude providers
        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(this.orbit, this.attitudeLaw, this.attitudeLaw, ae,
                mu, FramesFactory.getCIRF(), c20, c30, c40, c50, c60,
                this.massModel, ParametersType.OSCULATING);
        propagator.addAdditionalStateProvider(this.massModel);

        // polynomial models for interpolation error in position, velocity and attitude
        // these models grow as follows
        // interpolation time (s) position error (m) velocity error (m/s) attitude error (°)
        // 60 20 1 0.00007
        // 120 100 2 0.00025
        // 300 600 4 0.00125
        // 600 2000 6 0.0028
        // 900 4000 6 0.0075
        // the expected maximal residuals with respect to these models are about 4m, 4cm/s and 7.0e-5°
        final PolynomialFunction pModel = new PolynomialFunction(new double[] {
            1.0861222899572454, -0.09715781161843842, 0.007738813180913936, -3.273915351103342E-6
        });
        final PolynomialFunction vModel = new PolynomialFunction(new double[] {
            -0.02580749589147073, 0.015669287539738435, -1.0221727893509467E-5, 4.903886053117456E-10
        });
        final PolynomialFunction aModel = new PolynomialFunction(new double[] {
            2.367656161750781E-5, -9.04040437097894E-7, 2.7648633804186084E-8, -3.862811467792131E-11,
            -3.465934294894873E-15, 2.7789684889607137E-17
        });

        final AbsoluteDate centerDate = this.orbit.getDate().shiftedBy(100.0);
        final SpacecraftState centerState = propagator.propagate(centerDate);
        double maxResidualP = 0;
        double maxResidualV = 0;
        double maxResidualA = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            final SpacecraftState shifted = centerState.shiftedBy(dt);
            final SpacecraftState propagated = propagator.propagate(centerDate.shiftedBy(dt));
            final PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), shifted.getPVCoordinates());
            final double residualP = pModel.value(dt) - dpv.getPosition().getNorm();
            final double residualV = vModel.value(dt) - dpv.getVelocity().getNorm();
            final double residualA = aModel.value(dt) -
                MathLib.toDegrees(Rotation.distance(shifted.getAttitude().getRotation(),
                    propagated.getAttitude().getRotation()));
            maxResidualP = MathLib.max(maxResidualP, MathLib.abs(residualP));
            maxResidualV = MathLib.max(maxResidualV, MathLib.abs(residualV));
            maxResidualA = MathLib.max(maxResidualA, MathLib.abs(residualA));
        }
        Assert.assertEquals(4.0, maxResidualP, 0.2);
        Assert.assertEquals(0.04, maxResidualV, 0.01);
        Assert.assertEquals(7.0e-5, maxResidualA, 0.6e-2);
    }

    /*
     * Test shifted by method for coverage purpose
     */
    @Test
    public void testShifttedBy() throws PatriusException {
        final Attitude attEvents =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        SpacecraftState initialState = new SpacecraftState(this.orbit, attEvents, attEvents);
        initialState = initialState.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS);
        final SpacecraftState finalState = initialState.shiftedBy(100.);
        // Check final attitude value
        final double[] attAddState = finalState.getAdditionalState("ATTITUDE_EVENTS");
        final Attitude attitude = finalState.getAttitudeEvents();
        Assert.assertEquals(attAddState[0], attitude.getRotation().getQi()[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(attAddState[1], attitude.getRotation().getQi()[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(attAddState[2], attitude.getRotation().getQi()[2], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(attAddState[3], attitude.getRotation().getQi()[3], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(attAddState[4], attitude.getSpin().getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(attAddState[5], attitude.getSpin().getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(attAddState[6], attitude.getSpin().getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    // @Test
    public void testInterpolation() throws PatriusException {
        this.checkInterpolationError(2, 5162.2580, 1.47722511, 1.527841945E-4, 0.0);
        this.checkInterpolationError(3, 650.5940, 0.62788726, 9.9038452739E-7, 0.0);
        this.checkInterpolationError(4, 259.3868, 0.11878960, 5.94773345E-9, 0.0);
        this.checkInterpolationError(5, 29.5445, 0.02278694, 0.48e-9, 0.0);
        this.checkInterpolationError(6, 6.7633, 0.00336356, 0.09e-9, 0.0);
        this.checkInterpolationError(9, 0.0082, 0.00000577, 1.49e-9, 0.0);
        this.checkInterpolationError(10, 0.0011, 0.00000058, 5.61e-9, 0.0);
    }

    private void
            checkInterpolationError(final int n, final double expectedErrorP, final double expectedErrorV,
                                    final double expectedErrorA, final double expectedErrorM)
                                                                                             throws PatriusException {
        final AbsoluteDate centerDate = this.orbit.getDate().shiftedBy(100.0);
        final SpacecraftState centerState = this.propagator.propagate(centerDate);
        final List<SpacecraftState> sample = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            sample.add(this.propagator.propagate(centerDate.shiftedBy(i * 900.0 / (n - 1))));
        }
        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            final SpacecraftState interpolated = centerState.interpolate(centerDate.shiftedBy(dt), sample);
            final SpacecraftState propagated = this.propagator.propagate(centerDate.shiftedBy(dt));
            final PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = MathLib.max(maxErrorP, dpv.getPosition().getNorm());
            maxErrorV = MathLib.max(maxErrorV, dpv.getVelocity().getNorm());
            maxErrorA = MathLib.max(maxErrorA,
                MathLib.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation(),
                    propagated.getAttitude().getRotation())));
            final double interpolatedMassState = interpolated.getMass(DEFAULT);
            final double propagatedMassState = propagated.getMass(DEFAULT);
            maxErrorM = MathLib.max(maxErrorM, MathLib.abs(interpolatedMassState - propagatedMassState));
        }
        Assert.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assert.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assert.assertEquals(expectedErrorA, maxErrorA, 2.0e-9);
        Assert.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudesConsistency() throws PatriusException {
        // Test if attitudeForces == null and attitudeEvents!= null.
        final Attitude attEvents =
            this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame());
        new SpacecraftState(this.orbit, null, attEvents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeForcesDatesConsistency() throws PatriusException {
        // Test dates consistency between orbit and attitude for forces computation
        final Attitude attForces =
            this.attitudeLaw.getAttitude(this.orbit.shiftedBy(10.0), this.orbit.getDate().shiftedBy(10.0),
                this.orbit.getFrame());
        final Attitude attEvents =
            this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame());
        new SpacecraftState(this.orbit, attForces, attEvents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeEventsDatesConsistency() throws PatriusException {
        // Test dates consistency between orbit and attitude for events computation
        final Attitude attForces =
            this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame());
        final Attitude attEvents =
            this.attitudeLaw.getAttitude(this.orbit.shiftedBy(10.0), this.orbit.getDate().shiftedBy(10.0),
                this.orbit.getFrame());
        new SpacecraftState(this.orbit, attForces, attEvents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeForcesFramesConsistency() {
        // Test frames consistency between orbit and attitude for forces computation
        final Attitude attForces = new Attitude(this.orbit.getDate(), FramesFactory.getGCRF(), Rotation.IDENTITY,
            Vector3D.ZERO);
        final Attitude attEvents = new Attitude(this.orbit.getDate(), FramesFactory.getEME2000(), Rotation.IDENTITY,
            Vector3D.ZERO);
        new SpacecraftState(this.orbit, attForces, attEvents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeEventsFramesConsistency() {
        // Test frames consistency between orbit and attitude for events computation
        final Attitude attForces = new Attitude(this.orbit.getDate(), FramesFactory.getEME2000(), Rotation.IDENTITY,
            Vector3D.ZERO);
        final Attitude attEvents = new Attitude(this.orbit.getDate(), FramesFactory.getGCRF(), Rotation.IDENTITY,
            Vector3D.ZERO);
        new SpacecraftState(this.orbit, attForces, attEvents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeForcesAndAdditionalStatesConsistency() {
        // Test frames consistency between orbit and attitude for events computation
        final Attitude attForces =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Attitude attEvents =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE_FORCES.toString(), new double[] { 1.0, 1.0, 1.0, 1.0 });
        new SpacecraftState(this.orbit, attForces, attEvents, addStates);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeEventsAndAdditionalStatesConsistency() {
        // Test frames consistency between orbit and attitude for events computation
        final Attitude attForces =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Attitude attEvents =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE_EVENTS.toString(), new double[] { 1.0, 1.0, 1.0, 1.0 });
        new SpacecraftState(this.orbit, attForces, attEvents, addStates);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeAndAdditionalStatesConsistency() {
        // Test attitude additional state consistency
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE_EVENTS.toString(), new double[] { 1.0, 1.0, 1.0, 1.0 });
        addStates.put(AttitudeType.ATTITUDE.toString(), new double[] { 1.0, 1.0, 1.0, 1.0 });
        new SpacecraftState(this.orbit, null, null, addStates);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeAndAdditionalStatesConsistency2() {
        // Test attitude additional state consistency
        final Map<String, double[]> addStates = new HashMap<>();
        final Attitude att =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        addStates.put(AttitudeType.ATTITUDE.toString(), new double[] { 1.0, 1.0, 1.0, 1.0 });
        new SpacecraftState(this.orbit, att, null, addStates);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeAndAdditionalStatesConsistency3() {
        // Test attitude additional state consistency
        final Map<String, double[]> addStates = new HashMap<>();
        new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        addStates.put(AttitudeType.ATTITUDE.toString(), new double[] { 1.0, 1.0, 1.0, 1.0 });
        new SpacecraftState(this.orbit, null, null, addStates);
    }

    @Test(expected = PatriusException.class)
    public void testAddAdditionalStateError() throws PatriusException {
        final Attitude attForces =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Attitude attEvents =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final SpacecraftState sstate = new SpacecraftState(this.orbit, attForces, attEvents);
        sstate.addAdditionalState(AttitudeType.ATTITUDE_EVENTS.toString(), new double[] { 0.1, 0.1 });
    }

    // single attitude treatment expected
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError1() throws PatriusException {
        final Attitude att =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE.toString(), att.mapAttitudeToArray());
        final SpacecraftState sstate = new SpacecraftState(this.orbit, att, null, addStates);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_FORCES);
    }

    // single attitude treatment expected
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError2() throws PatriusException {
        final Attitude att =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE.toString(), att.mapAttitudeToArray());
        final SpacecraftState sstate = new SpacecraftState(this.orbit, att, null, addStates);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS);
    }

    // two attitudes treatment expected
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError3() throws PatriusException {
        final Attitude att =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE_FORCES.toString(), att.mapAttitudeToArray());
        final SpacecraftState sstate = new SpacecraftState(this.orbit, att, null, addStates);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE);
    }

    // two attitudes treatment expected
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError4() throws PatriusException {
        final Attitude att =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final Map<String, double[]> addStates = new HashMap<>();
        addStates.put(AttitudeType.ATTITUDE_FORCES.toString(), att.mapAttitudeToArray());
        final SpacecraftState sstate = new SpacecraftState(this.orbit, att, att, addStates);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE);
    }

    // no attitude for forces computation defined
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError5() throws PatriusException {
        new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final SpacecraftState sstate = new SpacecraftState(this.orbit);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_FORCES);
    }

    // no attitude for events computation defined
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError6() throws PatriusException {
        new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final SpacecraftState sstate = new SpacecraftState(this.orbit);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS);
    }

    // no attitude defined
    @Test(expected = PatriusException.class)
    public void testaddAttitudeToAdditionalStatesError7() throws PatriusException {
        final SpacecraftState sstate = new SpacecraftState(this.orbit);
        sstate.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE);
    }

    @Test(expected = PatriusException.class)
    public void testGetAdditionalStateError() throws PatriusException {
        final SpacecraftState sstate = new SpacecraftState(this.orbit);
        sstate.getAdditionalState("NO_ADD_STATE");
    }

    @Test(expected = PatriusException.class)
    public void testInterpolateError() throws PatriusException {
        final Attitude attitude =
            new Attitude(this.orbit.getDate(), this.orbit.getFrame(), Rotation.IDENTITY, Vector3D.ZERO);
        final SpacecraftState sstate = new SpacecraftState(this.orbit, attitude);
        final SpacecraftState sstate1 = new SpacecraftState(this.orbit, attitude, this.massModel);
        final SpacecraftState sstate2 = new SpacecraftState(this.orbit, attitude);
        final List<SpacecraftState> sample = new ArrayList<>();
        sample.add(sstate1);
        sample.add(sstate2);
        sstate.interpolate(this.orbit.getDate().shiftedBy(100.), sample);
    }

    @Test(expected = PatriusException.class)
    public void testUpdateMassError() throws PatriusException {
        final SpacecraftState sstate = new SpacecraftState(this.orbit, this.massModel);
        sstate.updateMass("Sensor", 0.8);
    }

    /**
     * Test transformation from orbit/attitude inertial frame to spacecraft frame.
     * Tested methods : toTransform(), toTransformForces(), toTransformEvents()
     */
    @Test
    public void testTransformInertialToSpacecraftFrame() throws PatriusException {
        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < this.orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = this.propagator.propagate(this.orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransformForces().getInverse();
            final PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            final PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates());
            final Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            final double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates().getPosition());
            maxDP = MathLib.max(maxDP, dPV.getPosition().getNorm());
            maxDV = MathLib.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = MathLib.max(maxDA, MathLib.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);

        maxDP = 0;
        maxDV = 0;
        maxDA = 0;

        // Initial state with two attitudes
        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        this.propagator =
            new EcksteinHechlerPropagator(this.orbit, this.attitudeLaw, this.attitudeLaw, ae, mu,
                this.orbit.getFrame(),
                c20, c30, c40, c50, c60, ParametersType.OSCULATING);

        for (double t = 0; t < this.orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = this.propagator.propagate(this.orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransformEvents().getInverse();
            final PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            final PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates());
            final Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            final double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates().getPosition());
            maxDP = MathLib.max(maxDP, dPV.getPosition().getNorm());
            maxDV = MathLib.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = MathLib.max(maxDA, MathLib.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);

    }

    /**
     * Coverage test for transformations methods.
     * Tested methods : toTransformForces(), an exception should be raised since no attitude is defined
     *
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public void testTransformForcesException() throws PatriusException {
        final SpacecraftState state = new SpacecraftState(this.orbit);
        state.toTransformForces();
    }

    /**
     * Coverage test for transformations methods.
     * Tested methods : toTransformEvents(), an exception should be raised since no attitude is defined
     *
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public void testTransformEventsException() throws PatriusException {
        final SpacecraftState state = new SpacecraftState(this.orbit);
        state.toTransformEvents();
    }

    /**
     * Test transformation from specified frame to spacecraft frame.
     * Tested methods : toTransform(Frame), toTransformForces(Frame), toTransformEvents(Frame)
     */
    @Test
    public void testTransformFrameToSpacecraftFrame() throws PatriusException {
        final Frame frame = FramesFactory.getGCRF();
        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < this.orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = this.propagator.propagate(this.orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransformForces(frame).getInverse();
            final PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            final PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates(frame));
            final Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            final double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates(frame).getPosition());
            maxDP = MathLib.max(maxDP, dPV.getPosition().getNorm());
            maxDV = MathLib.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = MathLib.max(maxDA, MathLib.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-5);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);

        maxDP = 0;
        maxDV = 0;
        maxDA = 0;
        // Initial state with two attitudes
        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        this.propagator =
            new EcksteinHechlerPropagator(this.orbit, this.attitudeLaw, this.attitudeLaw, ae, mu,
                this.orbit.getFrame(),
                c20, c30, c40, c50, c60, ParametersType.OSCULATING);

        for (double t = 0; t < this.orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = this.propagator.propagate(this.orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransformEvents(frame).getInverse();
            final PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            final PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates(frame));
            final Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            final double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates(frame).getPosition());
            maxDP = MathLib.max(maxDP, dPV.getPosition().getNorm());
            maxDV = MathLib.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = MathLib.max(maxDA, MathLib.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);
    }

    /**
     * Test transformation from orbit/attitude inertial frame to local orbital frame.
     * Tested method : toTransform(LOFType)
     */
    @Test
    public void testTransformInertialToLOF() throws PatriusException {
        final LOFType lofType = LOFType.LVLH;
        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < this.orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = this.propagator.propagate(this.orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransform(lofType).getInverse();
            final PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            final PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates());
            final Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            final double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates().getPosition());
            maxDP = MathLib.max(maxDP, dPV.getPosition().getNorm());
            maxDV = MathLib.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = MathLib.max(maxDA, MathLib.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);
    }

    /**
     * Test transformation from specified frame to local orbital frame.
     * Tested method : toTransform(Frame, LOFType)
     */
    @Test
    public void testTransformFrameToLOF() throws PatriusException {
        final LOFType lofType = LOFType.LVLH;
        final Frame frame = FramesFactory.getGCRF();
        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < this.orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = this.propagator.propagate(this.orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransform(frame, lofType).getInverse();
            final PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            final PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates(frame));
            final Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            final double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates(frame).getPosition());
            maxDP = MathLib.max(maxDP, dPV.getPosition().getNorm());
            maxDV = MathLib.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = MathLib.max(maxDA, MathLib.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);
    }

    /**
     *
     * @testType UT
     *
     * @testedMethod {@link SpacecraftState#getAttitude(Frame)}
     * @testedMethod {@link SpacecraftState#getAttitudeForces(Frame)}
     * @testedMethod {@link SpacecraftState#getAttitudeEvents(Frame)}
     *
     * @description This test verifies that the computation of the spacecraft's attitude
     *              in given output frame is correct : expected rotation, spin and acceleration are computed and
     *              compared to the obtained ones.
     *
     * @input a spacecraftState defined by an orbit in EME2000 and two attitudes :
     *        an attitude forces, an attitude event (same than forces).
     *
     * @output the attitude in GCRF.
     *
     * @testPassCriteria rotation, spin and acceleration must be the one expected.
     *
     * @referenceVersion 3.3
     *
     * @nonRegressionVersion 3.3
     * @throws PatriusException
     */
    @Test
    public void testGetAttitudeInFrame() throws PatriusException {

        // Orbit defined in EME2000, define the spacecraft fixed wrt this frame
        final AbsoluteDate date = this.orbit.getDate();
        final Frame eme2000 = this.orbit.getFrame();
        final Attitude attEvents = new Attitude(date, eme2000, Rotation.IDENTITY, Vector3D.ZERO);
        final SpacecraftState initialState = new SpacecraftState(this.orbit, attEvents, attEvents);

        // Null attitude events for coverage purpose
        final Attitude attEventsNull = null;
        final SpacecraftState initialStateNullEvents = new SpacecraftState(this.orbit, attEvents, attEventsNull);

        // Get the attitude in GCRF
        final Frame gcrf = FramesFactory.getGCRF();

        /** Expected attitude. */

        // Transform from inertial frame EME200 into GCRF
        final Transform transform = eme2000.getTransformTo(gcrf, date, true).getInverse(true);

        final Rotation expectedRot = transform.getRotation();
        final Vector3D expectedRate = transform.getRotationRate();
        final Vector3D expectedAcc = transform.getRotationAcceleration();

        /** Actual computed attitude. */
        final Attitude actualAtt = initialState.getAttitude(gcrf);
        final Attitude actualAttForces = initialState.getAttitudeForces(gcrf);
        final Attitude actualAttEvents = initialState.getAttitudeEvents(gcrf);
        final Attitude actualAttNullEvents = initialStateNullEvents.getAttitudeEvents(gcrf);
        final Rotation actualRot = actualAtt.getRotation();
        final Rotation actualRotForces = actualAttForces.getRotation();
        final Rotation actualRotEvents = actualAttEvents.getRotation();
        final Rotation actualRotNullEvents = actualAttNullEvents.getRotation();

        final Vector3D actualRate = actualAtt.getSpin();
        final Vector3D actualRateForces = actualAttForces.getSpin();
        final Vector3D actualRateEvents = actualAttEvents.getSpin();
        final Vector3D actualRateNullEvents = actualAttNullEvents.getSpin();
        final Vector3D actualAcc = actualAtt.getRotationAcceleration();
        final Vector3D actualAccForces = actualAttForces.getRotationAcceleration();
        final Vector3D actualAccEvents = actualAttEvents.getRotationAcceleration();
        final Vector3D actualAccNullEvents = actualAttNullEvents.getRotationAcceleration();

        /** Comparison : same results with attitude forces or events. */
        final double eps = Precision.EPSILON;
        Assert.assertTrue(expectedRot.isEqualTo(actualRot, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRate).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAcc).getNorm(), 0., eps);

        Assert.assertTrue(expectedRot.isEqualTo(actualRotForces, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRateForces).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAccForces).getNorm(), 0., eps);

        Assert.assertTrue(expectedRot.isEqualTo(actualRotEvents, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRateEvents).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAccEvents).getNorm(), 0., eps);

        Assert.assertTrue(expectedRot.isEqualTo(actualRotNullEvents, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRateNullEvents).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAccNullEvents).getNorm(), 0., eps);
    }

    /**
     *
     * @testType UT
     *
     * @testedMethod {@link SpacecraftState#getAttitude(LOFType)}
     * @testedMethod {@link SpacecraftState#getAttitudeForces(LOFType)}
     * @testedMethod {@link SpacecraftState#getAttitudeEvents(LOFType)}
     *
     * @description This test verifies that the attitude conversion from the
     *              orbit's frame to a local orbital frame is well led : comparison between
     *              the computed attitude (rotation, spin and acceleration) and the one
     *              expected is performed.
     *
     * @input a spacecraftState defined by an orbit in EME2000 and two attitudes :
     *        an attitude forces, an attitude event (same than forces).
     *
     * @output the attitude in local orbital frame TNW.
     *
     * @testPassCriteria rotation, spin and acceleration must be the one expected.
     *
     * @referenceVersion 3.3
     *
     * @nonRegressionVersion 3.3
     * @throws PatriusException
     */
    @Test
    public void testGetAttitudeInLOF() throws PatriusException {

        // Orbit defined in EME2000, define the spacecraft fixed wrt this frame
        final AbsoluteDate date = this.orbit.getDate();
        final Frame eme2000 = this.orbit.getFrame();
        final Attitude attEvents = new Attitude(date, eme2000, Rotation.IDENTITY, Vector3D.ZERO);
        final SpacecraftState initialState = new SpacecraftState(this.orbit, attEvents, attEvents);

        // Null attitude events for coverage purpose
        final Attitude attEventsNull = null;
        final SpacecraftState initialStateNullEvents = new SpacecraftState(this.orbit, attEvents, attEventsNull);

        // Get the attitude in TNW local orbital frame
        final LOFType lofType = LOFType.TNW;

        /** Expected attitude. */

        // Transform from inertial frame EME2000 to local orbital frame
        final Transform transformToLof = lofType.transformFromInertial(date, this.orbit.getPVCoordinates(), true)
            .getInverse(true);

        final Rotation expectedRot = transformToLof.getRotation();
        final Vector3D expectedRate = transformToLof.getRotationRate();
        final Vector3D expectedAcc = transformToLof.getRotationAcceleration();

        /** Actual computed attitude. */
        final Attitude actualAtt = initialState.getAttitude(lofType);
        final Attitude actualAttForces = initialState.getAttitudeForces(lofType);
        final Attitude actualAttEvents = initialState.getAttitudeEvents(lofType);
        final Attitude actualAttNullEvents = initialStateNullEvents.getAttitudeEvents(lofType);
        final Rotation actualRot = actualAtt.getRotation();
        final Rotation actualRotForces = actualAttForces.getRotation();
        final Rotation actualRotEvents = actualAttEvents.getRotation();
        final Rotation actualRotNullEvents = actualAttNullEvents.getRotation();

        final Vector3D actualRate = actualAtt.getSpin();
        final Vector3D actualRateForces = actualAttForces.getSpin();
        final Vector3D actualRateEvents = actualAttEvents.getSpin();
        final Vector3D actualRateNullEvents = actualAttNullEvents.getSpin();
        final Vector3D actualAcc = actualAtt.getRotationAcceleration();
        final Vector3D actualAccForces = actualAttForces.getRotationAcceleration();
        final Vector3D actualAccEvents = actualAttEvents.getRotationAcceleration();
        final Vector3D actualAccNullEvents = actualAttNullEvents.getRotationAcceleration();

        /** Comparison : same results with attitude forces or events. */
        final double eps = Precision.EPSILON;
        Assert.assertTrue(expectedRot.isEqualTo(actualRot, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRate).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAcc).getNorm(), 0., eps);

        Assert.assertTrue(expectedRot.isEqualTo(actualRotForces, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRateForces).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAccForces).getNorm(), 0., eps);

        Assert.assertTrue(expectedRot.isEqualTo(actualRotEvents, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRateEvents).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAccEvents).getNorm(), 0., eps);

        Assert.assertTrue(expectedRot.isEqualTo(actualRotNullEvents, eps, eps));
        Assert.assertEquals(expectedRate.subtract(actualRateNullEvents).getNorm(), 0., eps);
        Assert.assertEquals(expectedAcc.subtract(actualAccNullEvents).getNorm(), 0., eps);
    }

    @Test
    public void testGetAdditionalStates() {
        final Orbit orb = new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        final Map<String, double[]> addStates = new HashMap<>();
        // SpacecraftState constructors
        final SpacecraftState state = new SpacecraftState(orb, null, null, addStates);
        final double hashCode = state.getAdditionalStates().hashCode();
        final double hashCode2 = state.getAdditionalStates().hashCode();
        Assert.assertNotSame(hashCode, hashCode2);
    }

    @Test
    public void testGetAdditionalStatesMass() {
        final Orbit orb = new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        // SpacecraftState constructors
        final SpacecraftState state = new SpacecraftState(orb, this.massModel);
        final Map<String, double[]> addStates = state.getAdditionalStatesMass();
        Assert.assertEquals(this.massModel.getAllPartsNames().size(), addStates.size());
    }

    @Test
    public void testGetAdditionalStatesInfos() {
        final Orbit orb = new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        final Map<String, double[]> addStates = new HashMap<>();
        // SpacecraftState constructors
        final SpacecraftState state = new SpacecraftState(orb, null, null, addStates);
        final double hashCode = state.getAdditionalStatesInfos().hashCode();
        final double hashCode2 = state.getAdditionalStatesInfos().hashCode();
        Assert.assertNotSame(hashCode, hashCode2);
    }

    @Test
    public void testEqualsAddStates() {

        // Not equals case (different size)
        final Map<String, double[]> state1 = new TreeMap<>();
        final Map<String, double[]> state2 = new TreeMap<>();
        state1.put("STATE1", new double[] { 1. });
        state1.put("STATE2", new double[] { 2. });
        state2.put("STATE1", new double[] { 2. });
        Assert.assertFalse(SpacecraftState.equalsAddStates(state1, state2));

        // Not equals case
        state2.put("STATE3", new double[] { 2. });
        Assert.assertFalse(SpacecraftState.equalsAddStates(state1, state2));

        // Equals case
        state1.put("STATE3", new double[] { 2. });
        state2.put("STATE2", new double[] { 2. });
        Assert.assertTrue(SpacecraftState.equalsAddStates(state1, state2));
    }

    @Test
    public void testAddAttitude() throws PatriusException {
        SpacecraftState state = new SpacecraftState(this.orbit);
        state =
            state.addAttitude(this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame()),
                AttitudeType.ATTITUDE);
        Assert.assertNotNull(state.getAttitude());

        SpacecraftState state2 = new SpacecraftState(this.orbit);
        state2 =
            state.addAttitude(this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame()),
                AttitudeType.ATTITUDE_FORCES);
        Assert.assertNotNull(state2.getAttitudeForces());

        new SpacecraftState(this.orbit);
        state2 =
            state.addAttitude(this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame()),
                AttitudeType.ATTITUDE_EVENTS);
        Assert.assertNotNull(state2.getAttitudeEvents());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAttitudeException() throws PatriusException {
        // Try to ass an attitude for events computation when attitudeForces==null
        SpacecraftState state = new SpacecraftState(this.orbit);
        state =
            state.addAttitude(this.attitudeLaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame()),
                AttitudeType.ATTITUDE_EVENTS);
    }

    /**
     * @testedMethod {@link SpacecraftState#getSpacecraftStateLight(AbsoluteDate)}
     *
     * @description unit test to evaluate a spacecraft state that only wrap a date.
     *
     * @testPassCriteria the spacecraft state can be built without error and gives access to the
     *                   expected date
     */
    @Test
    public void testSpacecraftStateLight() {
        final SpacecraftState state = SpacecraftState
                .getSpacecraftStateLight(AbsoluteDate.J2000_EPOCH);
        Assert.assertTrue(state.getDate().equals(AbsoluteDate.J2000_EPOCH));
    }

    /**
     * @throws PatriusException
     * @testedMethod {@link SpacecraftState#SpacecraftState(AttitudeProvider, AttitudeProvider, Orbit, MassProvider, Map)}
     *
     * @description unit test to check the laziness of a SpacecraftState constructor
     *
     * @testPassCriteria the spacecraft state can be built without error and gives a null attitude
     *                   if no provider is given, otherwise becomes not null.
     */
    @Test
    public void testSpacecraftStateLazy() throws PatriusException {
        // If no attitude provider, the attitude stays null
        final SpacecraftState stateNoProvider = new SpacecraftState(null, null, orbit, massModel, new TreeMap<>());
        Assert.assertNull(stateNoProvider.getAttitude());
        // With a not null attitude provider, the attitude is set when the getter is called
        final SpacecraftState state = new SpacecraftState(attitudeLaw, attitudeLaw, orbit, massModel, new TreeMap<>());
        Assert.assertNotNull(state.getAttitude());
    }

    /**
     * Test covering
     * {@link SpacecraftState#SpacecraftState(AttitudeProvider, AttitudeProvider, Orbit, MassProvider, Map)}
     * 
     * @throws PatriusException
     */
    @Test
    public void testSpacecraftStateLazyWithAdditionalStates() throws PatriusException {
        final double[] states = { 0 };
        final String name = "states";
        // One additional state
        final TreeMap<String, double[]> treeMap = new TreeMap<String, double[]>();
        treeMap.put(name, states);
        Assert.assertEquals(treeMap.size(), 1);
        // The constructor adds adds automatically a mass provider
        final SpacecraftState spacecraftState = new SpacecraftState(null, null, orbit, massModel, treeMap);
        final Map<String, double[]> additionalState = spacecraftState.getAdditionalStates();
        Assert.assertEquals(additionalState.size(), 2);
        // Verify additional states
        final double[] newState = additionalState.get(name);
        Assert.assertEquals(newState[0], 0, Precision.DOUBLE_COMPARISON_EPSILON);
        final double[] massState = additionalState.get("MASS_DEFAULT");
        Assert.assertEquals(massState[0], 1000.0, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.clear();
            final double mu = 3.9860047e14;
            final double ae = 6.378137e6;
            final double c20 = -1.08263e-3;
            final double c30 = 2.54e-6;
            final double c40 = 1.62e-6;
            final double c50 = 2.3e-7;
            final double c60 = -5.5e-7;

            this.massModel = new SimpleMassModel(1000., DEFAULT);
            final double a = 7187990.1979844316;
            final double e = 0.5e-4;
            final double i = 1.7105407051081795;
            final double omega = 1.9674147913622104;
            final double OMEGA = MathLib.toRadians(261);
            final double lv = 0;

            final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());
            this.orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                FramesFactory.getEME2000(), date, mu);
            this.attitudeLaw = new BodyCenterPointing(FramesFactory.getITRF());
            this.propagator =
                new EcksteinHechlerPropagator(this.orbit, this.attitudeLaw, ae, mu, this.orbit.getFrame(),
                    c20, c30, c40, c50, c60, this.massModel, ParametersType.OSCULATING);
            ((EcksteinHechlerPropagator) this.propagator).addAdditionalStateProvider(this.massModel);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() throws PatriusException {
        this.massModel = null;
        this.orbit = null;
        this.attitudeLaw = null;
        this.propagator = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

    }

    private MassProvider massModel;
    private Orbit orbit;
    private AttitudeProvider attitudeLaw;
    private Propagator propagator;
    private static final String DEFAULT = "DEFAULT";

}
