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
 * HISTORY
 * VERSION:4.11:DM:DM-3218:22/05/2023:[PATRIUS] Evolution de format des fichiers SP3
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2504:27/01/2021:[PATRIUS] Consommation de fichiers SP3 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.files.sp3;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.files.general.OrbitFile.TimeSystem;
import fr.cnes.sirius.patrius.files.general.SatelliteTimeCoordinate;
import fr.cnes.sirius.patrius.files.sp3.SP3File.SP3OrbitType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class SP3ParserTest {

    @Test
    public void testParseSP3a1() throws PatriusException {
        // simple test for version sp3-a, only contains position entries
        final String ex = "/sp3/sp3_a_example1.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(25, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("1");
        Assert.assertEquals(3, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        checkPVEntry(new PVCoordinates(new Vector3D(16258.524750E3, -3529.015750E3, -20611.427050E3),
            new Vector3D(0, 0, 0)), coord.getCoordinate());
    }

    @Test
    public void testParseSP3a2() throws PatriusException {
        // simple test for version sp3-a, contains p/v entries
        final String ex = "/sp3/sp3_a_example2.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(25, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("1");
        Assert.assertEquals(3, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        // V 1 -6560.373522 25605.954994 -9460.427179 -0.024236
        checkPVEntry(new PVCoordinates(new Vector3D(16258.524750E3, -3529.015750E3, -20611.427050E3),
            new Vector3D(-656.0373, 2560.5954, -946.0427)),
            coord.getCoordinate());
    }

    @Test
    public void testParseSP3c1() throws PatriusException {
        // simple test for version sp3-c, contains p/v entries
        final String ex = "/sp3/sp3_c_example1.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.HLM, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(26, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("G01");
        Assert.assertEquals(2, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 2001 8 8 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // PG01 -11044.805800 -10475.672350 21929.418200 189.163300 18 18 18 219
        checkPVEntry(new PVCoordinates(new Vector3D(-11044.805800E3, -10475.672350E3, 21929.418200E3),
            new Vector3D(0, 0, 0)), coord.getCoordinate());
    }

    /**
     * c3 and d1 are identical except for their version.
     * 
     * @throws PatriusException
     */
    @Test
    public void testParseSP3c3() throws PatriusException {
        // simple test for version sp3-c, only contains position entries
        final String ex = "/sp3/sp3_c_example3.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(111, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("G01");
        Assert.assertEquals(864, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 2022 5 16 10 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(2022, 5, 16, 10, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // PG01 -15087.765162 -13356.559871 -17784.747754 361.860426
        checkPVEntry(new PVCoordinates(new Vector3D(-1.5087765162E7, -1.3356559871E7, -1.7784747754E7),
            new Vector3D(0, 0, 0)), coord.getCoordinate());
    }

    /**
     * c4 and d2 are identical except for their version.
     * 
     * @throws PatriusException
     */
    @Test
    public void testParseSP3c4() throws PatriusException {
        // simple test for version sp3-d, only contains position entries
        final String ex = "/sp3/sp3_c_example4.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(51, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("G01");
        Assert.assertEquals(96, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 2022 5 18 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(2022, 5, 18, 0, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // PG01 -4169.068554 16849.574018 -20292.262586 360.722533
        checkPVEntry(new PVCoordinates(new Vector3D(-4.169068554E6, 1.6849574018E7, -2.0292262586E7),
            new Vector3D(0, 0, 0)), coord.getCoordinate());
    }

    /**
     * c3 and d1 are identical except for their version.
     * 
     * @throws PatriusException
     */
    @Test
    public void testParseSP3d1() throws PatriusException {
        // simple test for version sp3-c, only contains position entries
        final String ex = "/sp3/sp3_d_example1.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(111, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("G01");
        Assert.assertEquals(864, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 2022 5 16 10 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(2022, 5, 16, 10, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // PG01 -15087.765162 -13356.559871 -17784.747754 361.860426
        checkPVEntry(new PVCoordinates(new Vector3D(-1.5087765162E7, -1.3356559871E7, -1.7784747754E7),
            new Vector3D(0, 0, 0)), coord.getCoordinate());
    }

    /**
     * c4 and d2 are identical except for their version.
     * 
     * @throws PatriusException
     */
    @Test
    public void testParseSP3d2() throws PatriusException {
        // simple test for version sp3-d, only contains position entries
        final String ex = "/sp3/sp3_d_example2.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(51, file.getSatelliteCount());

        final List<SatelliteTimeCoordinate> coords = file.getSatelliteCoordinates("G01");
        Assert.assertEquals(96, coords.size());

        final SatelliteTimeCoordinate coord = coords.get(0);

        // 2022 5 18 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(2022, 5, 18, 0, 0, 0,
            TimeScalesFactory.getGPS()), coord.getEpoch());

        // PG01 -4169.068554 16849.574018 -20292.262586 360.722533
        checkPVEntry(new PVCoordinates(new Vector3D(-4.169068554E6, 1.6849574018E7, -2.0292262586E7),
            new Vector3D(0, 0, 0)), coord.getCoordinate());
    }

    /**
     * Version "e" does not exist yet. Should return a PatruisException.
     * 
     * @throws PatriusException
     */
    @Test
    public void testParseSP3e1() throws PatriusException {
        // simple test for version sp3-e, only contains position entries
        final String ex = "/sp3/sp3_e_example1.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);

        try {
            parser.parse(inEntry);
            Assert.fail();
        } catch (PatriusException pe) {
            Assert
                .assertEquals(pe.getMessage(),
                    MessageFormat.format(PatriusMessages.SP3_UNSUPPORTED_VERSION.getSourceString(), "e"));
        }
    }

    private static void checkPVEntry(final PVCoordinates expected, final PVCoordinates actual) {
        final Vector3D expectedPos = expected.getPosition();
        final Vector3D expectedVel = expected.getVelocity();

        final Vector3D actualPos = actual.getPosition();
        final Vector3D actualVel = actual.getVelocity();

        final double eps = 1e-2;

        Assert.assertEquals(expectedPos.getX(), actualPos.getX(), eps);
        Assert.assertEquals(expectedPos.getY(), actualPos.getY(), eps);
        Assert.assertEquals(expectedPos.getZ(), actualPos.getZ(), eps);

        Assert.assertEquals(expectedVel.getX(), actualVel.getX(), eps);
        Assert.assertEquals(expectedVel.getY(), actualVel.getY(), eps);
        Assert.assertEquals(expectedVel.getZ(), actualVel.getZ(), eps);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
