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
 * @history creation 18/10/2011
 *
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import java.util.Objects;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * @description <p>
 *              Topocentric position coordinates
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton
 * 
 * @version $Id: TopocentricPosition.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class TopocentricPosition implements Position {

    /** Fixed position at origin (p is zero vector). */
    public static final TopocentricPosition ZERO = new TopocentricPosition(0., 0., 0.);

    /** Serializable UID. */
    private static final long serialVersionUID = -5115813588245021791L;

    /** Elevation angle. */
    private final double e;
    /** Azimuth angle. */
    private final double a;
    /** Distance from the origin. */
    private final double r;

    /**
     * Build topocentic coordinates. To be consistent with the TopocentricFrame, the convention concerning the azimuth
     * is the same ie azimuth angles are counted clockwise from the local North.
     * 
     * @param elevation
     *        elevation angle in radian (between {@code -PI/2} and {@code PI/2})
     * @param azimuth
     *        azimuth angle in radian (between {@code 0} and {@code 2PI})
     * @param range
     *        distance from the origin of the topocentric frame
     */
    public TopocentricPosition(final double elevation, final double azimuth, final double range) {
        this.e = elevation;
        this.a = azimuth;
        this.r = range;
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

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition() {
        return new Vector3D(this.e, this.a, this.r);
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
            final TopocentricPosition other = (TopocentricPosition) object;

            // Evaluate the attitudes components
            isEqual = Double.doubleToLongBits(this.e) == Double.doubleToLongBits(other.e)
                    && Double.doubleToLongBits(this.a) == Double.doubleToLongBits(other.a)
                    && Double.doubleToLongBits(this.r) == Double.doubleToLongBits(other.r);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.e, this.a, this.r);
    }

    /**
     * Produces the following String representation of the topocentric coordinates : (elevation, azimuth, range).
     * 
     * @return string representation of this position
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").append(this.e).append(comma).append(this.a).append(comma)
            .append(this.r)
            .append(")}").toString();
    }
}
