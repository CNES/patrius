/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Point location relative to a 2D body surface of type {@link EllipsoidBodyShape}.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 *
 * @see AbstractBodyPoint
 * 
 * @author Emmanuel Bignon
 *
 * @since 4.12
 */
public class EllipsoidPoint extends AbstractBodyPoint {

    /** Serializable UID. */
    private static final long serialVersionUID = 7862466825590075399L;

    /**
     * Constructor from lat/long/height coordinates.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param coordSystem
     *        LLH coordinates system in which are expressed the entered lat/long/height
     * @param latitude
     *        input latitude
     * @param longitude
     *        input longitude
     * @param height
     *        input height
     * @param name
     *        name of the point
     */
    public EllipsoidPoint(final EllipsoidBodyShape bodyShape, final LLHCoordinatesSystem coordSystem,
                          final double latitude, final double longitude, final double height, final String name) {
        this(bodyShape, new LLHCoordinates(coordSystem, latitude, longitude, height), name);
    }

    /**
     * Constructor from lat/long/height coordinates.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param coordIn
     *        lat/long/height coordinates associated with the coordinates system in which they are expressed
     * @param name
     *        name of the point
     */
    public EllipsoidPoint(final EllipsoidBodyShape bodyShape, final LLHCoordinates coordIn, final String name) {
        super(bodyShape, coordIn, name);
    }

    /**
     * Constructor from a position.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        point position in body frame
     * @param name
     *        name of the point
     */
    public EllipsoidPoint(final EllipsoidBodyShape bodyShape, final Vector3D position, final String name) {
        super(bodyShape, position, false, name);
    }

    /**
     * Constructor from a position expressed in any given frame.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        position in provided frame at provided date
     * @param frame
     *        frame
     * @param date
     *        date
     * @param name
     *        name of the point
     * @throws PatriusException
     *         if frame transformation cannot be computed at provided date
     */
    public EllipsoidPoint(final EllipsoidBodyShape bodyShape, final Vector3D position, final Frame frame,
                          final AbsoluteDate date, final String name)
        throws PatriusException {
        this(bodyShape, frame.getTransformTo(bodyShape.getBodyFrame(), date).transformPosition(position), name);
    }

    /**
     * Protected constructor from a position and a flag stating if the point is known to be on the shape: if the latter
     * is set true, the normal height is set to 0 and the closest and radial projections on the shape are set equal to
     * this.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        point position in body frame
     * @param onShapeSurface
     *        flag to be set to true value if the point is known to be on the shape
     * @param name
     *        name of the point
     */
    protected EllipsoidPoint(final EllipsoidBodyShape bodyShape, final Vector3D position, final boolean onShapeSurface,
                             final String name) {
        super(bodyShape, position, onShapeSurface, name);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidBodyShape getBodyShape() {
        // Body shape is necessarily an EllipsoidBodyShape
        return (EllipsoidBodyShape) super.getBodyShape();
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getRadialProjectionOnShape() {
        return (EllipsoidPoint) super.getRadialProjectionOnShape();
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getClosestPointOnShape() {
        return (EllipsoidPoint) super.getClosestPointOnShape();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *         if the entered body point is not an {@link EllipsoidPoint} instance
     */
    @Override
    protected void setClosestPointOnShape(final BodyPoint closestOnShape) {
        // Check if the entered body point is an EllipsoidPoint instance
        if (!(closestOnShape instanceof EllipsoidPoint)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.EXPECT_ELLIPSOIDPOINT_INSTANCE);
        }
        super.setClosestPointOnShape(closestOnShape);
    }

    /** {@inheritDoc} */
    @Override
    protected final Vector3D computeNormal() {
        // normal projection on the surface
        final Vector3D positionOnSurface = getClosestPointOnShape().getPosition();
        return getBodyShape().getEllipsoid().getNormal(positionOnSurface);
    }
}
