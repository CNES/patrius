/**
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
 * HISTORY
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.projections;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Despite the common name, the "Sanson-Flamsteed" projection was not first studied by either
 * Nicholas Sanson (ca. 1650) or John Flamsteed (1729, published posthumously), but possibly
 * by Mercator — at least it was included for maps of South America in later editions (1606)
 * of Mercator's atlas, and has been referred to as the Mercator equal-area projection, or Mercator-Sanson.
 * Since in the normal aspect all meridians are sinusoids, it is also called sinusoidal, and can be easily deduced.
 * The sinusoidal projection is equal-area and preserves distances along the horizontals,
 * i.e. all parallels in an equatorial map are standard lines — but only the central meridian.
 * Although the equatorial band is reproduced with little distortion, the polar caps suffer from poor legibility
 * due to shearing.
 * 
 * @see <a href="http://www.progonos.com/furuti/MapProj/Normal/ProjPCyl/projPCyl.html#SansonFlamsteed">
 *      http://www.progonos.com/furuti/MapProj/Normal/ProjPCyl/projPCyl.html#SansonFlamsteed</a>
 * 
 * @concurrency not thread-safe
 * @author Galpin Thomas
 * @version $Id$
 * @since 3.2
 */

public class GeneralizedFlamsteedSamson extends Mercator {

    /** Serial Version UID. */
    private static final long serialVersionUID = -5510747491232792509L;

    /**
     * Constructor : Mercator constructor is used with :
     * <ul>
     * <li>a pivot point</li>
     * <li>a reference shape</li>
     * <li>a cap</li>
     * <li>parameter centered set to true</li>
     * <li>parameter series set to false indicating that inverse transformation is done with iterations : slower but
     * more accurate</li>
     * </ul>
     * 
     * @param pivotPoint
     *        pivot point
     * @param shape
     *        reference shape
     * @param cap
     *        is angle from the north direction to the current direction in <b>CLOCKWISE</b> sense
     */
    public GeneralizedFlamsteedSamson(final GeodeticPoint pivotPoint,
        final EllipsoidBodyShape shape,
        final double cap) {
        super(pivotPoint, shape, cap, true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D applyTo(final double lat, final double lon) throws PatriusException {

        final GeodeticPoint geodeticPoint = new GeodeticPoint(lat, lon, 0.);

        // Mercator projection
        final Vector2D mercatorProjection = super.applyTo(lat, lon);

        // Projection of the point on Mercator ordinate axis
        final GeodeticPoint h = super.applyInverseTo(0., mercatorProjection.getY());

        // Allocation of coordinates computing Loxodromic distance
        final double xi = MathLib.signum(mercatorProjection.getX())
            * ProjectionEllipsoidUtils.computeLoxodromicDistance(h, geodeticPoint,
                this.getReference());
        final double yi = MathLib.signum(mercatorProjection.getY())
            * ProjectionEllipsoidUtils.computeLoxodromicDistance(h, this.getPivotPoint(),
                this.getReference());
        return new Vector2D(xi, yi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D applyTo(final GeodeticPoint geodeticPoint) throws PatriusException {
        return this.applyTo(geodeticPoint.getLatitude(), geodeticPoint.getLongitude());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GeodeticPoint applyInverseTo(final double x, final double y, final double alt) throws PatriusException {

        // Computation of geographic coordinates of projected points Hi on the reference
        // loxodrome, according to the orthogonal loxodrome
        final GeodeticPoint h = ProjectionEllipsoidUtils.computePointAlongLoxodrome(
            this.getPivotPoint(), y, this.getAzimuth(), this.getReference());

        // Computation of geographic coordinates, and point Mi,
        // at distance Xi from point H following the direction cap + PI/2
        return ProjectionEllipsoidUtils.computePointAlongLoxodrome(h, x, this.getAzimuth() + FastMath.PI
            / 2., this.getReference());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GeodeticPoint applyInverseTo(final double x, final double y) throws PatriusException {
        return this.applyInverseTo(x, y, 0.);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConformal() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEquivalent() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnumLineProperty getLineProperty() {
        return EnumLineProperty.NONE;
    }
}