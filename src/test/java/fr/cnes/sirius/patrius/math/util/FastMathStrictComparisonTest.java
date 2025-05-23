/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test to compare FastMath results against StrictMath results for boundary values.
 * <p>
 * Running all tests independently: <br/>
 * {@code mvn test -Dtest=FastMathStrictComparisonTest}<br/>
 * or just run tests against a single method (e.g. scalb):<br/>
 * {@code mvn test -Dtest=FastMathStrictComparisonTest -DargLine="-DtestMethod=scalb"}
 */
@RunWith(Parameterized.class)
public class FastMathStrictComparisonTest {

    // Values which often need special handling
    private static final Double[] DOUBLE_SPECIAL_VALUES = {
        -0.0, +0.0, // 1,2
        Double.NaN, // 3
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, // 4,5
        -Double.MAX_VALUE, Double.MAX_VALUE, // 6,7
        // decreasing order of absolute value to help catch first failure
        -Precision.EPSILON, Precision.EPSILON, // 8,9
        -Precision.SAFE_MIN, Precision.SAFE_MIN, // 10,11
        -Double.MIN_VALUE, Double.MIN_VALUE, // 12,13
    };

    private static final Float[] FLOAT_SPECIAL_VALUES = {
        -0.0f, +0.0f, // 1,2
        Float.NaN, // 3
        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, // 4,5
        Float.MIN_VALUE, Float.MAX_VALUE, // 6,7
        -Float.MIN_VALUE, -Float.MAX_VALUE, // 8,9
    };

    private static final Object[] LONG_SPECIAL_VALUES = {
        -1, 0, 1, // 1,2,3
        Long.MIN_VALUE, Long.MAX_VALUE, // 4,5
    };

    private static final Object[] INT_SPECIAL_VALUES = {
        -1, 0, 1, // 1,2,3
        Integer.MIN_VALUE, Integer.MAX_VALUE, // 4,5
    };

    private final Method mathMethod;
    private final Method fastMethod;
    private final Type[] types;
    private final Object[][] valueArrays;

    public FastMathStrictComparisonTest(final Method m, final Method f, final Type[] types, final Object[][] data)
        throws Exception {
        this.mathMethod = m;
        this.fastMethod = f;
        this.types = types;
        this.valueArrays = data;
    }

    @Test
    public void test1() throws Exception {
        setupMethodCall(this.mathMethod, this.fastMethod, this.types, this.valueArrays);
    }

    private static boolean isNumber(final Double d) {
        return !(d.isInfinite() || d.isNaN());
    }

    private static boolean isNumber(final Float f) {
        return !(f.isInfinite() || f.isNaN());
    }

    private static void reportFailedResults(final Method mathMethod, final Object[] params, final Object expected,
                                            final Object actual,
                                            final int[] entries) {
        final String methodName = mathMethod.getName();
        String format = null;
        long actL = 0;
        long expL = 0;
        if (expected instanceof Double) {
            final Double exp = (Double) expected;
            final Double act = (Double) actual;
            if (isNumber(exp) && isNumber(act) && exp != 0) { // show difference as hex
                actL = Double.doubleToLongBits(act);
                expL = Double.doubleToLongBits(exp);
                if (Math.abs(actL - expL) == 1) {
                    // Not 100% sure off-by-one errors are allowed everywhere, so only allow for these methods
                    if (methodName.equals("toRadians") || methodName.equals("atan2")) {
                        return;
                    }
                }
                format = "%016x";
            }
        } else if (expected instanceof Float) {
            final Float exp = (Float) expected;
            final Float act = (Float) actual;
            if (isNumber(exp) && isNumber(act) && exp != 0) { // show difference as hex
                actL = Float.floatToIntBits(act);
                expL = Float.floatToIntBits(exp);
                format = "%08x";
            }
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(mathMethod.getReturnType().getSimpleName());
        sb.append(" ");
        sb.append(methodName);
        sb.append("(");
        String sep = "";
        for (final Object o : params) {
            sb.append(sep);
            sb.append(o);
            sep = ", ";
        }
        sb.append(") expected ");
        if (format != null) {
            sb.append(String.format(format, expL));
        } else {
            sb.append(expected);
        }
        sb.append(" actual ");
        if (format != null) {
            sb.append(String.format(format, actL));
        } else {
            sb.append(actual);
        }
        sb.append(" entries ");
        sb.append(Arrays.toString(entries));
        final String message = sb.toString();
        final boolean fatal = true;
        if (fatal) {
            Assert.fail(message);
        } else {
            System.out.println(message);
        }
    }

    private static void callMethods(final Method mathMethod, final Method fastMethod,
                                    final Object[] params, final int[] entries) throws IllegalAccessException,
                                                                               InvocationTargetException {
        try {
            final Object expected = mathMethod.invoke(mathMethod, params);
            Object actual = null;
            try {
                actual = fastMethod.invoke(mathMethod, params);
            } catch (final InvocationTargetException e) {
                // FastMath method has returned an Arithmetic exception: this is expected (tested in FastMathTest),
                // Behavior supposed to be different from Math Behavior
                if (e.getTargetException() instanceof ArithmeticException) {
                    return;
                }
                    Assert.fail();
            }
            if (!expected.equals(actual)) {
                reportFailedResults(mathMethod, params, expected, actual, entries);
            }
        } catch (final IllegalArgumentException e) {
            Assert.fail(mathMethod + " " + e);
        }
    }

    private static void setupMethodCall(final Method mathMethod, final Method fastMethod,
                                        final Type[] types, final Object[][] valueArrays) throws Exception {
        final Object[] params = new Object[types.length];
        int entry1 = 0;
        final int[] entries = new int[types.length];
        for (final Object d : valueArrays[0]) {
            entry1++;
            params[0] = d;
            entries[0] = entry1;
            if (params.length > 1) {
                int entry2 = 0;
                for (final Object d1 : valueArrays[1]) {
                    entry2++;
                    params[1] = d1;
                    entries[1] = entry2;
                    callMethods(mathMethod, fastMethod, params, entries);
                }
            } else {
                callMethods(mathMethod, fastMethod, params, entries);
            }
        }
    }

    @Parameters
    public static List<Object[]> data() throws Exception {
        final String singleMethod = System.getProperty("testMethod");
        final List<Object[]> list = new ArrayList<>();
        for (final Method mathMethod : StrictMath.class.getDeclaredMethods()) {
            method: if (Modifier.isPublic(mathMethod.getModifiers())) {// Only test public methods
                final Type[] types = mathMethod.getGenericParameterTypes();
                if (types.length >= 1) { // Only check methods with at least one parameter
                    try {
                        // Get the corresponding FastMath method
                        final Method fastMethod =
                            MathLib.class.getDeclaredMethod(mathMethod.getName(), (Class[]) types);
                        if (Modifier.isPublic(fastMethod.getModifiers())) { // It must be public too
                            if (singleMethod != null && !fastMethod.getName().equals(singleMethod)) {
                                break method;
                            }
                            final Object[][] values = new Object[types.length][];
                            int index = 0;
                            for (final Type t : types) {
                                if (t.equals(double.class)) {
                                    values[index] = DOUBLE_SPECIAL_VALUES;
                                } else if (t.equals(float.class)) {
                                    values[index] = FLOAT_SPECIAL_VALUES;
                                } else if (t.equals(long.class)) {
                                    values[index] = LONG_SPECIAL_VALUES;
                                } else if (t.equals(int.class)) {
                                    values[index] = INT_SPECIAL_VALUES;
                                } else {
                                    System.out.println("Cannot handle class " + t + " for " + mathMethod);
                                    break method;
                                }
                                index++;
                            }
                            // System.out.println(fastMethod);
                            /*
                             * The current implementation runs each method as a separate test.
                             * Could be amended to run each value as a separate test
                             */
                            list.add(new Object[] { mathMethod, fastMethod, types, values });
                            // setupMethodCall(mathMethod, fastMethod, params, data);
                        } else {
                            System.out.println("Cannot find public FastMath method corresponding to: " + mathMethod);
                        }
                    } catch (final NoSuchMethodException e) {
                        System.out.println("Cannot find FastMath method corresponding to: " + mathMethod);
                    }
                }
            }
        }
        return list;
    }
}
