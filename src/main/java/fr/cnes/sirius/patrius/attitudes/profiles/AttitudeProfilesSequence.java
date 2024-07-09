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
 * @history created 28/02/12
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1950:14/11/2018:new attitude profile design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.Iterator;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles a sequence of {@link AttitudeProfile}.
 * 
 * @author delaygni
 *
 * @since 4.2
 */
public final class AttitudeProfilesSequence extends StrictAttitudeLegsSequence<AttitudeProfile> {

    /** Serial UID. */
    private static final long serialVersionUID = -1887681878952009836L;

    /** Nature. */
    private static final String DEFAULT_NATURE = "ATTITUDE_PROFILE_SEQUENCE";

    /** Nature. */
    private final String nature;

    /**
     * Constructor.
     * 
     * @param nature nature of the sequence
     */
    public AttitudeProfilesSequence(final String nature) {
        super();
        this.nature = nature;
    }

    /**
     * Constructor with default nature.
     */
    public AttitudeProfilesSequence() {
        this(DEFAULT_NATURE);
    }

    /**
     * Gets the attitude from the sequence.<br>
     * The {@link AttitudeProfile} matching the date is called to compute the attitude. If none is
     * found, an {@link PatriusException} is thrown. If two laws are juxtaposed at the given date,
     * the SECOND law is the valid one.
     *
     * @param pvProv spacecraft's position and velocity coordinates provider
     * @param date the date for which the attitude is computed
     * @param frame the frame for which the attitude is computed
     * @return the attitude
     * @throws PatriusException thrown if the date is out of the sequence's range
     */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final AttitudeProfile leg = this.current(date);
        if (leg == null) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_LEGS_SEQUENCE_INTERVAL, date, getTimeInterval());
        }
        return leg.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        super.setSpinDerivativesComputation(computeSpinDerivatives);
        // Change spin derivative flag of all legs
        final Iterator<AttitudeProfile> iterator = this.iterator();
        while (iterator.hasNext()) {
            iterator.next().setSpinDerivativesComputation(computeSpinDerivatives);
        }
    }

    /**
     * Returns the nature.
     * @return the nature
     */
    public String getNature() {
        return nature;
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
