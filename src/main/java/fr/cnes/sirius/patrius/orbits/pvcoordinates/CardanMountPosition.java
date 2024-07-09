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
 *              Cardan mount (position)
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton
 * 
 * @version $Id: CardanMountPosition.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class CardanMountPosition implements Position {

    /** Fixed position at origin (p is zero vector). */
    public static final CardanMountPosition ZERO = new CardanMountPosition(0., 0., 0.);

    /** Serializable UID. */
    private static final long serialVersionUID = -3033892402533461901L;

    /** Angle of the roation around x axis. */
    private final double x;
    /** Angle of the rotation around y' axis (image of y axis by the rotation around x axis). */
    private final double y;
    /** Distance from the origin of the frame. */
    private final double r;

    /**
     * Build Cardan mount.
     * 
     * @param xAngle
     *        angle of rotation around the local North axis counted clockwise from the
     *        zenith and expressed in radian (between {@code -PI} and {@code PI})
     * @param yAngle
     *        angle of rotation around y' axis (image of the West axis by the previous rotation) counted
     *        clockwise from the y' axis and expressed in radian (between {@code -PI/2} and {@code PI/2})
     * @param range
     *        distance from the origin of the topocentric frame
     */
    public CardanMountPosition(final double xAngle, final double yAngle, final double range) {
        this.x = xAngle;
        this.y = yAngle;
        this.r = range;
    }

    /**
     * Get the x angle.
     * 
     * @return x angle
     */
    public double getXangle() {
        return this.x;
    }

    /**
     * Get the y angle.
     * 
     * @return y angle
     */
    public double getYangle() {
        return this.y;
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
        return new Vector3D(this.x, this.y, this.r);
    }

    /**
     * Produces the following String representation of the Cardan mount :
     * (x angle, y angle, range).
     * 
     * @return string representation of this position
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
            append(this.x).append(comma).
            append(this.y).append(comma).
            append(this.r).append(")}").toString();
    }
}
