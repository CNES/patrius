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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test class for IERS 2003 and 2010 models. Since they are very close, the models are tested against
 * the same data.
 * 
 * @author Rami Houdroge
 * @version $Id: IERSTidalCorrectionModelsTest.java 18088 2017-10-02 17:01:51Z bignon $
 * @since 1.3
 */
public class IERSTidalCorrectionModelsTest {

    /** Test data. */
    private final Map<Double, Double[]> data = new TreeMap<>();

    /** Models. */
    private final IERS2003TidalCorrection corr03 = new IERS2003TidalCorrection();

    /** Models. */
    private final IERS2010TidalCorrection corr10 = new IERS2010TidalCorrection();

    /** Cached. */
    private final TidalCorrectionCache ccorr10 = new TidalCorrectionCache(new IERS2010TidalCorrection());

    /** Angular error corresponding to 1 mm on GEO orbit. */
    private final double eps1mmGEO = .001 / 42e6;

    /** UT1-UTC error corresponding to 1 mm on GEO orbit. */
    private final double epsT1mmGEO = .001 / 42e6 / (2 * FastMath.PI) * 86400;

    /** Ref date. */
    private final AbsoluteDate ref = new AbsoluteDate(
        new DateTimeComponents(DateComponents.FIFTIES_EPOCH, TimeComponents.H00),
        TimeScalesFactory.getTT());

    /** Constants. */
    private final double ar = Constants.ARC_SECONDS_TO_RADIANS;

    /** Test polar correction. */
    @Test
    public void testGetPoleCorrection() {

        AbsoluteDate date;
        Double[] current;

        for (final Double jjc : this.data.keySet()) {
            current = this.data.get(jjc);
            date = this.getDate(jjc);

            Assert.assertEquals(current[0] * this.ar, this.corr10.getPoleCorrection(date).getXp(), this.eps1mmGEO);
            Assert.assertEquals(current[1] * this.ar, this.corr10.getPoleCorrection(date).getYp(), this.eps1mmGEO);
            Assert.assertEquals(current[0] * this.ar, this.corr03.getPoleCorrection(date).getXp(), this.eps1mmGEO);
            Assert.assertEquals(current[1] * this.ar, this.corr03.getPoleCorrection(date).getYp(), this.eps1mmGEO);
            Assert.assertEquals(current[0] * this.ar, this.ccorr10.getPoleCorrection(date).getXp(), this.eps1mmGEO);
            Assert.assertEquals(current[1] * this.ar, this.ccorr10.getPoleCorrection(date).getYp(), this.eps1mmGEO);
        }
    }

    /** UT1-UTC correction. */
    @Test
    public void testGetUT1MinusUTCCorrection() {

        AbsoluteDate date;
        Double[] current;

        for (final Double jjc : this.data.keySet()) {
            current = this.data.get(jjc);
            date = this.getDate(jjc);

            Assert.assertEquals(current[2], this.ccorr10.getUT1Correction(date), this.epsT1mmGEO);
            Assert.assertEquals(current[2], this.corr10.getUT1Correction(date), this.epsT1mmGEO);
            Assert.assertEquals(current[2], this.corr03.getUT1Correction(date), this.epsT1mmGEO);
        }
    }

    /** LOD correction. */
    @Test
    public void testGetLODCorrection() {

        AbsoluteDate date = new AbsoluteDate();
        Double[] current = null;

        final TidalCorrectionCache newCorr10 = new TidalCorrectionCache(new IERS2010TidalCorrection(), 10, 10);

        for (final Double jjc : this.data.keySet()) {
            current = this.data.get(jjc);
            date = this.getDate(jjc);

            Assert.assertEquals(current[3], newCorr10.getLODCorrection(date), this.epsT1mmGEO);
            Assert.assertEquals(current[3], this.corr10.getLODCorrection(date), this.epsT1mmGEO);
            Assert.assertEquals(0, this.corr03.getLODCorrection(date), this.epsT1mmGEO);
        }

        // for coverage
        Assert.assertEquals(current[3], newCorr10.getLODCorrection(date), this.epsT1mmGEO);
    }

    /**
     * Date from jjcnes.
     * 
     * @param jjcnes
     *        jjcnes
     * @return the date
     */
    private AbsoluteDate getDate(final double jjcnes) {
        // Return offset from reference epoch in days
        final AbsoluteDate res = new AbsoluteDate(this.ref, jjcnes * Constants.JULIAN_DAY, TimeScalesFactory.getTT());
        return res;
    }

    /**
     * Get the reference data.
     * 
     * @throws PatriusException
     *         if illegal file format
     * @throws IOException
     *         if reading error
     */
    @Before
    public void testsetup() throws PatriusException, IOException {

        final String path = Utils.class.getClassLoader().getResource("iers2010").getPath();

        final FileInputStream is = new FileInputStream(new File(path + File.separator + "tides2010" + File.separator
            + "IERS2010TidalCorrectionsRefs"));
        final BufferedReader bf = new BufferedReader(new InputStreamReader(is));

        bf.readLine();
        for (String line = bf.readLine(); line != null; line = bf.readLine()) {

            final String[] tab = line.trim().split("\\s+");

            if (tab.length != 6) {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }

            final double[] t = new double[6];
            for (int i = 0; i < tab.length; i++) {
                t[i] = Double.parseDouble(tab[i]);
            }

            this.data.put(t[0], new Double[] { t[2], t[3], t[4], t[5] });
        }
    }
}
