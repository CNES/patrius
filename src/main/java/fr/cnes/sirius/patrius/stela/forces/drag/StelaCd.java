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
 * @history created 24/07/2013
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013:creation giving the user the possibility to use variable or constant Cd
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.drag;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class represent a drag coefficient model.
 * </p>
 * 
 * @concurrency immutable
 * @author Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 2.0
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public final class StelaCd implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 6190288937267720207L;

    /** Conversion */
    private static final double KM_M = 1000;

    /** The spacecraft drag coefficient. */
    private final double cd;

    /** The spacecraft drag coefficient map. */
    private final Map<Double, Double> cdMap;

    /** True if the Cd coefficient is constant. */
    private final boolean isCdConstant;

    /** The geodetic model. */
    private final GeodPosition geod;

    /**
     * Simple constructor for a constant Cd.
     * 
     * @param inCd
     *        the constant value of the cd
     */
    public StelaCd(final double inCd) {
        this.isCdConstant = true;
        this.cd = inCd;
        this.cdMap = null;
        this.geod = null;
    }

    /**
     * Constructor for a Cd model depending on spacecraft altitude.
     * 
     * @param inCd
     *        the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param rEq
     *        the Earth radius
     * @param f
     *        the Earth flattening
     */
    public StelaCd(final Map<Double, Double> inCd, final double rEq, final double f) {
        this.isCdConstant = false;
        this.cd = 0.0;
        this.cdMap = inCd;
        this.geod = new GeodPosition(rEq, f);
    }

    /**
     * Compute the value of the Cd coefficient depending on spacecraft altitude.
     * 
     * @param position
     *        the spacecraft position in the inertial frame
     * @return the value of the drag coefficient
     * 
     * @throws PatriusException
     *         if error while computing geodetic altitude
     */
    public double getCd(final Vector3D position) throws PatriusException {
        double result = 0;

        if (this.isCdConstant) {
            result = this.cd;
        } else {
            // Ff Cd is variable
            // Get geodetic altitude from geodetic model and spacecraft position in the inertial frame
            final double altitude = this.geod.getGeodeticAltitude(position) / KM_M;
            final Iterator<Double> iterator = this.cdMap.keySet().iterator();
            double previous = iterator.next();
            // loop on spacecraft drag coefficient map
            while (iterator.hasNext()) {
                final double current = iterator.next();
                if (current >= altitude) {
                    return this.cdMap.get(previous);
                }
                previous = current;
            }
            result = this.cdMap.get(previous);
        }

        return result;
    }
}
