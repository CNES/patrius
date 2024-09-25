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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface to represent a celestial body orientation.
 * 
 * @author Thibaut BONIT
 * 
 * @since 4.13
 */
public interface CelestialBodyOrientation extends Serializable {

    /**
     * Getter for the body North pole direction with respect to a reference frame.<br>
     * It takes into account constant, secular and harmonics terms (conversion from ICRF to true equator).
     * 
     * @param date
     *        Current date
     * @return the body North pole direction with respect to a reference frame
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    Vector3D getPole(final AbsoluteDate date) throws PatriusException;

    /**
     * Getter for the body North pole direction derivative with respect to a reference frame.<br>
     * It takes into account constant, secular and harmonics terms (conversion from ICRF to true equator).
     * 
     * @param date
     *        Current date
     * @return the body North pole direction derivative with respect to a reference frame
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    Vector3D getPoleDerivative(final AbsoluteDate date) throws PatriusException;

    /**
     * Getter for the prime meridian angle.<br>
     * It takes into account constant, secular and harmonics terms (conversion from ICRF to true rotating).
     * <p>
     * The prime meridian angle is the angle between the Q node and the prime meridian. Represents the body rotation.
     * </p>
     * 
     * @param date
     *        Current date
     * @return the prime meridian vector
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    double getPrimeMeridianAngle(final AbsoluteDate date) throws PatriusException;

    /**
     * Getter for the prime meridian angle derivative.<br>
     * It takes into account constant, secular and harmonics terms (conversion from ICRF to true rotating).
     * <p>
     * The prime meridian angle is the angle between the Q node and the prime meridian. Represents the body rotation.
     * </p>
     * 
     * @param date
     *        Current date
     * @return the prime meridian angle derivative
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    double getPrimeMeridianAngleDerivative(final AbsoluteDate date) throws PatriusException;

    /**
     * Getter for the orientation from the ICRF frame to the rotating frame.
     * 
     * @param date
     *        Current date
     * @return the orientation from the ICRF frame to the rotating frame
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    default AngularCoordinates getAngularCoordinates(final AbsoluteDate date) throws PatriusException {
        return getAngularCoordinates(date, OrientationType.ICRF_TO_ROTATING);
    }

    /**
     * Getter for the orientation.
     * 
     * @param date
     *        Current date
     * @param orientationType
     *        Indicates the expected orientation type
     * @return the orientation
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final OrientationType orientationType)
        throws PatriusException;

    /**
     * Returns a string representation of the body orientation.
     * 
     * @return a string representation of the body orientation
     */
    @Override
    String toString();

    /** Describe the orientation type. */
    enum OrientationType {
        /** Orientation from the ICRF frame to the rotating frame. */
        ICRF_TO_ROTATING,

        /** Orientation from the ICRF frame to the inertial frame. */
        ICRF_TO_INERTIAL,

        /** Orientation from the inertial frame to the rotating frame. */
        INERTIAL_TO_ROTATING;
    }
}
