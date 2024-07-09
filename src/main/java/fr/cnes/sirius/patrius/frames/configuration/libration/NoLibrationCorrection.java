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
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class ignores the libration effects.
 * 
 * @concurrency immutable.
 * 
 * @author Julie Anton
 * 
 * @version $Id: NoLibrationCorrection.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public class NoLibrationCorrection implements LibrationCorrectionModel {

    /** IUD. */
    private static final long serialVersionUID = -808628503671630206L;

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate t) {
        return PoleCorrection.NULL_CORRECTION;
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate t) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.NONE;
    }
}
