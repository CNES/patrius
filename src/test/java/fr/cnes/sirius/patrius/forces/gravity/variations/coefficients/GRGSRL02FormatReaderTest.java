/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * GRGSRL02FormatReader test.
 * 
 * @author houdroger
 * 
 */
public class GRGSRL02FormatReaderTest {

    /** threshold for validation */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle variable potential coefficients GRGSRL02 reader
         * 
         * @featureDescription variable coefficients GRGSRL02 reader test
         * 
         * @coveredRequirements DV-MOD_190, DV-MOD_220, DV-MOD_230
         */
        VARIABLE_COEFFICIENTS_GRGSRL02_READER
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_GRGSRL02_READER}
     * 
     * @testedMethod {@link GRGSRL02FormatReader#GRGSRL02FormatReader(String)}
     * @testedMethod {@link GRGSRL02FormatReader#getMu()}
     * @testedMethod {@link GRGSRL02FormatReader#getAe()}
     * @testedMethod {@link GRGSRL02FormatReader#stillAcceptsData()}
     * 
     * @description constants
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected constants, threshold 1e-14
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testVariablePotentialCoefficientsReader() throws PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
        final GRGSRL02FormatReader reader = new GRGSRL02FormatReader(VariableGravityFieldFactory.GRGSRL02_FILENAME);
        DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
        Assert.assertFalse(reader.stillAcceptsData());
        Assert.assertEquals(0.39860044150000E+15, reader.getMu(), this.eps);
        Assert.assertEquals(0.63781364600000E+07, reader.getAe(), this.eps);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_GRGSRL02_READER}
     * 
     * @testedMethod {@link GRGSRL02FormatReader#getDate()}
     * @testedMethod {@link GRGSRL02FormatReader#stillAcceptsData()}
     * 
     * @description date and name
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected constants, threshold 1e-14
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testStillAcceptsData() throws PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
        final GRGSRL02FormatReader reader = new GRGSRL02FormatReader(VariableGravityFieldFactory.GRGSRL02_FILENAME);
        DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
        Assert.assertFalse(reader.stillAcceptsData());
        Assert.assertEquals(
            0,
            new AbsoluteDate(2005, 1, 1, TimeScalesFactory.getUTC()).offsetFrom(reader.getDate(),
                TimeScalesFactory.getUTC()), this.eps);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_GRGSRL02_READER}
     * 
     * @testedMethod {@link GRGSRL02FormatReader#getSupportedNames()}
     * 
     * @description date and name
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected constants, threshold 1e-14
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetSupportedNames() {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
        final GRGSRL02FormatReader reader = new GRGSRL02FormatReader(VariableGravityFieldFactory.GRGSRL02_FILENAME);
        Assert.assertSame(reader.getSupportedNames(), VariableGravityFieldFactory.GRGSRL02_FILENAME);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_GRGSRL02_READER}
     * 
     * @testedMethod {@link GRGSRL02FormatReader#getData()}
     * 
     * @description date and name
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected constants, threshold 1e-14
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetData() throws PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
        final VariablePotentialCoefficientsReader reader = new GRGSRL02FormatReader(
            VariableGravityFieldFactory.GRGSRL02_FILENAME);
        DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
        final Map<Integer, Map<Integer, VariablePotentialCoefficientsSet>> data = reader.getData();

        Assert.assertEquals(161, data.size());
        for (int i = 0; i < 161; i++) {
            Assert.assertEquals(i + 1, data.get(i).size());
        }
        Assert.assertEquals(0, (9.5358963053068E-14 - data.get(50).get(0).getCc()[4]) / 9.5358963053068E-14, this.eps);
        Assert
            .assertEquals(0, (-1.1443954852192E-12 - data.get(20).get(1).getSc()[1]) / -1.1443954852192E-12, this.eps);
        Assert.assertEquals(160, reader.getMaxDegree());
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_GRGSRL02_READER}
     * 
     * @testedMethod {@link GRGSRL02FormatReader#GRGSRL02FormatReader(String)}
     * 
     * @description constructor exceptions
     * 
     * @input bad file
     * 
     * @output none
     * 
     * @testPassCriteria fail
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testBadFiles() throws PatriusException {
        try {
            CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
            final GRGSRL02FormatReader reader = new GRGSRL02FormatReader("EIGEN-GRGS.RL02bis.MEAN-FIELD_bad_file");
            DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
            Assert.fail();
        } catch (final Exception e) {
            // expected !
        }
        try {
            CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
            final GRGSRL02FormatReader reader = new GRGSRL02FormatReader("EIGEN-GRGS.RL02bis.MEAN-FIELD_bad_file2");
            DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
            Assert.fail();
        } catch (final Exception e) {
            // expected !
        }
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_GRGSRL02_READER}
     * 
     * @testedMethod {@link GRGSRL02FormatReader#GRGSRL02FormatReader(String)}
     * 
     * @description test inner class comparisons
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected order
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testDegOrdKey() throws IllegalArgumentException, SecurityException, InstantiationException,
                               IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final Class<?> dec = GRGSRL02FormatReader.class.getDeclaredClasses()[0];

        final Object instance1 = dec.getDeclaredConstructor(new Class<?>[] { int.class, int.class }).newInstance(
            new Integer[] { 3, 2 });
        final Object instance2 = dec.getDeclaredConstructor(new Class<?>[] { int.class, int.class }).newInstance(
            new Integer[] { 4, 2 });
        final Object instance3 = dec.getDeclaredConstructor(new Class<?>[] { int.class, int.class }).newInstance(
            new Integer[] { 5, 2 });

        Assert.assertEquals(-1, dec.getDeclaredMethod("compareTo", dec).invoke(instance1, instance2));
        Assert.assertEquals(1, dec.getDeclaredMethod("compareTo", dec).invoke(instance3, instance2));

    }

}
