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
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:217:10/03/2014:Corrected erroneous initialization of base vectors
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.RectangleField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the rectangle field of view
 *              </p>
 * 
 * @see RectangleField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RectangleFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Rectangle field of view
         * 
         * @featureDescription Rectangle field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        RECTANGLE_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * FA-3110: specific case with U-direction perpendicular to facet and direction exactly lying exactly on FOV pyramidal facet.
     */
    @Test 
    public void perpRectFieldTest() { 
        final String name = "rectangleField";
        final Vector3D mainDirection = new Vector3D(1.0, 0.0, 0.0); 
        final Vector3D approximationU = new Vector3D(0.0, 1.0, 0.0); 
  
        // Instantiation of the RectangleField 
        final RectangleField rectField = new RectangleField(name , mainDirection, 
                approximationU, MathLib.toRadians(45.0), MathLib.toRadians(15.0)); 
  
        final Vector3D direction = new Vector3D(1.0, 1.0, 0.0); 
        final double angDist = rectField.getAngularDistance(direction);
        Assert.assertEquals(0., angDist);
    } 

    /**
     * FA-3110: projection on field faces is larger than distance to vectors defining the field.
     * This occurs for very small FOV (1E-10 rad)
     */
    @Test 
    public void rectFieldEpsTest() { 
        // epsilon is 1e-10 
        final double epsilon = 1e-10; 
        final Vector3D mainDirection = new Vector3D(1.0, 0.0, 0.0); 
        final Vector3D approximationU = new Vector3D(0.0, 1.0, 0.0); 
        final RectangleField rectField = new RectangleField("", mainDirection, approximationU, epsilon, epsilon); 

        // Test both cases
        final Vector3D direction1 = new Vector3D(0.8, 0.0, 1.0); 
        Assert.assertFalse(rectField.isInTheField(direction1));

        final Vector3D direction2 = new Vector3D(0.8, 1.0, 0.0); 
        Assert.assertFalse(rectField.isInTheField(direction2));

        // Origin of the problem: distance to projection is larger to distance of another vector in same plane
//        final Vector3D projection = new Vector3D(0.8000000001, 0, 8.000000001000001E-11);
//        final Vector3D v1 = new Vector3D(1, -0.0000000001, 0.0000000001);
//        System.out.println(Vector3D.angle(direction, projection));
//        System.out.println(Vector3D.angle(direction, v1));
    } 

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_FIELD}
     * 
     * @testedMethod {@link RectangleField#RectangleField(String, Vector3D, Vector3D, double, double)}
     * @testedMethod {@link RectangleField#getU()}
     * @testedMethod {@link RectangleField#getV()}
     * @testedMethod {@link RectangleField#getW()}
     * 
     * @description test of the constructor for erroneous initialisation
     * 
     * @input mainDirection = (1,0,1) and approximativeU = (1,0,0)
     * 
     * @output u = (1, 0, -1) / sqrt(2)
     * 
     * @testPassCriteria U is correctly calculated (erroneous case was u = (0, 0, -1))
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testCorrectedConstructor() {

        final Vector3D mainDirection = new Vector3D(1, 0, 1);
        final Vector3D approximativeU = new Vector3D(1, 0, 0);

        final RectangleField field = new RectangleField("rect", mainDirection, approximativeU, .5, .4);

        // not the old result
        Assert.assertFalse(new Vector3D(0, 0, -1).equals(field.getW()));

        // a correct result!
        // normalize
        final Vector3D mainDir = mainDirection.normalize();
        // compute parallel component of U
        final Vector3D mainDirComponentOfU = new Vector3D(Vector3D.dotProduct(mainDir, approximativeU), mainDir);
        // subtract it and normalize result
        final Vector3D correctedU = approximativeU.subtract(mainDirComponentOfU).normalize();

        // the new result!
        Assert.assertTrue(correctedU.equals(field.getU()));

        // test the other base vectors
        Assert.assertTrue(mainDir.equals(field.getW()));
        Assert.assertTrue(Vector3D.crossProduct(mainDir, correctedU).equals(field.getV()));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_FIELD}
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D)}
     * @testedMethod {@link RectangleField#isInTheField(Vector3D)}
     * @testedMethod {@link RectangleField#getName()}
     * 
     * @description test of the basic methods of a rectangle field of view
     * 
     * @input a rectangle field of view, some vectors
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
    public void rectangleField() {

        final String name = "rectangleField";

        final Vector3D mainDirection = Vector3D.PLUS_K;
        final Vector3D uVector = new Vector3D(0.5, 0.0, 0.5);

        // tests with wrong angular aperture
        try {
            new RectangleField(name, mainDirection, uVector, 0.5, -0.1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector, 3.7, 1.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector, -0.5, 1.2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector, 0.5, 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector,
                FastMath.PI / 4.0, 3.0 * FastMath.PI / 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector,
                3.0 * FastMath.PI / 4.0, FastMath.PI / 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong direction vector
        try {
            new RectangleField(name, Vector3D.ZERO, uVector, 0.5, 0.5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // tests with wrong U vector
        try {
            new RectangleField(name, mainDirection, Vector3D.ZERO, 0.5, 0.5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, mainDirection, 0.5, 0.5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // tests with a right field
        RectangleField field = new RectangleField(name, mainDirection, uVector,
            FastMath.PI / 4.0, FastMath.PI / 8.0);

        // test with a vector in the field
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            FastMath.PI / 4.0 - MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of the field
        testedDirection = new Vector3D(0.0, 2.0, 1.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            -FastMath.PI / 4.0 + MathLib.atan2(1.0, 2.0) - FastMath.PI / 8.0, this.comparisonEpsilon);

        // test with a vector closest to an edge, outside
        field = new RectangleField(name, mainDirection, uVector,
            FastMath.PI / 4.0, FastMath.PI / 4.0);

        testedDirection = new Vector3D(2.0, 2.0, 0.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        Assert.assertEquals(name, field.getName());

        // test with reversed cone, and a vector closest to an edge, inside
        field = new RectangleField(name, mainDirection, uVector,
            3.0 * FastMath.PI / 4.0, 3.0 * FastMath.PI / 4.0);

        testedDirection = new Vector3D(2.0, 2.0, 0.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        Assert.assertEquals(name, field.getName());

        // test with zero direction
        testedDirection = Vector3D.ZERO;
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            0.0, this.comparisonEpsilon);
        
        // Test side axis (reference: math)
        final RectangleField sideField = new RectangleField("", Vector3D.PLUS_I, Vector3D.PLUS_J, FastMath.PI / 2., FastMath.PI / 2.);
        final Vector3D[] sideAxis = sideField.getSideAxis();
        final double sqrt2Over2 = MathLib.sqrt(2.) / 2.;
        Assert.assertEquals(0., sideAxis[0].distance(new Vector3D(0, sqrt2Over2, sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[1].distance(new Vector3D(0, -sqrt2Over2, sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[2].distance(new Vector3D(0, -sqrt2Over2, -sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[3].distance(new Vector3D(0, sqrt2Over2, -sqrt2Over2)), 1E-16);
    }

}
