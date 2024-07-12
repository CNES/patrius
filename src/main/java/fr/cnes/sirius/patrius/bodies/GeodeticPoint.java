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
 */
/*
 * HISTORY
* VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 *END-HISTORY */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Point location relative to a 2D body surface.
 * Note : this object is not intrinsically linked to a {@link BodyShape}.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see BodyShape
 * @author Luc Maisonobe
 */
public class GeodeticPoint implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 7862466825590075399L;

    /** Latitude of the point (rad). */
    private final double latitude;

    /** Longitude of the point (rad). */
    private final double longitude;

    /** Altitude of the point (m). */
    private final double altitude;

    /** Zenith direction. */
    private transient Vector3D zenith;

    /** Nadir direction. */
    private transient Vector3D nadir;

    /** North direction. */
    private transient Vector3D north;

    /** South direction. */
    private transient Vector3D south;

    /** East direction. */
    private transient Vector3D east;

    /** West direction. */
    private transient Vector3D west;

    /** Name */
    private final String name;

    /**
     * Build a new instance.
     * 
     * @param latitudeIn of the point
     * @param longitudeIn longitude of the point
     * @param altitudeIn altitude of the point
     */
    public GeodeticPoint(final double latitudeIn, final double longitudeIn, final double altitudeIn) {
        this(latitudeIn, longitudeIn, altitudeIn, "Point");
    }

    /**
     * Build a new instance.
     * 
     * @param latitudeIn of the point
     * @param longitudeIn longitude of the point
     * @param altitudeIn altitude of the point
     * @param nameIn name of the point
     */
    public GeodeticPoint(final double latitudeIn, final double longitudeIn,
                         final double altitudeIn, final String nameIn) {
        this.latitude = MathUtils.normalizeAngle(latitudeIn, 0);
        this.longitude = MathUtils.normalizeAngle(longitudeIn, 0);
        this.altitude = altitudeIn;
        this.name = nameIn;
    }

    /**
     * Get the geodetic latitude. To get planetocentric latitude use {@link SphericalCoordinates} class instead of
     * {@link GeodeticPoint}.
     * 
     * @return latitude
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * Get the longitude.
     * 
     * @return longitude
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * Get the altitude.
     * 
     * @return altitude
     */
    public double getAltitude() {
        return this.altitude;
    }

    /**
     * Get the direction above the point, expressed in parent shape frame.
     * <p>
     * The zenith direction is defined as the normal to local horizontal plane.
     * </p>
     * 
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        if (this.zenith == null) {
            final double[] sincosLon = MathLib.sinAndCos(longitude);
            final double sinLon = sincosLon[0];
            final double cosLon = sincosLon[1];
            final double[] sincosLat = MathLib.sinAndCos(latitude);
            final double sinLat = sincosLat[0];
            final double cosLat = sincosLat[1];
            this.zenith = new Vector3D(cosLon * cosLat, sinLon * cosLat, sinLat);
        }
        return this.zenith;
    }

    /**
     * Get the direction below the point, expressed in parent shape frame.
     * <p>
     * The nadir direction is the opposite of zenith direction.
     * </p>
     * 
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        if (this.nadir == null) {
            this.nadir = this.getZenith().negate();
        }
        return this.nadir;
    }

    /**
     * Get the direction to the north of point, expressed in parent shape frame.
     * <p>
     * The north direction is defined in the horizontal plane (normal to zenith direction) and following the local
     * meridian.
     * </p>
     * 
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        if (this.north == null) {
            final double[] sincosLon = MathLib.sinAndCos(longitude);
            final double sinLon = sincosLon[0];
            final double cosLon = sincosLon[1];
            final double[] sincosLat = MathLib.sinAndCos(latitude);
            final double sinLat = sincosLat[0];
            final double cosLat = sincosLat[1];
            this.north = new Vector3D(-cosLon * sinLat, -sinLon * sinLat, cosLat);
        }
        return this.north;
    }

    /**
     * Get the direction to the south of point, expressed in parent shape frame.
     * <p>
     * The south direction is the opposite of north direction.
     * </p>
     * 
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        if (this.south == null) {
            this.south = this.getNorth().negate();
        }
        return this.south;
    }

    /**
     * Get the direction to the east of point, expressed in parent shape frame.
     * <p>
     * The east direction is defined in the horizontal plane in order to complete direct triangle (east, north, zenith).
     * </p>
     * 
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        if (this.east == null) {
            final double[] sincosLon = MathLib.sinAndCos(longitude);
            final double sin = sincosLon[0];
            final double cos = sincosLon[1];
            this.east = new Vector3D(-sin, cos, 0);
        }
        return this.east;
    }

    /**
     * Get the direction to the west of point, expressed in parent shape frame.
     * <p>
     * The west direction is the opposite of east direction.
     * </p>
     * 
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        if (this.west == null) {
            this.west = this.getEast().negate();
        }
        return this.west;
    }

    /**
     * Getter for the name of the point.
     * 
     * @return the name of the point.
     */
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s: name=%s, latitude=%s, longitude=%s, altitude=%s",
            this.getClass().getSimpleName(), this.getName(), this.getLatitude(), this.getLongitude(),
            this.getAltitude());
    }
}
