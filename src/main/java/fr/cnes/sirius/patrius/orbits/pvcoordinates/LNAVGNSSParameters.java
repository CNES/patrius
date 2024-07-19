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
 * @history Created on 09/11/2015
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:09/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

/**
 * <p>
 * This class is a simple container for a broadcast model LNAV ephemeris description parameters of
 * GNSS satellites (GPS, Galileo or BeiDou)
 * </p>
 *
 * @author fteilhard
 *
 *
 */
public class LNAVGNSSParameters extends GNSSParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -4623503618154005177L;

    /**
     * Creates an instance of GNSS Parameter for the LNAV broadcast model (for GPS, Galileo or
     * BeiDou)
     * @param gnssType
     *        the type of satellite (GPS, Galileo or BeiDou)
     * @param refTime
     *        time applicability of the model
     * @param m0
     *        Initial Mean anomaly
     * @param e
     *        Eccentricity
     * @param sqrtA
     *        Semi major axis square root
     * @param omega0
     *        Initial Right ascencion
     * @param i0
     *        Orbital inclination
     * @param w0
     *        Argument of perigee
     * @param rateRa
     *        Rate of right ascension
     * @param af0
     *        Clock correcting parameter af0 (s)
     * @param af1
     *        Clock correcting parameter af1 (s/s)
     * @param af2
     *        Clock correcting parameter af2 (s/s2)
     * @param deltaN
     *        Mean motion difference from computed value (rad/s)
     * @param iRate
     *        Rate of inclination angle (rad/s)
     * @param cuc
     *        Amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     * @param cus
     *        Amplitude of the sine harmonic correction term to the argument of latitude (rad)
     * @param crc
     *        Amplitude of the cosine harmonic correction term to the orbit radius (m)
     * @param crs
     *        Amplitude of the sine harmonic correction term to the orbit radius (m)
     * @param cic
     *        Amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     * @param cis
     *        Amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public LNAVGNSSParameters(final GNSSType gnssType, final double refTime, final double m0, final double e,
            final double sqrtA, final double omega0, final double i0, final double w0, final double rateRa,
            final double af0, final double af1, final double af2, final double deltaN, final double iRate,
            final double cuc, final double cus, final double crc, final double crs, final double cic,
            final double cis) {
        super(gnssType, refTime, m0, e, sqrtA, omega0, i0, w0, rateRa, af0, af1, af2, deltaN, iRate, cuc, cus, crc,
                crs, cic, cis, 0., 0.);
    }
}
