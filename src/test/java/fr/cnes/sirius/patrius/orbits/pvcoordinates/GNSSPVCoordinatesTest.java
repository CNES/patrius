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
 * @history creation 10/11/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:10/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class validates GNSSPVCoordinates, AlmanacGNSSParameters
 * </p>
 *
 * @author fteilhard
 */
public class GNSSPVCoordinatesTest {

    /**
     * Comparison epsilon
     */
    private static final double EPSILON_POS = 1E-10;
    private static final double EPSILON_VEL = 7E-5;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GNSSPVCoordinatesTest.class.getSimpleName(), "GNSS PV coordinates");
    }

    /**
     * @throws PatriusException
     *         should not happen
     * @testType UT
     *
     * @testedMethod Frame conversion
     *
     * @description Get a PVCoordinates from almanac without frame transformation. Get the same PVCoordinates in
     *              EME2000.
     *              Check if the first PVCoordinates convert in EME2000 is equal to the second PVCoordinates.
     *
     *
     * @testPassCriteria The first PVCoordinates transformed in EME2000 is equal to the second one
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testFrameConvert() throws PatriusException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate currentDate = new AbsoluteDate(2015, 9, 27, 17, 4, 00, TimeScalesFactory.getGPS());

        final Transform wgs84ToOutputFrame = FramesFactory.getITRF().getTransformTo(FramesFactory.getEME2000(),
            currentDate);

        final AbsoluteDate weekDate = new AbsoluteDate("2015-09-27T00:00:19.000", TimeScalesFactory.getTAI());
        final AlmanacGNSSParameters almanac = new AlmanacGNSSParameters(GNSSType.GPS, 61440.0, 2.217106699,
                0.4714012146E-2, 5153.604004, 2.102607542, 0.9627610967, 0.480977878, -0.8183198006E-8,
                0., 0.);
        final GNSSPVCoordinates almanacPV = new GNSSPVCoordinates(almanac, weekDate);

        final PVCoordinates pvCoordWGS84 = almanacPV.getPVCoordinates(currentDate, FramesFactory.getITRF());

        final PVCoordinates pvCoordJ2000Expected = wgs84ToOutputFrame.transformPVCoordinates(pvCoordWGS84);

        final PVCoordinates pvCoordJ2000Actual = almanacPV.getPVCoordinates(currentDate, FramesFactory.getEME2000());

        // Expected relative difference between expected and actual position lower than EPSILON_POS
        final double deltaPos = pvCoordJ2000Actual.getPosition().subtract(pvCoordJ2000Expected.getPosition())
                .getNorm()
                / pvCoordJ2000Actual.getPosition().getNorm();
        Assert.assertEquals(0, deltaPos, EPSILON_POS);
        // Expected relative difference between expected and actual velocity lower than EPSILON_VEL
        final double deltaVel = pvCoordJ2000Actual.getVelocity().subtract(pvCoordJ2000Expected.getVelocity())
                .getNorm()
                / pvCoordJ2000Actual.getVelocity().getNorm();
        Assert.assertEquals(0, deltaVel, EPSILON_VEL);
    }

    /**
     * @description Evaluate the ephemeris serialization / deserialization process.
     *
     * @testPassCriteria The ephemeris can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate weekDate = new AbsoluteDate("2015-09-27T00:00:19.000", TimeScalesFactory.getTAI());
        final AlmanacGNSSParameters almanac = new AlmanacGNSSParameters(GNSSType.GPS, 61440.0, 2.217106699,
                0.4714012146E-2, 5153.604004, 2.102607542, 0.9627610967, 0.480977878, -0.8183198006E-8, 0., 0.);
        final GNSSPVCoordinates almanacPV = new GNSSPVCoordinates(almanac, weekDate);

        final GNSSPVCoordinates deserializedAlmanacPV = TestUtils.serializeAndRecover(almanacPV);

        final AbsoluteDate currentDate = new AbsoluteDate(2015, 9, 27, 17, 4, 00,
                TimeScalesFactory.getGPS());
        Assert.assertEquals(almanacPV.getPVCoordinates(currentDate, FramesFactory.getEME2000()),
                deserializedAlmanacPV.getPVCoordinates(currentDate, FramesFactory.getEME2000()));
    }

    /**
     * Check that the GNSSPVCoordinates throws an exception if the week date does not match the
     * reference epoch of the constellation.
     *
     * @throws PatriusException
     *         if the UTC datas can't be loaded
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkDate() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        // The date is one second after the correct weekDate
        final AbsoluteDate weekDateIncorrect = new AbsoluteDate("1999-09-04T23:59:48.000", TimeScalesFactory.getUTC());

        final GNSSParameters galileoAlmanacParams = new AlmanacGNSSParameters(GNSSType.Galileo, 0., 0., 0., 0., 0., 0.,
                0., 0., 0., 0.);

        // The GNSSPVCoordinates constructor is called to throw an exception with an incorrect date
        @SuppressWarnings("unused")
        final GNSSPVCoordinates galileoAlmanacPVCoordinates = new GNSSPVCoordinates(galileoAlmanacParams,
                weekDateIncorrect);
    }

    /**
     * Check that the native frame is ITRF
     */
    @Test
    public void testGetNativeFrame() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        final AbsoluteDate weekDate = new AbsoluteDate("1999-09-04T23:59:47.000", TimeScalesFactory.getUTC());
        final GNSSParameters galileoAlmanacParams = new AlmanacGNSSParameters(GNSSType.Galileo, 0., 0., 0., 0., 0., 0.,
                0., 0., 0., 0.);
        final GNSSPVCoordinates galileoAlmanacPVCoordinates = new GNSSPVCoordinates(galileoAlmanacParams, weekDate);

        Assert.assertEquals(FramesFactory.getITRF(), galileoAlmanacPVCoordinates.getNativeFrame(null));
    }
}
