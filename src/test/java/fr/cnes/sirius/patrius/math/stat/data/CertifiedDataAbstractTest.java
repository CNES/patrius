/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.stat.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;

/**
 * @version $Id: CertifiedDataAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class CertifiedDataAbstractTest {

    private DescriptiveStatistics descriptives;

    private SummaryStatistics summaries;

    private Map<String, Double> certifiedValues;

    @Before
    public void setUp() throws IOException {
        this.descriptives = new DescriptiveStatistics();
        this.summaries = new SummaryStatistics();
        this.certifiedValues = new HashMap<String, Double>();

        this.loadData();
    }

    private void loadData() throws IOException {
        BufferedReader in = null;

        try {
            final URL resourceURL = this.getClass().getClassLoader().getResource(this.getResourceName());
            in = new BufferedReader(new InputStreamReader(resourceURL.openStream()));

            String line = in.readLine();
            while (line != null) {

                /*
                 * this call to StringUtils did little for the
                 * following conditional structure
                 */
                line = line.trim();

                // not empty line or comment
                if (!("".equals(line) || line.startsWith("#"))) {
                    final int n = line.indexOf('=');
                    if (n == -1) {
                        // data value
                        final double value = Double.parseDouble(line);
                        this.descriptives.addValue(value);
                        this.summaries.addValue(value);
                    } else {
                        // certified value
                        final String name = line.substring(0, n).trim();
                        final String valueString = line.substring(n + 1).trim();
                        final Double value = Double.valueOf(valueString);
                        this.certifiedValues.put(name, value);
                    }
                }
                line = in.readLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected abstract String getResourceName();

    protected double getMaximumAbsoluteError() {
        return 1.0e-5;
    }

    @After
    public void tearDown() {
        this.descriptives.clear();
        this.descriptives = null;

        this.summaries.clear();
        this.summaries = null;

        this.certifiedValues.clear();
        this.certifiedValues = null;
    }

    @Test
    public void testCertifiedValues() {
        for (final String name : this.certifiedValues.keySet()) {
            final Double expectedValue = this.certifiedValues.get(name);

            final Double summariesValue = this.getProperty(this.summaries, name);
            if (summariesValue != null) {
                TestUtils.assertEquals("summary value for " + name + " is incorrect.",
                    summariesValue.doubleValue(), expectedValue.doubleValue(),
                    this.getMaximumAbsoluteError());
            }

            final Double descriptivesValue = this.getProperty(this.descriptives, name);
            if (descriptivesValue != null) {
                TestUtils.assertEquals("descriptive value for " + name + " is incorrect.",
                    descriptivesValue.doubleValue(), expectedValue.doubleValue(),
                    this.getMaximumAbsoluteError());
            }
        }
    }

    protected Double getProperty(final Object bean, final String name) {
        try {
            // Get the value of prop
            final String prop = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
            final Method meth = bean.getClass().getMethod(prop, new Class[0]);
            final Object property = meth.invoke(bean, new Object[0]);
            if (meth.getReturnType().equals(Double.TYPE)) {
                return (Double) property;
            } else if (meth.getReturnType().equals(Long.TYPE)) {
                return Double.valueOf(((Long) property).doubleValue());
            } else {
                Assert.fail("wrong type: " + meth.getReturnType().getName());
            }
        } catch (final NoSuchMethodException nsme) {
            // ignored
        } catch (final InvocationTargetException ite) {
            Assert.fail(ite.getMessage());
        } catch (final IllegalAccessException iae) {
            Assert.fail(iae.getMessage());
        }
        return null;
    }
}
