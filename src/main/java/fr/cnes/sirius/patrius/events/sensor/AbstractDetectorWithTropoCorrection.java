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

import fr.cnes.sirius.patrius.events.sensor.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if detector should be removed when entering the visibility zone
     * @param removeExit true if detector should be removed when exiting the visibility zone
     */
    protected AbstractDetectorWithTropoCorrection(final GeometricStationAntenna stationModel,
                                                  final AngularCorrection correctionModel, final double maxCheck,
                                                  final double threshold, final Action entry, final Action exit,
                                                  final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.correction = correctionModel;
        this.station = stationModel;
    }

    /**
     * Compute the apparent vector from the station to the spacecraft with tropospheric effects.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @param linkTypeIn the type of link (it can be uplink or downlink)
     * @return the apparent vector from the station to the spacecraft with tropospheric effects
     * @exception PatriusException if some specific error occurs
     */
    protected Vector3D getCorrectedVector(final SpacecraftState s, final LinkType linkTypeIn) throws PatriusException {

        // Check that the type of link is not null
        if (linkTypeIn == null) {
            // The type of link is null
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_LINK_TYPE);
        }
        
        // Define relevant date
        AbsoluteDate date = null;

        // Check the type of link
        if (linkTypeIn.equals(LinkType.DOWNLINK)) {
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
            final double trueElevation = this.station.getTopoFrame().getElevation(
                s.getPVCoordinates().getPosition(), s.getFrame(), date);

            // computation of the correction to be applied
            final double elevationCorrection = this.correction.computeElevationCorrection(trueElevation);
            // corrected topocentric coordinates
            final double apparentElevation = elevationCorrection + trueElevation;
            final double azim = this.station.getTopoFrame().getAzimuth(notCorrectedVector,
                this.station.getTopoFrame(), date);
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
