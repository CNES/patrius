/**
 * 
 * Copyright 2011-2017 CNES
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.groundstation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the RFStationAntenna class.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFStationAntennaTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle RF ground station antenna model
         * 
         * @featureDescription RF ground station antenna model validation
         * 
         * @coveredRequirements DV-VEHICULE_330
         */
        RF_STATION_ANTENNA_MODEL
    }

    /**
     * The ground station antenna model.
     */
    static RFStationAntenna station;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RF_STATION_ANTENNA_MODEL}
     * 
     * @testedMethod {@link RFStationAntenna#getMeritFactor()}
     * @testedMethod {@link RFStationAntenna#getGroundLoss()}
     * @testedMethod {@link RFStationAntenna#getEllipticityFactor()}
     * @testedMethod {@link RFStationAntenna#getCombinerLoss()}
     * 
     * @description test the getters of the RF station antenna model.
     * 
     * @input the RF ground station antenna model
     * 
     * @output the values
     * 
     * @testPassCriteria the values must be the expected ones.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testStationAntennaModelGetters() {
        // tests the getters:
        Assert.assertEquals(29.7, station.getMeritFactor(), 0.0);
        Assert.assertEquals(2.0, station.getGroundLoss(), 0.0);
        Assert.assertEquals(2.0, station.getEllipticityFactor(), 0.0);
        Assert.assertEquals(0., station.getCombinerLoss(), 0.0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RF_STATION_ANTENNA_MODEL}
     * 
     * @testedMethod {@link RFStationAntenna#getAtmosphericLoss(double)}
     * @testedMethod {@link RFStationAntenna#getPointingLoss(double)}
     * 
     * @description test the interpolation of the antenna atmospheric and pointing losses
     *              using simple test cases
     * 
     * @input the RF ground station antenna model
     * 
     * @output the values
     * 
     * @testPassCriteria the interpolated values must be the expected ones.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testStationAntennaLossesInterpolation() throws PatriusException {
        // tests the exception when the input elevation is not between -PI/2 and PI/2:
        boolean rez = false;
        try {
            station.getAtmosphericLoss(3.0);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // tests the interpolation when there is only one available value:
        Assert.assertEquals(2.0, station.getAtmosphericLoss(-0.5), 0.0);
        Assert.assertEquals(2.0, station.getAtmosphericLoss(1.06548), 0.0);
        // tests the interpolation when there are several available values:
        Assert.assertEquals(0.0, station.getPointingLoss(-0.6), 0.0);
        Assert.assertEquals(1.0, station.getPointingLoss(0.1), 0.0);
        Assert.assertEquals(5.0, station.getPointingLoss(0.5), 0.0);
        Assert.assertEquals(14.0, station.getPointingLoss(1.5), 0.0);
        // tests the interpolation with only two values; we have to re-create the station model:
        final double[][] atmLoss = new double[][] { { 0., 2.0 } };
        final double[][] pointLoss = new double[][] { { -0.5, 1.0 }, { 0.2, 8.0 } };
        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0., FramesFactory.getITRF());
        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "station");
        station = new RFStationAntenna(topoFrame, 29.7, 2.0, 2.0, atmLoss, pointLoss, 0.);
        Assert.assertEquals(1.0, station.getPointingLoss(-0.5), 0.0);
        Assert.assertEquals(1.0, station.getPointingLoss(-0.7), 0.0);
        Assert.assertEquals(7.0, station.getPointingLoss(0.1), 0.0);
        Assert.assertEquals(8.0, station.getPointingLoss(0.5), 0.0);
    }

    /**
     * Setup for all unit tests in the class.
     * It provides an RF antenna property.
     * 
     * @throws PatriusException
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        final double[][] atmLoss = new double[][] { { 0., 2.0 } };
        final double[][] pointLoss = new double[][] { { 0., 0.0 }, { 0.2, 2.0 }, { 0.6, 6.0 }, { 1.2, 12.0 },
            { 1.4, 14. } };

        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0., FramesFactory.getITRF());
        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "zero");
        station = new RFStationAntenna(topoFrame, 29.7, 2.0, 2.0, atmLoss, pointLoss, 0.);
    }
}