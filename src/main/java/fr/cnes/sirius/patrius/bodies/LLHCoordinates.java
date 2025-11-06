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
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.14:OPENFD-310:22/08/2024: [PATRIUS] Attribut "name" dans LLHCoordinates
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class aims at gathering in one single object the three coordinates latitude, longitude and height, and the
 * associated coordinates system in which they are expressed. The user can define a name for the coordinates by using
 * the dedicated constructeur
 * {@link LLHCoordinates#LLHCoordinates(LLHCoordinatesSystem, double, double, double, String)}, otherwise the default
 * value for the corresponding attribute is an empty String.
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

    /** Name of coordinates. */
    private final String name;

    /**
     * Constructor. This constructor initializes name attribute with an empty String, to initialize this attribute with
     * a user-defined value the constructor
     * {@link LLHCoordinates#LLHCoordinates(LLHCoordinatesSystem, double, double, double, String)} should be used
     * instead.
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
        this.name = "";
    }

    /**
     * Constructor with name option. This constructor enables the initialization of name attribute with a user-defined
     * name.
     *
     * @param coordSystem
     *        coordinates system in which latitude, longitude and height coordinates are expressed
     * @param latitude
     *        latitude coordinate
     * @param longitude
     *        longitude coordinate
     * @param height
     *        height coordinate (signed value)
     * @param name
     *        name of coordinates
     */
    public LLHCoordinates(final LLHCoordinatesSystem coordSystem, final double latitude, final double longitude,
                          final double height, final String name) {
        this.coordSystem = coordSystem;
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
        this.name = name;
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

    /**
     * Getter for the name of the coordinates. It returns an empty String if the user created object without using
     * constructor with name option.
     * 
     * @return the name of the coordinates.
     */
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String strReturn;
        if (this.name.isEmpty()) {
            strReturn = String.format("%s={lat=%s, long=%s}rad, %s=%sm", this.coordSystem.getLatLongSystemLabel(),
                this.latitude, this.longitude, this.coordSystem.getHeightSystemLabel(), this.height);
        } else {
            strReturn = String.format("%s={lat=%s, long=%s}rad, %s=%sm, %s", this.coordSystem.getLatLongSystemLabel(),
                this.latitude, this.longitude, this.coordSystem.getHeightSystemLabel(), this.height, this.name);
        }
        return strReturn;
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
                    && Double.doubleToLongBits(this.height) == Double.doubleToLongBits(other.height)
                    && Objects.equals(this.name, other.name);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.coordSystem, this.latitude, this.longitude, this.height, this.name);
    }
}
