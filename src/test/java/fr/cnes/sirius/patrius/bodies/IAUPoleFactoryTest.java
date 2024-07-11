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
 * @history created 17/02/2017
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe UserCelestialBody
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:769:20/02/2017:add UserDefinedCelestialBody
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader.EphemerisType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Test class for class {@link IAUPoleFactory}.
 */
public class IAUPoleFactoryTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(IAUPoleFactoryTest.class.getSimpleName(), "IAU pole factory");
    }

    @Test
    public void testPoleData() {
        Utils.setDataRoot("regular-data");

        Report.printMethodHeader("testPoleData", "Pole data (alpha, delta, W) computation", "Scilab", 1E-12,
            ComparisonType.ABSOLUTE);

        // Actual data
        final IAUPole sunActual = IAUPoleFactory.getIAUPole(EphemerisType.SUN);
        final IAUPole mercuryActual = IAUPoleFactory.getIAUPole(EphemerisType.MERCURY);
        final IAUPole venusActual = IAUPoleFactory.getIAUPole(EphemerisType.VENUS);
        final IAUPole earthActual = IAUPoleFactory.getIAUPole(EphemerisType.EARTH);
        final IAUPole moonActual = IAUPoleFactory.getIAUPole(EphemerisType.MOON);
        final IAUPole marsActual = IAUPoleFactory.getIAUPole(EphemerisType.MARS);
        final IAUPole jupiterActual = IAUPoleFactory.getIAUPole(EphemerisType.JUPITER);
        final IAUPole saturnActual = IAUPoleFactory.getIAUPole(EphemerisType.SATURN);
        final IAUPole uranusActual = IAUPoleFactory.getIAUPole(EphemerisType.URANUS);
        final IAUPole neptuneActual = IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE);
        final IAUPole plutoActual = IAUPoleFactory.getIAUPole(EphemerisType.PLUTO);

        // Expected
        final double[] sunExpected = { 4.9939105887313753, 1.1147417932487782, 306.963719657295144 };
        final double[] mercuryExpected = { 4.90452515416524903, 1.07187885346933154, 137.958817908393883 };
        final double[] venusExpected = { 4.7605600677397328, 1.17216312563939162, -29.1087600757607241 };
        final double[] earthExpected = { -0.00037797261227981, 1.57046788569499030, 7777.99709134327622 };
        final double[] moonExpected = { 4.65293449930367586, 1.175016428320405, 284.507916968532356 };
        final double[] marsExpected = { 5.54452436292647466, 0.92300764450951023, 7560.37076011738009 };
        final double[] jupiterExpected = { 4.67846437218242261, 1.12566767096894793, 18754.0231258159874 };
        final double[] saturnExpected = { 0.70839046230096114, 1.4579933385950568, 17463.0415026143928 };
        final double[] uranusExpected = { 4.49092415159912850, -0.26485371399013952, -10790.1096689685528 };
        final double[] neptuneExpected = { 5.224736088099851, 0.74961911208199394, 11555.1833790136407 };
        final double[] plutoExpected = { 2.32116573210481869, -0.10756464180041053, 1218.04185854675688 };

        // Check
        this.checkIAUData(sunActual, sunExpected, "Sun");
        this.checkIAUData(mercuryActual, mercuryExpected, "Mercury");
        this.checkIAUData(venusActual, venusExpected, "Venus");
        this.checkIAUData(earthActual, earthExpected, "Earth");
        this.checkIAUData(moonActual, moonExpected, "Moon");
        this.checkIAUData(marsActual, marsExpected, "Mars");
        this.checkIAUData(jupiterActual, jupiterExpected, "Jupiter");
        this.checkIAUData(saturnActual, saturnExpected, "Saturn");
        this.checkIAUData(uranusActual, uranusExpected, "Uranus");
        this.checkIAUData(neptuneActual, neptuneExpected, "Neptune");
        this.checkIAUData(plutoActual, plutoExpected, "Pluto");
    }

    /**
     * Check IAU data.
     * 
     * @param actual
     *        actual result as IAUPole object
     * @param expected
     *        expected result [pole alpha, pole delta, prime meridian angle]
     * @param body
     *        body name
     */
    private void checkIAUData(final IAUPole actual, final double[] expected, final String body) {
        final AbsoluteDate ref = new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTDB());
        final AbsoluteDate date = ref.shiftedBy(1234. * Constants.JULIAN_DAY);
        Report.printToReport(body + " alpha", this.mod(expected[0]), this.mod(actual.getPole(date).getAlpha()));
        Report.printToReport(body + " delta", this.mod(expected[1]), this.mod(actual.getPole(date).getDelta()));
        Report.printToReport(body + " W", this.mod(expected[2]), this.mod(actual.getPrimeMeridianAngle(date)));
        // Results are not perfectly accurate since Scilab coding does take into account time dilatation due to TDB
        // scale
        Assert.assertEquals(0., actual.getPole(date).distance(new Vector3D(expected[0], expected[1])), 2E-12);
        Assert.assertEquals(expected[2], actual.getPrimeMeridianAngle(date), 1E-6);
    }

    /**
     * Modulo 2Pi.
     * 
     * @param value
     *        value
     * @return value [2Pi]
     */
    private double mod(final double value) {
        return (value + 2. * FastMath.PI) % (2. * FastMath.PI);
    }
}
