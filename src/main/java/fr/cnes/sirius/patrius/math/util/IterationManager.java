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
package fr.cnes.sirius.patrius.math.util;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * This abstract class provides a general framework for managing iterative
 * algorithms. The maximum number of iterations can be set, and methods are
 * provided to monitor the current iteration count. A lightweight event
 * framework is also provided.
 * 
 * @version $Id: IterationManager.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class IterationManager {

    /** Keeps a count of the number of iterations. */
    private final Incrementor iterations;

    /** The collection of all listeners attached to this iterative algorithm. */
    private final Collection<IterationListener> listeners;

    /**
     * Creates a new instance of this class.
     * 
     * @param maxIterations
     *        the maximum number of iterations
     */
    public IterationManager(final int maxIterations) {
        this.iterations = new Incrementor(maxIterations);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Creates a new instance of this class.
     * 
     * @param maxIterations
     *        the maximum number of iterations
     * @param callBack
     *        the function to be called when the maximum number of
     *        iterations has been reached
     * @throws fr.cnes.sirius.patrius.math.exception.NullArgumentException
     *         if {@code callBack} is {@code null}
     * @since 3.1
     */
    public IterationManager(final int maxIterations,
        final Incrementor.MaxCountExceededCallback callBack) {
        this.iterations = new Incrementor(maxIterations, callBack);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Attaches a listener to this manager.
     * 
     * @param listener
     *        A {@code IterationListener} object.
     */
    public void addIterationListener(final IterationListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Informs all registered listeners that the initial phase (prior to the
     * main iteration loop) has been completed.
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    public void fireInitializationEvent(final IterationEvent e) {
        for (final IterationListener l : this.listeners) {
            l.initializationPerformed(e);
        }
    }

    /**
     * Informs all registered listeners that a new iteration (in the main
     * iteration loop) has been performed.
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    public void fireIterationPerformedEvent(final IterationEvent e) {
        for (final IterationListener l : this.listeners) {
            l.iterationPerformed(e);
        }
    }

    /**
     * Informs all registered listeners that a new iteration (in the main
     * iteration loop) has been started.
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    public void fireIterationStartedEvent(final IterationEvent e) {
        for (final IterationListener l : this.listeners) {
            l.iterationStarted(e);
        }
    }

    /**
     * Informs all registered listeners that the final phase (post-iterations)
     * has been completed.
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    public void fireTerminationEvent(final IterationEvent e) {
        for (final IterationListener l : this.listeners) {
            l.terminationPerformed(e);
        }
    }

    /**
     * Returns the number of iterations of this solver, 0 if no iterations has
     * been performed yet.
     * 
     * @return the number of iterations.
     */
    public int getIterations() {
        return this.iterations.getCount();
    }

    /**
     * Returns the maximum number of iterations.
     * 
     * @return the maximum number of iterations.
     */
    public int getMaxIterations() {
        return this.iterations.getMaximalCount();
    }

    /**
     * Increments the iteration count by one, and throws an exception if the
     * maximum number of iterations is reached. This method should be called at
     * the beginning of a new iteration.
     * 
     * @throws MaxCountExceededException
     *         if the maximum number of iterations is
     *         reached.
     */
    public void incrementIterationCount() {
        this.iterations.incrementCount();
    }

    /**
     * Removes the specified iteration listener from the list of listeners
     * currently attached to {@code this} object. Attempting to remove a
     * listener which was <em>not</em> previously registered does not cause any
     * error.
     * 
     * @param listener
     *        The {@link IterationListener} to be removed.
     */
    public void removeIterationListener(final IterationListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Sets the iteration count to 0. This method must be called during the
     * initial phase.
     */
    public void resetIterationCount() {
        this.iterations.resetCount();
    }
}
