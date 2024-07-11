/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * <p>
 * This class is a simple container for GPS almanac parameters.
 * </p>
 * 
 * @concurrency immutable
 * @author galpint
 * @version $Id: GPSAlmanacParameters.java 17584 2017-05-10 13:26:39Z bignon $
 * @since 3.2
 * 
 */
public class GPSAlmanacParameters extends AlmanacParameter {

    /** Roll-over week number for GPS parameters. */
    public static final int ROLL_OVER_WEEK_GPS = 1024;

    /** Serializable UID. */
    private static final long serialVersionUID = 6216495970280681770L;

    /**
     * Creates an instance of GPS Almanac parameters.
     * 
     * @param refDuration
     *        time applicability of GPS almanac
     * @param week
     *        week of the GPS almanac
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
     */
    public GPSAlmanacParameters(final double refDuration, final int week, final double m0, final double e,
        final double squareRootA, final double omega0, final double i0, final double w0, final double rateRa) {
        super(refDuration, week, m0, e, squareRootA, omega0, i0, w0, rateRa);
    }

    /** {@inheritDoc} */
    @Override
    public int getRolloverWeeks() {
        return ROLL_OVER_WEEK_GPS;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate(final int weekNumber, final double milliInWeek) {
        return AbsoluteDate.createGPSDate(weekNumber, milliInWeek);
    }
}
