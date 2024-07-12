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
 * @history Created 04/07/2014
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:281:04/07/2014:add factory class for earth gravitational model.
 * VERSION::DM:700:13/03/2017:Add model name
 * VERSION::DM:1174:26/06/2017:allow incomplete coefficients files
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Validation class for {@link EarthGravitationalModelFactory}
 * </p>
 * 
 * @see EarthGravitationalModelFactory
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id: EarthGravitationalModelFactoryTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 * 
 */
public class EarthGravitationalModelFactoryTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle earth gravitational model factory
         * 
         * @featureDescription test the earth gravitational model factory methods
         * 
         * @coveredRequirements FT 281
         */
        EARTH_GRAVITATIONAL_MODEL_FACTORY
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_GRAVITATIONAL_MODEL_FACTORY}
     * 
     * @testedMethod {@link EarthGravitationalModelFactory#getGravitationalModel(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * 
     * @description Test for Balmino model using getGravitationalModel method and using GRGS gravity field files
     * 
     * @input GravityFieldNames potentialFileName = GravityFieldNames.SHM : the regular expression for gravity field
     *        file name
     * @input int n = 5 : the degree
     * @input int n = 5 : the order
     * 
     * @output an instance of Balmino model
     * 
     * @testPassCriteria An instance of Balmino model is correctly created.
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGetGravitationalModel() throws IOException, ParseException, PatriusException {

        // sets a default data root
        Utils.setDataRoot("potentialFactory/grgs-format");
        this.potentialFileName = GravityFieldNames.GRGS;

        final GravityModel model = EarthGravitationalModelFactory.getGravitationalModel(
            this.potentialFileName, "grim5_C1.dat", this.n, this.m, false);
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getClass(), BalminoGravityModel.class);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_GRAVITATIONAL_MODEL_FACTORY}
     * 
     * @testedMethod {@link EarthGravitationalModelFactory#getBalmino(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * 
     * @description Test for Balmino model using ICGEM gravity field files
     * 
     * @input GravityFieldNames potentialFileName = GravityFieldNames.SHM : the regular expression for gravity field
     *        file name
     * @input int n = 5 : the degree
     * @input int n = 5 : the order
     * 
     * @output an instance of Balmino model
     * 
     * @testPassCriteria An instance of Balmino model is correctly created.
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGetBalmino() throws IOException, ParseException, PatriusException {
        // sets a default data root
        Utils.setDataRoot("potentialFactory/icgem-format");
        this.potentialFileName = GravityFieldNames.ICGEM;

        final GravityModel model = EarthGravitationalModelFactory.getGravitationalModel(
            this.potentialFileName, "g007_eigen_05c_coef", this.n, this.m, false);
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getClass(), BalminoGravityModel.class);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_GRAVITATIONAL_MODEL_FACTORY}
     * 
     * @testedMethod {@link EarthGravitationalModelFactory#getCunningham(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * 
     * @description Test for Cunningham model using EGM gravity field files
     * 
     * @input GravityFieldNames potentialFileName = GravityFieldNames.SHM : the regular expression for gravity field
     *        file name
     * @input int n = 5 : the degree
     * @input int n = 5 : the order
     * 
     * @output an instance of Cunningham model
     * 
     * @testPassCriteria An instance of Cunningham model is correctly created.
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGetCunningham() throws IOException, ParseException, PatriusException {
        // sets a default data root
        Utils.setDataRoot("potentialFactory/egm-format");
        this.potentialFileName = GravityFieldNames.EGM;

        final GravityModel model =
            EarthGravitationalModelFactory.getCunningham(this.potentialFileName,
                "egm96_to5.ascii", this.n, this.m, false);
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getClass(), CunninghamGravityModel.class);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_GRAVITATIONAL_MODEL_FACTORY}
     * 
     * @testedMethod {@link EarthGravitationalModelFactory#getDroziner(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * 
     * @description Test for Droziner model using SHM gravity field files
     * 
     * @input GravityFieldNames potentialFileName = GravityFieldNames.SHM : the regular expression for gravity field
     *        file name
     * @input int n = 5 : the degree
     * @input int n = 5 : the order
     * 
     * @output an instance of Droziner model
     * 
     * @testPassCriteria An instance of Droziner model is correctly created.
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGetDroziner() throws IOException, ParseException, PatriusException {
        // sets a default data root
        Utils.setDataRoot("potentialCNES/shm-format");
        this.potentialFileName = GravityFieldNames.SHM;

        final GravityModel model =
            EarthGravitationalModelFactory.getDroziner(this.potentialFileName,
                "eigen_cg03c_coef", this.n, this.m, false);
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getClass(), DrozinerGravityModel.class);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_GRAVITATIONAL_MODEL_FACTORY}
     * 
     * @testedMethod {@link EarthGravitationalModelFactory#getBalmino(GravityFieldNames, String, int, int)}
     * @testedMethod {@link EarthGravitationalModelFactory#getCunningham(GravityFieldNames, String, int, int)}
     * @testedMethod {@link EarthGravitationalModelFactory#getDroziner(GravityFieldNames, String, int, int)}
     * @testedMethod {@link EarthGravitationalModelFactory#getGravitationalModel(GravityFieldNames, String, int, int)}
     * 
     * @description test potential file name properly taken into account for each method in
     *              EarthGravitationalModelFactory
     * 
     * @input potential file name for each method in EarthGravitationalModelFactory
     * 
     * @output Earth gravitational model
     * 
     * @testPassCriteria right model properly loaded (MU parameter and force in case of EGM data are as expected)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testPotentialName() throws IOException, ParseException, PatriusException {
        // Initialization
        Utils.setDataRoot("potentialCNES/potentials");

        // Test all potential models and file format
        final BalminoGravityModel balmino1 = (BalminoGravityModel) EarthGravitationalModelFactory.getBalmino(
            GravityFieldNames.EGM, "egm1", 5, 5, false);
        final BalminoGravityModel balmino2 = (BalminoGravityModel) EarthGravitationalModelFactory.getBalmino(
            GravityFieldNames.EGM, "egm2", 5, 5, false);
        final CunninghamGravityModel cunningham1 = (CunninghamGravityModel) EarthGravitationalModelFactory
            .getCunningham(GravityFieldNames.GRGS, "grgs1.dat", 5, 5, false);
        final CunninghamGravityModel cunningham2 = (CunninghamGravityModel) EarthGravitationalModelFactory
            .getCunningham(GravityFieldNames.GRGS, "grgs2.dat", 5, 5, false);
        final DrozinerGravityModel droziner1 = (DrozinerGravityModel) EarthGravitationalModelFactory.getDroziner(
            GravityFieldNames.ICGEM, "icgem1", 5, 5, false);
        final DrozinerGravityModel droziner2 = (DrozinerGravityModel) EarthGravitationalModelFactory.getDroziner(
            GravityFieldNames.ICGEM, "icgem2", 5, 5, false);
        final BalminoGravityModel gravitationalModel1 = (BalminoGravityModel) EarthGravitationalModelFactory
            .getGravitationalModel(GravityFieldNames.SHM, "shm1", 5, 5, false);
        final BalminoGravityModel gravitationalModel2 = (BalminoGravityModel) EarthGravitationalModelFactory
            .getGravitationalModel(GravityFieldNames.SHM, "shm2", 5, 5, false);

        // Check that values (mu) are the right ones meaning the model has been built with the chosen file
        // In the case of EGM data, MU parameter is not file-dependent: since read coefficients cannot be checked, only
        // module of resulting force is roughly checked ensuring the two files have been properly taken into account
        final PVCoordinates pv = new PVCoordinates(new Vector3D(7000000, 0, 0), new Vector3D(0, 7000, 0));
        Assert
            .assertTrue(balmino1.computeNonCentralTermsAcceleration(pv.getPosition(), null)
                .getNorm() > 1.
                    && balmino1.computeNonCentralTermsAcceleration(pv.getPosition(), null)
                        .getNorm() < 100.);
        Assert.assertTrue(balmino2.computeNonCentralTermsAcceleration(pv.getPosition(), null)
            .getNorm() > 1000.);
        Assert.assertEquals(0.39860044150000E+14, cunningham1.getMu(), 0.);
        Assert.assertEquals(0.39860044150000E+16, cunningham2.getMu(), 0.);
        Assert.assertEquals(0.3986004415E+17, droziner1.getMu(), 0.);
        Assert.assertEquals(0.3986004415E+18, droziner2.getMu(), 0.);
        Assert.assertEquals(0.3986004415E+19, gravitationalModel1.getMu(), 0.);
        Assert.assertEquals(0.3986004415E+20, gravitationalModel2.getMu(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_GRAVITATIONAL_MODEL_FACTORY}
     * 
     * @testedMethod {@link EarthGravitationalModelFactory#getGravitationalModel(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * @testedMethod {@link EarthGravitationalModelFactory#getDroziner(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * @testedMethod {@link EarthGravitationalModelFactory#getCunningham(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * @testedMethod {@link EarthGravitationalModelFactory#getBalmino(fr.cnes.sirius.patrius.forces.gravity.EarthGravitationalModelFactory.GravityFieldNames, int, int)}
     * 
     * @description Check that a model with missing coefficients is built if missing coefficients are allowed
     * 
     * @input gravity field data wit missing coefficient
     * 
     * @output built models
     * 
     * @testPassCriteria no exception is thrown
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testMissingCoefficients() throws IOException, ParseException {
        Utils.setDataRoot("potentialFactory/missingCoef");
        try {
            Assert.assertNotNull(EarthGravitationalModelFactory.getGravitationalModel(GravityFieldNames.GRGS,
                "grim4s4_gr", 5, 5, true));
            Assert.assertNotNull(EarthGravitationalModelFactory.getDroziner(GravityFieldNames.GRGS, "grim4s4_gr", 5, 5,
                true));
            Assert.assertNotNull(EarthGravitationalModelFactory.getCunningham(GravityFieldNames.GRGS, "grim4s4_gr", 5,
                5, true));
            Assert.assertNotNull(EarthGravitationalModelFactory.getBalmino(GravityFieldNames.GRGS, "grim4s4_gr", 5, 5,
                true));
        } catch (final PatriusException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Before
    public void setUp() {
        this.n = 5;
        this.m = 5;
    }

    private GravityFieldNames potentialFileName;
    private int n;
    private int m;
}
