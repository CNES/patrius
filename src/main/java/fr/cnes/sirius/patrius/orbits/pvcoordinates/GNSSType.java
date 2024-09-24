/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:426:30/10/2015: Add asbtract method convertType implemented in each item of the enum
 * VERSION::DM:1798:10/12/2018: Changes after AlternateEquinoctialParameters creation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;


/**
 * Enumerate for GNSS satellites type
 */
public enum GNSSType {

    /** GPS */
    GPS(Constants.GRS80_EARTH_MU, GNSSType.GPS_GALILEO_EARTH_ROTATION_RATE, AbsoluteDate.GPS_EPOCH),
    /** Galileo */
    Galileo(Constants.WGS84_EARTH_MU, GNSSType.GPS_GALILEO_EARTH_ROTATION_RATE, AbsoluteDate.GALILEO_EPOCH),
    /** Beidou */
    BeiDou(Constants.WGS84_EARTH_MU, Constants.GRS80_EARTH_ANGULAR_VELOCITY, AbsoluteDate.BEIDOU_EPOCH);

    /** Earth rotation rate value used by GPS and Galileo systems */
    private static final double GPS_GALILEO_EARTH_ROTATION_RATE = 7.2921151467e-5;
    /** Standard gravitational parameter (m^3/s^2) */
    private final double mu;
    /** Earth rotation rate (rad/s) */
    private final double earthRotationRate;
    /** Epoch date (launching date of the navigation system) */
    private final AbsoluteDate epochDate;

    /**
     * @param mu
     *        the value of the standard gravitational parameter (m^3/s^2) used by the gnss system
     * @param earthRotationRate
     *        the value of the earth rotation rate (rad/s) used by the gnss system
     * @param epochDate
     *        the epoch date of the gnss system
     */
    private GNSSType(final double mu, final double earthRotationRate, final AbsoluteDate epochDate) {
        this.mu = mu;
        this.earthRotationRate = earthRotationRate;
        this.epochDate = epochDate;
    }

    /**
     * @return the standard gravitational parameter (m^3/s^2) (m^3/s^2)
     */
    public double getMu() {
        return mu;
    }

    /**
     * @return the earthRotationRate (rad/s)
     */
    public double getEarthRotationRate() {
        return earthRotationRate;
    }

    /**
     * @return the epoch date
     */
    public AbsoluteDate getEpochDate() {
        return epochDate;
    }
}
