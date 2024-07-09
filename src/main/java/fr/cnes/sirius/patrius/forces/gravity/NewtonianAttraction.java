/**
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
 */
/* Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Force model for Newtonian central body attraction.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to the <b>central
 * attraction coefficient</b>.
 * </p>
 * 
 * @author Luc Maisonobe
 */

public class NewtonianAttraction extends JacobiansParameterizable implements ForceModel, GradientModel,
    AttractionModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -7754312556095545327L;

    /** Central attraction coefficient parameter. */
    private Parameter paramMu = null;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /** Multiplicative coefficient. */
    private double k;

    /**
     * Simple constructor.
     * 
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     */
    public NewtonianAttraction(final double mu) {
        this(mu, true);
    }

    /**
     * Simple constructor.
     * 
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     */
    public NewtonianAttraction(final double mu, final boolean computePD) {
        this(new Parameter(MU, mu), computePD);
    }

    /**
     * Simple constructor using {@link Parameter}.
     * 
     * @param mu
     *        parameter representing central attraction coefficient (m^3/s^2)
     */
    public NewtonianAttraction(final Parameter mu) {
        this(mu, true);
    }

    /**
     * Simple constructor using {@link Parameter}.
     * 
     * @param mu
     *        parameter representing central attraction coefficient (m^3/s^2)
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     */
    public NewtonianAttraction(final Parameter mu, final boolean computePD) {
        super();
        this.addJacobiansParameter(mu);
        this.enrichParameterDescriptors();
        this.paramMu = mu;
        this.computePartialDerivativesWrtPosition = computePD;
        this.k = 1.;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        adder.addKeplerContribution(this.paramMu.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return this.paramMu.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        return this.computeAcceleration(s.getPVCoordinates(), s.getFrame(), s.getDate());
    }

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we do not want to set up an instance of the SpacecraftState object
     * to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * 
     * <p>
     * (see Story #V82 and Feature #34 on https://www.orekit.org/forge/issues/34)
     * </p>
     * 
     * <p>
     * Out of the validation context, one must use the method Vector3D computeAcceleration(final SpacecraftState s)
     * </p>
     * 
     * @param pv
     *        PV coordinates of the spacecraft
     * @param frame
     *        frame in which the acceleration is computed
     * @param date
     *        date
     * @throws PatriusException
     *         if an Orekit error occurs
     * 
     * @return acceleration vector
     * 
     */
    public Vector3D computeAcceleration(final PVCoordinates pv, final Frame frame,
                                        final AbsoluteDate date) throws PatriusException {
        // velocity derivative is Newtonian acceleration
        final Vector3D position = pv.getPosition();
        final double r2 = position.getNormSq();
        final double coeff = -this.paramMu.getValue() / (r2 * MathLib.sqrt(r2)) * this.k;
        final double x = coeff * position.getX();
        final double y = coeff * position.getY();
        final double z = coeff * position.getZ();
        return new Vector3D(x, y, z);
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // Only derivative wrt position
            // derivative wrt velocity is null
            final Vector3D position = s.getPVCoordinates().getPosition();
            final double r2 = position.getNormSq();
            // Acceleration
            final Vector3D acceleration = new Vector3D(-this.paramMu.getValue() / (r2 * MathLib.sqrt(r2)), position);
            // Square data
            final double x2 = position.getX() * position.getX();
            final double y2 = position.getY() * position.getY();
            final double z2 = position.getZ() * position.getZ();
            final double xy = position.getX() * position.getY();
            final double yz = position.getY() * position.getZ();
            final double zx = position.getZ() * position.getX();
            final double prefix = -Vector3D.dotProduct(acceleration, position) / (r2 * r2) * this.k;
            // the only non-null contribution for this force is on dAcc/dPos
            dAccdPos[0][0] += prefix * (2 * x2 - y2 - z2);
            dAccdPos[0][1] += prefix * 3 * xy;
            dAccdPos[0][2] += prefix * 3 * zx;
            dAccdPos[1][0] += prefix * 3 * xy;
            dAccdPos[1][1] += prefix * (2 * y2 - z2 - x2);
            dAccdPos[1][2] += prefix * 3 * yz;
            dAccdPos[2][0] += prefix * 3 * zx;
            dAccdPos[2][1] += prefix * 3 * yz;
            dAccdPos[2][2] += prefix * (2 * z2 - x2 - y2);
        }
    }

    /**
     * {@inheritDoc}.
     * {@link NewtonianAttraction#MU} --> derivatives with respect to the central attraction coefficient <br>
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }
        final Vector3D position = s.getPVCoordinates().getPosition();
        final double r2 = position.getNormSq();
        final double factor = -1.0 / (r2 * MathLib.sqrt(r2)) * this.k;
        dAccdParam[0] += factor * position.getX();
        dAccdParam[1] += factor * position.getY();
        dAccdParam[2] += factor * position.getZ();
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double getMultiplicativeFactor() {
        return this.k;
    }

    /** {@inheritDoc} */
    @Override
    public void setMultiplicativeFactor(final double coefficient) {
        this.k = coefficient;
    }
}
