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
 * VERSION:4.14:OPENFD-:22/08/2024:
 * VERSION:4.14:OPENFD-141:22/08/2024: Isolation des algorithmes de somme et produit precis
 * VERSION:4.14:OPENFD-178:22/08/2024: [PATRIUS] Renommage de l'enumere DatationChoice
 * VERSION:4.14:OPENFD-179:22/08/2024: [PATRIUS] Gestion emetteur/recepteur dans les detecteurs d'evenements
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import java.util.HashMap;
import java.util.Map;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.detectors.LinkTypeHandler.SignalPropagationRole;
import fr.cnes.sirius.patrius.events.detectors.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Common parts shared by several events finders related to signal propagation concept.<br>
 * A default implementation of most of the methods of EventDetector Interface.<br>
 * Make it easier to create a new detector.
 * 
 * @see AbstractDetector
 * 
 * @author Thibaut BONIT
 *
 * @since 4.13
 */
public abstract class AbstractSignalPropagationDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 6783957131213226783L;

    /** Link type handler. */
    private final LinkTypeHandler linkTypeHandler;

    /** Propagation delay type (initialized to {@link PropagationDelayType#INSTANTANEOUS} by default). */
    private PropagationDelayType propagationDelayType;

    /** Inertial frame for signal propagation computation. */
    private Frame inertialFrame;

    /**
     * Epsilon for signal propagation computation (initialized to {@link VacuumSignalPropagationModel#DEFAULT_THRESHOLD}
     * by default).
     */
    private double epsSignalPropagation = VacuumSignalPropagationModel.DEFAULT_THRESHOLD;

    /**
     * Maximum number of iterations for signal propagation computation (initialized to
     * {@link VacuumSignalPropagationModel#DEFAULT_MAX_ITER} by default).
     */
    private int maxIterSignalPropagation = VacuumSignalPropagationModel.DEFAULT_MAX_ITER;

    /** Other element date: date at which the other element emits or receives the signal. */
    private final Map<AbsoluteDate, AbsoluteDate> otherDatesCache = new HashMap<>();

    /** Propagation delay type. */
    public enum PropagationDelayType {

        /** Instantaneous. */
        INSTANTANEOUS,

        /** Computed considering light speed. */
        LIGHT_SPEED;
    }

    /** Describe if the event datation type corresponds to the emitter date or the receiver date. */
    public enum EventDatationType {

        /** Emitter date. */
        EMITTER,

        /** Receiver date. */
        RECEIVER;
    }

    /**
     * Build a new instance. The detector will detect both ascending and descending g-function related events.
     * 
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     * @param linkTypeHandler
     *        link type handler
     */
    public AbstractSignalPropagationDetector(final double maxCheckIn, final double thresholdIn,
                                             final LinkTypeHandler linkTypeHandler) {
        super(maxCheckIn, thresholdIn);
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
        this.linkTypeHandler = linkTypeHandler;

    }

    /**
     * Build a new instance.
     * 
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     * @param linkTypeHandler
     *        link type handler
     */
    public AbstractSignalPropagationDetector(final int slopeSelectionIn, final double maxCheckIn,
                                             final double thresholdIn, final LinkTypeHandler linkTypeHandler) {
        super(slopeSelectionIn, maxCheckIn, thresholdIn);
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
        this.linkTypeHandler = linkTypeHandler;

    }

    /**
     * Build a new instance.
     *
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     * @param actionIn
     *        action performed when entering/exiting the eclipse depending on slope selection
     * @param removeIn
     *        when the spacecraft point enters or exit the zone depending on slope selection
     * @param linkTypeHandler
     *        link type handler
     */
    public AbstractSignalPropagationDetector(final int slopeSelectionIn, final double maxCheckIn,
                                             final double thresholdIn, final Action actionIn, final boolean removeIn,
                                             final LinkTypeHandler linkTypeHandler) {
        super(slopeSelectionIn, maxCheckIn, thresholdIn, actionIn, removeIn);
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
        this.linkTypeHandler = linkTypeHandler;

    }

    /**
     * Build a new instance. The detector will detect both ascending and descending g-function related events.
     *
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     * @param actionAtEntryIn
     *        action performed at increasing event detection
     * @param actionAtExitIn
     *        action performed at decreasing event detection
     * @param removeAtEntryIn
     *        states if the detector should be removed at increasing event detection
     * @param removeAtExitIn
     *        states if the detector should be removed at decreasing event detection
     * @param linkTypeHandler
     *        link type handler
     */
    public AbstractSignalPropagationDetector(final double maxCheckIn, final double thresholdIn,
                                             final Action actionAtEntryIn, final Action actionAtExitIn,
                                             final boolean removeAtEntryIn, final boolean removeAtExitIn,
                                             final LinkTypeHandler linkTypeHandler) {
        super(maxCheckIn, thresholdIn, actionAtEntryIn, actionAtExitIn, removeAtEntryIn, removeAtExitIn);
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
        this.linkTypeHandler = linkTypeHandler;

    }

    /**
     * Build a new instance.
     *
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     * @param actionAtEntryIn
     *        action performed at increasing event detection
     * @param actionAtExitIn
     *        action performed at decreasing event detection
     * @param removeAtEntryIn
     *        states if the detector should be removed at increasing event detection
     * @param removeAtExitIn
     *        states if the detector should be removed at decreasing event detection
     * @param linkTypeHandler
     *        link type handler
     */
    public AbstractSignalPropagationDetector(final int slopeSelectionIn, final double maxCheckIn,
                                             final double thresholdIn, final Action actionAtEntryIn,
                                             final Action actionAtExitIn, final boolean removeAtEntryIn,
                                             final boolean removeAtExitIn, final LinkTypeHandler linkTypeHandler) {
        super(slopeSelectionIn, maxCheckIn, thresholdIn, actionAtEntryIn, actionAtExitIn, removeAtEntryIn,
                removeAtExitIn);
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
        this.linkTypeHandler = linkTypeHandler;

    }

    /**
     * Getter for the link type handler.
     * 
     * @return the link type handler
     */
    public LinkTypeHandler getLinkTypeHandler() {
        return this.linkTypeHandler;
    }

    /**
     * Getter for the signal emitter.
     * 
     * @param s
     *        the spacecraft state used by the detector
     * @return the signal emitter
     */
    public PVCoordinatesProvider getEmitter(final SpacecraftState s) {
        final PVCoordinatesProvider emitter;
        // Determine the signal emitter depending on the role of the main element (SpacecraftState)
        if (this.linkTypeHandler.getMainRole() == SignalPropagationRole.EMITTER) {
            emitter = s.getOrbit();
        } else {
            emitter = this.linkTypeHandler.getOtherElement();
        }
        return emitter;
    }

    /**
     * Getter for the signal receiver.
     * 
     * @param s
     *        the spacecraft state used by the detector
     * @return the signal receiver
     */
    public PVCoordinatesProvider getReceiver(final SpacecraftState s) {
        final PVCoordinatesProvider receiver;
        // Determine the signal receiver depending on the role of the main element (SpacecraftState)
        if (this.linkTypeHandler.getMainRole() == SignalPropagationRole.RECEIVER) {
            receiver = s.getOrbit();
        } else {
            receiver = this.linkTypeHandler.getOtherElement();
        }
        return receiver;
    }

    /**
     * Specify if the event datation type corresponds to the emitter date or the receiver date.
     * 
     * @return the corresponding event datation type
     */
    public EventDatationType getEventDatationType() {
        final EventDatationType eventDatationType;
        // Determine the event datation type depending on role of the main element (SpacecraftState)
        if (this.linkTypeHandler.getMainRole() == SignalPropagationRole.EMITTER) {
            eventDatationType = EventDatationType.EMITTER;
        } else {
            eventDatationType = EventDatationType.RECEIVER;
        }
        return eventDatationType;
    }

    /**
     * Setter for the propagation delay computation type. Warning: check Javadoc of detector to see if detector takes
     * into account propagation time delay. if not, signals are always considered instantaneous. The provided frame is
     * used to compute the signal propagation when delay is taken into account.
     * 
     * @param propagationDelayTypeIn
     *        Propagation delay type used in events computation
     * @param frameIn
     *        Frame to use for signal propagation with delay (may be null if propagation delay type is
     *        considered instantaneous). Warning: the usage of a pseudo inertial frame is tolerated, however it will
     *        lead to some inaccuracies due to the non-invariance of the frame with respect to time. For this reason,
     *        it is suggested to use the ICRF frame or a frame which is frozen with respect to the ICRF.
     * @throws IllegalArgumentException
     *         if the provided frame is not pseudo inertial.
     */
    public void setPropagationDelayType(final PropagationDelayType propagationDelayTypeIn, final Frame frameIn) {

        // check whether the provided frame is pseudo inertial or not
        if (frameIn != null && !frameIn.isPseudoInertial()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME, frameIn);
        }
        this.propagationDelayType = propagationDelayTypeIn;
        this.inertialFrame = frameIn;
    }

    /**
     * Compute the signal emission date which is the date at which the signal received by the spacecraft (receiver) has
     * been emitted by the emitter depending on {@link PropagationDelayType}.
     * 
     * @param s
     *        the spacecraft state used by the detector
     * @return the signal emission date
     * @throws PatriusException
     *         if computation failed
     */
    public AbsoluteDate getSignalEmissionDate(final SpacecraftState s) throws PatriusException {
        return this.getSignalEmissionDate(this.getEmitter(s), s.getOrbit(), s.getDate());
    }

    /**
     * Compute the signal emission date which is the date at which the signal received by the spacecraft (receiver) has
     * been emitted by the emitter depending on {@link PropagationDelayType}.
     * 
     * @param emitter
     *        Emitter
     * @param orbit
     *        Orbit of the spacecraft (receiver)
     * @param date
     *        Date at which the spacecraft orbit is defined
     * @return the signal emission date
     * @throws PatriusException
     *         if computation failed
     */
    public AbsoluteDate getSignalEmissionDate(final PVCoordinatesProvider emitter,
                                              final PVCoordinatesProvider orbit, final AbsoluteDate date)
        throws PatriusException {
        return VacuumSignalPropagationModel.getSignalEmissionDate(emitter, orbit, date, this.epsSignalPropagation,
            this.propagationDelayType, this.inertialFrame, this.maxIterSignalPropagation);
    }

    /**
     * Compute the signal reception date which is the date at which the signal emitted by the spacecraft (emitter) has
     * been received by the receiver depending on {@link PropagationDelayType}.
     * 
     * @param s
     *        the spacecraft state used by the detector
     * @return the signal reception date
     * @throws PatriusException thrown if computation failed
     */
    public AbsoluteDate getSignalReceptionDate(final SpacecraftState s) throws PatriusException {
        return this.getSignalReceptionDate(this.getReceiver(s), s.getOrbit(), s.getDate());
    }

    /**
     * Compute the signal reception date which is the date at which the signal emitted by the spacecraft (emitter) has
     * been received by the receiver depending on {@link PropagationDelayType}.
     * 
     * @param receiver
     *        Receiver
     * @param orbit
     *        Orbit of the spacecraft (emitter)
     * @param date
     *        Date at which the spacecraft orbit is defined
     * @return the signal reception date
     * @throws PatriusException thrown if computation failed
     */
    public AbsoluteDate getSignalReceptionDate(final PVCoordinatesProvider receiver,
                                               final PVCoordinatesProvider orbit, final AbsoluteDate date)
        throws PatriusException {
        return VacuumSignalPropagationModel.getSignalReceptionDate(receiver, orbit, date, this.epsSignalPropagation,
            this.propagationDelayType, this.inertialFrame, this.maxIterSignalPropagation);
    }

    /**
     * Compute other element date taking into account {@link PropagationDelayType} and {@link LinkType}.
     * <p>
     * Note: once computed, the other element date is saved in cache. one for each spacecraftstate date.
     * </p>
     * 
     * @param s
     *        spacecraft state
     * @return other element date
     * @throws PatriusException
     *         if computation failed
     */
    public AbsoluteDate getOtherDate(final SpacecraftState s) throws PatriusException {
        // Define other element date: date at which the other element emits or receives the signal
        if (!this.otherDatesCache.containsKey(s.getDate())) {
            // Check the type of link
            if (this.linkTypeHandler.getMainRole() == SignalPropagationRole.EMITTER) {
                // The main role satellite is the emitter, the other element is the receiver, so compute the reception
                // date
                this.otherDatesCache.put(s.getDate(), this.getSignalReceptionDate(s));
            } else {
                // The main role satellite is the emitter, the other element is the receiver, so compute the emission
                // date
                this.otherDatesCache.put(s.getDate(), this.getSignalEmissionDate(s));
            }
        }
        return this.otherDatesCache.get(s.getDate());
    }

    /**
     * Getter for the propagation delay type.
     * 
     * @return the propagation delay type
     */
    public PropagationDelayType getPropagationDelayType() {
        return this.propagationDelayType;
    }

    /**
     * Getter for the inertial frame used for signal propagation computation.
     * 
     * @return the inertial frame
     */
    public Frame getInertialFrame() {
        return this.inertialFrame;
    }

    /**
     * Setter for the epsilon for signal propagation when signal propagation is taken into account.<br>
     * This epsilon (in s) directly reflect the accuracy of signal propagation (1s of accuracy = 3E8m of accuracy on
     * distance between emitter and receiver)
     * 
     * @param epsilon
     *        Epsilon for the signal propagation
     */
    public void setEpsilonSignalPropagation(final double epsilon) {
        this.epsSignalPropagation = epsilon;
    }

    /**
     * Getter for the epsilon for signal propagation when signal propagation is taken into account.
     * 
     * @return the epsilon for signal propagation when signal propagation is taken into account
     */
    public double getEpsilonSignalPropagation() {
        return this.epsSignalPropagation;
    }

    /**
     * Setter for the maximum number of iterations for signal propagation when signal propagation is taken into account.
     * 
     * @param maxIterSignalPropagationIn
     *        Maximum number of iterations for signal propagation
     */
    public void setMaxIterSignalPropagation(final int maxIterSignalPropagationIn) {
        this.maxIterSignalPropagation = maxIterSignalPropagationIn;
    }

    /**
     * Getter for the maximum number of iterations for signal propagation when signal propagation is taken into account.
     * 
     * @return the maximum number of iterations for signal propagation
     */
    public int getMaxIterSignalPropagation() {
        return this.maxIterSignalPropagation;
    }
}
