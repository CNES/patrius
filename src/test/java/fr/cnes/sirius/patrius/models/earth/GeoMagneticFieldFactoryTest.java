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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test for geomagnetic field factory
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id: GeoMagneticFieldFactoryTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class GeoMagneticFieldFactoryTest {

    private final static double EPSILON_NT = 1.0;

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
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#GEOMAGNETIC}
     * 
     * @testedMethod {@link org.orekit.models.earth.GeoMagneticField##getIGRF()}
     * @testedMethod {@link org.orekit.models.earth.GeoMagneticField##getWMM()}
     * 
     * @description Test Orekit exception when it doesn't find the resources model file
     * 
     * @input none
     * 
     * @output none
     * 
     * @throws PatriusException
     * 
     * @testPassCriteria expected Orekit exception
     * 
     * @see none
     * 
     * @comments none
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = PatriusException.class)
    public final void testOrekitResourceException() throws PatriusException {
        Utils.setDataRoot("empty-data");
        GeoMagneticFieldFactory.getField(GeoMagneticFieldFactory.FieldModel.IGRF, 2010.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GEOMAGNETIC}
     * 
     * @testedMethod {@link org.orekit.models.earth.GeoMagneticField##getModelName()}
     * @testedMethod {@link org.orekit.models.earth.GeoMagneticField##getField(final FieldModel type, final double
     *               year)}
     * 
     * @description Test each model to north and south pole. But we don't have any reference value because external tool
     *              return Nan to the pole.
     *              So we compare to reference value from external tool at latitude 89.999 deg and -89.999 deg to Orekit
     *              result at latitude 90 deg and -90 deg.
     * 
     * 
     * @input External tool output at 89.999 and -89.999 deg of latitude
     * 
     * @output Print the min and max relative difference on each coordinate of the geomagnetic field
     * 
     * @testPassCriteria Difference on each coordinate less than 1 nT.
     * 
     * @see http://www.ngdc.noaa.gov/geomag/models.shtml
     * 
     * @comments none
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testGetField() throws PatriusException, IOException {

        Utils.setDataRoot("geomagnetic-validation-data");
        // Create a list of element to the pole for the each model

        GeoMagneticElements geoMagElement;
        final List<GeoMagneticElements> geoMagList = new ArrayList<GeoMagneticElements>();

        // Get geomagnetic field on 1st January 2010 for each model

        final GeoMagneticField IGRFField =
            GeoMagneticFieldFactory.getField(GeoMagneticFieldFactory.FieldModel.IGRF, 2010.0);
        Assert.assertEquals("IGRF2010", IGRFField.getModelName());

        final GeoMagneticField WMMField =
            GeoMagneticFieldFactory.getField(GeoMagneticFieldFactory.FieldModel.WMM, 2010.0);
        Assert.assertEquals("WMM2010", WMMField.getModelName());

        // Get IGRF geomagnetic field to the pole

        geoMagList.add(IGRFField.calculateField(90.0, 0.0, 0.0));
        geoMagList.add(IGRFField.calculateField(-90.0, 0.0, 0.0));

        // Get WMM geomagnetic field to the pole

        geoMagList.add(WMMField.calculateField(90.0, 0.0, 0.0));
        geoMagList.add(WMMField.calculateField(-90.0, 0.0, 0.0));

        // Get output reference list (x,y,z,i,d,f,h) from the reference file
        final List<GeoMagRefOutput> geoMagRefList = ReferenceReader.readOutputExternalReference(
            "geomagnetic-validation-data", "/geomagnetic-validation-data/reference_data_pole_IGRF.txt");
        geoMagRefList.addAll(ReferenceReader.readOutputExternalReference("geomagnetic-validation-data",
            "/geomagnetic-validation-data/reference_data_pole_WMM.txt"));

        // To track delta on each coordinate
        double delta_X, delta_Y, delta_Z;
        double minDelta_X = Double.MAX_VALUE;
        double maxDelta_X = Double.MIN_VALUE;
        double minDelta_Y = Double.MAX_VALUE;
        double maxDelta_Y = Double.MIN_VALUE;
        double minDelta_Z = Double.MAX_VALUE;
        double maxDelta_Z = Double.MIN_VALUE;

        // For each input, compute the Patrius geomagnetic element and compare only the Vector3D
        int i = 0;
        for (final GeoMagRefOutput curentRef : geoMagRefList) {

            // Get the computed element
            geoMagElement = geoMagList.get(i);

            // Compare each vector's coordinate
            delta_X = MathLib.abs((curentRef.getB().getX() - geoMagElement.getFieldVector().getX())
                / curentRef.getB().getX());
            delta_Y = MathLib.abs((curentRef.getB().getY() - geoMagElement.getFieldVector().getY())
                / curentRef.getB().getY());
            delta_Z = MathLib.abs((curentRef.getB().getZ() - geoMagElement.getFieldVector().getZ())
                / curentRef.getB().getZ());

            minDelta_X = (delta_X < minDelta_X) ? delta_X : minDelta_X;
            minDelta_Y = (delta_Y < minDelta_Y) ? delta_Y : minDelta_Y;
            minDelta_Z = (delta_Z < minDelta_Z) ? delta_Z : minDelta_Z;

            maxDelta_X = (delta_X > maxDelta_X) ? delta_X : maxDelta_X;
            maxDelta_Y = (delta_Y > maxDelta_Y) ? delta_Y : maxDelta_Y;
            maxDelta_Z = (delta_Z > maxDelta_Z) ? delta_Z : maxDelta_Z;

            Assert.assertEquals(curentRef.getB().getX(), geoMagElement.getFieldVector().getX(), EPSILON_NT);
            Assert.assertEquals(curentRef.getB().getY(), geoMagElement.getFieldVector().getY(), EPSILON_NT);
            Assert.assertEquals(curentRef.getB().getZ(), geoMagElement.getFieldVector().getZ(), EPSILON_NT);

            i++;
        }

        System.out.println("Composante   |   Ecart Relatif MIN          |   Ecart Relatif MAX  ");
        System.out.println("   X         |       " + minDelta_X + "     |      " + maxDelta_X);
        System.out.println("   Y         |       " + minDelta_Y + "    |      " + maxDelta_Y);
        System.out.println("   Z         |       " + minDelta_Z + "    |      " + maxDelta_Z);

    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#GEOMAGNETIC}
     * 
     * @testedMethod {@link org.orekit.models.earth.GeoMagneticField##getField()}
     * 
     * @description Test Orekit exception when the geomagnetic model doesn't exist for this date
     * 
     * @input none
     * 
     * @output none
     * 
     * @throws PatriusException
     * 
     * @testPassCriteria expected Orekit Exception
     * 
     * @see none
     * 
     * @comments none
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = PatriusException.class)
    public final void testUnavalaibleModelException() throws PatriusException {
        GeoMagneticFieldFactory.getField(GeoMagneticFieldFactory.FieldModel.WMM, 2009.0);
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#GEOMAGNETIC}
     * 
     * @testedMethod {@link org.orekit.models.earth.GeoMagneticField##getField()}
     * 
     * @description Test if the model doesn't support time transform
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria The field on June 2001 is created from the DGRF2000 model.
     * 
     * @throws PatriusException
     *         : should not happen
     * 
     * @see none
     * 
     * @comments none
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Ignore
    @Test
    public final void testTimeTransform() throws PatriusException {
        final GeoMagneticField GeoMagField = GeoMagneticFieldFactory
            .getField(GeoMagneticFieldFactory.FieldModel.IGRF, 2001.5);
        Assert.assertEquals("DGRF2000", GeoMagField.getModelName());
    }

}
