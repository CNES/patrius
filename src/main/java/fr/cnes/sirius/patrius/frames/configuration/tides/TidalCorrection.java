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
 *
 * @history creation 12/10/2012
 *
 * HISTORY
* VERSION:4.8:FA:FA-2964:15/11/2021:[PATRIUS] Javadoc incoherente pour TidalCorrection (UT1 correction) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * This class represents a Pole, UT1-TAI and length of day correction set for a given date.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: TidalCorrection.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public final class TidalCorrection implements TimeStamped, Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -5435272662230566277L;

    /** t<sub>p</sub> parameter. */
    private final double ut1Correction;

    /** Pole correction. */
    private final PoleCorrection pole;

    /** Length of day correction. */
    private final double lod;

    /** Date. */
    private final AbsoluteDate date;

    /**
     * @param dateIn
     *        the date of the coordinates set
     * @param poleIn
     *        the coordinates correction data
     * @param ut1Corr
     *        the UT1-TAI correction (seconds)
     * @param lodCor
     *        length of day correction (seconds)
     */
    public TidalCorrection(final AbsoluteDate dateIn, final PoleCorrection poleIn,
        final double ut1Corr, final double lodCor) {
        this.date = dateIn;
        this.pole = poleIn;
        this.ut1Correction = ut1Corr;
        this.lod = lodCor;
    }

    /**
     * @return the pole correction data
     */
    public PoleCorrection getPoleCorrection() {
        return this.pole;
    }

    /**
     * Returns the UT1-TAI correction.
     * @return the UT1-TAI correction (seconds)
     */
    public double getUT1Correction() {
        return this.ut1Correction;
    }

    /**
     * @return the date
     */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Get the length of day correction.
     * 
     * @return lod correction
     */
    public double getLODCorrection() {
        return this.lod;
    }
}
