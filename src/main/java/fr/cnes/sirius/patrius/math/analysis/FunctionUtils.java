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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Identity;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Utilities for manipulating function objects.
 * 
 * @version $Id: FunctionUtils.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public final class FunctionUtils {
    /**
     * Class only contains static methods.
     */
    private FunctionUtils() {
    }

    /**
     * Composes functions. <br/>
     * The functions in the argument list are composed sequentially, in the
     * given order. For example, compose(f1,f2,f3) acts like f1(f2(f3(x))).
     * 
     * @param f
     *        List of functions.
     * @return the composite function.
     */
    public static UnivariateFunction compose(final UnivariateFunction... f) {
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 6472430598847430383L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                double r = x;
                for (int i = f.length - 1; i >= 0; i--) {
                    r = f[i].value(r);
                }
                return r;
            }
        };
    }

    /**
     * Composes functions. <br/>
     * The functions in the argument list are composed sequentially, in the
     * given order. For example, compose(f1,f2,f3) acts like f1(f2(f3(x))).
     * 
     * @param f
     *        List of functions.
     * @return the composite function.
     * @since 3.1
     */
    public static UnivariateDifferentiableFunction compose(final UnivariateDifferentiableFunction... f) {
        return new UnivariateDifferentiableFunction(){

            /** Serializable UID. */
            private static final long serialVersionUID = -7645687979962168936L;

            /** {@inheritDoc} */
            @Override
            public double value(final double t) {
                double r = t;
                for (int i = f.length - 1; i >= 0; i--) {
                    r = f[i].value(r);
                }
                return r;
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure value(final DerivativeStructure t) {
                DerivativeStructure r = t;
                for (int i = f.length - 1; i >= 0; i--) {
                    r = f[i].value(r);
                }
                return r;
            }
        };
    }

    /**
     * Adds functions.
     * 
     * @param f
     *        List of functions.
     * @return a function that computes the sum of the functions.
     */
    public static UnivariateFunction add(final UnivariateFunction... f) {
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2319735598727158284L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                double r = f[0].value(x);
                for (int i = 1; i < f.length; i++) {
                    r += f[i].value(x);
                }
                return r;
            }
        };
    }

    /**
     * Adds functions.
     * 
     * @param f
     *        List of functions.
     * @return a function that computes the sum of the functions.
     * @since 3.1
     */
    public static UnivariateDifferentiableFunction add(final UnivariateDifferentiableFunction... f) {
        return new UnivariateDifferentiableFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8138953674658294123L;

            /** {@inheritDoc} */
            @Override
            public double value(final double t) {
                double r = f[0].value(t);
                for (int i = 1; i < f.length; i++) {
                    r += f[i].value(t);
                }
                return r;
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure value(final DerivativeStructure t) {
                DerivativeStructure r = f[0].value(t);
                for (int i = 1; i < f.length; i++) {
                    r = r.add(f[i].value(t));
                }
                return r;
            }

        };
    }

    /**
     * Multiplies functions.
     * 
     * @param f
     *        List of functions.
     * @return a function that computes the product of the functions.
     */
    public static UnivariateFunction multiply(final UnivariateFunction... f) {
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7091625421955582609L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                double r = f[0].value(x);
                for (int i = 1; i < f.length; i++) {
                    r *= f[i].value(x);
                }
                return r;
            }
        };
    }

    /**
     * Multiplies functions.
     * 
     * @param f
     *        List of functions.
     * @return a function that computes the product of the functions.
     * @since 3.1
     */
    public static UnivariateDifferentiableFunction multiply(final UnivariateDifferentiableFunction... f) {
        return new UnivariateDifferentiableFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1136576946512766783L;

            /** {@inheritDoc} */
            @Override
            public double value(final double t) {
                double r = f[0].value(t);
                for (int i = 1; i < f.length; i++) {
                    r *= f[i].value(t);
                }
                return r;
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure value(final DerivativeStructure t) {
                DerivativeStructure r = f[0].value(t);
                for (int i = 1; i < f.length; i++) {
                    r = r.multiply(f[i].value(t));
                }
                return r;
            }

        };
    }

    /**
     * Returns the univariate function <br/>
     * {@code h(x) = combiner(f(x), g(x))}.
     * 
     * @param combiner
     *        Combiner function.
     * @param f
     *        Function.
     * @param g
     *        Function.
     * @return the composite function.
     */
    public static UnivariateFunction combine(final BivariateFunction combiner,
                                             final UnivariateFunction f,
                                             final UnivariateFunction g) {
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 630568504439153022L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return combiner.value(f.value(x), g.value(x));
            }
        };
    }

    /**
     * Returns a MultivariateFunction h(x[]) defined by
     * 
     * <pre>
     * <code>
     * h(x[]) = combiner(...combiner(combiner(initialValue,f(x[0])),f(x[1]))...),f(x[x.length-1]))
     * </code>
     * </pre>
     * 
     * @param combiner
     *        Combiner function.
     * @param f
     *        Function.
     * @param initialValue
     *        Initial value.
     * @return a collector function.
     */
    public static MultivariateFunction collector(final BivariateFunction combiner,
                                                 final UnivariateFunction f,
                                                 final double initialValue) {
        return new MultivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double[] point) {
                double result = combiner.value(initialValue, f.value(point[0]));
                for (int i = 1; i < point.length; i++) {
                    result = combiner.value(result, f.value(point[i]));
                }
                return result;
            }
        };
    }

    /**
     * Returns a MultivariateFunction h(x[]) defined by
     * 
     * <pre>
     * <code>
     * h(x[]) = combiner(...combiner(combiner(initialValue,x[0]),x[1])...),x[x.length-1])
     * </code>
     * </pre>
     * 
     * @param combiner
     *        Combiner function.
     * @param initialValue
     *        Initial value.
     * @return a collector function.
     */
    public static MultivariateFunction collector(final BivariateFunction combiner,
                                                 final double initialValue) {
        return collector(combiner, new Identity(), initialValue);
    }

    /**
     * Creates a unary function by fixing the first argument of a binary function.
     * 
     * @param f
     *        Binary function.
     * @param fixed
     *        Value to which the first argument of {@code f} is set.
     * @return the unary function h(x) = f(fixed, x)
     */
    public static UnivariateFunction fix1stArgument(final BivariateFunction f,
                                                    final double fixed) {
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 6518928187686876240L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return f.value(fixed, x);
            }
        };
    }

    /**
     * Creates a unary function by fixing the second argument of a binary function.
     * 
     * @param f
     *        Binary function.
     * @param fixed
     *        Value to which the second argument of {@code f} is set.
     * @return the unary function h(x) = f(x, fixed)
     */
    public static UnivariateFunction fix2ndArgument(final BivariateFunction f,
                                                    final double fixed) {
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3903321673401849196L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return f.value(x, fixed);
            }
        };
    }

    /**
     * Samples the specified univariate real function on the specified interval. <br/>
     * The interval is divided equally into {@code n} sections and sample points
     * are taken from {@code min} to {@code max - (max - min) / n}; therefore {@code f} is not sampled at the upper
     * bound {@code max}.
     * 
     * @param f
     *        Function to be sampled
     * @param min
     *        Lower bound of the interval (included).
     * @param max
     *        Upper bound of the interval (excluded).
     * @param n
     *        Number of sample points.
     * @return the array of samples.
     * @throws NumberIsTooLargeException
     *         if the lower bound {@code min} is
     *         greater than, or equal to the upper bound {@code max}.
     * @throws NotStrictlyPositiveException
     *         if the number of sample points {@code n} is negative.
     */
    public static double[] sample(final UnivariateFunction f,
                                  final double min, final double max, final int n) {

        // Sanity check:
        // Check number of samples
        if (n <= 0) {
            throw new NotStrictlyPositiveException(
                PatriusMessages.NOT_POSITIVE_NUMBER_OF_SAMPLES,
                Integer.valueOf(n));
        }
        // Check interval bounds
        if (min >= max) {
            throw new NumberIsTooLargeException(min, max, false);
        }

        final double[] s = new double[n];
        final double h = (max - min) / n;
        for (int i = 0; i < n; i++) {
            s[i] = f.value(min + i * h);
        }
        // return the constructed sample
        return s;
    }
}
