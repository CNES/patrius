/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;



/**
 * Generic grid attraction provider. This interface represents a grid attraction provider, i.e. a class which provides
 * {@link AttractionData} for gravity force models defined by a 3D grid.
 * <p>This class is to be used in conjunction with {@link GridAttractionModel} for attraction data loading.</p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public interface GridAttractionProvider {

    /**
     * Returns the read data.
     * @return the read data
     */
    AttractionData getData();
}
