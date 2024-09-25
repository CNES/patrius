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
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class aims at creation an orientation angle leg whose orientation angle is constant in its
 * interval of validity.
 *
 * @author delaygni
 *
 * @since 4.2
 */
public class ConstantOrientationAngleLeg extends AbstractOrientationAngleLeg {

    /** Serial ID. */
    private static final long serialVersionUID = -2714288845790459809L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "CONSTANT";

    /** Angle value. */
    private final double angle;

    /**
     * Constructor with default nature.
     * 
     * @param interval time interval of validity
     * @param angleIn angle value
     */
    public ConstantOrientationAngleLeg(final AbsoluteDateInterval interval,
            final double angleIn) {
        this(interval, angleIn, DEFAULT_NATURE);
    }

    /**
     * Constructor.
     * 
     * @param interval time interval of validity
     * @param angleIn angle value
     * @param nature nature
     */
    public ConstantOrientationAngleLeg(final AbsoluteDateInterval interval,
            final double angleIn,
            final String nature) {
        super(interval, nature);
        this.angle = angleIn;
    }

    /** {@inheritDoc} */
    @Override
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date) throws PatriusException {
        if (this.getTimeInterval().contains(date)) {
            return this.angle;
        } else {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_LEGS_SEQUENCE_INTERVAL, date, getTimeInterval());
        }
    }

    /** {@inheritDoc}
     * <p>Provided interval does not have to be included in current time interval.</p>
     */
    @Override
    public ConstantOrientationAngleLeg copy(final AbsoluteDateInterval newInterval) {
        return new ConstantOrientationAngleLeg(newInterval, angle, getNature());
    }
}
