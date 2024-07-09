/**
 *
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
 *
 * @history creation 18/10/2011
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * @description
 *              <p>
 *              Topocentric coordinates
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton
 * 
 * @version $Id: TopocentricPV.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class TopocentricPV implements PV {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 6047068671778435002L;

    /** Elevation angle. */
    private final double e;
    /** Azimuth angle. */
    private final double a;
    /** Distance from the origin. */
    private final double r;
    /** Elevation rate. */
    private final double eRate;
    /** Azimuth rate. */
    private final double aRate;
    /** Range rate. */
    private final double rRate;

    /**
     * Build topocentric coordinates.
     * To be consistent with the TopocentricFrame, the convention concerning the azimuth
     * is the same ie azimuth angles are counted clockwise from the local North.
     * 
     * @param elevation
     *        elevation angle in radian, between {@code -PI/2} and {@code PI/2}
     * @param azimuth
     *        azimuth angle in radian, between {@code 0} and {@code 2PI}
     * @param range
     *        distance from the origin of the topocentric frame
     * @param elevationRate
     *        elevation rate in rad/s
     * @param azimuthRate
     *        azimuth rate in rad/s
     * @param rangeRate
     *        range rate in m/s
     */
    public TopocentricPV(final double elevation, final double azimuth, final double range,
        final double elevationRate, final double azimuthRate, final double rangeRate) {
        this.e = elevation;
        this.a = azimuth;
        this.r = range;
        this.eRate = elevationRate;
        this.aRate = azimuthRate;
        this.rRate = rangeRate;
    }

    /**
     * Get the elevation angle.
     * 
     * @return elevation angle
     */
    public double getElevation() {
        return this.e;
    }

    /**
     * Get the azimuth angle.
     * 
     * @return azimuth angle
     */
    public double getAzimuth() {
        return this.a;
    }

    /**
     * Get the range.
     * 
     * @return the range
     */
    public double getRange() {
        return this.r;
    }

    /**
     * Get the elevation rate.
     * 
     * @return the elevation rate
     */
    public double getElevationRate() {
        return this.eRate;
    }

    /**
     * Get the azimuth rate.
     * 
     * @return the azimuth rate
     */
    public double getAzimuthRate() {
        return this.aRate;
    }

    /**
     * Get the range rate.
     * 
     * @return the range rate
     */
    public double getRangeRate() {
        return this.rRate;
    }

    /**
     * Get the Topocentric position.
     * 
     * @return the Topocentric position.
     */
    public TopocentricPosition getTopocentricPosition() {
        return new TopocentricPosition(this.e, this.a, this.r);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition() {
        return new Vector3D(this.e, this.a, this.r);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity() {
        return new Vector3D(this.eRate, this.aRate, this.rRate);
    }

    /**
     * Produces the following String representation of the Topocentric coordinates :
     * (elevation, azimuth, range, elevation rate, azimuth rate, range rate).
     * 
     * @return string representation of this position/velocity
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
            append(this.e).append(comma).
            append(this.a).append(comma).
            append(this.r).append("), V(").
            append(this.eRate).append(comma).
            append(this.aRate).append(comma).
            append(this.rRate).append(")}").toString();
    }
}
