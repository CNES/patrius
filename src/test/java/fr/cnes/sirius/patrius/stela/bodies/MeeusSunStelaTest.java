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
 * @history creation 19/02/2013
 *
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the coverage of the class MeeusSunStela.
 * 
 * @author Cedric Dental
 * 
 * @version
 * 
 * @since 1.3
 * 
 */
public class MeeusSunStelaTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Meeus Sun test class
         * 
         * @featureDescription perform simple tests in accordance with Stela ones
         * 
         * @coveredRequirements
         */
        MEEUS_SUN_STELA
    }

    /** Sun */
    private CelestialBody sun;
    /** Date */
    private AbsoluteDate date;

    /** Use STELA Meeus model. */
    private MODEL model;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MeeusSunStelaTest.class.getSimpleName(), "STELA Meeus Sun model");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
        FramesFactory.clear();
    }

    /**
     * setUp.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        this.model = MODEL.STELA;
        this.date = new AbsoluteDate(new DateComponents(2009, 07, 29),
            new TimeComponents(0, 0, 34.), TimeScalesFactory.getTAI());
        this.sun = new MeeusSun(this.model);
    }

    /**
     * Test update position.
     * 
     * @throws PatriusException
     */
    @Test
    public void testUpdatePosition() throws PatriusException {
        MeeusSun.resetTransform();
        final Vector3D position = this.sun.getPVCoordinates(this.date, FramesFactory.getMOD(false)).getPosition();
        final double normP = position.getNorm();
        final double xp = position.getX() / normP;
        final double yp = position.getY() / normP;
        position.getZ();

        Assert.assertEquals((xp + 0.588192245680033) / 0.588192245680033, 0, 2e-14);
        Assert.assertEquals((yp - 0.7419941326909959) / 0.7419941326909959, 0, 2e-14);

    }

    /**
     * Tests method getPVCoordinate with cached CIRF transformation
     * 
     * @throws PatriusException
     *         thrown if transformation computation failed
     * @testPassCriteria results close from Stela v2.6 (relative tolerance: 1E-12)
     * @since 3.0
     */
    @Test
    public void testUpdatePositionCIRF() throws PatriusException {

        Report.printMethodHeader("testUpdatePositionCIRF", "Position computation (CIRF frame)", "STELA 2.6", 5E-12,
            ComparisonType.RELATIVE);

        // Initialization
        MeeusSun.resetTransform();
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22370 * 86400 - 67.184 + 35);
        // Computation
        final Vector3D position = this.sun.getPVCoordinates(date, FramesFactory.getCIRF()).getPosition();
        final double normP = position.getNorm();
        final double xp = position.getX() / normP;
        final double yp = position.getY() / normP;
        final double zp = position.getZ() / normP;

        final double[] exp = { 0.9823088542838403, 0.17141194839936727, 0.0754139160997568 };
        // Check result
        Assert.assertEquals((exp[0] - xp) / exp[0], 0, 5E-12);
        Assert.assertEquals((exp[1] - yp) / exp[1], 0, 5E-12);
        Assert.assertEquals((exp[2] - zp) / exp[2], 0, 5E-12);

        Report.printToReport("Position", new Vector3D(exp), new Vector3D(xp, yp, zp));

        MeeusSun.resetTransform();
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_SUN_STELA}
     * 
     * @description check that IAU data have been initialised and body frame have been defined (through parent method)
     * 
     * @input None
     * 
     * @output transformation
     * 
     * @testPassCriteria returned transformation is not null.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testTransformation() throws PatriusException {
        Assert.assertNotNull(this.sun.getInertialFrame(IAUPoleModelType.CONSTANT).getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
        Assert.assertNotNull(this.sun.getRotatingFrame(IAUPoleModelType.TRUE).getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
    }

    /**
     * @throws PatriusException
     *         if position cannot be computed in given frame
     * @description Evaluate the Meeus Sun serialization / deserialization process.
     *
     * @testPassCriteria The Meeus Sun can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final Frame frame = FramesFactory.getGCRF();

        // Loop over each Meeus Sun models
        for (final MODEL model : MODEL.values()) {
            final MeeusSun meeusSun = new MeeusSun(model);
            final MeeusSun deserializedMeeusSun = TestUtils.serializeAndRecover(meeusSun);

            // Check the model through PVCoordinates computation on several dates
            for (int i = 0; i < 10; i++) {
                final AbsoluteDate currentDate = this.date.shiftedBy(i);
                Assert.assertEquals(meeusSun.getPVCoordinates(currentDate, frame),
                        deserializedMeeusSun.getPVCoordinates(currentDate, frame));
            }
        }
    }
}
