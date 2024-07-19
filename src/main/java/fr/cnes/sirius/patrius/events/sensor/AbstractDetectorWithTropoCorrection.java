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
 * @history creation 07/06/2012
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:FA:FA-3283:22/05/2023:[PATRIUS] Mutualisation detecteurs
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the signal correction classes
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.events.sensor.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPosition;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Abstract event detector using a station elevation correction.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of frames makes this class not thread-safe.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public abstract class AbstractDetectorWithTropoCorrection extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -1504378004238040001L;

    /** The correction model to apply to the station elevation. */
    private final AngularCorrection correction;

    /** Topocentric frame in which elevation should be evaluated. */
    private final GeometricStationAntenna station;

    /** Type of link (it can be uplink or downlink). */
    private final LinkType linkType;

    /** The spacecraft sensor */
    private final SensorModel inSensorSpacecraft;

    /** The assembly to consider */
    private final Assembly inSpacecraft;

    /** True if maskings must be computed */
    private final boolean maskingCheck;

    /**
     * Constructor for the abstract event detector using a station elevation correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the geometric model of the ground station antenna
     * @param correctionModel the angular correction model (set null to ignore the station elevation
     *        correction)
     * @param sensorModel sensor model of the spacecraft used for masking computations (not used otherwise,
     *        can be set to null). The sensor model of the main part frame of the assembly must have a parent
     *        frame! Make sure that the main target of this sensor model is the above station!
     * @param withMasking set true to compute maskings of spacecraft's sensor by occulting bodies: in this case, there
     *        is no visibility if one of the masking bodies of the sensor is between the spacecraft and the station
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if detector should be removed when entering the visibility zone
     * @param removeExit true if detector should be removed when exiting the visibility zone
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     */
    protected AbstractDetectorWithTropoCorrection(final GeometricStationAntenna stationModel,
            final AngularCorrection correctionModel,
            final SensorModel sensorModel,
            final boolean withMasking,
            final double maxCheck,
            final double threshold,
            final Action entry,
            final Action exit,
            final boolean removeEntry,
            final boolean removeExit,
            final LinkType linkTypeIn) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.correction = correctionModel;
        this.station = stationModel;
        this.linkType = linkTypeIn;

        // Set masking-related attributes: the spacecraft assembly and its sensor
        this.inSensorSpacecraft = sensorModel;
        this.inSpacecraft = this.inSensorSpacecraft == null ? null : this.inSensorSpacecraft.getAssembly();

        // Set the masking check flag: true if asked for and if the sensor's spacecraft is not null
        this.maskingCheck = withMasking && (this.inSpacecraft != null);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Create station date
        AbsoluteDate stationDate = null;
        if (this instanceof StationToSatMutualVisibilityDetector
                || (this instanceof VisibilityFromStationDetector && isMaskingCheck())) {
            // Update frame (spacecraft cannot be null if maskingCheck is true)
            getAssembly().updateMainPartFrame(state);

            // Define station date: date at which the station emits (uplink) or receives (downlink) the signal
            // This date is used to compute maskings (if asked)
            stationDate = getStationDate(state);
        }

        // Define minimal masking distance
        final double minMaskingDistance;
        if (isMaskingCheck()) {
            // Positive if no masking
            minMaskingDistance = getMinMaskingDistance(state, getSensor(), stationDate);
        } else {
            // No masking computation
            minMaskingDistance = Double.POSITIVE_INFINITY;
        }

        // Position of the spacecraft in topocentric frame: apparent vector with tropospheric effects
        // Propagation delay is taken into account during corrected vector computation
        final Vector3D correctedVector = this.getCorrectedVector(state);

        // Angular distance of the spacecraft sensor in the station field of view: positive if the vector is within the
        // station field of view
        final double angularDistanceInStationSensor = this.getStation().getFOV().getAngularDistance(correctedVector);

        final double angleVisi;
        if (this instanceof StationToSatMutualVisibilityDetector) {
            // Case of StationToSatMutualVisibilityDetector
            // Angular distance of the station in the spacecraft sensor field of view
            final double angularRadiusInSpacecraftSensor = getSensor().getTargetCenterFOVAngle(stationDate);

            // Both must be positive for mutual visibility to be true
            angleVisi = MathLib.min(angularRadiusInSpacecraftSensor, angularDistanceInStationSensor);
        } else {
            // Case of VisibilityFromStationDetector
            angleVisi = angularDistanceInStationSensor;
        }

        // Both must be positive for mutual visibility to be true
        return MathLib.min(angleVisi, minMaskingDistance);
    }

    /**
     * Compute the apparent vector from the station to the spacecraft with tropospheric effects.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return the apparent vector from the station to the spacecraft with tropospheric effects
     * @exception PatriusException if some specific error occurs
     */
    protected Vector3D getCorrectedVector(final SpacecraftState s) throws PatriusException {

        // Check that the type of link is not null
        if (linkType == null) {
            // The type of link is null
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_LINK_TYPE);
        }

        // Define relevant date
        AbsoluteDate date = null;

        // Check the type of link
        if (linkType.equals(LinkType.DOWNLINK)) {
            // It is downlink
            // Emitter is the satellite, station is the receiver (since elevation is wrt to station),
            // so compute the reception date
            date = getSignalReceptionDate(this.station, s.getOrbit(), s.getDate());
        } else {
            // It is uplink
            // Emitter is the station, satellite is the receiver, so compute the emission date
            date = getSignalEmissionDate(this.station, s.getOrbit(), s.getDate());
        }

        // not corrected vector from the station to the spacecraft in the topocentric frame
        final Transform t = s.getFrame().getTransformTo(this.station.getTopoFrame(), date);
        final Vector3D notCorrectedVector = t.transformPosition(s.getPVCoordinates().getPosition());
        Vector3D vector = notCorrectedVector;

        if (this.correction != null) {
            // true elevation of the spacecraft
            final double trueElevation = this.station.getTopoFrame().getElevation(s.getPVCoordinates().getPosition(),
                    s.getFrame(), date);

            // computation of the correction to be applied
            final double elevationCorrection = this.correction.computeElevationCorrection(date, trueElevation);
            // corrected topocentric coordinates
            final double apparentElevation = elevationCorrection + trueElevation;
            final double azim = this.station.getTopoFrame().getAzimuth(notCorrectedVector, this.station.getTopoFrame(),
                    date);
            final TopocentricPosition topoCoord = new TopocentricPosition(apparentElevation, azim, 1.0);

            // computation of the vector
            vector = this.station.getTopoFrame().transformFromTopocentricToPosition(topoCoord);
        }

        return vector;
    }

    /**
     * Compute station date taking into account {@link PropagationDelayType} and {@link LinkType}.
     * @param s spacecraft state
     * @return station date
     * @throws PatriusException thrown if computation failed
     */
    protected AbsoluteDate getStationDate(final SpacecraftState s) throws PatriusException {
        // Define station date: date at which the station emits (uplink) or receives (downlink) the signal
        AbsoluteDate stationDate = null;
        // Check the type of link
        if (linkType.equals(LinkType.DOWNLINK)) {
            // It is downlink
            // Emitter is the satellite, station is the receiver, so compute the reception date
            stationDate = getSignalReceptionDate(getStation(), s.getOrbit(), s.getDate());
        } else {
            // It is uplink
            // Emitter is the station, satellite is the receiver, so compute the emission date
            stationDate = getSignalEmissionDate(getStation(), s.getOrbit(), s.getDate());
        }
        return stationDate;
    }

    /**
     * Computes minimum masking distance by either body or spacecraft defined in sensor model.
     * @param s spacecraft state
     * @param model spacecraft sensor model
     * @param stationDate station date
     * @return minimum masking distance by either body or spacecraft defined in sensor model
     * @throws PatriusException thrown if computation failed
     */
    protected double getMinMaskingDistance(final SpacecraftState s,
            final SensorModel model,
            final AbsoluteDate stationDate) throws PatriusException {
        // Check potential body masking (occulting body between station and spacecraft)
        final double bodyMaskingDistance = model.celestialBodiesMaskingDistance(s.getDate(), stationDate,
                getPropagationDelayType(), linkType);

        // Check potential masking by another spacecraft
        final double spacecraftsMaskingDistance = model.spacecraftsMaskingDistance(s.getDate(), stationDate,
                getPropagationDelayType(), linkType);

        // Positive if no masking
        return MathLib.min(bodyMaskingDistance, spacecraftsMaskingDistance);
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

    /**
     * Returns the type of link (it can be uplink or downlink).
     * 
     * @return the type of link (it can be uplink or downlink)
     */
    public LinkType getLinkType() {
        return this.linkType;
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
     * True if masking are taken into account.
     * 
     * @return true if masking are taken into account, false otherwise
     */
    public boolean isMaskingCheck() {
        return this.maskingCheck;
    }
}
