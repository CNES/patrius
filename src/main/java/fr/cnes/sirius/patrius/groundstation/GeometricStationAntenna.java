/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.groundstation;

import fr.cnes.sirius.patrius.events.sensor.StationToSatMutualVisibilityDetector;
import fr.cnes.sirius.patrius.fieldsofview.AzimuthElevationField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class representing an a geometric model for a ground station antenna.<br>
 * It is used in reverse station visibility event detection.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment link to the tree of frames
 * 
 * @see StationToSatMutualVisibilityDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class GeometricStationAntenna implements PVCoordinatesProvider {

    /** Topocentric frame of the station */
    private final TopocentricFrame topo;

    /** Sensor field of view defined in the topocentric frame */
    private final IFieldOfView field;

    /**
     * Constructor of the ground station antenna (for reverse visibility detection)
     * 
     * @param topoFrame
     *        topocentric frame of the station
     * @param fieldOfView
     *        sensor field of view in defined in the topocentric frame
     */
    public GeometricStationAntenna(final TopocentricFrame topoFrame, final IFieldOfView fieldOfView) {
        this.topo = topoFrame;
        this.field = fieldOfView;
    }

    /**
     * Constructor of the ground station antenna (for reverse visibility detection)
     * 
     * @param topoFrame
     *        topocentric frame of the station
     * @param azimElevMask
     *        the azimuth - elevation mask (rad)
     */
    public GeometricStationAntenna(final TopocentricFrame topoFrame, final double[][] azimElevMask) {
        this.topo = topoFrame;
        this.field = new AzimuthElevationField(azimElevMask, this.topo.getOrientation(), "azimElevMask");
    }

    /**
     * @return the station topocentric frame
     */
    public TopocentricFrame getTopoFrame() {
        return this.topo;
    }

    /**
     * @return the field of view
     */
    public IFieldOfView getFOV() {
        return this.field;
    }

    /**
     * Get the {@link PVCoordinates} of the station antenna in the selected frame.
     * 
     * @param date
     *        current date
     * @param frame
     *        the frame where to define the position
     * @return position/velocity of the body (m and m/s)
     * @exception PatriusException
     *            if position cannot be computed in given frame
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // transformation to output frame
        final Transform t = this.topo.getTransformTo(frame, date);
        final PVCoordinates centerNullCoordinates = new PVCoordinates();

        // sensor center in output frame
        return t.transformPVCoordinates(centerNullCoordinates);
    }
}
