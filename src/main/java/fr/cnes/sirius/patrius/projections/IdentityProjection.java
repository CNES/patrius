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
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au
 * lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
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
     * @param ellipsoid
     *        ellipsoid shape
     */
    public IdentityProjection(final EllipsoidBodyShape ellipsoid) {
        super(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0., 0., 0., BodyPointName.DEFAULT));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canMap(final EllipsoidPoint point) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Vector2D applyTo(final EllipsoidPoint point) {
        return new Vector2D(point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(),
            point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude());
    }

    /** {@inheritDoc} */
    @Override
    public final Vector2D applyTo(final double lat, final double lon) {
        return new Vector2D(lat, lon);
    }

    /** {@inheritDoc} */
    @Override
    public final EllipsoidPoint applyInverseTo(final double x, final double y) {
        return new EllipsoidPoint(getReference(), getReference().getLLHCoordinatesSystem(), x, y, 0.,
            BodyPointName.DEFAULT);
    }

    /** {@inheritDoc} */
    @Override
    public final EllipsoidPoint applyInverseTo(final double x, final double y, final double alt) {
        return new EllipsoidPoint(getReference(), getReference().getLLHCoordinatesSystem(), x, y, alt,
            BodyPointName.DEFAULT);
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
     * {@inheritDoc}
     * 
     * <p>
     * <b>WARNING: this method must not be used with the {@link IdentityProjection} class : it always returns an
     * {@link PatriusException}.</b>
     * </p>
     */
    @Override
    public double getDistortionFactor(final double lat) {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_IMPLEMENTED);
    }
}
