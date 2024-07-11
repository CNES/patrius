/**
 * Copyright 2011-2021 CNES
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
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.legs.Leg;

/**
 * Interface for all <i>attitude legs</i>: {@link Leg} and {@link AttitudeProvider}.
 *
 * @see Leg
 * @see AttitudeProvider
 *
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
 * 
 * @since 4.7
 */
public interface AttitudeLeg extends Leg, AttitudeProvider {

    /** Default nature for attitude legs. */
    public static String LEG_NATURE = "ATTITUDE";

    /** {@inheritDoc}
     * Time interval boundaries are properly taken into account.
     */
    @Override
    default Vector3D computeSpinByFD(final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate date,
            final double computationStep) throws PatriusException {

        // Verification of the left and right bound
        final Attitude attitude1;
        final Attitude attitude2;
        double step = computationStep;
        final AbsoluteDateInterval interval = getTimeInterval();

        // Date at date - dt, date + dt, date - dt/2 and date + dt/2
        final AbsoluteDate dateMinus = date.shiftedBy(-step);
        final AbsoluteDate datePlus = date.shiftedBy(step);
        final AbsoluteDate dateMinus2 = date.shiftedBy(-step / 2.);
        final AbsoluteDate datePlus2 = date.shiftedBy(step / 2.);

        if (interval.contains(datePlus2) && interval.contains(dateMinus2)) {
            // For normal dates - 2nd order centered difference
            attitude1 = getAttitude(pvProv, dateMinus2, frame);
            attitude2 = getAttitude(pvProv, datePlus2, frame);
        } else if (interval.contains(datePlus)) {
            // For first date - 1st order forward difference
            attitude1 = getAttitude(pvProv, date, frame);
            attitude2 = getAttitude(pvProv, datePlus, frame);
        } else if (interval.contains(dateMinus)) {
            // For last date - - 1st order backward difference
            attitude1 = getAttitude(pvProv, dateMinus, frame);
            attitude2 = getAttitude(pvProv, date, frame);
        } else {
            // Specific case: time interval smaller than FD step
            step = interval.getDuration();
            attitude1 = getAttitude(pvProv, interval.getLowerData(), frame);
            attitude2 = getAttitude(pvProv, interval.getUpperData(), frame);
        }

        // Compute derivative by finite differences
        return AngularCoordinates.estimateRate(attitude1.getRotation(), attitude2.getRotation(), step);
    }

    /** {@inheritDoc}
     * Time interval boundaries are properly taken into account.
     */
    @Override
    default Vector3D computeSpinDerivativeByFD(final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate date,
            final double computationStep) throws PatriusException {

        // Verification of the left and right bound
        final Attitude attitude1;
        final Attitude attitude2;
        double step = computationStep;
        final AbsoluteDateInterval interval = getTimeInterval();

        // Date at date - dt, date + dt, date - dt/2 and date + dt/2
        final AbsoluteDate dateMinus = date.shiftedBy(-step);
        final AbsoluteDate datePlus = date.shiftedBy(step);
        final AbsoluteDate dateMinus2 = date.shiftedBy(-step / 2.);
        final AbsoluteDate datePlus2 = date.shiftedBy(step / 2.);

        if (interval.contains(datePlus2) && interval.contains(dateMinus2)) {
            // For normal dates - 2nd order centered difference
            attitude1 = getAttitude(pvProv, dateMinus2, frame);
            attitude2 = getAttitude(pvProv, datePlus2, frame);
        } else if (interval.contains(datePlus)) {
            // For first date - 1st order forward difference
            attitude1 = getAttitude(pvProv, date, frame);
            attitude2 = getAttitude(pvProv, datePlus, frame);
        } else if (interval.contains(dateMinus)) {
            // For last date - - 1st order backward difference
            attitude1 = getAttitude(pvProv, dateMinus, frame);
            attitude2 = getAttitude(pvProv, date, frame);
        } else {
            // Specific case: time interval smaller than FD step
            step = interval.getDuration();
            attitude1 = getAttitude(pvProv, interval.getLowerData(), frame);
            attitude2 = getAttitude(pvProv, interval.getUpperData(), frame);
        }

        // Compute derivative by finite differences
        return attitude2.getSpin().subtract(attitude1.getSpin()).scalarMultiply(1 / step);
    }

    /** {@inheritDoc} */
    @Override
    AttitudeLeg copy(final AbsoluteDateInterval newInterval);
}
