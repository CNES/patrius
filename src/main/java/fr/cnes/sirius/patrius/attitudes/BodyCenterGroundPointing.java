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
 * VERSION:4.9:FA:FA-3174:10/05/2022:[PATRIUS] Corriger les differences de convention entre toutes les methodes...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:creation
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles body center pointing attitude provider; the difference between {@link BodyCenterPointing} and this
 * class is that the target point of the former is the body
 * center, while that of the latter is the corresponding point on the ground.
 * <p>
 * By default, the satellite z axis is pointing to the body frame center.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see BodyCenterPointing
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: BodyCenterGroundPointing.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 2.0
 */
public class BodyCenterGroundPointing extends AbstractGroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 6757742593371484171L;

    /**
     * Constructor. Create a BodyCenterGroundPointing attitude provider.
     * 
     * @param shape a body shape
     */
    public BodyCenterGroundPointing(final BodyShape shape) {
        super(shape);
    }

    /**
     * Constructor. Create a BodyCenterGroundPointing attitude provider with specified los axis in
     * satellite frame.
     * 
     * @param shape Body shape
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    public BodyCenterGroundPointing(final BodyShape shape, final Vector3D losInSatFrameVec,
                                    final Vector3D losNormalInSatFrameVec) {
        // Call constructor of superclass
        super(shape, losInSatFrameVec, losNormalInSatFrameVec);
    }

    /**
     * Constructor. Create a BodyCenterGroundPointing attitude provider with specified los axis in
     * satellite frame.
     * 
     * @param shape Body shape
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     * @param targetVelocityDeltaT the delta-T used to compute target velocity by finite differences
     */
    public BodyCenterGroundPointing(final BodyShape shape, final Vector3D losInSatFrameVec,
                                    final Vector3D losNormalInSatFrameVec, final double targetVelocityDeltaT) {
        // Call constructor of superclass
        super(shape, losInSatFrameVec, losNormalInSatFrameVec, targetVelocityDeltaT);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getTargetPoint(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                      final Frame frame) throws PatriusException {
        // Body frame - frame transform:
        final Transform transform =
            this.getBodyFrame().getTransformTo(frame, date, this.getSpinDerivativesComputation());
        final Vector3D center = transform.transformPosition(Vector3D.ZERO);
        final Vector3D sat = pvProv.getPVCoordinates(date, frame).getPosition();
        final Line line = new Line(center, sat);
        // inverse transform to expressed sat position in body frame
        final Vector3D satInBodyFrame = transform.getInverse().transformPosition(sat);
        // The geodetic point on the body surface:
        final GeodeticPoint targetGeo = this.getBodyShape().getIntersectionPoint(line, satInBodyFrame, frame, date);
        // Transform the geodetic point in cartesian coordinates in the input frame:
        return transform.transformPosition(this.getBodyShape().transform(targetGeo));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
