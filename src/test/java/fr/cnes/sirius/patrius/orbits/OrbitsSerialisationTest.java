/**
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.AlternateEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisRadiusParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CircularParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquatorialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PVCoordinatesTest;

/**
 * Test
 */
public class OrbitsSerialisationTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @Test
    public void alternEquiSerialTest() {
        // Initialization
        final double a = 10000E3;
        final double n = MathLib.sqrt(this.mu / a) / a;
        final AlternateEquinoctialParameters param = new AlternateEquinoctialParameters(n, 0.1,
                0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE, this.mu);
        final AlternateEquinoctialOrbit orbit = new AlternateEquinoctialOrbit(param,
                FramesFactory.getEME2000(), this.date);

        // serialisation
        final AlternateEquinoctialOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsAlternEquiOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsAlternEquiOrbit(final AlternateEquinoctialOrbit orbit1,
            final AlternateEquinoctialOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final AlternateEquinoctialParameters param1 = orbit1.getAlternateEquinoctialParameters();
        final AlternateEquinoctialParameters param2 = orbit2.getAlternateEquinoctialParameters();
        KeplerianParametersTest.assertEqualsKeplerianParameters(param1.getKeplerianParameters(),
                param2.getKeplerianParameters());
    }

    @Test
    public void apsisSeriaTest() {
        // Initialization
        final ApsisRadiusParameters param = new ApsisRadiusParameters(10000E3, 20000E3, 0.2, 0.3, 0.4, 0.5,
            PositionAngle.TRUE, this.mu);
        final ApsisOrbit orbit = new ApsisOrbit(param, FramesFactory.getEME2000(), this.date);

        // serialisation
        final ApsisOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsApsisOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsApsisOrbit(final ApsisOrbit orbit1, final ApsisOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final ApsisRadiusParameters param1 = orbit1.getApsisParameters();
        final ApsisRadiusParameters param2 = orbit2.getApsisParameters();
        KeplerianParametersTest.assertEqualsKeplerianParameters(param1.getKeplerianParameters(),
            param2.getKeplerianParameters());
        Assert.assertEquals(param1.getPeriapsis(), param2.getPeriapsis(), 0);
        Assert.assertEquals(param1.getApoapsis(), param2.getApoapsis(), 0);
    }

    @Test
    public void carteSeriaTest() {
        // Initialization
        final CartesianParameters param = new CartesianParameters(new Vector3D(10000E3, 20000E3, 30000E3),
            new Vector3D(10E3, 2E3, 3E3), new Vector3D(5, 6, 7), this.mu);
        final CartesianOrbit orbit = new CartesianOrbit(param, FramesFactory.getEME2000(), this.date);

        // serialisation
        final CartesianOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsCarteOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsCarteOrbit(final CartesianOrbit orbit1,
            final CartesianOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final CartesianParameters param1 = orbit1.getCartesianParameters();
        final CartesianParameters param2 = orbit2.getCartesianParameters();
        PVCoordinatesTest.assertEqualsPVCoordinates(param1.getPVCoordinates(), param2.getPVCoordinates());
        Assert.assertEquals(param1.getMu(), param2.getMu(), 0);
    }

    @Test
    public void circuSeriaTest() {
        // Initialization
        final CircularParameters param = new CircularParameters(10000E3, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            this.mu);
        final CircularOrbit orbit = new CircularOrbit(param, FramesFactory.getEME2000(), this.date);

        // serialisation
        final CircularOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsCircuOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsCircuOrbit(final CircularOrbit orbit1,
            final CircularOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final CircularParameters param1 = orbit1.getCircularParameters();
        final CircularParameters param2 = orbit2.getCircularParameters();
        Assert.assertEquals(param1.getA(), param2.getA(), 0);
        Assert.assertEquals(param1.getCircularEx(), param2.getCircularEx(), 0);
        Assert.assertEquals(param1.getCircularEy(), param2.getCircularEy(), 0);
        Assert.assertEquals(param1.getI(), param2.getI(), 0);
        Assert.assertEquals(param1.getRightAscensionOfAscendingNode(), param2.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(param1.getMu(), param2.getMu(), 0);
    }

    @Test
    public void equaSeriaTest() {
        // Initialization
        final EquatorialParameters param = new EquatorialParameters(10000E3, 0.1, 0.2, 0.3, 0.4, 0.5,
            PositionAngle.TRUE, this.mu);
        final EquatorialOrbit orbit = new EquatorialOrbit(param, FramesFactory.getEME2000(), this.date);

        // serialisation
        final EquatorialOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsEquaOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsEquaOrbit(final EquatorialOrbit orbit1,
            final EquatorialOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final EquatorialParameters param1 = orbit1.getEquatorialParameters();
        final EquatorialParameters param2 = orbit2.getEquatorialParameters();
        Assert.assertEquals(param1.getA(), param2.getA(), 0);
        Assert.assertEquals(param1.getE(), param2.getE(), 0);
        Assert.assertEquals(param1.getIx(), param2.getIx(), 0);
        Assert.assertEquals(param1.getIy(), param2.getIy(), 0);
        Assert.assertEquals(param1.getPomega(), param2.getPomega(), 0);
        Assert.assertEquals(param1.getTrueAnomaly(), param2.getTrueAnomaly(), 0);
        Assert.assertEquals(param1.getMu(), param2.getMu(), 0);

    }

    @Test
    public void equiSeriaTest() {
        // Initialization
        final EquinoctialParameters param = new EquinoctialParameters(10000E3, 0.1, 0.2, 0.3, 0.4, 0.5,
            PositionAngle.TRUE, this.mu);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(param, FramesFactory.getEME2000(), this.date);

        // serialisation
        final EquinoctialOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsEquiOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsEquiOrbit(final EquinoctialOrbit orbit1,
            final EquinoctialOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final EquinoctialParameters param1 = orbit1.getEquinoctialParameters();
        final EquinoctialParameters param2 = orbit2.getEquinoctialParameters();
        Assert.assertEquals(param1.getA(), param2.getA(), 0);
        Assert.assertEquals(param1.getEquinoctialEx(), param2.getEquinoctialEx(), 0);
        Assert.assertEquals(param1.getEquinoctialEy(), param2.getEquinoctialEy(), 0);
        Assert.assertEquals(param1.getHx(), param2.getHx(), 0);
        Assert.assertEquals(param1.getHy(), param2.getHy(), 0);
        Assert.assertEquals(param1.getLv(), param2.getLv(), 0);
        Assert.assertEquals(param1.getMu(), param2.getMu(), 0);
    }

    @Test
    public void kepleSeriaTest() {
        // Initialization
        final KeplerianParameters param = new KeplerianParameters(10000E3, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            this.mu);
        final KeplerianOrbit orbit = new KeplerianOrbit(param, FramesFactory.getEME2000(), this.date);

        // serialisation
        final KeplerianOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsKepleOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsKepleOrbit(final KeplerianOrbit orbit1,
            final KeplerianOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final KeplerianParameters param1 = orbit1.getKeplerianParameters();
        final KeplerianParameters param2 = orbit2.getKeplerianParameters();
        KeplerianParametersTest.assertEqualsKeplerianParameters(param1, param2);
    }

    @Test
    public void stelaEquiSerialTest() {
        // Initialization
        final StelaEquinoctialParameters param = new StelaEquinoctialParameters(1.e7, 0.1, -0.3,
                -0.5, 0.05, 3., Constants.CNES_STELA_MU, false);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(param,
                FramesFactory.getEME2000(), this.date);

        // serialisation
        final StelaEquinoctialOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        assertEqualsStelaEquiOrbit(orbit, orbit2);
        Assert.assertTrue(orbit.equals(orbit2));
        Assert.assertEquals(orbit.hashCode(), orbit2.hashCode());
    }

    private static void assertEqualsStelaEquiOrbit(final StelaEquinoctialOrbit orbit1,
            final StelaEquinoctialOrbit orbit2) {
        assertEqualsOrbit(orbit1, orbit2);
        final StelaEquinoctialParameters param1 = orbit1.getEquinoctialParameters();
        final StelaEquinoctialParameters param2 = orbit2.getEquinoctialParameters();
        KeplerianParametersTest.assertEqualsKeplerianParameters(param1.getKeplerianParameters(),
                param2.getKeplerianParameters());
    }

    private static void assertEqualsOrbit(final Orbit orbit1, final Orbit orbit2) {
        Assert.assertEquals(orbit1.getDate(), orbit2.getDate());
        Assert.assertEquals(orbit1.getMu(), orbit2.getMu(), 0);
        PVCoordinatesTest.assertEqualsPVCoordinates(orbit1.getPVCoordinates(), orbit2.getPVCoordinates());
        Assert.assertEquals(orbit1.getFrame().getName(), orbit2.getFrame().getName());
    }

    /**
     * setup
     */
    @Before
    public void setUp() {
        // Computation date
        this.date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        this.mu = 3.9860047e14;
    }
}
