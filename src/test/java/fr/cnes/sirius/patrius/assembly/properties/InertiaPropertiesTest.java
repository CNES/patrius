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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.MainPart;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              This is a test class for all inertia properties.
 *              </p>
 * 
 * @see InertiaCylinderProperty
 * @see InertiaParallelepipedProperty
 * @see InertiaSphereProperty
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class InertiaPropertiesTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Inertia properties
         * 
         * @featureDescription All inertia part's properties :
         *                     sphere, cylinder, parallelepiped.
         * 
         * @coveredRequirements DV-VEHICULE_130, DV-VEHICULE_140, DV-VEHICULE_170
         */
        INERTIA_PROPERTIES
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_PROPERTIES}
     * 
     * @testedMethod {@link InertiaCylinderProperty#getInertiaMatrix()}
     * @testedMethod {@link InertiaCylinderProperty#getMass()}
     * @testedMethod {@link InertiaCylinderProperty#getMassCenter()}
     * 
     * @description Creation of a "cylinder" inertia property and test of the getters and computations.
     * 
     * @input a "cylinder" inertia property
     * 
     * @output the mass, inertia matrix, mass center and reference frame
     * 
     * @testPassCriteria the output values are the expected ones, particularly the computed
     *                   inertia matrix.
     * 
     * @see InertiaCylinderProperty
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void cylinderTest() throws PatriusException {
        Utils.clear();
        
        final MassProperty massProp = new MassProperty(55.0);
        IInertiaProperty inertiaProp;
        final IPart part = new MainPart("part");
        part.addProperty(massProp);

        try {
            inertiaProp = new InertiaCylinderProperty(-5.0, 12.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inertiaProp = new InertiaCylinderProperty(0.0, 12.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inertiaProp = new InertiaCylinderProperty(5.0, -12.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inertiaProp = new InertiaCylinderProperty(5.0, 0.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        inertiaProp = new InertiaCylinderProperty(5.0, 12.0, massProp);

        Assert.assertEquals(55.0, inertiaProp.getMass(), this.comparisonEpsilon);
        final double[][] dataIn = { { 2983.75, 0.0, 0.0 },
            { 0.0, 2983.75, 0.0 }, { 0.0, 0.0, 687.5 } };
        final Matrix3D expectedMatrix = new Matrix3D(dataIn);
        Assert.assertTrue(expectedMatrix.equals(inertiaProp.getInertiaMatrix()));

        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getY(), this.comparisonEpsilon);
        Assert.assertEquals(6.0, inertiaProp.getMassCenter().getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_PROPERTIES}
     * 
     * @testedMethod {@link InertiaSphereProperty#getInertiaMatrix()}
     * @testedMethod {@link InertiaSphereProperty#getMass()}
     * @testedMethod {@link InertiaSphereProperty#getMassCenter()}
     * 
     * @description Creation of a "sphere" inertia property and test of the getters and computations.
     * 
     * @input a "sphere" inertia property
     * 
     * @output the mass, inertia matrix, mass center and reference frame
     * 
     * @testPassCriteria the output values are the expected ones, particularly the computed
     *                   inertia matrix.
     * 
     * @see InertiaSphereProperty
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void sphereTest() throws PatriusException {
        Utils.clear();
        
        final IPart part = new MainPart("part");
        final MassProperty massProp = new MassProperty(55.0);
        IInertiaProperty inertiaProp;
        part.addProperty(massProp);

        try {
            inertiaProp = new InertiaSphereProperty(-5.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inertiaProp = new InertiaSphereProperty(0.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        inertiaProp = new InertiaSphereProperty(5.0, massProp);

        Assert.assertEquals(55.0, inertiaProp.getMass(), this.comparisonEpsilon);
        final double[][] dataIn = { { 550.0, 0.0, 0.0 },
            { 0.0, 550.0, 0.0 }, { 0.0, 0.0, 550.0 } };
        final Matrix3D expectedMatrix = new Matrix3D(dataIn);
        Assert.assertTrue(expectedMatrix.equals(inertiaProp.getInertiaMatrix()));

        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_PROPERTIES}
     * 
     * @testedMethod {@link InertiaParallelepipedProperty#getInertiaMatrix()}
     * @testedMethod {@link InertiaParallelepipedProperty#getMass()}
     * @testedMethod {@link InertiaParallelepipedProperty#getMassCenter()}
     * 
     * @description Creation of a "parallelepiped" inertia property and test of the getters and computations.
     * 
     * @input a "parallelepiped" inertia property
     * 
     * @output the mass, inertia matrix, mass center and reference frame
     * 
     * @testPassCriteria the output values are the expected ones, particularly the computed
     *                   inertia matrix.
     * 
     * @see InertiaParallelepipedProperty
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void parallelepipedTest() throws PatriusException {
        Utils.clear();
        
        final IPart part = new MainPart("part");
        final MassProperty massProp = new MassProperty(55.0);
        IInertiaProperty inertiaProp;
        part.addProperty(massProp);

        try {
            inertiaProp = new InertiaParallelepipedProperty(-6.0, 12.0, 27.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inertiaProp = new InertiaParallelepipedProperty(6.0, -12.0, 27.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inertiaProp = new InertiaParallelepipedProperty(6.0, 12.0, -27.0, massProp);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        inertiaProp = new InertiaParallelepipedProperty(6.0, 12.0, 27.0, massProp);

        Assert.assertEquals(55.0, inertiaProp.getMass(), this.comparisonEpsilon);
        final double[][] dataIn = { { 4001.25, 0.0, 0.0 },
            { 0.0, 3506.25, 0.0 }, { 0.0, 0.0, 825.0 } };
        final Matrix3D expectedMatrix = new Matrix3D(dataIn);
        Assert.assertTrue(expectedMatrix.equals(inertiaProp.getInertiaMatrix()));

        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, inertiaProp.getMassCenter().getZ(), this.comparisonEpsilon);

    }

}
