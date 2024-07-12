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
 * HISTORY
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.math.analysis.polynomials.UnivariateDateFunction;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.legs.Leg;

/**
 * Interface that must be implemented by an orientation angle provider which is also a leg, i.e.
 * that has an interval of validity and a nature.
 *
 * @author morerem
 *
 * @since 4.2
 */
public interface OrientationAngleLeg extends OrientationAngleProvider, Leg {

    /**
     * {@inheritDoc} Time interval boundaries are properly taken into account.
     */
    @Override
    default double computeSpinByFD(final PVCoordinatesProvider pvProv,
                                   final AbsoluteDate date,
                                   final double computationStep) throws PatriusException {

        // Verification of the left and right bound
        final double orientation1;
        final double orientation2;
        double step = computationStep;
        final AbsoluteDateInterval interval = getTimeInterval();

        // Date at date - dt, date + dt
        final AbsoluteDate dateMinus = date.shiftedBy(-step);
        final AbsoluteDate datePlus = date.shiftedBy(step);

        if (interval.contains(datePlus)) {
            // For normal dates and first date - 1st order forward difference
            orientation1 = getOrientationAngle(pvProv, date);
            orientation2 = getOrientationAngle(pvProv, datePlus);
        } else if (interval.contains(dateMinus)) {
            // For last date - 1st order backward difference
            orientation1 = getOrientationAngle(pvProv, dateMinus);
            orientation2 = getOrientationAngle(pvProv, date);
        } else {
            // Specific case: time interval smaller than FD step
            step = interval.getDuration();
            orientation1 = getOrientationAngle(pvProv, interval.getLowerData());
            orientation2 = getOrientationAngle(pvProv, interval.getUpperData());
        }

        // Compute derivative by finite differences
        return OrientationAngleProvider.computeSpinNumerical(orientation1, orientation2, step);
    }

    /**
     * {@inheritDoc} Time interval boundaries are properly taken into account.
     */
    @Override
    default double computeSpinDerivativeByFD(final PVCoordinatesProvider pvProv,
                                             final AbsoluteDate date,
                                             final double computationStep) throws PatriusException {

        // Verification of the left and right bound
        final double spin1;
        final double spin2;
        double step = computationStep;
        final AbsoluteDateInterval interval = getTimeInterval();

        // Date at date - dt, date + dt
        final AbsoluteDate dateMinus = date.shiftedBy(-step);
        final AbsoluteDate dateMinus2 = date.shiftedBy(-step * 2.);
        final AbsoluteDate datePlus = date.shiftedBy(step);

        if (interval.contains(datePlus)) {
            // For normal dates and first date - 1st order forward difference
            spin1 = computeSpinByFD(pvProv, date, step);
            spin2 = computeSpinByFD(pvProv, datePlus, step);
        } else if (interval.contains(dateMinus2)) {
            // For last date - 1st order backward difference
            // Specific case, we want backward scheme to be used for both boundaries
            // Calling computeSpinByFD on dateMinus would use a forward scheme
            final double orientation1 = getOrientationAngle(pvProv, dateMinus2);
            final double orientation2 = getOrientationAngle(pvProv, dateMinus);
            spin1 = OrientationAngleProvider.computeSpinNumerical(orientation1, orientation2, step);
            spin2 = computeSpinByFD(pvProv, date, step);
        } else {
            // Specific case: time interval smaller than FD step
            // Step is set to half interval duration to avoid cancelling effect between lower bound forward difference
            // and upper bound backward differene
            step = interval.getDuration() / 2.;
            spin1 = computeSpinByFD(pvProv, interval.getLowerData(), step);
            spin2 = computeSpinByFD(pvProv, interval.getUpperData(), step);
        }

        // Compute derivative by finite differences
        return (spin2 - spin1) / step;
    }

    /** {@inheritDoc} */
    @Override
    OrientationAngleLeg copy(final AbsoluteDateInterval newInterval);

    /**
     * Build an {@link OrientationAngleLeg} from this.
     * 
     * @param function
     *        input function
     * @param timeInterval
     *        leg's validity interval
     * @param nature
     *        leg nature
     * @return an orientation angle leg
     */
    static OrientationAngleLeg build(final UnivariateDateFunction function, final AbsoluteDateInterval timeInterval,
                                     final String nature) {
        return new OrientationAngleLeg(){

            /** Serializable UID. */
            private static final long serialVersionUID = 8654192767935195998L;

            /** {@inheritDoc} */
            @Override
            public AbsoluteDateInterval getTimeInterval() {
                return timeInterval;
            }

            /**
             * {@inheritDoc}
             *
             * The parameter <i>pvProv</i> is not used by the method.
             */
            @Override
            public Double getOrientationAngle(final PVCoordinatesProvider pvProv, final AbsoluteDate date) {
                return function.value(date);
            }

            /** {@inheritDoc} */
            @Override
            public OrientationAngleLeg copy(final AbsoluteDateInterval newInterval) {
                return build(function, newInterval, nature);
            }

            /** {@inheritDoc} */
            @Override
            public String getNature() {
                return nature;
            }
        };
    }
}
