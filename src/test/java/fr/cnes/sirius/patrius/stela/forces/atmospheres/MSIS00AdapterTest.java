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
 * @history Created 05/08/2016
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1196:15/11/2017:add getPressure() method
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.atmospheres;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000Test.features;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link MSIS00Adapter} class for the MSIS2000 Atmosphere model
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.3
 * 
 */
public class MSIS00AdapterTest {

    /**
     * @testType UT
     * 
     * @testedMethod {@link MSIS00Adapter#getData(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description test computation of all available atmospheric data
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output {@link AtmosphereData}
     * 
     * @testPassCriteria sum of partial densities equals total density. Atmosphere data are as expected (reference:
     *                   scilab ms_msis2000 - threshold: 1E-15)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testAtmosphereData() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        final SolarActivityDataProvider reader = SolarActivityDataFactory.getSolarActivityDataProvider();
        final ClassicalMSISE2000SolarData msis2000Data = new ClassicalMSISE2000SolarData(reader);
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final MSIS00Adapter atmosModel = new MSIS00Adapter(msis2000Data, 6378136.46, 1 / 0.29825765000000E+03, sun);
        final MSIS00Adapter atm2 = (MSIS00Adapter) atmosModel.copy();
        final AbsoluteDate date = new AbsoluteDate(2011, 03, 31, 22, 16, 55.4778569, TimeScalesFactory.getUTC());
        final Vector3D pos = new Vector3D(new double[] { 8749870.287474481, -976409.8027621375, -1110696.1958653878 });

        // Computation
        final AtmosphereData data = atm2.getData(date, pos, FramesFactory.getGCRF());
        final double density = data.getDensity();
        final double localTemperature = data.getLocalTemperature();
        final double exosphericTemperature = data.getExosphericTemperature();
        final double densityHe = data.getDensityHe();
        final double densityO = data.getDensityO();
        final double densityN2 = data.getDensityN2();
        final double densityO2 = data.getDensityO2();
        final double densityAr = data.getDensityAr();
        final double densityH = data.getDensityH();
        final double densityN = data.getDensityN();
        final double densityAnomalousOxygen = data.getDensityAnomalousOxygen();
        final double meanAtomicMass = data.getMeanAtomicMass();

        // Check density = sum partial densities
        final double expected = densityHe + densityO + densityN2 + densityO2 + densityAr + densityH + densityN
                + densityAnomalousOxygen;
        Assert.assertEquals(0., (expected - density) / expected, 2E-16);

        // Non-regression
        Assert.assertEquals(7.317913213266922E-17, density, 0.);
        Assert.assertEquals(944.4459641980035, localTemperature, 0.);
        Assert.assertEquals(944.4459641980035, exosphericTemperature, 0.);
        Assert.assertEquals(0, (1.0429437373281993E-17 - densityHe) / densityHe, 1E-15);
        Assert.assertEquals(0, (1.282572481304563E-25 - densityO) / densityO, 1E-15);
        Assert.assertEquals(0, (3.204942951899128E-36 - densityN2) / densityN2, 1E-15);
        Assert.assertEquals(0, (5.489586729977693E-41 - densityO2) / densityO2, 3E-15);
        Assert.assertEquals(0, (7.818015931987586E-50 - densityAr) / densityAr, 1E-15);
        Assert.assertEquals(0, (6.271435915314873E-17 - densityH) / densityH, 1E-15);
        Assert.assertEquals(0, (1.2336045420789356E-25 - densityN) / densityN, 1E-15);
        Assert.assertEquals(0, (3.5335354620790417E-20 - densityAnomalousOxygen) / densityAnomalousOxygen, 1E-15);
        Assert.assertEquals(1.4348008105509886, meanAtomicMass, 1E-15);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getPressure(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description test computation of pressure
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output pressure
     * 
     * @testPassCriteria pressure is as expected (reference: Celestlab, threshold: 1E-6 due to the fact that Celestlab
     *                   uses simple precision)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testPressure() throws PatriusException {

        Utils.setDataRoot("regular-data");

        final FramesConfiguration configSvg = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Initialization
        final SolarActivityDataProvider provider = new ConstantSolarActivity(250, 25);
        final MSISE2000InputParameters msis2000Data = new ClassicalMSISE2000SolarData(provider);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1 / 0.29825765000000E+03,
            FramesFactory.getITRF());
        final CelestialBody sun = new MeeusSun();
        final MSIS00Adapter atmosModel = new MSIS00Adapter(msis2000Data, 6378136.46, 1 / 0.29825765000000E+03, sun);
        final AbsoluteDate date = new AbsoluteDate("2008-01-01T16:25:00", TimeScalesFactory.getUTC());
        final Vector3D pos = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), 0, 0, 600000, "").getPosition();
        // STELA GeodPosition assume reference frame is CIRF, then provide position in CIRF
        final Transform t = FramesFactory.getITRF().getTransformTo(FramesFactory.getCIRF(), date);

        // Computation
        final double actual = atmosModel.getPressure(date, t.transformPosition(pos), FramesFactory.getCIRF());
        // Perfect gas constant used in Celestlab is 8.314472
        final double expected = 1.478134189718436740E-06 * Constants.PERFECT_GAS_CONSTANT / 8.314472;

        // Check
        Assert.assertEquals(0, (expected - actual) / expected, 2E-6);

        FramesFactory.setConfiguration(configSvg);
    }
}
