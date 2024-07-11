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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:added getJacobiansParametersNames() method
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class enabling basic {@link ForceModel} instances
 * to be used when processing spacecraft state partial derivatives.
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
public class Jacobianizer extends JacobiansParameterizable {

     /** Serializable UID. */
    private static final long serialVersionUID = 7736576299828036068L;

    /** Wrapped force model instance. */
    private final ForceModel forceModel;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private final double hPos;

    /** Step used for finite difference computation with respect to parameters value. */
    private final Map<Integer, Double> hParam;

    /** Dedicated adder used to retrieve nominal acceleration. */
    private final AccelerationRetriever nominal;

    /** Dedicated adder used to retrieve shifted acceleration. */
    private final AccelerationRetriever shifted;

    /**
     * Simple constructor.
     * 
     * @param forceModelIn
     *        force model instance to wrap
     * @param paramsAndSteps
     *        collection of parameters and their associated steps
     * @param hPosIn
     *        step used for finite difference computation with respect to spacecraft position (m)
     */
    public Jacobianizer(final ForceModel forceModelIn, final Collection<ParameterConfiguration> paramsAndSteps,
        final double hPosIn) {
        super();

        this.forceModel = forceModelIn;
        this.hParam = new HashMap<>();
        this.hPos = hPosIn;
        this.nominal = new AccelerationRetriever();
        this.shifted = new AccelerationRetriever();

        // set up parameters for jacobian computation
        for (final ParameterConfiguration param : paramsAndSteps) {
            final Parameter p = param.getParameter();
            if (forceModelIn.supportsParameter(p)) {
                double step = param.getHP();
                if (Double.isNaN(step)) {
                    step = MathLib.max(1.0, MathLib.abs(p.getValue())) *
                        MathLib.sqrt(Precision.EPSILON);
                }
                this.hParam.put(p.hashCode(), step);
                this.addJacobiansParameter(p);
            }
        }
    }

    /**
     * Compute acceleration.
     * 
     * @param retriever
     *        acceleration retriever to use for storing acceleration
     * @param s
     *        original state
     * @param p
     *        shifted position
     * @param v
     *        shifted velocity
     * @exception PatriusException
     *            if the underlying force models cannot compute the acceleration
     */
    private void computeShiftedAcceleration(final AccelerationRetriever retriever, final SpacecraftState s,
                                            final Vector3D p, final Vector3D v) throws PatriusException {
        final Orbit shiftedORbit = new CartesianOrbit(new PVCoordinates(p, v), s.getFrame(), s.getDate(), s.getMu());
        retriever.initDerivatives(null, shiftedORbit);
        this.forceModel
            .addContribution(
                new SpacecraftState(shiftedORbit, s.getAttitudeForces(), s.getAttitudeEvents(), s
                    .getAdditionalStates()), retriever);
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s,
                              final double[][] dAccdPos, final double[][] dAccdVel) throws PatriusException {

        if (!(this.forceModel instanceof GradientModel)) {
            // The model does not have gradient information, throw an exception
            throw new PatriusException(PatriusMessages.NOT_GRADIENT_MODEL);
        }
        final GradientModel gradientModel = (GradientModel) this.forceModel;

        if (gradientModel.computeGradientPosition() || gradientModel.computeGradientVelocity()) {
            // estimate the scalar velocity step, assuming energy conservation
            // and hence differentiating equation V = sqrt(mu (2/r - 1/a))
            final PVCoordinates pv = s.getPVCoordinates();
            final Vector3D p0 = pv.getPosition();
            final Vector3D v0 = pv.getVelocity();
            final double r2 = p0.getNormSq();

            // compute df/dy where f is the ODE and y is the CARTESIAN state array
            this.computeShiftedAcceleration(this.nominal, s, p0, v0);

            if (gradientModel.computeGradientPosition()) {
                // jacobian with respect to position
                // Euler forward differences
                this.computeShiftedAcceleration(this.shifted, s,
                    new Vector3D(p0.getX() + this.hPos, p0.getY(), p0.getZ()), v0);
                dAccdPos[0][0] += (this.shifted.getX() - this.nominal.getX()) / this.hPos;
                dAccdPos[1][0] += (this.shifted.getY() - this.nominal.getY()) / this.hPos;
                dAccdPos[2][0] += (this.shifted.getZ() - this.nominal.getZ()) / this.hPos;

                this.computeShiftedAcceleration(this.shifted, s,
                    new Vector3D(p0.getX(), p0.getY() + this.hPos, p0.getZ()), v0);
                dAccdPos[0][1] += (this.shifted.getX() - this.nominal.getX()) / this.hPos;
                dAccdPos[1][1] += (this.shifted.getY() - this.nominal.getY()) / this.hPos;
                dAccdPos[2][1] += (this.shifted.getZ() - this.nominal.getZ()) / this.hPos;

                this.computeShiftedAcceleration(this.shifted, s, new Vector3D(p0.getX(), p0.getY(), p0.getZ()
                    + this.hPos), v0);
                dAccdPos[0][2] += (this.shifted.getX() - this.nominal.getX()) / this.hPos;
                dAccdPos[1][2] += (this.shifted.getY() - this.nominal.getY()) / this.hPos;
                dAccdPos[2][2] += (this.shifted.getZ() - this.nominal.getZ()) / this.hPos;
            }

            if (gradientModel.computeGradientVelocity()) {
                final double hVel = s.getMu() * this.hPos / (v0.getNorm() * r2);
                // jacobian with respect to velocity
                // Euler forward differences
                this.computeShiftedAcceleration(this.shifted, s, p0,
                    new Vector3D(v0.getX() + hVel, v0.getY(), v0.getZ()));
                dAccdVel[0][0] += (this.shifted.getX() - this.nominal.getX()) / this.hPos;
                dAccdVel[1][0] += (this.shifted.getY() - this.nominal.getY()) / this.hPos;
                dAccdVel[2][0] += (this.shifted.getZ() - this.nominal.getZ()) / this.hPos;

                this.computeShiftedAcceleration(this.shifted, s, p0,
                    new Vector3D(v0.getX(), v0.getY() + hVel, v0.getZ()));
                dAccdVel[0][1] += (this.shifted.getX() - this.nominal.getX()) / this.hPos;
                dAccdVel[1][1] += (this.shifted.getY() - this.nominal.getY()) / this.hPos;
                dAccdVel[2][1] += (this.shifted.getZ() - this.nominal.getZ()) / this.hPos;

                this.computeShiftedAcceleration(this.shifted, s, p0, new Vector3D(v0.getX(), v0.getY(), v0.getZ()
                    + hVel));
                dAccdVel[0][2] += (this.shifted.getX() - this.nominal.getX()) / this.hPos;
                dAccdVel[1][2] += (this.shifted.getY() - this.nominal.getY()) / this.hPos;
                dAccdVel[2][2] += (this.shifted.getZ() - this.nominal.getZ()) / this.hPos;
            }
        }

        // No result to return
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        // It is not necessary to check if the input parameter is supported
        // Jacobianizer is class with package visibility
        // and this method is only called by PartialDerivativesEquations with input param supported.
        final double hP = this.hParam.get(param.hashCode());
        this.nominal.initDerivatives(null, s.getOrbit());
        this.forceModel.addContribution(s, this.nominal);

        final double paramValue = param.getValue();
        param.setValue(paramValue + hP);
        this.shifted.initDerivatives(null, s.getOrbit());
        this.forceModel.addContribution(s, this.shifted);
        param.setValue(paramValue);

        dAccdParam[0] += (this.shifted.getX() - this.nominal.getX()) / hP;
        dAccdParam[1] += (this.shifted.getY() - this.nominal.getY()) / hP;
        dAccdParam[2] += (this.shifted.getZ() - this.nominal.getZ()) / hP;
    }

    /** Internal class for retrieving accelerations. */
    private static class AccelerationRetriever implements TimeDerivativesEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = 6410400549499020323L;

        /** Stored acceleration. */
        private final double[] acceleration;

        /** Current orbit. */
        private Orbit orbit;

        /**
         * Simple constructor.
         */
        protected AccelerationRetriever() {
            this.acceleration = new double[3];
            this.orbit = null;
        }

        /**
         * Get X component of acceleration.
         * 
         * @return X component of acceleration
         */
        public double getX() {
            return this.acceleration[0];
        }

        /**
         * Get Y component of acceleration.
         * 
         * @return Y component of acceleration
         */
        public double getY() {
            return this.acceleration[1];
        }

        /**
         * Get Z component of acceleration.
         * 
         * @return Z component of acceleration
         */
        public double getZ() {
            return this.acceleration[2];
        }

        /** {@inheritDoc} */
        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit) {

            this.acceleration[0] = 0;
            this.acceleration[1] = 0;
            this.acceleration[2] = 0;
            this.orbit = currentOrbit;

        }

        /** {@inheritDoc} */
        @Override
        public void addKeplerContribution(final double mu) {
            final Vector3D position = this.orbit.getPVCoordinates().getPosition();
            final double r2 = position.getNormSq();
            final double coeff = -mu / (r2 * MathLib.sqrt(r2));
            this.acceleration[0] += coeff * position.getX();
            this.acceleration[1] += coeff * position.getY();
            this.acceleration[2] += coeff * position.getZ();
        }

        /** {@inheritDoc} */
        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            this.acceleration[0] += x;
            this.acceleration[1] += y;
            this.acceleration[2] += z;
        }

        /** {@inheritDoc} */
        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame) throws PatriusException {
            final Transform t = frame.getTransformTo(this.orbit.getFrame(), this.orbit.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            this.addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
            // we don't compute (yet) the mass part of the Jacobian, we just ignore this
        }

    }
}
