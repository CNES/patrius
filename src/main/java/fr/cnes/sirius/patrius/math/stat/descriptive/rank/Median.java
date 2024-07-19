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
package fr.cnes.sirius.patrius.math.stat.descriptive.rank;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * Returns the median of the available values. This is the same as the 50th percentile.
 * See {@link Percentile} for a description of the algorithm used.
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Median.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Median extends Percentile {

     /** Serializable UID. */
    private static final long serialVersionUID = -3961477041290915687L;

    /** 50. */
    private static final double FIFTY = 50;

    /**
     * Default constructor.
     */
    public Median() {
        // No try-catch or advertised exception - arg is valid
        super(FIFTY);
    }

    /**
     * Copy constructor, creates a new {@code Median} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Median} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Median(final Median original) {
        super(original);
    }

}
