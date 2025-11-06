/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.14:OPENFD-129:22/08/2024: [PATRIUS] Interpolation de trajectoire avec la methode de Lagrange
 * VERSION:4.14:OPENFD-160:22/08/2024: [PATRIUS] Repere defini par 2 directions
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.14:OPENFD-142:22/08/2024: [PATRIUS] Nouvel evenement PlaneCrossingDetector
 * VERSION:4.14:OPENFD-304:22/08/2024: [Patrius] Repere de la vitesse dans le detecteur d'angle d'aspect solaire
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class for plane crossing events detection.
 * 
 * @author Mathilde Lefevre
 *
 */
public class PlaneCrossingDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -6769652612279509506L;

    /** Frame for plane definition. **/
    protected final Frame referenceFrame;

    /** Point belonging to the plane for plane definition. **/
    private final Vector3D point;

    /** Normal vector to the plane for plane definition. **/
    private final Vector3D normalVector;

    /**
     * Constructor with default coordinates (0,0,0) for reference point and one detection in the given direction.
     * 
     * @param slopeSelection
     *        The direction in which the detection is performed.
     * @param referenceFrame
     *        The reference frame where the plane is considered.
     * @param normalVector
     *        A normal vector to the plane.
     * @param action
     *        The action to perform at considered crossing.
     * @param removeCrossing
     *        True if the detector has to be removed after first detection.
     * @param maxCheckIn
     *        Maximum checking interval (s).
     * @param thresholdIn
     *        Convergence threshold (s).
     */
    public PlaneCrossingDetector(final Vector3D normalVector, final Frame referenceFrame, final int slopeSelection,
                                 final Action action, final boolean removeCrossing, final double maxCheckIn,
                                 final double thresholdIn) {
        this(Vector3D.ZERO, normalVector, referenceFrame, slopeSelection, action, removeCrossing, maxCheckIn,
                thresholdIn);
    }

    /**
     * Constructor with default coordinates (0,0,0) for reference point and actions to be performed in both directions.
     * 
     * @param referenceFrame
     *        The reference frame where the plane is defined.
     * @param normalVector
     *        A normal vector to the plane.
     * @param ascendingCrossing
     *        The action to perform at ascending crossing.
     * @param descendingCrossing
     *        The action to perform at descending crossing.
     * @param removeIncreasingCrossing
     *        True if the detector at increasing crossing has to be removed after detection.
     * @param removeDecreasingCrossing
     *        True if the detector at decreasing crossing has to be removed after detection.
     * @param maxCheckIn
     *        Maximum checking interval (s).
     * @param thresholdIn
     *        Convergence threshold (s).
     */
    public PlaneCrossingDetector(final Vector3D normalVector, final Frame referenceFrame,
                                 final Action ascendingCrossing,
                                 final Action descendingCrossing,
                                 final boolean removeIncreasingCrossing, final boolean removeDecreasingCrossing,
                                 final double maxCheckIn, final double thresholdIn) {
        this(Vector3D.ZERO, normalVector, referenceFrame, ascendingCrossing, descendingCrossing, removeIncreasingCrossing,
                removeDecreasingCrossing, maxCheckIn, thresholdIn);
    }

    /**
     * Constructor for case with action at descending or ascending crossing.
     * 
     * @param slopeSelection
     *        The direction in which the detection has to be performed.
     * @param referenceFrame
     *        The reference frame where the plane is defined.
     * @param point
     *        A point belonging to the plane.
     * @param normalVector
     *        A normal vector to the plane.
     * @param action
     *        The action to be performed at considered crossing.
     * @param removeCrossing
     *        True if the detector has to be removed after first detection.
     * @param maxCheckIn
     *        Maximum checking interval (s).
     * @param thresholdIn
     *        Convergence threshold (s).
     */
    public PlaneCrossingDetector(final Vector3D point, final Vector3D normalVector, final Frame referenceFrame,
                                 final int slopeSelection, final Action action,
                                 final boolean removeCrossing, final double maxCheckIn, final double thresholdIn) {
        super(slopeSelection, maxCheckIn, thresholdIn, action, removeCrossing);
        if (normalVector.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.ZERO_NORM_VECTOR_FOR_PLANE_DEFINITION);
        }
        this.shouldBeRemovedFlag = removeCrossing;
        if (slopeSelection == INCREASING) {
            this.actionAtEntry = action;
            this.actionAtExit = null;
            this.removeAtEntry = removeCrossing;
            this.removeAtExit = false;
        } else if (slopeSelection == DECREASING) {
            this.actionAtEntry = null;
            this.actionAtExit = action;
            this.removeAtEntry = false;
            this.removeAtExit = removeCrossing;
        } else {
            // detection at ascending and descending crossings
            this.actionAtEntry = action;
            this.actionAtExit = action;
            this.removeAtEntry = removeCrossing;
            this.removeAtExit = removeCrossing;
        }

        this.referenceFrame = referenceFrame;
        this.point = point;
        this.normalVector = normalVector.normalize();
    }

    /**
     * Constructor for case with different actions at increasing crossing and decreasing crossing.
     * 
     * @param referenceFrame
     *        The reference frame where the plane is defined.
     * @param point
     *        A point belonging to the plane.
     * @param normalVector
     *        A normal vector to the plane.
     * @param increasingCrossing
     *        The action to be performed at increasing crossing.
     * @param decreasingCrossing
     *        The action to be performed at decreasing crossing.
     * @param removeIncreasingCrossing
     *        True if the detection at increasing crossing has to be performed once.
     * @param removeDecreasingCrossing
     *        True if the detection at decreasing crossing has to be performed once.
     * @param maxCheckIn
     *        Maximum checking interval (s).
     * @param thresholdIn
     *        Convergence threshold (s).
     */
    public PlaneCrossingDetector(final Vector3D point, final Vector3D normalVector, final Frame referenceFrame,
                                 final Action increasingCrossing,
                                 final Action decreasingCrossing,
                                 final boolean removeAscendingCrossing, final boolean removeDescendingCrossing,
                                 final double maxCheckIn, final double thresholdIn) {
        super(maxCheckIn, thresholdIn, increasingCrossing, decreasingCrossing, removeAscendingCrossing,
                removeDescendingCrossing);
        if (normalVector.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.ZERO_NORM_VECTOR_FOR_PLANE_DEFINITION);
        }
        this.referenceFrame = referenceFrame;
        this.point = point;
        this.normalVector = normalVector.normalize();
    }

    /**
     * Getter for the frame for plane definition.
     * 
     * @return referenceFrame
     */
    public Frame getFrame() {
        return this.referenceFrame;
    }

    /**
     * Getter for the point belonging to the plane used for plane definition.
     * 
     * @return point
     */
    public Vector3D getPoint() {
        return this.point;
    }

    /**
     * Getter for the normal vector to the plane used for plane definition.
     * 
     * @return normalVector
     */
    public Vector3D getNormalVector() {
        return this.normalVector;
    }

    /**
     * @return a copy of the considered PlaneCrossingDetector.
     */
    @Override
    public EventDetector copy() {
        final EventDetector detector;
        if (this.getSlopeSelection() == INCREASING) {
            detector =
                new PlaneCrossingDetector(point, normalVector, referenceFrame, INCREASING, this.getActionAtEntry(),
                removeAtEntry, this.getMaxCheckInterval(), this.getThreshold());
        } else if (this.getSlopeSelection() == DECREASING) {
            detector =
                new PlaneCrossingDetector(point, normalVector, referenceFrame, DECREASING, this.getActionAtExit(),
                    removeAtExit, this.getMaxCheckInterval(), this.getThreshold());
        } else {
            detector = new PlaneCrossingDetector(point, normalVector, referenceFrame, this.getActionAtEntry(),
                this.getActionAtExit(), removeAtEntry, removeAtExit, this.getMaxCheckInterval(), this.getThreshold());
        }
        return detector;
    }

    /**
     * Event detection function. The plane crossing occurs when the position vector of the spacecraft with respect to
     * the point defining the plane and the normal vector to the plane are normal.
     * 
     * @param s
     *        Spacecraft state at a given moment.
     * @return the value of the dot product between the satellite position and the normal to the plane.
     * 
     */
    @Override
    public double g(final SpacecraftState s) throws PatriusException {
        return s.getPVCoordinates(this.referenceFrame).getPosition().subtract(this.point).dotProduct(this.normalVector);
    }

    /**
     * @param s
     *        The spacecraft state at the moment the event occurs.
     * @param increasing
     *        if true, the value of the switching function increases when times increases around event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when ascending or/and descending crossing is reached.
     * @throws PatriusException
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        Action outputAction;
        if (this.getSlopeSelection() == INCREASING) {
            outputAction = this.getActionAtEntry();
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else if (this.getSlopeSelection() == DECREASING) {
            outputAction = this.getActionAtExit();
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else {
            if (forward ^ !increasing) {
                // ascending crossing case
                outputAction = this.getActionAtEntry();
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            } else {
                // descending crossing case
                outputAction = this.getActionAtExit();
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            }
        }
        return outputAction;
    }

}
