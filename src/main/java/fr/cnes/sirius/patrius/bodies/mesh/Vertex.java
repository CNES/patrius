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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * 3D vertex definition. A vertex is a 3D point.
 * For efficient computation, this class also stores the list of {@link Triangle} "owning" the vertex.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class Vertex implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -6278336027182409932L;

    /** Vertex ID. */
    private final int id;

    /** Vertex position in body frame. */
    private final Vector3D position;

    /** Direct neighbors ("owning" the vertex). */
    private final List<Triangle> neighbors;

    /**
     * Constructor.
     * @param id identifier
     * @param position 3D position
     */
    public Vertex(final int id,
            final Vector3D position) {
        this.id = id;
        this.position = position;

        // Initialize neighbors
        // Filled-in when triangles are initialized
        this.neighbors = new ArrayList<Triangle>();
    }

    /**
     * Returns the vertex identifier.
     * @return the vertex identifier
     */
    public int getID() {
        return id;
    }

    /**
     * Returns the vertex 3D position.
     * @return the vertex 3D position
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Returns the triangle neighbors (i.e. "owning" the vertex).
     * @return the triangle neighbors
     */
    public List<Triangle> getNeighbors() {
        return neighbors;
    }

    /**
     * Add a triangle neighbor (package method only to be used at initialization of {@link FacetBodyShape}).
     * <p>
     * Warning: no check is performed in order to verify is the provided triangle is really a neighbor.
     * </p>
     * @param neighbor the triangle neighbor to set
     */
    protected void addNeighbors(final Triangle neighbor) {
        this.neighbors.add(neighbor);
    }
}
