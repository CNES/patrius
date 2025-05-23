/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history created 16/05/12
 * 
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::FA:1307:11/09/2017:correct formulation of g() function
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.10:DM:DM-3238:03/11/2022:[PATRIUS] Masquages par des corps celestes dans VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AzimuthElevationCalculator;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects the time when the spacecraft reaches the elevation extrema in a given topocentric
 * frame.<pr> The local minimum or maximum is chosen through a constructor parameter, with values
 * {@link ExtremaElevationDetector#MIN}, {@link ExtremaElevationDetector#MAX} and
 * {@link ExtremaElevationDetector#MIN_MAX} for both.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the
 * minimum/maximum elevation is reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the link to the tree of frames makes this class not thread-safe
 * 
 * @see EventDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ExtremaElevationDetector.java 17902 2017-09-11 09:17:02Z bignon $
 * 
 * @since 1.2
 */
public class ExtremaElevationDetector extends AbstractSignalPropagationDetector {

    /** Flag for local minimum elevation detection. */
    public static final int MIN = 0;

    /** Flag for local maximum elevation detection. */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum elevation detection. */
    public static final int MIN_MAX = 2;

     /** Serializable UID. */
    private static final long serialVersionUID = -4883716696126831700L;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Action performed */
    private final Action actionExtremaElevation;
    
    /** Type of link (it can be uplink or downlink, or null if instantaneous propagation). */
    private final LinkType linkType;

    /**
     * Constructor for a min and max elevation detector.
     * <p>
     * This constructor takes default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck) {
        this(topoFrame, extremumType, maxCheck, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extremum is reached.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck, final double threshold) {
        this(topoFrame, extremumType, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at extrema elevation detection
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck, final double threshold, final Action action) {
        this(topoFrame, extremumType, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at extrema elevation detection
     * @param remove true if detector should be removed at extrema elevation detection
     * @since 3.1
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        this(topoFrame, extremumType, maxCheck, threshold, action, remove, null);
    }
    
    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at extrema elevation detection
     * @param remove true if detector should be removed at extrema elevation detection
     * @param linkTypeIn the type of link (it can be uplink or downlink, or null if instantaneous propagation)
     * @since 4.10
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType, final double maxCheck,
                        final double threshold, final Action action, final boolean remove, final LinkType linkTypeIn) {
        super(extremumType, maxCheck, threshold);
        this.topo = topoFrame;
        // action
        this.actionExtremaElevation = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
        // Set link type
        this.linkType = linkTypeIn;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // does nothing
    }

    /**
     * Handle an extrema distance event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected extrema elevation is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionExtremaElevation;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Event occurs when elevation rate in topocentric frame cancels
        return getElevationRate(state);
    }
    
    /**
     * Getter for the elevation rate of a point.
     * 
     * @param state
     *        the current state information: date, kinematics, attitude
     * @return elevation rate of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    private double getElevationRate(final SpacecraftState state) throws PatriusException {

        final PVCoordinatesProvider extPVProv = state.getOrbit();
        final Frame frame = state.getFrame();
        final AbsoluteDate date = state.getDate();

        final PVCoordinates extPVTopo;
        // Case of light speed propagation (dedicated in order to optimize computation times)
        if (getPropagationDelayType().equals(PropagationDelayType.LIGHT_SPEED)) {

            // Check the type of link
            if (this.linkType.equals(LinkType.DOWNLINK)) {
                // It is downlink
                // Emitter is the satellite, station is the receiver (since elevation is wrt to station),
                // so compute the reception date
                // Parameter "date" is signal emission date from the satellite
                final AbsoluteDate stationReceptionDate = getSignalReceptionDate(state);

                // Compute satellite PV coordinates in topocentric frame
                // The transformation date is reception date by the station
                final PVCoordinates extPV = extPVProv.getPVCoordinates(date, frame);
                final Transform t = frame.getTransformTo(this.topo, stationReceptionDate);
                extPVTopo = t.transformPVCoordinates(extPV);

            } else {
                // It is uplink
                // Emitter is the station, satellite is the receiver, so compute the emission date
                // Parameter "date" is signal reception date at the satellite
                final AbsoluteDate stationEmissionDate = getSignalEmissionDate(state);

                // Compute satellite PV coordinates in topocentric frame
                // The transformation date is emission date by the station
                final PVCoordinates extPV = extPVProv.getPVCoordinates(date, frame);
                final Transform t = frame.getTransformTo(this.topo, stationEmissionDate);
                extPVTopo = t.transformPVCoordinates(extPV);
            }
        } else {
            // Instantaneous case
            extPVTopo = extPVProv.getPVCoordinates(date, this.topo);
        }

        // Compute elevation rate as seen from the station
        return AzimuthElevationCalculator.computeElevationRate(extPVTopo);

    }

    /**
     * @return the Topocentric frame in which elevation should be evaluated.
     */
    public TopocentricFrame getTopocentricFrame() {
        return this.topo;
    }
    
    /**
     * Returns the type of link (it can be uplink or downlink).
     * 
     * @return the type of link (it can be uplink or downlink)
     */
    public LinkType getLinkType() {
        return this.linkType;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getEmitter(final SpacecraftState s) {
        final PVCoordinatesProvider emitter;
        if (this.linkType == LinkType.UPLINK) {
            emitter = this.topo;
        } else { // DOWNLINK
            emitter = s.getOrbit();
        }
        return emitter;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getReceiver(final SpacecraftState s) {
        final PVCoordinatesProvider receiver;
        if (this.linkType == LinkType.UPLINK) {
            receiver = s.getOrbit();
        } else { // DOWNLINK
            receiver = this.topo;
        }
        return receiver;
    }

    /** {@inheritDoc} */
    @Override
    public DatationChoice getDatationChoice() {
        final DatationChoice datationChoice;
        if (this.linkType == LinkType.UPLINK) {
            datationChoice = DatationChoice.RECEIVER;
        } else { // DOWNLINK
            datationChoice = DatationChoice.EMITTER;
        }
        return datationChoice;
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
        return new ExtremaElevationDetector(this.topo, this.getSlopeSelection(), this.getMaxCheckInterval(),
            this.getThreshold(), this.actionExtremaElevation, this.shouldBeRemovedFlag, this.linkType);
    }
}
