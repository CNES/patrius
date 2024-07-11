/**
 * 
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
 * @history created 29/05/12
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3196:01/06/2022:[PATRIUS] Methode setPropagationDelayType non surchargee
 * VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the signal correction classes
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPosition;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.TroposphericCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Finder for satellite apparent entering in a station's field of view.
 * <p>
 * The tropospheric correction used can be set by the user.
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting. This can
 * be changed by using one of the provided constructors.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class not
 *                      thread-safe itself
 * 
 * @see TroposphericCorrection
 * @see EventDetector
 * @see GeometricStationAntenna
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class VisibilityFromStationDetector extends AbstractDetector {

    /**
     * Type of link (it can be uplink or downlink).
     *
     */
    public enum LinkType {
        /** Uplink (from ground station to satellite) type of link. */
        UPLINK,

        /** Downlink (from satellite to ground station) type of link. */
        DOWNLINK;
    }

    /** Flag for raising detection (slopeSelection = 0). */
    public static final int RAISING = 0;

    /** Flag for setting detection (slopeSelection = 1). */
    public static final int SETTING = 1;

    /** Flag for raising/setting detection (slopeSelection = 2). */
    public static final int RAISING_SETTING = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 764052454048950887L;

    /** The correction model to apply to the station elevation. */
    private final AngularCorrection correction;

    /** Topocentric frame in which elevation should be evaluated. */
    private final transient GeometricStationAntenna station;
    
    /** Type of link (it can be uplink or downlink). */
    private final LinkType linkType;

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final TopocentricFrame topoFrame,
        final double[][] azimElevMask, final AngularCorrection correctionModel,
        final double maxCheck, final double threshold, final LinkType linkTypeIn) {
        this(topoFrame, azimElevMask, correctionModel, maxCheck, threshold, Action.CONTINUE,
            Action.STOP, linkTypeIn);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final TopocentricFrame topoFrame,
        final double[][] azimElevMask, final AngularCorrection correctionModel,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting, final LinkType linkTypeIn) {
        this(topoFrame, azimElevMask, correctionModel, maxCheck, threshold, raising, setting,
            false, false, linkTypeIn);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising true if detector should be removed at raising
     * @param removeSetting true if detector should be removed at setting
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final TopocentricFrame topoFrame,
                                         final double[][] azimElevMask, final AngularCorrection correctionModel,
                                         final double maxCheck, final double threshold, final Action raising,
                                         final Action setting, final boolean removeRaising,
                                         final boolean removeSetting,
                                         final LinkType linkTypeIn) {
        super(maxCheck, threshold, raising, setting, removeRaising, removeSetting);
        // set the correction model and the station
        this.correction = correctionModel;
        this.station = new GeometricStationAntenna(topoFrame, azimElevMask);
        // set the type of link
        this.linkType = linkTypeIn;
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final LinkType linkTypeIn) {
        this(stationModel, correctionModel, maxCheck, threshold, Action.CONTINUE, Action.STOP, linkTypeIn);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final Action raising, final Action setting, final LinkType linkTypeIn) {
        this(stationModel, correctionModel, maxCheck, threshold, raising, setting, false, false, linkTypeIn);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising true if detector should be removed at raising
     * @param removeSetting true if detector should be removed at setting
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
                                         final AngularCorrection correctionModel, final double maxCheck,
                                         final double threshold,
                                         final Action raising, final Action setting, final boolean removeRaising,
                                         final boolean removeSetting, final LinkType linkTypeIn) {
        super(maxCheck, threshold, raising, setting, removeRaising, removeSetting);
        // set the correction model and the station
        this.correction = correctionModel;
        this.station = stationModel;
        // set the type of link
        this.linkType = linkTypeIn;
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param slopeSelection slope selection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed when propagation at raising/setting depending on slope
     *        selection
     * @param remove true if detector should be removed at raising/setting depending on slope
     *        selection
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
                                         final AngularCorrection correctionModel, final int slopeSelection,
                                         final double maxCheck, final double threshold, final Action action,
                                         final boolean remove, final LinkType linkTypeIn) {
        super(slopeSelection, maxCheck, threshold);
        // set the correction model and the station
        this.correction = correctionModel;
        this.station = stationModel;
        // set the type of link
        this.linkType = linkTypeIn;
        if (slopeSelection == RAISING) {
            this.actionAtEntry = action;
            this.actionAtExit = null;
            this.removeAtEntry = remove;
            this.removeAtExit = false;
        } else if (slopeSelection == SETTING) {
            this.actionAtEntry = null;
            this.actionAtExit = action;
            this.removeAtEntry = false;
            this.removeAtExit = remove;
        } else {
            // detection at ascending and descending node
            this.actionAtEntry = action;
            this.actionAtExit = action;
            this.removeAtEntry = remove;
            this.removeAtExit = remove;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle "visibility from station" event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event.
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when propagation raising or setting.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == RAISING) {
            result = this.getActionAtEntry();
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else if (this.getSlopeSelection() == SETTING) {
            result = this.getActionAtExit();
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else {
            if (forward ^ !increasing) {
                // ascending node case
                result = this.getActionAtEntry();
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            } else {
                // descending node case
                result = this.getActionAtExit();
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            }
        }
        // Return result
        return result;
    }

    /**
     * Compute the value of the switching function.
     * <p>
     * This function measures the angular distance between the current apparent vector to the spacecraft and the border
     * of the station's field of view. It is positive when the spacecraft is in the field
     * </p>
     * 
     * @param state state
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        final Vector3D correctedVector = this.getCorrectedVector(state.getOrbit(), state.getDate(), state.getFrame(), 
                this.linkType);
        return this.getStation().getFOV().getAngularDistance(correctedVector);
    }

    /**
     * Compute the apparent vector from the station to the spacecraft with tropospheric effects.
     * 
     * @param pvProv
     *        PV coordinates provider consistent with the {@link Propagator} that will be used for events detection
     * @param date
     *        date of the current spacecraft state
     * @param inertialFrame
     *        reference frame of the current spacecraft state: must be inertial
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     * @return the apparent vector from the station to the spacecraft with tropospheric effects
     * @exception PatriusException if some specific error occurs
     */
    private Vector3D getCorrectedVector(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                        final Frame inertialFrame, final LinkType linkTypeIn)
        throws PatriusException {
        // Define relevant date
        AbsoluteDate relevantDate = null;
        // Check that the type of link is not null
        if (linkTypeIn == null) {
            // The type of link is null
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_LINK_TYPE);
        } else {
            // Check the type of link
            if (linkTypeIn.equals(LinkType.DOWNLINK)) {
                // It is downlink
                // Emitter is the satellite, station is the receiver (since elevation is wrt to station),
                // so compute the reception date
                relevantDate = getSignalReceptionDate(this.station, pvProv, date, getThreshold());
            } else {
                // It is uplink
                // Emitter is the station, satellite is the receiver, so compute the emission date
                relevantDate = getSignalEmissionDate(this.station, pvProv, date, getThreshold());
            }
        }

        // not corrected vector from the station to the spacecraft in the topocentric frame
        final Transform t = inertialFrame.getTransformTo(this.station.getTopoFrame(), relevantDate);
        final Vector3D notCorrectedVector = t.transformPosition(pvProv.getPVCoordinates(date, inertialFrame)
            .getPosition());
        Vector3D vector = notCorrectedVector;

        if (this.correction != null) {
            // true elevation of the spacecraft
            final double trueElevation = this.station.getTopoFrame().getElevation(
                pvProv.getPVCoordinates(date, inertialFrame).getPosition(), inertialFrame, relevantDate);

            // computation of the correction to be applied
            final double elevationCorrection = this.correction.computeElevationCorrection(trueElevation);
            // corrected topocentric coordinates
            final double apparentElevation = elevationCorrection + trueElevation;
            final double azim = this.station.getTopoFrame().getAzimuth(notCorrectedVector,
                this.station.getTopoFrame(), relevantDate);
            final TopocentricPosition topoCoord = new TopocentricPosition(apparentElevation, azim,
                1.0);

            // computation of the vector
            vector = this.station.getTopoFrame().transformFromTopocentricToPosition(topoCoord);
        }

        return vector;
    }

    /** {@inheritDoc} */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>correction: {@link AngularCorrection}</li>
     * <li>station: {@link GeometricStationAntenna}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final VisibilityFromStationDetector detector;
        if (this.getSlopeSelection() == RAISING) {
            detector = new VisibilityFromStationDetector(this.getStation(), this.getCorrection(),
                this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(),
                this.isRemoveAtEntry(), this.linkType);
        } else if (this.getSlopeSelection() == SETTING) {
            detector = new VisibilityFromStationDetector(this.getStation(), this.getCorrection(),
                this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtExit(),
                this.isRemoveAtExit(), this.linkType);
        } else {
            detector = new VisibilityFromStationDetector(this.getStation(), this.getCorrection(),
                this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
                this.isRemoveAtEntry(), this.isRemoveAtExit(), this.linkType);
        }
        detector.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return detector;
    }

    /**
     * Getter for the station geodetic point.
     * 
     * @return the station geodetic point.
     */
    public GeodeticPoint getStationGeodeticPoint() {
        return this.getStation().getTopoFrame().getPoint();
    }

    /**
     * Getter for the Earth shape.
     * 
     * @return the Earth shape.
     */
    public BodyShape getBodyShape() {
        return this.getStation().getTopoFrame().getParentShape();
    }

    /**
     * Returns the type of link (it can be uplink or downlink).
     * 
     * @return the type of link (it can be uplink or downlink)
     */
    public LinkType getLinkType() {
        return linkType;
    }

    /**
     * Get the station.
     * 
     * @return the station antenna geometric model
     */
    public GeometricStationAntenna getStation() {
        return this.station;
    }

    /**
     * Get the correction.
     * 
     * @return the correction
     */
    public AngularCorrection getCorrection() {
        return this.correction;
    }
}
