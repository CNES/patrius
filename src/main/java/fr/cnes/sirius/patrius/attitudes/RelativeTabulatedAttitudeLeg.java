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
 * @history creation 18/11/2015
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:392:18/11/2015:Creation of the class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::FA:1771:20/10/2018:correction round-off error
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexOpenClosed;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.AngularDerivativesFilter;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class implements the tabulated attitude leg relative to a reference date.
 * WARNING : Double being less accurate than an AbsoluteDate, this class is less accurate
 * than the {@link TabulatedAttitude} class.
 * 
 * @concurrency immutable
 * 
 * @author galpint
 * 
 * @version $Id: RelativeTabulatedAttitudeLeg.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class RelativeTabulatedAttitudeLeg implements AttitudeLeg {

    /** Serializable UID. */
    private static final long serialVersionUID = 1L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "RELATIVE_TABULATED_ATTITUDE";

    /** Nature. */
    private final String nature;

    /** Reference date */
    private final AbsoluteDate refDate;

    /** Array of chronologically ordered angular coordinates */
    private final AngularCoordinates[] attitudes;

    /** Array (same size of attitudes array) containing for each attitude time elapsed since reference date */
    private final double[] durations;

    /** reference frame from which angular coordinates are computed */
    private final Frame refFrame;

    /** validity interval of the law (with closed endpoints). */
    private AbsoluteDateInterval validityInterval;

    /** Filter to use for Hermite interpolation */
    private final AngularDerivativesFilter filter;

    /** Number of points used for interpolation */
    private final int interpOrder;

    /** Flag to indicate if spin derivation computation is activated. */
    private boolean spinDerivativesComputation = false;

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of Rotations
     * associated with a double representing the time elapsed since the reference date.
     * The number of points used for interpolation is the default one (defined in {@link TabulatedAttitude})
     * Rotations rates (set to 0's) will not be used for interpolation.
     * 
     * @param referenceDate
     *        reference date
     * @param orientations
     *        rotations. WARNING : these must be chronologically ordered.
     * @param frame
     *        reference frame from which attitude is computed
     * @throws PatriusException
     *         thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final List<Pair<Double, Rotation>> orientations,
                                        final Frame frame) throws PatriusException {
        this(referenceDate, orientations, frame, TabulatedAttitude.DEFAULT_INTERP_ORDER, DEFAULT_NATURE);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of Rotations
     * associated with a double representing the time elapsed since the reference date
     * and a number of points used for interpolation.
     * The List of angular coordinates is built with rotations rates set to 0's
     * and rotations rates (set to 0's) will not be used for interpolation
     * 
     * @param referenceDate
     *        reference date
     * @param orientations
     *        rotations. WARNING : these must be chronologically ordered.
     * @param frame
     *        reference frame from which attitude is computed
     * @param nbInterpolationPoints
     *        nbInterpolationPoints number of points used for interpolation
     * @throws PatriusException
     *         thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final List<Pair<Double, Rotation>> orientations,
                                        final Frame frame,
                                        final int nbInterpolationPoints) throws PatriusException {
        this(referenceDate, buildAngularCoordinatesMap(orientations), frame, false, nbInterpolationPoints,
                DEFAULT_NATURE);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of angular coordinates
     * associated with a double representing the time elapsed since the reference date.
     * The rotation rates will be used for interpolation.
     * The number of points used for interpolation is the default one (defined in {@link TabulatedAttitude})
     * 
     * @param referenceDate
     *        reference date
     * @param angularCoordinates
     *        angular coordinates. WARNING : these must be chronologically ordered.
     * @param frame
     *        reference frame from which attitude is computed
     * @throws PatriusException
     *         thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
            final Frame frame,
            final List<Pair<Double, AngularCoordinates>> angularCoordinates) throws PatriusException {
        this(referenceDate, angularCoordinates, TabulatedAttitude.DEFAULT_INTERP_ORDER, frame, DEFAULT_NATURE);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of angular coordinates
     * associated with a double representing the time elapsed since the reference date
     * and a number of points used for interpolation. The rotation rates will be used for interpolation.
     * 
     * @param referenceDate
     *        reference date
     * @param angularCoordinates
     *        angular coordinates WARNING : these must be chronologically ordered.
     * @param frame
     *        reference frame from which attitude is computed
     * @param nbInterpolationPoints
     *        nbInterpolationPoints number of points used for interpolation
     * @throws PatriusException
     *         thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final List<Pair<Double, AngularCoordinates>> angularCoordinates,
                                        final int nbInterpolationPoints,
                                        final Frame frame) throws PatriusException {
        this(referenceDate, angularCoordinates, frame, true, nbInterpolationPoints, DEFAULT_NATURE);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of Rotations associated
     * with a double representing the time elapsed since the reference date. The number of points
     * used for interpolation is the default one (defined in {@link TabulatedAttitude}) Rotations
     * rates (set to 0's) will not be used for interpolation.
     * 
     * @param referenceDate reference date
     * @param orientations rotations. WARNING : these must be chronologically ordered.
     * @param frame reference frame from which attitude is computed
     * @param natureIn leg nature
     * @throws PatriusException thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final List<Pair<Double, Rotation>> orientations,
                                        final Frame frame,
                                        final String natureIn) throws PatriusException {
        this(referenceDate, orientations, frame, TabulatedAttitude.DEFAULT_INTERP_ORDER, natureIn);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of Rotations associated
     * with a double representing the time elapsed since the reference date and a number of points
     * used for interpolation. The List of angular coordinates is built with rotations rates set to
     * 0's and rotations rates (set to 0's) will not be used for interpolation
     * 
     * @param referenceDate reference date
     * @param orientations rotations. WARNING : these must be chronologically ordered.
     * @param frame reference frame from which attitude is computed
     * @param nbInterpolationPoints nbInterpolationPoints number of points used for interpolation
     * @param natureIn leg nature
     * @throws PatriusException thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final List<Pair<Double, Rotation>> orientations,
                                        final Frame frame,
                                        final int nbInterpolationPoints,
                                        final String natureIn) throws PatriusException {
        this(referenceDate, buildAngularCoordinatesMap(orientations), frame, false, nbInterpolationPoints, natureIn);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of angular coordinates
     * associated with a double representing the time elapsed since the reference date. The rotation
     * rates will be used for interpolation. The number of points used for interpolation is the
     * default one (defined in {@link TabulatedAttitude})
     * 
     * @param referenceDate reference date
     * @param angularCoordinates angular coordinates. WARNING : these must be chronologically
     *        ordered.
     * @param frame reference frame from which attitude is computed
     * @param natureIn leg nature
     * @throws PatriusException thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final Frame frame,
                                        final List<Pair<Double, AngularCoordinates>> angularCoordinates,
                                        final String natureIn) throws PatriusException {
        this(referenceDate, angularCoordinates, TabulatedAttitude.DEFAULT_INTERP_ORDER, frame, natureIn);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of angular coordinates
     * associated with a double representing the time elapsed since the reference date and a number
     * of points used for interpolation. The rotation rates will be used for interpolation.
     * 
     * @param referenceDate reference date
     * @param angularCoordinates angular coordinates WARNING : these must be chronologically
     *        ordered.
     * @param frame reference frame from which attitude is computed
     * @param nbInterpolationPoints nbInterpolationPoints number of points used for interpolation
     * @param natureIn leg nature
     * @throws PatriusException thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                        final List<Pair<Double, AngularCoordinates>> angularCoordinates,
                                        final int nbInterpolationPoints,
                                        final Frame frame,
                                        final String natureIn) throws PatriusException {
        this(referenceDate, angularCoordinates, frame, true, nbInterpolationPoints, natureIn);
    }

    /**
     * Build a RelativeTabulatedAttitudeLeg with a reference date, a list of angular coordinates
     * associated with a double representing the time elapsed since the reference date and a number
     * of points used for interpolation.
     * 
     * @param referenceDate reference date
     * @param angularCoordinates angular coordinates. WARNING : these must be chronologically
     *        ordered.
     * @param frame reference frame from which attitude is computed
     * @param useR flag to indicate if rotation rates should be used for Hermite interpolation
     * @param nbInterpolationPoints nbInterpolationPoints number of points used for interpolation
     * @param natureIn leg nature
     * @throws PatriusException thrown if there is not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    private RelativeTabulatedAttitudeLeg(final AbsoluteDate referenceDate,
                                         final List<Pair<Double, AngularCoordinates>> angularCoordinates,
                                         final Frame frame,
                                         final boolean useR,
                                         final int nbInterpolationPoints,
                                         final String natureIn) throws PatriusException {

        // The array's size must be at least 2 to interpolate and create a date interval
        final int arraySize = angularCoordinates.size();
        if (arraySize < 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.AT_LEAST_TWO_ATTITUDES_NEEDED);
        }

        // Initialize attitudes and durations
        this.attitudes = new AngularCoordinates[arraySize];
        this.durations = new double[arraySize];
        for (int i = 0; i < arraySize; i++) {
            this.durations[i] = angularCoordinates.get(i).getFirst();
            this.attitudes[i] = angularCoordinates.get(i).getSecond();
        }

        // dates interval creation and initialization of refDate, refFrame, useRotationRates and interpOrder
        this.refDate = referenceDate;
        this.validityInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            this.refDate.shiftedBy(this.durations[0]), this.refDate.shiftedBy(this.durations[arraySize - 1]),
            IntervalEndpointType.CLOSED);
        this.refFrame = frame;
        this.interpOrder = nbInterpolationPoints;

        // Use rotation rates, or not
        this.filter = useR ? AngularDerivativesFilter.USE_RR : AngularDerivativesFilter.USE_R;

        // check that there is enough data for Hermite interpolation
        if (arraySize < this.interpOrder) {
            throw new PatriusException(PatriusMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION);
        }

        this.nature = natureIn;
    }

    /**
     * build a list of angular coordinates with list of rotations and rotation rates set to 0.
     * 
     * @param orientations
     *        the list of rotations
     * @return the list of angular coordinates
     * @since 3.1
     */
    private static List<Pair<Double, AngularCoordinates>>
            buildAngularCoordinatesMap(final List<Pair<Double, Rotation>> orientations) {
        final List<Pair<Double, AngularCoordinates>> res = new ArrayList<Pair<Double, AngularCoordinates>>();
        for (int i = 0; i < orientations.size(); i++) {
            final Pair<Double, AngularCoordinates> pairToAdd = new Pair<Double, AngularCoordinates>(orientations.get(i)
                .getFirst(), new AngularCoordinates(orientations.get(i).getSecond(), Vector3D.ZERO));
            res.add(pairToAdd);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        return this.validityInterval;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {

        // test of the date
        if (!this.validityInterval.contains(date)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        }

        // interpolation date in seconds
        final double interplationDateSec = date.durationFrom(this.refDate);
        int pos = new BinarySearchIndexOpenClosed(this.durations).getIndex(interplationDateSec);

        if (date.compareTo(this.refDate.shiftedBy(this.durations[pos + 1])) == 1) {
            // Particular case: date is after tabulated date but due to loss of precision because
            // of durationFrom method, it is seen as tabulated date
            pos += 1;
        }

        // Attitude to return
        final Attitude attitude;

        if (pos == -1) {
            // Particular case: first attitude
            final Attitude attitudeTemp;
            final Vector3D acc = this.spinDerivativesComputation ? this.attitudes[0]
                    .getRotationAcceleration() : null;
            if (this.filter.equals(AngularDerivativesFilter.USE_R)) {
                // Interpolate the first attitude rotation rate to be consistent with the rotation
                final List<TimeStampedAngularCoordinates> attitudesList = 
                        new ArrayList<TimeStampedAngularCoordinates>();
                attitudesList.add(new TimeStampedAngularCoordinates(this.refDate
                        .shiftedBy(this.durations[0]), this.attitudes[0].getRotation(),
                        this.attitudes[0].getRotationRate(), acc));
                attitudesList.add(new TimeStampedAngularCoordinates(this.refDate
                        .shiftedBy(this.durations[1]), this.attitudes[1].getRotation(),
                        this.attitudes[1].getRotationRate(), acc));
                attitudeTemp = new Attitude(this.refFrame,
                        TimeStampedAngularCoordinates.interpolate(date, this.filter, attitudesList,
                                this.spinDerivativesComputation));
            } else {
                // Return attitude without interpolation
                attitudeTemp = new Attitude(this.refDate, this.refFrame,
                        this.attitudes[0].getRotation(), this.attitudes[0].getRotationRate(), acc);
            }
            attitude = attitudeTemp.withReferenceFrame(frame, this.spinDerivativesComputation);

        } else if (pos + 1 < this.attitudes.length
                && date.equals(this.refDate.shiftedBy(this.durations[pos + 1]))) {
            // Particular case: date matches attitude date from array
            final Attitude attitudeTemp;
            final Vector3D acc = this.spinDerivativesComputation ? this.attitudes[pos + 1]
                    .getRotationAcceleration() : null;
            if (this.filter.equals(AngularDerivativesFilter.USE_R)) {
                // Interpolate the attitude rotation rate to be consistent with the rotation
                final List<TimeStampedAngularCoordinates> attitudesList = 
                        new ArrayList<TimeStampedAngularCoordinates>();
                attitudesList.add(new TimeStampedAngularCoordinates(this.refDate
                        .shiftedBy(this.durations[pos]), this.attitudes[pos].getRotation(),
                        this.attitudes[pos].getRotationRate(), acc));
                attitudesList.add(new TimeStampedAngularCoordinates(this.refDate
                        .shiftedBy(this.durations[pos + 1]), this.attitudes[pos + 1].getRotation(),
                        this.attitudes[pos + 1].getRotationRate(), acc));
                attitudeTemp = new Attitude(this.refFrame,
                        TimeStampedAngularCoordinates.interpolate(date, this.filter, attitudesList,
                                this.spinDerivativesComputation));
            } else {
                // Return attitude without interpolation
                attitudeTemp = new Attitude(this.refDate.shiftedBy(this.durations[pos + 1]),
                        this.refFrame, this.attitudes[pos + 1].getRotation(),
                        this.attitudes[pos + 1].getRotationRate(), acc);
            }
            attitude = attitudeTemp.withReferenceFrame(frame, this.spinDerivativesComputation);

        } else {
            // Interpolate attitude
            attitude = this.interpolateAttitude(date, frame);
        }

        return attitude;
    }

    /**
     * Private method to compute interpolation
     * 
     * @param frame
     *        interpolation frame
     * @param date
     *        interpolation date
     * @return the interpolated attitude
     * @throws PatriusException
     *         if interpolation fails
     * @since 3.1
     */
    private Attitude interpolateAttitude(final AbsoluteDate date,
            final Frame frame) throws PatriusException {

        // Attitude size
        final int sizeAttitude = this.attitudes.length;

        // Local array of Hermite angular Coordinates
        final List<TimeStampedAngularCoordinates> attitudesToHermite = new ArrayList<TimeStampedAngularCoordinates>();

        // interpolation date in seconds
        final double interplationDateSec = date.durationFrom(this.refDate);

        // main algorithm
        final int posMin = new BinarySearchIndexOpenClosed(this.durations).getIndex(interplationDateSec);

        // determine beginning of array
        int debTab = posMin - (int) MathLib.ceil(this.interpOrder / 2.) + 1;
        // modification of debTab if
        // - we are constrained by beginning of array
        debTab = (debTab < 0) ? 0 : debTab;
        // - we are constrained by beginning of array
        debTab = (debTab + this.interpOrder > sizeAttitude) ? sizeAttitude - this.interpOrder : debTab;

        // fill attitudes to Hermite
        for (int i = 0; i < this.interpOrder; i++) {
            final Vector3D acc = this.spinDerivativesComputation ? this.attitudes[debTab + i].getRotationAcceleration()
                : null;
            attitudesToHermite.add(new TimeStampedAngularCoordinates(
                this.refDate.shiftedBy(this.durations[debTab + i]), this.attitudes[debTab + i].getRotation(),
                this.attitudes[debTab + i].getRotationRate(), acc));
        }

        // Call Hermite interpolation
        final Attitude interpolatedAttitude = new Attitude(this.refFrame, TimeStampedAngularCoordinates.interpolate(
            date, this.filter, attitudesToHermite, this.spinDerivativesComputation));

        // Transformations of the attitude in the interpolation frame given in input
        return interpolatedAttitude.withReferenceFrame(frame, this.spinDerivativesComputation);
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
    public RelativeTabulatedAttitudeLeg copy(final AbsoluteDateInterval newIntervalOfValidity) {

        // test of the date interval : must be contained in the one of this object
        if (!this.validityInterval.includes(newIntervalOfValidity)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Build new instance
        final List<Pair<Double, AngularCoordinates>> newList = new ArrayList<Pair<Double, AngularCoordinates>>();
        for (int i = 0; i < this.attitudes.length; i++) {
            newList.add(new Pair<Double, AngularCoordinates>(this.durations[i], this.attitudes[i]));
        }
        try {
            final boolean useR = this.filter == AngularDerivativesFilter.USE_RR ? true : false;
            final RelativeTabulatedAttitudeLeg res = new RelativeTabulatedAttitudeLeg(this.refDate, newList,
                this.refFrame, useR, this.interpOrder, this.nature);
            res.setSpinDerivativesComputation(this.spinDerivativesComputation);
            res.validityInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
                newIntervalOfValidity.getLowerData(), newIntervalOfValidity.getUpperData(),
                IntervalEndpointType.CLOSED);

            // Return result
            return res;
        } catch (final PatriusException e) {
            // Cannot happen since leg has already been created once before copy
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }
}
