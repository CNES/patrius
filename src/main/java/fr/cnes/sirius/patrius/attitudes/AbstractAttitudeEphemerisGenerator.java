/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history created 10/04/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:559:26/02/2016:minor corrections
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This abstract class handles the generation of attitude ephemeris from an attitude laws sequence
 * {@link StrictAttitudeLegsSequence}.<br>
 * The ephemeris generation can be done using a fixed time step or a variable time step, setting the generation time
 * interval (the default value is the time interval of the sequence), and the treatment to apply to the transition
 * points of the sequence (ignore them, compute the attitude of the initial date of the laws, compute the attitude of
 * the initial and final date of the laws).
 * 
 * @see FixedStepAttitudeEphemerisGenerator
 * @see VariableStepAttitudeEphemerisGenerator
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AbstractAttitudeEphemerisGenerator.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public abstract class AbstractAttitudeEphemerisGenerator {

    /** The start date of the laws are computed. */
    public static final int START_TRANSITIONS = 1;
    /** The start and the end point of the laws are computed. */
    public static final int START_END_TRANSITIONS = 2;
    /** The transition points are ignored. */
    public static final int NO_TRANSITIONS = 0;

    /** Attitude legs sequence. */
    protected final StrictAttitudeLegsSequence<AttitudeLeg> sequence;

    /** Flag specifying what to do with the attitude laws transition points. */
    protected final int transitions;

    /** PV coordinate provider. */
    protected final PVCoordinatesProvider provider;

    /**
     * Simple constructor. <br>
     * 
     * @param legsSequence
     *        the sequence of attitude legs.
     * @param transitionPoints
     *        what to do with the attitude laws transition points.
     * @param providerIn PV coordinates provider
     */
    public AbstractAttitudeEphemerisGenerator(final StrictAttitudeLegsSequence<AttitudeLeg> legsSequence,
                                              final int transitionPoints, final PVCoordinatesProvider providerIn) {
        this.sequence = legsSequence;
        this.transitions = transitionPoints;
        if (this.transitions != 0 && this.transitions != 1 && this.transitions != 2) {
            // invalid transition points parameter:
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNKNOWN_TRANSITION_PARAMETER);
        }
        this.provider = providerIn;
    }

    /**
     * Computes attitude ephemeris using a fixed or variable time step. The interval of validity coincides with
     * the time interval of the sequence.
     * 
     * @param frame
     *        the frame of the computed attitude ephemeris
     * @return the attitude ephemeris
     * @throws PatriusException
     *         an orekit exception
     */
    public SortedSet<Attitude> generateEphemeris(final Frame frame) throws PatriusException {
        return this.generateEphemeris(getTimeInterval(), frame);
    }

    /**
     * Computes attitude ephemeris using a fixed or variable time step and choosing the interval of validity.
     * 
     * @param ephemerisInterval
     *        the interval of validity of the ephemeris
     * @param frame
     *        the frame of the computed attitude ephemeris
     * @return the attitude ephemeris
     * @throws PatriusException
     *         an orekit exception
     */
    public SortedSet<Attitude> generateEphemeris(final AbsoluteDateInterval ephemerisInterval,
                                                 final Frame frame) throws PatriusException {
        if (!getTimeInterval().includes(ephemerisInterval)) {
            // the ephemeris time interval is not included in the interval of validity of the sequence:
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }
        // creates the output ephemeris:
        final TreeSet<Attitude> ephemeris = new TreeSet<Attitude>(new AttitudeChronologicalComparator());
        // sets the date:
        AbsoluteDate date = ephemerisInterval.getLowerData();
        // boolean that indicates if the current date is a transition date:
        boolean isTransition = false;
        AbsoluteDate transitionDate = this.getNextTransitionPoint(date, ephemerisInterval);
        // computes the duration between the current date and the transition date:
        double transitionStep = transitionDate.durationFrom(date);
        while (date.compareTo(ephemerisInterval.getUpperData()) < 0) {

            // call the method to compute the step:
            final double step = this.computeStep(date, ephemerisInterval);

            // computes ephemeris until the date does not exceed the upper limit of the sequence time interval:
            if (this.transitions == NO_TRANSITIONS) {
                ephemeris.add(this.sequence.getAttitude(this.provider, date, frame));
                date = date.shiftedBy(step);
            } else {
                if (isTransition) {
                    switch (this.transitions) {
                    // transition date:
                        case START_TRANSITIONS:
                            ephemeris.add(this.sequence.getAttitude(this.provider, date, frame));
                            break;
                        case START_END_TRANSITIONS:
                            ephemeris.add(getPreviousAttitude(this.provider, date, frame));
                            ephemeris.add(this.sequence.getAttitude(this.provider, date, frame));
                            break;
                        default:
                            // branch never reached.
                    }
                } else {
                    // no transition date:
                    ephemeris.add(this.sequence.getAttitude(this.provider, date, frame));
                }
                if (step >= transitionStep) {
                    // next date at exactly the current transition date
                    date = transitionDate;
                    // updates the transition date:
                    transitionDate = this.getNextTransitionPoint(transitionDate, ephemerisInterval);
                    transitionStep = transitionDate.durationFrom(date);
                    isTransition = true;
                } else {
                    date = date.shiftedBy(step);
                    transitionStep = transitionDate.durationFrom(date);
                    isTransition = false;
                }
            }
        }
        if (this.addLastPoint(ephemerisInterval) || date.durationFrom(ephemerisInterval.getUpperData()) == 0) {
            // adds the last point of the time interval:
            ephemeris.add(this.sequence.getAttitude(this.provider, ephemerisInterval.getUpperData(), frame));
        }
        return ephemeris;
    }

    /**
     * Returns attitude from previous leg (compared to leg matching provided date) from the
     * sequence.
     * <p>
     * Warning: previous attitude leg has to be defined at provided date. Otherwise null is returned
     * </p>
     * This method is mainly used for switching sequence in order to retrieve attitude before/after
     * a switch. Thus provided date should be a switching date.
     *
     * @param pvProv spacecraft's position and velocity coordinates provider
     * @param date the date for which the attitude is computed
     * @param frame the frame for which the attitude is computed
     * @return the attitude if existent, null otherwise
     * @throws PatriusException thrown if attitude could not be retrieved
     */
    public Attitude getPreviousAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final AttitudeLeg currentL = sequence.current(date);
        Attitude attitude = null;
        if (currentL != null) {
            final AttitudeLeg leg = sequence.previous(currentL);
            if (leg != null) {
                attitude = leg.getAttitude(pvProv, date, frame);
            }
        }
        return attitude;
    }

    /**
     * Computes the step used during attitude ephemeris generation.
     * 
     * @param date
     *        the date
     * @param ephemerisInterval
     *        the interval of validity of the ephemeris
     * @return the computed step
     * @throws PatriusException
     *         an orekit exception
     */
    protected abstract double computeStep(final AbsoluteDate date,
                                          final AbsoluteDateInterval ephemerisInterval) throws PatriusException;

    /**
     * Decide if adding the last point of the time interval to the ephemeris list.
     * 
     * @param ephemerisInterval
     *        the interval of validity of the ephemeris.
     * @return true if the attitude at the last point has to be computed.
     */
    protected abstract boolean addLastPoint(final AbsoluteDateInterval ephemerisInterval);

    /**
     * Returns the first transition date following another given transition date. A transition date represents a change
     * of attitude law in the sequence.
     * 
     * @param transitionDate
     *        the current transition date
     * @param ephemerisInterval
     *        the interval of validity of the ephemeris
     * @return the date of the first transition point next to the given date
     * @throws PatriusException
     *         an orekit exception
     * 
     */
    @SuppressWarnings("unchecked")
    private AbsoluteDate getNextTransitionPoint(final AbsoluteDate transitionDate,
                                                final AbsoluteDateInterval ephemerisInterval) throws PatriusException {
        AttitudeLeg law = (AttitudeLeg) this.sequence.first();
        AbsoluteDate next = null;
        // checks if the input date is contained in the current attitude law:
        boolean isDateInLaw = law.getTimeInterval().contains(transitionDate);
        while (!isDateInLaw) {
            // iterates over the sequence to find the attitude law containing the input date:
            law = (AttitudeLeg) this.sequence.next(law);
            isDateInLaw = law.getTimeInterval().contains(transitionDate);
        }
        if (transitionDate.equals(ephemerisInterval.getLowerData())) {
            // the given date is the beginning of the interval, gets the lower point of the first law:
            next = law.getTimeInterval().getUpperData();
        } else if (transitionDate.equals(ephemerisInterval.getUpperData())) {
            // the given date is the end of the interval, returns it:
            next = transitionDate;
        } else {
            // nominal case: gets the next attitude law and returns its upper point:
            next = this.sequence.next(law).getTimeInterval().getUpperData();
        }
        return next;
    }

    /**
     * Returns the underlying sequence time interval.
     * @return the underlying sequence time interval
     */
    protected AbsoluteDateInterval getTimeInterval() {
        if (this.sequence.isEmpty()) {
            return null;
        } else  {
            return new AbsoluteDateInterval(this.sequence.first().getDate(), this.sequence.last().getEnd());
        }
    }
}
