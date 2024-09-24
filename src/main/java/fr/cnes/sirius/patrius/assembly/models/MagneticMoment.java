/**
 *
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
 * @history creation 23/07/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:04/09/2013:Magnetic dipole model
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class represents the magnetic moment of a Spacecraft
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if the underlying frame is thread-safe
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class MagneticMoment implements MagneticMomentProvider {

    /**
     * Magnetic moment
     */
    private final Vector3D moment;

    /**
     * Create a magnetic moment expressed in spacecraft main frame (incl. attitude)
     * 
     * @param moment
     *        magnetic moment
     */
    public MagneticMoment(final Vector3D moment) {
        this.moment = moment;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getMagneticMoment(final AbsoluteDate date) {
        return this.moment;
    }
}
