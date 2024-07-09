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
 * @history Created on 09/11/2015
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:09/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class is a simple container for generic almanac parameters.
 * </p>
 * 
 * @concurrency immutable
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id: AlmanacParameter.java 17584 2017-05-10 13:26:39Z bignon $
 * 
 * @since 3.1
 * 
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class AlmanacParameter {
    // CHECKSTYLE: resume AbstractClassName check

    /** Reference week. */
    private final int weekRef;

    /** Time of applicability as a duration from the reference week in s. */
    private final double tRef;

    /** Initial Mean anomaly */
    private final double meanAnomaly0;

    /** Eccentricity */
    private final double eccentricity;

    /** Semi major axis square root */
    private final double sqrtA;

    /** Initial right ascension */
    private final double rightAscension0;

    /** Orbital inclination */
    private final double i;

    /** Argument of perigee */
    private final double w;

    /** Rate of right ascension */
    private final double omegaRate;

    /**
     * Creates an instance of AlmanacParameter
     * 
     * @param refDuration
     *        time applicability of the almanac
     * @param week
     *        week of the almanac
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
     * 
     * @since 3.1
     */
    public AlmanacParameter(final double refDuration, final int week, final double m0,
        final double e, final double squareRootA,
        final double omega0, final double i0,
        final double w0, final double rateRa) {
        this.tRef = refDuration;
        this.weekRef = week;
        this.meanAnomaly0 = m0;
        this.eccentricity = e;
        this.sqrtA = squareRootA;
        this.rightAscension0 = omega0;
        this.i = i0;
        this.w = w0;
        this.omegaRate = rateRa;
    }

    /**
     * @return the initial mean anomaly
     */
    public double getMeanAnomalyInit() {
        return this.meanAnomaly0;
    }

    /**
     * @return the eccentricity
     */
    public double getEccentricity() {
        return this.eccentricity;
    }

    /**
     * @return the square root of the semi-major axis
     */
    public double getSqrtA() {
        return this.sqrtA;
    }

    /**
     * @return the initial right ascension of ascending node
     */
    public double getOmegaInit() {
        return this.rightAscension0;
    }

    /**
     * @return the orbital inclination
     */
    public double getI() {
        return this.i;
    }

    /**
     * @return the Argument of perigee
     */
    public double getW() {
        return this.w;
    }

    /**
     * @return the Rate of right ascension
     */
    public double getOmegaRate() {
        return this.omegaRate;
    }

    /**
     * @return the week number
     */
    public int getWeekRef() {
        return this.weekRef;
    }

    /**
     * @return the number of seconds in the week
     */
    public double gettRef() {
        return this.tRef;
    }

    /**
     * Returns almanach unambiguous week number.
     * Example: GPS week is coded over 10 Bytes => 1024 possibles weeks. Hence 1024 is returned.
     * 
     * @return the roll-over week number
     */
    public abstract int getRolloverWeeks();

    /**
     * Returns GNSS date given a week number and second in the week.
     * 
     * @param weekNumber
     *        week number taking roll-over into account
     * @param milliInWeek
     *        millisecond in week
     * @return GNSS date given a week number and milliseconds in the week
     * @throws PatriusException if UTC data can't be load
     */
    public abstract AbsoluteDate getDate(final int weekNumber, final double milliInWeek) throws PatriusException;

}
