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
 * @history creation 27/06/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.sp;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface provides the s' correction (used for the following transformation : TIRF -> ITRF).
 * 
 * <p>
 * s is a quantity, named "TIO (Terrestrial Intermediate Origin) locator", which provides the position of the TIO on the
 * equator of the CIP (Celestial Intermediate Pole) corresponding to the kinematical definition of the "non-rotating"
 * origin (NRO) in the ITRS when the CIP is moving with respect to the ITRS due to polar motion. (see chapter 5.4.1 of
 * the IERS Convention 2010)
 * </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id: SPrimeModel.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public interface SPrimeModel extends Serializable {

    /**
     * Compute the correction S' at a given date.
     * 
     * @param t
     *        date
     * @return correction S'
     */
    double getSP(AbsoluteDate t);

    /**
     * Get IERS model origin.
     * 
     * @return IERS model origin
     */
    FrameConvention getOrigin();
}
