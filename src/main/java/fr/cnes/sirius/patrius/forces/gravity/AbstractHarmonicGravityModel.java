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
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a gravitational harmonic attraction model.
 * 
 * @author Alex Nardi
 * @since 4.11
 */
public abstract class AbstractHarmonicGravityModel extends AbstractGravityModel {

    /** Serial UID. */
    private static final long serialVersionUID = 4260816869246704237L;

    /**
     * Central term contribution (true if the central term is considered, false if not). The central term is considered
     * by default, so this boolean is true by default.
     */
    private boolean centralTermContribution = true;

    /**
     * Constructor.
     * 
     * @param bodyFrameIn bodyFrame
     * @param mu gravitational parameter
     */
    public AbstractHarmonicGravityModel(final Frame bodyFrameIn, final Parameter mu) {
        super(bodyFrameIn, mu);
    }

    /**
     * Compute the acceleration due to the central term of the gravitational attraction.
     * 
     * @param positionInBodyFrame
     *        the position expressed in the {@link #getBodyFrame() body frame}
     * @exception PatriusException if some specific error occurs
     * 
     * @return the central term acceleration in pos frame
     * 
     */
    protected Vector3D computeCentralTermAcceleration(final Vector3D positionInBodyFrame) throws PatriusException {
        final double r2 = positionInBodyFrame.getNormSq();
        final double coeff = -this.getMu() / (r2 * MathLib.sqrt(r2));
        final double x = coeff * positionInBodyFrame.getX();
        final double y = coeff * positionInBodyFrame.getY();
        final double z = coeff * positionInBodyFrame.getZ();

        return new Vector3D(x, y, z);
    }

    /**
     * Compute the acceleration due to the non-central terms of the gravitational attraction.
     * 
     * @param positionInBodyFrame
     *        the position expressed in the {@link #getBodyFrame() body frame}
     * @param date
     *        the date
     * @exception PatriusException if some specific error occurs
     * 
     * @return the non-central terms acceleration in the given frame
     */
    protected abstract Vector3D computeNonCentralTermsAcceleration(final Vector3D positionInBodyFrame,
                                                                   final AbsoluteDate date)
        throws PatriusException;

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final Vector3D positionInBodyFrame,
                                        final AbsoluteDate date) throws PatriusException {
        final Vector3D nonCentralAcceleration = computeNonCentralTermsAcceleration(positionInBodyFrame, date);
        final Vector3D totalAcceleration;
        if (isCentralTermContributionApplied()) {
            totalAcceleration = nonCentralAcceleration.add(computeCentralTermAcceleration(positionInBodyFrame));
        } else {
            totalAcceleration = nonCentralAcceleration;
        }
        return totalAcceleration;

    }

    /**
     * Compute acceleration derivatives with respect to the state parameters for the central term.
     * 
     * @param positionInBodyFrame position of the spacecraft in the body frame
     * @return acceleration derivatives with respect to position in the body frame
     * @exception PatriusException if derivatives cannot be computed
     */
    public double[][] computeCentralTermDAccDPos(final Vector3D positionInBodyFrame)
        throws PatriusException {
        // Only derivative wrt position
        // derivative wrt velocity is null
        final double r2 = positionInBodyFrame.getNormSq();
        // Acceleration
        final Vector3D acceleration = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), positionInBodyFrame);
        // Square data
        final double x2 = positionInBodyFrame.getX() * positionInBodyFrame.getX();
        final double y2 = positionInBodyFrame.getY() * positionInBodyFrame.getY();
        final double z2 = positionInBodyFrame.getZ() * positionInBodyFrame.getZ();
        final double xy = positionInBodyFrame.getX() * positionInBodyFrame.getY();
        final double yz = positionInBodyFrame.getY() * positionInBodyFrame.getZ();
        final double zx = positionInBodyFrame.getZ() * positionInBodyFrame.getX();
        final double prefix = -Vector3D.dotProduct(acceleration, positionInBodyFrame) / (r2 * r2);
        // the only non-null contribution for this force is on dAcc/dPos
        final double[][] dAccCentralDPosition = new double[3][3];
        dAccCentralDPosition[0][0] += prefix * (2 * x2 - y2 - z2);
        dAccCentralDPosition[0][1] += prefix * 3 * xy;
        dAccCentralDPosition[0][2] += prefix * 3 * zx;
        dAccCentralDPosition[1][0] += prefix * 3 * xy;
        dAccCentralDPosition[1][1] += prefix * (2 * y2 - z2 - x2);
        dAccCentralDPosition[1][2] += prefix * 3 * yz;
        dAccCentralDPosition[2][0] += prefix * 3 * zx;
        dAccCentralDPosition[2][1] += prefix * 3 * yz;
        dAccCentralDPosition[2][2] += prefix * (2 * z2 - x2 - y2);
        return dAccCentralDPosition;
    }

    /**
     * Compute acceleration derivatives with respect to the state parameters for the non-central terms.
     * 
     * @param positionInBodyFrame position of the spacecraft in the body frame
     * @param date date
     * @return acceleration derivatives with respect to position in the body frame
     * @exception PatriusException if derivatives cannot be computed
     */
    public abstract double[][] computeNonCentralTermsDAccDPos(final Vector3D positionInBodyFrame,
                                                              final AbsoluteDate date)
        throws PatriusException;

    /** {@inheritDoc}. */
    @Override
    public double[][] computeDAccDPos(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        // Non-central terms
        final double[][] dTotalAccDPosition = computeNonCentralTermsDAccDPos(positionInBodyFrame, date);
        if (isCentralTermContributionApplied()) {
            // Central
            final double[][] dCentralAccDPosition = computeCentralTermDAccDPos(positionInBodyFrame);

            for (int i = 0; i < dTotalAccDPosition.length; i++) {
                for (int j = 0; j < dTotalAccDPosition[0].length; j++) {
                    dTotalAccDPosition[i][j] += dCentralAccDPosition[i][j];
                }
            }
        }
        // Return result
        return dTotalAccDPosition;

    }

    /**
     * Get the boolean for the central term contribution (true if the central term is considered, false if not).
     *
     * @return the boolean for the central term contribution (true if the central term is considered, false if not)
     */
    public boolean isCentralTermContributionApplied() {
        return this.centralTermContribution;
    }

    /**
     * Set the boolean for the central term contribution (true if the central term is considered, false if not).
     *
     * @param centralTermContributionIn the boolean for the central term contribution (true if the central term is
     *        considered, false if not).
     */
    public void setCentralTermContribution(final boolean centralTermContributionIn) {
        this.centralTermContribution = centralTermContributionIn;
    }
}
