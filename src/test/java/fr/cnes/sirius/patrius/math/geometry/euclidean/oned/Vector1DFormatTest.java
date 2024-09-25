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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.geometry.VectorFormat;

/**
 * Tests for {@link Vector1DFormat}, for code coverage only.
 * 
 * @version $Id: Vector1DFormatTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 1.3
 */
public class Vector1DFormatTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Code coverage
         * 
         * @featureDescription Code coverage
         * 
         * @coveredRequirements none
         */
        CODE_COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testConstructors() {
        final String s1 = "GA";
        final String s2 = "BU";
        final String s3 = "ZO";
        final String s4 = "ME";
        final Vector1DFormat dflt = new Vector1DFormat();
        Assert.assertEquals(VectorFormat.DEFAULT_PREFIX, dflt.getPrefix());
        Assert.assertEquals(VectorFormat.DEFAULT_SUFFIX, dflt.getSuffix());
        Assert.assertEquals(VectorFormat.DEFAULT_SEPARATOR, dflt.getSeparator());
        final Vector1DFormat v1d1 = Vector1DFormat.getInstance();
        Assert.assertEquals(VectorFormat.DEFAULT_PREFIX, v1d1.getPrefix());
        Assert.assertEquals(VectorFormat.DEFAULT_SUFFIX, v1d1.getSuffix());
        Assert.assertEquals(VectorFormat.DEFAULT_SEPARATOR, v1d1.getSeparator());
        final Vector1DFormat v1d2 = Vector1DFormat.getInstance(Locale.US);
        Assert.assertEquals(VectorFormat.DEFAULT_PREFIX, v1d2.getPrefix());
        Assert.assertEquals(VectorFormat.DEFAULT_SUFFIX, v1d2.getSuffix());
        Assert.assertEquals(VectorFormat.DEFAULT_SEPARATOR, v1d2.getSeparator());
        final Vector1DFormat v1d3 = new Vector1DFormat(NumberFormat.getCurrencyInstance());
        Assert.assertEquals(VectorFormat.DEFAULT_PREFIX, v1d3.getPrefix());
        Assert.assertEquals(VectorFormat.DEFAULT_SUFFIX, v1d3.getSuffix());
        Assert.assertEquals(VectorFormat.DEFAULT_SEPARATOR, v1d3.getSeparator());
        final Vector1DFormat v1d4 = new Vector1DFormat(s1, s2);
        Assert.assertEquals(s1, v1d4.getPrefix());
        Assert.assertEquals(s2, v1d4.getSuffix());
        Assert.assertEquals(VectorFormat.DEFAULT_SEPARATOR, v1d4.getSeparator());
        final Vector1DFormat v1d5 = new Vector1DFormat(s3, s4, NumberFormat.getCurrencyInstance());
        Assert.assertEquals(s3, v1d5.getPrefix());
        Assert.assertEquals(s4, v1d5.getSuffix());
        Assert.assertEquals(VectorFormat.DEFAULT_SEPARATOR, v1d5.getSeparator());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testParse() {
        final Vector1DFormat dflt = Vector1DFormat.getInstance(Locale.US);
        final Vector1D vec1 = dflt.parse("{ 4.5 }");
        Assert.assertEquals(4.5, vec1.getX(), 0.);
        try {
            dflt.parse("{meuh}");
            Assert.fail("expected MathParseException");
        } catch (final MathParseException ex) {
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testFormat() {
        final Vector1DFormat dflt = Vector1DFormat.getInstance(Locale.US);
        final StringBuffer bff = new StringBuffer();
        bff.append("Vec:");
        final StringBuffer rez = dflt.format(new Vector1D(4.5), bff, new FieldPosition(0));
        Assert.assertEquals("Vec:{4.5}", rez.toString());
    }
}
