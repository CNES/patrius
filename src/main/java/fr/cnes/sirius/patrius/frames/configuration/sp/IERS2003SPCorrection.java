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
 * @history creation 24/09/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.sp;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Compute s' correction (IERS 2003 convention).
 * <p>
 * IERS 2003 convention exactly matches IERS 2010 convention. As a result this class returns exactly the same results as
 * {@link IERS2010SPCorrection}.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: IERS2003SPCorrection.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 3.3
 */
public class IERS2003SPCorrection implements SPrimeModel {

    /** IUD. */
    private static final long serialVersionUID = 6357789880736142045L;

    /**
     * S' rate in radians per julian century.
     * Approximately -47 microarcsecond per julian century (Lambert and Bizouard, 2002)
     */
    private static final double S_PRIME_RATE = -47e-6 * Constants.ARC_SECONDS_TO_RADIANS;

    /** {@inheritDoc} */
    @Override
    public double getSP(final AbsoluteDate date) {
        // offset from J2000 epoch in julian centuries
        final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
        final double ttc = tts / Constants.JULIAN_CENTURY;
        // s'
        return S_PRIME_RATE * ttc;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.IERS2003;
    }
}
