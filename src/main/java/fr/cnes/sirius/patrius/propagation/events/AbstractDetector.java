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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-73:30/06/2023:[PATRIUS] Reliquat OPENFD-14 VacuumSignalPropagationModel
 * VERSION:4.11.1:FA:FA-83:30/06/2023:[PATRIUS] Confusion dans les nombres max d'itérations dans AbstractDetector
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de AbstractEOPHistory
 * VERSION:4.11:DM:DM-14:22/05/2023:[PATRIUS] Nombre max d'iterations dans le calcul de la propagation du signal 
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Forcer normalisation dans QuaternionPolynomialSegment
 * VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3173:10/05/2022:[PATRIUS] Utilisation de FacetCelestialBody dans les calculs d'evenements 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3148:10/05/2022:[PATRIUS] Erreur dans la methode getSignalReceptionDate de la classe AbstractDet
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved() to manage detector suppression
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.CodingEventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Common parts shared by several events finders.
 * A default implementation of most of the methods of EventDetector Interface.
 * Make it easier to create a new detector.
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public abstract class AbstractDetector implements EventDetector {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s) for the algorithm which searches for the zero of the g function. */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default maximum number of iterations allowed for the algorithm which searches for the zero of the g function. */
    public static final int DEFAULT_MAXITER = 100;
    
    /** Serializable UID. */
    private static final long serialVersionUID = -8212002898109868489L;

    /** Action performed at entry */
    protected Action actionAtEntry;

    /** Action performed at exit */
    protected Action actionAtExit;

    /** True if detector should be removed at entry */
    protected boolean removeAtEntry;

    /** True if detector should be removed at exit */
    protected boolean removeAtExit;

    /** True if detector should be removed (updated by eventOccured) */
    protected boolean shouldBeRemovedFlag = false;

    /** Convergence threshold. */
    private final double threshold;

    /** Maximum number of iterations allowed for the algorithm which searches for the zero of the g function. */
    private int maxIter = DEFAULT_MAXITER;

    /** Select all events, increasing g related events or decreasing g related events only. */
    private final int slopeSelection;

    /** Max check interval. */
    private double maxCheck;

    /** Propagation delay type. Default is INSTANTANEOUS. */
    private PropagationDelayType propagationDelayType;
    
    /** Inertial frame for signal propagation computation. */
    private Frame inertialFrame;

    /** Epsilon for signal propagation computation. */
    private double epsSignalPropagation = VacuumSignalPropagationModel.DEFAULT_THRESHOLD;

    /** Maximum number of iterations for signal propagation computation */
    private int maxIterSignalPropagation = VacuumSignalPropagationModel.DEFAULT_MAX_ITER;

    /**
     * Propagation delay type.
     */
    public enum PropagationDelayType {

        /** Instantaneous. */
        INSTANTANEOUS,

        /** Computed considering light speed. */
        LIGHT_SPEED;
    }

    /**
     * Build a new instance. The detector will detect both ascending and descending g-function related events.
     * 
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     */
    public AbstractDetector(final double maxCheckIn, final double thresholdIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        this.slopeSelection = INCREASING_DECREASING;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;

        // Flags for detector behavior
        this.removeAtEntry = false;
        this.removeAtExit = false;
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
     */
    public AbstractDetector(final int slopeSelectionIn, final double maxCheckIn, final double thresholdIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        // Validate input
        if (slopeSelectionIn != 0 && slopeSelectionIn != 1 && slopeSelectionIn != 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_SLOPE_SELECTION_TYPE);
        }
        this.slopeSelection = slopeSelectionIn;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;

        // Flags for detector behavior
        this.removeAtEntry = false;
        this.removeAtExit = false;
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
     */
    public AbstractDetector(final int slopeSelectionIn, final double maxCheckIn, final double thresholdIn,
                            final Action actionIn, final boolean removeIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        // Validate input
        if (slopeSelectionIn != 0 && slopeSelectionIn != 1 && slopeSelectionIn != 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_SLOPE_SELECTION_TYPE);
        }
        this.slopeSelection = slopeSelectionIn;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;

        // Flags for detector behavior
        if (slopeSelectionIn == INCREASING) {
            this.actionAtExit = actionIn;
            this.removeAtExit = removeIn;
            this.removeAtEntry = false;
        } else if (slopeSelectionIn == DECREASING) {
            this.actionAtEntry = actionIn;
            this.removeAtExit = false;
            this.removeAtEntry = removeIn;
        } else {
            this.actionAtExit = actionIn;
            this.actionAtEntry = actionIn;
            this.removeAtExit = removeIn;
            this.removeAtEntry = removeIn;
        }
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
     */
    public AbstractDetector(final double maxCheckIn, final double thresholdIn, final Action actionAtEntryIn,
                            final Action actionAtExitIn, final boolean removeAtEntryIn, final boolean removeAtExitIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        this.slopeSelection = INCREASING_DECREASING;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;

        // Flags for detector behavior
        this.actionAtEntry = actionAtEntryIn;
        this.actionAtExit = actionAtExitIn;
        this.removeAtEntry = removeAtEntryIn;
        this.removeAtExit = removeAtExitIn;
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
     */
    public AbstractDetector(final int slopeSelectionIn, final double maxCheckIn, final double thresholdIn,
                            final Action actionAtEntryIn, final Action actionAtExitIn, final boolean removeAtEntryIn,
                            final boolean removeAtExitIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        // Validate input
        if (slopeSelectionIn != 0 && slopeSelectionIn != 1 && slopeSelectionIn != 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_SLOPE_SELECTION_TYPE);
        }
        this.slopeSelection = slopeSelectionIn;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;

        // Flags for detector behavior
        this.actionAtEntry = actionAtEntryIn;
        this.actionAtExit = actionAtExitIn;
        this.removeAtEntry = removeAtEntryIn;
        this.removeAtExit = removeAtExitIn;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
        // do nothing by default
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        // Check if it is increasing
        if (increasing) {
            // It is increasing, so shouldBeRemovedFlag is equal to removeAtEntry
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            // It is not increasing, so shouldBeRemovedFlag is equal to removeAtExit
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        }

        // Return actionAtEntry or actionAtExit depending on whether it is increasing or not
        return increasing ? this.actionAtEntry : this.actionAtExit;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public abstract double g(SpacecraftState s) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.maxCheck;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return maxIter;
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.threshold;
    }

    /**
     * @return the action at entry
     */
    public Action getActionAtEntry() {
        return this.actionAtEntry;
    }

    /**
     * @return the action at exit
     */
    public Action getActionAtExit() {
        return this.actionAtExit;
    }

    /**
     * @return the flag removeAtEntry
     */
    public boolean isRemoveAtEntry() {
        return this.removeAtEntry;
    }

    /**
     * @return the flag removeAtExit
     */
    public boolean isRemoveAtExit() {
        return this.removeAtExit;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        return oldState;
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.slopeSelection;
    }

    /**
     * Compute signal emission date which is the date at which the signal received by the spacecraft (receiver) has been
     * emitted by the emitter depending on {@link PropagationDelayType}.
     * 
     * @param emitter emitter
     * @param orbit orbit of the spacecraft (receiver)
     * @param date date at which the spacecraft orbit is defined
     * @return signal emission date
     * @throws PatriusException thrown if computation failed
     */
    protected AbsoluteDate getSignalEmissionDate(final PVCoordinatesProvider emitter,
                                                 final PVCoordinatesProvider orbit,
                                                 final AbsoluteDate date)
        throws PatriusException {
        return VacuumSignalPropagationModel.getSignalEmissionDate(emitter, orbit, date, this.epsSignalPropagation,
            this.propagationDelayType, this.inertialFrame, this.maxIterSignalPropagation);
    }

    /**
     * Compute signal reception date which is the date at which the signal emitted by the spacecraft (emitter) has been
     * received by the receiver depending on {@link PropagationDelayType}.
     * 
     * @param receiver receiver
     * @param orbit orbit of the spacecraft (emitter)
     * @param date date at which the spacecraft orbit is defined
     * @return signal reception date
     * @throws PatriusException thrown if computation failed
     */
    protected AbsoluteDate getSignalReceptionDate(final PVCoordinatesProvider receiver,
                                                  final PVCoordinatesProvider orbit,
                                                  final AbsoluteDate date)
        throws PatriusException {
        return VacuumSignalPropagationModel.getSignalReceptionDate(receiver, orbit, date, this.epsSignalPropagation,
            this.propagationDelayType, this.inertialFrame, this.maxIterSignalPropagation);
    }

    /**
     * Returns the propagation delay type.
     * @return the propagation delay type
     */
    public PropagationDelayType getPropagationDelayType() {
        return this.propagationDelayType;
    }
    
    /**
     * Gets the inertial frame used for signal propagation computation.
     * @return the inertial frame
     */
    public Frame getInertialFrame() {
        return this.inertialFrame;
    }
    
    /**
     * Setter for propagation delay computation type. Warning: check Javadoc of detector to see if detector takes into
     * account propagation time delay. if not, signals are always considered instantaneous. The provided frame is used
     * to compute the signal propagation when delay is taken into account.
     * 
     * @param propagationDelayTypeIn propagation delay type used in events computation
     * @param frameIn frame to use for signal propagation with delay (may be null if propagation delay type is
     *        considered instantaneous). Warning: the usage of a pseudo inertial frame is tolerated, however it will
     *        lead to some inaccuracies due to the non-invariance of the frame with respect to time. For this reason, 
     *        it is suggested to use the ICRF frame or a frame which is frozen with respect to the ICRF.
     * @exception IllegalArgumentException if the provided frame is not pseudo inertial.
     */
    protected void setPropagationDelayType(final PropagationDelayType propagationDelayTypeIn, final Frame frameIn) {
        
        // check whether the provided frame is pseudo inertial or not
        if (frameIn != null && !frameIn.isPseudoInertial()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME, frameIn);
        }
        this.propagationDelayType = propagationDelayTypeIn;
        this.inertialFrame = frameIn;
    }

    /**
     * Setter for the max check interval.
     * 
     * @param maxCheckIn the max check interval to set
     */
    public void setMaxCheckInterval(final double maxCheckIn) {
        this.maxCheck = maxCheckIn;
    }

    /**
     * Log detected events on a given time interval into the entered events logger.
     *
     * @param eventsLogger
     *        input events logger in which the events must be recorded
     * @param satProp
     *        spacecraft orbit propagator
     * @param detector
     *        detector to be monitored
     * @param interval
     *        time interval on which the events are looked for
     *
     * @return the spacecraft state after propagation
     * @throws PatriusException
     *         if spacecraft state cannot be propagated
     */
    public static SpacecraftState logEventsOverTimeInterval(final CodedEventsLogger eventsLogger,
                                                            final Propagator satProp,
                                                            final CodingEventDetector detector,
                                                            final AbsoluteDateInterval interval)
        throws PatriusException {

        // let's the logger monitor the visi detector
        final EventDetector monitoredDetector = eventsLogger.monitorDetector(detector);

        // add the detector to satProp
        satProp.addEventDetector(monitoredDetector);

        // propagation
        final SpacecraftState finalState = satProp.propagate(interval.getLowerData(), interval.getUpperData());

        // clear events detectors
        satProp.clearEventsDetectors();

        return finalState;
    }

    /**
     * Set the epsilon for signal propagation when signal propagation is taken into account.
     * This epsilon (in s) directly reflect the accuracy of signal propagation (1s of accuracy = 3E8m of accuracy on
     * distance between emitter and receiver)
     * @param epsilon epsilon for signal propagation
     */
    public void setEpsilonSignalPropagation(final double epsilon) {
        this.epsSignalPropagation = epsilon;
    }

    /**
     * Set the maximum number of iterations for signal propagation when signal propagation is taken into account.
     * 
     * @param maxIterSignalPropagationIn maximum number of iterations for signal propagation
     */
    public void setMaxIterSignalPropagation(final int maxIterSignalPropagationIn) {
        this.maxIterSignalPropagation = maxIterSignalPropagationIn;
    }

	/**
	 * Get the maximum number of iterations for signal propagation when signal
	 * propagation is taken into account.
	 * 
	 * @return returns the maximum number of iterations for signal propagation
	 *         when signal propagation is taken into account.
	 */
    public int getMaxIterSignalPropagation() {
        return this.maxIterSignalPropagation;
    }

    /**
     * Set the maximum number of iterations allowed for the algorithm which searches for the zero of the g function.
     * 
     * @param maxIterIn maximum number of iterations allowed for the algorithm which searches for the zero of the g
     *        function
     */
    public void setMaxIter(final int maxIterIn) {
        this.maxIter = maxIterIn;
    }
}
