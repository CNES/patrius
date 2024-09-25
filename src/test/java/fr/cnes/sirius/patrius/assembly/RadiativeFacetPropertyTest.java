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
 * @history creation 26/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1192:30/08/2017:update parts frame
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Test class for the RadiativeFacetProperty property.
 *              </p>
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class RadiativeFacetPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle RadiativeFacetProperty property.
         * 
         * @featureDescription RadiativeFacetProperty property for an assembly's part.
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430
         */
        RADIATIVE_FACET_PROPERTY
    }

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";

    /**
     * 2nd part's name
     */
    private final String part2 = "part2";

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_FACET_PROPERTY}
     * 
     * @testedMethod {@link RadiativeFacetProperty#RadiativeFacetProperty(Facet)}
     * 
     * @description Creation of an assembly and testing the radiative facet property of the parts.
     * 
     * @input Assembly with radiative facet properties.
     * 
     * @output RadiativeFacetProperty property.
     * 
     * @testPassCriteria The radiative facet properties are right.
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @Test
    public void radiativeFacetPropertyTest() {
        Utils.clear();
        /*
         * Test on a simple spacecraft
         */

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // properties adding
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

            // create the facet for the main part
            final Vector3D mainNormal = new Vector3D(2., 1.3, 0.9);
            final double mainArea = 25.9;
            final Facet mainFacet = new Facet(mainNormal, mainArea);

            final Vector3D partNormal = new Vector3D(0., 0., 3.);
            final double partArea = 0.6;
            final Facet partFacet = new Facet(partNormal, partArea);

            // add the radiative facet properties
            final IPartProperty radMainFacetProp = new RadiativeFacetProperty(mainFacet);
            builder.addProperty(radMainFacetProp, this.mainBody);

            final IPartProperty radPartFacetProp = new RadiativeFacetProperty(partFacet);
            builder.addProperty(radPartFacetProp, this.part2);

            // add the radiative properties on parts
            final IPartProperty radPropMain = new RadiativeProperty(0.7, 0.2, 0.2);
            builder.addProperty(radPropMain, this.mainBody);

            final IPartProperty radPropPart2 = new RadiativeProperty(0.5, 0.1, 0.);
            builder.addProperty(radPropPart2, this.part2);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            final IPart mainPart = assembly.getPart(this.mainBody);
            final IPart partTwo = assembly.getPart(this.part2);

            final IPartProperty propMainPart = mainPart.getProperty(PropertyType.RADIATIVE_FACET);
            final IPartProperty proPart2 = partTwo.getProperty(PropertyType.RADIATIVE_FACET);

            // main part
            final Facet mainPropFacet = ((RadiativeFacetProperty) propMainPart).getFacet();
            // get area
            Assert.assertEquals(25.9, mainPropFacet.getArea(), this.comparisonEpsilon);
            // get normal
            Assert.assertEquals(0.0, mainPropFacet.getNormal().subtract(mainNormal.normalize()).getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

            // part 2
            final Facet part2PropFacet = ((RadiativeFacetProperty) proPart2).getFacet();
            // get area
            Assert.assertEquals(0.6, part2PropFacet.getArea(), this.comparisonEpsilon);
            // get normal
            Assert.assertEquals(0.0, part2PropFacet.getNormal().subtract(partNormal.normalize()).getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

}
