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
 * @history creation 8/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the Facet cross section provider
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class FacetTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Facet
         * 
         * @featureDescription Facet cross section provider
         * 
         * @coveredRequirements DV-VEHICULE_110
         */
        FACET
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FACET}
     * 
     * @testedMethod {@link Facet#getArea()}
     * @testedMethod {@link Facet#getNormal()}
     * @testedMethod {@link Facet#getCrossSection(Vector3D)}
     * 
     * @description Creation of a facet and test of all its basic methods
     * 
     * @input an area and a normal vector to create the facet, and a direction
     *        to compute the cross section
     * 
     * @output basic properties (area and normal) and cross section
     * 
     * @testPassCriteria the cross section is the expected one
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @Test
    public void facetTest() {

        // facet creation
        final Vector3D normal = new Vector3D(0.0, 0.0, 2.0);
        final Facet facet = new Facet(normal, 7.0);

        // test of the getters
        Assert.assertEquals(7.0, facet.getArea(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, facet.getNormal().getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, facet.getNormal().getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, facet.getNormal().getZ(), this.comparisonEpsilon);

        // direction
        final Vector3D direction1 = new Vector3D(0.0, 2.0, -2.0);
        final Vector3D direction2 = new Vector3D(2.0, 2.0, -2.0);
        final Vector3D direction3 = new Vector3D(2.0, 2.0, 2.0);

        // test of the cross sections
        double crossSection = facet.getCrossSection(direction1);
        Assert.assertEquals(7.0 / MathLib.sqrt(2.0), crossSection, this.comparisonEpsilon);
        crossSection = facet.getCrossSection(direction2);
        Assert.assertEquals(7.0 / MathLib.sqrt(3.0), crossSection, this.comparisonEpsilon);
        crossSection = facet.getCrossSection(direction3);
        Assert.assertEquals(0.0, crossSection, this.comparisonEpsilon);
    }

}
