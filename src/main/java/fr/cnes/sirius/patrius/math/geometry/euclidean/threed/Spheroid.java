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
 * @history Created on 05/10/2011
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

/**
 * <p>
 * This is the Spheroid (also called Revolved Ellipsoid) class. Spheroids are ellipsoids that are revolved around an
 * axis. Thus, this class cannot represent all ellipsoid objects.
 * </p>
 * <p>
 * It creates a spheroid object.
 * </p>
 * <p>
 * <u>Usage:</u> With two Vector3D for position and Rev. Axis and two doubles for the two semi axes, call <br>
 * <center>Spheroid mySpheroid = new Spheroid(position, axis, a, b)</center>
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see IEllipsoid
 * @see SolidShape
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Spheroid.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class Spheroid extends Ellipsoid implements Serializable {

    /**
     * Generated Serial UID
     */
    private static final long serialVersionUID = -874292129066116967L;

    /**
     * This constructor builds a spheroid from its centers position, its revolution axis and its equatorial and
     * polar radius. A spheroid, or ellipsoid of revolution, is a quadric surface obtained by rotating an ellipse
     * about one of its principal axes.
     * 
     * @param myPosition
     *        The position of the spheroids center
     * @param myRevAxis
     *        The axis of revolution of the spheroid
     * @param equatorialRadius
     *        Equatorial radius : semi axis of the spheroid along a direction orthogonal to the axis of revolution
     * @param polarRadius
     *        Polar radius : semi axis of the spheroid along the axis of revolution
     * 
     * @exception IllegalArgumentException
     *            if semi-axis or norm of revolution axis is null
     * 
     * @since 1.0
     */
    public Spheroid(final Vector3D myPosition, final Vector3D myRevAxis, final double equatorialRadius,
        final double polarRadius) {
        super(myPosition, myRevAxis, myRevAxis.orthogonal(), equatorialRadius, equatorialRadius, polarRadius);
    }

    /**
     * Get equatorial radius of Spheroid.
     * 
     * @return equatorial radius value
     */
    public double getEquatorialRadius() {
        return this.getSemiA();
    }

    /**
     * Get polar radius of Spheroid.
     * 
     * @return b polar radius value
     */
    public double getPolarRadius() {
        return this.getSemiC();
    }

    /**
     * Get a representation for this spheroid.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this spheroid
     */
    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);
        // "Center":
        res.append("Center");
        res.append(this.getCenter().toString());
        res.append(comma);
        // "Revolution axis":
        res.append("Revolution axis");
        res.append(this.getSemiPrincipalZ().toString());
        res.append(comma);
        // "Radius A":
        res.append("Equatorial radius").append(open);
        res.append(this.getSemiA()).append(close);
        res.append(comma);
        // "Radius B":
        res.append("Polar radius").append(open);
        res.append(this.getSemiC()).append(close);
        res.append(close);

        return res.toString();
    }
}
