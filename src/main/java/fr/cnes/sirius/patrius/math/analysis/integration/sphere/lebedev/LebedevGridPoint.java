/**
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Lebedev grid point.
 * 
 * @since 4.0
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LebedevGridPoint {

    /** Coordinate of the point on the 1st axis. */
    private final double x;
    /** Coordinate of the point on the 2nd axis. */
    private final double y;
    /** Coordinate of the point on the 3rd axis. */
    private final double z;

    /** Radius. */
    private final double radius;
    /** Inclination angle between 0° (included) and 180° (included). */
    private final double theta;
    /** Azimuth angle between 0° (included) and 360° (excluded). */
    private final double phi;

    /** Weight associated to the point for Lebedev's rule. */
    private final double weight;

    /**
     * Builds a new <code>LebedevGridPoint</code> instance from the Cartesian
     * coordinates of the point and its associated weight.
     *
     * @param xIn
     *        coordinate of the point on the 1st axis
     * @param yIn
     *        coordinate of the point on the 1st axis
     * @param zIn
     *        coordinate of the point on the 1st axis
     * @param weightIn
     *        weight associated to the point
     */
    public LebedevGridPoint(final double xIn,
        final double yIn,
        final double zIn,
        final double weightIn) {
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
        this.weight = weightIn;

        // Other components
        this.radius = MathLib.sqrt(xIn * xIn + yIn * yIn + zIn * zIn);

        // (X,Y,Z) to Theta, Phi coordinates on the unit sphere.
        final double fact = MathLib.sqrt(xIn * xIn + yIn * yIn);

        double angX;
        if (fact > Precision.SAFE_MIN) {
            angX = MathLib.acos(xIn / fact);
        } else {
            angX = MathLib.acos(xIn);
        }
        if (yIn < 0) {
            angX = -angX;
        }
        this.theta = angX;
        this.phi = MathLib.acos(zIn);
    }

    /**
     * Compare to another point.
     * 
     * @param point point
     * @param absolutePrecision precision
     * @return true if same point
     */
    public boolean isSamePoint(final LebedevGridPoint point,
                               final double absolutePrecision) {
        final double dist = this.getXYZ().distance(point.getXYZ());
        return (dist <= absolutePrecision);
    }

    /**
     * Gets the Cartesian coordinates of the point.
     *
     * @return the Cartesian coordinates of the point
     */
    public Vector3D getXYZ() {
        return new Vector3D(this.x, this.y, this.z);
    }

    /**
     * Gets the coordinate of the point on the 1st axis.
     *
     * @return the x coordinate of the point
     */
    public double getX() {
        return this.x;
    }

    /**
     * Gets the coordinate of the point on the 2nd axis.
     *
     * @return the y coordinate of the point
     */
    public double getY() {
        return this.y;
    }

    /**
     * Gets the coordinate of the point on the 3rd axis.
     *
     * @return the z coordinate of the point
     */
    public double getZ() {
        return this.z;
    }

    /**
     * Gets the weight associated to the point for Lebedev's rule.
     *
     * @return the weight associated to the point
     */
    public double getWeight() {
        return this.weight;
    }

    /**
     * Gets the radius.
     *
     * @return the radius
     */
    public double getRadius() {
        return this.radius;
    }

    /**
     * Gets the azimuth angle between 0° (included) and 360° (excluded).
     *
     * @return the azimuth angle
     */
    public double getPhi() {
        return this.phi;
    }

    /**
     * Gets the inclination angle between 0° (included) and 180° (included).
     *
     * @return the inclination angle
     */
    public double getTheta() {
        return this.theta;
    }
}
