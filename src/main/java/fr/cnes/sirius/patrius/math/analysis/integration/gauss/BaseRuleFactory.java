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
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.Pair;

/**
 * Base class for rules that determines the integration nodes and their
 * weights.
 * Subclasses must implement the {@link #computeRule(int) computeRule} method.
 * 
 * @param <T>
 *        Type of the number used to represent the points and weights of
 *        the quadrature rules.
 * 
 * @since 3.1
 * @version $Id: BaseRuleFactory.java 18108 2017-10-04 06:45:27Z bignon $
 */
// CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BaseRuleFactory<T extends Number> {
    // CHECKSTYLE: resume AbstractClassName check

    /** List of points and weights, indexed by the order of the rule. */
    private final Map<Integer, Pair<T[], T[]>> pointsAndWeights = new ConcurrentHashMap<>();
    /** Cache for double-precision rules. */
    private final Map<Integer, Pair<double[], double[]>> pointsAndWeightsDouble = new ConcurrentHashMap<>();

    /**
     * Gets a copy of the quadrature rule with the given number of integration
     * points.
     * 
     * @param numberOfPoints
     *        Number of integration points.
     * @return a copy of the integration rule.
     * @throws NotStrictlyPositiveException
     *         if {@code numberOfPoints < 1}.
     */
    public Pair<double[], double[]> getRule(final int numberOfPoints) {
        // Try to obtain the rule from the cache.
        Pair<double[], double[]> cached = this.pointsAndWeightsDouble.get(numberOfPoints);

        if (cached == null) {
            // Rule not computed yet.

            // Compute the rule.
            final Pair<T[], T[]> rule = this.getRuleInternal(numberOfPoints);
            cached = convertToDouble(rule);

            // Cache it.
            this.pointsAndWeightsDouble.put(numberOfPoints, cached);
        }

        // Return a copy.
        return new Pair<>(cached.getFirst().clone(), cached.getSecond().clone());
    }

    /**
     * Gets a rule.
     * Synchronization ensures that rules will be computed and added to the
     * cache at most once.
     * The returned rule is a reference into the cache.
     * 
     * @param numberOfPoints
     *        Order of the rule to be retrieved.
     * @return the points and weights corresponding to the given order.
     * @throws NotStrictlyPositiveException
     *         if {@code numberOfPoints < 1}.
     */
    protected synchronized Pair<T[], T[]> getRuleInternal(final int numberOfPoints) {
        final Pair<T[], T[]> rule = this.pointsAndWeights.get(numberOfPoints);
        if (rule == null) {
            this.addRule(this.computeRule(numberOfPoints));
            // The rule should be available now.
            return this.getRuleInternal(numberOfPoints);
        }
        return rule;
    }

    /**
     * Stores a rule.
     * 
     * @param rule
     *        Rule to be stored.
     * @throws DimensionMismatchException
     *         if the elements of the pair do not
     *         have the same length.
     */
    protected void addRule(final Pair<T[], T[]> rule) {
        if (rule.getFirst().length != rule.getSecond().length) {
            throw new DimensionMismatchException(rule.getFirst().length,
                rule.getSecond().length);
        }

        this.pointsAndWeights.put(rule.getFirst().length, rule);
    }

    /**
     * Computes the rule for the given order.
     * 
     * @param numberOfPoints
     *        Order of the rule to be computed.
     * @return the computed rule.
     */
    protected abstract Pair<T[], T[]> computeRule(int numberOfPoints);

    /**
     * Converts the from the actual {@code Number} type to {@code double}
     * 
     * @param <T>
     *        Type of the number used to represent the points and
     *        weights of the quadrature rules.
     * @param rule
     *        Points and weights.
     * @return points and weights as {@code double}s.
     */
    private static <T extends Number> Pair<double[], double[]> convertToDouble(final Pair<T[], T[]> rule) {
        // Rule points and weights (T arrays)
        final T[] pT = rule.getFirst();
        final T[] wT = rule.getSecond();

        // Assign points and weights arrays as double arrays
        final int len = pT.length;
        final double[] pD = new double[len];
        final double[] wD = new double[len];

        // Fill double arrays with values from T arrays
        for (int i = 0; i < len; i++) {
            pD[i] = pT[i].doubleValue();
            wD[i] = wT[i].doubleValue();
        }

        return new Pair<>(pD, wD);
    }
}
