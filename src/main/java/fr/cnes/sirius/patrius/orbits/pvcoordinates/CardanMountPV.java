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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * @description
 *              <p>
 *              Cardan mounting
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton
 * 
 * @version $Id: CardanMountPV.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class CardanMountPV implements PV {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -7989767234360618822L;

    /** Angle of the roation around x axis. */
    private final double x;
    /** Angle of the rotation around y' axis (image of y axis by the rotation around x axis). */
    private final double y;
    /** Distance from the origin of the frame. */
    private final double r;
    /** The angle rate of the rotation around x axis. */
    private final double xRate;
    /** The angle rate of the rotation around y' axis (image of y axis by the rotation around x axis). */
    private final double yRate;
    /** The range rate. */
    private final double rRate;

    /**
     * Build cardan mounting.
     * 
     * @param xAngle
     *        angle of rotation around the local North axis counted clockwise from the
     *        zenith and expressed in radian (between {@code -PI} and {@code PI})
     * @param yAngle
     *        angle of rotation around y' axis (image of the West axis by the previous rotation) counted
     *        clockwise from the y' axis and expressed in radian (between {@code -PI/2} and {@code PI/2})
     * @param range
     *        distance from the origin of the topocentric frame
     * @param xAngleRate
     *        angle rate of rotation around x axis in rad/s
     * @param yAngleRate
     *        angle rate of rotation around y' axis (image of y axis by the previous rotation) in rad/s
     * @param rangeRate
     *        range rate in m/s
     */
    public CardanMountPV(final double xAngle, final double yAngle, final double range,
        final double xAngleRate, final double yAngleRate, final double rangeRate) {
        this.x = xAngle;
        this.y = yAngle;
        this.r = range;
        this.xRate = xAngleRate;
        this.yRate = yAngleRate;
        this.rRate = rangeRate;
    }

    /**
     * Get the angle of the rotation around the local North axis.
     * 
     * @return the x angle.
     */
    public double getXangle() {
        return this.x;
    }

    /**
     * Get the angle of the rotation around y' axis. Y' axis is the image of the West axis by the first rotation
     * around the North axis.
     * 
     * @return the y angle.
     */
    public double getYangle() {
        return this.y;
    }

    /**
     * Get the range.
     * 
     * @return the range.
     */
    public double getRange() {
        return this.r;
    }

    /**
     * Get the angle rate of the rotation around the North axis.
     * 
     * @return the x angle rate.
     */
    public double getXangleRate() {
        return this.xRate;
    }

    /**
     * Get the angle rate of the rotation around y' axis (which is the image of the West axis by the first
     * rotation around the North axis).
     * 
     * @return the y angle rate.
     */
    public double getYangleRate() {
        return this.yRate;
    }

    /**
     * Get the range rate.
     * 
     * @return the range rate.
     */
    public double getRangeRate() {
        return this.rRate;
    }

    /**
     * Get the Cardan mount position.
     * 
     * @return the Cardan mount position.
     */
    public CardanMountPosition getCardanMountPosition() {
        return new CardanMountPosition(this.x, this.y, this.r);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition() {
        return new Vector3D(this.x, this.y, this.r);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity() {
        return new Vector3D(this.xRate, this.yRate, this.rRate);
    }

    /**
     * Produces the following String representation of the Topocentric coordinates :
     * (x angle, y angle, range, x angle rate, y angle rate, range rate).
     * 
     * @return string representation of this position/velocity
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
            append(this.x).append(comma).
            append(this.y).append(comma).
            append(this.r).append("), V(").
            append(this.xRate).append(comma).
            append(this.yRate).append(comma).
            append(this.rRate).append(")}").toString();
    }
}
