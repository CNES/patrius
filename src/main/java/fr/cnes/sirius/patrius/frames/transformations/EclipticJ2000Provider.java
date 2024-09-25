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
 *
 * @history 08/08/2023
 *
 * HISTORY
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-43:08/08/2023:[PATRIUS] Introduction du repère ECLIPTIC_J2000.
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * {@link TransformProvider} for {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEclipticJ2000()}.
 * 
 * <p>
 * Spin derivative is either 0 or null since rotation is linear in time.
 * </p>
 * <p>
 * Frames configuration is unused.
 * </p>
 * 
 * @author Courtemanche Willy
 * 
 * @since 4.13
 */
public class EclipticJ2000Provider extends FixedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -4091290982072166211L;

    /** Fixed transform between GCRF and MOD at J2000 epoch. */
    private static Transform gcrfToEclipticMOD;

    /**
     * Constructor.
     */
    public EclipticJ2000Provider() {
        // build the bias transform
        super(getTransform());
    }

    /**
     * Compute the fixed transform between GCRF and MOD at J2000 epoch (lazy initialization).
     * 
     * @return fixed transform between GCRF and MOD at J2000 epoch
     */
    private static synchronized Transform getTransform() {
        try {
            if (gcrfToEclipticMOD == null) {
                gcrfToEclipticMOD = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEclipticMOD(true),
                        AbsoluteDate.J2000_EPOCH);
            }
            return gcrfToEclipticMOD;
        } catch (final PatriusException e) {
            // Cannot happen
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }
}
