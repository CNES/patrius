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
package fr.cnes.sirius.patrius.math.linear;

/**
 * This interface defines a visitor for the entries of a vector. Visitors
 * implementing this interface do not alter the entries of the vector being
 * visited.
 * 
 * @version $Id: RealVectorPreservingVisitor.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public interface RealVectorPreservingVisitor {
    /**
     * Start visiting a vector. This method is called once, before any entry
     * of the vector is visited.
     * 
     * @param dimension
     *        the size of the vector
     * @param start
     *        the index of the first entry to be visited
     * @param end
     *        the index of the last entry to be visited (inclusive)
     */
    void start(int dimension, int start, int end);

    /**
     * Visit one entry of the vector.
     * 
     * @param index
     *        the index of the entry being visited
     * @param value
     *        the value of the entry being visited
     */
    void visit(int index, double value);

    /**
     * End visiting a vector. This method is called once, after all entries of
     * the vector have been visited.
     * 
     * @return the value returned by {@link RealVector#walkInDefaultOrder(RealVectorPreservingVisitor)},
     *         {@link RealVector#walkInDefaultOrder(RealVectorPreservingVisitor, int, int)},
     *         {@link RealVector#walkInOptimizedOrder(RealVectorPreservingVisitor)} or
     *         {@link RealVector#walkInOptimizedOrder(RealVectorPreservingVisitor, int, int)}
     */
    double end();
}
