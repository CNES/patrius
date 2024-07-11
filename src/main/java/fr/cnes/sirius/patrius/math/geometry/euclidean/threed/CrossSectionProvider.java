/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 8/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

/**
 * <p>
 * Interface for all geometric objects that can provide their cross section from a direction defined by a Vector3D.
 * </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: CrossSectionProvider.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public interface CrossSectionProvider {

    /**
     * Computes the cross section from the direction
     * defined by a Vector3D.
     * 
     * @param direction
     *        the direction vector
     * @return the cross section
     */
    double getCrossSection(Vector3D direction);

}
