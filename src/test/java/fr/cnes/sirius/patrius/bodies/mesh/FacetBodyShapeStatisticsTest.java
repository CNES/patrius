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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-81:30/06/2023:[PATRIUS] Reliquat DM 3299
 * VERSION:4.11:DM:DM-3299:22/05/2023:[PATRIUS] Gestion des ellipsoïdes de FacetBodyShape
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3183:03/11/2022:[PATRIUS] Acces aux points les plus proches entre un GeometricBodyShape...
 * VERSION:4.10:FA:FA-3186:03/11/2022:[PATRIUS] Corriger la duplication entre getLocalRadius et getApparentRadius
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2870:18/05/2021:Complements pour la manipulation des coordonnees cart.
 * VERSION:4.7:FA:FA-2821:18/05/2021:Refus injustifié de calcul de l incidence solaire lorsqu elle dépasse 90 degres 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape.EllipsoidType;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for {@link FacetBodyShapeStatistics} class.
 *
 * @author Florian Test
 *
 * @since 4.11
 */
public class FacetBodyShapeStatisticsTest {

    /** Epsilon for double comparison. */
    private static final double EPS = 1E-14;

    /** Facet body shape used for tests: Phobos mesh in Moon position. */
    private static FacetBodyShape bodyPhobos;

    /** Facet body shape used for tests: Phobos mesh in Moon position. */
    private static FacetBodyShape bodySphere;

    /**
     * Load mesh.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, URISyntaxException {
        // Patrius data set
        Utils.setDataRoot("regular-dataPBASE");

        // Load Phobos .obj mesh
        final String modelFilePhobos = "mnt" + File.separator + "Phobos_Ernst_HD.obj";
        final String fullNamePhobos = FacetBodyShape.class.getClassLoader().getResource(modelFilePhobos).toURI()
                .getPath();

        bodyPhobos = new FacetBodyShape("", FramesFactory.getGCRF(), EllipsoidType.INNER_SPHERE, new ObjMeshLoader(
                fullNamePhobos));

        // Sphere .obj mesh
        final String modelFileSphere = "mnt" + File.separator + "SphericalBody.obj";
        final String fullNameSphere = FacetBodyShape.class.getClassLoader().getResource(modelFileSphere).toURI()
                .getPath();

        bodySphere = new FacetBodyShape("", FramesFactory.getGCRF(), EllipsoidType.INNER_SPHERE, new ObjMeshLoader(
                fullNameSphere));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @description
     *
     * @testPassCriteria
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void computeStatisticsForRadialDistanceTest() throws PatriusException {
        final OneAxisEllipsoid fittedEllipsoid = bodyPhobos.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID);
        final FacetBodyShapeStatistics facetStats = new FacetBodyShapeStatistics(bodyPhobos);
        final SummaryStatistics stats = facetStats.computeStatisticsForRadialDistance(fittedEllipsoid);

        final double refMin = -1674.0796550599657;
        final double refMax = 1762.9276174044958;
        final double refMean = -0.24195522282413748;
        final double refVariance = 415473.0188680636;
        final double refSumSq = 4.148290941409896E10;
        final double refStDev = 644.5719656237491;

        Assert.assertEquals(0., (refMin - stats.getMin()) / refMin, EPS);
        Assert.assertEquals(0., (refMax - stats.getMax()) / refMax, EPS);
        Assert.assertEquals(0., (refMean - stats.getMean()) / refMean, EPS);
        Assert.assertEquals(0., (refVariance - stats.getVariance()) / refVariance, EPS);
        Assert.assertEquals(0., (refSumSq - stats.getSumsq()) / refSumSq, EPS);
        Assert.assertEquals(0., (refStDev - stats.getStandardDeviation()) / refStDev, EPS);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @description
     *
     * @testPassCriteria
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void computeStatisticsForAltitudeTest() throws PatriusException {
        final OneAxisEllipsoid fittedEllipsoid = bodyPhobos.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID);
        final FacetBodyShapeStatistics facetStats = new FacetBodyShapeStatistics(bodyPhobos);
        final SummaryStatistics stats = facetStats.computeStatisticsForAltitude(fittedEllipsoid);

        final double refMin = -1621.7096502134605;
        final double refMax = 1759.8113038796785;
        final double refMean = 0.14077799435797175;
        final double refVariance = 397838.7634902792;
        final double refSumSq = 3.9722213319479256E10;
        final double refStDev = 630.7446103537304;

        Assert.assertEquals(0., (refMin - stats.getMin()) / refMin, EPS);
        Assert.assertEquals(0., (refMax - stats.getMax()) / refMax, EPS);
        Assert.assertEquals(0., (refMean - stats.getMean()) / refMean, EPS);
        Assert.assertEquals(0., (refVariance - stats.getVariance()) / refVariance, EPS);
        Assert.assertEquals(0., (refSumSq - stats.getSumsq()) / refSumSq, EPS);
        Assert.assertEquals(0., (refStDev - stats.getStandardDeviation()) / refStDev, EPS);

    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @description Check that for a sphere, the radius distance and the altitude are the same.
     *
     * @testPassCriteria The relative difference between each value of the two statistics is lower
     *                   than 1e-14
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void compareStatisticsAltitudeAndRadius() throws PatriusException {

        final FacetBodyShapeStatistics facetStats = new FacetBodyShapeStatistics(bodyPhobos);
        final OneAxisEllipsoid sphere11km = new OneAxisEllipsoid(11000., 0, FramesFactory.getGCRF());
        final SummaryStatistics radiusStats = facetStats.computeStatisticsForRadialDistance(sphere11km);
        final SummaryStatistics altitudeStats = facetStats.computeStatisticsForAltitude(sphere11km);

        // Since the ellipsoid is a sphere and the frames of the ellipsoid and the mesh are equal,
        // check that the statistics for the altitude and the radius distance are the same
        Assert.assertEquals(0, (radiusStats.getMin() - altitudeStats.getMin()) / radiusStats.getMin(), EPS);
        Assert.assertEquals(0, (radiusStats.getMax() - altitudeStats.getMax()) / radiusStats.getMax(), EPS);
        Assert.assertEquals(0, (radiusStats.getMean() - altitudeStats.getMean()) / radiusStats.getMean(), EPS);
        Assert.assertEquals(0, (radiusStats.getVariance() - altitudeStats.getVariance()) / radiusStats.getVariance(),
                EPS);
        Assert.assertEquals(0, (radiusStats.getSumsq() - altitudeStats.getSumsq()) / radiusStats.getSumsq(), EPS);
        Assert.assertEquals(0, (radiusStats.getStandardDeviation() - altitudeStats.getStandardDeviation())
                / radiusStats.getStandardDeviation(), EPS);

    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @description Check that for a sphere, the delta in the sphere radius leads to a proportional
     *              evolution of the statistics with the method computeStatisticsForAltitude.
     *              Statistics are computed for the same facet body shape but for two reference
     *              spheres, with a radius difference of 1000m.
     *
     * @testPassCriteria The difference between the min,max,mean values of the two
     *                   statistics is equal to 1000.
     *                   The variance and the standard deviation are equal
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testStatisticsAltitudeSphereEvolution() throws PatriusException {
        // The facet body shape
        final FacetBodyShapeStatistics facetStats = new FacetBodyShapeStatistics(bodyPhobos);
        // the sphere of 11km radius
        final OneAxisEllipsoid sphere11km = new OneAxisEllipsoid(11000., 0., FramesFactory.getGCRF());
        final SummaryStatistics altitudeStats11km = facetStats.computeStatisticsForAltitude(sphere11km);
        // the sphere of 12km radius
        final OneAxisEllipsoid sphere12km = new OneAxisEllipsoid(12000., 0., FramesFactory.getGCRF());
        final SummaryStatistics altitudeStats12km = facetStats.computeStatisticsForAltitude(sphere12km);

        Assert.assertEquals(0., (MathLib.abs(altitudeStats12km.getMin() - altitudeStats11km.getMin()) - 1000.) / 1000.,
                EPS);
        Assert.assertEquals(0., (MathLib.abs(altitudeStats12km.getMax() - altitudeStats11km.getMax()) - 1000.) / 1000.,
                EPS);
        Assert.assertEquals(0.,
                (MathLib.abs(altitudeStats12km.getMean() - altitudeStats11km.getMean()) - 1000.) / 1000., EPS);
        Assert.assertEquals(0,
                (altitudeStats12km.getVariance() - altitudeStats11km.getVariance()) / altitudeStats12km.getVariance(),
                EPS);
        Assert.assertEquals(0, (altitudeStats12km.getStandardDeviation() - altitudeStats11km.getStandardDeviation())
                / altitudeStats12km.getStandardDeviation(), EPS);

    }

}
