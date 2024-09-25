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
 * HISTORY
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class aims at gathering in one single object the three coordinates latitude, longitude and height, and the
 * associated coordinates system in which they are expressed.
 *
 * @author Alice Latourte
 */
public class LLHCoordinates implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6190598226474389874L;

    /** Coordinates system. */
    private final LLHCoordinatesSystem coordSystem;

    /** Latitude expressed in the coordinates system. */
    private final double latitude;

    /** Longitude expressed in the coordinates system. */
    private final double longitude;

    /**
     * Height expressed in the coordinates system: this is a signed value, positive if outside the shape, negative if
     * inside the shape.
     */
    private final double height;

    /**
     * Constructor.
     *
     * @param coordSystem
     *        coordinates system in which latitude, longitude and height coordinates are expressed
     * @param latitude
     *        latitude coordinate
     * @param longitude
     *        longitude coordinate
     * @param height
     *        height coordinate (signed value)
     */
    public LLHCoordinates(final LLHCoordinatesSystem coordSystem, final double latitude, final double longitude,
                          final double height) {
        this.coordSystem = coordSystem;
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
    }

    /**
     * Getter for the used LLH coordinates system.
     *
     * @return the coordinates system
     */
    public LLHCoordinatesSystem getLLHCoordinatesSystem() {
        return this.coordSystem;
    }

    /**
     * Getter for the latitude.
     *
     * @return the latitude
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * Getter for the longitude.
     *
     * @return the longitude
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * Getter for the height in meters with respect to the shape surface. Following the convention, it can be the
     * distance to shape (if normal height) or the radial height (if radial height). This is a signed value.
     * <p>
     * If the used height system is NORMAL, a positive value means the point is outside the shape, a negative value
     * means the point is inside the shape.
     * </p>
     *
     * @return the height in meters
     */
    public double getHeight() {
        return this.height;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s={lat=%s, long=%s}rad, %s=%sm", this.coordSystem.getLatLongSystemLabel(),
            this.latitude, this.longitude, this.coordSystem.getHeightSystemLabel(), this.height);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            // Identity
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final LLHCoordinates other = (LLHCoordinates) object;

            isEqual = Objects.equals(this.coordSystem, other.coordSystem)
                    && Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(other.latitude)
                    && Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(other.longitude)
                    && Double.doubleToLongBits(this.height) == Double.doubleToLongBits(other.height);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.coordSystem, this.latitude, this.longitude, this.height);
    }
}
