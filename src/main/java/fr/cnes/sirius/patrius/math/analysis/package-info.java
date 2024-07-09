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
/**
 *
 *    <p>
 *      Parent package for common numerical analysis procedures, including root finding,
 *      function interpolation and integration. Note that the optimization (i.e. minimization
 *      and maximization) is a huge separate top package, despite it also operate on functions
 *      as defined by this top-level package.
 *    </p>
 *    <p>
 *      Functions interfaces are intended to be implemented by user code to represent their
 *      domain problems. The algorithms provided by the library will then operate on these
 *      function to find their roots, or integrate them, or ... Functions can be multivariate
 *      or univariate, real vectorial or matrix valued, and they can be differentiable or not.
 *    </p>
 *
 */
package fr.cnes.sirius.patrius.math.analysis;

