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
package fr.cnes.sirius.patrius.math.optim.linear;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.cnes.sirius.patrius.math.optim.OptimizationData;

/**
 * Class that represents a set of {@link LinearConstraint linear constraints}.
 * 
 * @version $Id: LinearConstraintSet.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class LinearConstraintSet implements OptimizationData {
    /** Set of constraints. */
    private final Set<LinearConstraint> linearConstraints = new HashSet<LinearConstraint>();

    /**
     * Creates a set containing the given constraints.
     * 
     * @param constraints
     *        Constraints.
     */
    public LinearConstraintSet(final LinearConstraint... constraints) {
        for (final LinearConstraint c : constraints) {
            this.linearConstraints.add(c);
        }
    }

    /**
     * Creates a set containing the given constraints.
     * 
     * @param constraints
     *        Constraints.
     */
    public LinearConstraintSet(final Collection<LinearConstraint> constraints) {
        this.linearConstraints.addAll(constraints);
    }

    /**
     * Gets the set of linear constraints.
     * 
     * @return the constraints.
     */
    public Collection<LinearConstraint> getConstraints() {
        return Collections.unmodifiableSet(this.linearConstraints);
    }
}
