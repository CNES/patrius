/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * VERSION:4.14:OPENFD-161:22/08/2024:[PATRIUS] Adaptation de l'interface CelestialBody
 * car l'orientation n'est pas forcement IAU
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.IAUPoleFunction.IAUTimeDependency;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceKernelManager;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.function.SineFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import junit.framework.Assert;

/**
 * Test class for {@link UserCelestialBodyLoader} class.
 */
public class UserCelestialBodyLoaderTest {

    /** Deg to rad. */
    private static final double DEG_TO_RAD = MathLib.PI / 180.;

    /**
     * @objective loads PHOBOS body/Mars barycenter object through UserCelestialBodyLoader
     *
     * @description loads PHOBOS body/Mars barycenter object through UserCelestialBodyLoader
     *
     * @passCriteria body built as expected
     * 
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void testFunctional() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        final BSPEphemerisLoader ephemerisLoader =
            new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Phobos
        final CelestialBodyEphemeris phobosEphemeris = ephemerisLoader.loadCelestialBodyEphemeris("PHOBOS");
        final UserCelestialBodyLoader loaderPhobos =
            new UserCelestialBodyLoader(phobosEphemeris, 123, null, FramesFactory.getGCRF(), null);
        CelestialBodyFactory.addCelestialBodyLoader("PHOBOS", loaderPhobos);

        // Retrieve body
        final CelestialBody phobos = CelestialBodyFactory.getBody("PHOBOS");
        Assert.assertEquals(123, phobos.getGM(), 0.);
        Assert.assertEquals("Jupiter", loaderPhobos.getName("Jupiter"));

        // Mars barycenter
        final CelestialBodyEphemeris marsBarycenterEphemeris =
            ephemerisLoader.loadCelestialBodyEphemeris("MARS BARYCENTER");
        final UserCelestialBodyLoader loaderMarsBarycenter =
            new UserCelestialBodyLoader(marsBarycenterEphemeris, 1234, null, FramesFactory.getGCRF(), null);
        CelestialBodyFactory.addCelestialBodyLoader("MARS BARYCENTER", loaderMarsBarycenter);

        // Retrieve body
        final CelestialPoint marsBarycenter = CelestialBodyFactory.getPoint("MARS BARYCENTER");
        Assert.assertEquals(1234, marsBarycenter.getGM(), 0.);

        final CelestialPoint phobos2 = CelestialBodyFactory.getPoint("PHOBOS");
        Assert.assertEquals(123, phobos2.getGM(), 0.);
    }

    /**
     * @objective Test UserCelestialBodyLoader to create a UserIAUCelestialBody
     *
     * @description Check that when celestialBodyOrientation input is CelestialBodyIAUOrientation
     *              it is possible to get the inertial and rotating frames
     *
     * @passCriteria the inertial and rotating frames are not null
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void testUserIAUCelestialBody() throws PatriusException, URISyntaxException, IOException {

        // Initialization
        Utils.setDataRoot("spk_ephem_data");
        final String file = new File(ClassLoader
            .getSystemResource(
                "spk_ephem_data" + File.separator + "mer1_ls_040128_iau2000_v1.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);
        // build a MERCURY celestialBodyIauOrientation

        final List<UnivariateDifferentiableFunction> alpha0fDays = new ArrayList<>();
        alpha0fDays.add(new PolynomialFunction(new double[] { 281.0097 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> alpha0fCenturies = new ArrayList<>();
        alpha0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0328 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(alpha0fDays, alpha0fCenturies);
        final List<UnivariateDifferentiableFunction> delta0fDays = new ArrayList<>();
        delta0fDays.add(new PolynomialFunction(new double[] { 61.4143 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0049 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(delta0fDays, delta0fCenturies);
        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 329.5469 * DEG_TO_RAD, 6.1385025 * DEG_TO_RAD }));
        wfDays.add(new SineFunction(0.00993822 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 174.791086 * DEG_TO_RAD, 4.092335 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00104581 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 349.582171 * DEG_TO_RAD, 8.184670 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00010280 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 164.373257 * DEG_TO_RAD, 12.277005 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00002364 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 339.164343 * DEG_TO_RAD, 16.369340 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00000532 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 153.955429 * DEG_TO_RAD, 20.461675 * DEG_TO_RAD })));
        final List<UnivariateDifferentiableFunction> wfCenturies = new ArrayList<>();
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, wfCenturies);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation userMercuryOrientation = new UserIAUPole(coefficients);

        // get the built-in MERCURY model from celestialbodyfactory
        final CelestialBody mercuryBuiltin = CelestialBodyFactory.getMercury();

        // test the UserCelestialBodyLoader with each attribute of the builtin mercury body and IAUOrientation

        final UserCelestialBodyLoader userMercuryLoader = new UserCelestialBodyLoader(
            mercuryBuiltin.getEphemeris(),
            mercuryBuiltin.getGM(),
            userMercuryOrientation,
            FramesFactory.getICRF(),
            mercuryBuiltin.getShape());

        CelestialBodyFactory.addCelestialBodyLoader("MERCURY_USER_DEFINED", userMercuryLoader);

        // get the usercelestialbody just created
        final CelestialBody mercuryTest = CelestialBodyFactory.getBody("MERCURY_USER_DEFINED");

        // Check
        Assert.assertNotNull(mercuryTest.getInertialFrame());
        Assert.assertNotNull(mercuryTest.getRotatingFrame());

    }

    /**
     * Convert of list of IAU functions into a map if IAU functions.
     * 
     * @param functionInDays list of functions in days
     * @param functionInCenturies list of functions in centuries
     * @return IAUPole coefficient list
     */
    private static
        IAUPoleCoefficients1D
            buildCoefficientsList(final List<UnivariateDifferentiableFunction> functionInDays,
                                  final List<UnivariateDifferentiableFunction> functionInCenturies) {
        List<IAUPoleFunction> functions = new ArrayList<>();
        if (functionInDays != null) {
            for (final UnivariateDifferentiableFunction f : functionInDays) {
                functions.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, f, IAUTimeDependency.DAYS));
            }
        }
        if (functionInCenturies != null) {
            for (final UnivariateDifferentiableFunction f : functionInCenturies) {
                functions.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, f, IAUTimeDependency.CENTURIES));
            }
        }
        if (functionInDays == null && functionInCenturies == null) {
            functions = null;
        }
        return new IAUPoleCoefficients1D(functions);
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
