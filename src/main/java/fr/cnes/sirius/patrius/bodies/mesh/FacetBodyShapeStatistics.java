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
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au
 * lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3299:22/05/2023:[PATRIUS] Gestion des ellipsoïdes de FacetBodyShape
 * VERSION:4.11:FA:FA-3316:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un potentiel variable
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class containing statistics methods for the FacetBodyShape object.
 *
 * @author Florian Teilhard
 *
 * @since 4.11
 */
public class FacetBodyShapeStatistics {

    /** The facet body shape under scrutiny. */
    private final FacetBodyShape facetBodyShape;

    /**
     * Constructor.
     * 
     * @param facetBodyShapeIn
     *        the facet body shape
     */
    public FacetBodyShapeStatistics(final FacetBodyShape facetBodyShapeIn) {
        this.facetBodyShape = facetBodyShapeIn;
    }

    /**
     * Compute the summary of the distances between the radius at each facet body shape
     * vertex and a reference ellipsoid radius in the same direction.
     * 
     * @param ellipsoid
     *        The ellipsoid to be compared to
     * @return the statistics summary
     * @throws PatriusException
     *         if an error occurs
     */
    public SummaryStatistics computeStatisticsForRadialDistance(final OneAxisEllipsoid ellipsoid)
        throws PatriusException {
        // Define the summary statistics
        final SummaryStatistics stats = new SummaryStatistics();
        // Compute transform from facetBodyShape body frame to ellipsoid body frame
        final Transform transform = this.facetBodyShape.getBodyFrame().getTransformTo(ellipsoid.getBodyFrame(),
            AbsoluteDate.J2000_EPOCH);
        // Loop on the vertices
        for (final Vertex v : this.facetBodyShape.getMeshProvider().getVertices().values()) {
            // Compute the radius in the ellipsoid body frame
            final Vector3D posVertex = transform.transformPosition(v.getPosition());
            // Create the radius line
            final Line radiusLine = new Line(Vector3D.ZERO, posVertex);
            // Compute the ellipsoid intersection point
            final EllipsoidPoint point = ellipsoid.getIntersectionPoint(radiusLine, posVertex,
                ellipsoid.getBodyFrame(), AbsoluteDate.J2000_EPOCH);
            // Transform the ellipsoid point to a cartesian point
            final Vector3D posEllipsoid = point.getPosition();
            // Compute the error
            final double error = posEllipsoid.distance(posVertex);
            // Check the norms
            if (posEllipsoid.getNorm() < posVertex.getNorm()) {
                // Add the error to the summary statistics
                stats.addValue(error);
            } else {
                // Add the opposite of the error to the summary statistics
                stats.addValue(-error);
            }
        }

        // Return the summary statistics
        return stats;
    }

    /**
     * Compute the summary of the distances between the altitude of each facet body shape vertex to a reference
     * ellipsoid surface.
     * 
     * @param ellipsoid
     *        The ellipsoid to be compared to
     * @return the statistics summary
     * @throws PatriusException
     *         if an error occurs
     */
    public SummaryStatistics computeStatisticsForAltitude(final OneAxisEllipsoid ellipsoid) throws PatriusException {
        final SummaryStatistics stats = new SummaryStatistics();

        // Compute transform from facetBodyShape body frame to ellipsoid body frame
        final Transform transform = this.facetBodyShape.getBodyFrame().getTransformTo(ellipsoid.getBodyFrame(),
            AbsoluteDate.J2000_EPOCH);

        for (final Vertex v : this.facetBodyShape.getMeshProvider().getVertices().values()) {
            // Compute the radius in the ellipsoid body frame
            final Vector3D posVertex = transform.transformPosition(v.getPosition());

            // Vertex converted to an ellipsoid point in the ellipsoid frame
            final EllipsoidPoint point = new EllipsoidPoint(ellipsoid, posVertex, BodyPointName.DEFAULT);

            final double error = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();
            stats.addValue(error);
        }
        return stats;
    }
}
