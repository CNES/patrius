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
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * END-HISTORY
 */
/**
 */
package fr.cnes.sirius.patrius.projections;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This is the identity projection defined by <br>
 * <code>X = Lon<br>
 * Y = Lat</code><br>
 * <p>
 * The pivot point has a latitude and longitude to 0.
 * </p>
 * 
 * @concurrency not thread-safe
 * @author Galpin Thomas
 * @version $Id$
 * @since 3.2
 */
public class IdentityProjection extends AbstractProjection {

    /** Serializable UID. */
    private static final long serialVersionUID = 430763443341865968L;

    /**
     * Constructor with EllipsoidBodyShape object.
     * 
     * @param ellipsoid : geodetic system of reference
     */
    public IdentityProjection(final EllipsoidBodyShape ellipsoid) {
        super(new GeodeticPoint(0., 0., 0.), ellipsoid);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canMap(final GeodeticPoint coordinates) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Vector2D applyTo(final GeodeticPoint geoPoint) {
        return new Vector2D(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    /** {@inheritDoc} */
    @Override
    public final Vector2D applyTo(final double lat, final double lon) {
        return new Vector2D(lat, lon);
    }

    /** {@inheritDoc} */
    @Override
    public final GeodeticPoint applyInverseTo(final double x, final double y) {
        return new GeodeticPoint(x, y, 0.);
    }

    /** {@inheritDoc} */
    @Override
    public final GeodeticPoint applyInverseTo(final double x, final double y, final double alt) {
        return new GeodeticPoint(x, y, alt);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isConformal() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEquivalent() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public EnumLineProperty getLineProperty() {
        return EnumLineProperty.STRAIGHT;
    }

    /** {@inheritDoc} */
    @Override
    public final double getMaximumLatitude() {
        return FastMath.PI / 2.;
    }

    /** {@inheritDoc} */
    @Override
    public double getMaximumEastingValue() {
        return 0.;
    }

    /**
     * {@inheritDoc} WARNING : this method must not be used with the {@link IdentityProjection} class : it always
     * returns
     * an {@link PatriusException}.
     */
    @Override
    public double getDistortionFactor(final double lat) {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_IMPLEMENTED);
    }
}
