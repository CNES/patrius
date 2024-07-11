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
 * 
 * @history creation 17/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.9:DM:DM-3158:10/05/2022:[PATRIUS] Ajout d'une methode computeSideDirections(Frame) in class PyramidalField 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.bodies.mesh.Triangle;
import fr.cnes.sirius.patrius.bodies.mesh.Vertex;
import fr.cnes.sirius.patrius.fieldsofview.PyramidalField;
import fr.cnes.sirius.patrius.fieldsofview.RectangleField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.PolyhedronsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.PolygonsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @description
 *              <p>
 *              Test class for the pyramidal field of view.
 *              </p>
 * 
 * @see PyramidalField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class PyramidalFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Pyramidal field of view
         * 
         * @featureDescription Pyramidal field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        PYRAMIDAL_FIELD
    }

    /** Exception error message */
    private static final String SEVERAL_BOUNDARY_LOOPS = "The union or intersection is too complex and connot be computed.";
    
    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PYRAMIDAL_FIELD}
     * 
     * @testedMethod {@link PyramidalField#getAngularDistance(Vector3D)}
     * @testedMethod {@link PyramidalField#isInTheField(Vector3D)}
     * @testedMethod {@link PyramidalField#getName()}
     * 
     * @description test of the basic methods of a pyramidal field of view
     * 
     * @input a pyramidal field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void pyramidalTest() {

        final String name = "pyramidalField";

        // test with a zero-normed vector
        try {
            final Vector3D[] directions = new Vector3D[4];
            directions[0] = new Vector3D(0.0, 0.0, 0.0);
            directions[1] = new Vector3D(1.0, -1.0, 1.0);
            directions[2] = new Vector3D(-1.0, -1.0, 1.0);
            directions[3] = new Vector3D(-1.0, 1.0, 1.0);
            new PyramidalField(name, directions);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with too few vectors
        try {
            final Vector3D[] directions = new Vector3D[2];
            directions[0] = new Vector3D(1.0, 1.0, 1.0);
            directions[1] = new Vector3D(1.0, -1.0, 1.0);
            new PyramidalField(name, directions);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // CONVEX FIELD
        Vector3D[] directions = new Vector3D[4];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);

        PyramidalField field = new PyramidalField(name, directions);

        // test with a vector in this field
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            FastMath.PI / 4.0 - MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of this field
        testedDirection = new Vector3D(0.0, 2.0, 1.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            -FastMath.PI / 4.0 + MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // CONVEX FIELD 2
        directions = new Vector3D[5];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(0.0, -4.0, 1.0);
        directions[4] = new Vector3D(1.0, -1.0, 1.0);

        field = new PyramidalField(name, directions);

        // tests
        testedDirection = new Vector3D(1.0, -5.0, 1.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        testedDirection = new Vector3D(-1.0, -5.0, 1.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        testedDirection = new Vector3D(0.0, -3.0, 1.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        // CONCAVE FIELD
        directions = new Vector3D[5];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(0.0, -0.5, 1.0);
        directions[4] = new Vector3D(1.0, -1.0, 1.0);

        field = new PyramidalField(name, directions);

        // test with a vector in this field
        testedDirection = new Vector3D(0.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of this field
        testedDirection = new Vector3D(0.0, -0.75, 1.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));

        // CONCAVE FIELD 2
        directions = new Vector3D[7];
        directions[0] = new Vector3D(0.0, -0.5, 1.0);
        directions[1] = new Vector3D(1.0, -2.0, 1.0);
        directions[2] = new Vector3D(2.0, -2.0, 1.0);
        directions[3] = new Vector3D(2.0, 2.0, 1.0);
        directions[4] = new Vector3D(-2.0, 2.0, 1.0);
        directions[5] = new Vector3D(-2.0, -2.0, 1.0);
        directions[6] = new Vector3D(-1.0, -2.0, 1.0);

        field = new PyramidalField(name, directions);

        // tests with vectors in this field
        testedDirection = new Vector3D(0.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        testedDirection = new Vector3D(0.25, -0.25, 1.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        testedDirection = new Vector3D(-0.25, -0.25, 1.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        // test with a vector out of this field
        testedDirection = new Vector3D(0.0, -0.75, 1.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        // test with zero direction
        testedDirection = Vector3D.ZERO;
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            0.0, this.comparisonEpsilon);

        // name test
        Assert.assertEquals(name, field.getName());

        // Test side axis (reference: math)
        final RectangleField rectangleSideField = new RectangleField("", Vector3D.PLUS_I, Vector3D.PLUS_J,
            FastMath.PI / 2., FastMath.PI / 2.);
        final PyramidalField pyramidalSideField = new PyramidalField("", rectangleSideField.getSideAxis());
        final Vector3D[] sideAxis = pyramidalSideField.getSideAxis();
        final double sqrt2Over2 = MathLib.sqrt(2.) / 2.;
        Assert.assertEquals(0., sideAxis[0].distance(new Vector3D(0, sqrt2Over2, sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[1].distance(new Vector3D(0, -sqrt2Over2, sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[2].distance(new Vector3D(0, -sqrt2Over2, -sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[3].distance(new Vector3D(0, sqrt2Over2, -sqrt2Over2)), 1E-16);
    }

    /**
     * Test needed to validate the retrieval of the directions of a pyramidal
     * field of view.
     * 
     * @throws PatriusException if some frame specific errors occur during
     *         the retrieval of the directions.
     * 
     * @testedMethod {@link PyramidalField#computeSideDirections(Frame)}
     */
    @Test
    public void computeSideDirectionsTest() throws PatriusException {
        // define directions
        final Vector3D[] directions = new Vector3D[4];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);
        // try the creation of a pyramidal field of view with the given directions
        try {
            // expected
            // create the pyramidal field of view with the given directions
            final PyramidalField pyramidalField = new PyramidalField("field", directions);
            // define frame
            final Frame frame = FramesFactory.getGCRF();
            // retrieve directions of the new pyramidal field of view
            final IDirection[] retrievedDir = pyramidalField.computeSideDirections(frame);
            // loop over all viewing directions
            for (int i = 0; i < retrievedDir.length; i++) {
                // check that the retrieved direction coincides with the given one
                Assert.assertEquals(directions[i], retrievedDir[i].getVector(null, new AbsoluteDate(), frame));
            }
        } catch (final IllegalArgumentException e) {
            // not expected
            Assert.fail();
        }
    }

    /**
     * Test needed to validate the computation of the convexity of a pyramidal
     * field of view.
     * 
     * @throws PatriusException if some frame specific errors occur during
     *         the retrieval of the directions.
     * 
     * @testedMethod {@link PyramidalField#isConvex()}
     */
    @Test
    public void convexityTest() {

        // Definitions used here:
        // A closed field of view is when the field of view is inside the edges defined by the pyramidal field
        // directions. On the contrary, an open field of view is when the field of view is outside the edges
        // defined by the pyramidal field, that is to say the field of view is infinite since is has no borders.

        /*
         * Case 1: simple convex closed field of view
         */
        final List<Vector3D> convex1 = new ArrayList<>();
        convex1.add(new Vector3D(1.0, 1.0, 1.0));
        convex1.add(new Vector3D(-1.0, 1.0, 1.0));
        convex1.add(new Vector3D(-1.0, -1.0, 1.0));
        convex1.add(new Vector3D(1.0, -1.0, 1.0));
        Vector3D[] directions = convex1.toArray(new Vector3D[convex1.size()]);
        PyramidalField field = new PyramidalField("convex closed 1", directions);
        Assert.assertTrue(field.isConvex());
        
        /*
         * Case 2: conjugate of convex1 expected concave open
         */
        Assert.assertFalse(field.getComplementaryFieldOfView().isConvex());

        /*
         * Case 3: less simple convex closed field of view
         */
        final List<Vector3D> convex2 = new ArrayList<>();
        convex2.add(Vector3D.PLUS_I);
        convex2.add(Vector3D.PLUS_J);
        convex2.add(Vector3D.PLUS_K);
        directions = convex2.toArray(new Vector3D[convex2.size()]);
        field = new PyramidalField("convex closed 2", directions);
        Assert.assertTrue(field.isConvex());
        
        /*
         * Case 4: conjugate of convex2 expected concave open
         */
        Assert.assertFalse(field.getComplementaryFieldOfView().isConvex());
        
        /*
         * Case 5: simple convex closed field of view with colinear points
         */
        final List<Vector3D> convex3 = new ArrayList<>();
        convex3.add(new Vector3D(1.0, 1.0, 1.0));
        convex3.add(new Vector3D(-1.0, 1.0, 1.0));
        convex3.add(new Vector3D(-1.0, -1.0, 1.0));
        convex3.add(new Vector3D(0, 0, 1.0));
        directions = convex3.toArray(new Vector3D[convex3.size()]);
        field = new PyramidalField("convex closed 3", directions);
        Assert.assertTrue(field.isConvex());
        
        /*
         * Case 6: conjugate of convex3 expected concave open
         */
        Assert.assertFalse(field.getComplementaryFieldOfView().isConvex());
        
        /*
         * Case 7: concave closed field of view by adding a direction vector in convex1 that is contained by it
         */
        final List<Vector3D> concave1 = new ArrayList<>(convex1);
        concave1.add(1, new Vector3D(0, 0, 1));
        directions = concave1.toArray(new Vector3D[concave1.size()]);
        field = new PyramidalField("concave closed 1", directions);
        Assert.assertFalse(field.isConvex());
        
        /*
         * Case 8: conjugate of concave1 expected concave open
         */
        Assert.assertFalse(field.getComplementaryFieldOfView().isConvex());
        
        /*
         * Case 9: concave closed field of view by adding a direction vector in convex2 that is contained by it
         */
        final List<Vector3D> concave2 = new ArrayList<>(convex2);
        concave2.add(1, new Vector3D(1 / 3., 1 / 3., 1 / 3.));
        directions = concave2.toArray(new Vector3D[concave2.size()]);
        field = new PyramidalField("concave closed 2", directions);
        Assert.assertFalse(field.isConvex());
        
        /*
         * Case 10: conjugate of concave2 expected concave open
         */
        Assert.assertFalse(field.getComplementaryFieldOfView().isConvex());
        
    }

    /**
     * Test needed to validate the equality assertion between two fields of view.
     * 
     * @throws PatriusException if some frame specific errors occur during
     *         the retrieval of the directions.
     * 
     * @testedMethod {@link PyramidalField#equals(PyramidalField, double)}
     */
    @Test
    public void equalsTest() {
        // define directions
        final Vector3D[] directions = new Vector3D[4];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);

        final PyramidalField field = new PyramidalField("field", directions);

        PyramidalField other;

        // same object
        other = field;
        Assert.assertTrue(field.equals(other, 0));

        // same data
        other = new PyramidalField("other", directions);
        Assert.assertTrue(field.equals(other, this.comparisonEpsilon));

        // slightly rotated directions vector
        final Vector3D axis = Vector3D.PLUS_K;
        final double angle = MathLib.toRadians(1E-3);
        final Rotation rotation = new Rotation(axis, angle);
        final Vector3D[] newDirections = new Vector3D[directions.length];
        for (int i = 0; i < directions.length; i++) {
            newDirections[i] = rotation.applyTo(directions[i]);
        }
        // identical angular separation between each pair of sideAxis
        final double angularDistance = Vector3D.angle(directions[0], newDirections[0]);

        other = new PyramidalField("rotated-fov", newDirections);
        // tolerance juste below the angular separation
        Assert.assertFalse(field.equals(other, angularDistance - 1E-6));
        Assert.assertTrue(field.equals(other, angularDistance + 1E-6));

        // different number of side axis
        final Vector3D[] direction1 = new Vector3D[3];
        direction1[0] = new Vector3D(1.0, 1.0, 1.0);
        direction1[1] = new Vector3D(-1.0, 1.0, 1.0);
        direction1[2] = new Vector3D(-1.0, -1.0, 1.0);
        other = new PyramidalField("other", direction1);
        Assert.assertFalse(field.equals(other, 0));
    }

    /**
     * Test needed to validate the construction of the complementary field of view.
     * 
     * @throws PatriusException if some frame specific errors occur during
     *         the retrieval of the directions.
     * 
     * @testedMethod {@link PyramidalField#getComplementaryFieldOfView()}
     */
    @Test
    public void getComplementaryFieldOfViewTest() {
        // define directions
        final Vector3D[] directions = new Vector3D[4];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);

        final PyramidalField field = new PyramidalField("field", directions);
        final PyramidalField complementaryField = field.getComplementaryFieldOfView();

        // random point generator
        final VectorGenerator generator = new VectorGenerator(directions, new Well19937c());

        // generate vector that are contained in the original field of view, they must not be contained in the
        // complementary field of view.
        final int nTests = 200;
        for (int i = 0; i < nTests; i++) {
            final Vector3D dirInField = generator.generateVector();
            Assert.assertFalse(complementaryField.isInTheField(dirInField));
        }

        // build vectors outside the original field of view
        final Vector3D planeOffset = new Vector3D(0, 0, 1);
        for (final Vector3D direction : directions) {
            final Vector3D vectorNotInField = direction.add(direction.subtract(planeOffset));
            Assert.assertTrue(complementaryField.isInTheField(vectorNotInField));
        }
    }

    /**
     * Test needed to validate the inclusion of two pyramidal fields when the
     * original pyramidal field is convex.
     * 
     * @throws PatriusException if some frame specific errors occur during
     *         the retrieval of the directions.
     * 
     * @testedMethod {@link PyramidalField#isInTheField(PyramidalField)}
     */
    @Test
    public void inInTheFieldConvexTest() {

        // random number generator
        final RandomGenerator rng = new Well19937c();

        // case of convex field of view
        final Vector3D[] directions = new Vector3D[4];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);

        final PyramidalField field = new PyramidalField("field", directions);

        PyramidalField other;
        Vector3D[] newDirections;

        // fields are equal, they must contained each other
        other = field;
        Assert.assertTrue(field.isInTheField(other));

        // the other field with triangular base and a facet in common
        newDirections = new Vector3D[3];
        newDirections[0] = new Vector3D(1.0, 1.0, 1.0);
        newDirections[1] = new Vector3D(-1.0, 1.0, 1.0);
        newDirections[2] = new Vector3D(0, 0, 1);
        other = new PyramidalField("other", newDirections);
        Assert.assertTrue(field.isInTheField(other));

        // the other field with rectangular base and two facets in common
        // the last direction is slighlty shifted outside the original field of view
        // the resulting field of view is not contained in the original one
        newDirections = new Vector3D[4];
        newDirections[0] = new Vector3D(1.0, 1.0, 1.0);
        newDirections[1] = new Vector3D(-1.0, 1.0, 1.0);
        newDirections[2] = new Vector3D(-1.0, -1.0, 1.0);
        newDirections[3] = new Vector3D(1.0 + 1E-3, -1.0 - 1E-3, 1.0);
        other = new PyramidalField("other", newDirections);
        Assert.assertFalse(field.isInTheField(other));
        Assert.assertTrue(other.isInTheField(field));

        // the other field with rectangular base and two facets in common
        // the last direction is slightly shifted inside the original field
        // the resulting field of view is contained in the original one
        newDirections[3] = new Vector3D(1.0 - 1E-3, -1.0 + 1E-3, 1.0);
        other = new PyramidalField("other", newDirections);
        Assert.assertTrue(field.isInTheField(other));
        Assert.assertFalse(other.isInTheField(field));

        // the other field is defined as the symmetrical polyhedron wrt the plane {O, +I, +J}
        // they do not contain each other
        newDirections = new Vector3D[4];
        newDirections[0] = directions[2].negate();
        newDirections[1] = directions[1].negate();
        newDirections[2] = directions[0].negate();
        newDirections[3] = directions[3].negate();
        other = new PyramidalField("other", newDirections);
        Assert.assertFalse(field.isInTheField(other));
        Assert.assertFalse(other.isInTheField(field));

        // the other filed is defined with new directions strictly contained in the original field of view
        // the resulting pyramidal field must be contained
        newDirections = new Vector3D[4];
        for (int i = 0; i < directions.length; i++) {
            // build a vector in polyhedron base
            final double lambda = rng.nextDouble();
            final Vector2D vectInPlane = new Vector2D(-lambda, new Vector2D(directions[i].getX(), directions[i].getY()));
            // new directoin is built as direction - lambda * vectInPlane
            newDirections[i] = directions[i].add(new Vector3D(vectInPlane.getX(), vectInPlane.getY(), 1));
        }
        other = new PyramidalField("other", newDirections);
        Assert.assertTrue(field.isInTheField(other));
        Assert.assertFalse(other.isInTheField(field));
        
        // Additional test: conventional true if other FoV is null
        other = null;
        Assert.assertTrue(field.isInTheField(other));
        
    }

    /**
     * Test needed to validate the inclusion of two pyramidal fields when the
     * original pyramidal field is concave.
     * 
     * @throws PatriusException if some frame specific errors occur during
     *         the retrieval of the directions.
     * 
     * @testedMethod {@link PyramidalField#isInTheField(PyramidalField)}
     */
    @Test
    public void isInTheFieldNotConvexTest() {

        final Vector3D shift = Vector3D.PLUS_I.scalarMultiply(-0.5);
        final Vector3D vectorOnFacet4 = new Vector3D(1, 0, 1);

        // case of non convex field of view : the last vector is built so that the
        // it is contained in the field of view delimited by the first four directions
        final Vector3D[] directions = new Vector3D[5];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);
        directions[4] = vectorOnFacet4.add(shift);

        final PyramidalField field = new PyramidalField("original-field", directions);

        PyramidalField other;
        Vector3D[] newDirections;

        // the other field with triangular base and two facets in common with the original field of view. The resulting
        // field of view must be contained.
        newDirections = new Vector3D[3];
        newDirections[0] = new Vector3D(1.0, 1.0, 1.0);
        newDirections[1] = new Vector3D(-1.0, 1.0, 1.0);
        newDirections[2] = new Vector3D(-1.0, -1.0, 1.0);
        other = new PyramidalField("other", newDirections);
        Assert.assertTrue(field.isInTheField(other));
        Assert.assertFalse(other.isInTheField(field));

        // the other field is built with the first four directions of the original field of view. The resulting
        // field of view must not be contained in the orgininal one
        newDirections = new Vector3D[4];
        newDirections[0] = new Vector3D(1.0, 1.0, 1.0);
        newDirections[1] = new Vector3D(-1.0, 1.0, 1.0);
        newDirections[2] = new Vector3D(-1.0, -1.0, 1.0);
        newDirections[3] = new Vector3D(1.0, -1.0, 1.0);
        other = new PyramidalField("other", newDirections);
        Assert.assertFalse(field.isInTheField(other));
        Assert.assertTrue(other.isInTheField(field));

        // the other field is built with five directions : the first four directions are identical to of the original
        // field of view. The fifth vector is outside. The resulting field of view must not be contained in the
        // orgininal one
        newDirections = new Vector3D[5];
        newDirections[0] = new Vector3D(1.0, 1.0, 1.0);
        newDirections[1] = new Vector3D(-1.0, 1.0, 1.0);
        newDirections[2] = new Vector3D(-1.0, -1.0, 1.0);
        newDirections[3] = new Vector3D(1.0, -1.0, 1.0);
        newDirections[4] = vectorOnFacet4.add(shift.scalarMultiply(0.9));
        other = new PyramidalField("other", newDirections);
        Assert.assertFalse(field.isInTheField(other));
        Assert.assertTrue(other.isInTheField(field));

        // the other field is built with five directions : the first four directions are identical to of the original
        // field of view. The fifth vector is inside. The resulting field of view must be contained in the orgininal one
        newDirections = new Vector3D[5];
        newDirections[0] = new Vector3D(1.0, 1.0, 1.0);
        newDirections[1] = new Vector3D(-1.0, 1.0, 1.0);
        newDirections[2] = new Vector3D(-1.0, -1.0, 1.0);
        newDirections[3] = new Vector3D(1.0, -1.0, 1.0);
        newDirections[4] = vectorOnFacet4.add(shift.scalarMultiply(1.1));
        other = new PyramidalField("other", newDirections);
        Assert.assertTrue(field.isInTheField(other));
        Assert.assertFalse(other.isInTheField(field));

        // the other field is built with four directions : the last side axis is common to both field of view. The
        // resulting field of view must be contained in the orgininal one
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, MathLib.PI / 2.);
        newDirections = new Vector3D[4];
        newDirections[0] = vectorOnFacet4.add(shift);
        newDirections[1] = rotation.applyTo(newDirections[0]);
        newDirections[2] = rotation.applyTo(newDirections[1]);
        newDirections[3] = rotation.applyTo(newDirections[2]);
        other = new PyramidalField("other", newDirections);
        Assert.assertTrue(field.isInTheField(other));
        Assert.assertFalse(other.isInTheField(field));
    }

    /**
     * Test needed to validate the intersection computation between two pyramidal fields.
     * 
     * @throws PatriusException 
     * 
     * @testedMethod {@link PyramidalField#getIntersectionWith(PyramidalField)}
     */
    @Test
    public void getIntersectionWithTest() throws PatriusException {
        final Vector3D origin = Vector3D.ZERO;

        PyramidalField field1;
        PyramidalField field2;
        PyramidalField result;
        PyramidalField expected;
        Vector3D[] expectedDirections;

        /*
         * Case 1: two rectangular fields that overlap, a rotation of pi/2 is applied around pyramid main axis (Z-axis)
         */
        // rectangular field
        final RectangularPyramidalFieldBuilder builder1 = new RectangularPyramidalFieldBuilder(-2, +2, -0.5, +0.5, 1);
        // PI/2 rotation
        final RectangularPyramidalFieldBuilder builder2 = builder1.rotate(MathLib.PI / 2.);

        field1 = builder1.build("field1", origin);
        field2 = builder2.build("field2", origin);

        // the expected pyramid field is squared, xMin = -0.5, xMax = +0.5, yMin = -0.5, yMax = +0.5
        result = field1.getIntersectionWith(field2);
        expected = new RectangularPyramidalFieldBuilder(-0.5, +0.5, -0.5, +0.5, 1).build("expected", origin);
        Assert.assertTrue(result.equals(expected, this.comparisonEpsilon));

        /*
         * Case 2: two square fields that overlap, a rotation of pi/4 is applied around pyramid main axis (Z-axis)
         */
        // squared field
        final RectangularPyramidalFieldBuilder builder3 = new RectangularPyramidalFieldBuilder(-1, +1, -1, +1, 1);
        // PI/4 rotation
        final RectangularPyramidalFieldBuilder builder4 = builder3.rotate(MathLib.PI / 4.);
        field1 = builder3.build("field1", origin);
        field2 = builder4.build("field2", origin);

        // the expected pyramidal field has 8 side axis
        result = field1.getIntersectionWith(field2);
        final double twoSqrtMinusOne = MathLib.sqrt(2) - 1;
        expectedDirections = new Vector3D[8];
        expectedDirections[0] = new Vector3D(1, twoSqrtMinusOne, 1);
        expectedDirections[1] = new Vector3D(twoSqrtMinusOne, 1, 1);
        expectedDirections[2] = new Vector3D(-twoSqrtMinusOne, 1, 1);
        expectedDirections[3] = new Vector3D(-1, twoSqrtMinusOne, 1);
        expectedDirections[4] = new Vector3D(-1, -twoSqrtMinusOne, 1);
        expectedDirections[5] = new Vector3D(-twoSqrtMinusOne, -1, 1);
        expectedDirections[6] = new Vector3D(twoSqrtMinusOne, -1, 1);
        expectedDirections[7] = new Vector3D(1, -twoSqrtMinusOne, 1);
        expected = new PyramidalField("", expectedDirections);
        Assert.assertTrue(result.equals(expected, this.comparisonEpsilon));

        /*
         * Case 3: two square fields, one is included in the other
         */
        // squared field
        field1 = builder3.build("field1", origin);
        field2 = builder3.scale(0.5).build("reduced-field1", origin);

        // field2 is included in field1, the returned pyramidal field is field2
        result = field1.getIntersectionWith(field2);
        Assert.assertEquals(field2, result);

        // field1 is included in field2, the returned pyramidal field is field1
        field2 = builder3.build("field1", origin);
        field1 = builder3.scale(0.9).build("reduced-field1", origin);
        result = field1.getIntersectionWith(field2);
        Assert.assertEquals(field1, result);

        /*
         * Case 4: two rectangular fields disjointed: null result
         */
        // rectangular fields disjointed
        final RectangularPyramidalFieldBuilder builder5 = new RectangularPyramidalFieldBuilder(-1, 0, -1, +1, 1);
        final RectangularPyramidalFieldBuilder builder6 = new RectangularPyramidalFieldBuilder(0, +1, -1, +1, 1);
        field1 = builder5.build("field1", origin);
        field2 = builder6.build("field2", origin);
        // the resulting pyramidal field is null
        Assert.assertNull(field1.getIntersectionWith(field2));

        /*
         * Case 5: two rectangular fields jointed by one facet: null result
         */
        // rectangular fields with a single facet in common
        final RectangularPyramidalFieldBuilder builder7 = new RectangularPyramidalFieldBuilder(-1, 0, -1, +1, 1);
        final RectangularPyramidalFieldBuilder builder8 = new RectangularPyramidalFieldBuilder(0, +1, -1, +1, 1);
        field1 = builder7.build("field1", origin);
        field2 = builder8.build("field2", origin);
        // the resulting pyramidal field is null
        Assert.assertNull(field1.getIntersectionWith(field2));
        
        /*
         * Case 6: two fields with a rotation pi/2 around another axis than Z-axis: null result
         */
        // square fields
        field1 = builder3.build("field1", origin);
        // field2 is rotated by PI/2 around +I axis, the base facet is now parallel to (O, +I, +K) plane
        // the fields have a single facet in common
        field2 = rotate(field1, Vector3D.PLUS_I, MathLib.PI / 2.);
        // the resulting pyramidal field is null
        Assert.assertNull(field1.getIntersectionWith(field2));

        /*
         * Case 7: two fields with a rotation pi/4 around X-axis
         */
        // field2 is rotated by PI/4 around +I axis
        field2 = rotate(field1, Vector3D.PLUS_I, MathLib.PI / 4.);
        result = field1.getIntersectionWith(field2);
        // the resulting pyramidal field contains 6 side axis
        final double twoSqrtOverTwo = MathLib.sqrt(2) / 2.;
        expectedDirections = new Vector3D[6];
        expectedDirections[0] = new Vector3D(twoSqrtOverTwo, -twoSqrtOverTwo, twoSqrtOverTwo);
        expectedDirections[1] = new Vector3D(1, -twoSqrtMinusOne, 1);
        expectedDirections[2] = new Vector3D(twoSqrtOverTwo, 0, 1);
        expectedDirections[3] = new Vector3D(-twoSqrtOverTwo, 0, 1);
        expectedDirections[4] = new Vector3D(-1, -twoSqrtMinusOne, 1);
        expectedDirections[5] = new Vector3D(-twoSqrtOverTwo, -twoSqrtOverTwo, twoSqrtOverTwo);
        expected = new PyramidalField("intersection", expectedDirections);
        Assert.assertTrue(result.equals(expected, this.comparisonEpsilon));
        
        /*
         * Case 8: overlapping square fields combining horizontal and vertical translations
         */
        final RectangularPyramidalFieldBuilder builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        final RectangularPyramidalFieldBuilder builderB = new RectangularPyramidalFieldBuilder(0.5, 2.5, 0.5, 2.5, 1);
        field1 = builderA.build("field1", Vector3D.ZERO);
        field2 = builderB.build("field2", Vector3D.ZERO);
        result = field1.getIntersectionWith(field2);
        expected = new RectangularPyramidalFieldBuilder(0.5, 1, 0.5, 1, 1).build("expected", origin);
        Assert.assertTrue(equals(expected, result, this.comparisonEpsilon));
        
        /*
         * Case 9: two overlapping triangle pyramidal fields
         */
        final Vector3D v1 = new Vector3D(1, 0, 1);
        final Vector3D v2 = new Vector3D(0, 1, 1);
        final Vector3D v3 = new Vector3D(-1, 0, 1);
        final Vector3D v4 = new Vector3D(0, -1, 1);
        Vector3D translation = new Vector3D(1, 0.5, 0);
        Vector3D[] directions1 = new Vector3D[3];
        directions1[0] = v1;
        directions1[1] = v2;
        directions1[2] = v3;
        
        Vector3D[] directions2 = new Vector3D[3];
        directions2[0] = v1.add(translation);
        directions2[1] = v3.add(translation);
        directions2[2] = v4.add(translation);

        field1 = new PyramidalField("field1", directions1);
        field2 = new PyramidalField("field2", directions2);
        result = field1.getIntersectionWith(field2);
        
        expectedDirections = new Vector3D[4];
        expectedDirections[0] = new Vector3D(1, 0, 1);
        expectedDirections[1] = new Vector3D(0.5, 0.5, 1);
        expectedDirections[2] = new Vector3D(0, 0.5, 1);
        expectedDirections[3] = new Vector3D(0.5, 0, 1);
        expected = new PyramidalField("expected", expectedDirections);

        Assert.assertTrue(equals(expected, result, this.comparisonEpsilon));
        
        /*
         * Case 10: two overlapping concave fields
         */
        directions1 = new Vector3D[5];
        directions1[0] = new Vector3D(1, 1, 1);
        directions1[1] = new Vector3D(0, 0, 1);
        directions1[2] = new Vector3D(-1, 1, 1);
        directions1[3] = new Vector3D(-1, -1, 1);
        directions1[4] = new Vector3D(1, -1, 1);
        
        directions2 = new Vector3D[5];
        directions2[0] = new Vector3D(0, 0, 1);
        directions2[1] = new Vector3D(0, -2, 1);
        directions2[2] = new Vector3D(1, -1, 1);
        directions2[3] = new Vector3D(2, -2, 1);
        directions2[4] = new Vector3D(2, 0, 1);

        field1 = new PyramidalField("field1", directions1);
        field2 = new PyramidalField("field2", directions2);
        result = field1.getIntersectionWith(field2);

//        Vector3D[] dirs = result.getSideAxis();
//        for (int i = 0; i < dirs.length; i++) {
//            System.out.println(dirs[i]);
//        }

        expected = new RectangularPyramidalFieldBuilder(0, 1, -1, 0, 1).build("expected", origin);
        Assert.assertTrue(equals(expected, result, this.comparisonEpsilon));
        
        /*
         * Case 11: two concave fields whose intersections are disjointed because the union would create a "hole" in the
         * middle
         * Does not work
         */
        directions1 = new Vector3D[5];
        directions1[0] = new Vector3D(1, 1, 1);
        directions1[1] = new Vector3D(0, 0, 1);
        directions1[2] = new Vector3D(-1, 1, 1);
        directions1[3] = new Vector3D(-1, -1, 1);
        directions1[4] = new Vector3D(1, -1, 1);
        
        directions2 = new Vector3D[5];
        directions2[0] = new Vector3D(1, 0, 1);
        directions2[1] = new Vector3D(1, 2, 1);
        directions2[2] = new Vector3D(-1, 2, 1);
        directions2[3] = new Vector3D(-1, 0, 1);
        directions2[4] = new Vector3D(0, 1, 1);

        field1 = new PyramidalField("field1", directions1);
        field2 = new PyramidalField("field2", directions2);
        
        try{
            result = field1.getIntersectionWith(field2);
            Assert.fail();
        } catch(final PatriusException exception) {
            // Computation throws an error
            Assert.assertEquals(SEVERAL_BOUNDARY_LOOPS, exception.getMessage().toString());
        }
        
        /*
         * Case 12: a concave and a convex fields with a complex intersection pattern that looks like a boomerang
         */
        directions1 = new Vector3D[5];
        directions1[0] = new Vector3D(1, 1, 1);
        directions1[1] = new Vector3D(0, 0, 1);
        directions1[2] = new Vector3D(-1, 1, 1);
        directions1[3] = new Vector3D(-1, -1, 1);
        directions1[4] = new Vector3D(1, -1, 1);
        
        directions2 = new Vector3D[5];
        directions2[0] = new Vector3D(0, -1, 1);
        directions2[1] = new Vector3D(1, 0, 1);
        directions2[2] = new Vector3D(1, 2, 1);
        directions2[3] = new Vector3D(-1, 2, 1);
        directions2[4] = new Vector3D(-1, 0, 1);

        field1 = new PyramidalField("field1", directions1);
        field2 = new PyramidalField("field2", directions2);
        result = field1.getIntersectionWith(field2);
        
        expectedDirections = new Vector3D[6];
        expectedDirections[0] = new Vector3D(0, 0, 1);
        expectedDirections[1] = new Vector3D(-1, 1, 1);
        expectedDirections[2] = new Vector3D(-1, 0, 1);
        expectedDirections[3] = new Vector3D(0, -1, 1);
        expectedDirections[4] = new Vector3D(1, 0, 1);
        expectedDirections[5] = new Vector3D(1, 1, 1);
        expected = new PyramidalField("expected", expectedDirections);
        Assert.assertTrue(equals(expected, result, this.comparisonEpsilon));
        
    }
    
    /**
     * Test needed to validate the union computation between two pyramidal fields.
     * 
     * @throws PatriusException 
     * 
     * @testedMethod {@link PyramidalField#getUnionWith(PyramidalField)}
     */
    @Test
    public void getUnionWithTest() throws PatriusException {
        Vector3D origin = Vector3D.PLUS_K;
        final String nameA = "fieldA";
        final String nameB = "fieldB";

        PyramidalField fieldA;
        PyramidalField fieldB;
        PyramidalField union;
        
        /*
         * Case 1: two jointed triangle pyramidal fields
         */
        Vector3D v1 = new Vector3D(1, 0, -1);
        Vector3D v2 = new Vector3D(0, 1, -1);
        Vector3D v3 = new Vector3D(-1, 0, -1);
        Vector3D v4 = new Vector3D(0, -1, -1);
        
        Vector3D[] directions1 = new Vector3D[3];
        directions1[0] = v1;
        directions1[1] = v3;
        directions1[2] = v2;
        
        Vector3D[] directions2 = new Vector3D[3];
        directions2[0] = v1;
        directions2[1] = v4;
        directions2[2] = v3;

        // The two triangle fields have a facet in common, the resulting union is a squared field of view
        fieldA = new PyramidalField(nameA, directions1);
        fieldB = new PyramidalField(nameB, directions2);
        union = fieldA.getUnionWith(fieldB);
        PyramidalField expected = new PyramidalField("union", new Vector3D[]{v4, v3, v2, v1});
        Assert.assertTrue(union.equals(expected, this.comparisonEpsilon));
        
        /*
         * Case 2: two jointed rectangular fields by one common edge (horizontal translation)
         */
        RectangularPyramidalFieldBuilder builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        RectangularPyramidalFieldBuilder builderB = new RectangularPyramidalFieldBuilder(1, 2.5, -1, 1, 1);
        fieldA = builderA.build(nameA, Vector3D.ZERO);
        fieldB = builderB.build(nameB, Vector3D.ZERO);
        union = fieldA.getUnionWith(fieldB);
        expected = new RectangularPyramidalFieldBuilder(-1, 2.5, -1, 1, 1).build("union", Vector3D.ZERO);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 3: two jointed rectangular fields by one common edge (vertical translation)
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        builderB = new RectangularPyramidalFieldBuilder(-1, 1, 1, 2.5, 1);
        fieldA = builderA.build(nameA, Vector3D.ZERO);
        fieldB = builderB.build(nameB, Vector3D.ZERO);
        union = fieldA.getUnionWith(fieldB);
        expected = new RectangularPyramidalFieldBuilder(-1, 1, -1, 2.5, 1).build("union", Vector3D.ZERO);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 4: overlapping square fields (horizontal translation)
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 0.5, -1, +1, 0);
        builderB = new RectangularPyramidalFieldBuilder(0, +1, -1, +1, 0);
        
        // Union must contain vectors [-1, -1, -1], [1, -1, -1], [1, 1, -1] and [-1, 1, -1]
        fieldA = builderA.build(nameA, origin);
        fieldB = builderB.build(nameB, origin);
        union = fieldB.getUnionWith(fieldA);
        
        // Union should contain both fields
        Assert.assertTrue(union.isInTheField(fieldA));
        Assert.assertTrue(union.isInTheField(fieldB));
        
        // generator vectors that must be contain in the field
        final List<Vector3D> boundaries = new ArrayList<>();
        boundaries.add(new Vector3D(-1, -1, -1));
        boundaries.add(new Vector3D(1, -1, -1));
        boundaries.add(new Vector3D(1, 1, -1));
        boundaries.add(new Vector3D(-1, 1, -1));
        final VectorGenerator generator = new VectorGenerator(boundaries.toArray(new Vector3D[boundaries.size()]),
            new Well19937c());
        for(int i = 0; i < 1000; i++){
            Assert.assertTrue(union.isInTheField(generator.generateVector()));
        }
        
        expected = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 0).build("union", Vector3D.PLUS_K);
        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 5: two square fields jointed by one vertex only
         * Does not work
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        builderB = new RectangularPyramidalFieldBuilder(1, 2, 1, 2, 1);
        fieldA = builderA.build(nameA, Vector3D.ZERO);
        fieldB = builderB.build(nameB, Vector3D.ZERO);
        
        try{
            union = fieldA.getUnionWith(fieldB);
            Assert.fail();
        } catch(final PatriusException exception) {
            // Computation throws an error
            Assert.assertEquals(SEVERAL_BOUNDARY_LOOPS, exception.getMessage().toString());
        }
        
        /*
         * Case 6: two disjointed square fields
         * Does not work
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        builderB = new RectangularPyramidalFieldBuilder(1.5, 2.5, -1, 1, 1);
        fieldA = builderA.build(nameA, Vector3D.ZERO);
        fieldB = builderB.build(nameB, Vector3D.ZERO);
        
        try{
            union = fieldA.getUnionWith(fieldB);
            Assert.fail();
        } catch(final PatriusException exception) {
            // Computation throws an error
            Assert.assertEquals(SEVERAL_BOUNDARY_LOOPS, exception.getMessage().toString());
        }
        
        
        /*
         * Case 7: two square fields, a rotation of pi/4 is applied around pyramid main axis (Z-axis)
         * Does not work
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        builderB = builderA.rotate(MathLib.PI / 4.);
        fieldA = builderA.build(nameA, Vector3D.ZERO);
        fieldB = builderB.build(nameB, Vector3D.ZERO);
        
        try{
            union = fieldA.getUnionWith(fieldB);
            Assert.fail();
        } catch(final PatriusException exception) {
            // Expected field of view is a 8-branches star, but computation throws an error
            Assert.assertEquals(SEVERAL_BOUNDARY_LOOPS, exception.getMessage().toString());
        }
        
        /*
         * Case 8: overlapping square fields combining horizontal and vertical translations
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        builderB = new RectangularPyramidalFieldBuilder(0.5, 2.5, 0.5, 2.5, 1);
        fieldA = builderA.build(nameA, Vector3D.ZERO);
        fieldB = builderB.build(nameB, Vector3D.ZERO);
        union = fieldA.getUnionWith(fieldB);
        
        Vector3D[] expectedDirections = new Vector3D[8];
        expectedDirections[0] = new Vector3D(-1, -1, 1);
        expectedDirections[1] = new Vector3D(1, -1, 1);
        expectedDirections[2] = new Vector3D(1, 0.5, 1);
        expectedDirections[3] = new Vector3D(2.5, 0.5, 1);
        expectedDirections[4] = new Vector3D(2.5, 2.5, 1);
        expectedDirections[5] = new Vector3D(0.5, 2.5, 1);
        expectedDirections[6] = new Vector3D(0.5, 1, 1);
        expectedDirections[7] = new Vector3D(-1, 1, 1);
        expected = new PyramidalField("expected", expectedDirections);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 9: two square fields, one is included into the other 
         */
        builderA = new RectangularPyramidalFieldBuilder(-1, 1, -1, 1, 1);
        builderB = builderA.scale(0.9);
        fieldA = builderA.build(nameA, origin);
        fieldB = builderB.build(nameB, origin);
        Assert.assertTrue(fieldA.getUnionWith(fieldB).equals(fieldA, this.comparisonEpsilon));
        Assert.assertTrue(fieldB.getUnionWith(fieldA).equals(fieldA, this.comparisonEpsilon));
        
        /*
         * Case 10: two overlapping triangle pyramidal fields
         */
        v1 = new Vector3D(1, 0, 1);
        v2 = new Vector3D(0, 1, 1);
        v3 = new Vector3D(-1, 0, 1);
        v4 = new Vector3D(0, -1, 1);
        Vector3D translation = new Vector3D(1, 0.5, 0);
        directions1 = new Vector3D[3];
        directions1[0] = v1;
        directions1[1] = v2;
        directions1[2] = v3;
        
        directions2 = new Vector3D[3];
        directions2[0] = v1.add(translation);
        directions2[1] = v3.add(translation);
        directions2[2] = v4.add(translation);

        fieldA = new PyramidalField(nameA, directions1);
        fieldB = new PyramidalField(nameB, directions2);
        union = fieldA.getUnionWith(fieldB);
        
        expectedDirections = new Vector3D[6];
        expectedDirections[0] = new Vector3D(0, 1, 1);
        expectedDirections[1] = new Vector3D(-1, 0, 1);
        expectedDirections[2] = new Vector3D(0.5, 0, 1);
        expectedDirections[3] = new Vector3D(1, -0.5, 1);
        expectedDirections[4] = new Vector3D(2, 0.5, 1);
        expectedDirections[5] = new Vector3D(0.5, 0.5, 1);
        expected = new PyramidalField("expected", expectedDirections);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 11: two concave fields jointed on one "exterior" edge
         */
        directions1 = new Vector3D[5];
        directions1[0] = new Vector3D(1, 1, 1);
        directions1[1] = new Vector3D(0, 0, 1);
        directions1[2] = new Vector3D(-1, 1, 1);
        directions1[3] = new Vector3D(-1, -1, 1);
        directions1[4] = new Vector3D(1, -1, 1);
        
        directions2 = new Vector3D[5];
        directions2[0] = new Vector3D(1, 1, 1);
        directions2[1] = new Vector3D(1, -1, 1);
        directions2[2] = new Vector3D(2, 0, 1);
        directions2[3] = new Vector3D(3, -1, 1);
        directions2[4] = new Vector3D(3, 1, 1);

        fieldA = new PyramidalField(nameA, directions1);
        fieldB = new PyramidalField(nameB, directions2);
        union = fieldA.getUnionWith(fieldB);
        
        expectedDirections = new Vector3D[8];
        expectedDirections[0] = new Vector3D(1, 1, 1);
        expectedDirections[1] = new Vector3D(0, 0, 1);
        expectedDirections[2] = new Vector3D(-1, 1, 1);
        expectedDirections[3] = new Vector3D(-1, -1, 1);
        expectedDirections[4] = new Vector3D(1, -1, 1);
        expectedDirections[5] = new Vector3D(2, 0, 1);
        expectedDirections[6] = new Vector3D(3, -1, 1);
        expectedDirections[7] = new Vector3D(3, 1, 1);
        expected = new PyramidalField("expected", expectedDirections);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 12: two concave fields jointed on one "interior" edge: translate field2 of case 11
         */
        translation = new Vector3D(-1, 1, 0);
        for (int i = 0; i < directions2.length; i++) {
            directions2[i] = directions2[i].add(translation);
        }
        fieldB = new PyramidalField(nameB, directions2);
        union = fieldA.getUnionWith(fieldB);
        
        expectedDirections = new Vector3D[8];
        expectedDirections[0] = new Vector3D(0, 2, 1);
        expectedDirections[1] = new Vector3D(0, 0, 1);
        expectedDirections[2] = new Vector3D(-1, 1, 1);
        expectedDirections[3] = new Vector3D(-1, -1, 1);
        expectedDirections[4] = new Vector3D(1, -1, 1);
        expectedDirections[5] = new Vector3D(1, 1, 1);
        expectedDirections[6] = new Vector3D(2, 0, 1);
        expectedDirections[7] = new Vector3D(2, 2, 1);
        expected = new PyramidalField("expected", expectedDirections);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 13: one concave field "filled" by a convex one, resulting in a rectangular field
         */
        directions1 = new Vector3D[5];
        directions1[0] = new Vector3D(1, 1, 1);
        directions1[1] = new Vector3D(0, 0, 1);
        directions1[2] = new Vector3D(-1, 1, 1);
        directions1[3] = new Vector3D(-1, -1, 1);
        directions1[4] = new Vector3D(1, -1, 1);
        
        directions2 = new Vector3D[5];
        directions2[0] = new Vector3D(1, 1, 1);
        directions2[1] = new Vector3D(1, 3, 1);
        directions2[2] = new Vector3D(-1, 3, 1);
        directions2[3] = new Vector3D(-1, 1, 1);
        directions2[4] = new Vector3D(0, 0, 1);

        fieldA = new PyramidalField(nameA, directions1);
        fieldB = new PyramidalField(nameB, directions2);
        union = fieldA.getUnionWith(fieldB);
        
        expected = new RectangularPyramidalFieldBuilder(-1, 1, -1, 3, 1).build("union", Vector3D.ZERO);

        Assert.assertTrue(equals(expected, union, this.comparisonEpsilon));
        
        /*
         * Case 14: two concave fields whose union creates a "hole" in the middle
         */
        directions1 = new Vector3D[5];
        directions1[0] = new Vector3D(1, 1, 1);
        directions1[1] = new Vector3D(0, 0, 1);
        directions1[2] = new Vector3D(-1, 1, 1);
        directions1[3] = new Vector3D(-1, -1, 1);
        directions1[4] = new Vector3D(1, -1, 1);
        
        directions2 = new Vector3D[5];
        directions2[0] = new Vector3D(1, 0, 1);
        directions2[1] = new Vector3D(1, 2, 1);
        directions2[2] = new Vector3D(-1, 2, 1);
        directions2[3] = new Vector3D(-1, 0, 1);
        directions2[4] = new Vector3D(0, 1, 1);

        fieldA = new PyramidalField(nameA, directions1);
        fieldB = new PyramidalField(nameB, directions2);
        
        try{
            union = fieldA.getUnionWith(fieldB);
            Assert.fail();
        } catch(final PatriusException exception) {
            // Computation throws an error
            Assert.assertEquals(SEVERAL_BOUNDARY_LOOPS, exception.getMessage().toString());
        }
        
    }
    
    /**
     * Build a string representation of a pyramidal field.
     * 
     * @param field
     *        the pyramidal field
     * 
     * @return the string representation of the field
     */
    public static String printSideAxis(final PyramidalField field) {
        // string builder
        final StringBuilder builder = new StringBuilder();
        builder.append("Side axis of field : ");
        builder.append(field.getName());
        builder.append(System.lineSeparator());
        // display every directon vector on a line
        for (final Vector3D direction : field.getSideAxis()) {
            builder.append(direction.toString());
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }
    
    /**
     * Build a string representation of a pyramidal field.
     * 
     * @param polyhedron
     *        The polyhedron to display
     * @return
     */
    public static String printFacets(final PolyhedronsSet polyhedron) {
        final List<Vector3D> vertices = polyhedron.getBRep().getVertices();
        final List<int[]> facets = polyhedron.getBRep().getFacets();
        // string builder
        final StringBuilder builder = new StringBuilder();
        builder.append("Polyhedron facets : ");
        builder.append(System.lineSeparator());
        
        int iFacets = 1;
        // display every directon vector on a line
        for (final int[] facet : facets){
            builder.append("Facet n°");
            builder.append(iFacets);
            builder.append(System.lineSeparator());
            for(int i = 0; i < facet.length; i++){
                builder.append(vertices.get(facet[i]));
                if(i < facet.length - 1){
                    builder.append(", ");
                }
            }
            builder.append(System.lineSeparator());
            iFacets++;
        }

        return builder.toString();
    }
    
    /**
     * Build a new {@link RectangularFieldBuilder} by rotating the initial base vectors in the plane.
     * 
     * @param axis
     *        the rotation axis
     * 
     * @param angle
     *        the angle rotation
     * 
     * @return the new rectangular base
     */
    public static PyramidalField rotate(final PyramidalField field, final Vector3D axis, final double angle) {
        // array of side axis
        final Vector3D[] sideAxis = field.getSideAxis();
        
        // rotation to apply
        final Rotation rotation = new Rotation(axis, angle);

        // transformed points
        final Vector3D[] newPoints = new Vector3D[sideAxis.length];

        for (int i = 0; i < sideAxis.length; i++) {
            newPoints[i] = rotation.applyTo(sideAxis[i]);
        }

        return new PyramidalField("rotated-" + field.getName(), newPoints);
    }
    
    /**
     * Duplicated {@link PyramidalField} method but with different equality criteria: two fields with same facets are
     * considered equal, there is no need to have exactly the same vectors at the same array indices as long as these
     * arrays are linked by a circular shift transformation (this ensures facets have the same orientation).<br>
     * Check the equality between this and a provided {@link PyramidalField}.
     * 
     * @param thisFov
     *        the first pyramidal field
     * @param otherFov
     *        the other pyramidal field
     * @param angularTol
     *        the numerical tolerance to check the colinearity between side axis
     * 
     * @return {@code true} if both pyramidal field are equal within the tolerance
     */
    private static boolean equals(final PyramidalField thisFov, final PyramidalField otherFov, final double angularTol){
        
        boolean isEqual = false;
        
        if (otherFov == thisFov) {
            isEqual = true;
        } else if (otherFov != null) {

            isEqual = true;
            final Vector3D[] thisDir = thisFov.getSideAxis();
            final Vector3D[] otherDir = otherFov.getSideAxis();
            
            // check the side axis arrays length
            isEqual &= (otherDir.length == thisDir.length);
            
            if(isEqual){
                final Vector3D firstOtherDir = otherDir[0];
                int startIndex = 0;
                for (int index = 0; index < thisDir.length; index++) {
                    // check the first other direction vector is in this direction vectors and get the index
                    if (MathLib.abs(Vector3D.angle(thisDir[index], firstOtherDir)) <= angularTol) {
                        startIndex = index;
                        break;
                    }
                }
                    
                for (int index = 0; index < otherDir.length; index++) {
                    // retrieve the side vectors at same index position in the arrays taking the shift into account
                    final Vector3D thisVector = thisDir[(index+startIndex)%otherDir.length];
                    final Vector3D otherVector = otherDir[index];
                    
                    // compute angular distance
                    final double angularDist = Vector3D.angle(thisVector, otherVector);
                    isEqual &= MathLib.abs(angularDist) <= angularTol;
                }
            }        
        }
        
        return isEqual;
    }

    /**
     * Class to generate random vector that are contained in the
     * provided directions vectors. To do so, the polyhedron base
     * is meshed.
     * 
     * @author Thales
     */
    public static class VectorGenerator {

        /** Meshes of the polygon. */
        private final List<Triangle> triangles;

        /** Random number generator. */
        private final RandomGenerator rng;

        /**
         * Simple constructor, build the mesh of the polygon with the
         * provided vertices.
         * 
         * @param vertices
         *        the vertices of the polygon.
         * @param generator
         *        random number generator
         */
        public VectorGenerator(final Vector3D[] vertices, final RandomGenerator generator) {

            final List<Triangle> meshes = new ArrayList<>();

            // origin point of the meshs
            final Vertex v0 = new Vertex(0, vertices[0]);
            for (int i = 1; i < vertices.length - 1; i++) {

                final Vertex v1 = new Vertex(i, vertices[i]);
                final Vertex v2 = new Vertex(i + 1, vertices[i + 1]);
                meshes.add(new Triangle(v0, v1, v2));
            }

            this.triangles = meshes;

            this.rng = generator;
        }

        /**
         * Generate a random point inside the polygon's base.
         * 
         * @return random vector inside the polyhedron
         */
        public Vector3D generateVector() {

            // retrieve a random triangle of the mesh
            final int indexTriangle = this.rng.nextInt(this.triangles.size());
            final Triangle triangle = this.triangles.get(indexTriangle);

            // triangle points
            final Vector3D p0 = triangle.getVertices()[0].getPosition();
            final Vector3D p1 = triangle.getVertices()[1].getPosition();
            final Vector3D p2 = triangle.getVertices()[2].getPosition();

            // build the directions
            final Vector3D d1 = p1.subtract(p0);
            final Vector3D d2 = p2.subtract(p0);

            // scalar coefficient
            double u1 = this.rng.nextDouble();
            double u2 = this.rng.nextDouble();

            if (u1 + u2 > 1) {
                u1 = 1 - u1;
                u2 = 1 - u2;
            }

            final Vector3D vectInPlane = new Vector3D(u1, d1, u2, d2);

            return p0.add(vectInPlane);
        }
    }

    /**
     * This class builds {@link PyramidalField} with rectangular base defined by
     * 
     * @author Thales
     */
    public static class RectangularPyramidalFieldBuilder {

        private final Vector3D[] points;

        private final Vector3D barycenter;
        
        private final Vector3D normal;
        
        /**
         * Build a simple rectangle in 3D space from the provided points.
         * 
         * @param nameIn
         *        the name
         * @param points
         *        the points delimiting the rectangular field
         */

        public RectangularPyramidalFieldBuilder(final Vector3D[] pointsIn) {

            if (pointsIn.length != 4) {
                // 4 points are expected to build the rectangular 2D field
                throw new MathIllegalArgumentException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 4, pointsIn.length);
            }
            
            // Sort the points clockwise
            this.points = pointsIn;
            
            // Normal vector of the base directed towad +Z
            final Plane plane = new Plane(pointsIn[0], pointsIn[1], pointsIn[2]);
            this.normal = plane.getNormal();

            final Line diag1 = new Line(pointsIn[0], pointsIn[2]);
            final Line diag2 = new Line(pointsIn[1], pointsIn[3]);

            this.barycenter = diag1.intersection(diag2);
        }

        /**
         * Build a rectangular base delimited by abscissa and ordinates boundaries. The base plane
         * is parallel to the plane (O, +I, +J).
         * 
         * @param xMin
         *        the min abscissa
         * @param xMax
         *        the max abscissa
         * @param yMin
         *        the min ordinate
         * @param yMax
         *        the max ordinate
         * @param offsetZ
         *        the plane offset
         */
        public RectangularPyramidalFieldBuilder(final double xMin, final double xMax,
                                                final double yMin, final double yMax, final double offsetZ) {

            // Points are sorted counter clockwise
            this.points = new Vector3D[4];
            this.points[0] = new Vector3D(xMax, yMax, offsetZ);
            this.points[1] = new Vector3D(xMin, yMax, offsetZ);
            this.points[2] = new Vector3D(xMin, yMin, offsetZ);
            this.points[3] = new Vector3D(xMax, yMin, offsetZ);
            
            this.normal = Vector3D.PLUS_K;
            
            final Line diag1 = new Line(this.points[0], this.points[2]);
            final Line diag2 = new Line(this.points[1], this.points[3]);
            this.barycenter = diag1.intersection(diag2);
        }

        /**
         * Build a new {@link RectangularFieldBuilder} applying a translation of the initial base vectors.
         * 
         * @param translation
         *        the translation vector to apply
         * 
         * @return the new rectangular base
         */
        public RectangularPyramidalFieldBuilder translate(final Vector3D translation) {
            // transformed points
            final Vector3D[] newPoints = new Vector3D[this.points.length];

            for (int i = 0; i < this.points.length; i++) {
                newPoints[i] = this.points[i].add(translation);
            }

            return new RectangularPyramidalFieldBuilder(newPoints);
        }

        /**
         * Build a new {@link RectangularFieldBuilder} by rotating the initial base vectors in the plane.
         * 
         * @param angle
         *        the angle rotation
         * 
         * @return the new rectangular base
         */
        public RectangularPyramidalFieldBuilder rotate(final double angle) {
            // rotation over +K axis
            final Rotation rotation = new Rotation(Vector3D.PLUS_K, angle);

            // transformed points
            final Vector3D[] newPoints = new Vector3D[this.points.length];

            for (int i = 0; i < this.points.length; i++) {
                newPoints[i] = rotation.applyTo(this.points[i]);
            }

            return new RectangularPyramidalFieldBuilder(newPoints);
        }

        /**
         * Build a new {@link RectangularFieldBuilder} by rotating the initial base vectors in the plane.
         * 
         * @param scalingFactor
         *        the factor to apply
         * 
         * @return the new rectangular base
         */
        public RectangularPyramidalFieldBuilder scale(final double scalingFactor) {

            // transformed points
            final Vector3D[] newPoints = new Vector3D[this.points.length];

            for (int i = 0; i < this.points.length; i++) {
                final Vector3D direction = this.points[i].subtract(this.barycenter);
//                newPoints[i] = this.points[i].add(direction.scalarMultiply(scalingFactor - 1));
                newPoints[i] = new Vector3D(1., this.barycenter, scalingFactor, direction);
            }

            return new RectangularPyramidalFieldBuilder(newPoints);
        }

        /**
         * Compute the polygon of the polyhedron's base projected in XY plane.
         * 
         * @return the base polygon
         */
        public PolygonsSet getBasePolygonXYPlane() {

            final Vector2D[][] vertices = new Vector2D[1][this.points.length];
            for (int index = 0; index < this.points.length; index++) {
                vertices[0][index] = new Vector2D(this.points[index].getX(), this.points[index].getY());
            }

            return new PolygonsSet(vertices);
        }

        /**
         * Build the pyramid field of view corresponding to this rectangular base.
         * 
         * @param origin
         *        the coordinates of the pyramidal field top
         * 
         * @return the pyramidal field.
         */
        public PyramidalField build(final String name, final Vector3D origin) {
            // the pyramidal field directions
            final Vector3D[] directions = new Vector3D[this.points.length];
            
            final Vector3D axis = origin.subtract(this.barycenter);
            // Fill the direction array so that d[i-1] times d[i] oriented inside the 
            // the pyramidal field
            if(axis.dotProduct(this.normal) < 0){
                for (int i = 0; i < directions.length; i++) {
                    directions[i] = this.points[i].subtract(origin);
                }
            }else{
                for (int i = 0; i < directions.length; i++) {
                    directions[i] = this.points[directions.length - i - 1].subtract(origin);
                }
            }     

            return new PyramidalField(name, directions);
        }
    }
}
