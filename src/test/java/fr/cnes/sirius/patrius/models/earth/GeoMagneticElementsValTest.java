/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.tools.validationTool.Validate;

/**
 * <p>
 * Unit test for the geomagnetic field elements
 * </p>
 * 
 * @author chabaudp
 * 
 * @version $Id: GeoMagneticElementsValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class GeoMagneticElementsValTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Geomagnetic field elements
         * 
         * @featureDescription validation of the geomagnetic elements
         * 
         * @coveredRequirements DV-MOD_320
         */
        GEOMAGNETIC_ELEMENTS
    }

    private final static double EPSILON_REF_NT = 1.1E-10;
    private final static double EPSILON_REF_ANGLE = 1E-4;
    private final static double EPSILON_REG = 1E-10;
    private static Validate VAL;

    /**
     * Initialize Validate class
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VAL = new Validate(GeoMagneticElementsValTest.class);
    }

    /**
     * Write the log
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        VAL.produceLog();
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#GEOMAGNETIC_ELEMENTS}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.models.earth.GeoMagneticElements#GeoMagneticElements(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D)}
     * 
     * @description A C reference program has been used to generate reference data based on IGRF model :<br>
     *              600 points for :<br>
     *              <li>date from 2010 to 2014 every 6 months <li>latitude between [90°;90°] with step 45° (the
     *              reference program gives Nan at latitude -90° and 90°, the reference was computed at 89.99°) <li>
     *              longitude between [-180°;180°] with step 90° <li>altitude at 0 km, 300 km and 600 km
     * 
     *              Computes an Orekit geomagnetic elements from each magnetic field vector B reference and compare the
     *              different computed parameters declination, inclination, horizontal intensity, and total intensity to
     *              the reference values.
     * 
     * @input reference data based on IGRF model, and regression reference data from previous release
     * 
     * @output validate report and results to copy in resources for further release regression validation
     * 
     * @testPassCriteria difference with reference less than 1.1E-10 for nano tesla, and 1E-4 for angle (due to
     *                   reference parameter precision)<BR>
     *                   difference with previous release less than E-10
     * 
     * @see http://www.ngdc.noaa.gov/geomag/models.shtml
     * 
     * @comments none
     * 
     * @referenceVersion none
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGeoMagneticElements() throws IOException {

        // Magnetic field list computed from input reference to write regression result reference
        final List<GeoMagneticElements> geoMagElementList = new ArrayList<>();
        GeoMagneticElements currentOrekitElement;

        // Build a list of reference output (x,y,z,i,d,f,h) from the reference file
        final List<GeoMagRefOutput> referenceOutputList = ReferenceReader.readOutputExternalReference(
            "geomagnetic-validation-data", "/geomagnetic-validation-data/reference_data_WMM.txt");
        // Get regression reference output
        final List<GeoMagRefOutput> regressionOutputData = ReferenceReader.readRegressionReference(
            "geomagnetic-validation-data", "/geomagnetic-validation-data/RegressionElementsAttributeRef.txt");

        // For each input, compute the orekit geomagnetic element from the Vector3D and compare each value
        int i = 0;
        for (final GeoMagRefOutput refOutput : referenceOutputList) {

            // Compute geomagnetic element from reference B vector
            currentOrekitElement = new GeoMagneticElements(refOutput.getB());
            geoMagElementList.add(currentOrekitElement);

            final GeoMagRefOutput regressionRef = regressionOutputData.get(i);

            VAL.assertEquals(currentOrekitElement.getDeclination(), regressionRef.getD(), EPSILON_REG,
                refOutput.getD(), EPSILON_REF_ANGLE, "Declination for the vector : (" + refOutput.getB().getX() +
                    ", " + refOutput.getB().getY() +
                    ", " + refOutput.getB().getZ() + ")");
            VAL.assertEquals(currentOrekitElement.getTotalIntensity(), regressionRef.getF(), EPSILON_REG,
                refOutput.getF(), EPSILON_REF_NT, "Total Intensity for the vector : (" + refOutput.getB().getX() +
                    ", " + refOutput.getB().getY() +
                    ", " + refOutput.getB().getZ() + ")");
            VAL.assertEquals(currentOrekitElement.getHorizontalIntensity(), regressionRef.getH(), EPSILON_REG,
                refOutput.getH(), EPSILON_REF_NT, "Horizontal Intensity for the vector : ("
                    + refOutput.getB().getX() +
                    ", " + refOutput.getB().getY() +
                    ", " + refOutput.getB().getZ() + ")");
            VAL.assertEquals(currentOrekitElement.getInclination(), regressionRef.getI(), EPSILON_REG,
                refOutput.getI(), EPSILON_REF_ANGLE, "Inclination for the vector : (" + refOutput.getB().getX() +
                    ", " + refOutput.getB().getY() +
                    ", " + refOutput.getB().getZ() + ")");

            i++;
        }

        // Write the result of this release. The user can copy this file in the resources to use it as reference for
        // future release
        ReferenceReader.writeResultFile("RegressionElementsAttributeRef.txt", geoMagElementList);

    }

}
