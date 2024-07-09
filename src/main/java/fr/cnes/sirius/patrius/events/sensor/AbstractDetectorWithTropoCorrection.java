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
 * @history creation 07/06/2012
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the signal correction classes
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPosition;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

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
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    protected AbstractDetectorWithTropoCorrection(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold) {
        super(maxCheck, threshold);
        this.correction = correctionModel;
        this.station = stationModel;
    }

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
     * @param slopeSelection slope selection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    protected AbstractDetectorWithTropoCorrection(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final int slopeSelection,
        final double maxCheck, final double threshold) {
        super(slopeSelection, maxCheck, threshold);
        this.correction = correctionModel;
        this.station = stationModel;
    }

    /**
     * Compute the apparent vector from the station to the spacecraft with tropospheric effects.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return the apparent vector from the station to the spacecraft with tropospheric effects
     * @exception PatriusException if some specific error occurs
     */
    protected Vector3D getCorrectedVector(final SpacecraftState s) throws PatriusException {
        // Emitter is the satellite, station is the receiver (since elevation is wrt to station)
        final AbsoluteDate recDate = getSignalReceptionDate(this.station, s, getThreshold(), getPropagationDelayType());

        // not corrected vector from the station to the spacecraft in the topocentric frame
        final Transform t = s.getFrame().getTransformTo(this.station.getTopoFrame(), recDate);
        final Vector3D notCorrectedVector = t.transformPosition(s.getPVCoordinates().getPosition());
        Vector3D vector = notCorrectedVector;

        if (this.correction != null) {
            // true elevation of the spacecraft
            final double trueElevation = this.station.getTopoFrame().getElevation(
                s.getPVCoordinates().getPosition(), s.getFrame(), recDate);

            // computation of the correction to be applied
            final double elevationCorrection = this.correction.computeElevationCorrection(trueElevation);
            // corrected topocentric coordinates
            final double apparentElevation = elevationCorrection + trueElevation;
            final double azim = this.station.getTopoFrame().getAzimuth(notCorrectedVector,
                this.station.getTopoFrame(), recDate);
            final TopocentricPosition topoCoord = new TopocentricPosition(apparentElevation, azim,
                1.0);

            // computation of the vector
            vector = this.station.getTopoFrame().transformFromTopocentricToPosition(topoCoord);
        }

        return vector;
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
