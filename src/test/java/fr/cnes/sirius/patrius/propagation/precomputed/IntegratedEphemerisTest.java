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
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::DM:441:12/05/2015:add methods to set and retrieve partial derivatives
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class IntegratedEphemerisTest {

    @Test
    public void testNormalKeplerIntegration() throws PatriusException {

        // Keplerian propagator definition
        final KeplerianPropagator keplerEx = new KeplerianPropagator(this.initialOrbit);

        // Integrated ephemeris

        // Propagation
        final MassProvider mass = new SimpleMassModel(20.0, "part");
        final AbsoluteDate finalDate = this.initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        this.numericalPropagator.setEphemerisMode();
        this.numericalPropagator.setInitialState(new SpacecraftState(this.initialOrbit, mass));
        this.numericalPropagator.setMassProviderEquation(mass);
        final PVCoordinates finalPVNum = this.numericalPropagator.propagate(finalDate).getPVCoordinates();
        Assert.assertTrue(this.numericalPropagator.getCalls() < 3200);
        final IntegratedEphemeris ephemeris = (IntegratedEphemeris) this.numericalPropagator.getGeneratedEphemeris();

        // tests
        for (int i = 1; i <= Constants.JULIAN_DAY; i++) {
            final AbsoluteDate intermediateDate = this.initialOrbit.getDate().shiftedBy(i);
            final SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            final SpacecraftState numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            final Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            final Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
            Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 0.06);
        }
        this.setUp();
        // test inv
        final AbsoluteDate intermediateDate = this.initialOrbit.getDate().shiftedBy(41589);
        final SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        final SpacecraftState state = keplerEx.propagate(finalDate);
        this.numericalPropagator.setInitialState(state);
        this.numericalPropagator.setEphemerisMode();
        this.numericalPropagator.propagate(this.initialOrbit.getDate());
        final BoundedPropagator invEphemeris = this.numericalPropagator.getGeneratedEphemeris();
        final SpacecraftState numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        final Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        final Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
        Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 10e-2);

        // getPVCoordinates coverage + assert on PV :
        PVCoordinates actualPV = ephemeris.getPVCoordinates(finalDate, FramesFactory.getEME2000());
        Assert.assertEquals(0, finalPVNum.getPosition().subtract(actualPV.getPosition()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0, finalPVNum.getVelocity().subtract(actualPV.getVelocity()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0, finalPVNum.getAcceleration().subtract(actualPV.getAcceleration()).getNorm(),
            Precision.EPSILON);

        // resetInitialState exception:
        boolean rez = false;
        try {
            ephemeris.resetInitialState(state);
            Assert.fail();
        } catch (final PropagationException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // setInterpolatedDate exception:
        rez = false;
        try {
            ephemeris.propagateOrbit(finalDate.shiftedBy(10000.0));
            Assert.fail();
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // mass verification:
        final SpacecraftState finalState = ephemeris.propagate(finalDate);
        Assert.assertEquals(20.0, finalState.getMass("part"), 0.0);
        // propagateOrbit coverage:
        actualPV = ephemeris.propagateOrbit(finalDate).getPVCoordinates();
        Assert.assertEquals(0, finalPVNum.getPosition().subtract(actualPV.getPosition()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0, finalPVNum.getVelocity().subtract(actualPV.getVelocity()).getNorm(), Precision.EPSILON);
        Assert.assertEquals(0, finalPVNum.getAcceleration().subtract(actualPV.getAcceleration()).getNorm(),
            Precision.EPSILON);
    }

    @Test
    public void testPartialDerivativesIssue16() throws PatriusException {

        final String eqName = "derivatives";
        this.numericalPropagator.setEphemerisMode();
        this.numericalPropagator.setOrbitType(OrbitType.CARTESIAN);
        final PartialDerivativesEquations derivatives =
            new PartialDerivativesEquations(eqName, this.numericalPropagator);
        // The initial state should contain the partial derivatives:
        final SpacecraftState initialState = derivatives.setInitialJacobians(new SpacecraftState(this.initialOrbit,
            this.initialAttitude));
        final JacobiansMapper mapper = derivatives.getMapper();
        // NB : don't forget to set the initial state with partial derivatives in the propagator!
        this.numericalPropagator.setInitialState(initialState);
        this.numericalPropagator.propagate(this.initialOrbit.getDate().shiftedBy(3600.0));
        final BoundedPropagator ephemeris = this.numericalPropagator.getGeneratedEphemeris();
        ephemeris.setMasterMode(new PatriusStepHandler(){

            private static final long serialVersionUID = -5825020344303732268L;
            private final Array2DRowRealMatrix dYdY0 = new Array2DRowRealMatrix(6, 6);

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public
                    void
                    handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                                throws PropagationException {
                try {
                    final SpacecraftState state = interpolator.getInterpolatedState();
                    final double[] p = interpolator.getInterpolatedState().getAdditionalState(eqName);
                    Assert.assertEquals(mapper.getAdditionalStateDimension(), p.length);
                    mapper.getStateJacobian(state, this.dYdY0.getDataRef());
                    mapper.getParametersJacobian(state, null); // no parameters, this is a no-op and should work
                    final RealMatrix deltaId = this.dYdY0.subtract(MatrixUtils.createRealIdentityMatrix(6));
                    Assert.assertTrue(deltaId.getNorm() > 100);
                    Assert.assertTrue(deltaId.getNorm() < 3100);
                } catch (final PatriusException oe) {
                    throw new PropagationException(oe);
                }
            }

        });

        ephemeris.propagate(this.initialOrbit.getDate().shiftedBy(1800.0));

    }

    @Before
    public void setUp() throws PatriusException {
        // Definition of initial conditions with position and velocity
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final double mu = 3.9860047e14;

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        this.initialOrbit =
            new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), initDate, mu);
        this.initialAttitude = new LofOffset(this.initialOrbit.getFrame(), LOFType.LVLH).getAttitude(this.initialOrbit,
            this.initialOrbit.getDate(),
            this.initialOrbit.getFrame());

        // Numerical propagator definition
        final double[] absTolerance = {
            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8
        };
        final double[] relTolerance = {
            1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9
        };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(100);
        this.numericalPropagator = new NumericalPropagator(integrator);

    }

    private Orbit initialOrbit;
    private Attitude initialAttitude;
    private NumericalPropagator numericalPropagator;

}
