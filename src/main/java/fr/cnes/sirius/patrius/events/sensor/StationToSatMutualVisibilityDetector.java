/**
 * 
 * Copyright 2011-2017 CNES
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
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the signal correction classes
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
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
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType)} (default is signal being instantaneous).
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

    /** serial ID */
    private static final long serialVersionUID = -3026633624287530222L;

    /** The spacecraft sensor */
    private final SensorModel inSensorSpacecraft;

    /** The assembly to consider */
    private final Assembly inSpacecraft;

    /** True if maskings must be computed */
    private boolean maskingCheck;

    /** Action performed when entering the visibility zone. */
    private final Action actionAtEntry;

    /** Action performed when exiting the visibility zone. */
    private final Action actionAtExit;

    /** True if detector should be removed when entering the visibility zone. */
    private final boolean removeAtEntry;

    /** True if detector should be removed when exiting the visibility zone. */
    private final boolean removeAtExit;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

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
        super(station, correctionModel, maxCheck, threshold);
        this.inSensorSpacecraft = sensorModel1;
        this.inSpacecraft = this.inSensorSpacecraft.getAssembly();
        this.maskingCheck = withMasking;

        this.actionAtEntry = entry;
        this.actionAtExit = exit;
        this.removeAtEntry = removeEntry;
        this.removeAtExit = removeExit;
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
        super(new GeometricStationAntenna(topoFrame, azimElevMask), correctionModel, maxCheck,
            threshold);
        this.inSensorSpacecraft = sensorModel1;
        this.inSpacecraft = this.inSensorSpacecraft.getAssembly();

        this.actionAtEntry = entry;
        this.actionAtExit = exit;
        this.removeAtEntry = removeEntry;
        this.removeAtExit = removeExit;
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
            this.shouldBeRemovedFlag = this.removeAtEntry;
            return this.actionAtEntry;
        } else {
            this.shouldBeRemovedFlag = this.removeAtExit;
            return this.actionAtExit;
        }
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

        final AbsoluteDate targetDate = getSignalEmissionDate(inSensorSpacecraft.getMainTarget(), s, getThreshold(),
                getPropagationDelayType());

        // angular distance of the station in the spacecraft sensor field of view
        final double angularRadiusInSpacecraftSensor = this.inSensorSpacecraft.getTargetCenterFOVAngle(targetDate);

        // angular distance of the spacecraft sensor in the station field of view
        final Vector3D correctedVector = this.getCorrectedVector(s);
        final double angularDistanceInStationSensor = this.getStation().getFOV().getAngularDistance(
            correctedVector);

        // both must be positive for mutual visibility to be true
        final double minVisi = MathLib.min(angularRadiusInSpacecraftSensor,
            angularDistanceInStationSensor);

        final double minMaskingDist;
        if (this.maskingCheck) {
            // body shapes masking check
            final double bodyMaskingDistance = this.inSensorSpacecraft.celestialBodiesMaskingDistance(targetDate);

            // spacecrafts maskings
            final double spacecraftsMaskingDistance = this.inSensorSpacecraft
                .spacecraftsMaskingDistance(targetDate);
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
                this.getMaxCheckInterval(), this.getThreshold(), this.actionAtEntry, this.actionAtExit,
                this.removeAtEntry, this.removeAtExit);
        res.setPropagationDelayType(getPropagationDelayType());
        return res;
    }
}