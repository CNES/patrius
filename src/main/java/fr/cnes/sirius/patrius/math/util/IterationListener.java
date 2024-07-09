/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
package fr.cnes.sirius.patrius.math.util;

import java.util.EventListener;

/**
 * The listener interface for receiving events occurring in an iterative
 * algorithm.
 * 
 * @version $Id: IterationListener.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface IterationListener extends EventListener {
    /**
     * Invoked after completion of the initial phase of the iterative algorithm
     * (prior to the main iteration loop).
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    void initializationPerformed(IterationEvent e);

    /**
     * Invoked each time an iteration is completed (in the main iteration loop).
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    void iterationPerformed(IterationEvent e);

    /**
     * Invoked each time a new iteration is completed (in the main iteration
     * loop).
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    void iterationStarted(IterationEvent e);

    /**
     * Invoked after completion of the operations which occur after breaking out
     * of the main iteration loop.
     * 
     * @param e
     *        The {@link IterationEvent} object.
     */
    void terminationPerformed(IterationEvent e);
}
