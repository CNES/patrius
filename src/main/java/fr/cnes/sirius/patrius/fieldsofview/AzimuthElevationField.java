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
 * @history creation 30/05/2012
 *
 * HISTORY
* VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.6.1:DM:DM-2871:15/03/2021:Changement du sens des Azimuts (Annulation de SIRIUS-FT-2558)
 * VERSION:4.6:DM:DM-2558:27/01/2021:Changement du sens des Azimuts 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3.1:FA:FA-2141:11/07/2019: IndexOutOfBoundsException masque physique sans point avec azimut egal a 0
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1783:12/11/2018:Global improvement of azimuth elevation field
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import java.util.Arrays;
import java.util.Comparator;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AzimuthElevationCalculator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.propagation.events.GroundMaskElevationDetector;

/**
 * <p>
 * Field of view defined by an azimuth-elevation mask : the algorithms are from the Orekit
 * {@link GroundMaskElevationDetector} detector. The mask is defined by an azimuth-elevation array :
 * the vertical is the Z axis of the local frame, the angle between the local north and the x axis
 * must be given at construction. <br>
 * The angular distance to the field limit is not the exact shortest distance: it is the exact
 * angular distance on the local meridian (difference of elevation of the target and the linear
 * interpolated local elevation). Concerning the field of view, the limit between two consecutive
 * points is NOT a great circle (i.e. planar) limit: it is linear in azimuth-elevation. The fields
 * with planar limits are the Rectangle or Pyramidal fields.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AzimuthElevationField implements IFieldOfView {

    /** Serial UID. */
    private static final long serialVersionUID = 5748793289878194244L;

    /** Azimuth-elevation mask. */
    private final double[][] azelmask;

    /** the name of the fov (field of view) */
    private final String fovName;

    /** Point Azimuth, Elevation Calculator */
    private final AzimuthElevationCalculator azimuthElevationCalculator;

    /** Search algorithm. */
    private ISearchIndex searchAlgo;

    /**
     * Constructor for the azimuth - elevation mask
     * 
     * @param azimElevMask
     *        the azimuth (defined from North, clockwise, rad) - elevation mask (rad)
     * 
     * @param frameOrientation
     *        Oriented angle (trigowise, radian) between the local North and the Frame's x axis<br>
     * 
     *        Example :<br>
     *        If "Reference Azimuth" is aligned with the local North of a local topocentric frame<br>
     *        then a frameOrientation of -0.785 (=> -45°) means that the x axis of the Frame points
     *        to North-East
     * @param fovName
     *        the name of the field
     */
    public AzimuthElevationField(final double[][] azimElevMask,
            final double frameOrientation,
            final String fovName) {
        this.azelmask = this.checkMask(azimElevMask.clone());
        this.fovName = fovName;
        this.azimuthElevationCalculator = new AzimuthElevationCalculator(frameOrientation);
    }

    /**
     * Computes the angular distance between a vector and the border of the field.
     * The result is positive if the direction is in the field, negative otherwise.
     * This value is approximative, mostly when the mask has great elevation variations,
     * but its sign is right.
     * @param point
     *        Point Cartesian coordinates
     * @return the angular distance
     */
    @Override
    public double getAngularDistance(final Vector3D point) {
        // Elevation and Azimuth of the Point
        final double pointAzimuth = getAzimuthElevationCalculator().getAzimuth(point);
        final double pointElevation = getAzimuthElevationCalculator().getElevation(point);

        // fov (= Mask) Elevation for the Point Azimuth
        final double maskBorderElev = this.getElevation(pointAzimuth);

        return pointElevation - maskBorderElev;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D point) {
        // comparison to 0.0 : relative comparison can't be used
        return (this.getAngularDistance(point) > 0.0);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.fovName;
    }

    /**
     * Get the Mask interpolated Elevation for a given Azimuth :
     * this algorithm is from the orekit's {@link GroundMaskElevationDetector}
     * 
     * @param azimuth
     *        azimuth (counted clockwise) (rad)
     * @return elevation angle (rad)
     */
    public double getElevation(final double azimuth) {
        final double res;
        if (azimuth == azelmask[0][0]) {
            // Particular case of mask boundary
            res = MathLib.max(this.azelmask[0][1], this.azelmask[this.azelmask.length - 1][1]);
        } else {
            // Not on boundaries
            // Reminder: closed-open convention [a, b[
            // Find segment
            final int i = this.searchAlgo.getIndex(azimuth);
            // Particular case when the azimuth 0.0 rad isn't initialized and we call
            // an azimuth defined between 0. and the first initialized azimuth value.
            if (i == 0 && azelmask[0][0] != azelmask[1][0]) {

                final double azd = azelmask[0][0];
                final double azf = azelmask[1][0];
                final double eld = azelmask[0][1];
                final double elf = azelmask[1][1];

                res = linear(azimuth, azd, azf, eld, elf);
            } else {
                // Normal case
                final double azp = azelmask[i - 1][0];
                final double elp = azelmask[i - 1][1];
                final double azd = azelmask[i][0];
                final double azf = azelmask[i + 1][0];
                final double eld = azelmask[i][1];
                final double elf = azelmask[i + 1][1];
                if (azimuth == azd && azimuth == azp) {
                    // Double point on lower interval bound (a): return highest elevation
                    res = MathLib.max(eld, elp);
                } else {
                    // Regular point
                    // Compute corresponding elevation
                    if (azd == azf) {
                        // Same azimuth (a = b): return highest elevation
                        res = MathLib.max(eld, elf);
                    } else {
                        // Regular case
                        res = linear(azimuth, azd, azf, eld, elf);
                    }
                }
            }
        }
        return res;
    }

    /**
     * Checking and ordering the azimuth-elevation tabulation : this algorithm
     * is from the orekit's {@link GroundMaskElevationDetector}
     * 
     * @param azimelev
     *        azimuth-elevation tabulation to be checked and ordered
     * @return ordered azimuth-elevation tabulation ordered
     */
    private double[][] checkMask(final double[][] azimelev) {

        final int maskSize = azimelev.length + 2;

        // Copy of the given mask
        final double[][] mask = new double[maskSize][azimelev[0].length];
        for (int i = 0; i < azimelev.length; i++) {
            System.arraycopy(azimelev[i], 0, mask[i + 1], 0, azimelev[i].length);
            // Reducing azimuth between 0 and 2Pi
            mask[i + 1][0] = MathUtils.normalizeAngle(mask[i + 1][0], FastMath.PI);
        }

        // Sorting the mask with respect to azimuth
        Arrays.sort(mask, 1, mask.length - 1, new Comparator<double[]>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final double[] d1,
                    final double[] d2) {
                return Double.compare(d1[0], d2[0]);
            }
        });

        // Extending the mask in order to cover [0, 2PI] in azimuth
        // Add point in azimuth = 0 and 2Pi.
        // Corresponding elevation is computed thanks to linear interpolation
        final double el0 = this.linear(0., mask[mask.length - 2][0] - MathUtils.TWO_PI, mask[1][0],
                mask[mask.length - 2][1], mask[1][1]);
        mask[0][0] = 0;
        mask[0][1] = el0;
        mask[mask.length - 1][0] = MathUtils.TWO_PI;
        mask[mask.length - 1][1] = el0;

        // Retrieve azimuth part of mask and define search algorithm
        final double[] azimuth = new double[maskSize];
        for (int i = 0; i < azimuth.length; i++) {
            azimuth[i] = mask[i][0];
        }
        this.searchAlgo = new BinarySearchIndexClosedOpen(azimuth);

        return mask;
    }

    /**
     * Linear interpolation for given point x between (xa, ya) and (xb, yb).
     * 
     * @param x x
     * @param xa xa
     * @param xb xb
     * @param ya ya
     * @param yb yb
     * @return linear interpolation for given point x between (xa, ya) and (xb, yb)
     */
    public double linear(final double x,
            final double xa,
            final double xb,
            final double ya,
            final double yb) {
        return ya + (x - xa) * (yb - ya) / (xb - xa);
    }

    /**
     * Returns the Azimuth Elevation Calculator.
     * @return
     *         the Azimuth Elevation Calculator
     */
    public AzimuthElevationCalculator getAzimuthElevationCalculator() {
        return azimuthElevationCalculator;
    }
}
