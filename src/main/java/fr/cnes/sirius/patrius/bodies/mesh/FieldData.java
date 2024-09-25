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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-53:30/06/2023:[PATRIUS] Error in class FieldData
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Data container for {@link FacetBodyShape} field data. Field data are data related to a {@link FacetBodyShape} set of
 * triangles at a given date. In particular given a list of visible triangles, it can provides a contour.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class FieldData {

    /** Date. */
    private final AbsoluteDate date;

    /** Visible surface. */
    private final double visibleSurface;

    /** Visible triangles. */
    private final List<Triangle> visibleTriangles;

    /** Contour. */
    private final List<FacetPoint> contour;

    /**
     * Constructor.
     * 
     * @param date
     *        date
     * @param visibleTriangles
     *        visible triangle list
     * @param body
     *        body
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public FieldData(final AbsoluteDate date, final List<Triangle> visibleTriangles, final FacetBodyShape body) {
        this.date = date;
        this.visibleTriangles = visibleTriangles;

        // Compute visible surface
        double surface = 0;
        for (final Triangle triangle : visibleTriangles) {
            surface += triangle.getSurface();
        }
        this.visibleSurface = surface;

        // Compute contour: check each side of each visible triangle to determine if it's free (contour)
        // The free sides store their vertex into a list of pairs
        final List<Pair<Vertex, Vertex>> contourVertexPairs = new ArrayList<>();
        for (final Triangle triangle : visibleTriangles) {
            // Extract the triangles vertices
            final Vertex[] vertices = triangle.getVertices();
            final Vertex vertex1 = vertices[0];
            final Vertex vertex2 = vertices[1];
            final Vertex vertex3 = vertices[2];

            // Check if its first side (vertices 1 & 2) is free
            List<Triangle> neighborsSide = new ArrayList<>(vertex1.getNeighbors());
            // Keep only the neighbors common with the two vertices and only the visible neighbors,
            // then remove the current triangle
            neighborsSide.retainAll(vertex2.getNeighbors());
            neighborsSide.retainAll(visibleTriangles);
            neighborsSide.remove(triangle);
            if (neighborsSide.isEmpty()) {
                // If this side has no common neighbor other than the current triangle, the side is free
                // Store the two vertices in a pair
                contourVertexPairs.add(new Pair<>(vertex1, vertex2));
            }

            // Check if its second side (vertices 2 & 3) is free
            neighborsSide = new ArrayList<>(vertex2.getNeighbors());
            // Keep only the neighbors common with the two vertices and only the visible neighbors,
            // then remove the current triangle
            neighborsSide.retainAll(vertex3.getNeighbors());
            neighborsSide.retainAll(visibleTriangles);
            neighborsSide.remove(triangle);
            if (neighborsSide.isEmpty()) {
                // If this side has no common neighbor other than the current triangle, the side is free
                // Store the two vertices in a pair
                contourVertexPairs.add(new Pair<>(vertex2, vertex3));
            }

            // Check if its third side (vertices 3 & 1) is free
            neighborsSide = new ArrayList<>(vertex3.getNeighbors());
            // Keep only the neighbors common with the two vertices and only the visible neighbors,
            // then remove the current triangle
            neighborsSide.retainAll(vertex1.getNeighbors());
            neighborsSide.retainAll(visibleTriangles);
            neighborsSide.remove(triangle);
            if (neighborsSide.isEmpty()) {
                // If this side has no common neighbor other than the current triangle, the side is free
                // Store the two vertices in a pair
                contourVertexPairs.add(new Pair<>(vertex3, vertex1));
            }
        }

        if (contourVertexPairs.isEmpty()) {
            // If no free side has been found, the contour is empty
            this.contour = new ArrayList<>();
        } else {
            // Build the vertices continuous chain from the pairs as matching "dominos"
            // Use a Set to guarantee unity
            final Set<Vertex> contourVertex = new LinkedHashSet<>();

            // Extract the first pair information, keep the second element, then remove the pair from the list
            contourVertex.add(contourVertexPairs.get(0).getFirst());
            Vertex currentVertex = contourVertexPairs.get(0).getSecond();
            contourVertex.add(currentVertex);
            contourVertexPairs.remove(0);

            // Move from one element from a pair, to one element of the matching pair, then remove the previous pair
            int i = 0;
            while (i < contourVertexPairs.size()) {
                if (contourVertexPairs.get(i).getFirst().equals(currentVertex)) {
                    // If the current vertex matches the first element of the pair, move to the second element
                    currentVertex = contourVertexPairs.get(i).getSecond();
                    contourVertex.add(currentVertex);
                    contourVertexPairs.remove(i);
                    i = -1; // Reset the counter to loop over all the remaining elements
                } else if (contourVertexPairs.get(i).getSecond().equals(currentVertex)) {
                    // If the current vertex matches the second element of the pair, move to the first element
                    currentVertex = contourVertexPairs.get(i).getFirst();
                    contourVertex.add(currentVertex);
                    contourVertexPairs.remove(i);
                    i = -1; // Reset the counter to loop over all the remaining elements
                }
                i++;
            }
            // The contour of vertices is completed

            // Convert the vertices to facet points
            this.contour = new ArrayList<>(contourVertex.size());
            for (final Vertex vertex : contourVertex) {
                this.contour.add(new FacetPoint(body, vertex.getPosition(), "point_" + vertex.getID()));
            }
        }
    }

    /**
     * Getter for the date.
     * 
     * @return the date
     */
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Getter for the list of visible triangles. A triangle is visible if in the field of view, oriented toward the
     * field and not masked by any other triangle.
     * 
     * @return the list of visible triangles
     */
    public List<Triangle> getVisibleTriangles() {
        return this.visibleTriangles;
    }

    /**
     * Getter for the visible surface. This is the sum of all the visible triangles surface.
     * 
     * @return the visible surface
     */
    public double getVisibleSurface() {
        return this.visibleSurface;
    }

    /**
     * Getter for the contour of the visible surface on the body. The contour is made of the vertices of the triangles
     * which lies on the inner boundary of the visible surface (and are hence within the field of view).
     * <p>
     * Closest point to each other are next to each other in contour list.
     * </p>
     * 
     * @return the contour of the visible surface on the body
     */
    public List<FacetPoint> getContour() {
        return this.contour;
    }
}
