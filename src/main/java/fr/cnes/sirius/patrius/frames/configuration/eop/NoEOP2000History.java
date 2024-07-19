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
 *
 * @history creation 28/11/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.util.Iterator;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * NoEOP2000History.
 */
public final class NoEOP2000History extends EOP2000History {

    /** Serializable UID. */
    private static final long serialVersionUID = -7542790039542590139L;

    /**
     * NoEOP2000History.
     */
    public NoEOP2000History() {
        super(EOPInterpolators.LINEAR);
    }

    /**
     * Iterator.
     * 
     * @return null
     */
    @Override
    public Iterator<TimeStamped> iterator() {
        return new Iterator<TimeStamped>(){

            /** {@inheritDoc} */
            @Override
            public boolean hasNext() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public TimeStamped next() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void remove() {
                // Nothing to do
            }
        };
    }

    /**
     * size.
     * 
     * @return 0
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * getStartDate.
     * 
     * @return PAST_INFINITY
     */
    @Override
    public AbsoluteDate getStartDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /**
     * getEndDate.
     * 
     * @return FUTURE_INFINITY
     */
    @Override
    public AbsoluteDate getEndDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /**
     * getUT1MinusTAI.
     * 
     * @param date
     *        date
     * @return 0
     */
    @Override
    public double getUT1MinusTAI(final AbsoluteDate date) {
        try {
            return TimeScalesFactory.getUTC().offsetFromTAI(date);
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * getLOD.
     * 
     * @param date
     *        date
     * @return 0
     */
    @Override
    public double getLOD(final AbsoluteDate date) {
        return 0;
    }

    /**
     * EOPInterpolators.
     * 
     * @return 0
     */
    @Override
    public EOPInterpolators getEOPInterpolationMethod() {
        return EOPInterpolators.LINEAR;
    }

    /** {@inheritDoc} */
    @Override
    public NutationCorrection getNutationCorrection(final AbsoluteDate date) {
        return NutationCorrection.NULL_CORRECTION;
    }

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        return PoleCorrection.NULL_CORRECTION;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isActive() {
        return false;
    }
}
