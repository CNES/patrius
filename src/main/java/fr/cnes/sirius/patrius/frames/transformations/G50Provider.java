/**
 * Copyright 2023-2023 CNES
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
 * HISTORY
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * G50 (Gamma 50) frame.
 * <p>
 * Its parent frame is the {@link GTODProvider} without EOP correction application.
 * </p>
 * <p>
 * This frame is mainly provided for consistency with legacy softwares.
 * </p>
 * 
 * <p>Spin derivative, when computed, is always 0.</p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 */
public final class G50Provider extends AbstractVeisProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 3285415063615994863L;

    /** {@inheritDoc} */
    @Override
    protected double getUT1MinusTAI(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        // UT1 - UTC is 0 in this case
        return TimeScalesFactory.getUTC().offsetFromTAI(date);
    }
}
