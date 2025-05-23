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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

/**
 * This enumerate lists available interpolators for EOP data.
 * 
 * @author Anton Julie
 */
public enum EOPInterpolators {

    /** Fourth order Lagrange polynomial interpolator. */
    LAGRANGE4 {
        /** {@inheritDoc} */
        @Override
        public int getInterpolationPoints() {
            return 4;
        }
    },

    /** Linear interpolator. */
    LINEAR {
        /** {@inheritDoc} */
        @Override
        public int getInterpolationPoints() {
            return 2;
        }
    };

    /**
     * Return the number of points to use in interpolation.
     *
     * @return the number of points to use in interpolation
     */
    public abstract int getInterpolationPoints();
}
