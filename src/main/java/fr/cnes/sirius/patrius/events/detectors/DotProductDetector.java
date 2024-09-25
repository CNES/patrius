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
 * HISTORY
 * VERSION:4.13:DM:DM-39:08/12/2023:[PATRIUS] Generalisation de DotProductDetector et ExtremaDotProductDetector
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the dot product value between a spacecraft and a target PV coordinates reaches a
 * threshold value.
 *
 * @since 4.11
 */
public final class DotProductDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = -4175714577269422597L;

    /** Threshold triggering the event. */
    private final double dotProductThreshold;

    /** Reference frame to project vectors (null to use satellite frame). */
    private final Frame projectionFrame;

    /** Target direction. */
    private final IDirection targetDirection;

    /** Reference direction. */
    private final IDirection referenceDirection;

    /** First coordinates normalize . */
    private final boolean normalizeReference;

    /** Second coordinates normalize. */
    private final boolean normalizeTarget;

    /** Action performed at excess detection when ascending. */
    private final Action actionAtAscending;

    /** Action performed at excess detection when descending. */
    private final Action actionAtDescending;

    /**
     * Constructor for an DotProductPassageDetector instance.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     */
    public DotProductDetector(final PVCoordinatesProvider target, final boolean normalizePos,
                                     final boolean normalizeTarget, final double dotProduct,
                                     final Frame projectionFrame, final int slopeSelectionIn) {
        this(target, normalizePos, normalizeTarget, dotProduct, projectionFrame, slopeSelectionIn, DEFAULT_MAXCHECK,
                DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     */
    public DotProductDetector(final PVCoordinatesProvider target, final boolean normalizePos,
                                     final boolean normalizeTarget, final double dotProduct,
                                     final Frame projectionFrame, final int slopeSelectionIn, final double maxCheck,
                                     final double threshold) {
        this(target, normalizePos, normalizeTarget, dotProduct, projectionFrame, slopeSelectionIn, maxCheck, threshold,
                Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param ascending
     *        action performed when ascending
     * @param descending
     *        action performed when descending
     */
    public DotProductDetector(final PVCoordinatesProvider target, final boolean normalizePos,
                                     final boolean normalizeTarget, final double dotProduct,
                                     final Frame projectionFrame, final int slopeSelectionIn, final double maxCheck,
                                     final double threshold, final Action ascending, final Action descending) {
        this(new GenericTargetDirection(target), normalizePos, normalizeTarget, dotProduct, projectionFrame,
                slopeSelectionIn, maxCheck, threshold, ascending, descending);
    }

    /**
     * Constructor for an DotProductPassageDetector instance.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     */
    public DotProductDetector(final IDirection target, final boolean normalizePos,
                                     final boolean normalizeTarget, final double dotProduct,
                                     final Frame projectionFrame, final int slopeSelectionIn) {
        this(target, normalizePos, normalizeTarget, dotProduct, projectionFrame, slopeSelectionIn, DEFAULT_MAXCHECK,
                DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     */
    public DotProductDetector(final IDirection target, final boolean normalizePos,
                                     final boolean normalizeTarget, final double dotProduct,
                                     final Frame projectionFrame, final int slopeSelectionIn, final double maxCheck,
                                     final double threshold) {
        this(target, normalizePos, normalizeTarget, dotProduct, projectionFrame, slopeSelectionIn, maxCheck, threshold,
                Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param ascending
     *        action performed when ascending
     * @param descending
     *        action performed when descending
     */
    public DotProductDetector(final IDirection target, final boolean normalizePos,
                                     final boolean normalizeTarget, final double dotProduct,
                                     final Frame projectionFrame, final int slopeSelectionIn, final double maxCheck,
                                     final double threshold, final Action ascending, final Action descending) {
        this(null, target, normalizePos, normalizeTarget, dotProduct, projectionFrame,
                slopeSelectionIn, maxCheck, threshold, ascending, descending);
    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param referenceDirection
     *        first coordinates provider.
     * @param targetDirection
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param ascending
     *        action performed when ascending
     * @param descending
     *        action performed when descending
     */
    public DotProductDetector(final IDirection referenceDirection, final IDirection targetDirection,
                                     final boolean normalizePos, final boolean normalizeTarget,
                                     final double dotProduct, final Frame projectionFrame, final int slopeSelectionIn,
                                     final double maxCheck, final double threshold, final Action ascending,
                                     final Action descending) {
        super(slopeSelectionIn, maxCheck, threshold);
        // initialize values
        this.targetDirection = targetDirection;
        this.referenceDirection = referenceDirection;
        this.normalizeReference = normalizePos;
        this.normalizeTarget = normalizeTarget;
        this.dotProductThreshold = dotProduct;
        this.projectionFrame = projectionFrame;

        this.actionAtAscending = ascending;
        this.actionAtDescending = descending;
    }

    /**
     * Handle an angular momentum excess event and choose what to do next.
     *
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, the value of the switching function increases when times increases around
     *        event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when angular momentum excess is reached.
     * @exception PatriusException
     *            if some specific error occurs
     */
    @Override
    public final Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        Action outputAction = this.actionAtDescending;
        if (increasing) {
            outputAction = this.actionAtAscending;
        }
        return outputAction;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {
        // Define the working frame
        final Frame projFrame;
        if (this.projectionFrame == null) {
            // no frame explicitly given, use state frame
            projFrame = s.getFrame();
        } else {
            // use desired user frame
            projFrame = this.projectionFrame;
        }

        // Define the target vector
        Vector3D targetVector = this.targetDirection.getVector(null, s.getDate(), projFrame);

        // Define the reference vector
        Vector3D referenceVector;
        if (this.referenceDirection == null) {
            // Use the spacecraft state position vector
            referenceVector = s.getPVCoordinates(projFrame).getPosition();
        } else {
            // Use the reference direction vector
            referenceVector = this.referenceDirection.getVector(null, s.getDate(), projFrame);
        }

        // Normalize the reference vector if needed
        if (this.normalizeReference) {
            referenceVector = referenceVector.normalize();
        }
        // Normalize the target vector if needed
        if (this.normalizeTarget) {
            targetVector = targetVector.normalize();
        }

        // Computes the actual dot product value
        final double dotProduct = referenceVector.dotProduct(targetVector);

        // Computes and return the difference between the actual dot product and the threshold value
        return dotProduct - this.dotProductThreshold;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new DotProductDetector(this.targetDirection, this.normalizeReference, this.normalizeTarget,
            this.dotProductThreshold, this.projectionFrame, getSlopeSelection(), getMaxCheckInterval(),
            getThreshold(), this.actionAtAscending, this.actionAtDescending);
    }
}
