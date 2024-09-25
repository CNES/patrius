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
 * This class is a simple container for an almanac ephemeris description parameters of GNSS
 * satellites (GPS, Galileo or BeiDou)
 * </p>
 *
 * @author fteilhard
 *
 *
 */
public class AlmanacGNSSParameters extends GNSSParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = 2652853861609212343L;

    /**
     * Creates an instance of GNSS Parameter for the almanac model (for GPS, Galileo or BeiDou)
     * @param gnssType
     *        the type of satellite (GPS, Galileo or BeiDou)
     * @param refDuration
     *        time applicability of the almanac
     * @param m0
     *        Initial Mean anomaly
     * @param e
     *        Eccentricity
     * @param squareRootA
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
     *
     * @since 3.1
     */
    public AlmanacGNSSParameters(final GNSSType gnssType, final double refDuration, final double m0,
            final double e, final double squareRootA, final double omega0, final double i0, final double w0,
            final double rateRa, final double af0, final double af1) {
        super(gnssType, refDuration, m0, e, squareRootA, omega0, i0, w0, rateRa, af0, af1, 0., 0., 0., 0., 0., 0., 0.,
                0., 0., 0., 0.);
    }
}
