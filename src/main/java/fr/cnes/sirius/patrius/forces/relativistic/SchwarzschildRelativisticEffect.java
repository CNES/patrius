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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
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
 * Computation of the relativistic Schwarzschild effect.
 * <p>
 * This is the 1st order relativistic effect.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author rodriguest
 * 
 * @version $Id: SchwarzschildRelativisticEffect.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.2
 * 
 */
public class SchwarzschildRelativisticEffect extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 1811756242976899848L;

    /** -2 */
    private static final double C_N2 = -2.;

    /** -3 */
    private static final double C_N3 = -3.;

    /** -4 */
    private static final double C_N4 = -4;

    /** Central body attraction coefficient. */
    private final double bodymu;

    /** True if acceleration partial derivatives wrt position have to be computed. */
    private final boolean computePartialDerivativesWrtPos;

    /** True if acceleration partial derivatives wrt velocity have to be computed. */
    private final boolean computePartialDerivativesWrtVel;

    /**
     * Constructor.
     * 
     * @param mu
     *        central body attraction coefficient
     * @param computePartialDerivativesPos
     *        if partial derivatives wrt position have to be computed
     * @param computePartialDerivativesVel
     *        if partial derivatives wrt velocity have to be computed
     */
    public SchwarzschildRelativisticEffect(final double mu, final boolean computePartialDerivativesPos,
                                           final boolean computePartialDerivativesVel) {
        super();
        this.bodymu = mu;
        this.computePartialDerivativesWrtPos = computePartialDerivativesPos;
        this.computePartialDerivativesWrtVel = computePartialDerivativesVel;
    }

    /**
     * Constructor with partial derivative computation by default.
     * 
     * @param mu
     *        central body attraction coefficient
     */
    public SchwarzschildRelativisticEffect(final double mu) {
        this(mu, true, true);
    }

    /** {@inheritDoc}. */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        // Compute acceleration in inertial frame
        final Vector3D acceleration = this.computeAcceleration(s);
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        // Verify that the spacecraftFrame is pseudo-inertial
        if (!s.getFrame().isPseudoInertial()) {
            // If frame is not pseudo-inertial, an exception is thrown
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
        // Constants
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Get position and velocity
        final Vector3D position = s.getPVCoordinates().getPosition();
        final Vector3D velocity = s.getPVCoordinates().getVelocity();

        // Compute some useful values
        final double r = position.getNorm();
        final double r3 = r * r * r;
        final double v2 = velocity.getNormSq();
        final double factor = this.bodymu / (c2 * r3);
        final double dotProduct = velocity.dotProduct(position);

        // Compute Schwarzschild acceleration in propagation frame
        return new Vector3D(factor * (MathLib.divide(4. * this.bodymu, r) - v2), position,
            factor * 4. * dotProduct, velocity);
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
            final double r3 = r * r2;
            final double v2 = velocity.getNormSq();
            final double factor = this.bodymu / (c2 * r3);

            // Map vectors to arrays
            final double[] pos = position.toArray();
            final double[] vel = velocity.toArray();

            // Partial derivatives wrt position
            if (this.computeGradientPosition()) {
                // Compute acceleration
                final double[] acc = this.computeAcceleration(s).toArray();

                final double coef = factor * (MathLib.divide(4. * this.bodymu, r) - v2);
                for (int i = 0; i < 3; i++) {
                    dAccdPos[i][i] += coef;
                    for (int j = 0; j < 3; j++) {
                        dAccdPos[i][j] += (MathLib.divide(C_N3 * pos[j], r2)) * acc[i]
                                + factor * ((MathLib.divide(C_N4 * this.bodymu, r3)) * pos[j] * pos[i]
                                + 4. * vel[j] * vel[i]);
                    }
                }
            }

            // Partial derivatives wrt velocity
            if (this.computeGradientVelocity()) {
                // Compute (pos . vel)
                final double dotProduct = position.dotProduct(velocity);
                final double coef = 4. * factor * dotProduct;

                for (int i = 0; i < 3; i++) {
                    dAccdVel[i][i] += coef;
                    for (int j = 0; j < 3; j++) {
                        dAccdVel[i][j] += factor * (C_N2 * vel[j] * pos[i] + 4. * pos[j] * vel[i]);
                    }
                }
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
