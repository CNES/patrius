/**
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
 * HISTORY
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
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
import fr.cnes.sirius.patrius.utils.legs.StrictLegsSequence;

/**
 * This class handles a sequence of one or several {@link OrientationAngleProfile}. This sequence
 * can be handled as an {@link StrictLegsSequence} of {@link OrientationAngleProfile}.
 *
 * @author delaygni
 *
 * @since 4.2
 */
public class OrientationAngleProfileSequence extends StrictLegsSequence<OrientationAngleProfile> implements
        OrientationAngleProfile {

    /** Serial ID. */
    private static final long serialVersionUID = -2385389105822101864L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ORIENTATION_ANGLE_PROFILE_SEQUENCE";

    /** Nature. */
    private final String nature;

    /**
     * Constructor.
     * 
     * @param nature nature of the sequence
     */
    public OrientationAngleProfileSequence(final String nature) {
        super();
        this.nature = nature;
    }

    /**
     * Constructor with default value for the profiles sequence nature.
     */
    public OrientationAngleProfileSequence() {
        this(DEFAULT_NATURE);
    }

    /** {@inheritDoc} */
    @Override
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date) throws PatriusException {
        final OrientationAngleProfile leg = this.current(date);
        if (leg == null) {
            return null;
        }
        return leg.getOrientationAngle(pvProv, date);
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return nature;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence copy(final AbsoluteDateInterval newInterval) {
        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Specific behavior: we don't want each leg to be necessarily included in global validity interval
        final OrientationAngleProfileSequence res = new OrientationAngleProfileSequence(getNature());
        for (final OrientationAngleProfile currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval().getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                res.add((OrientationAngleProfile) currentL.copy(intersection));
            }
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UselessOverridingMethod")
    // Reason: false positive, call to super is necessary otherwise ambiguous
    public String toPrettyString() {
        return super.toPrettyString();
    }

    /**
     * {@inheritDoc}
     * Sequence is supposed to be continuous.
     */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        if (isEmpty()) {
            return null;
        } else  {
            return new AbsoluteDateInterval(first().getDate(), last().getEnd());
        }
    }
}