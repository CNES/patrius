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
 * VERSION:4.13:FA:FA-146:08/12/2023:[PATRIUS] Erreur dans la methode
 * getTargetPosition de la classe TargetGroundPointing
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:creation
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles target pointing attitude provider; while the class {@link TargetPointing} does not guarantee the
 * target point belongs to the body surface, this class always provides a ground point target.
 * <p>
 * By default, the satellite z axis is pointing to a ground point target.
 * </p>
 *
 * @concurrency immutable
 *
 * @see TargetPointing
 *
 * @author Tiziana Sabatini
 *
 * @version $Id: TargetGroundPointing.java 18065 2017-10-02 16:42:02Z bignon $
 *
 * @since 2.0
 */
public class TargetGroundPointing extends AbstractGroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -8002434923471977301L;

    /** Target point name. */
    private static final String TARGET_NAME = "targetPoint";

    /** Target point. */
    private final BodyPoint targetPoint;

    /**
     * Creates a new instance from body shape and target expressed in cartesian coordinates.
     *
     * @param shape
     *        Body shape
     * @param targetIn
     *        Target position in body frame
     */
    public TargetGroundPointing(final BodyShape shape, final Vector3D targetIn) {
        this(shape.buildPoint(targetIn, TARGET_NAME));
    }

    /**
     * Creates a new instance from body shape and target expressed in geodetic coordinates.
     *
     * @param targetPointIn
     *        Target point
     */
    public TargetGroundPointing(final BodyPoint targetPointIn) {
        // Call constructor of superclass
        super(targetPointIn.getBodyShape());
        this.targetPoint = targetPointIn;
    }

    /**
     * Creates a new instance from body shape and target expressed in cartesian coordinates with specified los axis in
     * satellite frame.
     *
     * @param shape
     *        Body shape
     * @param targetIn
     *        Target position in body frame
     * @param losInSatFrameVec
     *        LOS in satellite frame axis
     * @param losNormalInSatFrameVec
     *        LOS normal axis in satellite frame
     */
    public TargetGroundPointing(final BodyShape shape, final Vector3D targetIn, final Vector3D losInSatFrameVec,
                                final Vector3D losNormalInSatFrameVec) {
        this(shape.buildPoint(targetIn, TARGET_NAME), losInSatFrameVec, losNormalInSatFrameVec);
    }

    /**
     * Creates a new instance from body target with specified los axis in satellite frame.
     *
     * @param targetPointIn
     *        Target point
     * @param losInSatFrameVec
     *        LOS in satellite frame axis
     * @param losNormalInSatFrameVec
     *        LOS normal axis in satellite frame
     */
    public TargetGroundPointing(final BodyPoint targetPointIn, final Vector3D losInSatFrameVec,
                                final Vector3D losNormalInSatFrameVec) {
        // Call constructor of superclass
        super(targetPointIn.getBodyShape(), losInSatFrameVec, losNormalInSatFrameVec);
        this.targetPoint = targetPointIn;
    }

    /**
     * Creates a new instance from body shape and target expressed in cartesian coordinates with specified los axis in
     * satellite frame.
     *
     * @param shape
     *        Body shape
     * @param targetIn
     *        Target position in body frame
     * @param losInSatFrameVec
     *        LOS in satellite frame axis
     * @param losNormalInSatFrameVec
     *        LOS normal axis in satellite frame
     * @param targetVelocityDeltaT
     *        Delta-T used to compute target velocity by finite differences
     */
    public TargetGroundPointing(final BodyShape shape, final Vector3D targetIn, final Vector3D losInSatFrameVec,
                                final Vector3D losNormalInSatFrameVec, final double targetVelocityDeltaT) {
        this(shape.buildPoint(targetIn, TARGET_NAME), losInSatFrameVec, losNormalInSatFrameVec, targetVelocityDeltaT);
    }

    /**
     * Creates a new instance from body target with specified los axis in satellite frame.
     *
     * @param targetPointIn
     *        Target point
     * @param losInSatFrameVec
     *        LOS in satellite frame axis
     * @param losNormalInSatFrameVec
     *        LOS normal axis in satellite frame
     * @param targetVelocityDeltaT
     *        Delta-T used to compute target velocity by finite differences
     */
    public TargetGroundPointing(final BodyPoint targetPointIn, final Vector3D losInSatFrameVec,
                                final Vector3D losNormalInSatFrameVec, final double targetVelocityDeltaT) {
        // Call constructor of superclass
        super(targetPointIn.getBodyShape(), losInSatFrameVec, losNormalInSatFrameVec, targetVelocityDeltaT);
        this.targetPoint = targetPointIn;
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getTargetPosition(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                         final Frame frame) throws PatriusException {
        // Body frame - frame transform:
        final Transform transform = getBodyFrame().getTransformTo(frame, date, getSpinDerivativesComputation());
        final Vector3D satPos = pvProv.getPVCoordinates(date, getBodyFrame()).getPosition();
        final Vector3D target = this.targetPoint.getPosition();
        // Create the line joining the body center and the target: 
        final Line line = new Line(satPos, target); 
        // Compute the intersection between the body shape and the center-target direction: 
        final BodyPoint cTargetPoint = getBodyShape().getIntersectionPoint(line, satPos, getBodyFrame(), date); 
        return transform.transformPosition(cTargetPoint.getPosition());
    }

    /**
     * Getter for the target point.
     * 
     * @return the target point
     */
    public BodyPoint getTargetPoint() {
        return this.targetPoint;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s: targetPoint=%s", this.getClass().getSimpleName(), this.targetPoint.toString());
    }
}
