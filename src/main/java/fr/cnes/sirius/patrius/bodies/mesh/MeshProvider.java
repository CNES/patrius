/**
 * Copyright 2011-2020 CNES
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
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.Serializable;
import java.util.Map;


/**
 * Generic mesh provider. This interface represents a mesh provider, i.e. a class which provides the list of
 * {@link Triangle} and {@link Vertex} of a given mesh.
 * <p>This class is to be used in conjunction with {@link FacetBodyShape} for body mesh loading.</p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public interface MeshProvider extends Serializable {

    /**
     * Returns the list of triangles of the mesh.
     * @return list of triangles of the mesh
     */
    Triangle[] getTriangles();

    /**
     * Returns the list of vertices of the mesh.
     * @return list of vertices of the mesh
     */
    public Map<Integer, Vertex> getVertices();
}
