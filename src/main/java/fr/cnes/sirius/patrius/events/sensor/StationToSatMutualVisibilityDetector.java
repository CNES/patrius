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
 * @history creation 25/05/2012
 *
 * HISTORY
 * VERSION:4.10.2:FA:FA-3289:31/01/2023:[PATRIUS] Problemes sur le masquage d une visi avec LIGHT_TIME
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3238:03/11/2022:[PATRIUS] Masquages par des corps celestes dans VisibilityFromStationDetector
 * VERSION:4.10:DM:DM-3245:03/11/2022:[PATRIUS] Ajout du sens de propagation du signal dans ...
 * VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the signal correction classes
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.events.sensor.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Mutual station to spacecraft visibility detector : the g function is positive only if the station's sensor sees the
 * spacecraft's sensor AND the spacecraft's sensor sees the station's sensor.
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when entering
 * the eclipse and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when
 * exiting the eclipse. This can be changed by using one of the provided contructors.
 * </p>
 * <p>
 * This detector can take into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous). Default linktype is {@link LinkType#DOWNLINK}.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class not
 *                      thread-safe itself
 * 
 * @see Assembly
 * @see GeometricStationAntenna
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
public class StationToSatMutualVisibilityDetector extends AbstractDetectorWithTropoCorrection {

    /** Serializable UID. */
    private static final long serialVersionUID = -3026633624287530222L;

    /** The spacecraft sensor */
    private final SensorModel inSensorSpacecraft;

    /** The assembly to consider */
    private final Assembly inSpacecraft;

    /** True if maskings must be computed */
    private boolean maskingCheck;
    
    /** Type of link (it can be uplink or downlink). Downlink means satellite to station. */
    private final LinkType linkType;

    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the eclipse and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when exiting the eclipse.
     * </p>
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param station the ground station sensor geometric model.
     * @param correctionModel the elevation correction model for the station
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        spacecraft and the station.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final GeometricStationAntenna station, final AngularCorrection correctionModel,
        final boolean withMasking, final double maxCheck, final double threshold) {
        this(sensorModel1, station, correctionModel, withMasking, maxCheck, threshold,
            Action.CONTINUE, Action.STOP);

    }

    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param station the ground station sensor geometric model.
     * @param correctionModel the elevation correction model for the station
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        spacecraft and the station.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final GeometricStationAntenna station, final AngularCorrection correctionModel,
        final boolean withMasking, final double maxCheck, final double threshold,
        final Action entry, final Action exit) {
        this(sensorModel1, station, correctionModel, withMasking, maxCheck, threshold, entry, exit,
            false, false);
    }

    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param station the ground station sensor geometric model.
     * @param correctionModel the elevation correction model for the station
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        spacecraft and the station.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if detector should be removed when entering the visibility zone
     * @param removeExit true if detector should be removed when exiting the visibility zone
     * @since 3.1
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final GeometricStationAntenna station, final AngularCorrection correctionModel,
        final boolean withMasking, final double maxCheck, final double threshold,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit) {
        this(sensorModel1, station, correctionModel, withMasking, maxCheck, threshold, entry, exit, removeEntry,
                removeExit, LinkType.DOWNLINK);
    }
    
    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param station the ground station sensor geometric model.
     * @param correctionModel the elevation correction model for the station
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        spacecraft and the station.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if detector should be removed when entering the visibility zone
     * @param removeExit true if detector should be removed when exiting the visibility zone
     * @param linkTypeIn the type of link (it can be uplink or downlink). Downlink means satellite to station.
     * @since 4.10
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final GeometricStationAntenna station, final AngularCorrection correctionModel,
        final boolean withMasking, final double maxCheck, final double threshold,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit, final LinkType linkTypeIn) {
        super(station, correctionModel, maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.inSensorSpacecraft = sensorModel1;
        this.inSpacecraft = this.inSensorSpacecraft.getAssembly();
        this.maskingCheck = withMasking;
        this.linkType = linkTypeIn;
    }

    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the eclipse and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when exiting the eclipse.
     * </p>
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final TopocentricFrame topoFrame, final double[][] azimElevMask,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold) {
        this(sensorModel1, topoFrame, azimElevMask, correctionModel, maxCheck, threshold,
            Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final TopocentricFrame topoFrame, final double[][] azimElevMask,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final Action entry, final Action exit) {
        this(sensorModel1, topoFrame, azimElevMask, correctionModel, maxCheck, threshold, entry,
            exit, false, false);
    }

    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if detector should be removed when entering the visibility zone
     * @param removeExit true if detector should be removed when exiting the visibility zone
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final TopocentricFrame topoFrame, final double[][] azimElevMask,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit) {
        this(sensorModel1, topoFrame, azimElevMask, correctionModel, maxCheck, threshold, entry,
            exit, removeEntry, removeExit, LinkType.DOWNLINK);
    }
    
    /**
     * Constructor for the mutual station to spacecraft visibility detector
     * 
     * @param sensorModel1 the sensor model the main part frame of the assembly must have a parent
     *        frame ! Make sure that the main target of this sensor model is the following station !
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if detector should be removed when entering the visibility zone
     * @param removeExit true if detector should be removed when exiting the visibility zone
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    public StationToSatMutualVisibilityDetector(final SensorModel sensorModel1,
        final TopocentricFrame topoFrame, final double[][] azimElevMask,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit, final LinkType linkTypeIn) {
        super(new GeometricStationAntenna(topoFrame, azimElevMask), correctionModel, maxCheck,
                threshold, entry, exit, removeEntry, removeExit);
        this.inSensorSpacecraft = sensorModel1;
        this.inSpacecraft = this.inSensorSpacecraft.getAssembly();
        this.linkType = linkTypeIn;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            return this.getActionAtEntry();
        }
        this.shouldBeRemovedFlag = this.isRemoveAtExit();
        return this.getActionAtExit();
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        this.inSpacecraft.updateMainPartFrame(s);
        
        // Define station date: date at which the station emits (uplink) or receives (downlink) the signal
        AbsoluteDate stationDate = null;
        // Check the type of link
        if (linkType.equals(LinkType.DOWNLINK)) {
            // It is downlink
            // Emitter is the satellite, station is the receiver, so compute the reception date
            stationDate = getSignalReceptionDate(this.inSensorSpacecraft.getMainTarget(), s.getOrbit(), s.getDate());
        } else {
            // It is uplink
            // Emitter is the station, satellite is the receiver, so compute the emission date
            stationDate = getSignalEmissionDate(this.inSensorSpacecraft.getMainTarget(), s.getOrbit(), s.getDate());
        }

        // angular distance of the station in the spacecraft sensor field of view
        final double angularRadiusInSpacecraftSensor = this.inSensorSpacecraft.getTargetCenterFOVAngle(stationDate);

        // angular distance of the spacecraft sensor in the station field of view
        final Vector3D correctedVector = this.getCorrectedVector(s, this.linkType);
        final double angularDistanceInStationSensor = this.getStation().getFOV().getAngularDistance(correctedVector);

        // both must be positive for mutual visibility to be true
        final double minVisi = MathLib.min(angularRadiusInSpacecraftSensor, angularDistanceInStationSensor);

        final double minMaskingDist;
        if (this.maskingCheck) {
            // body shapes masking check
            final double bodyMaskingDistance = this.inSensorSpacecraft.celestialBodiesMaskingDistance(s.getDate(), 
                    stationDate, getPropagationDelayType(), linkType);

            // spacecrafts maskings
            final double spacecraftsMaskingDistance = this.inSensorSpacecraft.spacecraftsMaskingDistance(s.getDate(),
                    stationDate, getPropagationDelayType(), linkType);
            minMaskingDist = MathLib.min(bodyMaskingDistance, spacecraftsMaskingDistance);
        } else {
            minMaskingDist = Double.POSITIVE_INFINITY;
        }

        // both must be positive for mutual visibility to be true
        return MathLib.min(minVisi, minMaskingDist);
    }

    /**
     * Get the sensor.
     * 
     * @return the sensor
     */
    public SensorModel getSensor() {
        return this.inSensorSpacecraft;
    }

    /**
     * Get the assembly.
     * 
     * @return the assembly
     */
    public Assembly getAssembly() {
        return this.inSpacecraft;
    }
    
    /**
     * Returns the type of link (it can be uplink or downlink).
     * 
     * @return the type of link (it can be uplink or downlink)
     */
    public LinkType getLinkType() {
        return this.linkType;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inSensorSpacecraft: {@link SensorModel}</li>
     * <li>correction: {@link AngularCorrection}</li>
     * <li>station: {@link GeometricStationAntenna}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final StationToSatMutualVisibilityDetector res = new StationToSatMutualVisibilityDetector(
                this.inSensorSpacecraft, this.getStation(), this.getCorrection(), this.maskingCheck,
            this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
            this.isRemoveAtEntry(), this.isRemoveAtExit(), this.getLinkType());
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
