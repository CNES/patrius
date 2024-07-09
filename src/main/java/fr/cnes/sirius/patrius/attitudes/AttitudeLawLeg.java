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
 * @history creation 21/12/2011
 * HISTORY
 * VERSION:4.7:DM:DM-2847:18/05/2021:Modification de la gestion de la date hors intervalle
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.4:DM:DM-2209:04/10/2019:[PATRIUS] Amelioration de AttitudeLawLeg
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents an attitude law version "attitude", with an interval of validity (whose borders are closed
 * points).
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of an AttitudeLaw makes it thread-safe only if the AttitudeLaw is.
 * 
 * @author Julie Anton
 * 
 * @version $Id: AttitudeLawLeg.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class AttitudeLawLeg implements AttitudeLeg {

    /** Serializable UID. */
    private static final long serialVersionUID = 165478998919828375L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ATTITUDE_LAW_LEG";

    /** Nature. */
    private final String nature;

    /** Law provider. */
    private final AttitudeLaw law;

    /** Interval of validity (with closed endpoints). */
    private AbsoluteDateInterval interval;

    /** True if leg can be used outside its validity interval. */
    private boolean timeTolerant;
    
    /**
     * Build an attitude law version "attitude".
     * <p>
     * Its interval of validity has closed endpoints except for {@link AbsoluteDate#PAST_INFINITY} and
     * {@link AbsoluteDate#FUTURE_INFINITY}.
     * </p>
     * 
     * @param attitudeLaw
     *        : provider of the attitude law
     * @param initialDate
     *        : start date of the interval of validity
     * @param finalDate
     *        : end date of the interval of validity
     */
    public AttitudeLawLeg(final AttitudeLaw attitudeLaw, final AbsoluteDate initialDate,
        final AbsoluteDate finalDate) {
        this(attitudeLaw, initialDate, finalDate, DEFAULT_NATURE);
    }

    /**
     * Build an attitude law version "attitude".
     * <p>
     * Its interval of validity has closed endpoints except for {@link AbsoluteDate#PAST_INFINITY} and
     * {@link AbsoluteDate#FUTURE_INFINITY}.
     * </p>
     * 
     * @param attitudeLaw
     *        : provider of the attitude law
     * @param initialDate
     *        : start date of the interval of validity
     * @param finalDate
     *        : end date of the interval of validity
     * @param natureIn leg nature
     */
    public AttitudeLawLeg(final AttitudeLaw attitudeLaw, final AbsoluteDate initialDate,
                          final AbsoluteDate finalDate, final String natureIn) {
        this(attitudeLaw, initialDate, finalDate, natureIn, false);
    }

    /**
     * Build an attitude law version "attitude".
     * <p>
     * Its interval of validity has closed endpoints except for {@link AbsoluteDate#PAST_INFINITY} and
     * {@link AbsoluteDate#FUTURE_INFINITY}.
     * </p>
     * 
     * @param attitudeLaw
     *        : provider of the attitude law
     * @param initialDate
     *        : start date of the interval of validity
     * @param finalDate
     *        : end date of the interval of validity
     * @param natureIn leg nature
     * @param timeTolerant true if leg can be used outside its validity interval, false otherwise
     */
    public AttitudeLawLeg(final AttitudeLaw attitudeLaw, final AbsoluteDate initialDate,
                          final AbsoluteDate finalDate, final String natureIn,
                          final boolean timeTolerant) {
        this.law = attitudeLaw;

        final IntervalEndpointType lowerEndpoint;
        if (initialDate != AbsoluteDate.PAST_INFINITY) {
            lowerEndpoint = IntervalEndpointType.CLOSED;
        } else {
            lowerEndpoint = IntervalEndpointType.OPEN;
        }

        final IntervalEndpointType upperEndpoint;
        if (finalDate != AbsoluteDate.FUTURE_INFINITY) {
            upperEndpoint = IntervalEndpointType.CLOSED;
        } else {
            upperEndpoint = IntervalEndpointType.OPEN;
        }

        this.interval = new AbsoluteDateInterval(lowerEndpoint, initialDate, finalDate,
            upperEndpoint);
        this.nature = natureIn;
        this.timeTolerant = timeTolerant;
    }
    
    /**
     * Build an attitude law version "attitude".
     * 
     * @param attitudeLaw
     *        : provider of the attitude law
     * @param dateInterval
     *        : interval of validity
     */
    public AttitudeLawLeg(final AttitudeLaw attitudeLaw, final AbsoluteDateInterval dateInterval) {
        this(attitudeLaw, dateInterval, DEFAULT_NATURE);
    }

    /**
     * Build an attitude law version "attitude".
     * 
     * @param attitudeLaw 
     *        : provider of the attitude law
     * @param dateInterval
     *        : interval of validity
     * @param natureIn 
     *        : leg nature
     */
    public AttitudeLawLeg(final AttitudeLaw attitudeLaw, final AbsoluteDateInterval dateInterval,
                          final String natureIn) {
        this(attitudeLaw, dateInterval, natureIn, false);
    }

    /**
     * Build an attitude law version "attitude".
     * 
     * @param attitudeLaw 
     *        : provider of the attitude law
     * @param dateInterval
     *        : interval of validity
     * @param natureIn 
     *        : leg nature
     * @param timeTolerant true if leg can be used outside its validity interval, false otherwise
     */
    public AttitudeLawLeg(final AttitudeLaw attitudeLaw, final AbsoluteDateInterval dateInterval,
                          final String natureIn, final boolean timeTolerant) {
        this.law = attitudeLaw;
        this.interval = dateInterval;
        this.nature = natureIn;
        this.timeTolerant = timeTolerant;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        return this.interval;
    }

    /**
     * Gets the attitude law provider associated to the current attitude leg.
     * 
     * @return the {@link AttitudeLaw} of the current leg
     */
    public AttitudeLaw getAttitudeLaw() {
        return this.law;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        if (timeTolerant || this.interval.contains(date)) {
            return this.law.getAttitude(pvProv, date, frame);
        } else {
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        }
    }
    
    /** {@inheritDoc}
     * <p>Provided interval does not have to be included in current time interval.</p>
     */
    @Override
    public AttitudeLawLeg copy(final AbsoluteDateInterval newIntervalOfValidity) {
        return new AttitudeLawLeg(law, newIntervalOfValidity, nature);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivatives computation applies to underlying {@link #law}.
     * </p>
     */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.law.setSpinDerivativesComputation(computeSpinDerivatives);
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return this.nature;
    }
}
