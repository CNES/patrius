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
 * @history creation 15/02/12
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3315:22/05/2023:[PATRIUS] TabulatedAttitude compatible liste d'extension de Attitude
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:218:17/03/2014:the spin computation has been modified:
 * its value is the interpolation at the current date of the initial
 * and final spins
 * VERSION::DM:282:22/07/2014: add getAttitudes method
 * VERSION::FA:367:04/12/2014:Recette V2.3 corrections (changed getAttitudes return type)
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::DM:455:05/11/2015:Improved accuracy and performance of TabulatedAttitude class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::FA:1771:20/10/2018:correction round-off error
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexOpenClosed;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularDerivativesFilter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * This class implements the tabulated attitude leg.
 * </p>
 *
 * @concurrency immutable
 *
 * @see AttitudeLeg
 *
 * @author Thomas Trapier
 *
 * @version $Id: TabulatedAttitude.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 1.1
 *
 */
public class TabulatedAttitude implements AttitudeLeg {

    /** Default number of points used for interpolation. */
    public static final int DEFAULT_INTERP_ORDER = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 6595548109504426959L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "TABULATED_ATTITUDE";

    /** Nature. */
    private final String nature;

    /** Array of chronologically ordered attitudes. */
    private final Attitude[] attitudes;

    /** Array (same size of attitudes array) containing for each attitude time elapsed since first attitude. */
    private final double[] durations;

    /** Reference frame. */
    private final Frame referenceFrame;

    /** Number of points used for interpolation. */
    private final int interpOrder;

    /** Validity interval (with closed endpoints). */
    private final AbsoluteDateInterval validityInterval;

    /** Flag to indicate if spin derivation computation is activated. */
    private boolean spinDerivativesComputation = false;

    /** Filter to use in interpolation. */
    private AngularDerivativesFilter filter = AngularDerivativesFilter.USE_RR;

    /**
     * Constructor with default number N of points used for interpolation.
     *
     * @param attitudesIn
     *        the list of attitudes. WARNING : these attitudes must be ordered.
     * @throws PatriusException
     *         if there is not enough data for Hermite interpolation<br>
     *         if all the attitudes aren't associated to the same reference frame<br>
     *         if the attitudes aren't in chronological order
     */
    public TabulatedAttitude(final List<? extends Attitude> attitudesIn) throws PatriusException {
        this(attitudesIn, DEFAULT_INTERP_ORDER, DEFAULT_NATURE);
    }

    /**
     * Constructor with number of points used for interpolation.
     *
     * @param attitudesIn
     *        the list of attitudes. WARNING : these attitudes must be ordered.
     * @param nbInterpolationPoints
     *        number of points used for interpolation<br>
     *        Note: the special value {@code -1} is accepted to use the old
     *        {@link Attitude#slerp(AbsoluteDate, Attitude, Attitude, Frame, boolean) Attitude.slerp} constructor
     * @throws PatriusException
     *         if the number of points used for interpolation is {@code < 1} and {@code != -1}<br>
     *         if there is not enough data for Hermite interpolation<br>
     *         if all the attitudes aren't associated to the same reference frame<br>
     *         if the attitudes aren't in chronological order
     */
    public TabulatedAttitude(final List<? extends Attitude> attitudesIn,
                             final int nbInterpolationPoints) throws PatriusException {
        this(attitudesIn, nbInterpolationPoints, DEFAULT_NATURE);
    }

    /**
     * Constructor with default number N of points used for interpolation.
     *
     * @param attitudesIn
     *        the list of attitudes. WARNING : these attitudes must be ordered.
     * @param natureIn
     *        leg nature
     * @throws PatriusException
     *         if there is not enough data for Hermite interpolation<br>
     *         if all the attitudes aren't associated to the same reference frame<br>
     *         if the attitudes aren't in chronological order
     */
    public TabulatedAttitude(final List<? extends Attitude> attitudesIn, final String natureIn)
        throws PatriusException {
        this(attitudesIn, DEFAULT_INTERP_ORDER, natureIn);
    }

    /**
     * Constructor with number of points used for interpolation.
     *
     * @param attitudesIn
     *        the list of attitudes. WARNING : these attitudes must be ordered.
     * @param nbInterpolationPoints
     *        number of points used for interpolation<br>
     *        Note: the special value {@code -1} is accepted to use the old
     *        {@link Attitude#slerp(AbsoluteDate, Attitude, Attitude, Frame, boolean) Attitude.slerp} constructor
     * @param natureIn
     *        leg nature
     * @throws PatriusException
     *         if the number of points used for interpolation is {@code < 1} and {@code != -1}<br>
     *         if there is not enough data for Hermite interpolation<br>
     *         if all the attitudes aren't associated to the same reference frame<br>
     *         if the attitudes aren't in chronological order
     */
    public TabulatedAttitude(final List<? extends Attitude> attitudesIn, final int nbInterpolationPoints,
                             final String natureIn) throws PatriusException {
        this(attitudesIn, nbInterpolationPoints, false, natureIn);
    }

    /**
     * Constructor with number of points used for interpolation.
     *
     * @param attitudesIn
     *        the list of attitudes
     * @param nbInterpolationPoints
     *        number of points used for interpolation<br>
     *        Note: the special value {@code -1} is accepted to use the old
     *        {@link Attitude#slerp(AbsoluteDate, Attitude, Attitude, Frame, boolean) Attitude.slerp} constructor
     * @param needOrdering
     *        true if ordering is required (otherwise the attitudes must be ordered)
     * @param natureIn
     *        leg nature
     * @throws PatriusException
     *         if the number of points used for interpolation is {@code < 1} and {@code != -1}<br>
     *         if there is not enough data for Hermite interpolation<br>
     *         if all the attitudes aren't associated to the same reference frame<br>
     *         if the attitudes shouldn't be ordered ({@code needOrdering = false}) but their aren't in chronological
     *         order
     */
    public TabulatedAttitude(final List<? extends Attitude> attitudesIn, final int nbInterpolationPoints,
                             final boolean needOrdering, final String natureIn) throws PatriusException {


        // Check the number of points used for interpolation is valid
        if (nbInterpolationPoints < 1 && nbInterpolationPoints != -1) {
            throw new PatriusException(PatriusMessages.INVALID_NB_INTERPOLATION_POINTS);
        }

        // Check that there is enough data for Hermite interpolation
        final int attitudesSize = attitudesIn.size();
        if (attitudesSize < nbInterpolationPoints) {
            throw new PatriusException(PatriusMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION);
        }
        // Initialize number of points used for Hermite interpolation
        this.interpOrder = nbInterpolationPoints;

        // List to array transformation
        final Attitude[] tempArray = attitudesIn.toArray(new Attitude[attitudesSize]);

        // Extract the frame & date of the first attitude
        this.referenceFrame = tempArray[0].getReferenceFrame();
        AbsoluteDate date = tempArray[0].getDate();

        // Loop on each attitude (from the second one) to check their frames (and dates if require) are consistent
        for (int i = 1; i < attitudesSize; i++) {

            final Attitude currentAttitude = tempArray[i];
            // Check that all attitudes are associated to the same frame, otherwise throw an exception
            if (!this.referenceFrame.equals(currentAttitude.getReferenceFrame())) {
                throw new PatriusException(PatriusMessages.FRAMES_MISMATCH, this.referenceFrame,
                    currentAttitude.getReferenceFrame());
            }

            // If the attitudes don't need to be ordered, they should already be, otherwise throw an exception
            if (!needOrdering) {
                final AbsoluteDate currentDate = currentAttitude.getDate();
                if (currentDate.durationFrom(date) < 0) {
                    // The current attitude date is before the previous attitude date, meaning the attitudes aren't in
                    // chronological order
                    throw new PatriusException(PatriusMessages.NON_CHRONOLOGICAL_DATA);
                }
                date = currentDate;
            }
        }

        // Sort the attitudes if needed
        if (needOrdering) {
            Arrays.sort(tempArray);
        }
        this.attitudes = tempArray;

        // Dates interval creation
        final AbsoluteDate firstDate = this.attitudes[0].getDate();
        final AbsoluteDate lastDate = this.attitudes[attitudesSize - 1].getDate();
        this.validityInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, firstDate, lastDate,
            IntervalEndpointType.CLOSED);

        // Durations contains for each attitude the time elapsed since first attitude
        this.durations = new double[attitudesSize];
        for (int i = 0; i < attitudesSize; i++) {
            this.durations[i] = this.attitudes[i].getDate().durationFrom(firstDate);
        }
        this.nature = natureIn;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        return this.validityInterval;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProvider, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        return getAttitude(date, frame);
    }

    /**
     * Compute the attitude on the specified date in the reference frame.
     * 
     * @param date
     *        current date
     * @return attitude on the specified date
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    public Attitude getAttitude(final AbsoluteDate date) throws PatriusException {
        return getAttitude(date, this.referenceFrame);
    }

    /**
     * Compute the attitude on the specified date.
     * 
     * @param date
     *        current date
     * @param frame
     *        reference frame from which attitude is computed
     * @return attitude on the specified date
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    public Attitude getAttitude(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // test of the date
        if (!this.validityInterval.contains(date)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        }

        final double interplationDateSec = date.durationFrom(this.attitudes[0].getDate());
        int pos = new BinarySearchIndexOpenClosed(this.durations).getIndex(interplationDateSec);

        if (date.compareTo(this.attitudes[pos + 1].getDate()) == 1) {
            // Particular case: date is after tabulated date but due to loss of precision because
            // of durationFrom method, it is seen as tabulated date
            pos += 1;
        }

        // Attitude to return
        final Attitude attitude;

        if (pos == -1) {
            // Particular case: first attitude
            final Attitude attitudeTemp;
            if (this.filter.equals(AngularDerivativesFilter.USE_R)) {
                // Interpolate the first attitude rotation rate to be consistent with the rotation
                final List<Attitude> attitudesList = new ArrayList<>();
                attitudesList.add(this.attitudes[0]);
                attitudesList.add(this.attitudes[1]);
                attitudeTemp = new Attitude(frame, null).interpolate(date, attitudesList,
                    this.spinDerivativesComputation, this.filter);
            } else {
                // Return attitude without interpolation
                attitudeTemp = this.attitudes[0];
            }
            attitude = attitudeTemp.withReferenceFrame(frame, this.spinDerivativesComputation);

        } else if (pos + 1 < this.attitudes.length
                && date.equals(this.attitudes[pos + 1].getDate())) {
            // Particular case: date matches attitude date from array
            final Attitude attitudeTemp;
            if (this.filter.equals(AngularDerivativesFilter.USE_R)) {
                // Interpolate the attitude rotation rate to be consistent with the rotation
                final List<Attitude> attitudesList = new ArrayList<>();
                attitudesList.add(this.attitudes[pos]);
                attitudesList.add(this.attitudes[pos + 1]);
                attitudeTemp = new Attitude(frame, null).interpolate(date, attitudesList,
                    this.spinDerivativesComputation, this.filter);
            } else {
                // Return attitude without interpolation
                attitudeTemp = this.attitudes[pos + 1];
            }
            attitude = attitudeTemp.withReferenceFrame(frame, this.spinDerivativesComputation);

        } else {
            // Interpolate attitude
            if (this.interpOrder == -1) {
                // old constructor is used : use Attitude.slerp
                attitude = Attitude.slerp(date, this.attitudes[pos], this.attitudes[pos + 1],
                    frame, this.spinDerivativesComputation);
            } else {
                // Hermite interpolation
                attitude = this.computeAttitude(date, frame, pos);
            }
        }

        return attitude;
    }

    /**
     * Computation of the attitude using Hermite interpolation.
     *
     * @param dateInterpolate
     *        the interpolation date
     * @param frame
     *        the expression frame
     * @param pos
     *        position of attitude in attitude array
     * @return the interpolated {@link Attitude} at a given date
     *         using Hermite interpolation with {@link TabulatedAttitude#interpOrder} points
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    private Attitude computeAttitude(final AbsoluteDate dateInterpolate, final Frame frame,
                                     final int pos) throws PatriusException {

        // Local array of Hermite attitudes
        final List<Attitude> attitudesToHermite = new ArrayList<>();

        // determine beginning of array
        int debTab = pos - (int) MathLib.ceil(this.interpOrder / 2.) + 1;
        // modification of debTab if
        // - we are constrained by beginning of array
        debTab = (debTab < 0) ? 0 : debTab;
        // - we are constrained by end of array
        debTab =
            (debTab + this.interpOrder > this.attitudes.length) ? this.attitudes.length - this.interpOrder : debTab;

        // fill attitudes to Hermite
        for (int i = 0; i < this.interpOrder; i++) {
            attitudesToHermite.add(this.attitudes[debTab + i]
                .withReferenceFrame(frame, this.spinDerivativesComputation));
        }

        // Interpolation
        return new Attitude(frame, null).interpolate(dateInterpolate, attitudesToHermite,
            this.spinDerivativesComputation, this.filter);
    }

    /**
     * Return a new law with the specified interval.
     *
     * @param interval
     *        new interval of validity
     * @return new tabulated attitude law
     */
    public TabulatedAttitude setTimeInterval(final AbsoluteDateInterval interval) {

        // test of the date interval : must be contained in the one of this object
        if (!this.validityInterval.includes(interval)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        try {
            // the reference frame for the ending points is the one of the first known attitude
            final Frame frame = this.attitudes[0].getReferenceFrame();

            // the first and last points must be computed
            // search of the new starting attitude
            final Attitude firstAtt = this.getAttitude(null, interval.getLowerData(), frame);
            final double relativeDateFirst = firstAtt.getDate().durationFrom(this.attitudes[0].getDate());
            final int lastBeforeFirst = new BinarySearchIndexOpenClosed(this.durations).getIndex(relativeDateFirst);

            // search of the new ending attitude
            final Attitude lastAtt = this.getAttitude(null, interval.getUpperData(), frame);
            final double relativeDateLast = lastAtt.getDate().durationFrom(this.attitudes[0].getDate());
            final int lastBeforeLast = new BinarySearchIndexOpenClosed(this.durations).getIndex(relativeDateLast);

            // computation of the new attitude array size
            final int newArraySize = lastBeforeLast - lastBeforeFirst;
            final List<Attitude> newList = new ArrayList<>();

            // filling of the new list
            newList.add(firstAtt);
            for (int i = 1; i < newArraySize + 1; i++) {
                newList.add(this.attitudes[lastBeforeFirst + i]);
            }
            newList.add(lastAtt);

            // return of the new law containing the new array
            final TabulatedAttitude res = new TabulatedAttitude(newList, this.interpOrder, this.nature);
            res.setSpinDerivativesComputation(this.spinDerivativesComputation);
            res.setAngularDerivativesFilter(this.filter);
            return res;
        } catch (final PatriusException e) {
            // Should not happen
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /**
     * Getter for the non-interpolated and rightly ordered attitudes.
     *
     * @return the list of attitudes
     */
    public List<Attitude> getAttitudes() {
        final List<Attitude> list = new ArrayList<>();
        final int listSize = this.attitudes.length;
        for (int i = 0; i < listSize; i++) {
            list.add(this.attitudes[i]);
        }
        return list;
    }

    /**
     * Getter for the reference frame.
     *
     * @return referenceFrame reference frame from which attitude is defined.
     */
    public Frame getReferenceFrame() {
        return this.referenceFrame;
    }

    /**
     * Getter for the durations.
     *
     * @return the durations
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getDurations() {
        return this.durations;
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.spinDerivativesComputation = computeSpinDerivatives;
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return this.nature;
    }

    /** {@inheritDoc} */
    @Override
    public TabulatedAttitude copy(final AbsoluteDateInterval newIntervalOfValidity) {
        return this.setTimeInterval(newIntervalOfValidity);
    }

    /**
     * Returns spin derivatives computation flag.
     *
     * @return spin derivatives computation flag
     */
    public boolean isSpinDerivativesComputation() {
        return this.spinDerivativesComputation;
    }

    /**
     * Setter for the filter to use in interpolation.
     *
     * @param angularDerivativeFilter the filter to set
     */
    public void setAngularDerivativesFilter(final AngularDerivativesFilter angularDerivativeFilter) {
        this.filter = angularDerivativeFilter;
    }

    /**
     * Getter for the angular derivative filter.
     * 
     * @return the angular derivative filter
     */
    protected AngularDerivativesFilter getAngularDerivativeFilter() {
        return this.filter;
    }

    /**
     * Getter for the interpolation order.
     * 
     * @return the interpolation order
     */
    protected int getInterpolationOrder() {
        return this.interpOrder;
    }
}
