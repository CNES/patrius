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
 * @history creation 15/05/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.SectorField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the "sector" field of view
 *              </p>
 * 
 * @see SectorField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SectorFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Sector field of view
         * 
         * @featureDescription Sector field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VISI_40, DV-VEHICULE_270, DV-VEHICULE_250
         */
        SECTOR_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SECTOR_FIELD}
     * 
     * @testedMethod {@link SectorField#getAngularDistance(Vector3D)}
     * @testedMethod {@link SectorField#isInTheField(Vector3D)}
     * @testedMethod {@link SectorField#getName()}
     * 
     * @description test of the basic methods of a sector field of view
     * 
     * @input a sector field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void sectorField() {

        final String name = "sectorField";
        final Vector3D vectorPole = Vector3D.PLUS_I;

        // wrong sector fields creations
        try {
            final Vector3D vectorV1 = new Vector3D(2.0, 6.0, 4.0);
            final Vector3D vectorV2 = new Vector3D(-2.0, 4.0, 6.0);
            new SectorField(name, vectorPole, vectorV1, vectorV2);
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            final Vector3D vectorV1 = new Vector3D(-2.0, 0.0, 0.0);
            final Vector3D vectorV2 = new Vector3D(2.0, 4.0, 6.0);
            new SectorField(name, vectorPole, vectorV1, vectorV2);
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            final Vector3D vectorV1 = new Vector3D(-2.0, 6.0, 4.0);
            final Vector3D vectorV2 = new Vector3D(2.0, 0.0, 0.0);
            new SectorField(name, vectorPole, vectorV1, vectorV2);
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // field creation
        final Vector3D vectorV1 = new Vector3D(-1.0, 1.0, 0.0);
        final Vector3D vectorV2 = new Vector3D(1.0, 0.0, 1.0);
        final IFieldOfView field = new SectorField(name, vectorPole, vectorV1, vectorV2);

        // test with a vector in the middle of the field
        Vector3D testedDirection = new Vector3D(0.0, 1.0, 1.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(FastMath.PI / 4.0, field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // test with a vector in the field, closest to a meridian
        testedDirection = new Vector3D(0.0, 2.0, 1.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(MathLib.atan2(1, 2), field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // test with a vector on a limit parallel
        testedDirection = Vector3D.PLUS_I.add((new Vector3D(0.0, 1.0, 1.0)).normalize());

        Assert.assertEquals(field.getAngularDistance(testedDirection),
            0.0, this.comparisonEpsilon);

        // test with a vector out of the field, closest to a parallel
        testedDirection = Vector3D.PLUS_I.add(0.5, (new Vector3D(0.0, 1.0, 1.0)).normalize());

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(MathLib.atan2(1, 2) - FastMath.PI / 4.0, field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // test with a vector out of the field, closest to a meridian
        testedDirection = new Vector3D(0.0, 2.0, -1.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(-MathLib.atan2(1, 2), field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // name test
        Assert.assertEquals(name, field.getName());

        // test with zero direction
        testedDirection = Vector3D.ZERO;
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            0.0, this.comparisonEpsilon);
    }

}
