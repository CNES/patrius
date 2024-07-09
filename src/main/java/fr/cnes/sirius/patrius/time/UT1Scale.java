/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Removed dependency to UTC-TAI
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::FA:1465:26/04/2018:multi-thread environment optimisation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;

/**
 * Universal Time 1.
 * <p>
 * UT1 is a time scale directly linked to the actual rotation of the Earth. It is an irregular scale, reflecting Earth
 * irregular rotation rate. The offset between UT1 and {@link UTCScale UTC} is found in the Earth Orientation Parameters
 * published by IERS.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @since 5.1
 */
public class UT1Scale implements TimeScale {

    /**
     * Package private constructor for the factory.
     */
    UT1Scale() {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return FramesFactory.getConfiguration().getUT1MinusTAI(date);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date,
                              final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < 3; i++) {
            offset = -this.offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "UT1";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Package-private getter for the EOPHistory object.
     * 
     * @return current history object.
     */
    public EOPHistory getHistory() {
        return FramesFactory.getConfiguration().getEOPHistory();
    }

}
