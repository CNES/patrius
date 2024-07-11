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
 * @history creation 16/06/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:01/08/2016:add wall property
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.cook;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Constant alpha (energy accomodation coefficient).
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public class AlphaConstant implements AlphaProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 1856751844630084583L;

    /** Alpha (energy accomodation coefficient). */
    private final double alpha;

    /**
     * Constructor.
     * 
     * @param a
     *        alpha
     */
    public AlphaConstant(final double a) {
        this.alpha = a;
    }

    /** {@inheritDoc} */
    @Override
    public double getAlpha(final SpacecraftState state) {
        return this.alpha;
    }
}
