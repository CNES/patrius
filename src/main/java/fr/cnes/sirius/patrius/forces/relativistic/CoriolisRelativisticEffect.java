/**
 * 
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.relativistic;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computation of the relativistic Coriolis effect (Einstein-de-Sitter effect)
 * - IERS2003 standard (applies to Earth only).
 * <p>
 * This is the 2nd order relativistic effect.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author rodriguest
 * 
 * @version $Id: CoriolisRelativisticEffect.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.2
 * 
 */
public class CoriolisRelativisticEffect extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 3037029521564550509L;

    /** Sun central attraction coefficient. */
    private final double mu;

    /** Sun PV coordinates. */
    private final PVCoordinatesProvider pv;

    /** True if acceleration partial derivatives wrt velocity have to be computed. */
    private final boolean computePartialDerivativesWrtVel;

    /**
     * Constructor with partial derivative computation by default.
     * 
     * @param sunMu
     *        sun gravitational coefficient
     * @param sunPV
     *        sun PV coordinates provider
     */
    public CoriolisRelativisticEffect(final double sunMu, final PVCoordinatesProvider sunPV) {
        this(sunMu, sunPV, true);
    }

    /**
     * Constructor.
     * 
     * @param sunMu
     *        sun gravitational coefficient
     * @param sunPV
     *        sun PV coordinates provider
     * @param computePartialDerivativesVel
     *        if partial derivatives wrt velocity have to be computed
     */
    public CoriolisRelativisticEffect(final double sunMu, final PVCoordinatesProvider sunPV,
        final boolean computePartialDerivativesVel) {
        super();
        this.mu = sunMu;
        this.pv = sunPV;
        this.computePartialDerivativesWrtVel = computePartialDerivativesVel;
    }

    /** {@inheritDoc}. */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        // compute acceleration in inertial frame
        final Vector3D acceleration = this.computeAcceleration(s);
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        // Get velocity
        final Vector3D velocity = s.getPVCoordinates().getVelocity();

        // Compute Coriolis acceleration
        return this.computeOmega(s).crossProduct(velocity).scalarMultiply(2.);
    }

    /**
     * @throws PatriusException
     *         Simple method to compute the Coriolis angular velocity according the model.
     * @param s
     *        current state information: date, kinematics, attitude
     * @return the angular velocity
     */
    private Vector3D computeOmega(final SpacecraftState s) throws PatriusException {

        // Constants
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Sun PV
        final PVCoordinates sunPV = this.pv.getPVCoordinates(s.getDate(), s.getFrame());
        final Vector3D sunPos = sunPV.getPosition();
        final double sunDist = sunPos.getNorm();
        final double sunDist2 = sunDist * sunDist;
        final double sunDist3 = sunDist2 * sunDist;

        // Sun position/velocity
        final Vector3D sunVel = sunPV.getVelocity();

        // Coriolis angular velocity
        final double factor = -3 * this.mu / (2. * c2 * sunDist3);
        return new Vector3D(factor, sunVel.crossProduct(sunPos));
    }

    /** {@inheritDoc}. */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return false;
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

        // Only dAcc/dVel is not zero
        if (this.computeGradientVelocity()) {
            final Vector3D omega2 = this.computeOmega(s).scalarMultiply(2);
            dAccdVel[0][1] = -omega2.getZ();
            dAccdVel[0][2] = omega2.getY();
            dAccdVel[1][0] = omega2.getZ();
            dAccdVel[1][2] = -omega2.getX();
            dAccdVel[2][0] = -omega2.getY();
            dAccdVel[2][1] = omega2.getX();
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
