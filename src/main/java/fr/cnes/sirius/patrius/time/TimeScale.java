/*
 * Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * HISTORY
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;

/**
 * Interface for time scales.
 * <p>
 * This is the interface representing all time scales. Time scales are related to each other by some offsets that may be
 * discontinuous (for example the {@link UTCScale UTC scale} with respect to the {@link TAIScale
 * TAI scale}).
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public interface TimeScale extends Serializable {

    /**
     * Get the offset to convert locations from {@link TAIScale} to instance.
     * 
     * @param date
     *        conversion date
     * @return offset in seconds to add to a location in <em>{@link TAIScale}
     * time scale</em> to get a location in <em>instance time scale</em>
     * @see #offsetToTAI(DateComponents, TimeComponents)
     */
    double offsetFromTAI(AbsoluteDate date);

    /**
     * Get the offset to convert locations from instance to {@link TAIScale}.
     * 
     * @param date
     *        date location in the time scale
     * @param time
     *        time location in the time scale
     * @return offset in seconds to add to a location in <em>instance time scale</em> to get a location in
     *         <em>{@link TAIScale} time scale</em>
     * @see #offsetFromTAI(AbsoluteDate)
     */
    double offsetToTAI(final DateComponents date, final TimeComponents time);

    /**
     * Get the name time scale.
     * 
     * @return name of the time scale
     */
    String getName();

}
