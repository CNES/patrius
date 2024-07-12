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
 * @history creation 01/11/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:392:10/11/2015:Creation of the class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a relative tabulated attitude law version "attitudeLeg", with an interval of validity
 * (whose borders are closed points) and attitude laws outside this interval of validity, which can be of two
 * types : a {@link ConstantAttitudeLaw}, or an ExtrapolatedAttitudeLaw (private class)
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of an RelativeTabulatedAttitudeLaw makes it thread-safe only if
 *                      the AttitudeLeglaw is
 * 
 * @author galpint
 * 
 * @version $Id: RelativeTabulatedAttitudeLaw.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 */
public class RelativeTabulatedAttitudeLaw implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -5640183467396778053L;
    /** Attitude Leg Law . */
    private final AttitudeLegLaw attitudeLegLaw;

    /**
     * Enumeration of the existing constraint types
     */
    public enum AroundAttitudeType {
        /** Constant attitude law */
        CONSTANT_ATT,
        /** Extrapolated attitude law */
        EXTRAPOLATED_ATT
    }

    /**
     * Create a RelativeTabulatedAttitudeLaw object with list of rotations (during the interval of validity),
     * a law before the interval and a law after the interval.
     * 
     * @param refDate
     *        reference date
     * @param orientations
     *        List of rotations
     * @param frame
     *        reference frame
     * @param lawBefore
     *        Attitude law used before the reference date
     * @param lawAfter
     *        Attitude law used after the date corresponding to the last attitude
     * @throws PatriusException
     *         if not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLaw(final Frame frame, final AbsoluteDate refDate,
        final List<Pair<Double, Rotation>> orientations,
        final RelativeTabulatedAttitudeLaw.AroundAttitudeType lawBefore,
        final RelativeTabulatedAttitudeLaw.AroundAttitudeType lawAfter) throws PatriusException {

        // relative tabulated attitude leg
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg = new RelativeTabulatedAttitudeLeg(refDate,
            orientations, frame);

        // build a list of angular coordinates with list of rotations and rotation rates set to 0.
        final List<Pair<Double, AngularCoordinates>> listAr = buildAngularCoordinatesMap(orientations);

        // Law before
        final AttitudeLaw attitudeLawBefore = buildAttitudeLaw(lawBefore, listAr.get(0), frame, refDate);

        // Law after
        final AttitudeLaw attitudeLawAfter =
            buildAttitudeLaw(lawAfter, listAr.get(listAr.size() - 1), frame, refDate);

        this.attitudeLegLaw = new AttitudeLegLaw(attitudeLawBefore, relativeTabulatedAttitudeLeg, attitudeLawAfter);
    }

    /**
     * Create a RelativeTabulatedAttitudeLaw object with list of Angular Coordinates (during the interval of validity),
     * a law before the interval and a law after the interval.
     * 
     * @param refDate
     *        reference date
     * @param angularCoordinates
     *        List of angular coordinates
     * @param frame
     *        reference frame
     * @param lawBefore
     *        Attitude law used before the reference date
     * @param lawAfter
     *        Attitude law used after the date corresponding to the last attitude
     * @throws PatriusException
     *         if not enough data for Hermite interpolation
     * 
     * @since 3.1
     */
    public RelativeTabulatedAttitudeLaw(final AbsoluteDate refDate,
        final List<Pair<Double, AngularCoordinates>> angularCoordinates,
        final Frame frame, final RelativeTabulatedAttitudeLaw.AroundAttitudeType lawBefore,
        final RelativeTabulatedAttitudeLaw.AroundAttitudeType lawAfter) throws PatriusException {

        // relative tabulated attitude leg
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg = new RelativeTabulatedAttitudeLeg(refDate,
            frame, angularCoordinates);

        // Law before
        final AttitudeLaw attitudeLawBefore = buildAttitudeLaw(lawBefore, angularCoordinates.get(0), frame, refDate);

        // Law after
        final AttitudeLaw attitudeLawAfter = buildAttitudeLaw(lawAfter,
            angularCoordinates.get(angularCoordinates.size() - 1), frame, refDate);

        this.attitudeLegLaw = new AttitudeLegLaw(attitudeLawBefore, relativeTabulatedAttitudeLeg, attitudeLawAfter);
    }

    /**
     * Build a list of angular coordinates with list of rotations and rotation rates set to 0.
     * 
     * @param orientations
     *        the list of rotations
     * @return the list of angular coordinates
     * @since 3.1
     */
    private static List<Pair<Double, AngularCoordinates>>
        buildAngularCoordinatesMap(final List<Pair<Double, Rotation>> orientations) {
        final List<Pair<Double, AngularCoordinates>> res = new ArrayList<>();
        for (int i = 0; i < orientations.size(); i++) {
            final AngularCoordinates ac = new AngularCoordinates(orientations.get(i).getSecond(), Vector3D.ZERO);
            res.add(new Pair<>(orientations.get(i).getFirst(), ac));
        }
        return res;
    }

    /**
     * Method to build boundary attitude law.
     * 
     * @param law
     *        type of attitude law
     * @param data
     *        angular coordinates at boundary
     * @param refDate
     *        the date
     * @param frame
     *        the frame
     * @return the boundary attitude law
     * @since 3.1
     */
    private static AttitudeLaw buildAttitudeLaw(final AroundAttitudeType law,
                                         final Pair<Double, AngularCoordinates> data,
                                         final Frame frame, final AbsoluteDate refDate) {
        AttitudeLaw attitudeLawAfter = null;
        if (law.equals(RelativeTabulatedAttitudeLaw.AroundAttitudeType.CONSTANT_ATT)) {
            attitudeLawAfter = new ConstantAttitudeLaw(frame, data.getSecond().getRotation());
        } else if (law.equals(RelativeTabulatedAttitudeLaw.AroundAttitudeType.EXTRAPOLATED_ATT)) {
            attitudeLawAfter = new ExtrapolatedAttitudeLaw(frame, new TimeStampedAngularCoordinates(
                refDate.shiftedBy(data.getFirst()),
                data.getSecond().getRotation(),
                data.getSecond().getRotationRate(),
                data.getSecond().getRotationAcceleration()));
        }
        return attitudeLawAfter;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        return this.attitudeLegLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.attitudeLegLaw.setSpinDerivativesComputation(computeSpinDerivatives);
    }

    /**
     * Local class to create interpolated attitude law
     */
    private static class ExtrapolatedAttitudeLaw implements AttitudeLaw {

        /** Serializable UID. */
        private static final long serialVersionUID = -4235197693614358395L;

        /** frame in which extrapolation must be performed */
        private final Frame propagationFrame;

        /** the time stamped angular coordinates to propagate */
        private final TimeStampedAngularCoordinates angularCoordinates;

        /** Flag to indicate if spin derivation computation is activated. */
        private boolean innerSpinDerivativesComputation = false;

        /**
         * Create an extrapolated attitude law with a frame a TimeStampedAngularCoordinates object.
         * 
         * @param frame
         *        frame in which extrapolation must be performed
         * @param ar
         *        the angular coordinates to propagate
         * 
         * @since 3.1
         */
        public ExtrapolatedAttitudeLaw(final Frame frame, final TimeStampedAngularCoordinates ar) {
            this.propagationFrame = frame;
            this.angularCoordinates = ar;
        }

        /** {@inheritDoc} */
        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                    final Frame frame) throws PatriusException {
            // propagate
            final TimeStampedAngularCoordinates tar =
                this.angularCoordinates.shiftedBy(date.durationFrom(this.angularCoordinates
                    .getDate()), this.innerSpinDerivativesComputation);

            // Propagated attitude
            final Attitude propagatedAttitude = new Attitude(this.propagationFrame, tar);

            // Transformations of the attitude in the interest frame given in input
            return propagatedAttitude.withReferenceFrame(frame, this.innerSpinDerivativesComputation);
        }

        /** {@inheritDoc} */
        @Override
        public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
            this.innerSpinDerivativesComputation = computeSpinDerivatives;
        }
    }
}
