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
 * This interface defines mappers between a space and one of its sub-spaces.
 * 
 * <p>
 * Sub-spaces are the lower dimensions subsets of a n-dimensions space. The (n-1)-dimension sub-spaces are specific
 * sub-spaces known as {@link Hyperplane hyperplanes}. This interface can be used regardless of the dimensions
 * differences. As an example, {@link fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line Line} in 3D implements
 * Embedding<{@link fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D Vector3D}, {link
 * fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D Vector1D>, i.e. it maps directly dimensions 3 and 1.
 * </p>
 * 
 * <p>
 * In the 3D euclidean space, hyperplanes are 2D planes, and the 1D sub-spaces are lines.
 * </p>
 * 
 * @param <S>
 *        Type of the embedding space.
 * @param <T>
 *        Type of the embedded sub-space.
 * 
 * @see Hyperplane
 * @version $Id: Embedding.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface Embedding<S extends Space, T extends Space> {

    /**
     * Transform a space point into a sub-space point.
     * 
     * @param point
     *        n-dimension point of the space
     * @return (n-1)-dimension point of the sub-space corresponding to
     *         the specified space point
     * @see #toSpace
     */
    @SuppressWarnings("PMD.LooseCoupling")
    Vector<T> toSubSpace(Vector<S> point);

    /**
     * Transform a sub-space point into a space point.
     * 
     * @param point
     *        (n-1)-dimension point of the sub-space
     * @return n-dimension point of the space corresponding to the
     *         specified sub-space point
     * @see #toSubSpace
     */
    @SuppressWarnings("PMD.LooseCoupling")
    Vector<S> toSpace(Vector<T> point);

    // CHECKSTYLE: resume IllegalType check
}
