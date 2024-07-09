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
 * @history creation 28/06/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class contains the different ut1-utc corrections (libration, tidal effects).
 * 
 * @concurrency not thread safe because its attributes are not guaranteed to be thread safe.
 * 
 * @author Julie Anton, Rami Houdroge
 * 
 * @version $Id: DiurnalRotation.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public class DiurnalRotation implements Serializable {

    /** IUD. */
    private static final long serialVersionUID = -3155586485425973676L;
    /** Correction due to tidal effects. */
    private final TidalCorrectionModel tides;
    /** Correction due to libration. */
    private final LibrationCorrectionModel libration;

    /**
     * Simple constructor.
     * 
     * @param tidesCorrection
     *        correction due to tidal effects
     * @param librationCorrection
     *        correction due to libration
     */
    public DiurnalRotation(final TidalCorrectionModel tidesCorrection,
        final LibrationCorrectionModel librationCorrection) {
        this.tides = tidesCorrection;
        this.libration = librationCorrection;
    }

    /**
     * Compute ut1-tai correction.
     * 
     * @param date
     *        date for which one we want to compute the correction
     * @return ut1-tai correction as a double
     */
    public double getUT1Correction(final AbsoluteDate date) {
        return this.tides.getUT1Correction(date) + this.libration.getUT1Correction(date);
    }

    /**
     * @return the tidal model
     */
    public TidalCorrectionModel getTidalCorrectionModel() {
        return this.tides;
    }

    /**
     * @return the libration model
     */
    public LibrationCorrectionModel getLibrationCorrectionModel() {
        return this.libration;
    }
}
