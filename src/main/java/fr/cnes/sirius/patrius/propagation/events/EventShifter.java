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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Wrapper shifting events occurrences times.
 * <p>
 * This class wraps an {@link EventDetector event detector} to slightly shift the events occurrences times. A typical
 * use case is for handling operational delays before or after some physical event really occurs.
 * </p>
 * <p>
 * For example, the satellite attitude mode may be switched from sun pointed to spin-stabilized a few minutes before
 * eclipse entry, and switched back to sun pointed a few minutes after eclipse exit. This behavior is handled by
 * wrapping an {@link EclipseDetector eclipse detector} into an instance of this class with a positive times shift for
 * increasing events (eclipse exit) and a negative times shift for decreasing events (eclipse entry).
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @see EventDetector
 * @author Luc Maisonobe
 */
public class EventShifter extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4910163524748330428L;

    /** Event detector for the raw unshifted event. */
    private final EventDetector detector;

    /** Indicator for using shifted or unshifted states at event occurrence. */
    private final boolean useShiftedStates;

    /** Offset to apply to find increasing events. */
    private final double increasingOffset;

    /** Offset to apply to find decreasing events. */
    private final double decreasingOffset;

    /**
     * Build a new instance.
     * <p>
     * The {@link #getMaxCheckInterval() max check interval}, the {@link #getThreshold() convergence threshold} of the
     * raw unshifted events will be used for the shifted event. When an event occurs, the
     * {@link #eventOccurred(SpacecraftState, boolean, boolean) eventOccurred} method of the raw unshifted events will
     * be called (with spacecraft state at either the shifted or the unshifted event date depending on the
     * <code>useShiftedStates</code> parameter).
     * </p>
     * 
     * @param detectorIn
     *        event detector for the raw unshifted event
     * @param useShiftedStatesIn
     *        if true, the state provided to {@link #eventOccurred(SpacecraftState, boolean, boolean) eventOccurred}
     *        method of
     *        the <code>detector</code> will remain shifted, otherwise it will
     *        be <i>unshifted</i> to correspond to the underlying raw event.
     * @param increasingTimeShift
     *        increasing events time shift.
     * @param decreasingTimeShift
     *        decreasing events time shift.
     */
    public EventShifter(final EventDetector detectorIn, final boolean useShiftedStatesIn,
        final double increasingTimeShift, final double decreasingTimeShift) {
        super(detectorIn.getMaxCheckInterval(), detectorIn.getThreshold());
        this.detector = detectorIn;
        this.useShiftedStates = useShiftedStatesIn;
        this.increasingOffset = -increasingTimeShift;
        this.decreasingOffset = -decreasingTimeShift;
    }

    /**
     * Get the increasing events time shift.
     * 
     * @return increasing events time shift
     */
    public double getIncreasingTimeShift() {
        return this.increasingOffset;
    }

    /**
     * Get the decreasing events time shift.
     * 
     * @return decreasing events time shift
     */
    public double getDecreasingTimeShift() {
        return this.decreasingOffset;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
        this.detector.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.detector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {

        if (this.useShiftedStates) {
            // the state provided by the caller already includes the time shift
            return this.detector.eventOccurred(s, increasing, forward);
        }

        // we need to "unshift" the state
        final double offset = increasing ? this.increasingOffset : this.decreasingOffset;
        return this.detector.eventOccurred(s.shiftedBy(offset), increasing, forward);

    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        final double incShiftedG = this.detector.g(s.shiftedBy(this.increasingOffset));
        final double decShiftedG = this.detector.g(s.shiftedBy(this.decreasingOffset));
        return (this.increasingOffset >= this.decreasingOffset) ?
            MathLib.max(incShiftedG, decShiftedG) : MathLib.min(incShiftedG, decShiftedG);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>topo: {@link TopocentricFrame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new EventShifter(this.detector.copy(), this.useShiftedStates, this.increasingOffset,
            this.decreasingOffset);
    }
}
