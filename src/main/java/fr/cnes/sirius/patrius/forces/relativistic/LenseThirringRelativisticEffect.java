/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history Created 16/02/2016
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:529:23/02/2016: relativistic effects
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.relativistic;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computation of the relativistic Lense-Thirring effect - IERS2003 standard (applies to Earth only).
 * <p>
 * This is the 3rd order relativistic effect.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author rodriguest
 * 
 * @version $Id: LenseThirringRelativisticEffect.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.2
 * 
 */
public class LenseThirringRelativisticEffect extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 626504608796493978L;

    /** J constant for Earth. */
    private static final double J_EARTH = 9.8E8;

    /** -3 */
    private static final double C_N3 = -3.;
    // private static final double J_EARTH = 0.4 * 0.6378137E+07 * 0.6378137E+07 * 0.72921151467E-04; // ZOOM value

    /** -2. */
    private static final double C_N2 = -2;

    /** Central body attraction coefficient. */
    private final double bodymu;

    /** Frame defining pole of central body. */
    private final Frame bodyframe;

    /** True if acceleration partial derivatives wrt position have to be computed. */
    private final boolean computePartialDerivativesWrtPos;

    /** True if acceleration partial derivatives wrt velocity have to be computed. */
    private final boolean computePartialDerivativesWrtVel;

    /**
     * Constructor.
     * 
     * @param mu
     *        central body attraction coefficient
     * @param frame
     *        frame defining pole of central body
     * @param computePartialDerivativesPos
     *        if partial derivatives wrt position have to be computed
     * @param computePartialDerivativesVel
     *        if partial derivatives wrt velocity have to be computed
     */
    public LenseThirringRelativisticEffect(final double mu, final Frame frame,
                                           final boolean computePartialDerivativesPos,
                                           final boolean computePartialDerivativesVel) {
        super();
        this.bodymu = mu;
        this.bodyframe = frame;
        this.computePartialDerivativesWrtPos = computePartialDerivativesPos;
        this.computePartialDerivativesWrtVel = computePartialDerivativesVel;
    }

    /**
     * Constructor and with partial derivative computation by default.
     * 
     * @param mu
     *        central body attraction coefficient
     * @param frame
     *        frame defining pole of central body
     */
    public LenseThirringRelativisticEffect(final double mu, final Frame frame) {
        this(mu, frame, true, true);
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        // Constants
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Get position and velocity
        final Vector3D position = s.getPVCoordinates().getPosition();
        final Vector3D velocity = s.getPVCoordinates().getVelocity();

        // Compute some useful values
        final double r2 = position.getNormSq();
        final double r = MathLib.sqrt(r2);
        final double r3 = r * r2;
        final double factor = 2. * this.bodymu / (c2 * r3);

        // Angular momentum
        final Vector3D j = this.computeJ(s);

        final double rDotJ = position.dotProduct(j);
        final Vector3D vect1 = new Vector3D(MathLib.divide(3. * rDotJ, r2), position, -1., j);

        // Compute Lense-Thirring acceleration
        return new Vector3D(factor, vect1.crossProduct(velocity));
    }

    /** {@inheritDoc}. */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        // compute acceleration in inertial frame
        final Vector3D acceleration = this.computeAcceleration(s);
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    /**
     * @throws PatriusException
     *         Simple method to compute the angular momentum of central body according the model J.
     * @param s
     *        current state information: date, kinematics, attitude
     * @return the angular momentum
     */
    private Vector3D computeJ(final SpacecraftState s) throws PatriusException {
        // Get the rotation axis of central body
        final Transform t = this.bodyframe.getTransformTo(s.getFrame(), s.getDate());
        final Vector3D uzCC = t.transformVector(Vector3D.PLUS_K);
        return uzCC.scalarMultiply(J_EARTH);
    }

    /** {@inheritDoc}. */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPos;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return this.computePartialDerivativesWrtVel;
    }

    /** {@inheritDoc}. */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition() || this.computeGradientVelocity()) {

            // Constants
            final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

            // Get position and velocity
            final Vector3D position = s.getPVCoordinates().getPosition();
            final Vector3D velocity = s.getPVCoordinates().getVelocity();

            // Compute some useful values
            final double r2 = position.getNormSq();
            final double r = MathLib.sqrt(r2);
            final double r3 = r2 * r;
            // Angular momentum
            final Vector3D j = this.computeJ(s);
            final double rDotJ = position.dotProduct(j);

            if (this.computeGradientPosition()) {
                // Compute pos ^ vel
                final Vector3D cross = position.crossProduct(velocity);

                // Turn vectors to arrays
                final double[] pos = position.toArray();
                final double[] vel = velocity.toArray();
                final double[] acc = this.computeAcceleration(s).toArray();
                final double[] crossArray = cross.toArray();
                final double[] jArray = j.toArray();
                final double[][] dcrossArray = {
                    { 0., vel[2], -vel[1] },
                    { -vel[2], 0., vel[0] },
                    { vel[1], -vel[0], 0. },
                };

                final double factor = 6. * this.bodymu / (c2 * r2 * r3);

                // dAcc / dPos
                for (int i = 0; i < 3; i++) {
                    for (int k = 0; k < 3; k++) {
                        dAccdPos[i][k] += MathLib.divide(C_N3 * pos[k] * acc[i], r2)
                                + factor * ((jArray[k] - MathLib.divide(2. * pos[k] * rDotJ, r2)) * crossArray[i]
                                + rDotJ * dcrossArray[i][k]);
                    }
                }
            }

            if (this.computeGradientVelocity()) {
                final double factor = this.bodymu / (c2 * r3);
                final Vector3D omegaLT = new Vector3D(MathLib.divide(3. * factor * rDotJ, r2), position, -factor, j);

                // dAcc / dVel
                dAccdVel[0][1] = C_N2 * omegaLT.getZ();
                dAccdVel[0][2] = 2 * omegaLT.getY();
                dAccdVel[1][0] = 2 * omegaLT.getZ();
                dAccdVel[1][2] = C_N2 * omegaLT.getX();
                dAccdVel[2][0] = C_N2 * omegaLT.getY();
                dAccdVel[2][1] = 2 * omegaLT.getX();
            }
        }
    }

    /**
     * {@inheritDoc}.
     * No parameter is supported by this force model.
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
