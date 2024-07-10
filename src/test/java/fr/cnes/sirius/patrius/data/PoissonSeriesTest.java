/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class PoissonSeriesTest {

    @Test(expected = PatriusException.class)
    public void testEmptyData() throws PatriusException {
        this.buildData("");
    }

    @Test(expected = PatriusException.class)
    public void testNoCoeffData() throws PatriusException {
        this.buildData("this is NOT an IERS nutation model file\n");
    }

    @Test(expected = PatriusException.class)
    public void testEmptyArrayData() throws PatriusException {
        this.buildData("  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n");
    }

    @Test(expected = PatriusException.class)
    public void testMissingTermData() throws PatriusException {
        this.buildData("  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n"
            + "j = 0  Nb of terms = 1\n");
    }

    private PoissonSeries buildData(final String data) throws PatriusException {
        return new PoissonSeries(new ByteArrayInputStream(data.getBytes()),
            1.0, "<file-content>" + data + "</file-content>");
    }

    @Test(expected = PatriusException.class)
    public void testNoFile() throws PatriusException {
        final InputStream stream =
            PoissonSeriesTest.class.getResourceAsStream("/org/orekit/resources/missing");
        new PoissonSeries(stream, 1.0, "missing");
    }

    @Test
    public void testSmall() throws PatriusException {
        final String data =
            "  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n"
                + "j = 0  Nb of terms = 1\n"
                + "1 0.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
        final PoissonSeries nd =
            new PoissonSeries(new ByteArrayInputStream(data.getBytes()), 1.0, "");
        Assert.assertNotNull(nd);
    }

    @Test
    public void testSecondsMarkers() throws PatriusException {
        final String data =
            "  0''.0 + 0''.0 t - 0''.0 t^2 - 0''.0 t^3 - 0''.0 t^4 + 0''.0 t^5\n"
                + "j = 0  Nb of terms = 1\n"
                + "1 0.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
        final PoissonSeries nd =
            new PoissonSeries(new ByteArrayInputStream(data.getBytes()), 1.0, "");
        Assert.assertNotNull(nd);
    }

    @Test
    public void testExtract() throws PatriusException {
        final String data =
            "Expression for the X coordinate of the CIP in the GCRS based on the IAU2000A\n"
                + "precession-nutation model\n"
                + "\n"
                + "\n"
                + "----------------------------------------------------------------------\n"
                + "\n"
                + "X = polynomial part + non-polynomial part\n"
                + "\n"
                + "----------------------------------------------------------------------\n"
                + "\n"
                + "Polynomial part (unit microarcsecond)\n"
                + "\n"
                + "  -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
                + "\n"
                + "----------------------------------------------------------------------\n"
                + "\n"
                + "Non-polynomial part (unit microarcsecond)\n"
                + "(ARG being for various combination of the fundamental arguments of the nutation theory)\n"
                + "\n"
                + "  Sum_i[a_{s,0})_i * sin(ARG) + a_{c,0})_i * cos(ARG)] \n"
                + "\n"
                + "+ Sum_i)j=1,4 [a_{s,j})_i * t^j * sin(ARG) + a_{c,j})_i * cos(ARG)] * t^j]\n"
                + "\n"
                + "The Table below provides the values for a_{s,j})_i and a_{c,j})_i\n"
                + "\n"
                + "The expressions for the fundamental arguments appearing in columns 4 to 8 (luni-solar part) \n"
                + "and in columns 6 to 17 (planetary part) are those of the IERS Conventions 2000\n"
                + "\n"
                + "----------------------------------------------------------------------\n"
                + "\n"
                + "    i    a_{s,j})_i      a_{c,j})_i    l    l'   F    D   Om L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A\n"
                + "\n"
                + "----------------------------------------------------------------------\n"
                + "-16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
                + "j = 0  Nb of terms = 2\n"
                + "\n"
                + "   1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
                + "1306           0.11           0.00    0    0    4   -4    4    0    0    0    0    0    0    0    0    0\n"
                + "\n"
                + "j = 1  Nb of terms = 2\n"
                + "\n"
                + " 1307       -3328.48      205833.15    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
                + " 1559           0.00          -0.10    1   -1   -2   -2   -1    0    0    0    0    0    0    0    0    0\n"
                + "\n"
                + " j = 2  Nb of terms = 2\n"
                + "\n"
                + "  1560        2038.00          82.26    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
                + "  1595          -0.12           0.00    1    0   -2   -2   -1    0    0    0    0    0    0    0    0    0\n"
                + "  \n"
                + " j = 3  Nb of terms = 2\n"
                + "\n"
                + "  1596           1.76         -20.39    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
                + "  1599           0.00           0.20    0    0    0    0    2    0    0    0    0    0    0    0    0    0\n"
                + "\n"
                + " j = 4  Nb of terms = 1\n"
                + "       \n"
                + "  1600          -0.10          -0.02    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n";
        Assert.assertNotNull(new PoissonSeries(new ByteArrayInputStream(data.getBytes()),
            1.0, "dummy"));
    }

    @Test
    public void testTrueFiles() throws PatriusException {
        final String directory = "/META-INF/IERS-conventions-2003/";
        final InputStream xStream =
            this.getClass().getResourceAsStream(directory + "tab5.2a.txt");
        Assert.assertNotNull(new PoissonSeries(xStream, 1.0, "tab5.2a.txt"));
        final InputStream yStream =
            this.getClass().getResourceAsStream(directory + "tab5.2b.txt");
        Assert.assertNotNull(new PoissonSeries(yStream, 1.0, "tab5.2b.txt"));
        final InputStream zStream =
            this.getClass().getResourceAsStream(directory + "tab5.2c.txt");
        Assert.assertNotNull(new PoissonSeries(zStream, 1.0, "tab5.2c.txt"));
        final InputStream gstStream =
            this.getClass().getResourceAsStream(directory + "tab5.4.txt");
        Assert.assertNotNull(new PoissonSeries(gstStream, 1.0, "tab5.4.txt"));
    }

}
