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
* VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
* VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Data container for {@link FacetCelestialBody} field data. Field data are data related to a {@link FacetCelestialBody}
 * set of triangles at a given date. In particular given a list of visible triangles, it can provides a contour.
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
    private final List<GeodeticPoint> contour;

    /**
     * Constructor.
     * @param date date
     * @param visibleTriangles visible triangle list
     * @param body body
     * @throws PatriusException thrown if computation failed (should not happen)
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public FieldData(final AbsoluteDate date,
            final List<Triangle> visibleTriangles,
            final FacetCelestialBody body) throws PatriusException {
        this.date = date;
        this.visibleTriangles = visibleTriangles;

        // Compute visible surface
        double surface = 0;
        for (final Triangle triangle : visibleTriangles) {
            surface += triangle.getSurface();
        }
        this.visibleSurface = surface;

        // Compute contour
        final List<GeodeticPoint> contourTmp = new ArrayList<GeodeticPoint>();
        final Map<Vertex, Integer> verticesMap = new HashMap<Vertex, Integer>();
        for (final Triangle triangle : visibleTriangles) {
            final Vertex[] vertices = triangle.getVertices();
            for (final Vertex vertex : vertices) {
                final Integer v = verticesMap.get(vertex);
                if (v == null) {
                    verticesMap.put(vertex, 1);
                } else {
                    verticesMap.put(vertex, v + 1);
                }
            }
        }
        // Contour vertices are those connected with max 3 triangles
        for (final Entry<Vertex, Integer> entry : verticesMap.entrySet()) {
            if (entry.getValue() <= 3) {
                final GeodeticPoint point = body.transform(entry.getKey().getPosition(), body.getBodyFrame(), null);
                contourTmp.add(point);
            }
        }
        
        // Order contour points (closest point to each other will be next to each other in contour list)
        this.contour = new ArrayList<GeodeticPoint>();
        if (!contourTmp.isEmpty()) {
            GeodeticPoint current = contourTmp.get(0);
            this.contour.add(current);
            contourTmp.remove(current);
            
            while (!contourTmp.isEmpty()) {
                // Find next closest point
                double closestDist = Double.POSITIVE_INFINITY;
                GeodeticPoint closestPoint = null;
                for (final GeodeticPoint point : contourTmp) {
                    final double dist = distance(point, current);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestPoint = point;
                    }
                }
                
                // Update list
                this.contour.add(closestPoint);
                contourTmp.remove(closestPoint);
                current = closestPoint;
            }
        }
    }

    /**
     * Returns distance between two geodetic points on a unit sphere.
     * @param p1 first geodetic point
     * @param p2 second geodetic point
     * @return distance between the two geodetic points on a unit sphere
     */
    private double distance(final GeodeticPoint p1, final GeodeticPoint p2) {
        final double dlat = p2.getLatitude() - p1.getLatitude();
        final double dlon = p2.getLongitude() - p1.getLongitude();
        final double sindlat2 = MathLib.sin(dlat / 2);
        final double sindlon2 = MathLib.sin(dlon / 2);
        final double a = sindlat2 * sindlat2 + MathLib.cos(p1.getLatitude()) * MathLib.cos(p2.getLatitude()) * sindlon2
                * sindlon2;
        return 2. * MathLib.atan2(MathLib.sqrt(a), MathLib.sqrt(1 - a));
    }
    
    /**
     * Returns the date.
     * @return the date
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Returns the list of visible triangles. A triangle is visible if in the field of view, oriented toward the
     * field and not masked by any other triangle.
     * @return the list of visible triangles
     */
    public List<Triangle> getVisibleTriangles() {
        return visibleTriangles;
    }

    /**
     * Returns the visible surface. This is the sum of all the visible triangles surface.
     * @return the visible surface
     */
    public double getVisibleSurface() {
        return visibleSurface;
    }

    /**
     * Returns the contour of the visible surface on the body. The contour is made of the vertices of the triangles
     * which lies on the inner boundary of the visible surface (and are hence within the field of view).
     * <p>
     * Closest point to each other are next to each other in contour list.
     * </p>
     * @return the contour of the visible surface on the body
     */
    public List<GeodeticPoint> getContour() {
        return contour;
    }
}