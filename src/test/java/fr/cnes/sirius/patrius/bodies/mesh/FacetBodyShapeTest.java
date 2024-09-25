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
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-137:08/12/2023:[PATRIUS] Exception asin dans FacetBodyShape.getApparentRadius
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-53:30/06/2023:[PATRIUS] Error in class FieldData
 * VERSION:4.11.1:FA:FA-81:30/06/2023:[PATRIUS] Reliquat DM 3299
 * VERSION:4.11.1:FA:FA-60:30/06/2023:[PATRIUS] Erreur dans les méthodes getNeighbors de FacetBodyShape
 * VERSION:4.11:DM:DM-7:22/05/2023:[PATRIUS] Symetriser les methodes closestPointTo de BodyShape
 * VERSION:4.11:DM:DM-3288:22/05/2023:[PATRIUS] ID de facette pour un FacetBodyShape
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Besoin de forcer la normalisation dans la classe QuaternionPolynomialSegment
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.10:DM:DM-3183:03/11/2022:[PATRIUS] Acces aux points les plus proches entre un GeometricBodyShape...
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.8:FA:FA-3091:15/11/2021:[PATRIUS] Corriger la methode bodyShapeMaskingDistance de SensorModel
 * VERSION:4.7:DM:DM-2870:18/05/2021:Complements pour la manipulation des coordonnees cart.
 * VERSION:4.7:FA:FA-2821:18/05/2021:Refus injustifié de calcul de l incidence solaire lorsqu elle dépasse 90 degres 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyShape.MarginType;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape.EllipsoidType;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.events.detectors.SensorVisibilityDetector;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for {@link FacetCelestialBody} class.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class FacetBodyShapeTest {

    /** Epsilon for double comparison. */
    private static final double EPS = 1E-14;

    /** Epsilon for double comparison. */
    private static final double EPS_OPTIMIZER = 1E-11;

    /** Body radius (m). */
    private final double bodyRadius = 10000.;

    /** User celestial body used for tests. */
    private UserCelestialBody celestialBody;

    /** Facet celestial body used for tests. */
    private StarConvexFacetBodyShape body;

    /** Mesh loader. */
    private MeshProvider meshProv;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(FacetBodyShapeTest.class.getSimpleName(), "Facet Body Shape");
    }

    /**
     * Builds a theoretical spherical celestial whose poles are aligned with GCRF and whose
     * PVCoordinates are (0, 0, 0)in GCRF frame.
     */
    @Before
    public void setUp() throws PatriusException, IOException {

        // Build body file
        final String spherBodyObjPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "mnt" + File.separator + "SphericalBody.obj";
        final String modelFile = System.getProperty("user.dir") + File.separator + spherBodyObjPath;
        writeBodyFile(modelFile, 51, 100, this.bodyRadius / 1E3, 0.);

        // Retrieve body
        final PVCoordinatesProvider pvCoordinates = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 1794415506210153289L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return PVCoordinates.ZERO;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        this.celestialBody = new UserCelestialBody("My body", pvCoordinates, 0, IAUPoleFactory.getIAUPole(null),
            FramesFactory.getGCRF(), null);
        this.meshProv = new ObjMeshLoader(modelFile);
        this.body = new StarConvexFacetBodyShape("My body", this.celestialBody.getRotatingFrame(IAUPoleModelType.TRUE),
            new ObjMeshLoader(modelFile));
    }

    /**
     * Build spherical model with a possible flattening and write it in file
     *
     * @param modelFile output model file name
     * @param latitudeNumber number of latitude points (should be odd)
     * @param longitudeNumber number longitude points
     * @param radius equatorial body radius (km)
     * @param flattening the flattening of the sphere
     */
    private static void writeBodyFile(final String modelFile, final int latitudeNumber, final int longitudeNumber,
                                      final double radius, final double flattening) throws IOException {
        // Initialization, open resources
        final FileOutputStream fileOutputStream = new FileOutputStream(modelFile);
        final OutputStreamWriter fileWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8")
            .newEncoder());
        final PrintWriter printWriter = new PrintWriter(fileWriter);

        // Build body
        final int latitudeNumber2 = (latitudeNumber - 1) / 2 - 1;
        final int numberPoints = (2 * latitudeNumber2 + 1) * longitudeNumber + 2;

        // Points

        // South pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., -radius * (1 - flattening)));

        // Regular points excluding poles
        for (int i = -latitudeNumber2; i <= latitudeNumber2; i++) {
            final double latitude = (double) i / (latitudeNumber2 + 1) * MathLib.PI / 2.;
            for (int j = 0; j < longitudeNumber; j++) {
                final double longitude = (double) j / longitudeNumber * 2. * MathLib.PI;
                final double coslat = MathLib.cos(latitude);
                final double sinlat = MathLib.sin(latitude);
                final double coslon = MathLib.cos(longitude);
                final double sinlon = MathLib.sin(longitude);
                final Vector3D pv = new Vector3D(coslat * coslon, coslat * sinlon, sinlat * (1 - flattening))
                    .scalarMultiply(radius);
                printWriter
                    .println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", pv.getX(), pv.getY(), pv.getZ()));
            }
        }

        // North pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., radius * (1 - flattening)));

        // Triangles

        // South pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, j + 3, j + 2));
        }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, 2, longitudeNumber + 1));

        // Regular points excluding poles
        for (int i = 0; i < latitudeNumber - 3; i++) {
            for (int j = 0; j < longitudeNumber - 1; j++) {
                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + j, i
                        * longitudeNumber + 2 + j + 1, (i + 1) * longitudeNumber + 2 + j));

                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + j + 1, (i + 1)
                        * longitudeNumber + 2 + j + 1, (i + 1)
                        * longitudeNumber + 2 + j));
            }
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + longitudeNumber
                    - 1, i * longitudeNumber + 2, (i + 1) * longitudeNumber + 2 + longitudeNumber - 1));

            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2, (i + 1)
                    * longitudeNumber + 2, (i + 1)
                    * longitudeNumber + 2 + longitudeNumber - 1));
        }

        // North pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints, numberPoints - j - 2,
                numberPoints - j - 1));
        }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints,
            numberPoints - 1, numberPoints - longitudeNumber));

        // Close resources
        printWriter.close();
        fileWriter.close();
        fileOutputStream.close();
    }

    /**
     * @testType UT
     *
     * @description check that body surface is as expected.
     *
     * @testPassCriteria body surface is equal to 4Pi.r.r (reference: math, relative threshold:
     *                   1E-3, limited due to
     *                   limited number of vertices in the model)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void surfaceTest() {
        double actual = 0;
        for (final Triangle triangle : this.body.getTriangles()) {
            actual += triangle.getSurface();
        }
        final double reference = 4. * MathLib.PI * this.bodyRadius * this.bodyRadius;
        Assert.assertEquals(0., (reference - actual) / reference, 1E-3);

        Assert.assertTrue(this.body.isDefaultLLHCoordinatesSystem()); // Quick test to cover FA-125
    }

    /**
     * @testType UT
     *
     * @description check that intersection between a line of sight and the body is properly
     *              computed for various
     *              configurations:
     *              <ul>
     *              <li>Body center pointing case</li>
     *              <li>Anti-body center pointing case</li>
     *              <li>No intersection case</li>
     *              <li>Tangent case</li>
     *              </ul>
     *
     * @testPassCriteria intersection between a line of sight and the body is properly computed for
     *                   various
     *                   configurations (reference: math, relative threshold: 1E-3, limited due to
     *                   number of vertices in
     *                   the
     *                   model)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void intersectionTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final Intersection actual1 = this.body.getIntersection(lineOfSight1, position1, frame, date);
        final Vector3D reference1 = position1.normalize().scalarMultiply(this.bodyRadius);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual1.getPoint()) / reference1.getNorm(), 1E-3);

        // Intersection (basic case: opposite to body center pointing, line is still intersecting)
        final Vector3D position2 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight2 = new Line(position2, position2.scalarMultiply(2.));
        final Intersection actual2 = this.body.getIntersection(lineOfSight2, position2, frame, date);
        final Vector3D reference2 = position2.normalize().scalarMultiply(this.bodyRadius);
        Assert.assertEquals(0., Vector3D.distance(reference2, actual2.getPoint()) / reference2.getNorm(), 1E-3);

        // Intersection (no intersection but close)
        final Vector3D position3 = new Vector3D(10001, 0, 0);
        final Line lineOfSight3 = new Line(position3, position3.add(Vector3D.PLUS_J));
        final Intersection actual3 = this.body.getIntersection(lineOfSight3, position3, frame, date);
        Assert.assertNull(actual3);

        // Intersection (tangent case)
        final Vector3D position4 = new Vector3D(0, 0, 10000);
        final Line lineOfSight4 = new Line(position4, position4.add(Vector3D.PLUS_I));
        final Frame bodyFrame = this.body.getBodyFrame();
        final Intersection actual4 = this.body.getIntersection(lineOfSight4, position4, bodyFrame, date);
        final Vector3D reference4 = position4;
        Assert.assertEquals(0., Vector3D.distance(reference4, actual4.getPoint()) / reference4.getNorm(), 0.);
    }

    /**
     * @testType UT
     *
     * @description check that intersection between a line of sight and the body is properly
     *              computed for various configurations:
     *              <ul>
     *              <li>Body center pointing case</li>
     *              <li>Anti-body center pointing case</li>
     *              <li>No intersection case</li>
     *              <li>Tangent case</li>
     *              </ul>
     *
     * @testPassCriteria intersection between a line of sight and the body is properly computed for
     *                   various configurations (reference: math, relative threshold: 1E-3, limited due to number of
     *                   vertices in the model)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void intersectionPointTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final FacetPoint actual1 = this.body.getIntersectionPoint(lineOfSight1, position1, frame, date);
        final FacetPoint reference1 = this.body.buildPoint(position1, frame, date, "");
        Assert.assertEquals(reference1.getLLHCoordinates().getLatitude(), actual1.getLLHCoordinates().getLatitude(),
            0.02);
        Assert.assertEquals(reference1.getLLHCoordinates().getLongitude(), actual1.getLLHCoordinates().getLongitude(),
            0.04);
        Assert.assertEquals(0., actual1.getLLHCoordinates().getHeight() / position1.getNorm(), 0.01);

        // Intersection (basic case: opposite to body center pointing, line is still intersecting)
        final Vector3D position2 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight2 = new Line(position2, position2.scalarMultiply(2.));
        final FacetPoint actual2 = this.body.getIntersectionPoint(lineOfSight2, position2, frame, date);
        final FacetPoint reference2 = this.body.buildPoint(position2, frame, date, "");
        Assert.assertEquals(reference2.getLLHCoordinates().getLatitude(), actual2.getLLHCoordinates().getLatitude(),
            0.02);
        Assert.assertEquals(reference2.getLLHCoordinates().getLongitude(), actual2.getLLHCoordinates().getLongitude(),
            0.04);
        Assert.assertEquals(0., actual2.getLLHCoordinates().getHeight() / position2.getNorm(), 0.01);

        // Intersection (no intersection but close)
        final Vector3D position3 = new Vector3D(10001, 0, 0);
        final Line lineOfSight3 = new Line(position3, position3.add(Vector3D.PLUS_J));
        final FacetPoint actual3 = this.body.getIntersectionPoint(lineOfSight3, position3, frame, date);
        Assert.assertNull(actual3);
    }

    /**
     * @testType UT
     *
     * @description check that intersections between a line of sight and the body is properly
     *              computed for various configurations:
     *              <ul>
     *              <li>Body center pointing case</li>
     *              <li>Anti-body center pointing case</li>
     *              <li>No intersection case</li>
     *              <li>Tangent case</li>
     *              </ul>
     *
     * @testPassCriteria intersections between a line of sight and the body is properly computed for various
     *                   configurations (reference: math, relative threshold: 1E-3, limited due to number of vertices in
     *                   the model)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void intersectionsTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final FacetPoint[] actual1 = this.body.getIntersectionPoints(lineOfSight1, frame, date);
        final Vector3D[] reference1 = new Vector3D[] { position1.normalize().scalarMultiply(-this.bodyRadius),
            position1.normalize().scalarMultiply(this.bodyRadius) };
        Assert.assertEquals(0., Vector3D.distance(reference1[0], actual1[0].getPosition()) / reference1[0].getNorm(),
            1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1[1], actual1[1].getPosition()) / reference1[1].getNorm(),
            1E-3);

        // Intersection (basic case: opposite to body center pointing, line is still intersecting)
        final Vector3D position2 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight2 = new Line(position2, position2.scalarMultiply(2.));
        final FacetPoint[] actual2 = this.body.getIntersectionPoints(lineOfSight2, frame, date);
        final Vector3D[] reference2 = new Vector3D[] { position2.normalize().scalarMultiply(-this.bodyRadius),
            position2.normalize().scalarMultiply(this.bodyRadius) };
        Assert.assertEquals(0., Vector3D.distance(reference2[0], actual2[0].getPosition()) / reference2[0].getNorm(),
            1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference2[1], actual2[1].getPosition()) / reference2[1].getNorm(),
            1E-3);

        // Intersection (no intersection but close)
        final Vector3D position3 = new Vector3D(10001, 0, 0);
        final Line lineOfSight3 = new Line(position3, position3.add(Vector3D.PLUS_J));
        final FacetPoint[] actual3 = this.body.getIntersectionPoints(lineOfSight3, frame, date);
        Assert.assertEquals(0, actual3.length);

        // Intersection (tangent case at north pole, 1 point expected since duplicates are
        // eliminated)
        final Vector3D position4 = new Vector3D(0, 0, 10000);
        final Line lineOfSight4 = new Line(position4, position4.add(Vector3D.PLUS_I));
        final Frame bodyFrame = this.body.getBodyFrame();
        final FacetPoint[] actual4 = this.body.getIntersectionPoints(lineOfSight4, bodyFrame, date);
        final Vector3D[] reference4 = new Vector3D[] { position4 };
        Assert.assertEquals(1, actual4.length);
        Assert.assertEquals(0., Vector3D.distance(reference4[0], actual4[0].getPosition()) / reference4[0].getNorm(),
            0.);

        // Intersection (no valid intersection because of abscissa too low)
        final Vector3D position5 = new Vector3D(100E3, 200E3, 300E3);
        final Vector3D pointMinAbscissa = new Vector3D(-100E3, -200E3, -300E3);
        final Line lineOfSight5 = new Line(position5, Vector3D.ZERO, pointMinAbscissa);
        final FacetPoint[] actual5 = this.body.getIntersectionPoints(lineOfSight5, frame, date);
        Assert.assertEquals(0, actual5.length);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link FacetCelestialBody#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}
     *
     * @description Checks intersection between a line and an ellipsoid at a given altitude on two cases:
     *              <ul>
     *              <li>Intersection at altitude = 100m: altitude of computed points should be 100m (accuracy: 1E-3m)</li>
     *              <li>Intersection at altitude = 0m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              </ul>
     *
     * @testPassCriteria altitude of computed points are as expected. Points are on the initial line.
     *
     * @referenceVersion 4.8
     *
     * @nonRegressionVersion 4.8
     */
    @Test
    public void intersectionPointsAltitudeTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Test with a random point and altitude = 100m
        final Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction = new Vector3D(0.0, 1.0, 1.0);
        final Line line = new Line(point, point.add(direction));
        try {
            this.body.getIntersectionPoint(line, point, this.body.getBodyFrame(), date, 100);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Test with a random point and altitude = 0m. Exact result is expected
        final Vector3D point2 = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction2 = new Vector3D(0.0, 1.0, 1.0);
        final Line line2 = new Line(point2, point2.add(direction2));
        final Frame bodyFrame = this.body.getBodyFrame();
        final FacetPoint gp2 = this.body.getIntersectionPoint(line2, point2, bodyFrame, date, 0);
        final FacetPoint gpRef = this.body.getIntersectionPoint(line2, point2, bodyFrame, date);
        Assert.assertEquals(gp2.getLLHCoordinates().getLatitude(), gpRef.getLLHCoordinates().getLatitude(), 0);
        Assert.assertEquals(gp2.getLLHCoordinates().getLongitude(), gpRef.getLLHCoordinates().getLongitude(), 0);
        Assert.assertEquals(gp2.getLLHCoordinates().getHeight(), gpRef.getLLHCoordinates().getHeight(), 0);
    }

    /**
     * @testType UT
     *
     * @description check that eclipse is properly detected for various configurations: <li>In eclipse</li> <li>
     *              Satellite between Sun and Body (no eclipse)</li> <li>Satellite outside of segment [Sun, Body] (no
     *              eclipse)</li> </ul>
     *
     * @testPassCriteria eclipse is properly detected for various configurations (reference: math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void eclipseTest() throws PatriusException {
        // Initialization
        final PVCoordinatesProvider sun = new MeeusSun();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();

        // Eclipse
        final Vector3D position1 = sun.getPVCoordinates(date, frame).getPosition().negate();
        final boolean actual1 = this.body.isInEclipse(date, position1, frame, sun);
        Assert.assertTrue(actual1);

        // No eclipse (body not on the path Sun - satellite)
        final Vector3D position2 = sun.getPVCoordinates(date, frame).getPosition().add(Vector3D.PLUS_I);
        final boolean actual2 = this.body.isInEclipse(date, position2, frame, sun);
        Assert.assertFalse(actual2);

        // No eclipse (body not on the path Sun - satellite but satellite on the path Sun - body)
        final Vector3D position3 = sun.getPVCoordinates(date, frame).getPosition().scalarMultiply(0.5);
        final boolean actual3 = this.body.isInEclipse(date, position3, frame, sun);
        Assert.assertFalse(actual3);
    }

    /**
     * @testType UT
     *
     * @description check that cartesian - geodetic coordinates are properly performed.
     *
     * @testPassCriteria cartesian - geodetic coordinates are properly performed (reference:
     *                   OneAxisEllipsoid, threshold: 1E-11, limited because file does contain numbers with only 11
     *                   digits)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void transformTest() throws PatriusException {
        // builds the map of the ellipsoid type and the expected BodyShape
        final Map<EllipsoidType, EllipsoidBodyShape> map = new HashMap<>();
        map.put(EllipsoidType.INNER_SPHERE, this.body.getEllipsoid(EllipsoidType.INNER_SPHERE));
        map.put(EllipsoidType.OUTER_SPHERE, this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE));
        map.put(EllipsoidType.INNER_ELLIPSOID, this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID));
        map.put(EllipsoidType.OUTER_ELLIPSOID, this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID));
        map.put(EllipsoidType.FITTED_ELLIPSOID, this.body.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID));

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D position = new Vector3D(11E3, 12E3, 13E3);
        final Frame frame = FramesFactory.getEME2000();

        for (final Map.Entry<EllipsoidType, EllipsoidBodyShape> entry : map.entrySet()) {
            // tests for each enumerate, get a copy of the original FacetBodyShape
            final FacetBodyShape bodyShape = new FacetBodyShape(this.body.getName(), this.body.getBodyFrame(),
                this.meshProv);

            // reference ellipsoid
            final EllipsoidBodyShape ellipsoid = entry.getValue();

            // Check cartesian => geodetic coordinates
            final FacetPoint actual1 = bodyShape.buildPoint(position, frame, date, "");
            final EllipsoidPoint expected1 = ellipsoid.buildPoint(position, frame, date, "");
            Assert.assertEquals(expected1.getLLHCoordinates().getLatitude(), actual1.getLLHCoordinates().getLatitude(),
                0.02);
            Assert.assertEquals(expected1.getLLHCoordinates().getLongitude(), actual1.getLLHCoordinates()
                .getLongitude(), 0.013);
            Assert.assertEquals(0., (expected1.getLLHCoordinates().getHeight() - actual1.getLLHCoordinates()
                .getHeight()) / expected1.getLLHCoordinates().getHeight(), 1e-3);

            // Check geodetic coordinates => cartesian
            final Vector3D actual2 = actual1.getPosition();
            final Vector3D expected2 = expected1.getPosition();
            Assert.assertEquals(0., Vector3D.distance(expected2, actual2), EPS);
        }
    }

    /** Test for a flattened sphere */
    @Test
    public void ellipsoidTest() throws PatriusException, IOException {

        // Flattening used in this test
        final double flattening = 0.2;
        // Build body file
        final String spherBodyObjPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "mnt" + File.separator + "EllipsoidBody.obj";
        final String modelFile = System.getProperty("user.dir") + File.separator + spherBodyObjPath;
        writeBodyFile(modelFile, 51, 100, this.bodyRadius / 1E3, flattening);

        this.meshProv = new ObjMeshLoader(modelFile);
        this.body = new StarConvexFacetBodyShape("My body", this.celestialBody.getRotatingFrame(IAUPoleModelType.TRUE),
            new ObjMeshLoader(modelFile));

        final double eps = 1E-6;
        Assert.assertEquals(8000., this.body.getEllipsoid(EllipsoidType.INNER_SPHERE).getARadius(), eps);
        Assert.assertEquals(10000., this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE).getARadius(), eps);
        Assert.assertEquals(10000., this.body.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getARadius(), eps);
        Assert.assertEquals(8000., this.body.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getCRadius(), eps);
        Assert.assertEquals(10000., this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getARadius(), eps);
        Assert.assertEquals(8000., this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getCRadius(), eps);
        Assert.assertEquals(10000., this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getARadius(), eps);
        Assert.assertEquals(8000., this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getCRadius(), eps);
    }

    /**
     * @testType UT
     *
     * @description check that distance to body is properly computed for various configurations:
     *              <ul>
     *              <li>Line at 10km from the body</li>
     *              <li>Body crossing (distance = 0)</li>
     *              </ul>
     *
     * @testPassCriteria distance to body is properly computed for various configurations (reference: math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void distanceToTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Case 1: tangent line 10km further
        final Vector3D position = new Vector3D(0, 0, 20E3);
        final Line line1 = new Line(position, position.add(Vector3D.PLUS_I));
        final double actual1 = this.body.distanceTo(line1, this.body.getBodyFrame(), date);
        Assert.assertEquals(10000, actual1, 0);

        // Case 2: crossing case
        final Line line2 = new Line(position, Vector3D.ZERO);
        final double actual2 = this.body.distanceTo(line2, this.body.getBodyFrame(), date);
        Assert.assertEquals(0, actual2, 0);
    }

    /**
     * @testType UT
     *
     * @description check that the getNeighbors method properly returns the neighbors triangles of
     *              provided triangle for various configurations:
     *              <ul>
     *              <li>0m distance: only initial triangle is included</li>
     *              <li>10m distance: only initial triangle is included</li>
     *              <li>100m distance: only initial triangle and neighbors are included</li>
     *              <li>14000m distance: half of all triangles are included</li>
     *              <li>20000m distance: all triangles are included</li>
     *              </ul>
     *              Also in the case of a provided order of "neighborhood":
     *              <ul>
     *              <li>Order 0: provided triangle is expected</li>
     *              <li>Order 1: provided triangle and immediate neighbors are expected</li>
     *              <li>Order 2: provided triangle and immediate neighbors as well as their immediate neighbors are
     *              expected</li>
     *              <li>Order 1 000 000: all triangles are expected</li>
     *              </ul>
     *
     * @testPassCriteria returned neighbors are as expected (reference: math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.11.1 (from version 4.11.1, the methods getNeighbors(Triangle, double) and
     *                       getNeighbors(Triangle, int) use a normal distance instead of a radial one to extract the
     *                       neighbors to the given triangle)
     */
    @Test
    public void neighborsTest() {
        // Initialization
        final Triangle triangle = this.body.getTriangles()[0];

        // getNeighbors(Triangle, distance)

        // Max distance = 0m: only initial triangle is included
        final List<Triangle> actual1 = this.body.getNeighbors(triangle, 0.);
        Assert.assertEquals(1, actual1.size());
        checkTriangles(actual1, triangle, 0);

        // Max distance = 10m: only initial triangle is included
        final List<Triangle> actual2 = this.body.getNeighbors(triangle, 10.);
        Assert.assertEquals(1, actual2.size());
        checkTriangles(actual2, triangle, 10);

        // Max distance = 20000m: all triangles are included
        final List<Triangle> actual3 = this.body.getNeighbors(triangle, 20000.);
        Assert.assertEquals(9800, actual3.size());
        checkTriangles(actual3, triangle, 20000);

        // Max distance = 14 000m: half of the bodies triangles are included
        final List<Triangle> actual3bis = this.body.getNeighbors(triangle, 10000 * MathLib.sqrt(2));
        Assert.assertEquals(4911, actual3bis.size());
        checkTriangles(actual3bis, triangle, 10000 * MathLib.sqrt(2));

        // Max distance = 100m: only first neighbors are included
        final List<Triangle> actual4 = this.body.getNeighbors(triangle, 100.);
        Assert.assertEquals(7, actual4.size());
        checkTriangles(actual4, triangle, 100);

        // getNeighbors(BodyPoint, distance)
        final OneAxisEllipsoid fittedEllipsoid = this.body.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID);

        // Max distance = this.body.getFittedEllipsoid()100m with far ellipsoid point: no neighbors are included
        final EllipsoidPoint point5 = new EllipsoidPoint(fittedEllipsoid, LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 1,
            2, 101, "");
        final List<Triangle> actual5 = this.body.getNeighbors(point5, 100.);
        Assert.assertEquals(0, actual5.size());
        checkTriangles(actual5, point5, 100);

        // Max distance = 100m with ellipsoid point 100m from north pole: no neighbors are included
        // (since triangle
        // center is taken into account)
        final EllipsoidPoint point6 = new EllipsoidPoint(fittedEllipsoid, LLHCoordinatesSystem.BODYCENTRIC_NORMAL,
            MathLib.PI / 2., 0, 100, "");
        final List<Triangle> actual6 = this.body.getNeighbors(point6, 100.);
        Assert.assertEquals(0, actual6.size());
        checkTriangles(actual6, point6, 100);

        // Max distance = 600m with ellipsoid point 100m from north pole: all north pole neighbors
        // are included
        final EllipsoidPoint point7 = new EllipsoidPoint(fittedEllipsoid, LLHCoordinatesSystem.BODYCENTRIC_NORMAL,
            MathLib.PI / 2., 0, 100, "");
        final List<Triangle> actual7 = this.body.getNeighbors(point7, 600.);
        Assert.assertEquals(100, actual7.size());
        checkTriangles(actual7, point7, 600);

        // getNeighbors(Triangle, order)

        // Max distance = 0 order: only initial triangle is included
        final List<Triangle> actual8 = this.body.getNeighbors(triangle, 0);
        Assert.assertEquals(1, actual8.size());
        checkTriangles(actual8, triangle, 0);

        // Max distance = 1 order: only initial triangle and first neighbors are included
        final List<Triangle> actual9 = this.body.getNeighbors(triangle, 1);
        Assert.assertEquals(4, actual9.size());
        checkTriangles(actual9, triangle, 500.);

        // Max distance = 2 orders: only initial triangle and first neighbors and their neightbors
        // are included
        final List<Triangle> actual10 = this.body.getNeighbors(triangle, 2);
        Assert.assertEquals(10, actual10.size());
        checkTriangles(actual10, triangle, 1000.);

        // Max distance = 1 000 000 orders: all triangles are included
        final List<Triangle> actual11 = this.body.getNeighbors(triangle, 1000000);
        Assert.assertEquals(9800, actual11.size());
        checkTriangles(actual11, triangle, 20000);

        // getNeighbors(BodyPoint, order)

        // Max distance = 0 order: only closest triangle is included
        final EllipsoidPoint point12 = new EllipsoidPoint(fittedEllipsoid, LLHCoordinatesSystem.ELLIPSODETIC,
            -FastMath.PI / 2., 0, 10, "");
        final List<Triangle> actual12 = this.body.getNeighbors(point12, 0);
        Assert.assertEquals(1, actual12.size());
        checkTriangles(actual12, triangle, 0);

        // Max distance = 1 order: only initial triangle and first neighbors are included
        final List<Triangle> actual13 = this.body.getNeighbors(point12, 1);
        Assert.assertEquals(4, actual13.size());
        checkTriangles(actual13, triangle, 500.);

        // Max distance = 2 orders: only initial triangle and first neighbors and their neightbors
        // are included
        final List<Triangle> actual14 = this.body.getNeighbors(point12, 2);
        Assert.assertEquals(10, actual14.size());
        checkTriangles(actual14, triangle, 1000.);

        // Max distance = 1 000 000 orders: all triangles are included
        final List<Triangle> actual15 = this.body.getNeighbors(point12, 1000000);
        Assert.assertEquals(9800, actual15.size());
        checkTriangles(actual15, triangle, 20000);

        // getNeighbors(Vector3D, distance)

        // Max distance = 100m with far ellipsoid point: no neighbors are included
        final Vector3D point16 = new Vector3D(10101, 0, 0);
        final List<Triangle> actual16 = this.body.getNeighbors(point16, 100.);
        Assert.assertEquals(0, actual16.size());
        checkTriangles(actual5, point16, 100);

        // Max distance = 100m with ellipsoid point 100m from north pole: no neighbors are included
        // (since triangle center is taken into account)
        final Vector3D point17 = new Vector3D(10100, 0, 0);
        final List<Triangle> actual17 = this.body.getNeighbors(point17, 100.);
        Assert.assertEquals(0, actual17.size());
        checkTriangles(actual17, point17, 100);

        // Max distance = 600m with ellipsoid point 100m from north pole: all north pole neighbors
        // are included
        final Vector3D point18 = new Vector3D(0, 0, 10100);
        final List<Triangle> actual18 = this.body.getNeighbors(point18, 600.);
        Assert.assertEquals(100, actual18.size());
        checkTriangles(actual18, point18, 600);

        // getNeighbors(Vector3D, order)

        // Max distance = 0 order: only closest triangle is included
        // Pos is exactly the same as GeodeticPoint(-Pi / 2, 0, 10). Exact value should be (0, 0,
        // -10010)
        final Vector3D point19 = new Vector3D(6.1293572297325E-13, 0, -10009.999999999996);
        final List<Triangle> actual19 = this.body.getNeighbors(point19, 0);
        Assert.assertEquals(1, actual19.size());
        checkTriangles(actual19, triangle, 0);

        // Max distance = 1 order: only initial triangle and first neighbors are included
        final List<Triangle> actual20 = this.body.getNeighbors(point19, 1);
        Assert.assertEquals(4, actual20.size());
        checkTriangles(actual20, triangle, 500.);

        // Max distance = 2 orders: only initial triangle and first neighbors and their neightbors
        // are included
        final List<Triangle> actual21 = this.body.getNeighbors(point19, 2);
        Assert.assertEquals(10, actual21.size());
        checkTriangles(actual21, triangle, 1000.);

        // Max distance = 1 000 000 orders: all triangles are included
        final List<Triangle> actual22 = this.body.getNeighbors(point19, 1000000);
        Assert.assertEquals(9800, actual22.size());
        checkTriangles(actual22, triangle, 20000);
    }

    /**
     * @testType UT
     *
     * @description check that field data are properly computed: list of visible triangles, visible
     *              surface and contour in different cases (infinite field of view, narrow field of view and standard
     *              field of view):
     *              <ul>
     *              <li>Infinite field of view: half of the body is supposed to be seen</li>
     *              <li>Infinitely small field of view: none of the body should be seen</li>
     *              <li>Standard field of view (from North pole with a field of view such that latitudes larger than
     *              60&deg; are covered), Fast algorithm</li>
     *              </ul>
     *
     * @testPassCriteria returned field data are as expected (reference: math, threshold 1E-2, limited due to limited
     *                   number of vertices)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void fieldDataTest() throws PatriusException {
        // State
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 1E5);
        final Attitude attitude = attitudeProvider.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Case 1: infinite field of view: half of the body should be seen
        final IFieldOfView fieldOfView1 = new CircularField("", MathLib.PI / 2., Vector3D.PLUS_K);
        final FieldData actual1 = this.body.getFieldData(state, fieldOfView1, null);
        // Checks
        Assert.assertEquals(this.body.getTriangles().length / 2, actual1.getVisibleTriangles().size());
        final double referenceSurface = 4. * MathLib.PI * this.bodyRadius * this.bodyRadius / 2.;
        Assert.assertEquals(0., (referenceSurface - actual1.getVisibleSurface()) / referenceSurface, 1E-3);
        Assert.assertEquals(0, actual1.getDate().durationFrom(state.getDate()), 0.);
        final List<FacetPoint> contour1 = actual1.getContour();
        Assert.assertEquals(100, contour1.size());
        for (final FacetPoint point : contour1) {
            Assert.assertEquals(0., point.getLLHCoordinates().getLatitude(), 1e-16);
            Assert.assertEquals(0., point.getLLHCoordinates().getHeight() / this.bodyRadius, 1E-10);
        }

        // Case 2: 0deg field of view: none of the body should be seen
        final IFieldOfView fieldOfView2 = new CircularField("", 1.0e-13, Vector3D.PLUS_K);
        final FieldData actual2 = this.body.getFieldData(state, fieldOfView2, null);
        // Checks
        Assert.assertEquals(0, actual2.getVisibleTriangles().size());
        Assert.assertEquals(0., actual2.getVisibleSurface(), 0);
        Assert.assertEquals(0, actual2.getContour().size());

        // Case 3: 5000/20000E3 field of view : half of the visible body should be seen (annulus of
        // latitude 60deg)
        final IFieldOfView fieldOfView3 = new CircularField("", MathLib.atan(5000. / 20000E3), Vector3D.PLUS_K);
        final FieldData actual3 = this.body.getFieldData(state, fieldOfView3, Vector3D.PLUS_K);
        // Checks (first point)
        Assert.assertEquals(1500, actual3.getVisibleTriangles().size());
        final double referenceSurface3 = 2. * MathLib.PI * this.bodyRadius * this.bodyRadius
                * (1 - MathLib.sqrt(3) / 2.);
        Assert.assertEquals(0., (referenceSurface3 - actual3.getVisibleSurface()) / referenceSurface, 2E-2);
        // Visible triangles should have a Z coordinate > 8600m (above annulus of latitude 60deg)
        for (int i = 0; i < actual3.getVisibleTriangles().size(); i++) {
            // 5m margin to account for limited number of vertices (center of triangle is not
            // exactly on the body sphere
            // but a few meters outside)
            Assert.assertTrue(actual3.getVisibleTriangles().get(i).getCenter().getZ() >= MathLib.sqrt(3) / 2.
                    * this.bodyRadius - 5);
        }
        // Contour should have 100 points of latitude 60deg and altitude 10 000m
        final List<FacetPoint> contour3 = actual3.getContour();
        Assert.assertEquals(100, contour3.size());
        final double referenceLatitude3 = MathLib.toRadians(60.);
        for (final FacetPoint point : contour3) {
            Assert.assertEquals(0.,
                (point.getLLHCoordinates().getLatitude() - referenceLatitude3) / referenceLatitude3, 5E-2);
            Assert.assertEquals(0., point.getLLHCoordinates().getHeight() / this.bodyRadius, 1E-10);
        }
    }

    /**
     * @testType UT
     *
     * @description Build a FieldData from several visible triangles selected from an external analysis to consider
     *              multiple cases: a contour vertex can have 0, 1, 2, ..., up to 4 neighbors.<br>
     *              Then, extract the computed contour and check it is the same as expected (the expected contour
     *              vertices are defined from an external analysis).
     * 
     * @testPassCriteria returned the expected contour
     *
     * @referenceVersion 4.11.1
     *
     * @nonRegressionVersion 4.11.1
     */
    @Test
    public void contourTest() throws PatriusException {

        // State
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 1E5);
        final Attitude attitude = attitudeProvider.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Infinite field of view: half of the body should be seen (from fieldDataTest())
        final IFieldOfView fieldOfView = new CircularField("", MathLib.PI / 2., Vector3D.PLUS_K);

        // Initialize a reference FieldData to extract the reference visible triangles
        final FieldData refFieldData = this.body.getFieldData(state, fieldOfView, null);
        final List<Triangle> refTriangles = refFieldData.getVisibleTriangles();

        /*
         * From this list, we select the following triangles such as (from an external analysis):
         * -4903: vertices[2403: 0 neighbor, contour ; 2503: 4 neighbors, contour ; 2404: 3 neighbors, contour]
         * -4904: vertices[2504: 6 neighbors, inside ; 2503: 4 neighbors, contour ; 2404: 3 neighbors, contour]
         * -4905: vertices[2504: 6 neighbors, inside ; 2503: 4 neighbors, contour ; 2405: 2 neighbors, contour]
         * -4906: vertices[2504: 6 neighbors, inside ; 2505: 2 neighbors, contour ; 2405: 2 neighbors, contour]
         * -5102: vertices[2602: 0 neighbor, contour ; 2503: 4 neighbors, contour ; 2603: 3 neighbors, contour]
         * -5103: vertices[2504: 6 neighbors, inside ; 2503: 4 neighbors, contour ; 2603: 3 neighbors, contour]
         * -5104: vertices[2504: 6 neighbors, inside ; 2604: 2 neighbors, contour ; 2603: 3 neighbors, contour]
         * -5105: vertices[2504: 6 neighbors, inside ; 2604: 2 neighbors, contour ; 2505: 2 neighbors, contour]
         */
        final List<Integer> trianglesID = Arrays.asList(4903, 4904, 4905, 4906, 5102, 5103, 5104, 5105);

        // Extract the selected triangles for the analysis
        final List<Triangle> triangles = new ArrayList<>();
        for (final Triangle triangle : refTriangles) {
            if (trianglesID.contains(triangle.getID())) {
                triangles.add(triangle);
            }
        }

        // Build the analysis field data with the selected triangles
        final FieldData actualBis = new FieldData(state.getDate(), triangles, this.body);

        // Extract the computed contour
        final List<FacetPoint> contour = actualBis.getContour();

        // From the selected triangles, the external analysis has shown that the following vertices should define the
        // contour
        final List<Integer> vertexID = Arrays.asList(2604, 2603, 2403, 2404, 2405, 2505, 2602, 2503);

        // Extract the expected contour vertices
        final Set<Vertex> expectedVectex = new HashSet<>();
        for (final Triangle triangle : refTriangles) {
            final Vertex[] vertices = triangle.getVertices();
            for (final Vertex vertex : vertices) {
                if (vertexID.contains(vertex.getID())) {
                    expectedVectex.add(vertex);
                }
            }
        }

        // Convert them in FacetPoint as the expected contour
        final List<FacetPoint> expectedContour = new ArrayList<>();
        for (final Vertex vertex : expectedVectex) {
            final FacetPoint point = this.body.buildPoint(vertex.getPosition(), "");
            expectedContour.add(point);
        }

        // Check that the computed coutour defines all the points in the expected one
        // Implementation note: as FacetPoint doesn't override equals, we can't use containAll as the points in the
        // two lists are stored in different instances
        Assert.assertEquals(expectedContour.size(), contour.size());
        for (final FacetPoint contour1 : contour) {
            boolean equals = false;
            for (final FacetPoint contour2 : expectedContour) {
                if (Math.abs(contour1.getLLHCoordinates().getLatitude() - contour2.getLLHCoordinates().getLatitude()) < EPS
                        && Math.abs(contour1.getLLHCoordinates().getLongitude()
                                - contour2.getLLHCoordinates().getLongitude()) < EPS
                        && Math
                            .abs(contour1.getLLHCoordinates().getHeight() - contour2.getLLHCoordinates().getHeight()) < EPS) {
                    equals = true;
                    break;
                }
            }
            Assert.assertTrue(equals);
        }
    }

    /**
     * @testType UT
     *
     * @description Build a FieldData with four vertices / two triangles.
     *              Then check the contour is well built with a continuous chain.
     * 
     * @testPassCriteria returned the expected contour
     *
     * @referenceVersion 4.13
     *
     * @nonRegressionVersion 4.13
     */
    @Test
    public void contourSpecialCaseTest() {

        // Build 4 vertices such as: v0[-0.1 ; 0], v1[0 ; -0.5], v2[0.1 ; 0], v3[0 ; 0.4]
        // Note: some vertices are closer on purpose to check the contour continuity is well managed
        final Vertex v0 = new Vertex(0, this.body.buildPoint(LLHCoordinatesSystem.BODYCENTRIC_NORMAL,
            MathLib.toRadians(-0.1), MathLib.toRadians(0.), 0., "").getPosition());
        final Vertex v1 = new Vertex(1, this.body.buildPoint(LLHCoordinatesSystem.BODYCENTRIC_NORMAL,
            MathLib.toRadians(0.), MathLib.toRadians(-0.5), 0., "").getPosition());
        final Vertex v2 = new Vertex(2, this.body.buildPoint(LLHCoordinatesSystem.BODYCENTRIC_NORMAL,
            MathLib.toRadians(0.1), MathLib.toRadians(0.), 0., "").getPosition());
        final Vertex v3 = new Vertex(3, this.body.buildPoint(LLHCoordinatesSystem.BODYCENTRIC_NORMAL,
            MathLib.toRadians(0.), MathLib.toRadians(0.4), 0., "").getPosition());

        // Case #1: horizontal separation between the two triangles
        Triangle t1 = new Triangle(11, v1, v2, v3); // Top triangle
        Triangle t2 = new Triangle(22, v1, v0, v3); // Below triangle
        List<Triangle> visibleTriangles = Arrays.asList(t1, t2);

        // Build the field data
        FieldData fieldData = new FieldData(null, visibleTriangles, this.body);

        // Extract the contour and check it is continuous (expected vertices order: 1-2-3-0)
        List<FacetPoint> contour = fieldData.getContour();
        Assert.assertEquals("point_1", contour.get(0).getName());
        Assert.assertEquals("point_2", contour.get(1).getName());
        Assert.assertEquals("point_3", contour.get(2).getName());
        Assert.assertEquals("point_0", contour.get(3).getName());

        // Case #2: vertical separation between the two triangles
        t1 = new Triangle(11, v0, v1, v2); // Left triangle
        t2 = new Triangle(22, v0, v2, v3); // Right triangle
        visibleTriangles = Arrays.asList(t1, t2);

        // Build the field data
        fieldData = new FieldData(null, visibleTriangles, this.body);

        // Extract the contour and check it is continuous (expected vertices order: 0-1-2-3)
        contour = fieldData.getContour();
        Assert.assertEquals("point_0", contour.get(0).getName());
        Assert.assertEquals("point_1", contour.get(1).getName());
        Assert.assertEquals("point_2", contour.get(2).getName());
        Assert.assertEquals("point_3", contour.get(3).getName());
    }

    /**
     * @testType UT
     *
     * @description check that list of never visible triangles is properly computed in some various
     *              cases:
     *              <ul>
     *              <li>1 point ephemeris with far satellite: half of the body should be seen</li>
     *              <li>2 opposite points ephemeris with far satellite: no points are hidden</li>
     *              <li>2 points with Pi/2 between each: 3/4 of the body should be seen</li>
     *              </ul>
     *
     * @testPassCriteria returned lists of never visible triangles are as expected (reference: math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void neverVisibleTrianglesTest() throws PatriusException {
        // Case 1: one point ephemeris: half of the body should be seen

        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 1; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2.,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU)
                .shiftedBy(i);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }

        final List<Triangle> ephemeris1 = this.body.getNeverVisibleTriangles(statesList, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(4900, ephemeris1.size());

        // Case 2: two opposite points ephemeris: all of the body should be seen, no permanently
        // hidden triangle

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList2 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList2.add(new SpacecraftState(orbit, attitude));
        }

        final List<Triangle> ephemeris2 = this.body.getNeverVisibleTriangles(statesList2, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(0, ephemeris2.size());

        // Case 3: two points ephemeris with Pi/2 angle between each: 3/4 of the body should be seen

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList3 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI / 2.
                    * i, PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList3.add(new SpacecraftState(orbit, attitude));
        }

        final List<Triangle> ephemeris3 = this.body.getNeverVisibleTriangles(statesList3, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(2450, ephemeris3.size());
    }

    /**
     * @testType UT
     *
     * @description check that list of never enlightened triangles is properly computed in some
     *              various cases:
     *              <ul>
     *              <li>1 point ephemeris with far satellite: half of the body should be seen</li>
     *              <li>2 opposite points ephemeris with far satellite: no points are hidden</li>
     *              <li>2 points with Pi/2 between each: 3/4 of the body should be seen</li>
     *              </ul>
     *
     * @testPassCriteria returned lists of never enlightened triangles are as expected (reference:
     *                   math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void neverEnlightenedTrianglesTest() throws PatriusException {
        // Case 1: one point ephemeris: half of the body should be seen

        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<>();
        final List<AbsoluteDate> datesList = new ArrayList<>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 1; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2.,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU)
                .shiftedBy(i);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
            datesList.add(orbit.getDate());
        }
        final PVCoordinatesProvider sun1 = statesList.get(0).getOrbit();

        final List<Triangle> ephemeris1 = this.body.getNeverEnlightenedTriangles(datesList, sun1);
        // Checks
        Assert.assertEquals(4900, ephemeris1.size());

        // Case 2: two opposite points ephemeris: all of the body should be seen, no permanently
        // hidden triangle

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList2 = new ArrayList<>();
        final List<AbsoluteDate> datesList2 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i),
                Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList2.add(new SpacecraftState(orbit, attitude));
            datesList2.add(orbit.getDate());
        }
        final PVCoordinatesProvider sun2 = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1424986854737879063L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI), 0, MathLib.cos(dt * MathLib.PI))
                    .scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris2 = this.body.getNeverEnlightenedTriangles(datesList2, sun2);
        // Checks
        Assert.assertEquals(0, ephemeris2.size());

        // Case 3: two points ephemeris with Pi/2 angle between each: 3/4 of the body should be seen

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList3 = new ArrayList<>();
        final List<AbsoluteDate> datesList3 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI / 2.
                    * i, PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i),
                Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList3.add(new SpacecraftState(orbit, attitude));
            datesList3.add(orbit.getDate());
        }
        final PVCoordinatesProvider sun3 = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7207356693993459470L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI / 2), 0, MathLib.cos(dt * MathLib.PI / 2))
                    .scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris3 = this.body.getNeverEnlightenedTriangles(datesList3, sun3);
        // Checks
        Assert.assertEquals(2450, ephemeris3.size());
    }

    /**
     * @testType UT
     *
     * @description check that list of visible and enlightened triangles is properly computed in some various cases:
     *              <ul>
     *              <li>1 point ephemeris with far satellite, Sun at same location: half of the body should be seen</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at same location, omni-directional FOV: no
     *              points are hidden</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at opposite location, omni-directional FOV:
     *              no points are visible</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at same location, 0deg circular field of
     *              view: no points are visible</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at same location, 90deg circular field of
     *              view with opposite direction: no points are visible</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at initial ephemeris location, standard
     *              field of view (from North pole with a field of view such that latitudes larger than 60&deg; are
     *              covered): half of the body should be seen</li>
     *              </ul>
     *
     * @testPassCriteria returned lists of visible and enlightened triangles are as expected
     *                   (reference: math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void visibleAndEnlightenedTrianglesTest() throws PatriusException {
        // Case 1: one point ephemeris, Sun at same location: half of the body should be seen

        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 1; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2.,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU)
                .shiftedBy(i);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun1 = statesList.get(0).getOrbit();

        final List<Triangle> ephemeris1 = this.body.getVisibleAndEnlightenedTriangles(statesList, sun1,
            new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(4900, ephemeris1.size());

        // Case 2: two opposite points ephemeris, Sun at same location: all of the body should be
        // seen, no permanently hidden triangle

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList2 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i),
                Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList2.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun2 = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 6446447939729655569L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI), 0, MathLib.cos(dt * MathLib.PI))
                    .scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris2 = this.body.getVisibleAndEnlightenedTriangles(statesList2, sun2,
            new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(9800, ephemeris2.size());

        // Same check with 0deg field of view: 0 visible and enlightened triangles at the same time
        final List<Triangle> ephemeris2bis = this.body.getVisibleAndEnlightenedTriangles(statesList2, sun2,
            new CircularField("", 1E-6, Vector3D.PLUS_K));
        Assert.assertEquals(0, ephemeris2bis.size());
        // Same check with opposite field of view: 0 visible and enlightened triangles at the same
        // time
        final List<Triangle> ephemeris2ter = this.body.getVisibleAndEnlightenedTriangles(statesList2, sun2,
            new CircularField("", MathLib.PI / 2., Vector3D.MINUS_K));
        Assert.assertEquals(0, ephemeris2ter.size());

        // Case 3: two opposite points ephemeris, Sun at opposite location: no visible and
        // enlightened triangles at the same time

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList3 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i),
                Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList3.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun3 = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -9009174894475393698L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI), 0, -MathLib.cos(dt * MathLib.PI))
                    .scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris3 = this.body.getVisibleAndEnlightenedTriangles(statesList3, sun3,
            new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(0, ephemeris3.size());

        // Case 4: two opposite points ephemeris with far satellite, Sun at initial ephemeris location, standard field
        // of view (from North pole with a field of view such that latitudes larger than 60&deg; are covered): half of
        // the body should be seen

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList4 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i),
                Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList4.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun4 = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -9084741184985812991L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final Vector3D pos = new Vector3D(0, 0, 1).scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final IFieldOfView fieldOfView4 = new CircularField("", MathLib.atan(5000. / 20000E3), Vector3D.PLUS_K);

        final List<Triangle> ephemeris4 = this.body.getVisibleAndEnlightenedTriangles(statesList4, sun4, fieldOfView4);
        // Checks
        Assert.assertEquals(1500, ephemeris4.size());
    }

    /**
     * @testType UT
     *
     * @description check all class exceptions: failed to load mesh, unavailable methods.
     *
     * @testPassCriteria exceptions are thrown as expected
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void exceptionsTest() {
        try {
            new StarConvexFacetBodyShape("", this.celestialBody.getRotatingFrame(IAUPoleModelType.TRUE),
                new ObjMeshLoader("Dummy.obj"));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new StarConvexFacetBodyShape("", this.celestialBody.getRotatingFrame(IAUPoleModelType.TRUE),
                new GeodeticMeshLoader("Dummy.tab"));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     *
     * @description check that the ID of the triangles from a geodetic mesh are as expected.
     *
     * @testPassCriteria the ID of the triangles from a geodetic mesh are as expected.
     *
     * @throws URISyntaxException if the URL is not formatted strictly according to to RFC2396 and
     *         cannot be converted to a URI.
     * @throws PatriusException if the loading of the geodetic mesh fails
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void geodeticMeshTriangleIdTest() throws URISyntaxException, PatriusException {
        // Load geodetic mesh
        final String modelFile = "mnt" + File.separator + "m1phobos.tab";
        final String fullName = StarConvexFacetBodyShape.class.getClassLoader().getResource(modelFile).toURI()
            .getPath();
        final GeodeticMeshLoader loader = new GeodeticMeshLoader(fullName);

        Assert.assertEquals(32070, loader.getTriangles().length);
        Assert.assertEquals(1, loader.getTriangles()[0].getID());
        Assert.assertEquals(loader.getTriangles().length,
            loader.getTriangles()[loader.getTriangles().length - 1].getID());
    }

    /**
     * @testType UT
     *
     * @description check that {@link EclipseDetector} can be used together with {@link Propagator} and
     *              {@link FacetCelestialBody}.
     *              Detected eclipse dates must be similar to dates obtained with a same-size {@link OneAxisEllipsoid}.
     *
     * @testPassCriteria eclipse dates are as expected (reference: propagation with {@link OneAxisEllipsoid},
     *                   relative threshold: 1E-14)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void eclipseDetectorTest() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(15000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date,
            Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = date.shiftedBy(Constants.JULIAN_DAY);

        // Reference: a = 10 000m, f = 0
        final OneAxisEllipsoid reference = new OneAxisEllipsoid(10000, 0, FramesFactory.getGCRF(), "");
        final KeplerianPropagator propagatorRef = new KeplerianPropagator(orbit);
        final List<AbsoluteDate> referenceDates = new ArrayList<>();
        propagatorRef.addEventDetector(new EclipseDetector(new MeeusSun(), 700000E3, reference, 0, 600, 1E-14,
            Action.CONTINUE, Action.CONTINUE){
            /** Serializable UID. */
            private static final long serialVersionUID = -129410259435002540L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
                referenceDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorRef.propagate(finalDate);

        // Actual data
        final KeplerianPropagator propagatorAct = new KeplerianPropagator(orbit);
        final List<AbsoluteDate> actualDates = new ArrayList<>();
        propagatorAct.addEventDetector(new EclipseDetector(new MeeusSun(), 700000E3, this.body, 0, 600, 1E-14,
            Action.CONTINUE, Action.CONTINUE){
            /** Serializable UID. */
            private static final long serialVersionUID = 1756731750080967659L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
                actualDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorAct.propagate(finalDate);

        // Check detected dates
        for (int i = 0; i < referenceDates.size(); i++) {
            Assert.assertEquals(0, referenceDates.get(i).durationFrom(actualDates.get(i)), 1E-14);
        }
    }

    /**
     * @testType UT
     *
     * @description Check that {@link SensorModel} can be used together with {@link Propagator} and
     *              {@link FacetCelestialBody}.
     *              Detected sensor visibility dates must be similar to dates obtained with a same-size
     *              {@link OneAxisEllipsoid}.
     *
     * @testPassCriteria sensor visibility dates are as expected (reference: propagation with {@link OneAxisEllipsoid},
     *                   absolute threshold: 1E-4, limited due to limited number of vertices)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void sensorModelTest() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(15000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date,
            Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = date.shiftedBy(Constants.JULIAN_DAY / 2.);
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        final SpacecraftState state = new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit));

        // Reference: a = 10 000m, f = 0
        final OneAxisEllipsoid reference = new OneAxisEllipsoid(10000, 0, FramesFactory.getGCRF(), "");

        // Assembly reference
        final AssemblyBuilder builderRef = new AssemblyBuilder();
        builderRef.addMainPart("Main");
        final SensorProperty sensorPropertyRef = new SensorProperty(Vector3D.PLUS_K);
        sensorPropertyRef.setMainFieldOfView(new CircularField("", MathLib.PI / 4., Vector3D.PLUS_K));
        sensorPropertyRef.setMainTarget(new MeeusSun(), new ConstantRadiusProvider(700000E3));
        builderRef.addProperty(sensorPropertyRef, "Main");
        builderRef.initMainPartFrame(state);
        final Assembly assemblyRef = builderRef.returnAssembly();

        final KeplerianPropagator propagatorRef = new KeplerianPropagator(orbit);
        propagatorRef.setAttitudeProvider(attitudeProvider);
        final SensorModel sensorModelRef = new SensorModel(assemblyRef, "Main");
        sensorModelRef.addMaskingCelestialBody(reference);
        final List<AbsoluteDate> referenceDates = new ArrayList<>();
        propagatorRef.addEventDetector(new SensorVisibilityDetector(sensorModelRef, 600, 1E-14, Action.CONTINUE,
            Action.CONTINUE){
            /** Serializable UID. */
            private static final long serialVersionUID = 5656497617994239180L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
                referenceDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorRef.propagate(finalDate);

        // Actual data

        // Assembly actual
        final AssemblyBuilder builderAct = new AssemblyBuilder();
        builderAct.addMainPart("Main2");
        final SensorProperty sensorPropertyAct = new SensorProperty(Vector3D.PLUS_K);
        sensorPropertyAct.setMainFieldOfView(new CircularField("", MathLib.PI / 4., Vector3D.PLUS_K));
        sensorPropertyAct.setMainTarget(new MeeusSun(), new ConstantRadiusProvider(700000E3));
        builderAct.addProperty(sensorPropertyAct, "Main2");
        builderAct.initMainPartFrame(state);
        final Assembly assemblyAct = builderAct.returnAssembly();

        final KeplerianPropagator propagatorAct = new KeplerianPropagator(orbit);
        propagatorAct.setAttitudeProvider(attitudeProvider);
        final SensorModel sensorModelAct = new SensorModel(assemblyAct, "Main2");
        sensorModelAct.addMaskingCelestialBody(this.body);
        final List<AbsoluteDate> actualDates = new ArrayList<>();
        propagatorAct.addEventDetector(new SensorVisibilityDetector(sensorModelAct, 600, 1E-14, Action.CONTINUE,
            Action.CONTINUE){
            /** Serializable UID. */
            private static final long serialVersionUID = 537948557020608214L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
                actualDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorAct.propagate(finalDate);

        // Check detected dates
        for (int i = 0; i < referenceDates.size(); i++) {
            Assert.assertEquals(0, referenceDates.get(i).durationFrom(actualDates.get(i)), 1E-4);
        }
    }

    /**
     * @testType UT
     *
     * @description check that min/max norm of body is properly computed
     *
     * @testPassCriteria min/max norm is as expected (10000m) (reference: math)
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public void minmaxNormTest() {
        final double expected = 10000;
        Assert.assertEquals(0, (expected - this.body.getMinNorm()) / expected, EPS);
        Assert.assertEquals(0, (expected - this.body.getMaxNorm()) / expected, EPS);
    }

    /**
     * @testType UT
     *
     * @description check that inner ellipsoid is as expected.
     *
     * @testPassCriteria inner ellipsoid is an ellipsoid of radius, the radius and the flattening of the ellipsoidal
     *                   body (reference math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void innerEllipsoidTest() {
        final OneAxisEllipsoid innerEllipsoid = this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID);
        Assert.assertEquals(0., (innerEllipsoid.getEquatorialRadius() - this.bodyRadius) / this.bodyRadius, EPS);
        Assert.assertEquals(0., innerEllipsoid.getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening(),
            innerEllipsoid.getFlattening(), EPS);
    }

    /**
     * @testType UT
     *
     * @description check that outer ellipsoid is as expected.
     *
     * @testPassCriteria outer ellipsoid is an ellipsoid of radius, the radius and the flattening of the ellipsoidal
     *                   body (reference math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void outerEllipsoidTest() {
        final OneAxisEllipsoid outerEllipsoid = this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID);
        Assert.assertEquals(0., (outerEllipsoid.getEquatorialRadius() - this.bodyRadius) / this.bodyRadius, EPS);
        Assert.assertEquals(0., outerEllipsoid.getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening(),
            outerEllipsoid.getFlattening(), EPS);
    }

    /**
     * @testType UT
     *
     * @description check that inner sphere is as expected.
     *
     * @testPassCriteria inner sphere is a sphere of radius, the radius and the flattening of the spherical body
     *                   (reference math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void innerSphereTest() {
        final OneAxisEllipsoid innerSphere = this.body.getEllipsoid(EllipsoidType.INNER_SPHERE);
        Assert.assertEquals(0., (innerSphere.getEquatorialRadius() - this.bodyRadius) / this.bodyRadius, EPS);
        Assert.assertEquals(0., innerSphere.getFlattening(), EPS);
    }

    /**
     * @testType UT
     *
     * @description check that outer sphere is as expected.
     *
     * @testPassCriteria outer sphere is a sphere of radius, the radius and the flattening of the spherical body
     *                   (reference math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void outerSphereTest() {
        final OneAxisEllipsoid outerSphere = this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE);
        Assert.assertEquals(0., (outerSphere.getEquatorialRadius() - this.bodyRadius) / this.bodyRadius, EPS);
        Assert.assertEquals(0., outerSphere.getFlattening(), EPS);
    }

    /**
     * @testType UT
     *
     * @description check that the resized body sphere is as expected.
     *
     * @testPassCriteria resized body differs from the original body by the defined margin value and type (reference
     *                   math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     *
     * @throws PatriusException if the margin value is invalid
     */
    @Test
    public void resizeTest() throws PatriusException {
        // Case with a zero margin distance
        final double marginValue2 = 0.;
        final FacetBodyShape body2 = this.body.resize(MarginType.DISTANCE, marginValue2);
        checkTriangles(body2.getTriangles(), MarginType.DISTANCE, marginValue2);
        // Case with a positive margin distance
        final double marginValue3 = 1E3;
        final FacetBodyShape body3 = this.body.resize(MarginType.DISTANCE, marginValue3);
        checkTriangles(body3.getTriangles(), MarginType.DISTANCE, marginValue3);
        // Case with a negative margin distance whose absolute value is smaller than the minimal
        // norm
        final double marginValue4 = -this.body.getMinNorm() + 1E3;
        final FacetBodyShape body4 = this.body.resize(MarginType.DISTANCE, marginValue4);
        checkTriangles(body4.getTriangles(), MarginType.DISTANCE, marginValue4);
        // Case with a negative margin distance whose absolute value is larger than the minimal norm
        final double marginValue4b = -this.body.getMinNorm() - 1E3;
        try {
            this.body.resize(MarginType.DISTANCE, marginValue4b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a zero margin scale factor
        final double marginValue5 = 0.;
        try {
            this.body.resize(MarginType.SCALE_FACTOR, marginValue5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a positive margin scale factor smaller than 1
        final double marginValue6 = 0.5;
        final FacetBodyShape body6 = this.body.resize(MarginType.SCALE_FACTOR, marginValue6);
        checkTriangles(body6.getTriangles(), MarginType.SCALE_FACTOR, marginValue6);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_SPHERE).getEquatorialRadius() * marginValue6,
            body6.getEllipsoid(EllipsoidType.INNER_SPHERE).getEquatorialRadius(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_SPHERE).getFlattening(),
            body6.getEllipsoid(EllipsoidType.INNER_SPHERE).getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius() * marginValue6,
            body6.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE).getFlattening(),
            body6.getEllipsoid(EllipsoidType.OUTER_SPHERE).getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getEquatorialRadius() * marginValue6,
            body6.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getEquatorialRadius(), EPS_OPTIMIZER);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getFlattening(),
            body6.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius() * marginValue6,
            body6.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getFlattening(),
            body6.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getFlattening(), EPS);
        // Case with a positive margin scale factor larger than 1
        final double marginValue7 = 2.;
        final FacetBodyShape body7 = this.body.resize(MarginType.SCALE_FACTOR, marginValue7);
        checkTriangles(body7.getTriangles(), MarginType.SCALE_FACTOR, marginValue7);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_SPHERE).getEquatorialRadius() * marginValue7,
            body7.getEllipsoid(EllipsoidType.INNER_SPHERE).getEquatorialRadius(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_SPHERE).getFlattening(),
            body7.getEllipsoid(EllipsoidType.INNER_SPHERE).getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius() * marginValue7,
            body7.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE).getFlattening(),
            body7.getEllipsoid(EllipsoidType.OUTER_SPHERE).getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getEquatorialRadius() * marginValue7,
            body7.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getEquatorialRadius(), EPS_OPTIMIZER);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getFlattening(),
            body7.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getFlattening(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius() * marginValue7,
            body7.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius(), EPS);
        Assert.assertEquals(this.body.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getFlattening(),
            body7.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getFlattening(), EPS);
        // Case with a negative margin scale
        final double marginValue8 = -0.5;
        try {
            this.body.resize(MarginType.SCALE_FACTOR, marginValue8);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     *
     * @description check that the returned apparent radius is as expected.
     *
     * @testPassCriteria returned apparent radius coincides with the radius of the outer sphere (reference math),
     *                   no exception thrown in case of spacecrat below body surface 
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     *
     * @throws PatriusException
     *         if the {@link PVCoordinatesProvider} computation fails
     *         if the position cannot be computed in the given frame
     *         if the line cannot be converted to the body frame
     */
    @Test
    public void apparentRadiusTest() throws PatriusException {
        // Set data root
        Utils.setDataRoot("regular-data");
        // Define date
        final AbsoluteDate date = new AbsoluteDate();
        // Define frame
        final Frame frame = this.body.getBodyFrame();
        // Define body position
        final Vector3D bodyPosition = this.body.getPVCoordinates(date, frame).getPosition();
        // Define distance vector between body and spacecraft
        final Vector3D distance = new Vector3D(1E4, 2E4, 3E4);
        // Define spacecraft position
        final Vector3D scPosition = bodyPosition.add(distance);
        // Retrieve celestial body
        final PVCoordinatesProvider pvCoordinatesCel = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 5181126612404413097L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(11E3, 12E3, 13E3), Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) {
                return FacetBodyShapeTest.this.body.getBodyFrame();
            }
        };
        // Compute the apparent radius
        final double apparentRadius = this.body.getApparentRadius(new ConstantPVCoordinatesProvider(scPosition, frame),
            date, pvCoordinatesCel, PropagationDelayType.INSTANTANEOUS);
        // Retrieve the expected radius
        final double expectedRadius = this.body.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius();
        // Check that the apparent radius coincides with the radius of the outer sphere
        Assert.assertEquals(0., (apparentRadius - expectedRadius) / expectedRadius, EPS);

        // Light speed case (no impact since this is a sphere)
        this.body.setEpsilonSignalPropagation(1E-15);
        final double apparentRadius2 = this.body.getApparentRadius(
            new ConstantPVCoordinatesProvider(scPosition, frame), date, pvCoordinatesCel,
            PropagationDelayType.LIGHT_SPEED);
        // Check that the apparent radius coincides with the radius of the outer sphere
        Assert.assertEquals(0., (apparentRadius2 - expectedRadius) / expectedRadius, EPS);
        
        // Test case of spacecraft below body surface (no exception thrown)
        try {
        this.body.getApparentRadius(
                new ConstantPVCoordinatesProvider(Vector3D.PLUS_I, frame), date, pvCoordinatesCel,
                PropagationDelayType.LIGHT_SPEED);
        Assert.assertTrue(true);
        } catch (ArithmeticException e) {
            Assert.fail();
        }
        
    }

    /**
     * Check that provided list of triangles is within provided distance of provided triangle.
     *
     * @param triangles
     *        triangles list to check
     * @param triangle
     *        triangle
     * @param maxDistance
     *        max distance
     */
    private static void
        checkTriangles(final List<Triangle> triangles, final Triangle triangle, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(triangle.getCenter()) <= maxDistance);
        }
    }

    /**
     * Check that provided list of triangles is within provided distance of provided geodetic point.
     *
     * @param triangles
     *        triangles list to check
     * @param point
     *        point
     * @param maxDistance
     *        max distance
     */
    private static void checkTriangles(final List<Triangle> triangles, final EllipsoidPoint point,
                                       final double maxDistance) {
        final Vector3D position = point.getPosition();
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(position) <= maxDistance);
        }
    }

    /**
     * Check that provided list of triangles is within provided distance of provided 3D point.
     *
     * @param triangles
     *        triangles list to check
     * @param position
     *        3D position
     * @param maxDistance
     *        max distance
     */
    private static void
        checkTriangles(final List<Triangle> triangles, final Vector3D position, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(position) <= maxDistance);
        }
    }

    /**
     * Check that the provided array of modified triangles differ with respect to the original one
     * by the given margin value of the specified margin type.
     *
     * @param modifiedTriangles
     *        array of modified triangles to check
     * @param marginType
     *        margin type
     * @param marginValue
     *        margin value
     */
    private void checkTriangles(final Triangle[] modifiedTriangles, final MarginType marginType,
                                final double marginValue) {
        final Triangle[] originalTriangles = this.body.getTriangles();
        if (marginType.equals(MarginType.DISTANCE)) {
            for (int indexTriangle = 0; indexTriangle < modifiedTriangles.length; indexTriangle++) {
                final Triangle originalTriangle = originalTriangles[indexTriangle];
                final Triangle modifiedTriangle = modifiedTriangles[indexTriangle];
                final Vertex[] originalVertices = originalTriangle.getVertices();
                final Vertex[] modifiedVertices = modifiedTriangle.getVertices();
                for (int indexVertex = 0; indexVertex < modifiedVertices.length; indexVertex++) {
                    Assert.assertEquals(
                        0.,
                        modifiedVertices[indexVertex].getPosition().getNorm()
                                - originalVertices[indexVertex]
                                    .getPosition()
                                    .normalize()
                                    .scalarMultiply(
                                        originalVertices[indexVertex].getPosition().getNorm() + marginValue)
                                    .getNorm(), 1E-12);
                }
            }
        } else {
            for (int indexTriangle = 0; indexTriangle < modifiedTriangles.length; indexTriangle++) {
                final Triangle originalTriangle = originalTriangles[indexTriangle];
                final Triangle modifiedTriangle = modifiedTriangles[indexTriangle];
                final Vertex[] originalVertices = originalTriangle.getVertices();
                final Vertex[] modifiedVertices = modifiedTriangle.getVertices();
                for (int indexVertex = 0; indexVertex < modifiedVertices.length; indexVertex++) {
                    if (originalVertices[indexVertex].getPosition().getX() != 0) {
                        Assert.assertEquals(marginValue, modifiedVertices[indexVertex].getPosition().getX()
                                / originalVertices[indexVertex].getPosition().getX(), 1E-12);
                    } else {
                        Assert.assertEquals(0., modifiedVertices[indexVertex].getPosition().getX(), 1E-12);
                    }
                    if (originalVertices[indexVertex].getPosition().getY() != 0) {
                        Assert.assertEquals(marginValue, modifiedVertices[indexVertex].getPosition().getY()
                                / originalVertices[indexVertex].getPosition().getY(), 1E-12);
                    } else {
                        Assert.assertEquals(0., modifiedVertices[indexVertex].getPosition().getY(), 1E-12);
                    }
                    if (originalVertices[indexVertex].getPosition().getZ() != 0) {
                        Assert.assertEquals(marginValue, modifiedVertices[indexVertex].getPosition().getZ()
                                / originalVertices[indexVertex].getPosition().getZ(), 1E-12);
                    } else {
                        Assert.assertEquals(0., modifiedVertices[indexVertex].getPosition().getZ(), 1E-12);
                    }
                }
            }
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @throws IOException
     *         if an I/O error occurs
     * @description Evaluate the facet body shape serialization / deserialization process.
     *
     * @testPassCriteria The facet body shape can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException, IOException {

        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Build body file
        final String spherBodyObjPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "mnt" + File.separator + "SphericalBody.obj";
        final String modelFile = System.getProperty("user.dir") + File.separator + spherBodyObjPath;
        writeBodyFile(modelFile, 51, 100, this.bodyRadius / 1E3, 0.);

        final PVCoordinatesProvider sunPV = new MeeusSun();
        final UserCelestialBody celestialBodyBis = new UserCelestialBody("My body", sunPV, 0,
            IAUPoleFactory.getIAUPole(null), frame, null);

        final StarConvexFacetBodyShape bodyBis = new StarConvexFacetBodyShape("My body",
            celestialBodyBis.getRotatingFrame(IAUPoleModelType.TRUE),
            new ObjMeshLoader(modelFile));
        final StarConvexFacetBodyShape deserializedBody = TestUtils.serializeAndRecover(bodyBis);

        final OneAxisEllipsoid fittedEllipsoid1 = bodyBis.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID);
        final OneAxisEllipsoid fittedEllipsoid2 = deserializedBody.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID);
        checkEllipsoid(fittedEllipsoid1, fittedEllipsoid2);

        final OneAxisEllipsoid innerEllipsoid1 = bodyBis.getEllipsoid(EllipsoidType.INNER_ELLIPSOID);
        final OneAxisEllipsoid innerEllipsoid2 = deserializedBody.getEllipsoid(EllipsoidType.INNER_ELLIPSOID);
        checkEllipsoid(innerEllipsoid1, innerEllipsoid2);

        final OneAxisEllipsoid outerEllipsoid1 = bodyBis.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID);
        final OneAxisEllipsoid outerEllipsoid2 = deserializedBody.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID);
        checkEllipsoid(outerEllipsoid1, outerEllipsoid2);

        final OneAxisEllipsoid innerSphere1 = bodyBis.getEllipsoid(EllipsoidType.INNER_SPHERE);
        final OneAxisEllipsoid innerSphere2 = deserializedBody.getEllipsoid(EllipsoidType.INNER_SPHERE);
        checkEllipsoid(innerSphere1, innerSphere2);

        final OneAxisEllipsoid outerSphere1 = bodyBis.getEllipsoid(EllipsoidType.OUTER_SPHERE);
        final OneAxisEllipsoid outerSphere2 = deserializedBody.getEllipsoid(EllipsoidType.OUTER_SPHERE);
        checkEllipsoid(outerSphere1, outerSphere2);

        Assert.assertEquals(bodyBis.getPVCoordinates(date, frame), deserializedBody.getPVCoordinates(date, frame));

        Assert.assertEquals(bodyBis.getName(), deserializedBody.getName());
        Assert.assertEquals(bodyBis.getMinNorm(), deserializedBody.getMinNorm(), 0.);
        Assert.assertEquals(bodyBis.getMaxNorm(), deserializedBody.getMaxNorm(), 0.);
        Assert.assertEquals(bodyBis.getThreshold(), deserializedBody.getThreshold(), 0.);
    }

    /**
     * Check two ellipsoids.
     *
     * @param ellipsoid1
     *        First ellipsoid
     * @param ellipsoid2
     *        Second ellipsoid
     * @throws PatriusException
     *         if an error occurs
     */
    private static void checkEllipsoid(final OneAxisEllipsoid ellipsoid1, final OneAxisEllipsoid ellipsoid2)
        throws PatriusException {
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        Assert.assertEquals(ellipsoid1.getName(), ellipsoid2.getName());
        Assert.assertEquals(ellipsoid1.getEquatorialRadius(), ellipsoid2.getEquatorialRadius(), 0.);
        Assert.assertEquals(ellipsoid1.getFlattening(), ellipsoid2.getFlattening(), 0.);
        Assert.assertEquals(ellipsoid1.getPVCoordinates(date, frame), ellipsoid2.getPVCoordinates(date, frame));
    }

    /**
     * @testType UT
     *
     * @description check that method closestPointTo returns the two expected points
     *
     * @testPassCriteria the two points (one from the shape and one from the line) are those
     *                   expected (reference math), with or without intersection of infinite and semi-finite lines
     *
     * @referenceVersion 4.10
     *
     * @nonRegressionVersion 4.10
     *
     * @throws PatriusException if an error occurs with line transformation
     */
    @Test
    public void closestPointToTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = this.body.getBodyFrame();

        // Case 1: infinite line with intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(10E3, 20E3, 30E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final FacetPoint[] actual1 = this.body.closestPointTo(lineOfSight1, frame, date);
        final Vector3D reference1 = position1.normalize().scalarMultiply(this.bodyRadius);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual1[0].getPosition()) / reference1.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual1[1].getPosition()) / reference1.getNorm(), 1E-3);

        // Case 2: same as case 1 with semi-finite line, min abscissa before intersection (points
        // shall be the same)
        final Line lineOfSight2 = new Line(position1, Vector3D.ZERO, position1);
        final FacetPoint[] actual2 = this.body.closestPointTo(lineOfSight2, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual2[0].getPosition()) / reference1.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual2[1].getPosition()) / reference1.getNorm(), 1E-3);

        // Case 3: same as case 1 with inverted semi-finite line, no intersection due to min
        // abscissa
        final Vector3D position2 = new Vector3D(0, 0, 20E3);
        final Vector3D reference2 = position2.normalize().scalarMultiply(this.bodyRadius);

        // Case 3: same as case 1 with inverted semi-finite line, no intersection due to min
        // abscissa
        final Line lineOfSight3 = new Line(Vector3D.ZERO, position2, position2);
        final FacetPoint[] actual3 = this.body.closestPointTo(lineOfSight3, this.body.getBodyFrame(), date);
        Assert.assertEquals(0., reference2.subtract(actual3[1].getPosition()).getNorm(), 0.);
    }

    /**
     * @testType UT
     *
     * @description check that method closestPointTo(Line) returns the same result than
     *              closestPointTo(Line,getBodyFrame(),J2000_EPOCH)
     *
     * @testPassCriteria the two methods return the same result
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     *
     * @throws PatriusException if an error occurs with line transformation
     */
    @Test
    public void closestPointToNoFrameNoDateTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = this.body.getBodyFrame();

        // Case 1: infinite line with intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(10E3, 20E3, 30E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final BodyPoint[] actual1 = this.body.closestPointTo(lineOfSight1);
        final FacetPoint[] reference1 = this.body.closestPointTo(lineOfSight1, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference1[0].getPosition(), actual1[0].getPosition())
                / reference1[0].getPosition().getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1[1].getPosition(), actual1[1].getPosition())
                / reference1[1].getPosition().getNorm(), 1E-3);

        // Case 2: same as case 1 with semi-finite line, min abscissa before intersection (points
        // shall be the same)
        final Line lineOfSight2 = new Line(position1, Vector3D.ZERO, position1);
        final BodyPoint[] actual2 = this.body.closestPointTo(lineOfSight2);
        final FacetPoint[] reference2 = this.body.closestPointTo(lineOfSight2, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference2[0].getPosition(), actual2[0].getPosition())
                / reference2[0].getPosition().getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference2[1].getPosition(), actual2[1].getPosition())
                / reference2[1].getPosition().getNorm(), 1E-3);

        // Case 3: same as case 1 with inverted semi-finite line, no intersection due to min
        // abscissa
        final Vector3D position2 = new Vector3D(0, 0, 20E3);
        final Vector3D reference3 = position2.normalize().scalarMultiply(this.bodyRadius);

        // Case 3: same as case 1 with inverted semi-finite line, no intersection due to min
        // abscissa
        final Line lineOfSight3 = new Line(Vector3D.ZERO, position2, position2);
        final FacetPoint[] actual3 = this.body.closestPointTo(lineOfSight3, this.body.getBodyFrame(), date);
        Assert.assertEquals(0., reference3.subtract(actual3[1].getPosition()).getNorm(), 0.);
    }

    // /**
    // * @testType UT
    // *
    // * @description check that the method getIntersectionPoint returns a non-null intersection
    // *
    // * @testPassCriteria the returned intersection shall be non-null
    // *
    // * @referenceVersion 4.11.1
    // *
    // * @nonRegressionVersion 4.11.1
    // *
    // * @throws PatriusException if an error occurs
    // * @throws URISyntaxException if an error about URI syntax occurs
    // */
    // @Test
    // public void PhobosErnst200KTest() throws PatriusException, URISyntaxException {
    // // File path to Phobos_Ernst_200K.obj
    // final String modelFile1 = "mnt" + File.separator + "Phobos_Ernst_200K.obj";
    // final String fullName1 = this.getClass().getClassLoader().getResource(modelFile1).toURI().getPath();
    // final ObjMeshLoader loader = new ObjMeshLoader(fullName1);
    // final FacetBodyShape facetShape = new FacetBodyShape("Facet body shape", FramesFactory.getITRF(),
    // EllipsoidType.INNER_SPHERE, loader);
    // // Input planetocentric latitude and longitude
    // final double planetocentricLatitude = 0.04008717791700243;
    // final double planetocentricLongitude = 1.3591551872957268;
    // // Longitude, cosine and sine
    // final double[] sincosLon = MathLib.sinAndCos(planetocentricLongitude);
    // final double sinLon = sincosLon[0];
    // final double cosLon = sincosLon[1];
    // // Latitude, cosine and sine
    // final double[] sincosLat = MathLib.sinAndCos(planetocentricLatitude);
    // final double sinLat = sincosLat[0];
    // final double cosLat = sincosLat[1];
    // // Define a position outside the shape with entered latitude and longitude
    // final Vector3D position = new Vector3D(cosLat * cosLon, cosLat * sinLon, sinLat).scalarMultiply(facetShape
    // .getMaxNorm() * 2);
    // // Half line from the origin to the position
    // final Line halfLine = new Line(Vector3D.ZERO, position, Vector3D.ZERO);
    // // Check that the intersection with the shape is not null
    // Assert.assertNotNull(facetShape.getIntersectionPoint(halfLine, position,
    // facetShape.getBodyFrame(), null));
    // }
}
