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
 * @history creation 02/04/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:303:02/04/2015: addition of constant outside history EOP
 * VERSION::FA:831:25/01/2017: computation times optimization for calls outside interval)
 * VERSION::FA:981:01/09/2017: correction for getUT1MinusUTC (outside interval)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * This class extends the EOP data outside of the historic definition interval. Outside of this interval the value
 * corresponding to the closest bound will be returned.
 * 
 * <p>
 * Warning: as the UT1-TAI remains constant the (UT1-UTC) absolute value may become higher than 0.9 second depending on
 * leap seconds (for UTC time scale) existing outside this interval.
 * </p>
 * 
 * @author fiorentinoa
 * @since version 3.0
 */
public class EOP2000HistoryConstantOutsideInterval extends EOP2000History {

    /** Serializable UID. */
    private static final long serialVersionUID = 4948633092701598354L;

    /**
     * Constructor.
     * 
     * @param interpMethod
     *        EOP interpolation method
     */
    public EOP2000HistoryConstantOutsideInterval(final EOPInterpolators interpMethod) {
        super(interpMethod);
    }

    @Override
    /** {@inheritDoc} */
    public AbsoluteDate getStartDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    @Override
    /** {@inheritDoc} */
    public AbsoluteDate getEndDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /**
     * {@inheritDoc}
     * 
     * @return UT1-TAI in seconds if date is within history interval bounds. If date is outside history interval bounds
     *         value corresponding to closest bound is returned
     */
    @Override
    public double getUT1MinusTAI(final AbsoluteDate date) {

        if (this.isEmpty()) {
            // NO EOP
            try {
                return TimeScalesFactory.getUTC().offsetFromTAI(date);
            } catch (final PatriusException e) {
                throw new PatriusExceptionWrapper(e);
            }
        } else {
            final EOPEntry bound = this.getBound(date);
            return bound == null ? super.getUT1MinusTAI(date) : bound.getUT1MinusTAI();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @return UT1-UTC in seconds if date is within history interval bounds. If date is outside history interval bounds
     *         value corresponding to closest bound is returned. As the UT1-TAI remains constant the (UT1-UTC) absolute
     *         value may
     *         become higher than 0.9 second depending on leap seconds (for UTC time scale) existing outside this
     *         interval.
     */
    @Override
    public double getUT1MinusUTC(final AbsoluteDate date) {

        if (this.isEmpty()) {
            // NO EOP
            return 0;
        } else {
            try {
                final EOPEntry bound = this.getBound(date);
                return bound == null ? super.getUT1MinusTAI(date) - TimeScalesFactory.getUTC().offsetFromTAI(date) :
                    bound.getUT1MinusTAI() - TimeScalesFactory.getUTC().offsetFromTAI(date);
            } catch (final PatriusException e) {
                throw new PatriusExceptionWrapper(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @return LoD in seconds if date is within history interval bounds. If date is outside history interval bounds
     *         value corresponding to closest bound is returned
     */
    @Override
    public double getLOD(final AbsoluteDate date) {

        if (this.isEmpty()) {
            // NO EOP
            return 0;
        } else {
            final EOPEntry bound = this.getBound(date);
            return bound == null ? super.getLOD(date) : bound.getLOD();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @return pole correction to be applied if date is within history interval bounds. If date is outside history
     *         interval bounds value corresponding to closest bound is returned
     */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) throws TimeStampedCacheException {

        if (this.isEmpty()) {
            // NO EOP
            return PoleCorrection.NULL_CORRECTION;
        } else {
            final EOPEntry bound = this.getBound(date);
            return bound == null ? super.getPoleCorrection(date) : new PoleCorrection(bound.getX(), bound.getY());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @return nutation correction to be applied if date is within history interval bounds. If date is outside history
     *         interval bounds value corresponding to closest bound is returned
     */
    @Override
    public NutationCorrection getNutationCorrection(final AbsoluteDate date) {

        if (this.isEmpty()) {
            // NO EOP
            return NutationCorrection.NULL_CORRECTION;
        } else {
            final EOPEntry bound = this.getBound(date);
            return bound == null ? super.getNutationCorrection(date) : new NutationCorrection(bound.getDX(),
                bound.getDY());
        }
    }

    /**
     * Get lowest bound if date is before lowest bound, uppest bound if date is after highest bound, null otherwise.
     * 
     * @param date
     *        date
     * @return lowest bound if date is before lowest bound, uppest bound if date is after highest bound, null otherwise
     */
    private EOPEntry getBound(final AbsoluteDate date) {
        final EOPEntry last = this.getLast();
        final EOPEntry first = this.getFirst();
        EOPEntry res = null;
        if (date.compareTo(last.getDate()) >= 0) {
            // Upper bound
            res = last;
        } else if (date.compareTo(first.getDate()) <= 0) {
            // Lower bound
            res = first;
        }
        return res;
    }
}
