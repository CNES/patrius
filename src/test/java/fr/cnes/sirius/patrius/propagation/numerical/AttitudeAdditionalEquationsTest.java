/**
 * 
 * Copyright 2011-2017 CNES
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
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:449:10/08/2015:Added error if attitudeForces == null and attitudeEvents != null
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.kinematics.KinematicsToolkit;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation tests for attitude additional states and equations.
 * 
 * @author sabatinit
 * 
 * @version $Id: AttitudeAdditionalEquationsTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class AttitudeAdditionalEquationsTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Attitude additional states and equations
         * 
         * @featureDescription Test the additional states representing attitude and spin and their equations
         *                     in case of numerical propagations
         * 
         * @coveredRequirements
         */
        ATTITUDE_ADDITIONAL_STATES_AND_EQUATIONS
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ATTITUDE_ADDITIONAL_STATES_AND_EQUATIONS}
     * 
     * @testedMethod {@link AttitudeEquation#AbstractAttitudeEquation(AttitudeType)}
     * @testedMethod {@link AttitudeEquation#computeDerivatives(SpacecraftState, TimeDerivativesEquations)}
     * 
     * @description test the propagation of attitude additional states using custom additional equations in a two
     *              attitudes configuration.
     * 
     * @input a circular orbit and a numerical propagator
     * 
     * @output additional states and attitude/spin after 1/2 orbit propagation
     * 
     * @testPassCriteria expected attitude and spin after 1/2 orbit propagation
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateEventDetector() throws PatriusException {
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7.0e6, 0.0, MathLib.toRadians(8), 0.0, 0.0, 0.0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, Constants.EGM96_EARTH_MU);

        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final Attitude attForces = bodyCenterPointing.getAttitude(orbit, initDate, FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(true, 0.1,
            0.2, 0.4, 0.1)));
        final Attitude attEvents = inertial.getAttitude(orbit, initDate, FramesFactory.getGCRF());

        // add the additional states representing the attitudes to the initial spacecraftstate:
        SpacecraftState initialState = new SpacecraftState(orbit, attForces, attEvents);
        initialState = initialState.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_FORCES);
        initialState = initialState.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS);

        double[] addAttFor = initialState.getAdditionalStates().get(AttitudeType.ATTITUDE_FORCES.toString());
        double[] addAttEve = initialState.getAdditionalStates().get(AttitudeType.ATTITUDE_EVENTS.toString());
        final Attitude initialAttForces = initialState.getAttitudeForces();
        final Attitude initialAttEvents = initialState.getAttitudeEvents();

        // define basic absolute and relative tolerance
        final double[] absTolerance = { 1.0e-6, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };

        // define a variable step integrator
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0., 30, absTolerance, relTolerance);
        integrator.setInitialStepSize(1.0);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(initialState);

        // add the additional equations to the propagator:
        final AttitudeAdditionalEquations forcesAttEqs = new AttitudeAdditionalEquations(AttitudeType.ATTITUDE_FORCES);
        propagator.addAttitudeEquation(forcesAttEqs);
        final AttitudeAdditionalEquations eventsAttEqs = new AttitudeAdditionalEquations(AttitudeType.ATTITUDE_EVENTS);
        propagator.addAttitudeEquation(eventsAttEqs);
        // propagate:
        SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(0.5 * orbit.getKeplerianPeriod()));
        addAttFor = finalState.getAdditionalStates().get(AttitudeType.ATTITUDE_FORCES.toString());
        addAttEve = finalState.getAdditionalStates().get(AttitudeType.ATTITUDE_EVENTS.toString());

        final Attitude finalAttForces = finalState.getAttitudeForces();
        final Attitude finalAttEvents = finalState.getAttitudeEvents();

        // First attitude - ATTITUDE_FORCES: The final attitude retrieved from the Attitude object should match the
        // corresponding additional states:
        Assert.assertEquals(addAttFor[0], finalAttForces.getRotation().getQi()[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttFor[1], finalAttForces.getRotation().getQi()[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttFor[2], finalAttForces.getRotation().getQi()[2], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttFor[3], finalAttForces.getRotation().getQi()[3], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttFor[4], finalAttForces.getSpin().getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttFor[5], finalAttForces.getSpin().getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttFor[6], finalAttForces.getSpin().getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
        // First attitude - ATTITUDE_FORCES: the angular distance between the initial attitude and the attitude after
        // 1/2 orbit should be PI
        // (the orbit is circular - constant spin - body center pointing attitude law):
        Assert.assertEquals(FastMath.PI, finalAttForces.getRotation()
            .applyInverseTo(initialAttForces.getRotation()).getAngle(), 1E-11);
        // First attitude - ATTITUDE_FORCES: The spin value should not have changed:
        Assert.assertEquals(initialAttForces.getSpin(), finalAttForces.getSpin());

        // Second attitude - ATTITUDE_EVENTS: The final attitude retrieved from the Attitude object should match the
        // corresponding additional states:
        Assert.assertEquals(addAttEve[0], finalAttEvents.getRotation().getQi()[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttEve[1], finalAttEvents.getRotation().getQi()[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttEve[2], finalAttEvents.getRotation().getQi()[2], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttEve[3], finalAttEvents.getRotation().getQi()[3], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttEve[4], finalAttEvents.getSpin().getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttEve[5], finalAttEvents.getSpin().getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAttEve[6], finalAttEvents.getSpin().getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
        // Second attitude - ATTITUDE_EVENTS: the angular distance between the initial attitude and the attitude after
        // 1/2 orbit should be 0
        // (fixed pointing law)
        Assert.assertEquals(0.0, finalAttEvents.getOrientation().getRotation()
            .applyInverseTo(initialAttEvents.getOrientation().getRotation()).getAngle(), 1E-11);
        // Second attitude - ATTITUDE_EVENTS: The spin value should not have changed:
        Assert.assertEquals(finalAttEvents.getSpin(), finalAttEvents.getSpin());

        initialState = new SpacecraftState(orbit);
        final AdaptiveStepsizeIntegrator integrator2 =
            new DormandPrince853Integrator(0., 30, absTolerance, relTolerance);
        final NumericalPropagator propWithAttLaw = new NumericalPropagator(integrator2);
        propWithAttLaw.resetInitialState(initialState);
        propWithAttLaw.setAttitudeProvider(bodyCenterPointing);
        finalState = propWithAttLaw.propagate(initDate.shiftedBy(0.5 * orbit.getKeplerianPeriod()));
        // Compare the attitude computed with the attitude provider propagation with the attitude computed using the
        // additional states:
        Assert.assertEquals(0.0,
            Rotation.distance(finalState.getAttitude().getRotation(), finalAttForces.getRotation()), 1E-11);
        Assert.assertEquals(finalAttForces.getSpin().getX(), finalState.getAttitude().getSpin().getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(finalAttForces.getSpin().getY(), finalState.getAttitude().getSpin().getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(finalAttForces.getSpin().getZ(), finalState.getAttitude().getSpin().getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType TU
     * 
     * @testedFeature {@link features#ATTITUDE_ADDITIONAL_STATES_AND_EQUATIONS}
     * 
     * @testedMethod {@link AttitudeEquation#AbstractAttitudeEquation(AttitudeType)}
     * @testedMethod {@link AttitudeEquation#computeDerivatives(SpacecraftState, TimeDerivativesEquations)}
     * 
     * @description test the propagation of attitude additional states using custom additional equations in a single
     *              attitude configuration (attitude for forces computation).
     * 
     * @input a circular orbit and a numerical propagator
     * 
     * @output additional states and attitude/spin after 1/2 orbit propagation
     * 
     * @testPassCriteria expected attitude and spin after 1/2 orbit propagation
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testCoverage() throws PatriusException {
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7.0e6, 0.0, MathLib.toRadians(8), 0.0, 0.0, 0.0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, Constants.EGM96_EARTH_MU);

        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final Attitude att = bodyCenterPointing.getAttitude(orbit, initDate, FramesFactory.getGCRF());

        // add the additional states representing the attitudes to the initial spacecraftstate:
        SpacecraftState initialState = new SpacecraftState(orbit, att);
        initialState = initialState.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE);

        double[] addAtt = initialState.getAdditionalStates().get(AttitudeType.ATTITUDE.toString());
        final Attitude initialAttForces = initialState.getAttitudeForces();

        // define basic absolute and relative tolerance
        final double[] absTolerance = { 1.0e-6, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };

        // define a variable step integrator
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0., 30, absTolerance, relTolerance);
        integrator.setInitialStepSize(1.0);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(initialState);

        // add the additional equations to the propagator:
        final AttitudeAdditionalEquations AttEqs = new AttitudeAdditionalEquations(AttitudeType.ATTITUDE);
        propagator.addAttitudeEquation(AttEqs);
        // propagate:
        SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(0.5 * orbit.getKeplerianPeriod()));
        addAtt = finalState.getAdditionalStates().get(AttitudeType.ATTITUDE.toString());

        final Attitude finalAtt = finalState.getAttitude();

        // ATTITUDE: The final attitude retrieved from the Attitude object should match the corresponding additional
        // states:
        Assert.assertEquals(addAtt[0], finalAtt.getRotation().getQi()[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAtt[1], finalAtt.getRotation().getQi()[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAtt[2], finalAtt.getRotation().getQi()[2], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAtt[3], finalAtt.getRotation().getQi()[3], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAtt[4], finalAtt.getSpin().getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAtt[5], finalAtt.getSpin().getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(addAtt[6], finalAtt.getSpin().getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
        // ATTITUDE: the angular distance between the initial attitude and the attitude after 1/2 orbit should be PI
        // (the orbit is circular - constant spin - body center pointing attitude law):
        Assert.assertEquals(FastMath.PI, finalAtt.getOrientation().getRotation()
            .applyInverseTo(initialAttForces.getOrientation().getRotation()).getAngle(), 1E-11);
        // ATTITUDE: The spin value should not have changed:
        Assert.assertEquals(initialAttForces.getSpin(), finalAtt.getSpin());

        initialState = new SpacecraftState(orbit);
        final AdaptiveStepsizeIntegrator integrator2 =
            new DormandPrince853Integrator(0., 30, absTolerance, relTolerance);
        final NumericalPropagator propWithAttLaw = new NumericalPropagator(integrator2);
        propWithAttLaw.resetInitialState(initialState);
        propWithAttLaw.setAttitudeProvider(bodyCenterPointing);
        finalState = propWithAttLaw.propagate(initDate.shiftedBy(0.5 * orbit.getKeplerianPeriod()));
        // Compare the attitude computed with the attitude provider propagation with the attitude computed using the
        // additional states:
        Assert.assertEquals(0.0, finalState.getAttitude().getRotation().applyInverseTo(finalAtt.getRotation())
            .getAngle(), 1E-11);
        Assert.assertEquals(finalAtt.getSpin().getX(), finalState.getAttitude().getSpin().getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(finalAtt.getSpin().getY(), finalState.getAttitude().getSpin().getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(finalAtt.getSpin().getZ(), finalState.getAttitude().getSpin().getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * This class implements the differential equations to compute the attitude for the forces or events computation
     * during the propagation.
     */
    private class AttitudeAdditionalEquations extends AttitudeEquation {

        /**
         * Constructor.
         */
        public AttitudeAdditionalEquations(final AttitudeType attitudeType) {
            super(attitudeType);
        }

        @Override
        public void computeDerivatives(final SpacecraftState s,
                final TimeDerivativesEquations adder) throws PatriusException {
            // current value of the additional parameters:
            final double[] p = s.getAdditionalState(this.getName());
            // initialize the derivatives of the additional parameters:
            final double[] pDot = new double[p.length];

            final Quaternion q = new Quaternion(p[0], p[1], p[2], p[3]);
            final Vector3D spin = new Vector3D(p[4], p[5], p[6]);
            // compute the dq from q and spin values:
            final Quaternion dq = KinematicsToolkit.differentiateQuaternion(q, spin);
            pDot[0] = dq.getQ0();
            pDot[1] = dq.getQ1();
            pDot[2] = dq.getQ2();
            pDot[3] = dq.getQ3();

            pDot[4] = 0.0;
            pDot[5] = 0.0;
            pDot[6] = 0.0;

            adder.addAdditionalStateDerivative(this.getName(), pDot);
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
    }
}
