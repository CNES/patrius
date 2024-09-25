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
 * @history created 28/05/18
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Date detector defined by relative date.
 *
 * @author Marie Capot
 *
 * @version $Id: NullMassPartDetector.java 18094 2017-10-02 17:18:57Z bignon $
 *
 * @since 4.1
 *
 */
public class RelativeDateDetector extends DateDetector {

    /** Serial identifier. **/
    private static final long serialVersionUID = -8906352140403481083L;

    /** Epsilon. */
    private static final double EPS = 0.5E-9;

    /** Target relative date. **/
    private final double relativeDate;

    /** Date of the reference event. **/
    private final AbsoluteDate referenceDate;

    /** Time scale. */
    private final TimeScale timeScale;

    /**
     * Constructor with target relative date and reference date. maxCheck (1.e10 s) and threshold
     * (1.0e-9 s). Default action after event detection RESET_STATE.
     *
     * @param relativeDateIn target relative date to find.
     * @param referenceDateIn reference date of the event.
     * @param offsetTimeScale time scale in which the relativeDate is expressed.
     * @throws ArithmeticException if relativeDate is NaN or if referenceDate contains NaN.
     */
    public RelativeDateDetector(final double relativeDateIn, final AbsoluteDate referenceDateIn,
        final TimeScale offsetTimeScale) throws ArithmeticException {
        super(getAbsoluteDate(referenceDateIn, relativeDateIn, offsetTimeScale));
        this.relativeDate = relativeDateIn;
        this.referenceDate = referenceDateIn;
        this.timeScale = offsetTimeScale;
    }

    /**
     * Constructor with target relative date and reference date. maxCheck, threshold and action are
     * also configurable.
     *
     * @param relativeDateIn target relative date.
     * @param referenceDateIn reference date of the event.
     * @param offsetTimeScale time scale in which the relativeDate is expressed.
     * @param maxCheck maximum checking interval (s).
     * @param threshold convergence threshold (s).
     * @param action action to be performed in case of event detection.
     * @throws ArithmeticException if referenceDate contains NaN or if relativeDate is NaN.
     */
    public RelativeDateDetector(final double relativeDateIn, final AbsoluteDate referenceDateIn,
        final TimeScale offsetTimeScale, final double maxCheck, final double threshold,
        final Action action) throws ArithmeticException {
        super(getAbsoluteDate(referenceDateIn, relativeDateIn, offsetTimeScale), maxCheck, threshold,
            action);
        this.relativeDate = relativeDateIn;
        this.referenceDate = referenceDateIn;
        this.timeScale = offsetTimeScale;
    }

    /**
     * Get the absolute date of the event detector.
     * 
     * @param relativeDate target relative date.
     * @param referenceDate reference date of the event.
     * @param timeScale time scale in which the relativeDate is expressed in.
     * @return the event detector absolute date.
     * @throws ArithmeticException if referenceDate contains NaN or if relativeDate is NaN.
     */
    private static AbsoluteDate
            getAbsoluteDate(final AbsoluteDate referenceDate,
                            final double relativeDate, final TimeScale timeScale) throws ArithmeticException {
        if (referenceDate == null || timeScale == null) {
            throw new IllegalArgumentException("Reference date or time scale are undefined");
        }

        // Converting offset in seconds to a integer number of days from ref and
        // a real number of seconds from start of day (always positive)
        final double daysFromRef = relativeDate / Constants.JULIAN_DAY;
        int daysIntFromRef = (int) daysFromRef;
        if ((daysFromRef < 0) && (MathLib.abs(daysIntFromRef - daysFromRef) > EPS)) {
            daysIntFromRef = daysIntFromRef - 1;
        }
        final double secOfDay = relativeDate - daysIntFromRef * Constants.JULIAN_DAY;

        // Computing the new date and setting value in date field
        return computeDate(referenceDate, daysIntFromRef, secOfDay, timeScale);
    }

    /**
     * Compute an AbsoluteDate from an origin with an elapsed time given by a number of integer days
     * and the seconds of day in a given time scale.
     * 
     * @param dateOrigin origin of date
     * @param jjFromOrigin integer number of julian days from the origin
     * @param secOfDay real number of seconds of day
     * @param timeScale time scale
     * @return the AbsoluteDate computed
     */
    private static AbsoluteDate computeDate(final AbsoluteDate dateOrigin, final int jjFromOrigin,
                                            final double secOfDay, final TimeScale timeScale) {
        // Integer number of seconds from the origin to the start of the day
        final double secFromOriginToStartOfDay = jjFromOrigin * Constants.JULIAN_DAY;

        // Computing the date associated to the start of day
        final AbsoluteDate date = new AbsoluteDate(dateOrigin, secFromOriginToStartOfDay, timeScale);

        // Updating date
        return new AbsoluteDate(date, secOfDay, timeScale);
    }

    /**
     * Getter for the target relative date in seconds.
     * 
     * @return the target relative date in seconds.
     */
    public double getRelativeDate() {
        return this.relativeDate;
    }

    /**
     * Getter for the reference date of the event.
     * 
     * @return the reference date of the event.
     */
    public AbsoluteDate getReferenceDate() {
        return this.referenceDate;
    }

    /**
     * Getter for the time scale of the event.
     * 
     * @return the time scale of the event.
     */
    public TimeScale getTimeScale() {
        return this.timeScale;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new RelativeDateDetector(this.relativeDate, this.referenceDate, this.timeScale);
    }
}
