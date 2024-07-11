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
 * @history 23/01/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
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
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * Validation tests for geomagnetic field factory
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id: GeoMagneticFieldFactoryValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */

public class GeoMagneticFieldFactoryValTest {

    private final static double EPSILON_NT = 0.015;
    private final static double EPSILON_REG = 1E-10;
    private static Validate VAL;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Geomagnetic field
         * 
         * @featureDescription validation of the geomagnetic models
         * 
         * @coveredRequirements DV-MOD_320
         */
        GEOMAGNETIC
    }

    /**
     * Initialize Validate class
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VAL = new Validate(GeoMagneticFieldFactoryValTest.class);
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
     * @testedFeature {@link features#GEOMAGNETIC}
     * 
     * @testedMethod {@link GeoMagneticFieldFactory#getIGRF(double)}
     * 
     * @description A C reference program has been used to generate reference data based on IGRF model :<br>
     *              600 points for :<br>
     *              <li>date from 2010 to 2014 every 6 months <li>latitude between [90°;90°] with step 45° (the
     *              reference program gives Nan at latitude -90° and 90°, the reference was computed at 89.99°) <li>
     *              longitude between [-180°;180°] with step 90° <li>altitude at 0 km, 300 km and 600 km
     * 
     *              For each reference input data (date, latitude, longitude) compute the magnetic field vector B from
     *              Orekit IGRF model in object GeoMagneticElements.<br>
     * 
     *              Use the validate class to produce log.
     * 
     * @input output data from the C reference program for IGRF model
     * 
     * @output validate report and result for regression validation in further release
     * 
     * @testPassCriteria Difference for each coordinate of all reference data under 0.015 (due to computation difference
     *                   with the reference C program recompile under Linux)<br>
     *                   and E-10 for regression
     * 
     * @see http://www.ngdc.noaa.gov/geomag/models.shtml
     * 
     * @comments
     * 
     * @referenceVersion none
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetIGRF() throws IOException, PatriusException {
        this.executeValidation("IGRF");
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#GEOMAGNETIC}
     * 
     * @testedMethod {@link GeoMagneticFieldFactory#getWMM(double)}
     * 
     * @description Same validation than IGRF model with WMM model external reference.
     *              Use the validate class to produce log
     * 
     * @input output data from the C reference program for WMM model
     * 
     * @output validate report and result for regression validation in further release
     * 
     * @testPassCriteria Difference for each coordinate of all reference data under 0.015 (due to computation difference
     *                   with the reference C program recompile under Linux)<br>
     *                   and E-10 for regression
     * 
     * @see http://www.ngdc.noaa.gov/geomag-web/#igrfwmm
     * 
     * @comments
     * 
     * @referenceVersion none
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetWMM() throws IOException, PatriusException {
        this.executeValidation("WMM");
    }

    /**
     * Body of the test method for any model.
     */
    private void executeValidation(final String modelName) throws IOException, PatriusException {

        GeoMagneticField geoMagField = null;

        // Get inputs and outputs from external reference file
        final List<GeoMagRefInput> inputDataList = ReferenceReader.readInput("geomagnetic-validation-data",
            "/geomagnetic-validation-data/reference_data_" + modelName + ".txt");
        final List<GeoMagRefOutput> outputDataList = ReferenceReader.readOutputExternalReference(
            "geomagnetic-validation-data", "/geomagnetic-validation-data/reference_data_" + modelName + ".txt");

        // Get regression reference output
        final List<GeoMagRefOutput> regressionOutputData = ReferenceReader.readRegressionReference(
            "geomagnetic-validation-data", "/geomagnetic-validation-data/RegressionReference" + modelName
                + "Elements.txt");

        // Magnetic field list computed from input reference to write regression result reference
        final List<GeoMagneticElements> geoMagElementList = new ArrayList<GeoMagneticElements>();
        GeoMagneticElements geoMagElement;

        // For each input, compute the Patrius geomagnetic element and :
        // 1. compare the Vector3D with the reference C program results
        // 2. compare the attributes of geomagneticelements to the regressiondatas
        // 3. Fill a list to write the regression reference
        int i = 0;
        for (final GeoMagRefInput curentInput : inputDataList) {

            // Compute geomagnetic field for this date
            if (modelName.equals("IGRF")) {
                geoMagField = GeoMagneticFieldFactory.getIGRF(curentInput.getDecimalYear());
            } else if (modelName.equals("WMM")) {
                geoMagField = GeoMagneticFieldFactory.getWMM(curentInput.getDecimalYear());
            }

            // Compute the element for this latitude, longitude, altitude and save it to write result for regression
            geoMagElement = geoMagField.calculateField(curentInput.getLatitude(), curentInput.getLongitude(),
                curentInput.getAltitude());
            geoMagElementList.add(geoMagElement);

            // Get the external reference output
            final GeoMagRefOutput refOutput = outputDataList.get(i);

            // Get the regression reference
            final GeoMagRefOutput regressionRef = regressionOutputData.get(i);

            VAL.assertEquals(geoMagElement.getFieldVector().getX(), regressionRef.getB().getX(), EPSILON_REG,
                refOutput.getB().getX(), EPSILON_NT, "X component's field vector at date : "
                    + inputDataList.get(i).getDecimalYear() +
                    ", latitude :" + inputDataList.get(i).getLatitude() +
                    ", longitude :" + inputDataList.get(i).getLongitude() +
                    ", altitude :" + inputDataList.get(i).getAltitude());

            VAL.assertEquals(geoMagElement.getFieldVector().getY(), regressionRef.getB().getY(), EPSILON_REG,
                refOutput.getB().getY(), EPSILON_NT, "Y component's field vector at date : "
                    + inputDataList.get(i).getDecimalYear() +
                    ", latitude :" + inputDataList.get(i).getLatitude() +
                    ", longitude :" + inputDataList.get(i).getLongitude() +
                    ", altitude :" + inputDataList.get(i).getAltitude());

            VAL.assertEquals(geoMagElement.getFieldVector().getZ(), regressionRef.getB().getZ(), EPSILON_REG,
                refOutput.getB().getZ(), EPSILON_NT, "Z component's field vector at date : "
                    + inputDataList.get(i).getDecimalYear() +
                    ", latitude :" + inputDataList.get(i).getLatitude() +
                    ", longitude :" + inputDataList.get(i).getLongitude() +
                    ", altitude :" + inputDataList.get(i).getAltitude());

            i++;
        }

        // Write the result of this release. The user can copy this file in the resources to use it as reference for
        // future release
        ReferenceReader.writeResultFile("RegressionReference" + modelName + "Elements.txt", geoMagElementList);

    }
}
