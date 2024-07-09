/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.cnesmerge.data;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import fr.cnes.sirius.patrius.data.PoissonSeries;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * -this class adds tests in order to raise the cover rate of PoissonSeries
 * </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: PoissonSeriesTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class PoissonSeriesTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Increase the code coverage
         * 
         * @featureDescription Increase the code coverage
         * 
         * @coveredRequirements NA
         */
        COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.PoissonSeries#PoissonSeries(java.io.InputStream, double, java.lang.String)}
     * 
     * @description the J described in the data line is not the expected one : one or more is missing.
     * 
     * @input none
     * 
     * @output OrekitException
     * 
     * @testPassCriteria the constructor should detect the leap in J index and throw an OrekitException
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public final void testMissingJ() throws PatriusException {
        final String data = "  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n" + "j = 450  Nb of terms = 1\n"
            + "1 0.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
        new PoissonSeries(new ByteArrayInputStream(data.getBytes()), 1.0, "");
        fail();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.PoissonSeries#PoissonSeries(java.io.InputStream, double, java.lang.String)}
     * 
     * @description there are too many coefficients in the data line (18).
     * 
     * @input none
     * 
     * @output OrekitException
     * 
     * @testPassCriteria the constructor should detect there are too many coefficients and throw an OrekitException
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public final void testTooManyCoeffs() throws PatriusException {
        final String data = "  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n" + "j = 0  Nb of terms = 1\n"
            + "1 0.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0\n";
        new PoissonSeries(new ByteArrayInputStream(data.getBytes()), 1.0, "");
        fail();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.PoissonSeries#PoissonSeries(java.io.InputStream, double, java.lang.String)}
     * 
     * @description there are not enough coefficients in the data line (18).
     * 
     * @input none
     * 
     * @output OrekitException
     * 
     * @testPassCriteria the constructor should detect there are not enough coefficients and throw an OrekitException
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public final void testTooFewCoeffs() throws PatriusException {
        final String data = "  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n" + "j = 0  Nb of terms = 1\n"
            + "1 0.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0\n";
        new PoissonSeries(new ByteArrayInputStream(data.getBytes()), 1.0, "");
        fail();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.PoissonSeries#PoissonSeries(java.io.InputStream, double, java.lang.String)}
     * 
     * @description there are not enough coefficients in the data line (16), but with the spaces at the beginning,
     *              the split function applied detects 17 coefficients, with the first one an empty String ("").
     * 
     * @input none
     * 
     * @output OrekitException
     * 
     * @testPassCriteria the constructor should detect there are not enough coefficients and throw an OrekitException
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public final void testMimickSeventeenCoeffs() throws PatriusException {
        final String data = "  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n" + "j = 0  Nb of terms = 1\n"
            + "  1 0.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0\n";
        new PoissonSeries(new ByteArrayInputStream(data.getBytes()), 1.0, "");
        fail();
    }
}
