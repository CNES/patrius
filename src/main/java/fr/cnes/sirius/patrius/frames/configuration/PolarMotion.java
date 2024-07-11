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
 * @history creation 28/06/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class contains the different polar motion corrections (libration, tidal effects, sp correction).
 * 
 * @concurrency not thread safe because its attributes are not guaranteed to be thread safe.
 * 
 * @author Julie Anton
 * 
 * @version $Id: PolarMotion.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public class PolarMotion implements Serializable {

    /** IUD. */
    private static final long serialVersionUID = -1495152425256550661L;
    /** Correction due to tidal effects. */
    private final TidalCorrectionModel tides;
    /** Correction due to libration. */
    private final LibrationCorrectionModel libration;
    /** S' correction. */
    private final SPrimeModel sp;
    /** Flag for use of EOP data. */
    private final boolean useEop;

    /**
     * Simple constructor.
     * 
     * @param useEopData
     *        true if EOP pole correction data is to be used, flase if not.
     * @param tidesCorrection
     *        corrections due to tidal effects
     * @param librationCorrection
     *        correction due to libration
     * @param spCorrection
     *        TIO (Terrestrial Intermediate Origin) position on the equator of the CIP (Celestial Intermediate
     *        Pole)
     */
    public PolarMotion(final boolean useEopData, final TidalCorrectionModel tidesCorrection,
        final LibrationCorrectionModel librationCorrection,
        final SPrimeModel spCorrection) {
        this.useEop = useEopData;
        this.tides = tidesCorrection;
        this.libration = librationCorrection;
        this.sp = spCorrection;
    }

    /**
     * Compute pole correction.
     * 
     * @param date
     *        date for which the pole correction is computed
     * @return pole correction as a {@link PoleCorrection}
     * @throws PatriusException
     *         when an Orekit error occurs
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) throws PatriusException {
        final PoleCorrection corrTides = this.tides.getPoleCorrection(date);
        final PoleCorrection corrLib = this.libration.getPoleCorrection(date);
        return new PoleCorrection(corrTides.getXp() + corrLib.getXp(), corrTides.getYp() + corrLib.getYp());
    }

    /**
     * Compute S'.
     * 
     * @param date
     *        date for which the s' quantity is computed
     * @return s' as a double
     */
    public double getSP(final AbsoluteDate date) {
        return this.sp.getSP(date);
    }

    /**
     * Use EOP pole correction data.
     * 
     * @return true if EOP data is to be used, flase if not.
     */
    public boolean useEopData() {
        return this.useEop;
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

    /**
     * @return the sp model
     */
    public SPrimeModel getSPrimeModel() {
        return this.sp;
    }
}
