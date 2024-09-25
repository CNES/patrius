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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformStateProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * One degree of liberty transform provider.
 * It is defined by:
 * <ul>
 * <li>A reference transform which provides a reference orientation of the part</li>
 * <li>An axis which provides the rotation axis of the part</li>
 * <li>An {@link OrientationAngleProvider} which provides an angle through time</li>
 * </ul>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.4
 */
public class OrientationAngleTransform implements TransformStateProvider {

     /** Serializable UID. */
    private static final long serialVersionUID = -3042358151481137814L;

    /** Reference transform. */
    private final Transform reference;

    /** Axis of rotation. */
    private final Vector3D axis;

    /** Orientation angle provider. */
    private final OrientationAngleProvider orientationAngleProvider;

    /**
     * Constructor.
     * @param reference reference transformation
     * @param axis axis of degree of liberty
     * @param orientationAngleProvider orientation angle provider
     */
    public OrientationAngleTransform(final Transform reference, final Vector3D axis,
            final OrientationAngleProvider orientationAngleProvider) {
        this.reference = reference;
        this.axis = axis;
        this.orientationAngleProvider = orientationAngleProvider;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final SpacecraftState state) throws PatriusException {
        final double angle = orientationAngleProvider.getOrientationAngle(state.getOrbit(), state.getDate());
        final Rotation rotation = new Rotation(axis, angle);
        // Rotation rate is unknown
        final AngularCoordinates angularCoordinates = new AngularCoordinates(rotation, Vector3D.ZERO);
        final Transform t = new Transform(state.getDate(), PVCoordinates.ZERO, angularCoordinates);
        return new Transform(state.getDate(), reference, t);
    }

    /**
     * Returns the reference transform. This is the transform return by the {@link #getTransform(SpacecraftState)}
     * method if the orientation angle is 0.
     * @return the reference transform
     */
    public Transform getReference() {
        return reference;
    }

    /**
     * Returns the axis of the transform in the frame defined by the reference transform.
     * @return the axis of the transform
     */
    public Vector3D getAxis() {
        return axis;
    }

    /**
     * The orientation angle provider which provides an angle through time.
     * The final transform is the reference transform + rotation around {@link #getAxis()}.
     * @return the orientation angle provider which provides an angle through time
     */
    public OrientationAngleProvider getOrientationAngleProvider() {
        return orientationAngleProvider;
    }
}
