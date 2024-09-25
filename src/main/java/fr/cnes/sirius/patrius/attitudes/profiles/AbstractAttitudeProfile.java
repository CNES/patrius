/**
 *
 * Copyright 2011-2022 CNES
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
 * @history Created 20/03/2013
  * VERSION::DM:403:20/10/2015:Improving ergonomics
  * VERSION::DM:1950:14/11/2018:new attitude profile design
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2656:27/01/2021:[PATRIUS] delTa parametrable utilise pour le calcul de vitesse dans
 * QuaternionPolynomialProfile
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class provides implementations for classes implementing {@link AttitudeProfile}.
 * 
 * @author delaygni
 *
 * @since 4.2
 */
public abstract class AbstractAttitudeProfile implements AttitudeProfile {

     /** Serializable UID. */
    private static final long serialVersionUID = 1774730921131738218L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ATTITUDE_PROFILE";

    /** Default delta-t used for spin computation by finite differences. */
    private static final double DEFAULT_SPIN_DELTAT = 0.2;

    /** Interval of validity. */
    private final AbsoluteDateInterval interval;

    /** Nature. */
    private final String nature;

    /** Delta-t used for spin computation by finite differences. */
    private final double spinDeltaT;

    /**
     * Constructor with a default value for its spin delta-t.
     * 
     * @param timeInterval interval of profile
     * @param natureIn the nature of the slew
     */
    public AbstractAttitudeProfile(final AbsoluteDateInterval timeInterval, final String natureIn) {
        this(timeInterval, natureIn, DEFAULT_SPIN_DELTAT);
    }

    /**
     * Constructor with a default value for its nature and spin delta-t.
     * 
     * @param timeInterval interval of validity
     */
    public AbstractAttitudeProfile(final AbsoluteDateInterval timeInterval) {
        this(timeInterval, DEFAULT_NATURE);
    }

    /**
     * Constructor.
     * 
     * @param timeInterval interval of profile
     * @param natureIn the nature of the slew
     * @param spinDeltaT delta-t used for spin computation by finite differences
     */
    public AbstractAttitudeProfile(final AbsoluteDateInterval timeInterval,
            final String natureIn,
            final double spinDeltaT) {
        this.interval = timeInterval;
        this.nature = natureIn;
        this.spinDeltaT = spinDeltaT;
    }

    /**
     * Constructor with a default value for its nature.
     * 
     * @param timeInterval interval of validity
     * @param spinDeltaT delta-t used for spin computation by finite differences
     */
    public AbstractAttitudeProfile(final AbsoluteDateInterval timeInterval, final double spinDeltaT) {
        this(timeInterval, DEFAULT_NATURE, spinDeltaT);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        return this.interval;
    }

    /**
     * Check date validity.
     * 
     * @param userDate date
     * @throws PatriusException thrown if the user date is outside the interval
     */
    public void checkDate(final AbsoluteDate userDate) throws PatriusException {
        if (!this.getTimeInterval().contains(userDate)) {
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return this.nature;
    }
    
    /**
     * Returns the delta-t used for spin computation by finite differences.
     * @return the delta-t used for spin computation by finite differences
     */
    public double getSpinDeltaT() {
        return this.spinDeltaT;
    }
}
