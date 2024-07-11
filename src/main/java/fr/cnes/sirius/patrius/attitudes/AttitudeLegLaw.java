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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:392:10/11/2015:Creation of the class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents an attitude law version "attitudeLeg", with an interval of validity (whose borders are closed
 * points) and attitude laws outside this interval of validity.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of an AttitudeLegLaw makes it thread-safe only if
 *                      the two AttitudeLaw and the AttitudeLeg are.
 * 
 * @author galpint
 * 
 * @version $Id: AttitudeLegLaw.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 */
public class AttitudeLegLaw implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 1120527407708346861L;

    /** Law before interval of validity . */
    private final AttitudeLaw lawBefore;

    /** Law during interval of validity . */
    private final AttitudeLeg leg;

    /** Law after interval of validity . */
    private final AttitudeLaw lawAfter;

    /**
     * Build an attitude law version "attitudeLeg". Its interval of validity has closed endpoints.
     * If the interpolation date is contained in the interval of validity of leg,
     * the interpolated attitude is returned according to this law.
     * If the interpolation date is before (resp. after) the interval of validity of leg,
     * the returned interpolated attitude is computed following lawBefore (reps. lawAfter)
     * 
     * @param attitudelawBefore
     *        : law before interval of validity
     * @param attitudeleg
     *        : law during interval of validity
     * @param attitudelawAfter
     *        : Law after interval of validity
     */
    public AttitudeLegLaw(final AttitudeLaw attitudelawBefore, final AttitudeLeg attitudeleg,
        final AttitudeLaw attitudelawAfter) {
        this.lawBefore = attitudelawBefore;
        this.leg = attitudeleg;
        this.lawAfter = attitudelawAfter;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        // Attitude to return
        final Attitude res;
        final AbsoluteDateInterval interval = this.leg.getTimeInterval();
        if (interval.contains(date)) {
            res = this.leg.getAttitude(pvProv, date, frame);
        } else if (date.compareTo(interval.getLowerData()) < 0) {
            res = this.lawBefore.getAttitude(pvProv, date, frame);
        } else {
            res = this.lawAfter.getAttitude(pvProv, date, frame);
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivatives computation applies to {@link #lawBefore}, {@link #leg} and {@link #lawAfter}.
     * </p>
     */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.lawBefore.setSpinDerivativesComputation(computeSpinDerivatives);
        this.leg.setSpinDerivativesComputation(computeSpinDerivatives);
        this.lawAfter.setSpinDerivativesComputation(computeSpinDerivatives);
    }
}
