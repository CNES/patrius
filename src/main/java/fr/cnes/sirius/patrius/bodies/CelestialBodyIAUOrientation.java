/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w la classe UserIAUPole
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for IAU pole and primer meridian orientations.
 * <p>
 * This interface defines methods compliant with the report of the IAU/IAG Working Group on Cartographic Coordinates and
 * Rotational Elements of the Planets and Satellites (WGCCRE). These definitions are common for all recent versions of
 * this report published every three years.
 * </p>
 * <p>
 * The precise values of pole direction and W angle coefficients may vary from publication year as models are adjusted.
 * The latest value of constants for implementing this interface can be found in the <a
 * href="http://astrogeology.usgs.gov/Projects/WGCCRE/">working group site</a>.
 * </p>
 * 
 * @see CelestialBodyFactory
 * @author Luc Maisonobe
 */
public interface CelestialBodyIAUOrientation extends CelestialBodyOrientation {

    /**
     * Getter for the body North pole direction with respect to a reference frame.
     * 
     * @param date
     *        Current date
     * @param iauPoleType
     *        IAUPole data to take into account for ICRF/inertial/mean equator/true equator transformation
     * @return the body North pole direction with respect to a reference frame
     */
    Vector3D getPole(final AbsoluteDate date, final IAUPoleModelType iauPoleType);

    /**
     * Getter for the body North pole direction derivative with respect to a reference frame.<br>
     * It takes into account constant, secular and harmonics terms (conversion from ICRF to true equator).
     * 
     * @param date
     *        Current date
     * @param iauPoleType
     *        IAUPole data to take into account for ICRF/inertial/mean equator/true equator transformation
     * @return the body North pole direction derivative with respect to a reference frame
     */
    Vector3D getPoleDerivative(final AbsoluteDate date, final IAUPoleModelType iauPoleType);

    /**
     * Getter for the prime meridian angle.
     * <p>
     * The prime meridian angle is the angle between the Q node and the prime meridian. Represents the body rotation.
     * </p>
     * 
     * @param date
     *        Current date
     * @param iauPoleType
     *        IAUPole data to take into account for ICRF/inertial/mean rotating/true rotating transformation
     * @return the prime meridian vector
     */
    double getPrimeMeridianAngle(final AbsoluteDate date, final IAUPoleModelType iauPoleType);

    /**
     * Getter for the prime meridian angle derivative.
     * <p>
     * The prime meridian angle is the angle between the Q node and the prime meridian. Represents the body rotation.
     * </p>
     * 
     * @param date
     *        Current date
     * @param iauPoleType
     *        IAUPole data to take into account for ICRF/inertial/mean rotating/true rotating transformation
     * @return the prime meridian angle derivative
     */
    double getPrimeMeridianAngleDerivative(final AbsoluteDate date, final IAUPoleModelType iauPoleType);

    /**
     * Getter for the orientation from the ICRF frame to the rotating frame.
     * 
     * @param date
     *        Current date
     * @param iauPoleType
     *        IAUPole data to take into account for ICRF/inertial/mean rotating/true rotating transformation
     * @return the orientation from the ICRF frame to the rotating frame
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    default AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final IAUPoleModelType iauPoleType)
        throws PatriusException {
        return getAngularCoordinates(date, OrientationType.ICRF_TO_ROTATING, iauPoleType);
    }

    /**
     * Getter for the orientation.
     * 
     * @param date
     *        Current date
     * @param orientationType
     *        Indicates the expected orientation type
     * @param iauPoleType
     *        IAUPole data to take into account for ICRF/inertial/mean rotating/true rotating transformation
     * @return the orientation
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final OrientationType orientationType,
                                             final IAUPoleModelType iauPoleType) throws PatriusException;
}
