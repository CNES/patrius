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
package fr.cnes.sirius.patrius.math.exception.util;

import java.util.Locale;

/**
 * Dummy implementation of the {@link Localizable} interface, without localization.
 * 
 * @version $Id: DummyLocalizable.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
public class DummyLocalizable implements Localizable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8843275624471387299L;

    /** Source string. */
    private final String source;

    /**
     * Simple constructor.
     * 
     * @param sourceIn
     *        source text
     */
    public DummyLocalizable(final String sourceIn) {
        this.source = sourceIn;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceString() {
        return this.source;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalizedString(final Locale locale) {
        return this.source;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.source;
    }

}
