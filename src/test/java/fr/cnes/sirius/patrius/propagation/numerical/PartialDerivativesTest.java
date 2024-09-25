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
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
* VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
* VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
* VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
* VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Droziner to UnnormalizedDroziner
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::DM:441:12/05/2015:add methods to set and retrieve partial derivatives
 * VERSION::DM:483:20/10/2015: Modification of signature of some methods JaccobianMapper
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1798:10/12/2018: add AlternateEquinoctial case
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.CunninghamGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.AlternateEquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class PartialDerivativesTest {

    private static final String UNSUPPORTED_ORBIT_TYPE = "unsupported orbit type";

    @Test
    public void testPropagationTypesElliptical() throws PatriusException, ParseException,
            IOException {

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double mu = provider.getMu();
        final DrozinerGravityModel gravityModel = new DrozinerGravityModel(FramesFactory.getITRF(),
            6378136.460, mu, provider.getC(5, 5, true), provider.getS(5, 5, true));
        gravityModel.setCentralTermContribution(false);
        final ForceModel gravityField = new DirectBodyAttraction(gravityModel);
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2,
                PositionAngle.TRUE, FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit,
                orbit.getDate(), orbit.getFrame());
        SpacecraftState initialState = new SpacecraftState(orbit, attitude);
        final DirectBodyAttraction newtonian = new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu()));

        final double dt = 3200;
        final double dP = 0.001;
        for (final OrbitType orbitType : OrbitType.values()) {
            for (final PositionAngle angleType : PositionAngle.values()) {

                // compute state Jacobian using PartialDerivatives
                final NumericalPropagator propagator = PartialDerivativesTest.setUpPropagator(initialState, dP,
                    orbitType, angleType, gravityField, newtonian);
                propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(),
                        Rotation.IDENTITY));
                final PartialDerivativesEquations partials = new PartialDerivativesEquations(
                        "partials", propagator);
                // test the exceptions:
                boolean rez = false;
                try {
                    partials.getMapper();
                } catch (final PatriusException e) {
                    rez = true;
                }
                Assert.assertTrue(rez);
                rez = false;
                try {
                    partials.setInitialJacobians(initialState, new double[][] { { 0.0 }, { 0.2 } },
                            new double[][] { { 0.0 }, { 0.2 } });
                } catch (final PatriusException e) {
                    rez = true;
                }
                Assert.assertTrue(rez);
                rez = false;
                try {
                    partials.setInitialJacobians(initialState, new double[6][6], new double[][] {
                            { 0.0 }, { 0.2 } });
                } catch (final PatriusException e) {
                    rez = true;
                }
                Assert.assertTrue(rez);

                initialState = partials.setInitialJacobians(initialState);
                final JacobiansMapper mapper = partials.getMapper();
                // NB : don't forget to set the initial state with partial derivatives in the
                // propagator!
                propagator.setInitialState(initialState);
                final PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                final double[][] dYdY0 = pickUp.getdYdY0();

                // compute reference state Jacobian using finite differences
                final double[][] dYdY0Ref = new double[6][6];
                final NumericalPropagator propagator2 = PartialDerivativesTest.setUpPropagator(initialState, dP,
                    orbitType, angleType, gravityField, newtonian);
                final double[] steps = NumericalPropagator.tolerances(1000000 * dP,
                        initialState.getOrbit(), orbitType)[0];
                for (int i = 0; i < 6; ++i) {
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -4 * steps[i], i));
                    final SpacecraftState sM4h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -3 * steps[i], i));
                    final SpacecraftState sM3h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -2 * steps[i], i));
                    final SpacecraftState sM2h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -1 * steps[i], i));
                    final SpacecraftState sM1h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 1 * steps[i], i));
                    final SpacecraftState sP1h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 2 * steps[i], i));
                    final SpacecraftState sP2h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 3 * steps[i], i));
                    final SpacecraftState sP3h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 4 * steps[i], i));
                    final SpacecraftState sP4h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    PartialDerivativesTest.fillJacobianColumn(dYdY0Ref, i, orbitType, angleType, steps[i], sM4h,
                            sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
                }

                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        final double error = MathLib.abs((dYdY0[i][j] - dYdY0Ref[i][j])
                                / dYdY0Ref[i][j]);
                        if (orbitType.equals(OrbitType.CARTESIAN)) {
                            Assert.assertEquals(0, error, 2.6e-2);
                        }

                    }
                }

            }

        }

    }

    @Test
    public void testPropagationTypesHyperbolic() throws PatriusException, ParseException,
            IOException {

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final Parameter muParam = new Parameter("mu", provider.getMu());
        final Parameter aeParam = new Parameter("ae", 6378136.460);
        final DrozinerGravityModel gravityModel = new DrozinerGravityModel(FramesFactory.getITRF(),
            aeParam, muParam, provider.getC(5, 5, true), provider.getS(5, 5, true));
        gravityModel.setCentralTermContribution(false);
        final ForceModel gravityField = new DirectBodyAttraction(gravityModel);
        final Orbit orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(-1551946.0, 708899.0,
                6788204.0), new Vector3D(-9875.0, -3941.0, -1845.0)), FramesFactory.getEME2000(),
                AbsoluteDate.J2000_EPOCH, muParam.getValue());
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit,
                orbit.getDate(), orbit.getFrame());
        SpacecraftState initialState = new SpacecraftState(orbit, attitude);
        final DirectBodyAttraction newtonian = new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu()));
        final double dt = 3200;
        final double dP = 0.001;
        for (final OrbitType orbitType : new OrbitType[] { OrbitType.KEPLERIAN, OrbitType.CARTESIAN }) {
            for (final PositionAngle angleType : PositionAngle.values()) {

                // compute state Jacobian using PartialDerivatives
                final NumericalPropagator propagator = PartialDerivativesTest.setUpPropagator(initialState, dP,
                    orbitType, angleType, gravityField, newtonian);
                final PartialDerivativesEquations partials = new PartialDerivativesEquations(
                        "partials", propagator);
                initialState = partials.setInitialJacobians(initialState);
                // NB : don't forget to set the initial state with partial derivatives in the
                // propagator!
                propagator.setInitialState(initialState);
                propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(),
                        Rotation.IDENTITY));
                final JacobiansMapper mapper = partials.getMapper();
                final PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                final double[][] dYdY0 = pickUp.getdYdY0();

                // compute reference state Jacobian using finite differences
                final double[][] dYdY0Ref = new double[6][6];
                final NumericalPropagator propagator2 = PartialDerivativesTest.setUpPropagator(initialState, dP,
                    orbitType, angleType, gravityField, newtonian);
                final double[] steps = NumericalPropagator.tolerances(1000000 * dP,
                        initialState.getOrbit(), orbitType)[0];
                for (int i = 0; i < 6; ++i) {
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -4 * steps[i], i));
                    final SpacecraftState sM4h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -3 * steps[i], i));
                    final SpacecraftState sM3h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -2 * steps[i], i));
                    final SpacecraftState sM2h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, -1 * steps[i], i));
                    final SpacecraftState sM1h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 1 * steps[i], i));
                    final SpacecraftState sP1h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 2 * steps[i], i));
                    final SpacecraftState sP2h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 3 * steps[i], i));
                    final SpacecraftState sP3h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    propagator2.resetInitialState(PartialDerivativesTest.shiftState(initialState, orbitType,
                            angleType, 4 * steps[i], i));
                    final SpacecraftState sP4h = propagator2.propagate(initialState.getDate()
                            .shiftedBy(dt));
                    PartialDerivativesTest.fillJacobianColumn(dYdY0Ref, i, orbitType, angleType, steps[i], sM4h,
                            sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
                }

                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        final double error = MathLib.abs((dYdY0[i][j] - dYdY0Ref[i][j])
                                / dYdY0Ref[i][j]);
                        if (orbitType.equals(OrbitType.CARTESIAN)) {
                            Assert.assertEquals(0, error, 5.3e-3);
                        }

                    }
                }
                // Available parameters for jacobian computation:
                Assert.assertEquals(2, partials.getAvailableParameters().size());

            }
        }

    }

    @Test
    public void testCoverage() throws PatriusException, ParseException, IOException {

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double mu = provider.getMu();
        final ForceModel gravityField = new DirectBodyAttraction(new CunninghamGravityModel(FramesFactory.getITRF(),
            6378136.460, mu, provider.getC(5, 5, true), provider.getS(5, 5, true)));
        final Orbit orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(-1551946.0, 708899.0,
                6788204.0), new Vector3D(-9875.0, -3941.0, -1845.0)), FramesFactory.getEME2000(),
                AbsoluteDate.J2000_EPOCH, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit,
                orbit.getDate(), orbit.getFrame());
        SpacecraftState initialState = new SpacecraftState(orbit, attitude);
        // compute state Jacobian using PartialDerivatives
        final NumericalPropagator propagator = PartialDerivativesTest.setUpPropagator(initialState, 0.01,
                orbit.getType(), PositionAngle.TRUE, gravityField);
        final PartialDerivativesEquations partials = new PartialDerivativesEquations("partials",
                propagator);
        final List<Parameter> list = new ArrayList<>();
        list.add(new Parameter("central attraction coefficient", mu));
        partials.selectParameters(list);
        partials.setSteps(0.1);
        initialState = partials.setInitialJacobians(initialState);
        // NB : don't forget to set the initial state with partial derivatives in the propagator!
        propagator.setInitialState(initialState);
        final JacobiansMapper mapper = partials.getMapper();
        final PickUpHandler pickUp = new PickUpHandler(mapper, null);
        propagator.setMasterMode(pickUp);
        boolean rez = false;
        try {
            propagator.propagate(initialState.getDate().shiftedBy(3000));
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        List<ParameterConfiguration> parameters = partials.getSelectedParameters();
        Assert.assertEquals(1, parameters.size());
        Assert.assertEquals("central attraction coefficient", parameters.get(0).getParameter()
                .getName());
        Assert.assertEquals(mu, parameters.get(0).getParameter().getValue(), 0);
        Assert.assertFalse(partials.contains(new Parameter("central attraction coefficient", mu)));
        partials.clearSelectedParameters();
        parameters = partials.getSelectedParameters();
        Assert.assertEquals(0, parameters.size());
    }

    @Test
    public void testSetGetJacobians() throws PatriusException {

        // Initialization
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7000E3, 0, 0),
            new Vector3D(0, 7E3, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,
            Constants.EGM96_EARTH_MU);
        SpacecraftState state = new SpacecraftState(orbit);

        final NumericalPropagator propagator = new NumericalPropagator(
            new ClassicalRungeKuttaIntegrator(1.), FramesFactory.getGCRF(), OrbitType.CARTESIAN, PositionAngle.TRUE);
        final PartialDerivativesEquations equations = new PartialDerivativesEquations("Partial",
                propagator);
        final Parameter param1 = new Parameter("Param1", 1.);
        final Parameter param2 = new Parameter("Param2", 2.);
        final Parameter param3 = new Parameter("Param3", 3.);
        final List<Parameter> list = new ArrayList<>();
        list.add(param1);
        list.add(param2);
        equations.selectParameters(list);


        // ====================== Non-initialized partial derivatives ======================

        try {
            equations.setInitialJacobians(state, new double[6][6]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            equations.setInitialJacobians(state, param1, new double[6]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // ====================== Normal case ======================

        state = equations.setInitialJacobians(state);

        // Check mapper
        final JacobiansMapper mapper = equations.getMapper();
        Assert.assertTrue(mapper.getName().equals(equations.getName()));
        Assert.assertEquals(2, mapper.getParametersList().size());
        Assert.assertTrue(mapper.getParametersList().get(0).equals(param1));
        Assert.assertTrue(mapper.getParametersList().get(1).equals(param2));
        Assert.assertTrue(mapper.getOrbitType().equals(propagator.getOrbitType()));
        Assert.assertTrue(mapper.getAngleType().equals(propagator.getPositionAngleType()));
        Assert.assertTrue(mapper.getPropagationFrame().equals(propagator.getFrame()));

        // Test and check set and get state derivatives and parameters derivatives
        final double[][] dYdY0Ref = { { 0, 6, 0, 0, 0, 5 }, { 0, 2, 0, 0, 0, 6 },
                { 0, 0, 0, 5, 0, 4 }, { 0, 0, 0, 0, 2, 3 }, { 0, 0, 0, 0, 4, 0 },
                { 5, 0, 0, 0, 0, 0 } };
        final double[][] dYdY0Act = new double[6][6];
        double[][] dYdY0Act_return = new double[6][6];
        state = equations.setInitialJacobians(state, dYdY0Ref);
        mapper.getStateJacobian(state, dYdY0Act);
        checkDoubleArray(dYdY0Ref, dYdY0Act);

        dYdY0Act_return = mapper.getStateJacobian(state);
        checkDoubleArray(dYdY0Ref, dYdY0Act_return);

        final double[][] dYdYPRef = { { 1, 5 }, { 2, 4 }, { 3, 3 }, { 4, 2 }, { 5, 0 }, { 6, 6 } };
        final double[] dYdYP1Ref = { 1, 2, 3, 4, 5, 6 };
        final double[] dYdYP2Ref = { 5, 4, 3, 2, 0, 6 };
        final double[][] dYdYPAct = new double[6][2];
        final double[] dYdYP1Act = new double[6];
        final double[] dYdYP2Act = new double[6];
        final double[] dYdYP2ActItrf = new double[6];
        double[][] dYdYPAct_return = new double[6][2];
        double[] dYdYP1Act_return = new double[6];
        double[] dYdYP2Act_return = new double[6];
        state = equations.setInitialJacobians(state, param1, dYdYP1Ref);
        state = equations.setInitialJacobians(state, param2, dYdYP2Ref);
        mapper.getParametersJacobian(state, dYdYPAct);
        mapper.getParametersJacobian(param1, state, dYdYP1Act);
        mapper.getParametersJacobian(param2, state, dYdYP2Act);
        checkDoubleArray(dYdYPRef, dYdYPAct);
        checkDoubleArray(dYdYP1Ref, dYdYP1Act);
        checkDoubleArray(dYdYP2Ref, dYdYP2Act);

        dYdYPAct_return = mapper.getParametersJacobian(state);
        dYdYP1Act_return = mapper.getParametersJacobian(param1, state);
        dYdYP2Act_return = mapper.getParametersJacobian(param2, state);
        checkDoubleArray(dYdYPRef, dYdYPAct_return);
        checkDoubleArray(dYdYP1Ref, dYdYP1Act_return);
        checkDoubleArray(dYdYP2Ref, dYdYP2Act_return);

        // FA-3246: specify a different output frame and check that a transform is well used (non
        // regression). Previously the specified output frame wasn't used.
        final double[] dYdYP2ItrfRef = { -3.0521685348103436, 5.629009477032913, 2.999753246950042, 0.3541839237978011,
            1.9688492744248953, 5.999946102721538 };
        mapper.getParametersJacobian(param2, state, dYdYP2ActItrf, OrbitType.CARTESIAN,
                PositionAngle.TRUE, FramesFactory.getITRF());
        checkDoubleArray(dYdYP2ItrfRef, dYdYP2ActItrf);

        // ====================== Unknown parameter ======================

        // Set
        final double[] dYdYP3Ref = { 5, -4, -3, -2, 1, 3 };
        final double[][] dYdYPAct2 = new double[6][2];
        state = equations.setInitialJacobians(state, param3, dYdYP3Ref);
        mapper.getParametersJacobian(state, dYdYPAct2);
        checkDoubleArray(dYdYPRef, dYdYPAct2);

        // Get
        final double[] dYdYP3Act = null;
        mapper.getParametersJacobian(param3, state, dYdYP3Act);
        Assert.assertNull(dYdYP3Act);
    }

    @Test
    public void testNotGradientModel() {
        final ForceModel forceModel = new ForceModel() {

            /** Serializable UID. */
            private static final long serialVersionUID = 8106649013045612671L;

            @Override
            public boolean supportsParameter(final Parameter param) {
                return false;
            }

            @Override
            public ArrayList<Parameter> getParameters() {
                return null;
            }

            @Override
            public EventDetector[] getEventsDetectors() {
                return null;
            }

            @Override
            public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
                return null;
            }

            @Override
            public void addContribution(final SpacecraftState s,
                    final TimeDerivativesEquations adder) throws PatriusException {
                // nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public void checkData(final AbsoluteDate start, final AbsoluteDate end)
                    throws PatriusException {
                // Nothing to do
            }
        };
        final Jacobianizer jacobianizer = new Jacobianizer(forceModel,
                new ArrayList<ParameterConfiguration>(), 1);

        try {
            jacobianizer.addDAccDState(new SpacecraftState(new CartesianOrbit(PVCoordinates.ZERO,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 0.)), new double[3][3],
                    new double[3][3]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    private static void checkDoubleArray(final double[][] array1, final double[][] array2) {
        for (int i = 0; i < array1.length; i++) {
            for (int j = 0; j < array1[i].length; j++) {
                Assert.assertEquals(array1[i][j], array2[i][j], 0.);
            }
        }
    }

    private static void checkDoubleArray(final double[] array1, final double[] array2) {
        for (int i = 0; i < array1.length; i++) {
            Assert.assertEquals(array1[i], array2[i], 0.);
        }
    }

    private static void fillJacobianColumn(final double[][] jacobian, final int column,
            final OrbitType orbitType, final PositionAngle angleType, final double h,
            final SpacecraftState sM4h, final SpacecraftState sM3h, final SpacecraftState sM2h,
            final SpacecraftState sM1h, final SpacecraftState sP1h, final SpacecraftState sP2h,
            final SpacecraftState sP3h, final SpacecraftState sP4h) {
        final double[] aM4h = stateToArray(sM4h, orbitType, angleType);
        final double[] aM3h = stateToArray(sM3h, orbitType, angleType);
        final double[] aM2h = stateToArray(sM2h, orbitType, angleType);
        final double[] aM1h = stateToArray(sM1h, orbitType, angleType);
        final double[] aP1h = stateToArray(sP1h, orbitType, angleType);
        final double[] aP2h = stateToArray(sP2h, orbitType, angleType);
        final double[] aP3h = stateToArray(sP3h, orbitType, angleType);
        final double[] aP4h = stateToArray(sP4h, orbitType, angleType);
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = (-3 * (aP4h[i] - aM4h[i]) + 32 * (aP3h[i] - aM3h[i]) - 168
                    * (aP2h[i] - aM2h[i]) + 672 * (aP1h[i] - aM1h[i]))
                    / (840 * h);
        }
    }

    private static SpacecraftState shiftState(final SpacecraftState state,
            final OrbitType orbitType,
            final PositionAngle angleType, final double delta, final int column)
            throws PatriusException {

        final double[] array = stateToArray(state, orbitType, angleType);
        array[column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                state.getMu(), state.getAttitude());

    }

    private static double[] stateToArray(final SpacecraftState state, final OrbitType orbitType,
            final PositionAngle angleType) {
        final double[] array = new double[6];
        switch (orbitType) {
            case CARTESIAN: {
                final CartesianOrbit cart = (CartesianOrbit) orbitType
                        .convertType(state.getOrbit());
                array[0] = cart.getPVCoordinates().getPosition().getX();
                array[1] = cart.getPVCoordinates().getPosition().getY();
                array[2] = cart.getPVCoordinates().getPosition().getZ();
                array[3] = cart.getPVCoordinates().getVelocity().getX();
                array[4] = cart.getPVCoordinates().getVelocity().getY();
                array[5] = cart.getPVCoordinates().getVelocity().getZ();
            }
                break;
            case CIRCULAR: {
                final CircularOrbit circ = (CircularOrbit) orbitType.convertType(state.getOrbit());
                array[0] = circ.getA();
                array[1] = circ.getCircularEx();
                array[2] = circ.getCircularEy();
                array[3] = circ.getI();
                array[4] = circ.getRightAscensionOfAscendingNode();
                array[5] = circ.getAlpha(angleType);
            }
                break;
            case EQUINOCTIAL: {
                final EquinoctialOrbit equ = (EquinoctialOrbit) orbitType.convertType(state
                        .getOrbit());
                array[0] = equ.getA();
                array[1] = equ.getEquinoctialEx();
                array[2] = equ.getEquinoctialEy();
                array[3] = equ.getHx();
                array[4] = equ.getHy();
                array[5] = equ.getL(angleType);
            }
                break;
            case APSIS: {
                final ApsisOrbit aps = (ApsisOrbit) orbitType.convertType(state.getOrbit());
                array[0] = aps.getPeriapsis();
                array[1] = aps.getApoapsis();
                array[2] = aps.getI();
                array[3] = aps.getPerigeeArgument();
                array[4] = aps.getRightAscensionOfAscendingNode();
                array[5] = aps.getAnomaly(angleType);
            }
                break;
            case EQUATORIAL: {
                final EquatorialOrbit eqa = (EquatorialOrbit) orbitType.convertType(state
                        .getOrbit());
                array[0] = eqa.getA();
                array[1] = eqa.getE();
                array[2] = eqa.getPomega();
                array[3] = eqa.getIx();
                array[4] = eqa.getIy();
                array[5] = eqa.getAnomaly(angleType);
            }
                break;
            case KEPLERIAN: {
                final KeplerianOrbit kep = (KeplerianOrbit) orbitType.convertType(state.getOrbit());
                array[0] = kep.getA();
                array[1] = kep.getE();
                array[2] = kep.getI();
                array[3] = kep.getPerigeeArgument();
                array[4] = kep.getRightAscensionOfAscendingNode();
                array[5] = kep.getAnomaly(angleType);
            }
                break;
            case ALTERNATE_EQUINOCTIAL: {
                final AlternateEquinoctialOrbit equ = (AlternateEquinoctialOrbit) orbitType
                        .convertType(state.getOrbit());
                array[0] = equ.getN();
                array[1] = equ.getEquinoctialEx();
                array[2] = equ.getEquinoctialEy();
                array[3] = equ.getHx();
                array[4] = equ.getHy();
                array[5] = equ.getL(angleType);
            }
                break;
            default:
                throw new RuntimeException(UNSUPPORTED_ORBIT_TYPE);
        }

        return array;

    }

    private static SpacecraftState arrayToState(final double[] array, final OrbitType orbitType,
            final PositionAngle angleType, final Frame frame, final AbsoluteDate date,
            final double mu, final Attitude attitude) {
        Orbit orbit = null;
        switch (orbitType) {
            case CARTESIAN:
                orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(array[0], array[1],
                        array[2]), new Vector3D(array[3], array[4], array[5])), frame, date, mu);
                break;
            case CIRCULAR:
                orbit = new CircularOrbit(array[0], array[1], array[2], array[3], array[4],
                        array[5], angleType, frame, date, mu);
                break;
            case EQUINOCTIAL:
                orbit = new EquinoctialOrbit(array[0], array[1], array[2], array[3], array[4],
                        array[5], angleType, frame, date, mu);
                break;
            case APSIS:
                orbit = new ApsisOrbit(array[0], array[1], array[2], array[3], array[4], array[5],
                        angleType, frame, date, mu);
                break;
            case EQUATORIAL:
                orbit = new EquatorialOrbit(array[0], array[1], array[2], array[3], array[4],
                        array[5], angleType, frame, date, mu);
                break;
            case KEPLERIAN:
                orbit = new KeplerianOrbit(array[0], array[1], array[2], array[3], array[4],
                        array[5], angleType, frame, date, mu);
                break;
            case ALTERNATE_EQUINOCTIAL:
                orbit = new AlternateEquinoctialOrbit(array[0], array[1], array[2], array[3],
                        array[4], array[5], angleType, frame, date, mu);
                break;
            default:
                throw new RuntimeException(UNSUPPORTED_ORBIT_TYPE);
        }
        return new SpacecraftState(orbit, attitude);
    }

    private static NumericalPropagator setUpPropagator(final SpacecraftState state,
            final double dP, final OrbitType orbitType, final PositionAngle angleType,
                                                       final ForceModel... models) throws PatriusException {

        final double minStep = 0.001;
        final double maxStep = 1000;

        final double[][] tol = NumericalPropagator.tolerances(dP, state.getOrbit(), orbitType);
        final NumericalPropagator propagator = new NumericalPropagator(
            new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]), state.getFrame(), orbitType, angleType);
        for (final ForceModel model : models) {
            propagator.addForceModel(model);
        }
        propagator.setInitialState(state);
        return propagator;
    }

    private static class PickUpHandler implements PatriusStepHandler {

        private static final long serialVersionUID = 8040284226089555027L;
        private final JacobiansMapper mapper;
        private final AbsoluteDate pickUpDate;
        private final double[][] dYdY0;
        private final double[][] dYdP;

        public PickUpHandler(final JacobiansMapper mapper, final AbsoluteDate pickUpDate) {
            this.mapper = mapper;
            this.pickUpDate = pickUpDate;
            this.dYdY0 = new double[mapper.getStateDimension()][mapper.getStateDimension()];
            this.dYdP = new double[mapper.getStateDimension()][mapper.getParameters()];
        }

        public double[][] getdYdY0() {
            return this.dYdY0;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public void handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                throws PropagationException {
            try {
                if (this.pickUpDate == null) {
                    // we want to pick up the Jacobians at the end of last step
                    if (isLast) {
                        interpolator.setInterpolatedDate(interpolator.getCurrentDate());
                    }
                } else {
                    // we want to pick up some intermediate Jacobians
                    final double dt0 = this.pickUpDate.durationFrom(interpolator.getPreviousDate());
                    final double dt1 = this.pickUpDate.durationFrom(interpolator.getCurrentDate());
                    if (dt0 * dt1 > 0) {
                        // the current step does not cover the pickup date
                        return;
                    }
                    interpolator.setInterpolatedDate(this.pickUpDate);
                }

                final SpacecraftState state = interpolator.getInterpolatedState();
                this.mapper.getStateJacobian(state, this.dYdY0);
                this.mapper.getParametersJacobian(state, this.dYdP);

            } catch (final PropagationException pe) {
                throw pe;
            } catch (final PatriusException oe) {
                throw new PropagationException(oe);
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
