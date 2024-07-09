/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * 
 * Copyright 2011-2017 CNES
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
 * @history created 2.3.1
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/01/2015:create abstract class JacobiansParameterizable
 * VERSION::DM:505:19/08/2015:replace HashMap by ArrayList
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class {@link JacobiansParameterizable}.
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id: JacobiansParameterizableTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 2.3.1
 * 
 */
public class JacobiansParameterizableTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle JacobiansParameterizable
         * 
         * @featureDescription Test JacobiansParameterizable class
         * 
         * @coveredRequirements
         */
        JACOBIANS_PARAMETERIZABLE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#JACOBIANS_PARAMETERIZABLE}
     * 
     * @testedMethod {@link JacobiansParameterizable#JacobiansParameterizable(java.util.ArrayList)}
     * @testedMethod {@link JacobiansParameterizable#JacobiansParameterizable(java.util.HashMap)}
     * @testedMethod {@link JacobiansParameterizable#addJacobiansParameter(Parameter...)}
     * 
     * @description Test constructors and addJacobiansParameter.
     * 
     * @comments Test for coverage purpose
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public final void testname() {
        final ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        final Parameter p = new Parameter("test", 1);
        paramList.add(p);
        final JacobiansParameterizable jp = new JacobiansParameterizable(paramList){
            @Override
            public
                    void
                    addDAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel)
                                                                                                                throws PatriusException {
            }

            @Override
            public
                    void
                    addDAccDParam(final SpacecraftState s, final Parameter param, final double[] dAccdParam)
                                                                                                            throws PatriusException {
            }
        };
        Assert.assertEquals(1, jp.getParameters().size());
        Assert.assertTrue(jp.supportsParameter(p));
        Assert.assertFalse(jp.supportsJacobianParameter(p));

        final Parameter p2 = new Parameter("test", 1);
        final Parameter p3 = new Parameter("test", 1);
        jp.addJacobiansParameter(p2, p3);
        Assert.assertEquals(3, jp.getParameters().size());
        Assert.assertTrue(jp.supportsParameter(p2));
        Assert.assertTrue(jp.supportsJacobianParameter(p2));
        Assert.assertTrue(jp.supportsParameter(p3));
        Assert.assertTrue(jp.supportsJacobianParameter(p3));

        final IParamDiffFunction cste = new ConstantFunction(p);
        final JacobiansParameterizable jp2 = new JacobiansParameterizable(cste){
            @Override
            public
                    void
                    addDAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel)
                                                                                                                throws PatriusException {
            }

            @Override
            public
                    void
                    addDAccDParam(final SpacecraftState s, final Parameter param, final double[] dAccdParam)
                                                                                                            throws PatriusException {
            }
        };
        Assert.assertEquals(1, jp2.getParameters().size());
        Assert.assertTrue(jp2.supportsParameter(p));
        Assert.assertFalse(jp2.supportsJacobianParameter(p));

        // Coverage : Parameterizable
        final Parameterizable parameterizable = new Parameterizable(paramList);
        Assert.assertEquals(1, parameterizable.getParameters().size());
        Assert.assertTrue(parameterizable.supportsParameter(p));
    }

}
