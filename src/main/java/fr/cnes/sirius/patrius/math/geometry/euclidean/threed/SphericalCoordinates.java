/**
 * Copyright 2011-2021 CNES
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
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * This class provides spherical coordinates (elevation, azimuth, norm) from a Vector3D.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * 
 * @since 4.7
 */
public class SphericalCoordinates implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -1878591178259516682L;

    /** Elevation of the point (rad). */
    private final double delta;

    /** Azimuth of the point (rad). */
    private final double alpha;

    /** Norm of the point (m). */
    private final double norm;

    /** Cartesian coordinates. */
    private transient Vector3D cartesianCoordinates;
    
    /**
     * Build a new instance. Angles are normalized around 0.
     * 
     * @param delta elevation &delta; of the point
     * @param alpha azimuth &alpha; of the point
     * @param norm norm of the point
     */
    public SphericalCoordinates(final double delta,
            final double alpha,
            final double norm) {
        this(delta, alpha, norm, true);
    }

    /**
     * Build a new instance.
     * 
     * @param delta elevation &delta; of the point
     * @param alpha azimuth &alpha; of the point
     * @param norm norm of the point
     * @param normalizeAngles true if angles should be centered around 0, false if kept as provided
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: optimization
    public SphericalCoordinates(final double delta,
            final double alpha,
            final double norm,
            final boolean normalizeAngles) {
        if (normalizeAngles) {
            this.delta = MathUtils.normalizeAngle(delta, 0);
            this.alpha = MathUtils.normalizeAngle(alpha, 0);
        } else {
            this.delta = delta;
            this.alpha = alpha;
        }
        this.norm = norm;
        this.cartesianCoordinates = null;
    }

    /**
     * Build a new instance.
     * 
     * @param vector vector in cartesian coordinates
     */
    public SphericalCoordinates(final Vector3D vector) {
        this.norm = vector.getNorm();
        this.delta = MathLib.asin(vector.getZ() / this.norm);
        this.alpha = MathLib.atan2(vector.getY(), vector.getX());
        this.cartesianCoordinates = vector;
    }

    /**
     * Returns the cartesian coordinates.
     * @return the cartesian coordinates
     */
    public Vector3D getCartesianCoordinates() {
        if (cartesianCoordinates == null) {
            final double[] sincosLon = MathLib.sinAndCos(alpha);
            final double sinLon = sincosLon[0];
            final double cosLon = sincosLon[1];
            final double[] sincosLat = MathLib.sinAndCos(delta);
            final double sinLat = sincosLat[0];
            final double cosLat = sincosLat[1];
            cartesianCoordinates = new Vector3D(cosLon * cosLat, sinLon * cosLat, sinLat).scalarMultiply(norm);
        }
        return cartesianCoordinates;
    }
    
    /**
     * Get the elevation &delta;.
     * 
     * @return elevation (&delta;), between -&pi;/2 and +&pi;/2
     */
    public double getDelta() {
        return this.delta;
    }

    /**
     * Get the azimuth &alpha;.
     * 
     * @return azimuth (&alpha;), between -&pi; and +&pi;
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * Get the norm.
     * 
     * @return the norm
     */
    public double getNorm() {
        return this.norm;
    }
}
