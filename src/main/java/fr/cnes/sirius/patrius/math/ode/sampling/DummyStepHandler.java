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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.sampling;

/**
 * This class is a step handler that does nothing.
 * 
 * <p>
 * This class is provided as a convenience for users who are only interested in the final state of an integration and
 * not in the intermediate steps. Its handleStep method does nothing.
 * </p>
 * 
 * <p>
 * Since this class has no internal state, it is implemented using the Singleton design pattern. This means that only
 * one instance is ever created, which can be retrieved using the getInstance method. This explains why there is no
 * public constructor.
 * </p>
 * 
 * @see StepHandler
 * @version $Id: DummyStepHandler.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public final class DummyStepHandler implements StepHandler {

    /**
     * Private constructor.
     * The constructor is private to prevent users from creating
     * instances (Singleton design-pattern).
     */
    private DummyStepHandler() {
        // Nothing to do
    }

    /**
     * Get the only instance.
     * 
     * @return the only instance
     */
    public static DummyStepHandler getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        // Nothing to do
    }

    /**
     * Handle the last accepted step.
     * This method does nothing in this class.
     * 
     * @param interpolator
     *        interpolator for the last accepted step. For
     *        efficiency purposes, the various integrators reuse the same
     *        object on each call, so if the instance wants to keep it across
     *        all calls (for example to provide at the end of the integration a
     *        continuous model valid throughout the integration range), it
     *        should build a local copy using the clone method and store this
     *        copy.
     * @param isLast
     *        true if the step is the last one
     */
    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
        // Nothing to do
    }

    /**
     * Handle deserialization of the singleton.
     * 
     * @return the singleton instance
     */
    private Object readResolve() {
        // return the singleton instance
        return LazyHolder.INSTANCE;
    }

    /**
     * Holder for the instance.
     * <p>
     * We use here the Initialization On Demand Holder Idiom.
     * </p>
     */
    private static class LazyHolder {
        /** Cached field instance. */
        private static final DummyStepHandler INSTANCE = new DummyStepHandler();
    }

}
