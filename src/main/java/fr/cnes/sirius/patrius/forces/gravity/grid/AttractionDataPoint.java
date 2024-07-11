/**
 * Copyright 2021-2021 CNES
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Attraction data: 3D acceleration and potential for one grid point. Grid point can be expressed either in 3D
 * coordinates or in spherical coordinates.
 * <p>
 * This class is to be used in conjunction with {@link GridAttractionModel} for attraction force defined by a grid.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
public class AttractionDataPoint implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 583118511449064475L;

    /** Position. */
    private final Vector3D position;

    /** Position as spherical coordinates (kept in order to avoid modulo and round-off issues). */
    private final SphericalCoordinates coordinates;

    /** XYZ acceleration. */
    private final Vector3D acceleration;

    /** Potential. */
    private final double potential;

    /**
     * Constructor with 3D position.
     * @param position position
     * @param acceleration XYZ acceleration at position
     * @param potential potential at position
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: performances
    public AttractionDataPoint(final Vector3D position,
            final Vector3D acceleration,
            final double potential) {
        this.position = position;
        this.acceleration = acceleration;
        this.potential = potential;
        // Unused
        this.coordinates = null;
    }

    /**
     * Constructor with spherical coordinates.
     * @param coordinates spherical coordinates
     * @param acceleration XYZ acceleration
     * @param potential potential
     */
    public AttractionDataPoint(final SphericalCoordinates coordinates,
            final Vector3D acceleration,
            final double potential) {
        this.position = coordinates.getCartesianCoordinates();
        this.coordinates = coordinates;
        this.acceleration = acceleration;
        this.potential = potential;
    }

    /**
     * Returns the position.
     * @return the position
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Returns the spherical coordinates.
     * @return the spherical coordinates or null if unused
     */
    public SphericalCoordinates getSphericalCoordinates() {
        return coordinates;
    }

    /**
     * Returns the XYZ acceleration.
     * @return the XYZ acceleration
     */
    public Vector3D getAcceleration() {
        return acceleration;
    }

    /**
     * Returns the potential.
     * @return the potential
     */
    public double getPotential() {
        return potential;
    }
}
