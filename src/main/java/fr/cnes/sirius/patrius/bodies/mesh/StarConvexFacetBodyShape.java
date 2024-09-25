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
 * HISTORY
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3250:03/11/2022:[PATRIUS] Generalisation de TopocentricFrame
 * VERSION:4.10:FA:FA-3186:03/11/2022:[PATRIUS] Corriger la duplication entre getLocalRadius et getApparentRadius
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3127:01/06/2022:[PATRIUS] Utilisation des attributs minNorm et maxNorm
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3134:10/05/2022:[PATRIUS] ameliorations mineures de Vector2D 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3174:10/05/2022:[PATRIUS] Corriger les differences de convention entre toutes les methodes...
 * VERSION:4.9:DM:DM-3169:10/05/2022:[PATRIUS] Precision de l'hypothese de propagation instantanee de la lumiere
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3173:10/05/2022:[PATRIUS] Utilisation de FacetCelestialBody dans les calculs d'evenements 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import fr.cnes.sirius.patrius.bodies.StarConvexBodyShape;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Star-convex facet body shape defined by a list of facets. A facet is a 3D triangle defined in the
 * body frame.
 * <p>
 * This class extends the class {@link FacetBodyShape} and implements the interface {@link StarConvexBodyShape}.
 * </p>
 *
 * @author Florian Teilhard
 *
 * @since 4.11
 */
public class StarConvexFacetBodyShape extends FacetBodyShape implements StarConvexBodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = 4725702415576888010L;

    /**
     * Constructor for a star convex FacetBodyShape.
     * <p>
     * The {@link FacetBodyShape#getMaxSlope() maxSlope} value of the mesh must be smaller than PI/2 so it is considered
     * star convex. Otherwise, use the more generic {@link FacetBodyShape} constructor.
     * </p>
     *
     * @param name
     *        body name
     * @param bodyFrame
     *        frame in which celestial body coordinates are defined
     * @param meshLoader
     *        mesh loader
     * @throws IllegalArgumentException
     *         if loading failed or if the given mesh is not star convex
     */
    public StarConvexFacetBodyShape(final String name,
            final CelestialBodyFrame bodyFrame,
            final MeshProvider meshLoader) {
        super(name, bodyFrame, meshLoader);
        // Check that the maxSlope value is smaller than PI/2: it is the condition for the facet body shape to be
        // star-convex.
        if (this.getMaxSlope() >= MathLib.PI / 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INVALID_MESH_STARCONVEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public StarConvexFacetBodyShape resize(final MarginType marginType, final double marginValue) {
        final FacetBodyShape facetBodyShape = super.resize(marginType, marginValue);
        return new StarConvexFacetBodyShape(facetBodyShape.getName(), facetBodyShape.getBodyFrame(),
            facetBodyShape.getMeshProvider());
    }
}
