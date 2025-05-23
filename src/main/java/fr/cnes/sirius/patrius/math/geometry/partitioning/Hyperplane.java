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
package fr.cnes.sirius.patrius.math.geometry.partitioning;

import fr.cnes.sirius.patrius.math.geometry.Space;
import fr.cnes.sirius.patrius.math.geometry.Vector;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This interface represents an hyperplane of a space.
 * 
 * <p>
 * The most prominent place where hyperplane appears in space partitioning is as cutters. Each partitioning node in a
 * {@link BSPTree BSP tree} has a cut {@link SubHyperplane sub-hyperplane} which is either an hyperplane or a part of an
 * hyperplane. In an n-dimensions euclidean space, an hyperplane is an (n-1)-dimensions hyperplane (for example a
 * traditional plane in the 3D euclidean space). They can be more exotic objects in specific fields, for example a
 * circle on the surface of the unit sphere.
 * </p>
 * 
 * @param <S>
 *        Type of the space.
 * 
 * @version $Id: Hyperplane.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface Hyperplane<S extends Space> {

    /**
     * Copy the instance.
     * <p>
     * The instance created is completely independant of the original one. A deep copy is used, none of the underlying
     * objects are shared (except for immutable objects).
     * </p>
     * 
     * @return a new hyperplane, copy of the instance
     */
    Hyperplane<S> copySelf();

    /**
     * Get the offset (oriented distance) of a point.
     * <p>
     * The offset is 0 if the point is on the underlying hyperplane, it is positive if the point is on one particular
     * side of the hyperplane, and it is negative if the point is on the other side, according to the hyperplane natural
     * orientation.
     * </p>
     * 
     * @param point
     *        point to check
     * @return offset of the point
     */
    @SuppressWarnings("PMD.LooseCoupling")
    double getOffset(Vector<S> point);

    /**
     * Check if the instance has the same orientation as another hyperplane.
     * <p>
     * This method is expected to be called on parallel hyperplanes. The method should <em>not</em> re-check for
     * parallelism, only for orientation, typically by testing something like the sign of the dot-products of normals.
     * </p>
     * 
     * @param other
     *        other hyperplane to check against the instance
     * @return true if the instance and the other hyperplane have
     *         the same orientation
     */
    boolean sameOrientationAs(Hyperplane<S> other);

    /**
     * Build a sub-hyperplane covering the whole hyperplane.
     * 
     * @return a sub-hyperplane covering the whole hyperplane
     */
    SubHyperplane<S> wholeHyperplane();

    /**
     * Build a region covering the whole space.
     * 
     * @return a region containing the instance
     */
    Region<S> wholeSpace();

    // CHECKSTYLE: resume IllegalType check
}
