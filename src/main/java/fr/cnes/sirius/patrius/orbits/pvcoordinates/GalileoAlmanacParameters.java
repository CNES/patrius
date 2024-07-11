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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2608:27/01/2021:Mauvaise date de reference pour le Galileo System Time
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/**
 *
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class is a simple container for galileo almanac parameters.
 * </p>
 * 
 * @concurrency immutable
 * @author galpint
 * @version $Id: GalileoAlmanacParameters.java 17584 2017-05-10 13:26:39Z bignon $
 * @since 3.2
 * 
 */
public class GalileoAlmanacParameters extends AlmanacParameter {

    /** Roll-over week number for Galileo parameters. */
    public static final int ROLL_OVER_WEEK_GALILEO = 4096;

    /** 1000. */
    private static final double THOUSAND = 1000.0;
    
    /** 7 */
    private static final int SEVEN = 7;

    /**
     * Creates an instance of Galileo Almanac parameters
     * 
     * @param refDuration
     *        time applicability of Galileo almanac
     * @param week
     *        week of the Galileo almanac
     * @param m0
     *        Initial Mean anomaly
     * @param e
     *        Eccentricity
     * @param squareRootA
     *        Semi major axis square root
     * @param omega0
     *        Initial Right ascension
     * @param i0
     *        Orbital inclination
     * @param w0
     *        Argument of perigee
     * @param rateRa
     *        Rate of right ascension
     */
    public GalileoAlmanacParameters(final double refDuration, final int week, final double m0, final double e,
        final double squareRootA, final double omega0, final double i0, final double w0, final double rateRa) {
        super(refDuration, week, m0, e, squareRootA, omega0, i0, w0, rateRa);
    }

    /** {@inheritDoc} */
    @Override
    public int getRolloverWeeks() {
        return ROLL_OVER_WEEK_GALILEO;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate(final int weekNumber, final double milliInWeek) throws PatriusException {
        // Reference epoch for Galileo System Time: 1999-08-21T23:59:47 UTC (= 1999-08-22T00:00:19.000 TAI)
        final int day = (int) MathLib.floor(milliInWeek / (THOUSAND * Constants.JULIAN_DAY));
        final double secondsInDay = (milliInWeek / THOUSAND) - (day * Constants.JULIAN_DAY);
        // ref day 1999-08-22 shifted by day and secondsInDay
        return new AbsoluteDate(new DateComponents(DateComponents.GALILEO_EPOCH, (weekNumber * SEVEN) + day),
            new TimeComponents(secondsInDay), TimeScalesFactory.getGST());
    }
}
