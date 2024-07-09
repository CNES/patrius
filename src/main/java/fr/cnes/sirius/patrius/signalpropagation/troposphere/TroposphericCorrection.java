/**
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
 */
/* Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: refactoring and renaming of the interface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Defines a tropospheric model, used to calculate the signal delay for the signal path
 * imposed to electro-magnetic signals between an orbital satellite and a ground station.
 * 
 * @author Thomas Neidhart
 */
public interface TroposphericCorrection extends Serializable {

    /** Standard reference temperature. */
    static final double T0 = 18;

    /** Standard reference pressure. */
    static final double P0 = 101325;

    /** Standard reference humidity rate. */
    static final double RH0 = 0.5;

    /** Standard reference altitude. */
    static final double H0 = 0;

    /** Absolute zero for temperatures. */
    static final double ABSOLUTE_ZERO = 273.15;

    /**
     * Calculates the tropospheric signal delay for the signal path from a
     * ground station to a satellite.
     * 
     * @param elevation
     *        the elevation of the satellite [rad]
     * @return the signal delay due to the troposphere [s]
     */
    double computeSignalDelay(final double elevation);

    /**
     * Computes standard model values [T, P, RH] for provided altitude given reference values [T0, P0, RH0 H0] with:
     * <ul>
     * <li>T = temperature [K]</li>
     * <li>P = pressure [Pa]</li>
     * <li>RH = humidity rate [%] in [0, 1]</li>
     * </ul>
     * 
     * @param t0 reference temperature [K]
     * @param p0 reference pressure [Pa]
     * @param rh0 reference humidity rate [%] in [0, 1]
     * @param h0 reference altitude [m]
     * @param altitude altitude for which values [T, P, RH] should be returned
     * @return [T, P, RH] values
     */
    // CHECKSTYLE: stop MagicNumber check
    static double[] computeStandardValues(final double t0, final double p0, final double rh0,
            final double h0,
            final double altitude) {
        // CHECKSTYLE: resume MagicNumber check
        // Values from OACI models
        final double t = t0 - 0.0065 * (altitude - h0);
        final double p = p0 * MathLib.pow(1 - 0.0000226 * (altitude - h0), 5.225);
        final double r = rh0 * MathLib.exp(-0.0006396 * (altitude - h0));
        return new double[] { t, p, r};
    }


    /**
     * Computes standard model values [T, P, R] for provided altitude with standard reference values [T0, P0, RH0] 
     * provided by tropospheric models :
     * <ul>
     * <li>T = temperature [K] - T0 = 18 degree Celsius</li>
     * <li>P = pressure [Pa] - P0 = 101325 Pa</li>
     * <li>RH = humidity rate [%] in [0, 1] - RH0 = 50%</li>
     * </ul>
     * 
     * @param altitude altitude for which values [T, P, RH] should be returned
     * @return standard model values [T, P, RH]
     */
    static double[] computeStandardValues(final double altitude) {
        return computeStandardValues(T0 + ABSOLUTE_ZERO, P0, RH0, H0, altitude);
    }
}
