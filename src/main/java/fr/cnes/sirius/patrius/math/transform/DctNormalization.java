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
package fr.cnes.sirius.patrius.math.transform;

/**
 * This enumeration defines the various types of normalizations that can be
 * applied to discrete cosine transforms (DCT). The exact definition of these
 * normalizations is detailed below.
 * 
 * @see FastCosineTransformer
 * @version $Id: DctNormalization.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public enum DctNormalization {
    /**
     * Should be passed to the constructor of {@link FastCosineTransformer} to use the <em>standard</em> normalization
     * convention. The standard
     * DCT-I normalization convention is defined as follows
     * <ul>
     * <li>forward transform: y<sub>n</sub> = (1/2) [x<sub>0</sub> + (-1)<sup>n</sup>x<sub>N-1</sub>] +
     * &sum;<sub>k=1</sub><sup>N-2</sup> x<sub>k</sub> cos[&pi; nk / (N - 1)],</li>
     * <li>inverse transform: x<sub>k</sub> = [1 / (N - 1)] [y<sub>0</sub> + (-1)<sup>k</sup>y<sub>N-1</sub>] + [2 / (N
     * - 1)] &sum;<sub>n=1</sub><sup>N-2</sup> y<sub>n</sub> cos[&pi; nk / (N - 1)],</li>
     * </ul>
     * where N is the size of the data sample.
     */
    STANDARD_DCT_I,

    /**
     * Should be passed to the constructor of {@link FastCosineTransformer} to use the <em>orthogonal</em> normalization
     * convention. The orthogonal
     * DCT-I normalization convention is defined as follows
     * <ul>
     * <li>forward transform: y<sub>n</sub> = [2(N - 1)]<sup>-1/2</sup> [x<sub>0</sub> +
     * (-1)<sup>n</sup>x<sub>N-1</sub>] + [2 / (N - 1)]<sup>1/2</sup> &sum;<sub>k=1</sub><sup>N-2</sup> x<sub>k</sub>
     * cos[&pi; nk / (N - 1)],</li>
     * <li>inverse transform: x<sub>k</sub> = [2(N - 1)]<sup>-1/2</sup> [y<sub>0</sub> +
     * (-1)<sup>k</sup>y<sub>N-1</sub>] + [2 / (N - 1)]<sup>1/2</sup> &sum;<sub>n=1</sub><sup>N-2</sup> y<sub>n</sub>
     * cos[&pi; nk / (N - 1)],</li>
     * </ul>
     * which makes the transform orthogonal. N is the size of the data sample.
     */
    ORTHOGONAL_DCT_I;
}
