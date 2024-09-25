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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.11:FA:FA-3312:22/05/2023:[PATRIUS] Repere trueInertialFrame pas vraiment pseudo-inertiel
 * VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel lorsque SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for celestial points like Sun, Moon or solar system planets (without modeling the shape, orientation, etc).
 * 
 * @author Luc Maisonobe
 * @see CelestialBodyFactory
 * @see CelestialBodyIAUOrientation
 */
public interface CelestialPoint extends PVCoordinatesProvider {

    /** ICRF frame name. */
    public static final String ICRF_FRAME_NAME = "ICRF frame";

    /** EME2000 frame name. */
    public static final String EME2000_FRAME_NAME = "EME2000 frame";

    /** EclipticJ2000 frame name. */
    public static final String ECLIPTICJ2000_FRAME_NAME = "EclipticJ2000 frame";

    /**
     * Get an ICRF, celestial point centered frame.
     * <p>
     * The frame is always bound to the celestial point, and its axes have a fixed orientation with respect to other
     * inertial frames.
     * </p>
     * 
     * @return an inertially oriented, celestial point centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getRotatingFrame(IAUPoleModelType.TRUE)
     */
    CelestialBodyFrame getICRF() throws PatriusException;

    /**
     * Get an EME2000, celestial point centered frame.
     * <p>
     * The frame is always bound to the celestial point center, and its axes are colinear to Earth EME2000 frame.
     * </p>
     * 
     * @return an EME2000, celestial point centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getEME2000() throws PatriusException;

    /**
     * Get an Ecliptic J200, celestial point centered frame.
     * <p>
     * The frame is always bound to the celestial point center with constant rotation relative to the ICRF.
     * </p>
     * 
     * @return an EclipticJ200, celestial point centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getEclipticJ2000() throws PatriusException;

    /**
     * Get the name of the celestial point.
     * 
     * @return name of the celestial point
     */
    String getName();

    /**
     * Get the central attraction coefficient of the celestial point.
     * <p>
     * Warning: attraction model should not be null (it is not null by default).
     * </p>
     * 
     * @return central attraction coefficient of the celestial point (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getGM();

    /**
     * Set a central attraction coefficient to the celestial point.
     * 
     * @param gmIn
     *        the central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    void setGM(double gmIn);

    /**
     * Get the ephemeris of the celestial point.
     * 
     * @return the ephemeris
     */
    CelestialBodyEphemeris getEphemeris();

    /**
     * Set an ephemeris to the celestial point.
     * 
     * @param ephemerisIn
     *        the ephemeris
     */
    void setEphemeris(CelestialBodyEphemeris ephemerisIn);

    /**
     * Returns a string representation of the celestial point and its attributes.
     * 
     * @return a string representation of the celestial point and its attributes
     */
    @Override
    String toString();

    /**
     * Getter for the celestial point nature.
     * 
     * @return the celestial point nature
     */
    default BodyNature getBodyNature() {
        return BodyNature.POINT;
    }

    /** Body nature. */
    public enum BodyNature {

        /** Physical body. */
        PHYSICAL_BODY,

        /** Point. */
        POINT;
    }
}
