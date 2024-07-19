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
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when function values have the same sign at both
 * ends of an interval.
 * 
 * @since 3.0
 * @version $Id: NoBracketingException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NoBracketingException extends MathIllegalArgumentException {
     /** Serializable UID. */
    private static final long serialVersionUID = -3629324471511904459L;
    /** Lower end of the interval. */
    private final double lo;
    /** Higher end of the interval. */
    private final double hi;
    /** Value at lower end of the interval. */
    private final double fLo;
    /** Value at higher end of the interval. */
    private final double fHi;

    /**
     * Construct the exception.
     * 
     * @param loIn
     *        Lower end of the interval.
     * @param hiIn
     *        Higher end of the interval.
     * @param fLoIn
     *        Value at lower end of the interval.
     * @param fHiIn
     *        Value at higher end of the interval.
     */
    public NoBracketingException(final double loIn, final double hiIn,
        final double fLoIn, final double fHiIn) {
        this(PatriusMessages.SAME_SIGN_AT_ENDPOINTS, loIn, hiIn, fLoIn, fHiIn);
    }

    /**
     * Construct the exception with a specific context.
     * 
     * @param specific
     *        Contextual information on what caused the exception.
     * @param loIn
     *        Lower end of the interval.
     * @param hiIn
     *        Higher end of the interval.
     * @param fLoIn
     *        Value at lower end of the interval.
     * @param fHiIn
     *        Value at higher end of the interval.
     * @param args
     *        Additional arguments.
     */
    public NoBracketingException(final Localizable specific,
        final double loIn, final double hiIn,
        final double fLoIn, final double fHiIn,
        final Object... args) {
        super(specific, loIn, hiIn, fLoIn, fHiIn, args);
        this.lo = loIn;
        this.hi = hiIn;
        this.fLo = fLoIn;
        this.fHi = fHiIn;
    }

    /**
     * Get the lower end of the interval.
     * 
     * @return the lower end.
     */
    public double getLo() {
        return this.lo;
    }

    /**
     * Get the higher end of the interval.
     * 
     * @return the higher end.
     */
    public double getHi() {
        return this.hi;
    }

    /**
     * Get the value at the lower end of the interval.
     * 
     * @return the value at the lower end.
     */
    public double getFLo() {
        return this.fLo;
    }

    /**
     * Get the value at the higher end of the interval.
     * 
     * @return the value at the higher end.
     */
    public double getFHi() {
        return this.fHi;
    }
}
