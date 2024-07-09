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
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:272:09/10/2014:added H0 - n frame
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::FA:590:05/04/2016:correction to freeze H0 - n frame
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * "H0 - n" reference frame.
 * </p>
 * The "H0 - n" frame is a pseudo-inertial frame, built from the GCRF-ITRF transformation at the date H0 - n; this
 * transformation is "frozen" in time, and it is combined to a rotation of an angle "longitude" around the Z axis
 * of the ITRF frame.
 * <p>
 * Its parent frame is the GCRF frame.
 * </p>
 *
 * <p>Spin derivative, when computed, is always 0 by definition.</p>
 * <p>Frames configuration is unused.</p>
 * 
 * @see FramesFactory
 * @serial serializable.
 * @author Tiziana Sabatini
 * @version $Id: H0MinusNProvider.java 18074 2017-10-02 16:48:51Z bignon $
 * @since 2.3
 */
public final class H0MinusNProvider extends FixedTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -5974155860988189102L;

    /**
     * Simple constructor.
     * 
     * @param h0MinusN
     *        the H0 - n date.
     * @param longitude
     *        the rotation angle around the ITRF Z axis (rad).
     * @throws PatriusException
     *         when the ITRF-GCRF transformation cannot be computed
     */
    public H0MinusNProvider(final AbsoluteDate h0MinusN, final double longitude) throws PatriusException {
        super(computeTranform(h0MinusN, longitude));
    }

    /**
     * Compute the transformation from GCRF frame to "H0 - n" frame, using a frozen configuration
     * of the ITRF frame at the date H0 - n date, combined to a rotation around the Z axis of the ITRF frame.
     * 
     * @param h0MinusN
     *        the H0 - n date.
     * @param longitude
     *        the rotation angle around the ITRF Z axis (rad).
     * @return the transformation from parent frame to "H0 - n" frame.
     * @throws PatriusException
     *         when the ITRF-GCRF transformation cannot be computed
     */
    private static Transform computeTranform(final AbsoluteDate h0MinusN,
                                             final double longitude) throws PatriusException {
        final Transform transform = FramesFactory.getGCRF().getTransformTo(FramesFactory.getITRF(), h0MinusN);
        final Transform frozenTransform = transform.freeze();
        // Acceleration is null at this point. Need to set it to Vector3D.ZERO
        final Transform frozenTransform2 = new Transform(frozenTransform.getDate(), frozenTransform.getCartesian(),
            new AngularCoordinates(frozenTransform.getRotation(), Vector3D.ZERO));
        final Transform rotationZ = new Transform(h0MinusN, new Rotation(Vector3D.PLUS_K, longitude), Vector3D.ZERO,
            Vector3D.ZERO);
        return new Transform(h0MinusN, frozenTransform2, rotationZ, true);
    }
}
