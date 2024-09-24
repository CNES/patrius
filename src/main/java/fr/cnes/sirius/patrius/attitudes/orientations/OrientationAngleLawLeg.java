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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2847:18/05/2021:Modification de la gestion de la date hors intervalle
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents an {@link OrientationAngleLaw} on which an interval of validity is defined
 * (whose borders are closed points).
 *
 * @author delaygni
 *
 * @since 4.2
 */
public class OrientationAngleLawLeg extends AbstractOrientationAngleLeg {

    /** Serial ID. */
    private static final long serialVersionUID = 6004659654066056426L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ATTITUDE_ORIENTATION_ANGLE_LAW_LEG";

    /** Orientation angle law. */
    private final OrientationAngleLaw orientationAngleLaw;

    /** True if leg can be used outside its validity interval. */
    private boolean timeTolerant;

    /**
     * Constructor
     * 
     * @param orientationAngleLawIn provider of the AttitudeLaw
     * @param initialDate start date of the interval of validity
     * @param finalDate end date of the interval of validity
     */
    public OrientationAngleLawLeg(final OrientationAngleLaw orientationAngleLawIn,
            final AbsoluteDate initialDate,
            final AbsoluteDate finalDate) {
        this(orientationAngleLawIn, initialDate, finalDate, DEFAULT_NATURE);
    }

    /**
     * Constructor
     * 
     * @param orientationAngleLawIn provider of the AttitudeLaw
     * @param initialDate start date of the interval of validity
     * @param finalDate end date of the interval of validity
     * @param natureIn leg nature
     */
    public OrientationAngleLawLeg(final OrientationAngleLaw orientationAngleLawIn,
            final AbsoluteDate initialDate,
            final AbsoluteDate finalDate,
            final String natureIn) {
        this(orientationAngleLawIn, initialDate, finalDate, natureIn, false);
    }

    /**
     * Constructor
     * 
     * @param orientationAngleLawIn provider of the AttitudeLaw
     * @param initialDate start date of the interval of validity
     * @param finalDate end date of the interval of validity
     * @param natureIn leg nature
     * @param timeTolerant true if leg can be used outside its validity interval, false otherwise
     */
    public OrientationAngleLawLeg(final OrientationAngleLaw orientationAngleLawIn,
            final AbsoluteDate initialDate,
            final AbsoluteDate finalDate,
            final String natureIn,
            final boolean timeTolerant) {
        super(
                new AbsoluteDateInterval(IntervalEndpointType.CLOSED, initialDate, finalDate,
                        IntervalEndpointType.CLOSED), natureIn);
        this.orientationAngleLaw = orientationAngleLawIn;
        this.timeTolerant = timeTolerant;
    }

    /** {@inheritDoc} */
    @Override
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date) throws PatriusException {
        if (timeTolerant || this.getTimeInterval().contains(date)) {
            return this.orientationAngleLaw.getOrientationAngle(pvProv, date);
        } else {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_LEGS_SEQUENCE_INTERVAL, date, getTimeInterval());
        }
    }

    /** {@inheritDoc}
     * <p>Provided interval does not have to be included in current time interval.</p>
     */
    @Override
    public OrientationAngleLawLeg copy(final AbsoluteDateInterval newInterval) {
        return new OrientationAngleLawLeg(orientationAngleLaw, newInterval.getLowerData(), newInterval.getUpperData());
    }
}
